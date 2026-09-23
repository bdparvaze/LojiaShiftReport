package com.lojia.shiftreport.settings

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.PrimaryIndigo
import com.lojia.shiftreport.ui.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapsLocationPickerModal(
    initialAddress: String = "",
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    onLocationConfirmed: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Default to Dhaka / NYC / Initial coordinates if zero
    val defaultLat = if (initialLat != 0.0) initialLat else 23.8103
    val defaultLng = if (initialLng != 0.0) initialLng else 90.4125

    var addressText by remember { mutableStateOf(initialAddress) }
    var currentLat by remember { mutableDoubleStateOf(defaultLat) }
    var currentLng by remember { mutableDoubleStateOf(defaultLng) }
    var isGeocoding by remember { mutableStateOf(false) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Helper for Reverse Geocoding
    fun performReverseGeocode(lat: Double, lng: Double) {
        coroutineScope.launch {
            isGeocoding = true
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    val addrObj = addresses[0]
                    val sb = StringBuilder()
                    for (i in 0..addrObj.maxAddressLineIndex) {
                        if (i > 0) sb.append(", ")
                        sb.append(addrObj.getAddressLine(i))
                    }
                    val full = sb.toString()
                    if (full.isNotBlank()) {
                        addressText = full
                    }
                }
            } catch (e: Exception) {
                // Ignore geocode errors gracefully
            } finally {
                isGeocoding = false
            }
        }
    }

    // Helper for Forward Geocoding
    fun searchAddressCoordinates(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch {
            isGeocoding = true
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    currentLat = addr.latitude
                    currentLng = addr.longitude
                    statusMessage = "Found coordinates for query"
                } else {
                    statusMessage = "No location found for this address"
                }
            } catch (e: Exception) {
                statusMessage = "Unable to search map address"
            } finally {
                isGeocoding = false
            }
        }
    }

    // Helper to request live GPS location
    fun detectLiveGpsLocation() {
        isDetectingGps = true
        statusMessage = "Detecting live GPS location..."
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                @Suppress("MissingPermission")
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                currentLat = bestLocation.latitude
                currentLng = bestLocation.longitude
                statusMessage = "GPS Location captured!"
                performReverseGeocode(currentLat, currentLng)
            } else {
                statusMessage = "GPS unavailable. Drag map pin to select location."
            }
        } catch (e: Exception) {
            statusMessage = "GPS access error. Please select manually on map."
        } finally {
            isDetectingGps = false
        }
    }

    // Function to launch external Google Maps App or Web Browser
    fun openGoogleMapsApp() {
        val uri = Uri.parse("geo:$currentLat,$currentLng?q=$currentLat,$currentLng(${Uri.encode(addressText.ifBlank { "Business Location" })})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://maps.google.com/?q=$currentLat,$currentLng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEA4335).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.google_maps_location),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Map View Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                // Calculate offset shift on map canvas
                                val deltaLat = (0.5f - offset.y / 500f) * 0.05
                                val deltaLng = (offset.x / 500f - 0.5f) * 0.05
                                currentLat = (currentLat + deltaLat).coerceIn(-90.0, 90.0)
                                currentLng = (currentLng + deltaLng).coerceIn(-180.0, 180.0)
                                performReverseGeocode(currentLat, currentLng)
                            }
                        }
                ) {
                    // Stylized Map Canvas Background with Grid & Pin
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Map Grid Lines
                        val step = 30f
                        var x = 0f
                        while (x < width) {
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < height) {
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }

                        // Center Location Pin Indicator
                        val centerX = width / 2f
                        val centerY = height / 2f

                        // Pulse Ring
                        drawCircle(
                            color = Color(0x33EA4335),
                            radius = 28f,
                            center = Offset(centerX, centerY)
                        )

                        // Red Pin Icon Path
                        drawCircle(
                            color = Color(0xFFEA4335),
                            radius = 12f,
                            center = Offset(centerX, centerY - 10f)
                        )
                        drawCircle(
                            color = PureWhite,
                            radius = 4f,
                            center = Offset(centerX, centerY - 10f)
                        )
                        val pinPath = Path().apply {
                            moveTo(centerX - 8f, centerY - 8f)
                            lineTo(centerX + 8f, centerY - 8f)
                            lineTo(centerX, centerY + 8f)
                            close()
                        }
                        drawPath(path = pinPath, color = Color(0xFFEA4335))
                    }

                    // Map Overlay Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = PureWhite.copy(alpha = 0.92f),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                            Text("Tap map to position marker", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                        }
                    }

                    // Open Google Maps App Floating Button
                    IconButton(
                        onClick = { openGoogleMapsApp() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(PureWhite, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Map, contentDescription = "Open in Google Maps", tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
                    }
                }

                // Coordinates Display Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(stringResource(R.string.latitude_label), fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(String.format(Locale.US, "%.6f", currentLat), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(stringResource(R.string.longitude_label), fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(String.format(Locale.US, "%.6f", currentLng), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                    }
                }

                // Action Buttons Row: Detect GPS & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { detectLiveGpsLocation() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isDetectingGps
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.detect_live_gps), fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { openGoogleMapsApp() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.open_in_google_maps), fontSize = 12.sp)
                    }
                }

                // Address Input Field with Search Button
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    label = { Text(stringResource(R.string.address)) },
                    placeholder = { Text("e.g. 123 Commercial St, Downtown") },
                    trailingIcon = {
                        IconButton(onClick = { searchAddressCoordinates(addressText) }) {
                            if (isGeocoding) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = "Search Address")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_address_input")
                )

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage ?: "",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLocationConfirmed(addressText.trim(), currentLat, currentLng)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_location_button")
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.confirm_location))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(16.dp)
    )
}
