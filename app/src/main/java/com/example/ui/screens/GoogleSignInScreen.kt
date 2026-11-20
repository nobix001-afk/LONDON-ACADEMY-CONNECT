package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.AppUpdateCheckResult
import com.example.data.AppUpdateChecker
import com.example.data.AppUpdateInfo
import com.example.data.firebase.GoogleAuthManager
import com.example.data.firebase.GoogleSignInResult
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoyalDarkRed
import com.example.ui.theme.RoyalDarkRedDeep
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun GoogleSignInScreen(
    onSignedIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }
    val coroutineScope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        isSigningIn = true
        when (val result = GoogleAuthManager.attemptSilentSignIn(activityContext)) {
            is GoogleSignInResult.Success -> onSignedIn()
            is GoogleSignInResult.Error -> errorMessage = result.message
            GoogleSignInResult.Cancelled,
            GoogleSignInResult.NoSavedCredential -> Unit
        }
        isSigningIn = false
    }

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                            .testTag("google_signin_logo_image")
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
                text = "Cloud Sync & Verified Workspace Access",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .testTag("google_signin_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Authenticate your Firebase account to securely sync students, branches, attendance, and fee records in Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Real-time Firebase Firestore Cloud Backup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = RoyalDarkRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Protected by Firebase Authentication & Security Rules",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_signin_error_banner")
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            if (isSigningIn) return@Button
                            errorMessage = null
                            isSigningIn = true
                            coroutineScope.launch {
                                when (val result = GoogleAuthManager.signInInteractive(activityContext)) {
                                    is GoogleSignInResult.Success -> onSignedIn()
                                    is GoogleSignInResult.Error -> errorMessage = result.message
                                    GoogleSignInResult.Cancelled -> {
                                        errorMessage = "Google Sign-In was cancelled."
                                    }
                                    GoogleSignInResult.NoSavedCredential -> Unit
                                }
                                isSigningIn = false
                            }
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Signing in...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (BuildConfig.APK_UPDATE_URL.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        if (isCheckingUpdate) return@OutlinedButton
                        isCheckingUpdate = true
                        updateStatusText = null
                        coroutineScope.launch {
                            when (val res = AppUpdateChecker.checkForUpdate()) {
                                is AppUpdateCheckResult.UpdateAvailable -> {
                                    availableUpdate = res.info
                                }
                                is AppUpdateCheckResult.UpToDate -> {
                                    updateStatusText = "You are on the latest version (v${res.currentVersionName})."
                                }
                                is AppUpdateCheckResult.Error -> {
                                    AppUpdateChecker.openUpdateLink(context)
                                }
                            }
                            isCheckingUpdate = false
                        }
                    },
                    enabled = !isCheckingUpdate,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("google_signin_check_update_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCheckingUpdate) "Checking for Updates..." else "Check for App Update",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (updateStatusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateStatusText.orEmpty(),
                        fontSize = 11.sp,
                        color = Color(0xFFB9F6CA),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    availableUpdate?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            onUpdateNow = {
                AppUpdateChecker.openUpdateLink(context, updateInfo.downloadUrl)
                availableUpdate = null
            },
            onDismiss = { availableUpdate = null }
        )
    }
}
