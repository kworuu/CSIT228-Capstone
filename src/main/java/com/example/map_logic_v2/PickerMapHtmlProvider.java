package com.example.map_logic_v2;

public class PickerMapHtmlProvider {

    public static String getMapHTML(double brgyLat, double brgyLng, int zoom, int tilePort) {

        // Calculate a strict bounding box around the barangay (approx 1.2km radius)
        double radius = 0.012;
        double minLat = brgyLat - radius;
        double maxLat = brgyLat + radius;
        double minLng = brgyLng - radius;
        double maxLng = brgyLng + radius;

        String html = """
        <!DOCTYPE html>
        <html>
        
        <head>
            <meta charset="utf-8" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script>var L_DISABLE_3D = true;</script>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { padding: 0; margin: 0; background-color: #0f172a; }
                html, body, #map { height: 100%%; width: 100%%; cursor: crosshair; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                // Define the invisible walls
                var bounds = [[%f, %f], [%f, %f]];
                
                // Initialize map with maxBounds to lock them in their territory
                var map = L.map('map', {
                    center: [%f, %f],
                    zoom: %d,
                    maxBounds: bounds,
                    maxBoundsViscosity: 1.0, // Hard bounce-back effect
                    minZoom: 14 // Prevent them from zooming out too far to see other areas
                });
                
                L.tileLayer('http://localhost:%d/{z}/{x}/{y}.png').addTo(map);

                var currentPin = null;

                map.on('click', function(e) {
                    var lat = e.latlng.lat;
                    var lng = e.latlng.lng;
                    
                    if (currentPin) {
                        map.removeLayer(currentPin);
                    }
                    
                    currentPin = L.marker([lat, lng]).addTo(map);
                    
                    if (window.javaBridge) {
                        window.javaBridge.setCoordinates(lat, lng);
                    }
                });
            </script>
        </body>
        </html>
        """;

        // Inject the variables safely
        return html.formatted(minLat, minLng, maxLat, maxLng, brgyLat, brgyLng, zoom, tilePort);
    }
}