package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreAcademyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String? = null,
        activePortalRole: String? = null
    ): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = buildMap<String, Any?> {
                    put("userId", userId)
                    put("displayName", displayName.ifBlank { "Academy User" }.take(100))
                    put("email", email.take(120))
                    if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl.take(500))
                    if (!activePortalRole.isNullOrBlank()) put("activePortalRole", activePortalRole)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            } else {
                val updatePayload = buildMap<String, Any?> {
                    put("displayName", displayName.ifBlank { "Academy User" }.take(100))
                    put("email", email.take(120))
                    if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl.take(500))
                    if (!activePortalRole.isNullOrBlank()) put("activePortalRole", activePortalRole)
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null } as Map<String, Any>
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateActivePortalRole(userId: String, role: String): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            docRef.update(
                mapOf(
                    "activePortalRole" to role,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> =
        userDoc(userId).snapshots().map { snapshot ->
            snapshot.toObject(FirestoreUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    // Branches
    fun observeBranches(userId: String): Flow<List<FirestoreBranch>> =
        userDoc(userId).collection("branches").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreBranch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveBranch(userId: String, branch: BranchEntity): Result<String> {
        val docModel = branch.toFirestoreDoc()
        val docRef = userDoc(userId).collection("branches").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBranch(userId: String, branchName: String): Result<Unit> {
        val docId = sanitizeFirestoreId(branchName, "branch")
        val docRef = userDoc(userId).collection("branches").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Courses
    fun observeCourses(userId: String): Flow<List<FirestoreCourse>> =
        userDoc(userId).collection("courses").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreCourse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveCourse(userId: String, course: CourseEntity): Result<String> {
        val docModel = course.toFirestoreDoc()
        val docRef = userDoc(userId).collection("courses").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(userId: String, courseName: String): Result<Unit> {
        val docId = sanitizeFirestoreId(courseName, "course")
        val docRef = userDoc(userId).collection("courses").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Employees
    fun observeEmployees(userId: String): Flow<List<FirestoreEmployee>> =
        userDoc(userId).collection("employees").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreEmployee::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveEmployee(userId: String, employee: EmployeeEntity): Result<String> {
        val docModel = employee.toFirestoreDoc()
        val docRef = userDoc(userId).collection("employees").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(userId: String, employeeId: String): Result<Unit> {
        val docId = sanitizeFirestoreId(employeeId, "emp")
        val docRef = userDoc(userId).collection("employees").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Students
    fun observeStudents(userId: String): Flow<List<FirestoreStudent>> =
        userDoc(userId).collection("students").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreStudent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun getStudentByAdmissionNo(userId: String, admissionNo: String): Result<FirestoreStudent?> {
        val docId = sanitizeFirestoreId(admissionNo, "stu")
        val docRef = userDoc(userId).collection("students").document(docId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(FirestoreStudent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveStudent(userId: String, student: StudentEntity): Result<String> {
        val docModel = student.toFirestoreDoc()
        val docRef = userDoc(userId).collection("students").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(userId: String, admissionNo: String): Result<Unit> {
        val docId = sanitizeFirestoreId(admissionNo, "stu")
        val docRef = userDoc(userId).collection("students").document(docId)
        return try {
            docRef.delete().await()
            val instSnap = userDoc(userId).collection("fee_installments")
                .whereEqualTo("admissionNo", admissionNo)
                .get()
                .await()
            instSnap.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Employee Attendance
    fun observeEmployeeAttendance(userId: String): Flow<List<FirestoreEmployeeAttendance>> =
        userDoc(userId).collection("employee_attendance").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreEmployeeAttendance::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveEmployeeAttendance(userId: String, record: EmployeeAttendanceEntity): Result<String> {
        val docModel = record.toFirestoreDoc()
        val docRef = userDoc(userId).collection("employee_attendance").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Student Attendance
    fun observeStudentAttendance(userId: String): Flow<List<FirestoreStudentAttendance>> =
        userDoc(userId).collection("student_attendance").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreStudentAttendance::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveStudentAttendance(userId: String, record: StudentAttendanceEntity): Result<String> {
        val docModel = record.toFirestoreDoc()
        val docRef = userDoc(userId).collection("student_attendance").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Fee Payments
    fun observeFeePayments(userId: String): Flow<List<FirestoreFeePayment>> =
        userDoc(userId).collection("fee_payments").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreFeePayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveFeePayment(userId: String, payment: FeePaymentEntity): Result<String> {
        val docModel = payment.toFirestoreDoc()
        val docRef = userDoc(userId).collection("fee_payments").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteFeePayment(userId: String, receiptNo: String): Result<Unit> {
        val docId = sanitizeFirestoreId(receiptNo, "rec")
        val docRef = userDoc(userId).collection("fee_payments").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Fee Installments
    fun observeFeeInstallments(userId: String): Flow<List<FirestoreFeeInstallment>> =
        userDoc(userId).collection("fee_installments").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreFeeInstallment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveFeeInstallment(userId: String, installment: FeeInstallmentEntity): Result<String> {
        val docModel = installment.toFirestoreDoc()
        val docRef = userDoc(userId).collection("fee_installments").document(docModel.id)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(docModel.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Notifications
    fun observeNotifications(userId: String): Flow<List<FirestoreAppNotification>> =
        userDoc(userId).collection("notifications").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreAppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveNotification(userId: String, notification: AppNotificationEntity): Result<String> {
        val docId = sanitizeFirestoreId("${notification.date}_${notification.time}_${notification.title.take(24)}", "notif")
        val docRef = userDoc(userId).collection("notifications").document(docId)
        val docModel = FirestoreAppNotification(
            id = docId,
            title = notification.title,
            message = notification.message,
            category = notification.category,
            targetType = notification.targetType,
            targetValue = notification.targetValue.takeIf { it.isNotBlank() },
            date = notification.date,
            time = notification.time,
            sentBy = notification.sentBy
        )
        return try {
            docRef.set(docModel.toCreateMap()).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // Holidays
    fun observeHolidays(userId: String): Flow<List<FirestoreHoliday>> =
        userDoc(userId).collection("holidays").snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreHoliday::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveHoliday(userId: String, holiday: HolidayEntity): Result<String> {
        val docId = sanitizeFirestoreId("${holiday.date}_${holiday.holidayName}", "hol")
        val docRef = userDoc(userId).collection("holidays").document(docId)
        val docModel = FirestoreHoliday(
            id = docId,
            holidayName = holiday.holidayName,
            date = holiday.date,
            description = holiday.description.takeIf { it.isNotBlank() }
        )
        return try {
            docRef.set(docModel.toCreateMap()).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHoliday(userId: String, holiday: HolidayEntity): Result<Unit> {
        val docId = sanitizeFirestoreId("${holiday.date}_${holiday.holidayName}", "hol")
        val docRef = userDoc(userId).collection("holidays").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Office Settings
    fun observeOfficeSettings(userId: String): Flow<FirestoreOfficeSettings?> =
        userDoc(userId).collection("office_settings").document("default").snapshots().map { snapshot ->
            snapshot.toObject(FirestoreOfficeSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun saveOfficeSettings(userId: String, settings: OfficeSettingsEntity): Result<Unit> {
        val docModel = settings.toFirestoreDoc()
        val docRef = userDoc(userId).collection("office_settings").document("default")
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Append-only Audit Logs
    suspend fun logAudit(
        userId: String,
        actorId: String,
        role: String,
        action: String,
        details: String,
        date: String,
        time: String,
        deviceInfo: String = "Android Device"
    ): Result<String> {
        val docId = sanitizeFirestoreId("log_${UUID.randomUUID().toString().replace("-", "").take(16)}", "log")
        val docRef = userDoc(userId).collection("audit_logs").document(docId)
        val payload = buildMap<String, Any?> {
            put("actorId", actorId.ifBlank { "SYSTEM" }.take(80))
            put("role", role.ifBlank { "STAFF" }.take(40))
            put("action", action.ifBlank { "EVENT" }.take(80))
            put("details", details.ifBlank { "Action recorded" }.take(500))
            put("date", date.take(20))
            put("time", time.take(20))
            if (deviceInfo.isNotBlank()) put("deviceInfo", deviceInfo.take(120))
            put("createdAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
