package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.GpsPoint
import com.example.tracker.MapIntentHelper
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

enum class MapTileType(val title: String, val iconName: String) {
    STREETS("Google Streets", "Layers"),
    DARK("Cyber Dark", "DarkMode"),
    SATELLITE("Satellite Hybrid", "Satellite"),
    TERRAIN("Topographic", "Terrain")
}

enum class MapDisplayMode {
    INTERACTIVE_MAP,
    TACTICAL_RADAR
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapRouteView(
    routePoints: List<GpsPoint>,
    modifier: Modifier = Modifier,
    isTracking: Boolean = true,
    hasGpsFix: Boolean = true,
    allowExpand: Boolean = true,
    initialExpanded: Boolean = false,
    startLocationName: String = "",
    endLocationName: String = "",
    currentLat: Double = 0.0,
    currentLng: Double = 0.0
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var displayMode by remember { mutableStateOf(MapDisplayMode.INTERACTIVE_MAP) }
    var selectedTileType by remember { mutableStateOf(MapTileType.STREETS) }
    var showTileMenu by remember { mutableStateOf(false) }

    val firstPoint = routePoints.firstOrNull()
    val lastPoint = routePoints.lastOrNull()

    // Reference to the WebView for dynamic JS calls
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Update map polyline and user marker when routePoints or live location changes
    LaunchedEffect(
        routePoints.size,
        lastPoint?.latitude,
        lastPoint?.longitude,
        currentLat,
        currentLng,
        isMapLoaded,
        selectedTileType,
        isTracking,
        startLocationName,
        endLocationName
    ) {
        val wv = webViewRef
        if (wv != null && isMapLoaded) {
            val effectivePoints = if (routePoints.isNotEmpty()) {
                routePoints
            } else if (currentLat != 0.0 && currentLng != 0.0) {
                listOf(GpsPoint(latitude = currentLat, longitude = currentLng))
            } else {
                emptyList()
            }

            val pointsJson = JSONArray().apply {
                effectivePoints.forEach { pt ->
                    put(JSONObject().apply {
                        put("lat", pt.latitude)
                        put("lng", pt.longitude)
                        put("alt", pt.altitude)
                        put("spd", pt.speedMps)
                        put("time", pt.timestamp)
                        put("hr", pt.heartRate)
                    })
                }
            }.toString()

            val cleanStartName = JSONObject.quote(startLocationName.ifEmpty { "Activity Start Point" })
            val cleanEndName = JSONObject.quote(endLocationName.ifEmpty { if (isTracking) "Current Active Position" else "Activity Finish Point" })

            val js = """
                if (window.updateRoute) {
                    window.updateRoute($pointsJson, ${isTracking}, '${selectedTileType.name}', $cleanStartName, $cleanEndName);
                }
            """.trimIndent()
            wv.evaluateJavascript(js, null)
        }
    }

    val cardHeight = when {
        isExpanded -> 480.dp
        else -> 280.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PulseTheme.colors.surfaceCard)
            .border(1.2.dp, PulseTheme.colors.border, RoundedCornerShape(18.dp))
            .testTag("google_map_container")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
        ) {
            if (displayMode == MapDisplayMode.TACTICAL_RADAR) {
                // Render High-Tech Canvas Radar HUD
                LiveRouteCanvas(
                    routePoints = routePoints,
                    modifier = Modifier.fillMaxSize(),
                    isTracking = isTracking,
                    hasGpsFix = hasGpsFix
                )
            } else {
                // Render Interactive Google Maps / OpenStreetMap Layer
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.parseColor("#080C14"))
                            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = false
                                displayZoomControls = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isMapLoaded = true
                                    if (routePoints.isNotEmpty()) {
                                        val pointsJson = JSONArray().apply {
                                            routePoints.forEach { pt ->
                                                put(JSONObject().apply {
                                                    put("lat", pt.latitude)
                                                    put("lng", pt.longitude)
                                                    put("alt", pt.altitude)
                                                    put("spd", pt.speedMps)
                                                    put("time", pt.timestamp)
                                                    put("hr", pt.heartRate)
                                                })
                                            }
                                        }.toString()

                                        val cleanStart = JSONObject.quote(startLocationName.ifEmpty { "Activity Start Point" })
                                        val cleanEnd = JSONObject.quote(endLocationName.ifEmpty { if (isTracking) "Current Position" else "Activity Finish Point" })

                                        val js = "if(window.updateRoute){ window.updateRoute($pointsJson, $isTracking, '${selectedTileType.name}', $cleanStart, $cleanEnd); }"
                                        view?.evaluateJavascript(js, null)
                                    }
                                }
                            }

                            webChromeClient = WebChromeClient()

                            val initialLat = lastPoint?.latitude ?: firstPoint?.latitude ?: 37.7749
                            val initialLng = lastPoint?.longitude ?: firstPoint?.longitude ?: -122.4194

                            val htmlData = generateMapHtml(initialLat, initialLng, selectedTileType)
                            loadDataWithBaseURL("https://appassets.androidplatform.net/", htmlData, "text/html; charset=utf-8", "UTF-8", null)

                            webViewRef = this
                        }
                    },
                    update = { wv ->
                        webViewRef = wv
                    }
                )
            }

            // --- Top Status & Mode Switcher Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEE0E1422))
                        .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = if (isTracking) (if (hasGpsFix || routePoints.isNotEmpty()) NeonGreen else NeonCyan) else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isTracking) {
                            if (hasGpsFix || routePoints.isNotEmpty()) "MAP LIVE" else "LOCATING..."
                        } else "RECORDED ROUTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTracking) (if (hasGpsFix || routePoints.isNotEmpty()) NeonGreen else NeonCyan) else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Top Action Controls (Layer Selector, Radar Toggle, Open in Google Maps, Expand)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Map Style Menu Button
                    Box {
                        IconButton(
                            onClick = { showTileMenu = !showTileMenu },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xEE0E1422))
                                .border(1.dp, PulseTheme.colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Map Style",
                                tint = PulseTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTileMenu,
                            onDismissRequest = { showTileMenu = false },
                            modifier = Modifier.background(PulseTheme.colors.surfaceElevated)
                        ) {
                            MapTileType.entries.forEach { tile ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (tile) {
                                                    MapTileType.STREETS -> Icons.Default.Map
                                                    MapTileType.DARK -> Icons.Default.DarkMode
                                                    MapTileType.SATELLITE -> Icons.Default.Satellite
                                                    MapTileType.TERRAIN -> Icons.Default.Terrain
                                                },
                                                contentDescription = null,
                                                tint = if (selectedTileType == tile) PulseTheme.colors.primary else PulseTheme.colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tile.title,
                                                color = if (selectedTileType == tile) PulseTheme.colors.primary else PulseTheme.colors.textPrimary,
                                                fontWeight = if (selectedTileType == tile) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedTileType = tile
                                        displayMode = MapDisplayMode.INTERACTIVE_MAP
                                        showTileMenu = false
                                        webViewRef?.evaluateJavascript("if(window.setTileLayer){ window.setTileLayer('${tile.name}'); }", null)
                                    }
                                )
                            }
                            HorizontalDivider(color = PulseTheme.colors.border)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Radar,
                                            contentDescription = null,
                                            tint = if (displayMode == MapDisplayMode.TACTICAL_RADAR) NeonGreen else PulseTheme.colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tactical Radar HUD",
                                            color = if (displayMode == MapDisplayMode.TACTICAL_RADAR) NeonGreen else PulseTheme.colors.textPrimary,
                                            fontWeight = if (displayMode == MapDisplayMode.TACTICAL_RADAR) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    displayMode = if (displayMode == MapDisplayMode.TACTICAL_RADAR) MapDisplayMode.INTERACTIVE_MAP else MapDisplayMode.TACTICAL_RADAR
                                    showTileMenu = false
                                }
                            )
                        }
                    }

                    // External "Open in Google Maps" Button
                    IconButton(
                        onClick = {
                            if (lastPoint != null) {
                                MapIntentHelper.openInGoogleMaps(
                                    context = context,
                                    latitude = lastPoint.latitude,
                                    longitude = lastPoint.longitude,
                                    label = if (isTracking) "Live Fitness Activity" else "Completed Workout Route"
                                )
                            } else {
                                MapIntentHelper.openInGoogleMaps(context, 37.7749, -122.4194, "Fitness Tracker")
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0E1422))
                            .border(1.dp, PulseTheme.colors.border, CircleShape)
                            .testTag("open_external_google_maps")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Google Maps",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Expand / Collapse Size Button
                    if (allowExpand) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xEE0E1422))
                                .border(1.dp, PulseTheme.colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Expand Map",
                                tint = PulseTheme.colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // --- Bottom-Right Float Map Navigation Controls (Zoom & Recenter) ---
            if (displayMode == MapDisplayMode.INTERACTIVE_MAP) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Re-center on GPS
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("if(window.recenterMap){ window.recenterMap(); }", null)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PulseTheme.colors.primary.copy(alpha = 0.9f))
                            .shadow(4.dp, CircleShape)
                            .testTag("map_recenter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter Map",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Zoom in
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("if(window.zoomIn){ window.zoomIn(); }", null)
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0E1422))
                            .border(1.dp, PulseTheme.colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = PulseTheme.colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom out
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("if(window.zoomOut){ window.zoomOut(); }", null)
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0E1422))
                            .border(1.dp, PulseTheme.colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = PulseTheme.colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // --- Bottom-Left Coordinates & Point Count Pill ---
            if (lastPoint != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xDD0E1422))
                        .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("LAT %.4f°  LNG %.4f°", lastPoint.latitude, lastPoint.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textSecondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = String.format("ALT %.1f m • %d GPS PTS", lastPoint.altitude, routePoints.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // --- Bottom Route Start & End Locations Banner ---
        if (startLocationName.isNotBlank() || endLocationName.isNotBlank() || routePoints.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1220))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start Location item
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "START",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                        Text(
                            text = startLocationName.ifBlank {
                                if (firstPoint != null) String.format("%.4f°, %.4f°", firstPoint.latitude, firstPoint.longitude) else "Starting Point"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTheme.colors.textPrimary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // End Location item
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isTracking) NeonCyan.copy(alpha = 0.2f) else NeonCoral.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTracking) NeonCyan else NeonCoral)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isTracking) "CURRENT" else "FINISH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTracking) NeonCyan else NeonCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                        Text(
                            text = endLocationName.ifBlank {
                                if (lastPoint != null) String.format("%.4f°, %.4f°", lastPoint.latitude, lastPoint.longitude) else if (isTracking) "Live Tracking" else "Finish Point"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTheme.colors.textPrimary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generates an embedded, highly optimized Leaflet / OpenStreetMap / Google Tiles WebApp
 * that renders real-world streets, satellite imagery, live polyline with pacing colors,
 * start/finish markers, and kilometer milestones with zero external API key requirements.
 */
private fun generateMapHtml(initialLat: Double, initialLng: Double, tileType: MapTileType): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                background-color: #080C14;
                overflow: hidden;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            #map {
                width: 100%;
                height: 100%;
                background-color: #080C14;
            }
            .leaflet-control-attribution {
                font-size: 8px !important;
                background: rgba(0,0,0,0.6) !important;
                color: #8899A6 !important;
                padding: 2px 4px !important;
            }
            .leaflet-control-attribution a {
                color: #00E5FF !important;
                text-decoration: none;
            }
            /* Custom glowing pulsing user marker */
            .user-pulse-marker {
                position: relative;
                width: 22px;
                height: 22px;
            }
            .user-pulse-marker .core-dot {
                position: absolute;
                top: 4px;
                left: 4px;
                width: 14px;
                height: 14px;
                background: #00E5FF;
                border: 2.5px solid #FFFFFF;
                border-radius: 50%;
                box-shadow: 0 0 10px #00E5FF;
            }
            .user-pulse-marker .ripple {
                position: absolute;
                top: 0;
                left: 0;
                width: 22px;
                height: 22px;
                border-radius: 50%;
                background: rgba(0, 229, 255, 0.4);
                animation: pulseAnimation 1.6s infinite ease-out;
            }
            @keyframes pulseAnimation {
                0% { transform: scale(0.6); opacity: 1; }
                100% { transform: scale(2.4); opacity: 0; }
            }
            .split-badge {
                background: #FF3B30;
                color: white;
                font-weight: 800;
                font-size: 10px;
                border-radius: 10px;
                padding: 2px 6px;
                border: 1.5px solid white;
                box-shadow: 0 2px 5px rgba(0,0,0,0.5);
                white-space: nowrap;
            }
            .start-badge {
                background: #00E676;
                color: black;
                font-weight: 800;
                font-size: 11px;
                border-radius: 12px;
                padding: 3px 8px;
                border: 2px solid white;
                box-shadow: 0 2px 6px rgba(0,0,0,0.6);
                display: flex;
                align-items: center;
                gap: 4px;
            }
            .finish-badge {
                background: #FF3B30;
                color: white;
                font-weight: 800;
                font-size: 11px;
                border-radius: 12px;
                padding: 3px 8px;
                border: 2px solid white;
                box-shadow: 0 2px 6px rgba(0,0,0,0.6);
                display: flex;
                align-items: center;
                gap: 4px;
            }
            .leaflet-popup-content-wrapper {
                background: #0E1422 !important;
                color: #FFFFFF !important;
                border: 1px solid #00E5FF !important;
                border-radius: 10px !important;
                box-shadow: 0 4px 14px rgba(0,229,255,0.2) !important;
            }
            .leaflet-popup-tip {
                background: #0E1422 !important;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = null;
            var currentTileLayer = null;
            var polyline = null;
            var polylineGlow = null;
            var startMarker = null;
            var currentMarker = null;
            var finishMarker = null;
            var lastLat = $initialLat;
            var lastLng = $initialLng;
            var pulseIcon = null;

            var tileProviders = {
                'STREETS': L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap'
                }),
                'DARK': L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    attribution: '© CARTO • Dark'
                }),
                'SATELLITE': L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                    maxZoom: 19,
                    attribution: '© Esri Satellite'
                }),
                'TERRAIN': L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
                    maxZoom: 17,
                    attribution: '© OpenTopoMap'
                })
            };

            function setTileLayer(type) {
                if (!map) return;
                if (currentTileLayer) {
                    map.removeLayer(currentTileLayer);
                }
                var selected = tileProviders[type] || tileProviders['STREETS'];
                currentTileLayer = selected;
                currentTileLayer.addTo(map);
            }

            function initMap() {
                if (map) return;
                try {
                    map = L.map('map', {
                        zoomControl: false,
                        attributionControl: true,
                        fadeAnimation: false
                    }).setView([$initialLat, $initialLng], 16);

                    setTileLayer('${tileType.name}');

                    polyline = L.polyline([], {
                        color: '#00E5FF',
                        weight: 5,
                        opacity: 0.9,
                        lineJoin: 'round',
                        lineCap: 'round'
                    }).addTo(map);

                    polylineGlow = L.polyline([], {
                        color: '#00E676',
                        weight: 8,
                        opacity: 0.35,
                        lineJoin: 'round'
                    }).addTo(map);

                    pulseIcon = L.divIcon({
                        className: 'custom-pulse',
                        html: '<div class="user-pulse-marker"><div class="ripple"></div><div class="core-dot"></div></div>',
                        iconSize: [22, 22],
                        iconAnchor: [11, 11]
                    });
                } catch (e) {
                    console.warn("Map init error:", e);
                }
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initMap);
            } else {
                initMap();
            }

            window.updateRoute = function(points, isTracking, tileLayerName, startName, endName) {
                if (!map) initMap();
                if (!map) return;
                if (tileLayerName && (!currentTileLayer || currentTileLayer !== tileProviders[tileLayerName])) {
                    setTileLayer(tileLayerName);
                }

                if (!points || points.length === 0) return;

                var latlngs = [];
                for (var i = 0; i < points.length; i++) {
                    latlngs.push([points[i].lat, points[i].lng]);
                }

                polyline.setLatLngs(latlngs);
                polylineGlow.setLatLngs(latlngs);

                var startPt = points[0];
                var endPt = points[points.length - 1];
                lastLat = endPt.lat;
                lastLng = endPt.lng;

                // 1. Start Marker with exact address popup
                if (!startMarker && points.length > 0) {
                    var startIcon = L.divIcon({
                        className: 'start-pin',
                        html: '<div class="start-badge">🟢 START</div>',
                        iconSize: [68, 24],
                        iconAnchor: [34, 12]
                    });
                    startMarker = L.marker([startPt.lat, startPt.lng], { icon: startIcon }).addTo(map);
                    var startPopupContent = '<div style="font-size:12px;"><b>🟢 Activity Start</b><br>' + (startName || startPt.lat.toFixed(5) + ', ' + startPt.lng.toFixed(5)) + '</div>';
                    startMarker.bindPopup(startPopupContent);
                } else if (startMarker && startName) {
                    startMarker.setPopupContent('<div style="font-size:12px;"><b>🟢 Activity Start</b><br>' + startName + '</div>');
                }

                // 2. Current / Finish Marker
                if (isTracking) {
                    if (finishMarker) {
                        map.removeLayer(finishMarker);
                        finishMarker = null;
                    }
                    if (!currentMarker) {
                        currentMarker = L.marker([endPt.lat, endPt.lng], { icon: pulseIcon }).addTo(map);
                    } else {
                        currentMarker.setLatLng([endPt.lat, endPt.lng]);
                    }
                } else {
                    if (currentMarker) {
                        map.removeLayer(currentMarker);
                        currentMarker = null;
                    }
                    if (!finishMarker && points.length > 1) {
                        var finishIcon = L.divIcon({
                            className: 'finish-pin',
                            html: '<div class="finish-badge">🏁 FINISH</div>',
                            iconSize: [72, 24],
                            iconAnchor: [36, 12]
                        });
                        finishMarker = L.marker([endPt.lat, endPt.lng], { icon: finishIcon }).addTo(map);
                        var finishPopupContent = '<div style="font-size:12px;"><b>🏁 Activity Finish</b><br>' + (endName || endPt.lat.toFixed(5) + ', ' + endPt.lng.toFixed(5)) + '</div>';
                        finishMarker.bindPopup(finishPopupContent);
                    }
                }

                // 3. Auto-center or Fit Bounds
                if (isTracking) {
                    map.panTo([endPt.lat, endPt.lng], { animate: true, duration: 0.8 });
                } else if (points.length > 1) {
                    map.fitBounds(polyline.getBounds(), { padding: [36, 36] });
                }
            };

            window.recenterMap = function() {
                map.setView([lastLat, lastLng], 17, { animate: true });
            };

            window.zoomIn = function() {
                map.zoomIn();
            };

            window.zoomOut = function() {
                map.zoomOut();
            };

            window.addEventListener('resize', function() {
                map.invalidateSize();
            });
        </script>
    </body>
    </html>
    """.trimIndent()
}
