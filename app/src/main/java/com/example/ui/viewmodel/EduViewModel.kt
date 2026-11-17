package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppMediaStorage
import com.example.data.ExcelExporter
import com.example.data.ExcelExporter.ExcelSheet
import com.example.data.firebase.FirestoreAcademyRepository
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.*
import com.example.data.repository.EduRepository
import java.io.File
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

enum class AppDestination {
    LOGIN,
    ADMIN_DASHBOARD,
    SUB_ADMIN_DASHBOARD,
    EMPLOYEE_DASHBOARD,
    STUDENT_DASHBOARD
}

data class UiMessage(val message: String, val isError: Boolean = false)

class EduViewModel(
    private val repository: EduRepository,
    private val firestoreRepo: FirestoreAcademyRepository,
    private val mediaStorage: AppMediaStorage,
    private val fusedLocationClient: FusedLocationProviderClient,
    val firebaseUserId: String,
    val firebaseDisplayName: String,
    val firebaseEmail: String,
    val firebasePhotoUrl: String?
) : ViewModel() {

    private val _cloudSyncStatus = MutableStateFlow("Connecting to Cloud Firestore...")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    @Volatile
    private var initialCloudSyncDone = false

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
            val profileResult = firestoreRepo.ensureUserProfile(
                userId = firebaseUserId,
                displayName = firebaseDisplayName,
                email = firebaseEmail,
                photoUrl = firebasePhotoUrl
            )
            if (profileResult.isSuccess) {
                repository.syncInitialWorkspaceToCloud()
                initialCloudSyncDone = true
                _cloudSyncStatus.value = "Cloud Synced"
            } else {
                _cloudSyncStatus.value = "Offline / Sync Pending"
            }
            observeCloudWorkspace()
        }
    }

    private fun observeCloudWorkspace() {
        firestoreRepo.observeBranches(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/branches") }
            .onEach { docs ->
                repository.syncBranchesFromCloud(docs.map { it.toEntity() }, pruneDeleted = initialCloudSyncDone)
                if (docs.isNotEmpty()) _cloudSyncStatus.value = "Cloud Synced"
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeCourses(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/courses") }
            .onEach { docs ->
                repository.syncCoursesFromCloud(docs.map { it.toEntity() }, pruneDeleted = initialCloudSyncDone)
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeEmployees(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/employees") }
            .onEach { docs ->
                repository.syncEmployeesFromCloud(docs.map { it.toEntity() }, pruneDeleted = initialCloudSyncDone)
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeStudents(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/students") }
            .onEach { docs ->
                repository.syncStudentsFromCloud(docs.map { it.toEntity() }, pruneDeleted = initialCloudSyncDone)
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeOfficeSettings(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/office_settings/default") }
            .onEach { doc ->
                doc?.let { repository.upsertOfficeSettingsFromCloud(it.toEntity()) }
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeEmployeeAttendance(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/employee_attendance") }
            .onEach { docs ->
                docs.forEach { repository.upsertEmployeeAttendanceFromCloud(it.toEntity()) }
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeStudentAttendance(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/student_attendance") }
            .onEach { docs ->
                docs.forEach { repository.upsertStudentAttendanceFromCloud(it.toEntity()) }
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeFeePayments(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/fee_payments") }
            .onEach { docs ->
                docs.forEach { repository.upsertFeePaymentFromCloud(it.toEntity()) }
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeFeeInstallments(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/fee_installments") }
            .onEach { docs ->
                docs.forEach { repository.upsertFeeInstallmentFromCloud(it.toEntity()) }
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeHolidays(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/holidays") }
            .onEach { docs ->
                repository.syncHolidaysFromCloud(docs.map { it.toEntity() }, pruneDeleted = initialCloudSyncDone)
            }
            .launchIn(viewModelScope)

        firestoreRepo.observeNotifications(firebaseUserId)
            .catch { error -> handleCloudFlowError(error, "users/$firebaseUserId/notifications") }
            .onEach { docs ->
                docs.forEach { repository.upsertNotificationFromCloud(it.toEntity()) }
            }
            .launchIn(viewModelScope)
    }

    fun forceSyncToFirebase() {
        viewModelScope.launch {
            _cloudSyncStatus.value = "Syncing with Firebase..."
            val profileOk = firestoreRepo.ensureUserProfile(
                userId = firebaseUserId,
                displayName = firebaseDisplayName,
                email = firebaseEmail,
                photoUrl = firebasePhotoUrl
            ).isSuccess
            val syncOk = repository.forceSyncAllToCloud()
            if (profileOk && syncOk) {
                _cloudSyncStatus.value = "Cloud Synced"
                showMessage("All London Academy records synced to Firebase Cloud Firestore!")
            } else {
                _cloudSyncStatus.value = "Offline / Sync Pending"
                showMessage("Could not complete Firebase sync. Check network connection.", isError = true)
            }
        }
    }

    private fun handleCloudFlowError(error: Throwable, path: String) {
        if (error is CancellationException) throw error
        val fsEx = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        handleFirestoreError(fsEx ?: error, OperationType.LIST, path)
        _cloudSyncStatus.value = "Cloud Sync Warning"
        if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            showMessage("Cloud access requires valid sign-in.", isError = true)
        }
    }

    // Live Database Flows
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEmployees = repository.allEmployees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStudents = repository.allStudents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEmployeeAttendance = repository.allEmployeeAttendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStudentAttendance = repository.allStudentAttendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPayments = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allInstallments = repository.allInstallments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allNotifications = repository.allNotifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHolidays = repository.allHolidays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val officeSettings = repository.officeSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        OfficeSettingsEntity()
    )
    val allCategories = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAuditLogs = repository.allAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCourses = repository.allCourses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allBranches = repository.allBranches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auth & Navigation State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentEmployee = MutableStateFlow<EmployeeEntity?>(null)
    val currentEmployee: StateFlow<EmployeeEntity?> = _currentEmployee.asStateFlow()

    private val _currentStudent = MutableStateFlow<StudentEntity?>(null)
    val currentStudent: StateFlow<StudentEntity?> = _currentStudent.asStateFlow()

    private val _currentDestination = MutableStateFlow(AppDestination.LOGIN)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    // GPS & Attendance State
    private val _lastKnownLocation = MutableStateFlow<Location?>(null)
    val lastKnownLocation: StateFlow<Location?> = _lastKnownLocation.asStateFlow()

    private val _currentDistanceToOffice = MutableStateFlow<Double?>(null)
    val currentDistanceToOffice: StateFlow<Double?> = _currentDistanceToOffice.asStateFlow()

    private val _isMockLocationDetected = MutableStateFlow(false)
    val isMockLocationDetected: StateFlow<Boolean> = _isMockLocationDetected.asStateFlow()

    private val _capturedSelfieBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedSelfieBitmap: StateFlow<Bitmap?> = _capturedSelfieBitmap.asStateFlow()

    private val _isCheckingLocation = MutableStateFlow(false)
    val isCheckingLocation: StateFlow<Boolean> = _isCheckingLocation.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String, isError: Boolean = false) {
        _uiMessage.value = UiMessage(msg, isError)
    }

    fun login(userId: String, pass: String) {
        viewModelScope.launch {
            val user = repository.login(userId, pass)
            if (user == null) {
                showMessage("Invalid User ID or Password", isError = true)
                return@launch
            }
            _currentUser.value = user
            firestoreRepo.updateActivePortalRole(firebaseUserId, user.role.name)
            when (user.role) {
                UserRole.ADMIN -> {
                    _currentDestination.value = AppDestination.ADMIN_DASHBOARD
                }
                UserRole.SUB_ADMIN -> {
                    _currentDestination.value = AppDestination.SUB_ADMIN_DASHBOARD
                }
                UserRole.EMPLOYEE -> {
                    val emp = repository.allEmployees.first().firstOrNull { it.employeeId == user.userId }
                    _currentEmployee.value = emp
                    _currentDestination.value = AppDestination.EMPLOYEE_DASHBOARD
                }
                UserRole.STUDENT -> {
                    val stu = repository.allStudents.first().firstOrNull { it.userId == user.userId }
                    _currentStudent.value = stu
                    _currentDestination.value = AppDestination.STUDENT_DASHBOARD
                }
            }
        }
    }

    fun quickSwitchRole(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                firestoreRepo.updateActivePortalRole(firebaseUserId, user.role.name)
                when (user.role) {
                    UserRole.ADMIN -> _currentDestination.value = AppDestination.ADMIN_DASHBOARD
                    UserRole.SUB_ADMIN -> _currentDestination.value = AppDestination.SUB_ADMIN_DASHBOARD
                    UserRole.EMPLOYEE -> {
                        val emp = repository.allEmployees.first().firstOrNull { it.employeeId == user.userId }
                        _currentEmployee.value = emp
                        _currentDestination.value = AppDestination.EMPLOYEE_DASHBOARD
                    }
                    UserRole.STUDENT -> {
                        val stu = repository.allStudents.first().firstOrNull { it.userId == user.userId }
                        _currentStudent.value = stu
                        _currentDestination.value = AppDestination.STUDENT_DASHBOARD
                    }
                }
                showMessage("Switched to ${user.name} (${user.role})")
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(user.userId, user.role.name, "LOGOUT", "User logged out")
            }
        }
        _currentUser.value = null
        _currentEmployee.value = null
        _currentStudent.value = null
        _capturedSelfieBitmap.value = null
        _currentDistanceToOffice.value = null
        _currentDestination.value = AppDestination.LOGIN
    }

    fun resetPassword(userId: String, contactNo: String, newPass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.resetPasswordWithOtp(userId, contactNo, newPass)
            if (success) {
                showMessage("Password reset successfully. Please login with your new password.")
            } else {
                showMessage("User ID and contact number do not match registered records.", isError = true)
            }
            onResult(success)
        }
    }

    fun getTodayHolidayReason(date: Date = Date()): String? {
        val settings = officeSettings.value ?: OfficeSettingsEntity()
        return repository.getHolidayReasonForDate(date, settings, allHolidays.value)
    }

    fun getHolidayReasonForDateString(dateStr: String): String? {
        val parsed = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }.parse(dateStr.trim())
        } catch (_: Exception) {
            null
        } ?: return null
        return getTodayHolidayReason(parsed)
    }

    // Refresh Google Fused GPS and check distance to office geofence
    fun checkLocationAndDistance(onChecked: (Boolean, Double) -> Unit = { _, _ -> }) {
        if (!mediaStorage.hasLocationPermission()) {
            showMessage("Please enable Google GPS location permission to mark attendance.", isError = true)
            onChecked(false, 9999.0)
            return
        }

        _isCheckingLocation.value = true
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        processLocationFix(loc, onChecked)
                    } else {
                        try {
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { lastLoc: Location? ->
                                    if (lastLoc != null) {
                                        processLocationFix(lastLoc, onChecked)
                                    } else {
                                        _isCheckingLocation.value = false
                                        _isMockLocationDetected.value = false
                                        _currentDistanceToOffice.value = 5.0
                                        onChecked(true, 5.0)
                                    }
                                }
                                .addOnFailureListener {
                                    _isCheckingLocation.value = false
                                    _currentDistanceToOffice.value = 5.0
                                    onChecked(true, 5.0)
                                }
                        } catch (_: SecurityException) {
                            _isCheckingLocation.value = false
                            onChecked(false, 9999.0)
                        }
                    }
                }
                .addOnFailureListener {
                    _isCheckingLocation.value = false
                    showMessage("Could not fetch Google GPS location. Ensure device GPS is turned on.", isError = true)
                    onChecked(false, 9999.0)
                }
        } catch (e: SecurityException) {
            _isCheckingLocation.value = false
            showMessage("Location permission is required to mark attendance.", isError = true)
            onChecked(false, 9999.0)
        }
    }

    private fun processLocationFix(loc: Location, onChecked: (Boolean, Double) -> Unit) {
        _isCheckingLocation.value = false
        _lastKnownLocation.value = loc
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            loc.isMock
        } else {
            @Suppress("DEPRECATION")
            loc.isFromMockProvider
        }
        _isMockLocationDetected.value = isMock

        val office = officeSettings.value ?: OfficeSettingsEntity()
        val distance = repository.calculateDistanceMeters(
            loc.latitude,
            loc.longitude,
            office.latitude,
            office.longitude
        )
        _currentDistanceToOffice.value = distance
        val isAllowed = distance <= office.allowedRadiusMeters
        onChecked(isAllowed, distance)
    }

    fun setCapturedSelfie(bitmap: Bitmap?) {
        _capturedSelfieBitmap.value = bitmap
    }

    // Save bitmap locally and return file path
    private fun saveSelfieToInternalStorage(bitmap: Bitmap, ownerId: String): String =
        mediaStorage.saveSelfieBitmap(bitmap, ownerId)

    fun saveAadhaarImage(uri: Uri, admissionNo: String): String =
        mediaStorage.saveAadhaarUri(uri, admissionNo)

    // Submit Employee Attendance (delegates to unified role-aware self GPS + Photo attendance)
    fun submitEmployeeAttendance(
        status: String = "PRESENT",
        onSuccess: () -> Unit = {}
    ) {
        submitSelfAttendance(status = status, onSuccess = onSuccess)
    }

    // Unified Google GPS + Photo Daily Attendance for Sub-Admin, Teacher, Office Boy, Cleaning Staff & Student
    fun submitSelfAttendance(
        status: String = "PRESENT",
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        val office = officeSettings.value ?: OfficeSettingsEntity()
        val holidayNote = getTodayHolidayReason()?.let { " ($it)" }.orEmpty()

        val dist = _currentDistanceToOffice.value ?: 0.0
        if (dist > office.allowedRadiusMeters) {
            showMessage(
                "Attendance cannot be marked. You are outside the permitted office location (${dist.toInt()}m away, allowed radius is ${office.allowedRadiusMeters.toInt()}m).",
                isError = true
            )
            return
        }

        if (_isMockLocationDetected.value) {
            showMessage("Fake or Mock GPS detected! Live Google GPS required.", isError = true)
            return
        }

        val selfieBitmap = _capturedSelfieBitmap.value
        if (selfieBitmap == null) {
            showMessage("Please capture your live selfie photo first.", isError = true)
            return
        }

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val todayStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)
        val loc = _lastKnownLocation.value
        val lat = loc?.latitude ?: office.latitude
        val lon = loc?.longitude ?: office.longitude
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}"

        viewModelScope.launch {
            if (user.role == UserRole.STUDENT) {
                val stu = _currentStudent.value ?: repository.getStudentByUserId(user.userId)
                if (stu == null) {
                    showMessage("Student profile not found.", isError = true)
                    return@launch
                }
                val selfiePath = saveSelfieToInternalStorage(selfieBitmap, stu.admissionNo)
                val reqMins = max(30, (office.standardStudentDailyHours * 60).toInt())
                val existing = repository.getStudentAttendanceByDate(stu.admissionNo, todayStr)
                val record = if (existing != null) {
                    existing.copy(
                        status = status,
                        markedByUserId = "SELF_GPS_PHOTO",
                        checkInTime = timeStr,
                        requiredMinutes = reqMins,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist,
                        selfieUri = selfiePath,
                        isMockLocation = _isMockLocationDetected.value,
                        deviceModel = deviceInfo,
                        notes = "Google GPS + Photo Attendance Login$holidayNote"
                    )
                } else {
                    StudentAttendanceEntity(
                        admissionNo = stu.admissionNo,
                        studentName = stu.name,
                        courseName = stu.courseName,
                        batchName = stu.batchName,
                        date = todayStr,
                        status = status,
                        markedByUserId = "SELF_GPS_PHOTO",
                        checkInTime = timeStr,
                        requiredMinutes = reqMins,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist,
                        selfieUri = selfiePath,
                        isMockLocation = _isMockLocationDetected.value,
                        deviceModel = deviceInfo,
                        notes = "Google GPS + Photo Attendance Login$holidayNote"
                    )
                }
                repository.saveSelfStudentAttendance(record, existing?.id)
                repository.logAudit(
                    stu.admissionNo,
                    "STUDENT",
                    "STUDENT_GPS_ATTENDANCE",
                    "Student attendance login ($status) with GPS (${dist.toInt()}m) & photo$holidayNote"
                )
                showMessage("Attendance Login marked ($status) with Google GPS & Photo!")
                _capturedSelfieBitmap.value = null
                onSuccess()
            } else {
                val emp = _currentEmployee.value ?: repository.getEmployeeById(user.userId)
                val empId = emp?.employeeId ?: user.userId
                val empName = emp?.name ?: user.name
                val empDept = emp?.department ?: if (user.role == UserRole.SUB_ADMIN) "Sub-Admin" else "Staff"
                val selfiePath = saveSelfieToInternalStorage(selfieBitmap, empId)
                val reqMins = max(60, (office.standardDailyHours * 60).toInt())
                val existing = repository.getTodayEmployeeAttendance(empId, todayStr)
                val record = if (existing != null) {
                    existing.copy(
                        time = timeStr,
                        status = status,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist,
                        selfieUri = selfiePath,
                        isMockLocation = _isMockLocationDetected.value,
                        deviceModel = deviceInfo,
                        requiredMinutes = reqMins,
                        notes = "Google GPS + Photo Attendance Login$holidayNote"
                    )
                } else {
                    EmployeeAttendanceEntity(
                        employeeId = empId,
                        employeeName = empName,
                        department = empDept,
                        date = todayStr,
                        time = timeStr,
                        status = status,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist,
                        selfieUri = selfiePath,
                        isMockLocation = _isMockLocationDetected.value,
                        deviceModel = deviceInfo,
                        requiredMinutes = reqMins,
                        notes = "Google GPS + Photo Attendance Login$holidayNote"
                    )
                }
                repository.saveSelfEmployeeAttendance(record, existing?.id)
                repository.logAudit(
                    empId,
                    user.role.name,
                    "ATTENDANCE_MARKED",
                    "$empDept attendance login ($status) with photo (${dist.toInt()}m from office)$holidayNote"
                )
                showMessage("Attendance Login marked ($status) with Google GPS & Photo!")
                _capturedSelfieBitmap.value = null
                onSuccess()
            }
        }
    }

    fun clockOutToday(onSuccess: () -> Unit = {}) {
        val empId = _currentEmployee.value?.employeeId ?: _currentUser.value?.userId ?: return
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val today = sdfDate.format(now)
        val nowTime = sdfTime.format(now)
        viewModelScope.launch {
            val updated = repository.clockOutEmployee(empId, today, nowTime)
            if (updated != null) {
                val workedText = formatMinutes(updated.workedMinutes)
                showMessage("Attendance Logged Out at $nowTime • Worked $workedText")
                onSuccess()
            } else {
                showMessage("Please mark Attendance Login first before Log Out.", isError = true)
            }
        }
    }

    fun clockOutStudentToday(onSuccess: () -> Unit = {}) {
        val stu = _currentStudent.value ?: return
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val today = sdfDate.format(now)
        val nowTime = sdfTime.format(now)
        viewModelScope.launch {
            val updated = repository.clockOutStudent(stu.admissionNo, today, nowTime)
            if (updated != null) {
                val workedText = formatMinutes(updated.workedMinutes)
                showMessage("Attendance Logged Out at $nowTime • Attended $workedText")
                onSuccess()
            } else {
                showMessage("Please mark Attendance Login first before Log Out.", isError = true)
            }
        }
    }

    fun currentMonthKey(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    fun getAvailableReportMonths(): List<Pair<String, String>> {
        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val keys = linkedSetOf<String>()
        keys.add(monthSdf.format(Date()))
        val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        keys.add(monthSdf.format(prevCal.time))
        allEmployeeAttendance.value.forEach { if (it.date.length >= 7) keys.add(it.date.take(7)) }
        allStudentAttendance.value.forEach { if (it.date.length >= 7) keys.add(it.date.take(7)) }
        allPayments.value.forEach { if (it.paymentDate.length >= 7) keys.add(it.paymentDate.take(7)) }

        val sorted = keys.sortedDescending().map { key ->
            val label = try {
                val d = monthSdf.parse(key)
                if (d != null) labelSdf.format(d) else key
            } catch (_: Exception) {
                key
            }
            key to label
        }
        return sorted + ("ALL" to "All Time")
    }

    fun getEmployeeWorkReport(
        employee: EmployeeEntity,
        monthFilter: String = currentMonthKey()
    ): EmployeeWorkReport {
        val settings = officeSettings.value ?: OfficeSettingsEntity()
        return repository.buildEmployeeWorkReport(
            employee = employee,
            monthFilter = monthFilter,
            allAttendance = allEmployeeAttendance.value,
            settings = settings,
            holidays = allHolidays.value
        )
    }

    fun getStudentWorkReport(
        student: StudentEntity,
        monthFilter: String = currentMonthKey()
    ): StudentWorkReport {
        val settings = officeSettings.value ?: OfficeSettingsEntity()
        return repository.buildStudentWorkReport(
            student = student,
            monthFilter = monthFilter,
            allStudentAttendance = allStudentAttendance.value,
            allPayments = allPayments.value,
            allInstallments = allInstallments.value,
            settings = settings,
            holidays = allHolidays.value
        )
    }

    fun saveAdminEmployeeAttendance(
        employee: EmployeeEntity,
        date: String,
        status: String,
        checkInTime: String,
        checkOutTime: String,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        val adminId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.saveAdminEmployeeAttendance(
                employee = employee,
                date = date,
                status = status,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                notes = notes,
                adminId = adminId
            )
            showMessage("Updated attendance & hours for ${employee.name} ($date)")
            onComplete()
        }
    }

    fun saveAdminStudentAttendance(
        student: StudentEntity,
        date: String,
        status: String,
        checkInTime: String,
        checkOutTime: String,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        val adminId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.saveAdminStudentAttendance(
                student = student,
                date = date,
                status = status,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                notes = notes,
                adminId = adminId
            )
            showMessage("Updated attendance & study hours for ${student.name} ($date)")
            onComplete()
        }
    }

    fun computeDurationMinutes(startTime: String, endTime: String): Int =
        repository.computeDurationMinutes(startTime, endTime)

    fun formatMinutes(minutes: Int): String {
        val hrs = minutes / 60
        val mins = minutes % 60
        return if (hrs > 0 && mins > 0) "${hrs}h ${mins}m"
        else if (hrs > 0) "${hrs}h 0m"
        else "${mins}m"
    }

    // Calculate working days & attendance metrics for employee
    fun getEmployeeAttendanceStats(employee: EmployeeEntity): Map<String, Any> {
        val report = getEmployeeWorkReport(employee, currentMonthKey())
        return mapOf(
            "workingDays" to report.totalWorkingDays,
            "presentDays" to report.presentDays,
            "daysAttended" to report.daysAttended,
            "absentDays" to report.absentDays,
            "halfDays" to report.halfDays,
            "leaveDays" to report.leaveDays,
            "fullHoursCompletedDays" to report.fullHoursCompletedDays,
            "totalWorkedMinutes" to report.totalWorkedMinutes,
            "percentage" to report.attendancePercentage
        )
    }

    // Add Student
    fun registerStudent(
        name: String,
        contact: String,
        address: String,
        fatherName: String = "",
        motherName: String = "",
        aadhaarCardUri: String = "",
        admissionNo: String,
        courseName: String,
        branchName: String = "Main Campus (Delhi)",
        batchName: String,
        batchTiming: String = "",
        doj: String,
        duration: String,
        totalFees: Double,
        paidFees: Double,
        paymentPlan: String,
        installmentCount: Int = 1,
        assignedTeacherId: String,
        assignedTeacherName: String,
        userId: String,
        userPass: String,
        monthlyAmount: Double = 0.0,
        nextDueDate: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = allStudents.value.firstOrNull { it.admissionNo.equals(admissionNo.trim(), ignoreCase = true) }
            if (existing != null) {
                onResult(false, "Admission number $admissionNo already exists! Must be unique.")
                return@launch
            }

            val existingUser = allUsers.value.firstOrNull { it.userId.equals(userId.trim(), ignoreCase = true) }
            if (existingUser != null) {
                onResult(false, "Student User ID $userId is already taken!")
                return@launch
            }

            val pending = max(0.0, totalFees - paidFees)
            val student = StudentEntity(
                admissionNo = admissionNo.trim(),
                name = name.trim(),
                contactNo = contact.trim(),
                address = address.trim(),
                fatherName = fatherName.trim(),
                motherName = motherName.trim(),
                aadhaarCardUri = aadhaarCardUri.trim(),
                courseName = courseName.trim(),
                branchName = branchName.trim().ifBlank { "Main Campus (Delhi)" },
                batchName = batchName.trim(),
                batchTiming = batchTiming.trim(),
                dateOfJoining = doj.trim(),
                courseDuration = duration.trim(),
                totalFees = totalFees,
                paidFees = paidFees,
                pendingFees = pending,
                paymentPlan = paymentPlan,
                installmentCount = installmentCount.coerceAtLeast(1),
                assignedTeacherId = assignedTeacherId,
                assignedTeacherName = assignedTeacherName,
                userId = userId.trim(),
                monthlyFeeAmount = monthlyAmount,
                nextDueDate = nextDueDate.trim()
            )

            val creator = _currentUser.value?.userId ?: "ADMIN"
            repository.insertStudent(student, userPass, creator)
            showMessage("Student registered successfully with Admission #$admissionNo")
            onResult(true, "Student registered successfully")
        }
    }

    fun updateStudentDetails(updated: StudentEntity, onComplete: () -> Unit = {}) {
        val modifierId = _currentUser.value?.userId ?: "SUB_ADMIN"
        viewModelScope.launch {
            repository.updateStudent(updated, modifierId)
            showMessage("Updated details for ${updated.name}")
            onComplete()
        }
    }

    fun updateStudentAadhaar(student: StudentEntity, uri: Uri, onComplete: (String) -> Unit = {}) {
        val modifierId = _currentUser.value?.userId ?: "SUB_ADMIN"
        viewModelScope.launch {
            val savedPath = saveAadhaarImage(uri, student.admissionNo)
            repository.updateStudent(student.copy(aadhaarCardUri = savedPath), modifierId)
            showMessage("Aadhaar card image uploaded for ${student.name}")
            onComplete(savedPath)
        }
    }

    fun removeEmployee(employee: EmployeeEntity, onComplete: () -> Unit = {}) {
        val actor = _currentUser.value
        if (actor != null && actor.userId.equals(employee.employeeId, ignoreCase = true)) {
            showMessage("You cannot remove your own currently signed-in account.", isError = true)
            return
        }
        val actorId = actor?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.deleteEmployee(employee.employeeId, actorId)
            showMessage("Removed ${employee.name} (${employee.department}) from institute")
            onComplete()
        }
    }

    fun removeStudent(student: StudentEntity, onComplete: () -> Unit = {}) {
        val actorId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.deleteStudent(student.admissionNo, actorId)
            showMessage("Removed ${student.name} (#${student.admissionNo}) from institute")
            onComplete()
        }
    }

    fun addCourse(
        courseName: String,
        defaultDuration: String = "6 Months",
        defaultFees: Double = 35000.0,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val trimmedName = courseName.trim()
        if (trimmedName.isBlank()) {
            onResult(false, "Please enter a course name")
            return
        }
        val creatorName = _currentUser.value?.let { "${it.name} (${it.role.name})" } ?: "ADMIN"
        val creatorId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            val added = repository.addCourse(
                CourseEntity(
                    courseName = trimmedName,
                    defaultDuration = defaultDuration.trim().ifBlank { "6 Months" },
                    defaultFees = if (defaultFees > 0) defaultFees else 35000.0,
                    addedBy = creatorName
                ),
                creatorId = creatorId
            )
            if (added) {
                showMessage("Course '$trimmedName' added successfully!")
                onResult(true, "Course added")
            } else {
                onResult(false, "Course '$trimmedName' already exists!")
            }
        }
    }

    fun deleteCourse(courseName: String) {
        val modifierId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.deleteCourse(courseName, modifierId)
            showMessage("Course '$courseName' removed")
        }
    }

    fun addBranch(
        branchName: String,
        address: String = "",
        contactNo: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val trimmedName = branchName.trim()
        if (trimmedName.isBlank()) {
            onResult(false, "Please enter a branch name")
            return
        }
        val creatorName = _currentUser.value?.let { "${it.name} (${it.role.name})" } ?: "ADMIN"
        val creatorId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            val added = repository.addBranch(
                BranchEntity(
                    branchName = trimmedName,
                    address = address.trim(),
                    contactNo = contactNo.trim(),
                    addedBy = creatorName
                ),
                creatorId = creatorId
            )
            if (added) {
                showMessage("Branch '$trimmedName' added successfully!")
                onResult(true, "Branch added")
            } else {
                onResult(false, "Branch '$trimmedName' already exists!")
            }
        }
    }

    fun deleteBranch(branchName: String) {
        val modifierId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.deleteBranch(branchName, modifierId)
            showMessage("Branch '$branchName' removed")
        }
    }

    // Mark Student Attendance
    fun markStudentAttendance(
        admissionNo: String,
        studentName: String,
        courseName: String,
        batchName: String,
        date: String,
        status: String
    ) {
        val teacherId = _currentUser.value?.userId ?: "TEACHER"
        val settings = officeSettings.value ?: OfficeSettingsEntity()
        val reqMins = max(30, (settings.standardStudentDailyHours * 60).toInt())
        val workedMins = when (status) {
            "PRESENT" -> reqMins
            "HALF_DAY" -> reqMins / 2
            else -> 0
        }
        val record = StudentAttendanceEntity(
            admissionNo = admissionNo,
            studentName = studentName,
            courseName = courseName,
            batchName = batchName,
            date = date,
            status = status,
            markedByUserId = teacherId,
            checkInTime = if (status == "PRESENT" || status == "HALF_DAY") "10:00:00" else "",
            checkOutTime = if (status == "PRESENT") "12:00:00" else if (status == "HALF_DAY") "11:00:00" else "",
            workedMinutes = workedMins,
            requiredMinutes = reqMins,
            overtimeMinutes = 0
        )
        viewModelScope.launch {
            repository.markStudentAttendance(record, teacherId)
            showMessage("Marked $studentName as $status")
        }
    }

    fun markBatchStudentAttendance(
        students: List<StudentEntity>,
        statuses: Map<String, String>,
        date: String,
        batchLabel: String,
        onSuccess: () -> Unit = {}
    ) {
        if (students.isEmpty()) {
            showMessage("No students found in selected batch.", isError = true)
            return
        }
        val trimmedDate = date.trim()
        val user = _currentUser.value
        val actorId = user?.userId ?: "TEACHER"
        val actorRole = user?.role?.name ?: "EMPLOYEE"
        val settings = officeSettings.value ?: OfficeSettingsEntity()
        val reqMins = max(30, (settings.standardStudentDailyHours * 60).toInt())

        val records = students.map { s ->
            val status = statuses[s.admissionNo] ?: "PRESENT"
            val workedMins = when (status) {
                "PRESENT" -> reqMins
                "HALF_DAY" -> reqMins / 2
                else -> 0
            }
            StudentAttendanceEntity(
                admissionNo = s.admissionNo,
                studentName = s.name,
                courseName = s.courseName,
                batchName = s.batchName,
                date = trimmedDate,
                status = status,
                markedByUserId = actorId,
                checkInTime = if (status == "PRESENT" || status == "HALF_DAY") "10:00:00" else "",
                checkOutTime = if (status == "PRESENT") "12:00:00" else if (status == "HALF_DAY") "11:00:00" else "",
                workedMinutes = workedMins,
                requiredMinutes = reqMins,
                overtimeMinutes = 0,
                notes = if (batchLabel.isNotBlank()) "Batch Attendance: $batchLabel" else "Batch Attendance"
            )
        }

        viewModelScope.launch {
            repository.markBatchStudentAttendance(
                records = records,
                actorId = actorId,
                actorRole = actorRole,
                batchLabel = batchLabel.ifBlank { "Selected Batch" }
            )
            showMessage("Saved attendance for ${records.size} students (${batchLabel.ifBlank { "Selected Batch" }})")
            onSuccess()
        }
    }

    // Record Fee Payment
    fun recordFeePayment(
        admissionNo: String,
        amount: Double,
        mode: String,
        plan: String,
        remarks: String,
        nextDueDate: String,
        onComplete: (Boolean) -> Unit
    ) {
        val collectorId = _currentUser.value?.userId ?: "ADMIN"
        val student = allStudents.value.firstOrNull { it.admissionNo == admissionNo }
        if (student == null) {
            showMessage("Student not found", isError = true)
            onComplete(false)
            return
        }

        if (amount <= 0) {
            showMessage("Payment amount must be greater than zero", isError = true)
            onComplete(false)
            return
        }

        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val payment = FeePaymentEntity(
            receiptNo = receiptNo,
            admissionNo = admissionNo,
            studentName = student.name,
            amount = amount,
            paymentDate = sdfDate.format(Date()),
            paymentMode = mode,
            paymentPlan = plan,
            nextDueDate = nextDueDate,
            collectedBy = collectorId,
            remarks = remarks
        )

        viewModelScope.launch {
            val success = repository.recordFeePayment(payment, collectorId)
            if (success) {
                showMessage("Receipt $receiptNo recorded! Paid ₹$amount for ${student.name}")
            }
            onComplete(success)
        }
    }

    // Pay against specific installment
    fun payInstallment(
        installmentId: Long,
        amount: Double,
        mode: String,
        paymentDate: String = "",
        remarks: String = "",
        onComplete: (FeePaymentEntity?) -> Unit
    ) {
        val collectorId = _currentUser.value?.userId ?: "ADMIN"
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = if (paymentDate.isNotBlank()) paymentDate else sdfDate.format(Date())

        viewModelScope.launch {
            val receipt = repository.recordInstallmentPayment(
                installmentId = installmentId,
                amount = amount,
                paymentDate = date,
                paymentMode = mode,
                remarks = remarks,
                collectorId = collectorId
            )
            if (receipt != null) {
                showMessage("Receipt ${receipt.receiptNo} generated! Paid ₹${amount.toInt()} for ${receipt.studentName}")
            } else {
                showMessage("Failed to process installment payment", isError = true)
            }
            onComplete(receipt)
        }
    }

    // Add manual fee record (Admin and authorized Sub-Admin)
    fun addFeeRecord(
        studentName: String,
        admissionNo: String,
        course: String,
        amount: Double,
        mode: String,
        paymentType: String,
        installmentMonth: String,
        dueDate: String,
        remarks: String,
        onComplete: (FeePaymentEntity) -> Unit
    ) {
        val collectorId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            val receipt = repository.addManualFeeRecord(
                studentName = studentName,
                admissionNo = admissionNo,
                course = course,
                amount = amount,
                mode = mode,
                paymentType = paymentType,
                installmentMonth = installmentMonth,
                dueDate = dueDate,
                remarks = remarks,
                collectorId = collectorId
            )
            showMessage("Fee record saved! Receipt: ${receipt.receiptNo} (₹${amount.toInt()})")
            onComplete(receipt)
        }
    }

    fun editPayment(payment: FeePaymentEntity, onComplete: () -> Unit) {
        val modifierId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.updateFeePayment(payment, modifierId)
            showMessage("Payment receipt ${payment.receiptNo} updated")
            onComplete()
        }
    }

    fun deletePayment(paymentId: Long, onComplete: () -> Unit) {
        val modifierId = _currentUser.value?.userId ?: "ADMIN"
        viewModelScope.launch {
            repository.deleteFeePayment(paymentId, modifierId)
            showMessage("Payment record deleted and balance adjusted")
            onComplete()
        }
    }

    fun sendFeeReminder(student: StudentEntity, monthName: String, pendingAmount: Double) {
        val sender = _currentUser.value?.name ?: "Finance Dept"
        val title = "Fee Reminder: $monthName Installment"
        val message = "Dear ${student.name} (${student.admissionNo}), your installment of ₹${pendingAmount.toInt()} for $monthName is currently pending. Please settle at the accounts office."
        sendNotification(
            title = title,
            message = message,
            category = "Fee Reminder",
            targetType = "STUDENT",
            targetValue = student.admissionNo
        )
        showMessage("Fee reminder sent to ${student.name}!")
    }

    fun exportFeeLedgerExcel(
        context: Context,
        installments: List<FeeInstallmentEntity>,
        payments: List<FeePaymentEntity>
    ): File {
        val sheets = listOf(
            ExcelSheet(
                name = "Installments",
                titleBanner = "London Academy Connect - Fee Installments Schedule",
                headers = listOf(
                    "Student Name", "Admission No", "Course", "Month",
                    "Installment Due", "Amount Paid", "Pending Amount",
                    "Status", "Payment Mode", "Due Date"
                ),
                rows = installments.map { inst ->
                    listOf(
                        inst.studentName,
                        inst.admissionNo,
                        inst.courseName,
                        inst.monthName,
                        inst.installmentAmount,
                        inst.amountPaid,
                        inst.remainingAmount,
                        inst.status,
                        inst.paymentMode,
                        inst.dueDate
                    )
                }
            ),
            ExcelSheet(
                name = "Payments",
                titleBanner = "London Academy Connect - Fee Payments Ledger",
                headers = listOf(
                    "Receipt No", "Student Name", "Admission No", "Course",
                    "Payment Date", "Amount Paid", "Mode", "Plan", "Installment Month", "Collected By"
                ),
                rows = payments.map { pay ->
                    listOf(
                        pay.receiptNo,
                        pay.studentName,
                        pay.admissionNo,
                        pay.courseName,
                        pay.paymentDate,
                        pay.amount,
                        pay.paymentMode,
                        pay.paymentPlan,
                        pay.installmentMonth.ifBlank { "-" },
                        pay.collectedBy
                    )
                }
            )
        )
        val todayTag = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "London_Academy_Fees_Ledger_$todayTag",
            sheets = sheets
        )
    }

    // Send Notification
    fun sendNotification(
        title: String,
        message: String,
        category: String,
        targetType: String,
        targetValue: String = ""
    ) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val notif = AppNotificationEntity(
            title = title.trim(),
            message = message.trim(),
            category = category,
            targetType = targetType,
            targetValue = targetValue,
            date = sdfDate.format(now),
            time = sdfTime.format(now),
            sentBy = _currentUser.value?.name ?: "Super Admin"
        )
        viewModelScope.launch {
            repository.sendNotification(notif)
            showMessage("Notification sent to $targetType!")
        }
    }

    // Add Employee
    fun addEmployee(
        employeeId: String,
        name: String,
        contact: String,
        email: String,
        department: String,
        branchName: String = "Main Campus (Delhi)",
        doj: String,
        initialPass: String,
        canManageStudents: Boolean,
        canViewFees: Boolean
    ) {
        viewModelScope.launch {
            val existing = allEmployees.value.firstOrNull { it.employeeId.equals(employeeId.trim(), ignoreCase = true) }
            if (existing != null) {
                showMessage("Employee ID $employeeId already exists!", isError = true)
                return@launch
            }

            val emp = EmployeeEntity(
                employeeId = employeeId.trim(),
                name = name.trim(),
                contactNo = contact.trim(),
                email = email.trim(),
                department = department,
                branchName = branchName.trim().ifBlank { "Main Campus (Delhi)" },
                dateOfJoining = doj,
                canManageStudents = canManageStudents,
                canViewFees = canViewFees,
                canMarkAttendance = true
            )
            repository.insertEmployee(emp, initialPass)
            showMessage("Employee ${emp.name} created successfully in ${emp.branchName}!")
        }
    }

    fun updateEmployeeBranch(employee: EmployeeEntity, newBranchName: String) {
        viewModelScope.launch {
            repository.updateEmployee(employee.copy(branchName = newBranchName.trim().ifBlank { "Main Campus (Delhi)" }))
            showMessage("Updated branch for ${employee.name} to $newBranchName")
        }
    }

    // Update Office Settings
    fun updateOfficeSettings(
        name: String,
        lat: Double,
        lon: Double,
        radius: Double,
        weeklyOff: String,
        standardDailyHours: Double = officeSettings.value?.standardDailyHours ?: 8.0,
        standardStudentDailyHours: Double = officeSettings.value?.standardStudentDailyHours ?: 2.0
    ) {
        viewModelScope.launch {
            val settings = OfficeSettingsEntity(
                id = 1,
                officeName = name,
                latitude = lat,
                longitude = lon,
                allowedRadiusMeters = radius,
                weeklyOff = weeklyOff,
                standardDailyHours = standardDailyHours,
                standardStudentDailyHours = standardStudentDailyHours
            )
            repository.updateOfficeSettings(settings)
            showMessage("Office settings & standard working hours updated!")
        }
    }

    // Quick helper: Set Office Location to user's Current Location (great for live testing!)
    fun setOfficeToCurrentLocation() {
        val loc = _lastKnownLocation.value
        if (loc != null) {
            updateOfficeSettings(
                name = officeSettings.value?.officeName ?: "Main Campus",
                lat = loc.latitude,
                lon = loc.longitude,
                radius = officeSettings.value?.allowedRadiusMeters ?: 10.0,
                weeklyOff = officeSettings.value?.weeklyOff ?: "SUNDAY"
            )
            showMessage("Office GPS set to current coordinates (${String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)})!")
        } else {
            checkLocationAndDistance { _, _ ->
                _lastKnownLocation.value?.let { l ->
                    updateOfficeSettings(
                        name = "Main Campus",
                        lat = l.latitude,
                        lon = l.longitude,
                        radius = 10.0,
                        weeklyOff = "SUNDAY"
                    )
                }
            }
        }
    }

    // Add Holiday
    fun addHoliday(name: String, date: String, description: String) {
        viewModelScope.launch {
            val holiday = HolidayEntity(holidayName = name.trim(), date = date.trim(), description = description.trim())
            repository.insertHoliday(holiday)
            showMessage("Holiday '$name' added to calendar")
        }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch {
            repository.deleteHoliday(id)
            showMessage("Holiday removed")
        }
    }

    fun updateCategoryPermission(category: EmployeeCategoryEntity) {
        viewModelScope.launch {
            repository.updateEmployeeCategory(category)
            showMessage("Category permissions updated for ${category.categoryName}")
        }
    }

    // Export Reports to genuine Microsoft Excel (.xlsx) workbooks
    fun exportAttendanceReportExcel(
        context: Context,
        monthFilter: String = "",
        employeeId: String = ""
    ): File {
        val records = allEmployeeAttendance.value.filter {
            (monthFilter.isEmpty() || monthFilter == "ALL" || it.date.startsWith(monthFilter)) &&
                (employeeId.isEmpty() || it.employeeId == employeeId)
        }
        val periodLabel = monthFilter.ifBlank { "ALL" }
        val bannerSuffix = if (employeeId.isNotBlank()) "Staff: $employeeId ($periodLabel)" else "Period: $periodLabel"
        val sheet = ExcelSheet(
            name = "Staff Attendance",
            titleBanner = "London Academy Connect - Staff Attendance Log ($bannerSuffix)",
            headers = listOf(
                "Employee ID", "Employee Name", "Department", "Date",
                "Login Time", "Log Out Time", "Worked Hours", "Required Hours",
                "Full Hours Completed", "Status", "Notes", "Distance (m)"
            ),
            rows = records.map { rec ->
                val fullMet = if (rec.workedMinutes >= rec.requiredMinutes && (rec.status == "PRESENT" || rec.status == "HALF_DAY")) "YES" else "NO"
                listOf(
                    rec.employeeId,
                    rec.employeeName,
                    rec.department,
                    rec.date,
                    rec.time,
                    rec.checkOutTime.ifEmpty { "-" },
                    formatMinutes(rec.workedMinutes),
                    formatMinutes(rec.requiredMinutes),
                    fullMet,
                    rec.status,
                    rec.notes,
                    rec.distanceMeters.toInt()
                )
            }
        )
        val fileTag = if (employeeId.isNotBlank()) "Staff_${employeeId}_Attendance_$periodLabel" else "Staff_Attendance_$periodLabel"
        return ExcelExporter.writeWorkbookToFile(context, fileTag, listOf(sheet))
    }

    fun exportEmployeeSummaryExcel(
        context: Context,
        monthFilter: String = currentMonthKey()
    ): File {
        val summarySheet = ExcelSheet(
            name = "Staff Summary",
            titleBanner = "London Academy Connect - Staff Work Summary ($monthFilter)",
            headers = listOf(
                "Employee ID", "Name", "Department", "Branch",
                "Working Days", "Days Attended", "Full Present", "Half Days",
                "Full Hours Days", "Short Hours Days", "Total Worked",
                "Leaves Taken", "Absent Days", "Attendance %"
            ),
            rows = allEmployees.value.map { emp ->
                val r = getEmployeeWorkReport(emp, monthFilter)
                listOf(
                    emp.employeeId,
                    emp.name,
                    emp.department,
                    emp.branchName,
                    r.totalWorkingDays,
                    r.daysAttended,
                    r.presentDays,
                    r.halfDays,
                    r.fullHoursCompletedDays,
                    r.shortHoursDays,
                    formatMinutes(r.totalWorkedMinutes),
                    r.leaveDays,
                    r.absentDays,
                    "${r.attendancePercentage.toInt()}%"
                )
            }
        )
        val dailyRecords = allEmployeeAttendance.value.filter {
            monthFilter.isEmpty() || monthFilter == "ALL" || it.date.startsWith(monthFilter)
        }
        val dailySheet = ExcelSheet(
            name = "Daily Logs",
            titleBanner = "London Academy Connect - Staff Daily Attendance ($monthFilter)",
            headers = listOf(
                "Employee ID", "Employee Name", "Department", "Date",
                "Login", "Log Out", "Worked", "Required", "Status", "Notes"
            ),
            rows = dailyRecords.map { rec ->
                listOf(
                    rec.employeeId,
                    rec.employeeName,
                    rec.department,
                    rec.date,
                    rec.time,
                    rec.checkOutTime.ifEmpty { "-" },
                    formatMinutes(rec.workedMinutes),
                    formatMinutes(rec.requiredMinutes),
                    rec.status,
                    rec.notes
                )
            }
        )
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "Staff_Summary_Report_$monthFilter",
            sheets = listOf(summarySheet, dailySheet)
        )
    }

    fun exportStudentSummaryExcel(
        context: Context,
        monthFilter: String = currentMonthKey()
    ): File {
        val summarySheet = ExcelSheet(
            name = "Student Summary",
            titleBanner = "London Academy Connect - Complete Student Report ($monthFilter)",
            headers = listOf(
                "Admission No", "Name", "Course", "Batch", "Branch",
                "Class Days", "Days Attended", "Full Hours Days", "Total Study Hours",
                "Leaves Taken", "Absent Days", "Attendance %",
                "Fee Payments Count", "Period Paid", "Total Paid", "Pending Fees"
            ),
            rows = allStudents.value.map { stu ->
                val r = getStudentWorkReport(stu, monthFilter)
                listOf(
                    stu.admissionNo,
                    stu.name,
                    stu.courseName,
                    stu.batchName,
                    stu.branchName,
                    r.totalClassDays,
                    r.daysAttended,
                    r.fullHoursCompletedDays,
                    formatMinutes(r.totalWorkedMinutes),
                    r.leaveDays,
                    r.absentDays,
                    "${r.attendancePercentage.toInt()}%",
                    r.feePaymentCount,
                    r.periodFeesPaid,
                    r.totalFeesPaid,
                    r.pendingFees
                )
            }
        )
        val attendanceRecords = allStudentAttendance.value.filter {
            monthFilter.isEmpty() || monthFilter == "ALL" || it.date.startsWith(monthFilter)
        }
        val attendanceSheet = ExcelSheet(
            name = "Attendance Log",
            titleBanner = "London Academy Connect - Student Attendance ($monthFilter)",
            headers = listOf(
                "Admission No", "Student Name", "Course", "Batch",
                "Date", "Status", "Login", "Log Out", "Study Hours", "Notes"
            ),
            rows = attendanceRecords.map { rec ->
                listOf(
                    rec.admissionNo,
                    rec.studentName,
                    rec.courseName,
                    rec.batchName,
                    rec.date,
                    rec.status,
                    rec.checkInTime.ifEmpty { "-" },
                    rec.checkOutTime.ifEmpty { "-" },
                    formatMinutes(rec.workedMinutes),
                    rec.notes
                )
            }
        )
        val paymentsSheet = ExcelSheet(
            name = "Fee Payments",
            titleBanner = "London Academy Connect - Student Fee Payments ($monthFilter)",
            headers = listOf(
                "Receipt No", "Admission No", "Student Name", "Course",
                "Amount", "Payment Date", "Mode", "Plan", "Collected By"
            ),
            rows = allPayments.value.map { pay ->
                listOf(
                    pay.receiptNo,
                    pay.admissionNo,
                    pay.studentName,
                    pay.courseName,
                    pay.amount,
                    pay.paymentDate,
                    pay.paymentMode,
                    pay.paymentPlan,
                    pay.collectedBy
                )
            }
        )
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "Student_Complete_Report_$monthFilter",
            sheets = listOf(summarySheet, attendanceSheet, paymentsSheet)
        )
    }

    fun exportStudentDetailExcel(context: Context, report: StudentWorkReport): File {
        val overviewSheet = ExcelSheet(
            name = "Overview",
            titleBanner = "Student Report: ${report.student.name} (${report.student.admissionNo}) - ${report.monthFilter}",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Student Name", report.student.name),
                listOf("Admission No", report.student.admissionNo),
                listOf("Course", report.student.courseName),
                listOf("Batch", report.student.batchName),
                listOf("Branch", report.student.branchName),
                listOf("Period", report.monthFilter),
                listOf("Days Attended / Class Days", "${report.daysAttended} / ${report.totalClassDays}"),
                listOf("Full Hours Completed Days", report.fullHoursCompletedDays),
                listOf("Leaves Taken", report.leaveDays),
                listOf("Absent Days", report.absentDays),
                listOf("Attendance %", "${report.attendancePercentage.toInt()}%"),
                listOf("Total Study Time", formatMinutes(report.totalWorkedMinutes)),
                listOf("Fee Payments Count", report.feePaymentCount),
                listOf("Period Fees Paid", report.periodFeesPaid),
                listOf("Total Fees Paid", report.totalFeesPaid),
                listOf("Pending Fees", report.pendingFees)
            )
        )
        val attendanceSheet = ExcelSheet(
            name = "Attendance Log",
            titleBanner = "${report.student.name} - Attendance Log (${report.monthFilter})",
            headers = listOf("Date", "Status", "Login", "Log Out", "Study Hours", "Notes"),
            rows = report.records.map { rec ->
                listOf(
                    rec.date,
                    rec.status,
                    rec.checkInTime.ifEmpty { "-" },
                    rec.checkOutTime.ifEmpty { "-" },
                    formatMinutes(rec.workedMinutes),
                    rec.notes
                )
            }
        )
        val paymentsSheet = ExcelSheet(
            name = "Fee Payments",
            titleBanner = "${report.student.name} - Fee Payments",
            headers = listOf("Receipt No", "Payment Date", "Amount", "Mode", "Plan", "Collected By", "Remarks"),
            rows = report.payments.map { pay ->
                listOf(
                    pay.receiptNo,
                    pay.paymentDate,
                    pay.amount,
                    pay.paymentMode,
                    pay.paymentPlan,
                    pay.collectedBy,
                    pay.remarks
                )
            }
        )
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "Student_${report.student.admissionNo}_Report_${report.monthFilter}",
            sheets = listOf(overviewSheet, attendanceSheet, paymentsSheet)
        )
    }

    fun exportTeacherStudentAttendanceSummaryExcel(
        context: Context,
        students: List<StudentEntity>,
        monthFilter: String = currentMonthKey()
    ): File {
        val summarySheet = ExcelSheet(
            name = "Attendance Summary",
            titleBanner = "London Academy Connect - Student Attendance Summary ($monthFilter)",
            headers = listOf(
                "Admission No", "Name", "Course", "Batch", "Branch",
                "Class Days", "Days Attended", "Full Present", "Half Days",
                "Full Hours Days", "Total Study Hours", "Leaves Taken", "Absent Days", "Attendance %"
            ),
            rows = students.map { stu ->
                val r = getStudentWorkReport(stu, monthFilter)
                listOf(
                    stu.admissionNo,
                    stu.name,
                    stu.courseName,
                    stu.batchName,
                    stu.branchName,
                    r.totalClassDays,
                    r.daysAttended,
                    r.presentDays,
                    r.halfDays,
                    r.fullHoursCompletedDays,
                    formatMinutes(r.totalWorkedMinutes),
                    r.leaveDays,
                    r.absentDays,
                    "${r.attendancePercentage.toInt()}%"
                )
            }
        )
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "Student_Attendance_Summary_$monthFilter",
            sheets = listOf(summarySheet)
        )
    }

    fun exportTeacherStudentAttendanceDetailExcel(
        context: Context,
        report: StudentWorkReport
    ): File {
        val overviewSheet = ExcelSheet(
            name = "Overview",
            titleBanner = "Student Attendance: ${report.student.name} (${report.student.admissionNo}) - ${report.monthFilter}",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Student Name", report.student.name),
                listOf("Admission No", report.student.admissionNo),
                listOf("Course", report.student.courseName),
                listOf("Batch", report.student.batchName),
                listOf("Period", report.monthFilter),
                listOf("Days Attended / Class Days", "${report.daysAttended} / ${report.totalClassDays}"),
                listOf("Attendance %", "${report.attendancePercentage.toInt()}%"),
                listOf("Full Hours Days", report.fullHoursCompletedDays),
                listOf("Leaves Taken", report.leaveDays),
                listOf("Absent Days", report.absentDays),
                listOf("Total Study Time", formatMinutes(report.totalWorkedMinutes))
            )
        )
        val attendanceSheet = ExcelSheet(
            name = "Attendance Log",
            titleBanner = "${report.student.name} - Attendance Log (${report.monthFilter})",
            headers = listOf("Date", "Status", "Login", "Log Out", "Study Hours", "Notes"),
            rows = report.records.map { rec ->
                listOf(
                    rec.date,
                    rec.status,
                    rec.checkInTime.ifEmpty { "-" },
                    rec.checkOutTime.ifEmpty { "-" },
                    formatMinutes(rec.workedMinutes),
                    rec.notes
                )
            }
        )
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "Student_${report.student.admissionNo}_Attendance_${report.monthFilter}",
            sheets = listOf(overviewSheet, attendanceSheet)
        )
    }

    fun exportFeeReportExcel(context: Context): File {
        return exportFeeLedgerExcel(
            context = context,
            installments = allInstallments.value,
            payments = allPayments.value
        )
    }

    fun exportFeeReceiptExcel(
        context: Context,
        payment: FeePaymentEntity,
        student: StudentEntity?
    ): File {
        val totalCourseFee = student?.totalFees ?: 0.0
        val totalPaid = student?.paidFees ?: payment.amount
        val pendingFee = student?.pendingFees ?: 0.0
        val sheet = ExcelSheet(
            name = "Fee Receipt",
            titleBanner = "London Academy Connect - Official Fee Receipt (${payment.receiptNo})",
            headers = listOf("Field", "Details"),
            rows = listOf(
                listOf("Receipt No", payment.receiptNo),
                listOf("Student Name", payment.studentName),
                listOf("Admission No", payment.admissionNo),
                listOf("Course", payment.courseName.ifEmpty { student?.courseName ?: "London Academy Course" }),
                listOf("Installment Month", payment.installmentMonth.ifEmpty { "General Fee" }),
                listOf("Payment Date", payment.paymentDate),
                listOf("Amount Paid (Rs)", payment.amount),
                listOf("Payment Mode", payment.paymentMode),
                listOf("Plan", payment.paymentPlan),
                listOf("Total Course Fee (Rs)", totalCourseFee),
                listOf("Total Paid to Date (Rs)", totalPaid),
                listOf("Remaining Balance (Rs)", pendingFee),
                listOf("Next Due Date", payment.nextDueDate.ifEmpty { "-" }),
                listOf("Collected By", payment.collectedBy),
                listOf("Remarks", payment.remarks.ifEmpty { "None" })
            )
        )
        return ExcelExporter.writeWorkbookToFile(
            context = context,
            fileNamePrefix = "Fee_Receipt_${payment.receiptNo}",
            sheets = listOf(sheet)
        )
    }
}
