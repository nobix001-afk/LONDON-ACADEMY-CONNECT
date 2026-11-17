package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.io.File
import java.util.Locale

@Composable
fun AttendanceMarkingDialog(
    viewModel: EduViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentEmployee by viewModel.currentEmployee.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()
    val distance by viewModel.currentDistanceToOffice.collectAsState()
    val lastLocation by viewModel.lastKnownLocation.collectAsState()
    val isCheckingLocation by viewModel.isCheckingLocation.collectAsState()
    val isMockLocation by viewModel.isMockLocationDetected.collectAsState()
    val capturedSelfie by viewModel.capturedSelfieBitmap.collectAsState()
    val allHolidays by viewModel.allHolidays.collectAsState()

    var selectedStatus by remember { mutableStateOf("PRESENT") }
    var locationPermissionDenied by remember { mutableStateOf(false) }

    val holidayReason = remember(officeSettings, allHolidays) {
        viewModel.getTodayHolidayReason()
    }
    val isHolidayOff = holidayReason != null

    val roleLabel = remember(currentUser, currentEmployee) {
        when (currentUser?.role) {
            UserRole.SUB_ADMIN -> "Sub-Admin"
            UserRole.STUDENT -> "Student"
            UserRole.EMPLOYEE -> currentEmployee?.department ?: "Staff"
            else -> "Staff"
        }
    }

    // Camera launcher to capture live photo
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setCapturedSelfie(bitmap)
        }
    }

    // Dedicated camera permission launcher before invoking camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePictureLauncher.launch(null)
        } else {
            viewModel.showMessage("Camera permission is required to capture live photo attendance.", isError = true)
        }
    }

    // Combined permission launcher for Location and Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationPermissionDenied = false
            viewModel.checkLocationAndDistance()
        } else {
            locationPermissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA
            )
        )
        viewModel.checkLocationAndDistance()
    }

    val allowedRadius = officeSettings?.allowedRadiusMeters ?: 10.0
    val isWithinRadius = (distance ?: 9999.0) <= allowedRadius
    val displayLat = lastLocation?.latitude ?: officeSettings?.latitude ?: 28.6139
    val displayLon = lastLocation?.longitude ?: officeSettings?.longitude ?: 77.2090

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("attendance_marking_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Google GPS & Photo Attendance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentUser?.name ?: "Member"} • $roleLabel",
                                fontSize = 11.sp,
                                color = RoyalDarkRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sunday Weekly Off Informational Banner (Attendance still allowed)
                if (isHolidayOff) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sunday_holiday_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "Sunday Weekly Off",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = holidayReason ?: "Sunday Weekly Off",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Sunday Weekly Off par bhi aap GPS & Photo Attendance Login kar sakte hain.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Google GPS Location & Radius Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWithinRadius && !locationPermissionDenied) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isWithinRadius && !locationPermissionDenied) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isWithinRadius && !locationPermissionDenied) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (locationPermissionDenied) {
                                    "Google GPS Permission Needed"
                                } else if (isWithinRadius) {
                                    "Google GPS Verified (Inside Campus)"
                                } else {
                                    "Outside Permitted Campus Radius"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isWithinRadius && !locationPermissionDenied) SuccessGreen else ErrorRed,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isCheckingLocation) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching Google GPS coordinates...", fontSize = 12.sp, color = Navy700)
                            }
                        } else {
                            val distText = distance?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
                            Text(
                                text = "Distance: ${distText}m  |  Allowed: ${allowedRadius.toInt()}m",
                                fontSize = 12.sp,
                                color = Navy700,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(Locale.US, "GPS Pin: %.5f, %.5f", displayLat, displayLon),
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if ((!isWithinRadius || locationPermissionDenied) && !isCheckingLocation) {
                            Text(
                                text = if (locationPermissionDenied) {
                                    "Please allow GPS location permission and tap Re-scan GPS."
                                } else {
                                    "Attendance cannot be marked outside the permitted office radius."
                                },
                                color = ErrorRed,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (isMockLocation) {
                            Text(
                                text = "Fake / Mock GPS detected! Live hardware Google GPS required.",
                                color = ErrorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                    viewModel.checkLocationAndDistance()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-scan GPS", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val label = Uri.encode(officeSettings?.officeName ?: "London Academy Campus")
                                    val geoUri = Uri.parse("geo:$displayLat,$displayLon?q=$displayLat,$displayLon($label)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://maps.google.com/?q=$displayLat,$displayLon")
                                        )
                                        context.startActivity(webIntent)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_open_google_maps"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Map", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Photo Capture Section
                Text(
                    text = "Live Attendance Photo (Required)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            2.dp,
                            if (capturedSelfie != null) SuccessGreen else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedSelfie != null) {
                        Image(
                            bitmap = capturedSelfie!!.asImageBitmap(),
                            contentDescription = "Live Attendance Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No Photo",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            takePictureLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.testTag("button_take_selfie"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (capturedSelfie == null) "Capture Live Photo" else "Retake Photo")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status selection
                Text(
                    text = "Select Status",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PRESENT", "HALF_DAY", "LEAVE").forEach { status ->
                        val isSelected = selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatus = status },
                            label = { Text(status.replace("_", " "), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Attendance Login Button
                val canSubmit = isWithinRadius && !locationPermissionDenied && !isMockLocation && capturedSelfie != null

                Button(
                    onClick = {
                        viewModel.submitSelfAttendance(
                            status = selectedStatus,
                            onSuccess = onDismiss
                        )
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_attendance_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !isWithinRadius || locationPermissionDenied -> "Outside ${allowedRadius.toInt()}m Campus Radius"
                            capturedSelfie == null -> "Capture Live Photo First"
                            else -> "Confirm Attendance Login (GPS + Photo)"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AttendancePhotoThumbnail(
    selfieUri: String,
    modifier: Modifier = Modifier
) {
    val file = remember(selfieUri) {
        if (selfieUri.startsWith("/")) File(selfieUri) else null
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(PrimaryBlue.copy(alpha = 0.1f))
            .border(1.5.dp, SuccessGreen, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "Attendance Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Photo Verified",
                tint = SuccessGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DailyGpsPhotoAttendanceCard(
    roleLabel: String,
    todayStatus: String?,
    checkInTime: String = "",
    checkOutTime: String = "",
    distanceMeters: Double = 0.0,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    selfieUri: String = "",
    workedMinutes: Int = 0,
    allowedRadiusMeters: Double = 10.0,
    holidayReason: String? = null,
    markButtonTestTag: String,
    clockOutButtonTestTag: String,
    formatMinutes: (Int) -> String,
    onMarkAttendance: () -> Unit,
    onClockOut: () -> Unit
) {
    val context = LocalContext.current
    val isHoliday = holidayReason != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                todayStatus != null -> Color(0xFFF0FDF4)
                else -> PrimaryBlue.copy(alpha = 0.08f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isHoliday) {
                Surface(
                    color = RoyalDarkRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("sunday_weekly_off_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Weekly Off",
                            tint = RoyalDarkRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${holidayReason ?: "Sunday Weekly Off"} • Attendance Open",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalDarkRed
                        )
                    }
                }
            }

            when {
                todayStatus != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selfieUri.isNotBlank()) {
                            AttendancePhotoThumbnail(selfieUri = selfieUri)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (checkOutTime.isNotEmpty()) {
                                        "Attendance Login & Log Out Done"
                                    } else {
                                        "Attendance Logged In"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "$roleLabel • Status: $todayStatus • Login: ${checkInTime.ifBlank { "--" }}",
                                fontSize = 11.sp,
                                color = Navy800
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Google GPS: %.4f, %.4f (%dm from office)",
                                    latitude,
                                    longitude,
                                    distanceMeters.toInt()
                                ),
                                fontSize = 10.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (checkOutTime.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Log Out: $checkOutTime • Worked: ${formatMinutes(workedMinutes)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    } else if (todayStatus == "PRESENT" || todayStatus == "HALF_DAY") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onMarkAttendance,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag(markButtonTestTag),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Logged In", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                            }
                            Button(
                                onClick = onClockOut,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag(clockOutButtonTestTag),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalDarkRed)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (latitude != 0.0 || longitude != 0.0) {
                                OutlinedButton(
                                    onClick = {
                                        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (_: Exception) {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Open in Google Maps", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Attendance — Login & Log Out",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$roleLabel • Campus Google GPS (${allowedRadiusMeters.toInt()}m) + Live Photo",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onMarkAttendance,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag(markButtonTestTag),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attendance Login", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onClockOut,
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag(clockOutButtonTestTag),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
