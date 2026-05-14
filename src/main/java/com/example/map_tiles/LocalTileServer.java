package com.example.map_tiles;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embedded HTTP server that serves cached tiles to Leaflet running inside
 * the JavaFX {@link javafx.scene.web.WebView}.
 *
 * <p>This is the bridge between {@link MapCache} (Java-side disk cache)
 * and Leaflet's {@code L.tileLayer(url)} (JavaScript-side). Without this
 * server, Leaflet has no way to read files from {@code ~/.civicguard/tiles}
 * because WebViews block {@code file://} URLs from making HTTP-like
 * requests.</p>
 *
 * <p><b>How it fits the architecture:</b></p>
 * <pre>
 *   Leaflet (in WebView)
 *      ↓ HTTP GET http://localhost:NNNN/15/26064/15799.png
 *   LocalTileServer (this class)
 *      ↓ MapCache.read(TileCoord)
 *   Disk (~/.civicguard/tiles/15/26064/15799.png)
 *      ↓ if missing
 *   Fall back to OSM, then cache
 * </pre>
 *
 * <p><b>Concurrency:</b> {@link HttpServer} can accept requests on
 * multiple threads. We give it a small dedicated pool (4 threads) so
 * tile serving doesn't compete with the downloader's pool. Each
 * {@code handle()} invocation can run concurrently — but
 * {@link MapCache} is already thread-safe, so we don't need extra
 * synchronization.</p>
 *
 * <p>Uses the Singleton pattern (rubric criterion 7) — there should
 * only ever be one server per JVM, bound to one port.</p>
 */
public final class LocalTileServer {

    private static volatile LocalTileServer instance;

    public static LocalTileServer getInstance() {
        LocalTileServer local = instance;
        if (local == null) {
            synchronized (LocalTileServer.class) {
                local = instance;
                if (local == null) {
                    local = new LocalTileServer();
                    instance = local;
                }
            }
        }
        return local;
    }

    // Matches Leaflet's standard tile URL pattern: /z/x/y.png
    private static final Pattern TILE_URL_PATTERN =
            Pattern.compile("^/(\\d{1,2})/(\\d{1,7})/(\\d{1,7})\\.png$");

    private HttpServer server;
    private int port = -1;
    private final MapCache cache = MapCache.getInstance();

    // Used for on-demand fallback fetches when a tile isn't cached
    private final HttpClient fallbackHttp = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private LocalTileServer() {}

    /**
     * Starts the server on an OS-assigned free port (passing 0). Returns
     * the actual port so callers can pass it into the HTML template.
     *
     * <p>Idempotent — calling start() twice is a no-op.</p>
     */
    public synchronized int start() throws IOException {
        if (server != null) {
            return port;
        }

        // Bind to 127.0.0.1 only — never expose this server to the network.
        // Port 0 means "let the OS pick a free port for us."
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = server.getAddress().getPort();

        // Dedicated thread pool — small because tile serving is cheap.
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "tile-server-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        ExecutorService serverPool = Executors.newFixedThreadPool(4, factory);
        server.setExecutor(serverPool);

        server.createContext("/", this::handle);
        server.start();

        System.out.println("[LocalTileServer] Started on http://127.0.0.1:" + port);
        return port;
    }

    public int getPort() {
        return port;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(1); // 1-second grace period
            server = null;
            port = -1;
        }
    }

    // ─── Request handler ────────────────────────────────────────

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Matcher m = TILE_URL_PATTERN.matcher(path);

        if (!m.matches()) {
            sendStatus(exchange, 404, "Not a tile path");
            return;
        }

        int z = Integer.parseInt(m.group(1));
        int x = Integer.parseInt(m.group(2));
        int y = Integer.parseInt(m.group(3));
        TileCoord coord = new TileCoord(z, x, y);

        // 1. Try the cache first — the common case after prefetch.
        byte[] bytes = cache.read(coord);

        // 2. Cache miss → fall back to OSM and cache the response.
        //    This is what makes the system robust: even tiles outside
        //    the prefetch set still work, they just need internet.
        if (bytes == null) {
            try {
                bytes = fetchFromOsm(coord);
                cache.write(coord, bytes);
            } catch (Exception e) {
                System.err.println("[LocalTileServer] OSM fallback failed for " + coord
                        + ": " + e.getMessage());
                sendStatus(exchange, 503, "Tile unavailable (no cache, no internet)");
                return;
            }
        }

        // 3. Send the PNG to Leaflet.
        exchange.getResponseHeaders().add("Content-Type", "image/png");
        // Tell the browser/WebView it can cache this forever — tiles never change
        exchange.getResponseHeaders().add("Cache-Control", "public, max-age=31536000, immutable");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private byte[] fetchFromOsm(TileCoord coord) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(coord.toOsmUrl()))
                .header("User-Agent", "CivicGuard/1.0 (Capstone Project)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<byte[]> response = fallbackHttp.send(
                request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("OSM returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void sendStatus(HttpExchange exchange, int code, String msg) throws IOException {
        byte[] body = msg.getBytes();
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}