package com.example.map_logic;

public class MapHtmlProvider {
    public static String getMapHTML(){

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body {
                    width: 100vw;
                    height: 100vh;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                }
                #map {
                    width: 100vw;
                    height: 100vh;
                }
                
                /* Modern pulsing marker for CivicGuard */
                .pulse-marker {
                    width: 20px;
                    height: 20px;
                    background-color: #10b981;
                    border-radius: 50%;
                    border: 3px solid white;
                    box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
                    animation: pulse-animation 2s infinite;
                }
                @keyframes pulse-animation {
                    0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
                    70% { box-shadow: 0 0 0 15px rgba(16, 185, 129, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
                }
                .custom-leaflet-icon {
                    background: transparent;
                    border: none;
                }
            </style>
            
            <!-- THE 2D FIX: Must be declared BEFORE Leaflet loads to bypass JavaFX bugs -->
            <script>window.L_DISABLE_3D = true;</script>
            
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        </head>
        <body>
            <div id="map"></div>
            
            <script>
                // Give JavaFX time to stretch the window before booting Leaflet
                setTimeout(function() {
                    
                    // Initialize map with animations disabled to prevent WebKit crashes
                    // Centered exactly on Argao
                var map = L.map('map', {
                    zoomControl: false,
                    dragging: false,
                    touchZoom: false,
                    scrollWheelZoom: false,
                    doubleClickZoom: false,
                    zoomAnimation: false,
                    fadeAnimation: false
                }).setView([9.8828, 123.5953], 12);
                    
                    // Load OpenStreetMap tiles
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    // Add the glowing emerald marker
                    var emeraldPulseIcon = L.divIcon({
                        className: 'custom-leaflet-icon',
                        html: '<div class="pulse-marker"></div>',
                        iconSize: [20, 20],
                        iconAnchor: [10, 10]
                    });
                    
                    var marker = L.marker([9.8828, 123.5953], { icon: emeraldPulseIcon }).addTo(map);
                    marker.bindPopup("<b>Argao Command Center</b><br>Active").openPopup();

                    // The Heartbeat: Force redraw every 500ms to catch window resizes
                    setInterval(function() {
                        map.invalidateSize(true);
                    }, 500);

                }, 500); // 500ms boot delay
            </script>
        </body>
        </html>
        """;
    }
}
