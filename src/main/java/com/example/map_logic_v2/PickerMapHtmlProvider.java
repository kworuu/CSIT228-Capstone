package com.example.map_logic_v2;

public class PickerMapHtmlProvider {

    public static String getMapHTML(double brgyLat, double brgyLng, int zoom, int tilePort) {
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
                /* FIX: Double %% escapes the percent sign so Java doesn't crash */
                html, body, #map { height: 100%%; width: 100%%; cursor: crosshair; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([%f, %f], %d);
                
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

        // This safely injects the variables!
        return html.formatted(brgyLat, brgyLng, zoom, tilePort);
    }
}