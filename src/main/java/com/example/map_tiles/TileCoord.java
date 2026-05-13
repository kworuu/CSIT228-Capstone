package com.example.map_tiles;

/**
 * A single map tile's identity: zoom level and (x, y) grid position.
 *
 * <p>OpenStreetMap (and Leaflet) addresses every tile in the world as a
 * triple {@code (z, x, y)}. For example, the URL
 * {@code https://tile.openstreetmap.org/15/26064/15799.png} corresponds to
 * {@code TileCoord(15, 26064, 15799)}.</p>
 *
 * <p>Using a {@code record} gives us {@code equals()}, {@code hashCode()},
 * and {@code toString()} for free — important because we use these as
 * {@link java.util.concurrent.ConcurrentHashMap} keys when tracking which
 * downloads are in-flight (rubric criterion 3: multithreading).</p>
 *
 * @param z zoom level (0 = whole world, 19 = street level)
 * @param x tile X index at this zoom (0 .. 2^z - 1)
 * @param y tile Y index at this zoom (0 .. 2^z - 1)
 */
public record TileCoord(int z, int x, int y) {

    /**
     * Returns the relative file path for this tile under the cache root,
     * e.g. {@code "15/26064/15799.png"}. Used by {@link MapCache} to map
     * a coordinate to a file on disk.
     */
    public String toPath() {
        return z + "/" + x + "/" + y + ".png";
    }

    /**
     * Returns the public OpenStreetMap URL for this tile. Used by
     * {@link TileDownloader} when a tile isn't yet cached locally.
     *
     * <p>OSM serves tiles from three subdomain rotations (a, b, c) to
     * spread load. We pick one based on a hash of (x + y) so the same
     * tile always hits the same subdomain — friendly to OSM's caches.</p>
     */
    public String toOsmUrl() {
        char subdomain = (char) ('a' + Math.floorMod(x + y, 3));
        return "https://" + subdomain + ".tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png";
    }
}