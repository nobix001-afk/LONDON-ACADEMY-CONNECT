package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.firebase.GoogleAuthManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: EduViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showFirebaseDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Navy900,
                        PrimaryBlue,
                        RoyalDarkRed,
                        RoyalDarkRedDeep
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // London Academy Crest Logo (Exact Uploaded Crest)
            Surface(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_london_academy_logo),
                        contentDescription = "London Academy Connect Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("app_logo_image")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "London Academy Connect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Student & Employee Management System",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Google Workspace Connected & Cloud Firestore Sync Status Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showFirebaseDialog = true }
                    .testTag("google_workspace_banner"),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Google Workspace Connected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Workspace Connected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${viewModel.firebaseDisplayName.ifBlank { "Academy User" }} • ${viewModel.firebaseEmail}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$cloudSyncStatus • Tap for Firebase",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB9F6CA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.logout()
                                GoogleAuthManager.signOut(context)
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("google_signout_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "Sign Out",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter your User ID and password to access your role dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text("User ID") },
                        placeholder = { Text("e.g. admin or emp01") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_userid_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                        },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = "Toggle password visibility")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { showForgotPasswordDialog = true }
                                .padding(vertical = 4.dp)
                                .testTag("button_forgot_password")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (userId.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(userId, password)
                            } else {
                                viewModel.showMessage("Please enter both User ID and Password", isError = true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(
                            text = "Login to Portal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Demo Credentials Selector for Easy Testing & Evaluation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_demo_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 Quick Test / Demo Accounts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Tap any role to immediately test its unique dashboard and permissions:",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DemoRoleButton(
                            roleName = "Admin",
                            subtitle = "Full Access",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                userId = "admin"
                                password = "admin123"
                                viewModel.login("admin", "admin123")
                            }
                        )
                        DemoRoleButton(
                            roleName = "Sub-Admin",
                            subtitle = "Restricted",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                userId = "subadmin1"
                                password = "sub123"
                                viewModel.login("subadmin1", "sub123")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DemoRoleButton(
                            roleName = "Teacher",
                            subtitle = "Web Trainer",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                userId = "trainer1"
                                password = "teach123"
                                viewModel.login("trainer1", "teach123")
                            }
                        )
                        DemoRoleButton(
                            roleName = "Office Boy",
                            subtitle = "Support Staff",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                userId = "officeboy1"
                                password = "staff123"
                                viewModel.login("officeboy1", "staff123")
                            }
                        )
                        DemoRoleButton(
                            roleName = "Student",
                            subtitle = "Aman Gupta",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                userId = "student1"
                                password = "stud123"
                                viewModel.login("student1", "stud123")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (BuildConfig.APK_UPDATE_URL.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.APK_UPDATE_URL)))
                        }.onFailure {
                            viewModel.showMessage("Open link: ${BuildConfig.APK_UPDATE_URL}")
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("download_latest_apk_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Latest App Update", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Privacy Policy & Terms of Service",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .clickable { showPrivacyPolicy = true }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Forgot password dialog with OTP verification
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            viewModel = viewModel,
            onDismiss = { showForgotPasswordDialog = false }
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }

    if (showFirebaseDialog) {
        FirebaseConnectionDialog(
            viewModel = viewModel,
            cloudSyncStatus = cloudSyncStatus,
            onDismiss = { showFirebaseDialog = false }
        )
    }
}

@Composable
fun FirebaseConnectionDialog(
    viewModel: EduViewModel,
    cloudSyncStatus: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val projectId = remember {
        runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull().orEmpty()
    }
    val databaseId = remember(context) {
        runCatching { context.getString(R.string.firestore_database_id) }.getOrNull().orEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("firebase_connection_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firebase Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Firebase Dialog")
                    }
                }

                HorizontalDivider()

                Text("Project ID: ${projectId.ifBlank { "Configured" }}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Database ID: ${databaseId.ifBlank { "Configured" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Google Account: ${viewModel.firebaseEmail.ifBlank { viewModel.firebaseDisplayName }}", fontSize = 12.sp)
                Text("Firebase UID: ${viewModel.firebaseUserId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sync Status: $cloudSyncStatus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.forceSyncToFirebase() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("firebase_sync_now_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Cloud Now")
                }

                OutlinedButton(
                    onClick = {
                        val url = if (projectId.isNotBlank()) {
                            "https://console.firebase.google.com/project/$projectId/firestore/databases/$databaseId/data"
                        } else {
                            "https://console.firebase.google.com/"
                        }
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }.onFailure {
                            viewModel.showMessage("Open in browser: $url")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_firebase_console_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Firebase Console")
                }

                if (BuildConfig.APK_UPDATE_URL.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.APK_UPDATE_URL)))
                            }.onFailure {
                                viewModel.showMessage("Open link: ${BuildConfig.APK_UPDATE_URL}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("dialog_download_apk_update_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Latest APK Update")
                    }
                }
            }
        }
    }
}

@Composable
fun DemoRoleButton(
    roleName: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("demo_button_${roleName.lowercase()}"),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = roleName,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    viewModel: EduViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Enter ID & Mobile, 2: OTP, 3: New Pass
    var resetUserId by remember { mutableStateOf("") }
    var resetMobile by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val generatedOtp = "4892" // Simulated secure OTP

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("forgot_password_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (step) {
                    1 -> {
                        Text(
                            text = "Enter your User ID and registered contact number to receive a verification OTP:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = resetUserId,
                            onValueChange = { resetUserId = it },
                            label = { Text("User ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resetMobile,
                            onValueChange = { resetMobile = it },
                            label = { Text("Registered Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        if (errorText != null) {
                            Text(errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (resetUserId.isNotBlank() && resetMobile.isNotBlank()) {
                                    errorText = null
                                    step = 2
                                } else {
                                    errorText = "Please enter User ID and registered mobile"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Send OTP")
                        }
                    }
                    2 -> {
                        Text(
                            text = "A 4-digit verification OTP has been sent to +91 $resetMobile (Demo OTP is $generatedOtp):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = { enteredOtp = it },
                            label = { Text("Enter 4-Digit OTP") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        if (errorText != null) {
                            Text(errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (enteredOtp == generatedOtp || enteredOtp == "1234") {
                                    errorText = null
                                    step = 3
                                } else {
                                    errorText = "Invalid OTP. Please enter $generatedOtp"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verify OTP")
                        }
                    }
                    3 -> {
                        Text(
                            text = "OTP Verified! Create your new secure password:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        if (errorText != null) {
                            Text(errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (newPassword.length < 4) {
                                    errorText = "Password must be at least 4 characters"
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    errorText = "Passwords do not match"
                                    return@Button
                                }
                                viewModel.resetPassword(resetUserId, resetMobile, newPassword) { success ->
                                    if (success) onDismiss() else errorText = "Could not update password. Contact admin."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Update Password")
                        }
                    }
                }
            }
        }
    }
}
