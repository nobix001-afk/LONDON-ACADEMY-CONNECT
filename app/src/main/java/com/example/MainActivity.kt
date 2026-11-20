package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppDatabase
import com.example.data.AppMediaStorage
import com.example.data.AppUpdateCheckResult
import com.example.data.AppUpdateChecker
import com.example.data.AppUpdateInfo
import com.example.data.firebase.FirestoreAcademyRepository
import com.example.data.firebase.authStateFlow
import com.example.data.repository.EduRepository
import com.example.ui.components.AppUpdateDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.EmployeeDashboardScreen
import com.example.ui.screens.GoogleSignInScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.screens.SubAdminDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.EduViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val context = LocalContext.current
                var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    val result = AppUpdateChecker.checkForUpdate()
                    if (result is AppUpdateCheckResult.UpdateAvailable) {
                        pendingUpdate = result.info
                    }
                }

                val isFirebaseReady = remember(context) {
                    FirebaseApp.getApps(context).isNotEmpty() ||
                        runCatching { FirebaseApp.initializeApp(context) != null }.getOrDefault(false)
                }

                if (!isFirebaseReady) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Firebase Configuration Missing",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "This APK was built without app/google-services.json. Upload app/google-services.json to your GitHub repo or add the GOOGLE_SERVICES_JSON secret and rebuild.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val firebaseUser by Firebase.auth.authStateFlow()
                        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                    val activeUser = firebaseUser
                    if (activeUser == null) {
                        GoogleSignInScreen()
                    } else {
                        AuthenticatedAcademyApp(firebaseUser = activeUser)
                    }
                }

                pendingUpdate?.let { updateInfo ->
                    AppUpdateDialog(
                        updateInfo = updateInfo,
                        onUpdateNow = {
                            AppUpdateChecker.openUpdateLink(context, updateInfo.downloadUrl)
                            pendingUpdate = null
                        },
                        onDismiss = { pendingUpdate = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAcademyApp(firebaseUser: FirebaseUser) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: EduViewModel = viewModel(
        key = firebaseUser.uid,
        factory = viewModelFactory {
            initializer {
                val databaseId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val firestoreRepo = FirestoreAcademyRepository(firestore)
                val db = AppDatabase.getDatabase(appContext)
                val repository = EduRepository(
                    dao = db.appDao(),
                    firestoreRepo = firestoreRepo,
                    firebaseUid = firebaseUser.uid
                )
                val mediaStorage = AppMediaStorage(appContext)
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
                EduViewModel(
                    repository = repository,
                    firestoreRepo = firestoreRepo,
                    mediaStorage = mediaStorage,
                    fusedLocationClient = fusedLocationClient,
                    firebaseUserId = firebaseUser.uid,
                    firebaseDisplayName = firebaseUser.displayName ?: "Academy User",
                    firebaseEmail = firebaseUser.email.orEmpty(),
                    firebasePhotoUrl = firebaseUser.photoUrl?.toString()
                )
            }
        }
    )

    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(
                message = it.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDest) {
                AppDestination.LOGIN -> LoginScreen(viewModel = viewModel)
                AppDestination.ADMIN_DASHBOARD -> AdminDashboardScreen(viewModel = viewModel)
                AppDestination.SUB_ADMIN_DASHBOARD -> SubAdminDashboardScreen(viewModel = viewModel)
                AppDestination.EMPLOYEE_DASHBOARD -> EmployeeDashboardScreen(viewModel = viewModel)
                AppDestination.STUDENT_DASHBOARD -> StudentDashboardScreen(viewModel = viewModel)
            }
        }
    }
}
