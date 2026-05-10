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

    /**
     * Returns the full HTML string for the barangay WebView map.
     *
     * @param centersJson syntactically-valid JSON array of center objects (see above).
     *                    Pass {@code "[]"} for an empty map.
     */
    public static String getMapHTML(String centersJson) {
        String htmlTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
            <style>
                html, body {
                    width: 100vw; height: 100vh;
                    margin: 0; padding: 0; overflow: hidden;
                }
                #map { width: 100vw; height: 100vh; background: #f8fafc; }

                /* ── Marker styles ── */
                .marker-wrap {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    cursor: pointer;
                }

                /* Pin circle */
                .pin {
                    width: 18px; height: 18px;
                    border-radius: 50%;
                    border: 3px solid white;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.25);
                    transition: transform 0.15s ease, box-shadow 0.15s ease;
                }
                .pin-open { background: #10b981; }
                .pin-full { background: #ef4444; }

                /* Focal / selected bounce animation */
                @keyframes bounce {
                    0%,100% { transform: translateY(0); }
                    30%     { transform: translateY(-8px); }
                    60%     { transform: translateY(-4px); }
                }
                .pin-bounce { animation: bounce 0.5s ease; }

                /* Subtle pulse for open centers */
                @keyframes pulse {
                    0%   { box-shadow: 0 0 0 0 rgba(16,185,129,0.7); }
                    70%  { box-shadow: 0 0 0 12px rgba(16,185,129,0); }
                    100% { box-shadow: 0 0 0 0 rgba(16,185,129,0); }
                }
                .pin-open { animation: pulse 2.5s infinite; }

                /* Label below pin */
                .pin-label {
                    margin-top: 3px;
                    background: rgba(15,23,42,0.82);
                    color: white;
                    font-size: 10px;
                    font-family: 'Segoe UI', sans-serif;
                    font-weight: 600;
                    padding: 2px 6px;
                    border-radius: 6px;
                    white-space: nowrap;
                    pointer-events: none;
                }

                .custom-icon { background: transparent; border: none; }
            </style>

            <script>window.L_DISABLE_3D = true;</script>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        </head>
        <body>
            <div id="map"></div>

            <script>
                var centers = __CENTERS_JSON__;
                var markerMap = {};   // centerId -> L.marker
                var selectedId = null;

                setTimeout(function () {

                    var map = L.map('map', {
                        zoomControl: true,
                        dragging: true,
                        touchZoom: true,
                        scrollWheelZoom: true,
                        doubleClickZoom: true,
                        zoomAnimation: false,
                        fadeAnimation: false
                    });

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    // ── Build markers ──
                    centers.forEach(function (c) {
                        var pinClass  = (c.status === 'FULL') ? 'pin pin-full' : 'pin pin-open';
                        var shortName = c.name.length > 22 ? c.name.substring(0, 20) + '…' : c.name;
                        var html = '<div class="marker-wrap">'
                                 + '  <div id="pin-' + c.id + '" class="' + pinClass + '"></div>'
                                 + '  <div class="pin-label">' + shortName + '</div>'
                                 + '</div>';

                        var icon = L.divIcon({
                            className: 'custom-icon',
                            html: html,
                            iconSize: [120, 44],
                            iconAnchor: [60, 12]
                        });

                        var marker = L.marker([c.lat, c.lng], { icon: icon }).addTo(map);

                        marker.on('click', function () {
                            if (window.javaBridge && window.javaBridge.onMarkerClick) {
                                window.javaBridge.onMarkerClick(String(c.id));
                            }
                        });

                        markerMap[c.id] = marker;
                    });

                    // ── Fit map to all center bounds ──
                    if (centers.length > 0) {
                        var latlngs = centers.map(function(c){ return [c.lat, c.lng]; });
                        map.fitBounds(latlngs, { padding: [60, 60], maxZoom: 15 });
                    } else {
                        // Default to Cebu City if no centers loaded
                        map.setView([10.3157, 123.8854], 12);
                    }

                    // ── Public API for Java to call ──

                    // Highlight (bounce) a specific marker — called when card is clicked
                    window.highlightMarker = function(centerId) {
                        var pin = document.getElementById('pin-' + centerId);
                        if (!pin) return;
                        pin.classList.remove('pin-bounce');
                        // Force reflow to restart animation
                        void pin.offsetWidth;
                        pin.classList.add('pin-bounce');
                        // Pan map to that marker
                        var m = markerMap[centerId];
                        if (m) map.panTo(m.getLatLng(), { animate: true, duration: 0.5 });
                    };

                    // Heartbeat to recover from JavaFX resize quirks
                    setInterval(function () { map.invalidateSize(true); }, 500);

                }, 500);
            </script>
        </body>
        </html>
        """;

        return htmlTemplate.replace("__CENTERS_JSON__", centersJson);
    }
}