package com.example.map_logic_v2;

/**
 * Generates the HTML payload loaded into the dashboard's {@code WebView}.
 *
 * <p>The map renders an evacuation-center pin for every record passed in via
 * {@link #getMapHTML(String)} and routes click events back to JavaFX through
 * the {@code window.javaBridge} object that the controller installs.</p>
 *
 * <p>Argao Command Center is treated as the focal point: it always renders
 * with a larger, glowing emerald pin and is the initial map center.</p>
 */
public class MapHtmlProvider {

    private MapHtmlProvider() { /* utility class */ }

    /**
     * Backwards-compatible no-arg version — emits a single Argao Command
     * Center pin with no click bridge. Existing callers that haven't been
     * migrated to the JSON-aware overload still work.
     */
    public static String getMapHTML() {
        return getMapHTML("[]");
    }

    /**
     * Renders the full map page.
     *
     * @param centersJson a JSON array of objects with the shape
     *                    {@code {id, title, lat, lng, status, focus}}
     *                    where {@code focus=true} marks Argao Command Center.
     *                    Pass {@code "[]"} for an empty map.
     */
    public static String getMapHTML(String centersJson) {
        String htmlTemplate = """
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
                #map { width: 100vw; height: 100vh; background: #f8fafc; }

                /* Focal pin — Argao Command Center. Larger, brighter, pulsing. */
                .pulse-marker {
                    width: 22px;
                    height: 22px;
                    background-color: #10b981;
                    border-radius: 50%;
                    border: 3px solid white;
                    box-shadow: 0 0 12px rgba(16, 185, 129, 0.6);
                    animation: pulse-animation 2s infinite;
                    cursor: pointer;
                }
                @keyframes pulse-animation {
                    0%   { box-shadow: 0 0 0 0   rgba(16, 185, 129, 0.7); }
                    70%  { box-shadow: 0 0 0 18px rgba(16, 185, 129, 0); }
                    100% { box-shadow: 0 0 0 0   rgba(16, 185, 129, 0); }
                }

                /* Secondary pins — smaller, static, color reflects status. */
                .center-pin {
                    width: 16px;
                    height: 16px;
                    border-radius: 50%;
                    border: 3px solid white;
                    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.25);
                    cursor: pointer;
                    transition: transform 0.15s ease;
                }
                .center-pin:hover { transform: scale(1.2); }
                .pin-open { background-color: #10b981; }
                .pin-full { background-color: #ef4444; }

                .custom-leaflet-icon { background: transparent; border: none; }
            </style>

            <!-- 2D mode required to bypass JavaFX WebKit 3D-transform bugs -->
            <script>window.L_DISABLE_3D = true;</script>

            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        </head>
        <body>
            <div id="map"></div>

            <script>
                // Centers list passed in from JavaFX.
                var centers = __CENTERS_JSON__;

                // Boot delay lets JavaFX finish stretching the WebView before
                // Leaflet measures the container.
                setTimeout(function() {

                    var map = L.map('map', {
                        zoomControl:      false,
                        dragging:         false,
                        touchZoom:        false,
                        scrollWheelZoom:  false,
                        doubleClickZoom:  false,
                        zoomAnimation:    false,
                        fadeAnimation:    false
                    }).setView([9.8828, 123.5953], 13);

                    // --- FIX 2: pause pulse animation while panning ---
                    // (dragging is disabled on this minimap, but these guards
                    //  are kept in case dragging is ever re-enabled)
                    map.on('movestart', function() {
                        document.querySelectorAll('.pulse-marker').forEach(function(el) {
                            el.style.animationPlayState = 'paused';
                        });
                    });
                    map.on('moveend', function() {
                        document.querySelectorAll('.pulse-marker').forEach(function(el) {
                            el.style.animationPlayState = 'running';
                        });
                    });

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    // Render each center as its own marker.
                    centers.forEach(function(c) {
                        var iconHtml, iconSize;
                        if (c.focus) {
                            iconHtml = '<div class="pulse-marker"></div>';
                            iconSize = [22, 22];
                        } else {
                            var pinClass = (c.status === 'FULL') ? 'pin-full' : 'pin-open';
                            iconHtml = '<div class="center-pin ' + pinClass + '"></div>';
                            iconSize = [16, 16];
                        }

                        var icon = L.divIcon({
                            className: 'custom-leaflet-icon',
                            html:       iconHtml,
                            iconSize:   iconSize,
                            iconAnchor: [iconSize[0] / 2, iconSize[1] / 2]
                        });

                        var marker = L.marker([c.lat, c.lng], { icon: icon }).addTo(map);

                        // Bridge clicks back to JavaFX. The controller installs
                        // window.javaBridge after the page loads.
                        marker.on('click', function() {
                            if (window.javaBridge && window.javaBridge.onMarkerClick) {
                                window.javaBridge.onMarkerClick(c.id);
                            }
                        });
                    });

                    // Heartbeat to recover from JavaFX resize quirks.
                    setInterval(function() {
                        map.invalidateSize(true);
                    }, 500);

                }, 500);
            </script>
        </body>
        </html>
        """;

        return htmlTemplate.replace("__CENTERS_JSON__", centersJson);
    }
}