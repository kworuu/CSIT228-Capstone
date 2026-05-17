package com.example.map_logic_v2;

public class BrgyMapHtmlProvider {

    private BrgyMapHtmlProvider() {}

    public static String getMapHTML(String centersJson, double brgyLat, double brgyLng, int zoom, int tilePort) {
        String htmlTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css" />
            <script>var L_DISABLE_3D = true;</script>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js"></script>
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
                    0%   { transform: scale(1);   opacity: 0.6; }
                    100% { transform: scale(2.5); opacity: 0; }
                }
                .pin-label {
                    background: rgba(15,23,42,0.95); color: white; padding: 5px 10px;
                    border-radius: 6px; font-family: 'Segoe UI', sans-serif;
                    font-size: 11px; font-weight: 600;
                    white-space: nowrap; margin-top: 4px; border: 1px solid #334155;
                }
                .pin-bounce { animation: bounce 0.5s ease; }
                @keyframes bounce {
                    0%, 100% { transform: translateY(0); }
                    50%       { transform: translateY(-12px); }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var centers    = __CENTERS_JSON__;
                var markerMap  = {};
                var centersById = {};
                centers.forEach(function(c) { centersById[c.id] = c; });

                var isPinning = false;
                window.enablePinningMode = function() {
                    isPinning = true;
                    document.getElementById('map').style.cursor = 'crosshair';
                    document.querySelector('.leaflet-container').style.cursor = 'crosshair';
                };

                // --- FIX 3: icon factory (lightweight dot at low zoom) ---
                function makeIcon(c, zoomLevel) {
                    if (zoomLevel < 15) {
                        return L.divIcon({
                            className: '',
                            html: '<div style="width:12px;height:12px;background:#10b981;'
                                + 'border-radius:50%;border:2px solid white;'
                                + 'box-shadow:0 0 4px rgba(0,0,0,0.5)"></div>',
                            iconSize: [12, 12],
                            iconAnchor: [6, 6]
                        });
                    }
                    var shortName = c.name.length > 22 ? c.name.substring(0, 20) + '\\u2026' : c.name;
                    var svgIcon = '<div class="pin-container">'
                        + '<div class="pulse"></div>'
                        + '<svg class="custom-pin" viewBox="0 0 24 36" xmlns="http://www.w3.org/2000/svg">'
                        + '<path d="M12 0C5.373 0 0 5.373 0 12c0 8.438 11.125 23.336 11.535 23.893a.596.596 0 0 0 .93 0C12.875 35.336 24 20.438 24 12c0-6.627-5.373-12-12-12z" fill="currentColor"/>'
                        + '<path d="M12 5L7 9v6h3v-4h4v4h3V9l-5-4z" fill="#ffffff"/>'
                        + '</svg></div>';
                    var html = '<div class="marker-wrap"><div id="pin-' + c.id + '">' + svgIcon + '</div></div>';
                    return L.divIcon({ className: 'custom-icon', html: html, iconSize: [140, 60], iconAnchor: [70, 36] });
                }

                setTimeout(function () {
                    var brgyLat  = __BRGY_LAT__;
                    var brgyLng  = __BRGY_LNG__;
                    var brgyZoom = __BRGY_ZOOM__;
                    var homeLatLng = L.latLng(brgyLat, brgyLng);

                    var map = L.map('map', {
                        center: [brgyLat, brgyLng],
                        zoom: brgyZoom,
                        minZoom: 10,
                        maxZoom: 17
                    });

                    // --- FIX 2: pause pulse animation while panning ---
                    map.on('movestart', function() {
                        document.querySelectorAll('.pulse').forEach(function(el) {
                            el.style.animationPlayState = 'paused';
                        });
                    });
                    map.on('moveend', function() {
                        document.querySelectorAll('.pulse').forEach(function(el) {
                            el.style.animationPlayState = 'running';
                        });
                    });

                    L.tileLayer('http://localhost:__TILE_PORT__/{z}/{x}/{y}.png').addTo(map);

                    map.on('click', function(e) {
                        if (isPinning) {
                            isPinning = false;
                            document.getElementById('map').style.cursor = '';
                            document.querySelector('.leaflet-container').style.cursor = '';
                            if (window.javaBridge) {
                                window.javaBridge.onMapClicked(e.latlng.lat, e.latlng.lng);
                            }
                        }
                    });

                    var thresholdMeters = 1500;
                    map.on('move', function() {
                        var dist = map.distance(homeLatLng, map.getCenter());
                        if (window.javaBridge) {
                            window.javaBridge.toggleHomeButton(dist > thresholdMeters);
                        }
                    });

                    window.flyHome = function() {
                        map.flyTo(homeLatLng, brgyZoom, { duration: 1.5 });
                    };

                    // --- FIX 3: swap icons when zoom changes ---
                    map.on('zoomend', function() {
                        var z = map.getZoom();
                        Object.keys(markerMap).forEach(function(id) {
                            var c = centersById[id];
                            if (c) markerMap[id].setIcon(makeIcon(c, z));
                        });
                    });

                    // --- FIX 1: cluster group ---
                    var clusterGroup = L.markerClusterGroup({
                        maxClusterRadius: 60,
                        disableClusteringAtZoom: 15
                    });

                    centers.forEach(function(c) {
                        var marker = L.marker([c.lat, c.lng], { icon: makeIcon(c, map.getZoom()) });
                        marker.on('click', function() {
                            if (window.javaBridge) window.javaBridge.onMarkerClick(String(c.id));
                        });
                        markerMap[c.id] = marker;
                        clusterGroup.addLayer(marker);
                    });
                    map.addLayer(clusterGroup);

                    // --- FIX 4: updateMarkers without reloading HTML ---
                    window.updateMarkers = function(newCenters) {
                        clusterGroup.clearLayers();
                        markerMap  = {};
                        centersById = {};
                        newCenters.forEach(function(c) { centersById[c.id] = c; });
                        newCenters.forEach(function(c) {
                            var marker = L.marker([c.lat, c.lng], { icon: makeIcon(c, map.getZoom()) });
                            marker.on('click', function() {
                                if (window.javaBridge) window.javaBridge.onMarkerClick(String(c.id));
                            });
                            markerMap[c.id] = marker;
                            clusterGroup.addLayer(marker);
                        });
                    };

                    window.highlightMarker = function(id) {
                        var p = document.getElementById('pin-' + id);
                        if (p) { p.classList.remove('pin-bounce'); void p.offsetWidth; p.classList.add('pin-bounce'); }
                        if (markerMap[id]) map.panTo(markerMap[id].getLatLng());
                    };

                }, 500);
            </script>
        </body>
        </html>
        """;
        return htmlTemplate
                .replace("__CENTERS_JSON__", centersJson)
                .replace("__BRGY_LAT__",     String.valueOf(brgyLat))
                .replace("__BRGY_LNG__",     String.valueOf(brgyLng))
                .replace("__BRGY_ZOOM__",    String.valueOf(zoom))
                .replace("__TILE_PORT__",    String.valueOf(tilePort));
    }

    public static String getCityMapHTML(String centersJson,
                                        double swLat, double swLng,
                                        double neLat, double neLng,
                                        double centerLat, double centerLng,
                                        int maxZoom, int tilePort) {
        String htmlTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css" />
            <script>var L_DISABLE_3D = true;</script>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js"></script>
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
                    0%   { transform: scale(1);   opacity: 0.6; }
                    100% { transform: scale(2.5); opacity: 0; }
                }
                .pin-label {
                    background: rgba(15,23,42,0.95); color: white; padding: 5px 10px;
                    border-radius: 6px; font-family: 'Segoe UI', sans-serif;
                    font-size: 11px; font-weight: 600;
                    white-space: nowrap; margin-top: 4px; border: 1px solid #334155;
                }
                .pin-bounce { animation: bounce 0.5s ease; }
                @keyframes bounce {
                    0%, 100% { transform: translateY(0); }
                    50%       { transform: translateY(-12px); }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var centers    = __CENTERS_JSON__;
                var markerMap  = {};
                var centersById = {};
                centers.forEach(function(c) { centersById[c.id] = c; });

                // --- FIX 3: icon factory (lightweight dot at low zoom) ---
                function makeIcon(c, zoomLevel) {
                    if (zoomLevel < 15) {
                        return L.divIcon({
                            className: '',
                            html: '<div style="width:12px;height:12px;background:#10b981;'
                                + 'border-radius:50%;border:2px solid white;'
                                + 'box-shadow:0 0 4px rgba(0,0,0,0.5)"></div>',
                            iconSize: [12, 12],
                            iconAnchor: [6, 6]
                        });
                    }
                    var shortName = c.name.length > 22 ? c.name.substring(0, 20) + '\\u2026' : c.name;
                    var svgIcon = '<div class="pin-container">'
                        + '<div class="pulse"></div>'
                        + '<svg class="custom-pin" viewBox="0 0 24 36" xmlns="http://www.w3.org/2000/svg">'
                        + '<path d="M12 0C5.373 0 0 5.373 0 12c0 8.438 11.125 23.336 11.535 23.893a.596.596 0 0 0 .93 0C12.875 35.336 24 20.438 24 12c0-6.627-5.373-12-12-12z" fill="currentColor"/>'
                        + '<path d="M12 5L7 9v6h3v-4h4v4h3V9l-5-4z" fill="#ffffff"/>'
                        + '</svg></div>';
                    var html = '<div class="marker-wrap"><div id="pin-' + c.id + '">' + svgIcon + '</div></div>';
                    return L.divIcon({ className: 'custom-icon', html: html, iconSize: [140, 60], iconAnchor: [70, 36] });
                }

                setTimeout(function () {
                    var southWest  = L.latLng(__SW_LAT__, __SW_LNG__);
                    var northEast  = L.latLng(__NE_LAT__, __NE_LNG__);
                    var homeLatLng = L.latLng(__CENTER_LAT__, __CENTER_LNG__);
                    var brgyZoom   = 12;

                    var map = L.map('map', {
                        center: [__CENTER_LAT__, __CENTER_LNG__],
                        zoom: brgyZoom,
                        minZoom: 11,
                        maxZoom: __MAX_ZOOM__
                    });

                    // --- FIX 2: pause pulse animation while panning ---
                    map.on('movestart', function() {
                        document.querySelectorAll('.pulse').forEach(function(el) {
                            el.style.animationPlayState = 'paused';
                        });
                    });
                    map.on('moveend', function() {
                        document.querySelectorAll('.pulse').forEach(function(el) {
                            el.style.animationPlayState = 'running';
                        });
                    });

                    L.tileLayer('http://localhost:__TILE_PORT__/{z}/{x}/{y}.png').addTo(map);

                    var thresholdMeters = 3000;
                    var isZoomedIn = false;

                    map.on('zoomend', function() {
                        isZoomedIn = map.getZoom() > 14;
                        // --- FIX 3: swap icons on zoom change ---
                        var z = map.getZoom();
                        Object.keys(markerMap).forEach(function(id) {
                            var c = centersById[id];
                            if (c) markerMap[id].setIcon(makeIcon(c, z));
                        });
                    });

                    map.on('move', function() {
                        var dist = map.distance(homeLatLng, map.getCenter());
                        if (window.javaBridge) {
                            window.javaBridge.toggleHomeButton(dist > thresholdMeters || isZoomedIn);
                        }
                    });

                    window.flyHome = function() {
                        map.flyTo(homeLatLng, brgyZoom, { duration: 1.5 });
                    };

                    // --- FIX 1: cluster group ---
                    var clusterGroup = L.markerClusterGroup({
                        maxClusterRadius: 60,
                        disableClusteringAtZoom: 15
                    });

                    centers.forEach(function(c) {
                        var marker = L.marker([c.lat, c.lng], { icon: makeIcon(c, map.getZoom()) });
                        marker.on('click', function() {
                            if (window.javaBridge) window.javaBridge.onMarkerClick(String(c.id));
                        });
                        markerMap[c.id] = marker;
                        clusterGroup.addLayer(marker);
                    });
                    map.addLayer(clusterGroup);

                    // --- FIX 4: updateMarkers without reloading HTML ---
                    window.updateMarkers = function(newCenters) {
                        clusterGroup.clearLayers();
                        markerMap   = {};
                        centersById = {};
                        newCenters.forEach(function(c) { centersById[c.id] = c; });
                        newCenters.forEach(function(c) {
                            var marker = L.marker([c.lat, c.lng], { icon: makeIcon(c, map.getZoom()) });
                            marker.on('click', function() {
                                if (window.javaBridge) window.javaBridge.onMarkerClick(String(c.id));
                            });
                            markerMap[c.id] = marker;
                            clusterGroup.addLayer(marker);
                        });
                    };

                    window.highlightMarker = function(id) {
                        var p = document.getElementById('pin-' + id);
                        if (p) { p.classList.remove('pin-bounce'); void p.offsetWidth; p.classList.add('pin-bounce'); }
                        if (markerMap[id]) map.panTo(markerMap[id].getLatLng());
                    };

                }, 500);
            </script>
        </body>
        </html>
        """;
        return htmlTemplate
                .replace("__CENTERS_JSON__", centersJson)
                .replace("__SW_LAT__",       String.valueOf(swLat))
                .replace("__SW_LNG__",       String.valueOf(swLng))
                .replace("__NE_LAT__",       String.valueOf(neLat))
                .replace("__NE_LNG__",       String.valueOf(neLng))
                .replace("__CENTER_LAT__",   String.valueOf(centerLat))
                .replace("__CENTER_LNG__",   String.valueOf(centerLng))
                .replace("__MAX_ZOOM__",     String.valueOf(maxZoom))
                .replace("__TILE_PORT__",    String.valueOf(tilePort));
    }
}
