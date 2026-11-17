package com.example.data.firebase

import com.example.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

fun sanitizeFirestoreId(raw: String, fallbackPrefix: String = "doc"): String {
    val cleaned = raw.trim()
        .replace(Regex("[^a-zA-Z0-9_\\-]+"), "_")
        .trim('_')
    return if (cleaned.isBlank()) {
        "${fallbackPrefix}_${System.currentTimeMillis()}"
    } else {
        cleaned.take(120)
    }
}

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val activePortalRole: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBranch(
    @DocumentId val id: String = "",
    val branchName: String = "",
    val address: String? = null,
    val contactNo: String? = null,
    val addedBy: String = "ADMIN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BranchEntity = BranchEntity(
        branchName = branchName,
        address = address.orEmpty(),
        contactNo = contactNo.orEmpty(),
        addedBy = addedBy,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("branchName", branchName.take(100))
        if (!address.isNullOrBlank()) put("address", address.take(300))
        if (!contactNo.isNullOrBlank()) put("contactNo", contactNo.take(30))
        put("addedBy", addedBy.ifBlank { "ADMIN" }.take(80))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("branchName", branchName.take(100))
        if (!address.isNullOrBlank()) put("address", address.take(300))
        if (!contactNo.isNullOrBlank()) put("contactNo", contactNo.take(30))
        put("addedBy", addedBy.ifBlank { "ADMIN" }.take(80))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun BranchEntity.toFirestoreDoc(): FirestoreBranch = FirestoreBranch(
    id = sanitizeFirestoreId(branchName, "branch"),
    branchName = branchName,
    address = address.takeIf { it.isNotBlank() },
    contactNo = contactNo.takeIf { it.isNotBlank() },
    addedBy = addedBy.ifBlank { "ADMIN" }
)

@IgnoreExtraProperties
data class FirestoreCourse(
    @DocumentId val id: String = "",
    val courseName: String = "",
    val defaultDuration: String = "6 Months",
    val defaultFees: Double = 0.0,
    val addedBy: String = "ADMIN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): CourseEntity = CourseEntity(
        courseName = courseName,
        defaultDuration = defaultDuration,
        defaultFees = defaultFees,
        addedBy = addedBy,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "courseName" to courseName.take(120),
        "defaultDuration" to defaultDuration.ifBlank { "6 Months" }.take(40),
        "defaultFees" to defaultFees.coerceAtLeast(0.0),
        "addedBy" to addedBy.ifBlank { "ADMIN" }.take(80),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "courseName" to courseName.take(120),
        "defaultDuration" to defaultDuration.ifBlank { "6 Months" }.take(40),
        "defaultFees" to defaultFees.coerceAtLeast(0.0),
        "addedBy" to addedBy.ifBlank { "ADMIN" }.take(80),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

fun CourseEntity.toFirestoreDoc(): FirestoreCourse = FirestoreCourse(
    id = sanitizeFirestoreId(courseName, "course"),
    courseName = courseName,
    defaultDuration = defaultDuration,
    defaultFees = defaultFees,
    addedBy = addedBy.ifBlank { "ADMIN" }
)

@IgnoreExtraProperties
data class FirestoreEmployee(
    @DocumentId val id: String = "",
    val employeeId: String = "",
    val name: String = "",
    val contactNo: String? = null,
    val email: String? = null,
    val department: String = "",
    val branchName: String = "Main Campus (Delhi)",
    val dateOfJoining: String = "",
    val canManageStudents: Boolean = false,
    val canViewFees: Boolean = false,
    val canMarkAttendance: Boolean = true,
    val photoUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): EmployeeEntity = EmployeeEntity(
        employeeId = employeeId,
        name = name,
        contactNo = contactNo.orEmpty(),
        email = email.orEmpty(),
        department = department,
        branchName = branchName,
        dateOfJoining = dateOfJoining,
        canManageStudents = canManageStudents,
        canViewFees = canViewFees,
        canMarkAttendance = canMarkAttendance,
        photoUri = photoUri.orEmpty()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("employeeId", employeeId.take(80))
        put("name", name.take(100))
        if (!contactNo.isNullOrBlank()) put("contactNo", contactNo.take(30))
        if (!email.isNullOrBlank()) put("email", email.take(120))
        put("department", department.ifBlank { "Staff" }.take(80))
        put("branchName", branchName.ifBlank { "Main Campus (Delhi)" }.take(100))
        put("dateOfJoining", dateOfJoining.ifBlank { "2026-01-01" }.take(20))
        put("canManageStudents", canManageStudents)
        put("canViewFees", canViewFees)
        put("canMarkAttendance", canMarkAttendance)
        if (!photoUri.isNullOrBlank()) put("photoUri", photoUri.take(500))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("name", name.take(100))
        if (!contactNo.isNullOrBlank()) put("contactNo", contactNo.take(30))
        if (!email.isNullOrBlank()) put("email", email.take(120))
        put("department", department.ifBlank { "Staff" }.take(80))
        put("branchName", branchName.ifBlank { "Main Campus (Delhi)" }.take(100))
        put("dateOfJoining", dateOfJoining.ifBlank { "2026-01-01" }.take(20))
        put("canManageStudents", canManageStudents)
        put("canViewFees", canViewFees)
        put("canMarkAttendance", canMarkAttendance)
        if (!photoUri.isNullOrBlank()) put("photoUri", photoUri.take(500))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun EmployeeEntity.toFirestoreDoc(): FirestoreEmployee = FirestoreEmployee(
    id = sanitizeFirestoreId(employeeId, "emp"),
    employeeId = employeeId,
    name = name,
    contactNo = contactNo.takeIf { it.isNotBlank() },
    email = email.takeIf { it.isNotBlank() },
    department = department,
    branchName = branchName,
    dateOfJoining = dateOfJoining,
    canManageStudents = canManageStudents,
    canViewFees = canViewFees,
    canMarkAttendance = canMarkAttendance,
    photoUri = photoUri.takeIf { it.isNotBlank() }
)

@IgnoreExtraProperties
data class FirestoreStudent(
    @DocumentId val id: String = "",
    val admissionNo: String = "",
    val name: String = "",
    val contactNo: String? = null,
    val address: String? = null,
    val fatherName: String? = null,
    val motherName: String? = null,
    val studentPhotoUri: String? = null,
    val aadhaarCardUri: String? = null,
    val courseName: String = "",
    val branchName: String = "Main Campus (Delhi)",
    val batchName: String = "Batch A",
    val batchTiming: String? = null,
    val dateOfJoining: String = "",
    val courseDuration: String = "6 Months",
    val totalFees: Double = 0.0,
    val paidFees: Double = 0.0,
    val pendingFees: Double = 0.0,
    val paymentPlan: String = "MONTHLY",
    val installmentCount: Long = 1L,
    val assignedTeacherId: String = "",
    val assignedTeacherName: String = "",
    val portalUserId: String = "",
    val nextDueDate: String? = null,
    val monthlyFeeAmount: Double? = null,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): StudentEntity = StudentEntity(
        admissionNo = admissionNo,
        name = name,
        contactNo = contactNo.orEmpty(),
        address = address.orEmpty(),
        fatherName = fatherName.orEmpty(),
        motherName = motherName.orEmpty(),
        studentPhotoUri = studentPhotoUri.orEmpty(),
        aadhaarCardUri = aadhaarCardUri.orEmpty(),
        courseName = courseName,
        branchName = branchName,
        batchName = batchName,
        batchTiming = batchTiming.orEmpty(),
        dateOfJoining = dateOfJoining,
        courseDuration = courseDuration,
        totalFees = totalFees,
        paidFees = paidFees,
        pendingFees = pendingFees,
        paymentPlan = paymentPlan,
        installmentCount = installmentCount.toInt().coerceAtLeast(1),
        assignedTeacherId = assignedTeacherId,
        assignedTeacherName = assignedTeacherName,
        userId = portalUserId,
        nextDueDate = nextDueDate.orEmpty(),
        monthlyFeeAmount = monthlyFeeAmount ?: 0.0,
        status = status
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("admissionNo", admissionNo.take(60))
        put("name", name.take(100))
        if (!contactNo.isNullOrBlank()) put("contactNo", contactNo.take(30))
        if (!address.isNullOrBlank()) put("address", address.take(300))
        if (!fatherName.isNullOrBlank()) put("fatherName", fatherName.take(100))
        if (!motherName.isNullOrBlank()) put("motherName", motherName.take(100))
        if (!studentPhotoUri.isNullOrBlank()) put("studentPhotoUri", studentPhotoUri.take(500))
        if (!aadhaarCardUri.isNullOrBlank()) put("aadhaarCardUri", aadhaarCardUri.take(500))
        put("courseName", courseName.ifBlank { "Course" }.take(120))
        put("branchName", branchName.ifBlank { "Main Campus (Delhi)" }.take(100))
        put("batchName", batchName.ifBlank { "Batch A" }.take(80))
        if (!batchTiming.isNullOrBlank()) put("batchTiming", batchTiming.take(60))
        put("dateOfJoining", dateOfJoining.ifBlank { "2026-01-01" }.take(20))
        put("courseDuration", courseDuration.ifBlank { "6 Months" }.take(40))
        put("totalFees", totalFees.coerceAtLeast(0.0))
        put("paidFees", paidFees.coerceAtLeast(0.0))
        put("pendingFees", pendingFees.coerceAtLeast(0.0))
        put("paymentPlan", paymentPlan.ifBlank { "MONTHLY" }.take(40))
        put("installmentCount", installmentCount.coerceIn(1L, 60L))
        put("assignedTeacherId", assignedTeacherId.ifBlank { "trainer1" }.take(80))
        put("assignedTeacherName", assignedTeacherName.ifBlank { "Staff" }.take(100))
        put("portalUserId", portalUserId.ifBlank { admissionNo }.take(80))
        if (!nextDueDate.isNullOrBlank()) put("nextDueDate", nextDueDate.take(20))
        if (monthlyFeeAmount != null && monthlyFeeAmount >= 0.0) put("monthlyFeeAmount", monthlyFeeAmount)
        put("status", if (status in listOf("ACTIVE", "INACTIVE", "COMPLETED")) status else "ACTIVE")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("name", name.take(100))
        if (!contactNo.isNullOrBlank()) put("contactNo", contactNo.take(30))
        if (!address.isNullOrBlank()) put("address", address.take(300))
        if (!fatherName.isNullOrBlank()) put("fatherName", fatherName.take(100))
        if (!motherName.isNullOrBlank()) put("motherName", motherName.take(100))
        if (!studentPhotoUri.isNullOrBlank()) put("studentPhotoUri", studentPhotoUri.take(500))
        if (!aadhaarCardUri.isNullOrBlank()) put("aadhaarCardUri", aadhaarCardUri.take(500))
        put("courseName", courseName.ifBlank { "Course" }.take(120))
        put("branchName", branchName.ifBlank { "Main Campus (Delhi)" }.take(100))
        put("batchName", batchName.ifBlank { "Batch A" }.take(80))
        if (!batchTiming.isNullOrBlank()) put("batchTiming", batchTiming.take(60))
        put("dateOfJoining", dateOfJoining.ifBlank { "2026-01-01" }.take(20))
        put("courseDuration", courseDuration.ifBlank { "6 Months" }.take(40))
        put("totalFees", totalFees.coerceAtLeast(0.0))
        put("paidFees", paidFees.coerceAtLeast(0.0))
        put("pendingFees", pendingFees.coerceAtLeast(0.0))
        put("paymentPlan", paymentPlan.ifBlank { "MONTHLY" }.take(40))
        put("installmentCount", installmentCount.coerceIn(1L, 60L))
        put("assignedTeacherId", assignedTeacherId.ifBlank { "trainer1" }.take(80))
        put("assignedTeacherName", assignedTeacherName.ifBlank { "Staff" }.take(100))
        put("portalUserId", portalUserId.ifBlank { admissionNo }.take(80))
        if (!nextDueDate.isNullOrBlank()) put("nextDueDate", nextDueDate.take(20))
        if (monthlyFeeAmount != null && monthlyFeeAmount >= 0.0) put("monthlyFeeAmount", monthlyFeeAmount)
        put("status", if (status in listOf("ACTIVE", "INACTIVE", "COMPLETED")) status else "ACTIVE")
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun StudentEntity.toFirestoreDoc(): FirestoreStudent = FirestoreStudent(
    id = sanitizeFirestoreId(admissionNo, "stu"),
    admissionNo = admissionNo,
    name = name,
    contactNo = contactNo.takeIf { it.isNotBlank() },
    address = address.takeIf { it.isNotBlank() },
    fatherName = fatherName.takeIf { it.isNotBlank() },
    motherName = motherName.takeIf { it.isNotBlank() },
    studentPhotoUri = studentPhotoUri.takeIf { it.isNotBlank() },
    aadhaarCardUri = aadhaarCardUri.takeIf { it.isNotBlank() },
    courseName = courseName,
    branchName = branchName,
    batchName = batchName,
    batchTiming = batchTiming.takeIf { it.isNotBlank() },
    dateOfJoining = dateOfJoining,
    courseDuration = courseDuration,
    totalFees = totalFees,
    paidFees = paidFees,
    pendingFees = pendingFees,
    paymentPlan = paymentPlan,
    installmentCount = installmentCount.toLong().coerceAtLeast(1L),
    assignedTeacherId = assignedTeacherId,
    assignedTeacherName = assignedTeacherName,
    portalUserId = userId,
    nextDueDate = nextDueDate.takeIf { it.isNotBlank() },
    monthlyFeeAmount = monthlyFeeAmount.takeIf { it >= 0.0 },
    status = status
)

@IgnoreExtraProperties
data class FirestoreEmployeeAttendance(
    @DocumentId val id: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val department: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "PRESENT",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val selfieUri: String? = null,
    val isMockLocation: Boolean = false,
    val deviceModel: String? = null,
    val checkOutTime: String? = null,
    val workedMinutes: Long = 0L,
    val requiredMinutes: Long = 480L,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): EmployeeAttendanceEntity = EmployeeAttendanceEntity(
        employeeId = employeeId,
        employeeName = employeeName,
        department = department,
        date = date,
        time = time,
        status = status,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters,
        selfieUri = selfieUri.orEmpty(),
        isMockLocation = isMockLocation,
        deviceModel = deviceModel ?: "Android Device",
        checkOutTime = checkOutTime.orEmpty(),
        workedMinutes = workedMinutes.toInt(),
        requiredMinutes = requiredMinutes.toInt(),
        overtimeMinutes = 0,
        notes = notes.orEmpty()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("employeeId", employeeId.take(80))
        put("employeeName", employeeName.take(100))
        put("department", department.ifBlank { "Staff" }.take(80))
        put("date", date.take(20))
        put("time", time.take(20))
        put("status", if (status in listOf("PRESENT", "ABSENT", "HALF_DAY", "LEAVE")) status else "PRESENT")
        put("latitude", latitude)
        put("longitude", longitude)
        put("distanceMeters", distanceMeters.coerceAtLeast(0.0))
        if (!selfieUri.isNullOrBlank()) put("selfieUri", selfieUri.take(500))
        put("isMockLocation", isMockLocation)
        if (!deviceModel.isNullOrBlank()) put("deviceModel", deviceModel.take(120))
        if (!checkOutTime.isNullOrBlank()) put("checkOutTime", checkOutTime.take(20))
        put("workedMinutes", workedMinutes.coerceAtLeast(0L))
        put("requiredMinutes", requiredMinutes.coerceAtLeast(0L))
        if (!notes.isNullOrBlank()) put("notes", notes.take(300))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("employeeName", employeeName.take(100))
        put("department", department.ifBlank { "Staff" }.take(80))
        put("time", time.take(20))
        put("status", if (status in listOf("PRESENT", "ABSENT", "HALF_DAY", "LEAVE")) status else "PRESENT")
        put("latitude", latitude)
        put("longitude", longitude)
        put("distanceMeters", distanceMeters.coerceAtLeast(0.0))
        if (!selfieUri.isNullOrBlank()) put("selfieUri", selfieUri.take(500))
        put("isMockLocation", isMockLocation)
        if (!deviceModel.isNullOrBlank()) put("deviceModel", deviceModel.take(120))
        if (!checkOutTime.isNullOrBlank()) put("checkOutTime", checkOutTime.take(20))
        put("workedMinutes", workedMinutes.coerceAtLeast(0L))
        put("requiredMinutes", requiredMinutes.coerceAtLeast(0L))
        if (!notes.isNullOrBlank()) put("notes", notes.take(300))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun EmployeeAttendanceEntity.toFirestoreDoc(): FirestoreEmployeeAttendance = FirestoreEmployeeAttendance(
    id = sanitizeFirestoreId("${employeeId}_${date}", "empatt"),
    employeeId = employeeId,
    employeeName = employeeName,
    department = department,
    date = date,
    time = time,
    status = status,
    latitude = latitude,
    longitude = longitude,
    distanceMeters = distanceMeters,
    selfieUri = selfieUri.takeIf { it.isNotBlank() },
    isMockLocation = isMockLocation,
    deviceModel = deviceModel.takeIf { it.isNotBlank() },
    checkOutTime = checkOutTime.takeIf { it.isNotBlank() },
    workedMinutes = workedMinutes.toLong(),
    requiredMinutes = requiredMinutes.toLong(),
    notes = notes.takeIf { it.isNotBlank() }
)

@IgnoreExtraProperties
data class FirestoreStudentAttendance(
    @DocumentId val id: String = "",
    val admissionNo: String = "",
    val studentName: String = "",
    val courseName: String = "",
    val batchName: String = "",
    val date: String = "",
    val status: String = "PRESENT",
    val markedByUserId: String = "",
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val workedMinutes: Long = 0L,
    val requiredMinutes: Long = 120L,
    val notes: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val selfieUri: String? = null,
    val isMockLocation: Boolean = false,
    val deviceModel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): StudentAttendanceEntity = StudentAttendanceEntity(
        admissionNo = admissionNo,
        studentName = studentName,
        courseName = courseName,
        batchName = batchName,
        date = date,
        status = status,
        markedByUserId = markedByUserId,
        checkInTime = checkInTime.orEmpty(),
        checkOutTime = checkOutTime.orEmpty(),
        workedMinutes = workedMinutes.toInt(),
        requiredMinutes = requiredMinutes.toInt(),
        overtimeMinutes = 0,
        notes = notes.orEmpty(),
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters,
        selfieUri = selfieUri.orEmpty(),
        isMockLocation = isMockLocation,
        deviceModel = deviceModel.orEmpty()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("admissionNo", admissionNo.take(60))
        put("studentName", studentName.take(100))
        put("courseName", courseName.ifBlank { "Course" }.take(120))
        put("batchName", batchName.ifBlank { "Batch A" }.take(80))
        put("date", date.take(20))
        put("status", if (status in listOf("PRESENT", "ABSENT", "HALF_DAY", "LEAVE")) status else "PRESENT")
        put("markedByUserId", markedByUserId.ifBlank { "STAFF" }.take(80))
        if (!checkInTime.isNullOrBlank()) put("checkInTime", checkInTime.take(20))
        if (!checkOutTime.isNullOrBlank()) put("checkOutTime", checkOutTime.take(20))
        put("workedMinutes", workedMinutes.coerceAtLeast(0L))
        put("requiredMinutes", requiredMinutes.coerceAtLeast(0L))
        if (!notes.isNullOrBlank()) put("notes", notes.take(300))
        put("latitude", latitude)
        put("longitude", longitude)
        put("distanceMeters", distanceMeters.coerceAtLeast(0.0))
        if (!selfieUri.isNullOrBlank()) put("selfieUri", selfieUri.take(500))
        put("isMockLocation", isMockLocation)
        if (!deviceModel.isNullOrBlank()) put("deviceModel", deviceModel.take(120))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("studentName", studentName.take(100))
        put("courseName", courseName.ifBlank { "Course" }.take(120))
        put("batchName", batchName.ifBlank { "Batch A" }.take(80))
        put("status", if (status in listOf("PRESENT", "ABSENT", "HALF_DAY", "LEAVE")) status else "PRESENT")
        put("markedByUserId", markedByUserId.ifBlank { "STAFF" }.take(80))
        if (!checkInTime.isNullOrBlank()) put("checkInTime", checkInTime.take(20))
        if (!checkOutTime.isNullOrBlank()) put("checkOutTime", checkOutTime.take(20))
        put("workedMinutes", workedMinutes.coerceAtLeast(0L))
        put("requiredMinutes", requiredMinutes.coerceAtLeast(0L))
        if (!notes.isNullOrBlank()) put("notes", notes.take(300))
        put("latitude", latitude)
        put("longitude", longitude)
        put("distanceMeters", distanceMeters.coerceAtLeast(0.0))
        if (!selfieUri.isNullOrBlank()) put("selfieUri", selfieUri.take(500))
        put("isMockLocation", isMockLocation)
        if (!deviceModel.isNullOrBlank()) put("deviceModel", deviceModel.take(120))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun StudentAttendanceEntity.toFirestoreDoc(): FirestoreStudentAttendance = FirestoreStudentAttendance(
    id = sanitizeFirestoreId("${admissionNo}_${date}", "stuatt"),
    admissionNo = admissionNo,
    studentName = studentName,
    courseName = courseName,
    batchName = batchName,
    date = date,
    status = status,
    markedByUserId = markedByUserId,
    checkInTime = checkInTime.takeIf { it.isNotBlank() },
    checkOutTime = checkOutTime.takeIf { it.isNotBlank() },
    workedMinutes = workedMinutes.toLong(),
    requiredMinutes = requiredMinutes.toLong(),
    notes = notes.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude,
    distanceMeters = distanceMeters,
    selfieUri = selfieUri.takeIf { it.isNotBlank() },
    isMockLocation = isMockLocation,
    deviceModel = deviceModel.takeIf { it.isNotBlank() }
)

@IgnoreExtraProperties
data class FirestoreFeePayment(
    @DocumentId val id: String = "",
    val receiptNo: String = "",
    val admissionNo: String = "",
    val studentName: String = "",
    val amount: Double = 0.0,
    val paymentDate: String = "",
    val paymentMode: String = "",
    val paymentPlan: String = "",
    val installmentMonth: String? = null,
    val courseName: String? = null,
    val nextDueDate: String? = null,
    val collectedBy: String = "",
    val remarks: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): FeePaymentEntity = FeePaymentEntity(
        receiptNo = receiptNo,
        admissionNo = admissionNo,
        studentName = studentName,
        amount = amount,
        paymentDate = paymentDate,
        paymentMode = paymentMode,
        paymentPlan = paymentPlan,
        installmentMonth = installmentMonth.orEmpty(),
        courseName = courseName.orEmpty(),
        nextDueDate = nextDueDate.orEmpty(),
        collectedBy = collectedBy,
        remarks = remarks.orEmpty()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("receiptNo", receiptNo.take(60))
        put("admissionNo", admissionNo.take(60))
        put("studentName", studentName.take(100))
        put("amount", if (amount > 0.0) amount else 1.0)
        put("paymentDate", paymentDate.ifBlank { "2026-01-01" }.take(20))
        put("paymentMode", paymentMode.ifBlank { "Cash" }.take(50))
        put("paymentPlan", paymentPlan.ifBlank { "MONTHLY" }.take(40))
        if (!installmentMonth.isNullOrBlank()) put("installmentMonth", installmentMonth.take(80))
        if (!courseName.isNullOrBlank()) put("courseName", courseName.take(120))
        if (!nextDueDate.isNullOrBlank()) put("nextDueDate", nextDueDate.take(20))
        put("collectedBy", collectedBy.ifBlank { "ADMIN" }.take(80))
        if (!remarks.isNullOrBlank()) put("remarks", remarks.take(300))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("studentName", studentName.take(100))
        put("amount", if (amount > 0.0) amount else 1.0)
        put("paymentDate", paymentDate.ifBlank { "2026-01-01" }.take(20))
        put("paymentMode", paymentMode.ifBlank { "Cash" }.take(50))
        put("paymentPlan", paymentPlan.ifBlank { "MONTHLY" }.take(40))
        if (!installmentMonth.isNullOrBlank()) put("installmentMonth", installmentMonth.take(80))
        if (!courseName.isNullOrBlank()) put("courseName", courseName.take(120))
        if (!nextDueDate.isNullOrBlank()) put("nextDueDate", nextDueDate.take(20))
        put("collectedBy", collectedBy.ifBlank { "ADMIN" }.take(80))
        if (!remarks.isNullOrBlank()) put("remarks", remarks.take(300))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun FeePaymentEntity.toFirestoreDoc(): FirestoreFeePayment = FirestoreFeePayment(
    id = sanitizeFirestoreId(receiptNo, "rec"),
    receiptNo = receiptNo,
    admissionNo = admissionNo,
    studentName = studentName,
    amount = amount,
    paymentDate = paymentDate,
    paymentMode = paymentMode,
    paymentPlan = paymentPlan,
    installmentMonth = installmentMonth.takeIf { it.isNotBlank() },
    courseName = courseName.takeIf { it.isNotBlank() },
    nextDueDate = nextDueDate.takeIf { it.isNotBlank() },
    collectedBy = collectedBy,
    remarks = remarks.takeIf { it.isNotBlank() }
)

@IgnoreExtraProperties
data class FirestoreFeeInstallment(
    @DocumentId val id: String = "",
    val admissionNo: String = "",
    val studentName: String = "",
    val courseName: String = "",
    val monthName: String = "",
    val installmentNumber: Long = 1L,
    val dueDate: String = "",
    val installmentAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val status: String = "PENDING",
    val paymentMode: String? = null,
    val lastPaymentDate: String? = null,
    val remarks: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): FeeInstallmentEntity = FeeInstallmentEntity(
        admissionNo = admissionNo,
        studentName = studentName,
        courseName = courseName,
        monthName = monthName,
        installmentNumber = installmentNumber.toInt(),
        dueDate = dueDate,
        installmentAmount = installmentAmount,
        amountPaid = amountPaid,
        remainingAmount = remainingAmount,
        status = status,
        paymentMode = paymentMode.orEmpty(),
        lastPaymentDate = lastPaymentDate.orEmpty(),
        remarks = remarks.orEmpty()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("admissionNo", admissionNo.take(60))
        put("studentName", studentName.take(100))
        put("courseName", courseName.ifBlank { "Course" }.take(120))
        put("monthName", monthName.ifBlank { "Installment" }.take(80))
        put("installmentNumber", installmentNumber.coerceIn(1L, 60L))
        put("dueDate", dueDate.ifBlank { "2026-01-01" }.take(20))
        put("installmentAmount", installmentAmount.coerceAtLeast(0.0))
        put("amountPaid", amountPaid.coerceAtLeast(0.0))
        put("remainingAmount", remainingAmount.coerceAtLeast(0.0))
        put("status", if (status in listOf("PAID", "PARTIAL", "PENDING", "OVERDUE")) status else "PENDING")
        if (!paymentMode.isNullOrBlank()) put("paymentMode", paymentMode.take(50))
        if (!lastPaymentDate.isNullOrBlank()) put("lastPaymentDate", lastPaymentDate.take(20))
        if (!remarks.isNullOrBlank()) put("remarks", remarks.take(300))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("studentName", studentName.take(100))
        put("courseName", courseName.ifBlank { "Course" }.take(120))
        put("monthName", monthName.ifBlank { "Installment" }.take(80))
        put("dueDate", dueDate.ifBlank { "2026-01-01" }.take(20))
        put("installmentAmount", installmentAmount.coerceAtLeast(0.0))
        put("amountPaid", amountPaid.coerceAtLeast(0.0))
        put("remainingAmount", remainingAmount.coerceAtLeast(0.0))
        put("status", if (status in listOf("PAID", "PARTIAL", "PENDING", "OVERDUE")) status else "PENDING")
        if (!paymentMode.isNullOrBlank()) put("paymentMode", paymentMode.take(50))
        if (!lastPaymentDate.isNullOrBlank()) put("lastPaymentDate", lastPaymentDate.take(20))
        if (!remarks.isNullOrBlank()) put("remarks", remarks.take(300))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

fun FeeInstallmentEntity.toFirestoreDoc(): FirestoreFeeInstallment = FirestoreFeeInstallment(
    id = sanitizeFirestoreId("${admissionNo}_inst_${installmentNumber}_${monthName}", "inst"),
    admissionNo = admissionNo,
    studentName = studentName,
    courseName = courseName,
    monthName = monthName,
    installmentNumber = installmentNumber.toLong().coerceAtLeast(1L),
    dueDate = dueDate,
    installmentAmount = installmentAmount,
    amountPaid = amountPaid,
    remainingAmount = remainingAmount,
    status = status,
    paymentMode = paymentMode.takeIf { it.isNotBlank() },
    lastPaymentDate = lastPaymentDate.takeIf { it.isNotBlank() },
    remarks = remarks.takeIf { it.isNotBlank() }
)

@IgnoreExtraProperties
data class FirestoreAppNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "",
    val targetType: String = "",
    val targetValue: String? = null,
    val date: String = "",
    val time: String = "",
    val sentBy: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): AppNotificationEntity = AppNotificationEntity(
        title = title,
        message = message,
        category = category,
        targetType = targetType,
        targetValue = targetValue.orEmpty(),
        date = date,
        time = time,
        sentBy = sentBy
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("title", title.take(160))
        put("message", message.take(1000))
        put("category", category.ifBlank { "Announcement" }.take(60))
        put("targetType", targetType.ifBlank { "ALL_STUDENTS" }.take(60))
        if (!targetValue.isNullOrBlank()) put("targetValue", targetValue.take(120))
        put("date", date.ifBlank { "2026-01-01" }.take(20))
        put("time", time.ifBlank { "09:00:00" }.take(20))
        put("sentBy", sentBy.ifBlank { "Admin" }.take(100))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class FirestoreHoliday(
    @DocumentId val id: String = "",
    val holidayName: String = "",
    val date: String = "",
    val description: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): HolidayEntity = HolidayEntity(
        holidayName = holidayName,
        date = date,
        description = description.orEmpty()
    )

    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("holidayName", holidayName.take(120))
        put("date", date.take(20))
        if (!description.isNullOrBlank()) put("description", description.take(300))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class FirestoreOfficeSettings(
    @DocumentId val id: String = "",
    val officeName: String = "London Academy Campus",
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val allowedRadiusMeters: Double = 10.0,
    val weeklyOff: String = "SUNDAY",
    val standardDailyHours: Double = 8.0,
    val standardStudentDailyHours: Double = 2.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): OfficeSettingsEntity = OfficeSettingsEntity(
        id = 1,
        officeName = officeName,
        latitude = latitude,
        longitude = longitude,
        allowedRadiusMeters = allowedRadiusMeters,
        weeklyOff = weeklyOff,
        standardDailyHours = standardDailyHours,
        standardStudentDailyHours = standardStudentDailyHours
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "officeName" to officeName.ifBlank { "London Academy Campus" }.take(120),
        "latitude" to latitude,
        "longitude" to longitude,
        "allowedRadiusMeters" to allowedRadiusMeters.coerceAtLeast(1.0),
        "weeklyOff" to if (weeklyOff in listOf("SUNDAY", "SAT_SUN")) weeklyOff else "SUNDAY",
        "standardDailyHours" to standardDailyHours.coerceAtLeast(0.5),
        "standardStudentDailyHours" to standardStudentDailyHours.coerceAtLeast(0.5),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "officeName" to officeName.ifBlank { "London Academy Campus" }.take(120),
        "latitude" to latitude,
        "longitude" to longitude,
        "allowedRadiusMeters" to allowedRadiusMeters.coerceAtLeast(1.0),
        "weeklyOff" to if (weeklyOff in listOf("SUNDAY", "SAT_SUN")) weeklyOff else "SUNDAY",
        "standardDailyHours" to standardDailyHours.coerceAtLeast(0.5),
        "standardStudentDailyHours" to standardStudentDailyHours.coerceAtLeast(0.5),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

fun OfficeSettingsEntity.toFirestoreDoc(): FirestoreOfficeSettings = FirestoreOfficeSettings(
    id = "default",
    officeName = officeName,
    latitude = latitude,
    longitude = longitude,
    allowedRadiusMeters = allowedRadiusMeters,
    weeklyOff = weeklyOff,
    standardDailyHours = standardDailyHours,
    standardStudentDailyHours = standardStudentDailyHours
)
