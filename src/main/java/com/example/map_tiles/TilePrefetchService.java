package com.example.map_tiles;

import com.example.util.DBConnectionManager;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * High-level orchestrator that ties together {@link MapCache},
 * {@link TileDownloader}, and {@link LocalTileServer}.
 *
 * <p>This is the only class controllers should need to call. The typical
 * lifecycle is:</p>
 * <pre>{@code
 * // On app startup (e.g. in your Application.start())
 * TilePrefetchService prefetch = TilePrefetchService.getInstance();
 * int port = prefetch.startServer();        // start serving cached tiles
 * prefetch.prefetchAllBarangaysAsync(...);  // download in background
 *
 * // In your HTML template
 * "L.tileLayer('http://localhost:" + port + "/{z}/{x}/{y}.png')"
 * }</pre>
 *
 * <p>The prefetch is non-blocking — it returns a {@link CompletableFuture}
 * the caller can chain progress reporting onto. The UI thread never has
 * to wait for downloads to complete; tiles already on disk render
 * instantly, and tiles still downloading fall back to OSM (or the
 * dreaded gray checkerboard if both fail).</p>
 *
 * <p><b>Configurable prefetch coverage:</b> per the design decision,
 * we prefetch every barangay's bounding box at zoom levels 13-17.
 * Constants below can be tuned.</p>
 */
public final class TilePrefetchService {

    // ─── Tuning constants ───────────────────────────────────────

    /** Lowest zoom to prefetch (regional context — whole city visible). */
    private static final int MIN_ZOOM = 13;

    /** Highest zoom to prefetch (street-level — individual buildings legible). */
    private static final int MAX_ZOOM = 17;

    /**
     * How far around each barangay's center point to prefetch, in degrees.
     * 0.015 ≈ 1.5 km radius — enough to cover a full barangay plus its
     * immediate neighbors without blowing up the download count.
     */
    private static final double BBOX_RADIUS_DEGREES = 0.012;

    // ─── Singleton ──────────────────────────────────────────────

    private static volatile TilePrefetchService instance;

    public static TilePrefetchService getInstance() {
        TilePrefetchService local = instance;
        if (local == null) {
            synchronized (TilePrefetchService.class) {
                local = instance;
                if (local == null) {
                    local = new TilePrefetchService();
                    instance = local;
                }
            }
        }
        return local;
    }

    // ─── State ──────────────────────────────────────────────────

    private final TileDownloader downloader = new TileDownloader();
    private final LocalTileServer server = LocalTileServer.getInstance();

    private TilePrefetchService() {}

    // ─── Public API ─────────────────────────────────────────────

    /**
     * Starts the local HTTP server. Returns the port it bound to.
     * Idempotent — safe to call multiple times.
     */
    public int startServer() throws Exception {
        return server.start();
    }

    public int getServerPort() {
        return server.getPort();
    }

    /**
     * Loads every barangay from the database and computes the full list
     * of tiles to prefetch across all of them at zoom levels
     * {@value #MIN_ZOOM}-{@value #MAX_ZOOM}.
     *
     * <p>Uses a {@link HashSet} to deduplicate tiles where adjacent
     * barangays' bboxes overlap.</p>
     */
    public List<TileCoord> computeAllBarangayTiles() throws SQLException {
        // Step 1: pull every barangay's center coordinates from the DB
        List<double[]> centers = new ArrayList<>();
        String sql = "SELECT latitude, longitude FROM users WHERE role = 'barangay' AND latitude IS NOT NULL";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                centers.add(new double[] {
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                });
            }
        }

        // Step 2: expand each center into a bbox and gather tiles.
        // HashSet dedupes overlaps between adjacent barangays.
        Set<TileCoord> uniqueTiles = new HashSet<>();
        for (double[] c : centers) {
            double[] bbox = TileMath.bboxAround(c[0], c[1], BBOX_RADIUS_DEGREES);
            uniqueTiles.addAll(TileMath.tilesInBboxAtZooms(
                    bbox[0], bbox[1], bbox[2], bbox[3],
                    MIN_ZOOM, MAX_ZOOM));
        }

        return new ArrayList<>(uniqueTiles);
    }

    /**
     * Kicks off a background prefetch of every barangay's tiles. Returns
     * immediately with a {@link CompletableFuture} that completes when
     * all downloads have finished (success or fail).
     *
     * <p>The {@code progressListener} is wrapped automatically in
     * {@link Platform#runLater}, so it's safe to update JavaFX UI from it.
     * The listener receives {@code (completed, total, lastResult)} where
     * {@code lastResult} is non-null only on the final invocation.</p>
     */
    public CompletableFuture<TileDownloader.PrefetchResult> prefetchAllBarangaysAsync(
            ProgressListener progressListener) {

        try {
            List<TileCoord> tiles = computeAllBarangayTiles();
            System.out.println("[TilePrefetch] Will prefetch " + tiles.size() + " unique tiles "
                    + "across zoom levels " + MIN_ZOOM + "-" + MAX_ZOOM);

            // Wrap the progress callback so UI updates happen on the FX thread.
            TileDownloader.ProgressCallback wrappedCallback = null;
            if (progressListener != null) {
                wrappedCallback = (done, total) ->
                        Platform.runLater(() -> progressListener.onProgress(done, total, null));
            }

            return downloader.prefetchAll(tiles, wrappedCallback)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            System.err.println("[TilePrefetch] Failed: " + error.getMessage());
                        } else {
                            System.out.println("[TilePrefetch] " + result);
                            if (progressListener != null) {
                                Platform.runLater(() ->
                                        progressListener.onProgress(
                                                result.totalRequested(),
                                                result.totalRequested(),
                                                result));
                            }
                        }
                    });

        } catch (SQLException e) {
            CompletableFuture<TileDownloader.PrefetchResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * Convenience overload for prefetching a single specific area —
     * useful for "download this region" features later on.
     */
    public CompletableFuture<TileDownloader.PrefetchResult> prefetchAreaAsync(
            double minLat, double minLng,
            double maxLat, double maxLng,
            int minZoom, int maxZoom,
            ProgressListener listener) {

        List<TileCoord> tiles = TileMath.tilesInBboxAtZooms(
                minLat, minLng, maxLat, maxLng, minZoom, maxZoom);

        TileDownloader.ProgressCallback wrapped = listener == null ? null :
                (done, total) -> Platform.runLater(() -> listener.onProgress(done, total, null));

        return downloader.prefetchAll(tiles, wrapped);
    }

    /** Call on app shutdown to clean up threads. */
    public void shutdown() {
        downloader.shutdown();
        server.stop();
    }

    /** UI-facing progress callback — runs on the JavaFX Application Thread. */
    @FunctionalInterface
    public interface ProgressListener {
        /**
         * @param completed   how many tiles processed so far
         * @param total       total tile count for this prefetch
         * @param finalResult non-null only on the final invocation
         */
        void onProgress(int completed, int total, TileDownloader.PrefetchResult finalResult);
    }
}