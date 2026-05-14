package com.example.map_tiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-math utilities for working with the Web Mercator tile grid.
 *
 * <p>Web Mercator is the projection OpenStreetMap (and Google Maps, and
 * pretty much every web map) uses. It maps the curved Earth onto a flat
 * square, then slices that square into a pyramid of tiles: at zoom Z
 * there are {@code 2^Z × 2^Z} tiles covering the world.</p>
 *
 * <p>This class has zero dependencies on threading, networking, or disk
 * I/O — it's pure math. That makes it trivial to unit-test and impossible
 * to break with concurrency bugs.</p>
 *
 * <p>Reference: <a href="https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames">
 * OSM Slippy Map Tilenames</a></p>
 */
public final class TileMath {

    private TileMath() { /* utility class */ }

    /**
     * Converts a latitude/longitude pair plus a zoom level into the tile
     * coordinate that contains that point. This is the standard Web
     * Mercator forward projection used by OSM and every Leaflet map.
     *
     * @param lat  latitude in degrees, in range [-85.0511, 85.0511]
     * @param lng  longitude in degrees, in range [-180, 180]
     * @param zoom zoom level (typically 0-19)
     * @return the {@link TileCoord} containing this geographic point
     */
    public static TileCoord latLngToTile(double lat, double lng, int zoom) {
        double n = Math.pow(2.0, zoom);
        int x = (int) Math.floor((lng + 180.0) / 360.0 * n);
        double latRad = Math.toRadians(lat);
        int y = (int) Math.floor(
                (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI)
                        / 2.0 * n);
        return new TileCoord(zoom, clamp(x, 0, (int) n - 1), clamp(y, 0, (int) n - 1));
    }

    /**
     * Computes every tile coordinate covering a bounding box at a single
     * zoom level. The bbox is given as two corners: south-west (minLat,
     * minLng) and north-east (maxLat, maxLng).
     *
     * <p>For example, Brgy. Lahug at zoom 15 might cover a 3×3 area:
     * this returns all 9 {@link TileCoord} values.</p>
     *
     * @param minLat south edge latitude
     * @param minLng west edge longitude
     * @param maxLat north edge latitude
     * @param maxLng east edge longitude
     * @param zoom   zoom level
     * @return list of every tile coordinate inside the bbox at this zoom
     */
    public static List<TileCoord> tilesInBbox(
            double minLat, double minLng,
            double maxLat, double maxLng,
            int zoom) {

        // Top-left corner of the bbox = (maxLat, minLng) in tile space
        TileCoord topLeft = latLngToTile(maxLat, minLng, zoom);
        // Bottom-right corner = (minLat, maxLng)
        TileCoord bottomRight = latLngToTile(minLat, maxLng, zoom);

        int minX = Math.min(topLeft.x(), bottomRight.x());
        int maxX = Math.max(topLeft.x(), bottomRight.x());
        int minY = Math.min(topLeft.y(), bottomRight.y());
        int maxY = Math.max(topLeft.y(), bottomRight.y());

        List<TileCoord> tiles = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                tiles.add(new TileCoord(zoom, x, y));
            }
        }
        return tiles;
    }

    /**
     * Computes every tile coordinate covering a bbox across a range of
     * zoom levels (inclusive on both ends).
     *
     * <p>This is the workhorse used by {@link TilePrefetchService}:
     * given a barangay's bounding box, get every tile from zoom 13
     * (regional context) through zoom 17 (street-level detail).</p>
     */
    public static List<TileCoord> tilesInBboxAtZooms(
            double minLat, double minLng,
            double maxLat, double maxLng,
            int minZoom, int maxZoom) {

        List<TileCoord> all = new ArrayList<>();
        for (int z = minZoom; z <= maxZoom; z++) {
            all.addAll(tilesInBbox(minLat, minLng, maxLat, maxLng, z));
        }
        return all;
    }

    /**
     * Expands a single (lat, lng) point into a bounding box of the given
     * radius in degrees. Useful when we only have a barangay's center
     * point and want to prefetch a reasonable area around it.
     *
     * <p>Note: 1 degree of latitude ≈ 111 km. For Cebu City, a
     * {@code radiusDegrees} of 0.02 gives roughly a 2.2 km × 2.2 km box —
     * usually plenty for one barangay.</p>
     *
     * @return a 4-element array: {@code [minLat, minLng, maxLat, maxLng]}
     */
    public static double[] bboxAround(double lat, double lng, double radiusDegrees) {
        return new double[] {
                lat - radiusDegrees,  // minLat (south)
                lng - radiusDegrees,  // minLng (west)
                lat + radiusDegrees,  // maxLat (north)
                lng + radiusDegrees   // maxLng (east)
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}