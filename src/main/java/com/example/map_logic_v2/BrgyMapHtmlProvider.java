package com.example.map_logic_v2;

/**
 * Generates the Leaflet map HTML for the Barangay dashboard.
 *
 * <p>Unlike the admin/kiosk map, this one:</p>
 * <ul>
 *   <li>Accepts a JSON array of centers with lat/lng and fits the map bounds
 *       automatically so the view is always zoomed to exactly those centers.</li>
 *   <li>Enables dragging and scroll-wheel zoom (staff are at a desk, not a kiosk).</li>
 *   <li>Calls {@code window.javaBridge.onMarkerClick(centerId)} on marker click,
 *       which the controller intercepts to show the overlay panel.</li>
 *   <li>Exposes {@code window.highlightMarker(centerId)} so the controller can
 *       bounce a marker when a card is clicked from the list below the map.</li>
 * </ul>
 *
 * <p>JSON shape expected per center object:</p>
 * <pre>
 * {
 *   "id":      "1",
 *   "name":    "Lahug Elementary School",
 *   "lat":     10.3439,
 *   "lng":     123.9000,
 *   "status":  "OPEN"   // "OPEN" | "FULL"
 * }
 * </pre>
 */
public class BrgyMapHtmlProvider {

    private BrgyMapHtmlProvider() {}

    // NEW: Reads the local files directly into Java memory to bypass security blocks
    private static String readLocalResource(String path) {
        try (java.io.InputStream is = BrgyMapHtmlProvider.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to read local resource: " + path);
            return null;
        }
    }

    /**
     * Returns the full HTML string for the barangay WebView map.
     *
     * @param centersJson syntactically-valid JSON array of center objects (see above).
     *                    Pass {@code "[]"} for an empty map.
     */
    // 1. UPDATE THE METHOD SIGNATURE to accept the center coordinates
    public static String getMapHTML(String centersJson, double brgyLat, double brgyLng, int zoom, int tilePort) {
        // 1. Read the raw text of the CSS and JS files
        String localCss = readLocalResource("/leaflet/leaflet.css");
        String localJs = readLocalResource("/leaflet/leaflet.js");

        String headInjection;

        // 2. INJECT INLINE: If the local files exist, inject them directly into the HTML! 
        if (localCss != null && localJs != null) {
            headInjection = "<style>\n" + localCss + "\n</style>\n" +
                            "<script>var L_DISABLE_3D = true;</script>\n" +
                            "<script>\n" + localJs + "\n</script>\n";
        } else {
            // Fallback to internet just in case you haven't clicked "Rebuild Project" yet
            headInjection = "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                            "<script>var L_DISABLE_3D = true;</script>\n" +
                            "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n";
        }

        // 3. Build the HTML Template
        String htmlTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            __HEAD_INJECTION__
            <style>
                body { padding: 0; margin: 0; background-color: #0f172a; }
                html, body, #map { height: 100%; width: 100%; }
                
                .marker-wrap { 
                    position: relative; width: 140px; height: 60px; 
                    display: flex; flex-direction: column; align-items: center; 
                }
                
                /* The Pin Container */
                .pin-container { position: relative; width: 36px; height: 36px; }

                /* The SVG Pin */
                .custom-pin {
                    width: 36px; height: 36px; color: #10b981; /* Emergency Green */
                    filter: drop-shadow(0px 4px 4px rgba(0,0,0,0.5));
                    position: relative; z-index: 5;
                }

                /* The Pinging Radar Effect */
                .pulse {
                    position: absolute; top: 0; left: 0;
                    width: 36px; height: 36px;
                    border-radius: 50%;
                    background: #10b981;
                    opacity: 0.6;
                    z-index: 1;
                    animation: radar 2s infinite;
                }

                @keyframes radar {
                    0% { transform: scale(1); opacity: 0.6; }
                    100% { transform: scale(2.5); opacity: 0; }
                }

                .pin-label { 
                    background: rgba(15,23,42,0.95); color: white; padding: 5px 10px; 
                    border-radius: 6px; font-family: 'Segoe UI', sans-serif; font-size: 11px; font-weight: 600;
                    white-space: nowrap; margin-top: 4px; border: 1px solid #334155; 
                }
                
                .pin-bounce { animation: bounce 0.5s ease; }
                @keyframes bounce { 
                    0%, 100% { transform: translateY(0); } 
                    50% { transform: translateY(-12px); } 
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var centers = __CENTERS_JSON__;
                var markerMap = {};   

                // --- NEW: PHASE 1 PINNING LOGIC ---
                var isPinning = false;

                window.enablePinningMode = function() {
                    isPinning = true;
                    // Force the cursor to a crosshair for both the div and Leaflet container
                    document.getElementById('map').style.cursor = 'crosshair';
                    document.querySelector('.leaflet-container').style.cursor = 'crosshair';
                };

                setTimeout(function () {
                    var brgyLat = __BRGY_LAT__;
                    var brgyLng = __BRGY_LNG__;
                    var brgyZoom = __BRGY_ZOOM__;
                    var homeLatLng = L.latLng(brgyLat, brgyLng); // Save origin point
    
                    // Notice: maxBounds is completely REMOVED so they can scroll anywhere!
                    var map = L.map('map', {
                        center: [brgyLat, brgyLng],
                        zoom: brgyZoom,
                        minZoom: 10, // Allow zooming out further now
                        maxZoom: 17
                    });

                    L.tileLayer('http://localhost:__TILE_PORT__/{z}/{x}/{y}.png').addTo(map);

                    // --- NEW: MAP CLICK LISTENER ---
                    map.on('click', function(e) {
                        if (isPinning) {
                            isPinning = false; // Turn off pinning mode
                            document.getElementById('map').style.cursor = ''; // Reset cursor
                            document.querySelector('.leaflet-container').style.cursor = '';
                            
                            // Send the coordinates back to Java!
                            if (window.javaBridge) {
                                window.javaBridge.onMapClicked(e.latlng.lat, e.latlng.lng);
                            }
                        }
                    });

                    // --- NEW: DISTANCE TRACKER ---
                    var thresholdMeters = 1500; // 1.5 Kilometers
                    map.on('move', function() {
                        var currentCenter = map.getCenter();
                        var dist = map.distance(homeLatLng, currentCenter);
                        
                        // Tell Java to show button if distance > 1500m, hide if closer
                        if (window.javaBridge) {
                            window.javaBridge.toggleHomeButton(dist > thresholdMeters);
                        }
                    });

                    // --- NEW: FLY HOME COMMAND ---
                    window.flyHome = function() {
                        map.flyTo(homeLatLng, brgyZoom, { duration: 1.5 }); // Smooth flight animation
                    };

                    centers.forEach(function (c) {
                        var shortName = c.name.length > 22 ? c.name.substring(0, 20) + '…' : c.name;
                        
                        var svgIcon = '<div class="pin-container">' +
                                      '  <div class="pulse"></div>' +
                                      '  <svg class="custom-pin" viewBox="0 0 24 36" xmlns="http://www.w3.org/2000/svg">' +
                                      '    <path d="M12 0C5.373 0 0 5.373 0 12c0 8.438 11.125 23.336 11.535 23.893a.596.596 0 0 0 .93 0C12.875 35.336 24 20.438 24 12c0-6.627-5.373-12-12-12z" fill="currentColor" />' +
                                      '    <path d="M12 5L7 9v6h3v-4h4v4h3V9l-5-4z" fill="#ffffff" />' + 
                                      '  </svg>' +
                                      '</div>';

                        var html = '<div class="marker-wrap">'
                                 + '  <div id="pin-' + c.id + '">' + svgIcon + '</div>'
                                 + '</div>';

                        var icon = L.divIcon({ className: 'custom-icon', html: html, iconSize: [140, 60], iconAnchor: [70, 36] });
                        var marker = L.marker([c.lat, c.lng], { icon: icon }).addTo(map);

                        marker.on('click', function () {
                            if (window.javaBridge) window.javaBridge.onMarkerClick(String(c.id));
                        });
                        markerMap[c.id] = marker;
                    });

                    window.highlightMarker = function(id) {
                        var p = document.getElementById('pin-' + id);
                        if(p){ p.classList.remove('pin-bounce'); void p.offsetWidth; p.classList.add('pin-bounce'); }
                        if(markerMap[id]) map.panTo(markerMap[id].getLatLng());
                    };
                }, 500);
            </script>
        </body>
        </html>
        """;
        return htmlTemplate
                .replace("__HEAD_INJECTION__", headInjection)
                .replace("__CENTERS_JSON__", centersJson)
                .replace("__BRGY_LAT__", String.valueOf(brgyLat))
                .replace("__BRGY_LNG__", String.valueOf(brgyLng))
                .replace("__BRGY_ZOOM__", String.valueOf(zoom))
                .replace("__TILE_PORT__", String.valueOf(tilePort));
    }

    public static String getCityMapHTML(String centersJson,
                                        double swLat, double swLng,
                                        double neLat, double neLng,
                                        double centerLat, double centerLng,
                                        int maxZoom, int tilePort) {
        // 1. Read the raw text of the CSS and JS files
        String localCss = readLocalResource("/leaflet/leaflet.css");
        String localJs = readLocalResource("/leaflet/leaflet.js");

        String headInjection;

        // 2. INJECT INLINE: If the local files exist, inject them directly into the HTML! 
        if (localCss != null && localJs != null) {
            headInjection = "<style>\n" + localCss + "\n</style>\n" +
                            "<script>var L_DISABLE_3D = true;</script>\n" +
                            "<script>\n" + localJs + "\n</script>\n";
        } else {
            // Fallback to internet just in case you haven't clicked "Rebuild Project" yet
            headInjection = "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                            "<script>var L_DISABLE_3D = true;</script>\n" +
                            "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n";
        }
                                            
        String htmlTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            __HEAD_INJECTION__
            <style>
                body { padding: 0; margin: 0; background-color: #0f172a; }
                html, body, #map { height: 100%; width: 100%; }
                
                .marker-wrap { 
                    position: relative; width: 140px; height: 60px; 
                    display: flex; flex-direction: column; align-items: center; 
                }
                
                .pin-container { position: relative; width: 36px; height: 36px; }

                .custom-pin {
                    width: 36px; height: 36px; color: #10b981;
                    filter: drop-shadow(0px 4px 4px rgba(0,0,0,0.5));
                    position: relative; z-index: 5;
                }

                .pulse {
                    position: absolute; top: 0; left: 0;
                    width: 36px; height: 36px;
                    border-radius: 50%;
                    background: #10b981;
                    opacity: 0.6;
                    z-index: 1;
                    animation: radar 2s infinite;
                }

                @keyframes radar {
                    0% { transform: scale(1); opacity: 0.6; }
                    100% { transform: scale(2.5); opacity: 0; }
                }

                .pin-label { 
                    background: rgba(15,23,42,0.95); color: white; padding: 5px 10px; 
                    border-radius: 6px; font-family: 'Segoe UI', sans-serif; font-size: 11px; font-weight: 600;
                    white-space: nowrap; margin-top: 4px; border: 1px solid #334155; 
                }
                
                .pin-bounce { animation: bounce 0.5s ease; }
                @keyframes bounce { 
                    0%, 100% { transform: translateY(0); } 
                    50% { transform: translateY(-12px); } 
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var centers = __CENTERS_JSON__;
                var markerMap = {};   

                setTimeout(function () {
                   var southWest = L.latLng(__SW_LAT__, __SW_LNG__);
                   var northEast = L.latLng(__NE_LAT__, __NE_LNG__);
                   var bounds = L.latLngBounds(southWest, northEast);
                   
                   var homeLatLng = L.latLng(__CENTER_LAT__, __CENTER_LNG__);
                   var brgyZoom = 12;
    
                   var map = L.map('map', {
                       center: [__CENTER_LAT__, __CENTER_LNG__],
                       zoom: brgyZoom,
                       minZoom: 11,
                       maxZoom: __MAX_ZOOM__
                   });

                    L.tileLayer('http://localhost:__TILE_PORT__/{z}/{x}/{y}.png').addTo(map);

                    // --- NEW: DISTANCE TRACKER ---
                    var thresholdMeters = 3000; // 3 Kilometers
                    
                    // Add zoom distance tracking
                    var isZoomedIn = false;

                    map.on('zoomend', function() {
                         isZoomedIn = map.getZoom() > 14; 
                    });

                    map.on('move', function() {
                        var currentCenter = map.getCenter();
                        var dist = map.distance(homeLatLng, currentCenter);
                        
                        // Tell Java to show button if distance > 3000m or if it is zoomed in, hide if closer
                        if (window.javaBridge) {
                            window.javaBridge.toggleHomeButton(dist > thresholdMeters || isZoomedIn);
                        }
                    });

                    // --- NEW: FLY HOME COMMAND ---
                    window.flyHome = function() {
                        map.flyTo(homeLatLng, brgyZoom, { duration: 1.5 }); // Smooth flight animation
                    };

                    centers.forEach(function (c) {
                        var shortName = c.name.length > 22 ? c.name.substring(0, 20) + '…' : c.name;
                        
                        var svgIcon = '<div class="pin-container">' +
                                      '  <div class="pulse"></div>' +
                                      '  <svg class="custom-pin" viewBox="0 0 24 36" xmlns="http://www.w3.org/2000/svg">' +
                                      '    <path d="M12 0C5.373 0 0 5.373 0 12c0 8.438 11.125 23.336 11.535 23.893a.596.596 0 0 0 .93 0C12.875 35.336 24 20.438 24 12c0-6.627-5.373-12-12-12z" fill="currentColor" />' +
                                      '    <path d="M12 5L7 9v6h3v-4h4v4h3V9l-5-4z" fill="#ffffff" />' + 
                                      '  </svg>' +
                                      '</div>';

                        var html = '<div class="marker-wrap">'
                                 + '  <div id="pin-' + c.id + '">' + svgIcon + '</div>'
                                 + '</div>';

                        var icon = L.divIcon({ className: 'custom-icon', html: html, iconSize: [140, 60], iconAnchor: [70, 36] });
                        var marker = L.marker([c.lat, c.lng], { icon: icon }).addTo(map);

                        marker.on('click', function () {
                            if (window.javaBridge) window.javaBridge.onMarkerClick(String(c.id));
                        });
                        markerMap[c.id] = marker;
                    });

                    window.highlightMarker = function(id) {
                        var p = document.getElementById('pin-' + id);
                        if(p){ p.classList.remove('pin-bounce'); void p.offsetWidth; p.classList.add('pin-bounce'); }
                        if(markerMap[id]) map.panTo(markerMap[id].getLatLng());
                    };
                }, 500);
            </script>
        </body>
        </html>
        """;
        return htmlTemplate
                .replace("__HEAD_INJECTION__", headInjection)
                .replace("__CENTERS_JSON__", centersJson)
                .replace("__SW_LAT__", String.valueOf(swLat))
                .replace("__SW_LNG__", String.valueOf(swLng))
                .replace("__NE_LAT__", String.valueOf(neLat))
                .replace("__NE_LNG__", String.valueOf(neLng))
                .replace("__CENTER_LAT__", String.valueOf(centerLat))
                .replace("__CENTER_LNG__", String.valueOf(centerLng))
                .replace("__MAX_ZOOM__", String.valueOf(maxZoom))
                .replace("__TILE_PORT__", String.valueOf(tilePort));
    }
}
