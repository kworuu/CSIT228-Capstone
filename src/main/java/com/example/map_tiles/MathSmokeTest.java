package com.example.map_tiles;

public class MathSmokeTest {
    public static void main(String[] args) {
        double lahugLat = 10.334;
        double lahugLng = 123.895;

        // 1. Single Point
        TileCoord tile  = TileMath.latLngToTile(lahugLat, lahugLng, 15);
    }
}
