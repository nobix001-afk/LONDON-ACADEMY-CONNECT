package com.example.data.repository

import android.location.Location
import com.example.data.dao.AppDao
import com.example.data.firebase.FirestoreAcademyRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class EduRepository(
    private val dao: AppDao,
    private val firestoreRepo: FirestoreAcademyRepository? = null,
    private val firebaseUid: String? = null
) {

    private suspend inline fun syncCloud(block: suspend (FirestoreAcademyRepository, String) -> Unit) {
        val fs = firestoreRepo ?: return
        val uid = firebaseUid ?: return
        if (uid.isBlank()) return
        runCatching { block(fs, uid) }
    }

    val allUsers: Flow<List<UserEntity>> = dao.getAllUsersFlow()
    val allEmployees: Flow<List<EmployeeEntity>> = dao.getAllEmployeesFlow()
    val allStudents: Flow<List<StudentEntity>> = dao.getAllStudentsFlow()
    val allEmployeeAttendance: Flow<List<EmployeeAttendanceEntity>> = dao.getAllEmployeeAttendanceFlow()
    val allStudentAttendance: Flow<List<StudentAttendanceEntity>> = dao.getAllStudentAttendanceFlow()
    val allPayments: Flow<List<FeePaymentEntity>> = dao.getAllPaymentsFlow()
    val allInstallments: Flow<List<FeeInstallmentEntity>> = dao.getAllInstallmentsFlow()
    val allNotifications: Flow<List<AppNotificationEntity>> = dao.getAllNotificationsFlow()
    val allHolidays: Flow<List<HolidayEntity>> = dao.getAllHolidaysFlow()
    val officeSettings: Flow<OfficeSettingsEntity?> = dao.getOfficeSettingsFlow()
    val allCategories: Flow<List<EmployeeCategoryEntity>> = dao.getAllCategoriesFlow()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogsFlow()
    val allCourses: Flow<List<CourseEntity>> = dao.getAllCoursesFlow()
    val allBranches: Flow<List<BranchEntity>> = dao.getAllBranchesFlow()

    suspend fun login(userId: String, pass: String): UserEntity? {
        val user = dao.login(userId.trim(), pass)
        if (user != null) {
            logAudit(user.userId, user.role.name, "LOGIN", "User logged in successfully")
        }
        return user
    }

    suspend fun getUserById(userId: String): UserEntity? = dao.getUserById(userId)

    suspend fun resetPasswordWithOtp(userId: String, contactNo: String, newPass: String): Boolean {
        val user = dao.getUserByContact(userId.trim(), contactNo.trim()) ?: return false
        dao.updatePassword(user.userId, newPass)
        logAudit(user.userId, user.role.name, "PASSWORD_RESET", "Password was reset via OTP verification")
        return true
    }

    suspend fun insertUser(user: UserEntity) = dao.insertUser(user)

    suspend fun insertEmployee(employee: EmployeeEntity, initialPass: String = "emp123") {
        dao.insertEmployee(employee)
        val user = UserEntity(
            userId = employee.employeeId,
            password = initialPass,
            name = employee.name,
            role = UserRole.EMPLOYEE,
            contactNo = employee.contactNo,
            email = employee.email
        )
        dao.insertUser(user)
        syncCloud { fs, uid -> fs.saveEmployee(uid, employee) }
        logAudit("ADMIN", "ADMIN", "EMPLOYEE_CREATED", "Created employee: ${employee.name} (${employee.employeeId})")
    }

    suspend fun updateEmployee(employee: EmployeeEntity) {
        dao.updateEmployee(employee)
        syncCloud { fs, uid -> fs.saveEmployee(uid, employee) }
        logAudit("ADMIN", "ADMIN", "EMPLOYEE_UPDATED", "Updated employee: ${employee.employeeId}")
    }

    suspend fun deleteEmployee(employeeId: String, actorId: String = "ADMIN") {
        val emp = dao.getEmployeeById(employeeId)
        dao.deleteEmployee(employeeId)
        dao.deleteNonAdminUser(employeeId)
        syncCloud { fs, uid -> fs.deleteEmployee(uid, employeeId) }
        logAudit(
            actorId,
            "ADMIN",
            "EMPLOYEE_REMOVED",
            "Removed staff/teacher who left institute: ${emp?.name ?: employeeId} ($employeeId)"
        )
    }

    suspend fun insertStudent(student: StudentEntity, initialPass: String = "student123", creatorId: String = "ADMIN") {
        dao.insertStudent(student)
        val user = UserEntity(
            userId = student.userId,
            password = initialPass,
            name = student.name,
            role = UserRole.STUDENT,
            contactNo = student.contactNo
        )
        dao.insertUser(user)

        // If initial payment was made at registration, record it
        if (student.paidFees > 0) {
            val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
            val payment = FeePaymentEntity(
                receiptNo = receiptNo,
                admissionNo = student.admissionNo,
                studentName = student.name,
                courseName = student.courseName,
                amount = student.paidFees,
                paymentDate = student.dateOfJoining,
                paymentMode = "Cash / Initial",
                paymentPlan = student.paymentPlan,
                installmentMonth = "Initial Admission Fee",
                nextDueDate = student.nextDueDate,
                collectedBy = creatorId,
                remarks = "Initial fee payment at admission"
            )
            dao.insertPayment(payment)
        }

        // If student has pending fees, generate installment records
        if (student.pendingFees > 0) {
            if (student.paymentPlan == "ONE-TIME") {
                dao.insertInstallment(
                    FeeInstallmentEntity(
                        admissionNo = student.admissionNo,
                        studentName = student.name,
                        courseName = student.courseName,
                        installmentNumber = 1,
                        monthName = "Full Payment Balance",
                        dueDate = student.nextDueDate.ifEmpty { student.dateOfJoining },
                        installmentAmount = student.pendingFees,
                        amountPaid = 0.0,
                        remainingAmount = student.pendingFees,
                        status = "PENDING"
                    )
                )
            } else {
                val instCount = if (student.installmentCount > 0) {
                    student.installmentCount.coerceIn(1, 24)
                } else if (student.monthlyFeeAmount > 0) {
                    Math.ceil(student.pendingFees / student.monthlyFeeAmount).toInt().coerceIn(1, 12)
                } else 3

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val baseCal = Calendar.getInstance().apply {
                    val parsed = try {
                        if (student.nextDueDate.isNotBlank()) sdfDate.parse(student.nextDueDate) else null
                    } catch (_: Exception) { null }
                    if (parsed != null) {
                        time = parsed
                    } else {
                        add(Calendar.MONTH, 1)
                    }
                }

                val perInst = Math.round(student.pendingFees / instCount).toDouble()
                for (i in 0 until instCount) {
                    val instCal = (baseCal.clone() as Calendar).apply { add(Calendar.MONTH, i) }
                    val mName = "${sdfMonth.format(instCal.time)} (Inst. ${i + 1}/$instCount)"
                    val dDate = sdfDate.format(instCal.time)
                    val amt = if (i == instCount - 1) student.pendingFees - (perInst * (instCount - 1)) else perInst
                    dao.insertInstallment(
                        FeeInstallmentEntity(
                            admissionNo = student.admissionNo,
                            studentName = student.name,
                            courseName = student.courseName,
                            installmentNumber = i + 1,
                            monthName = mName,
                            dueDate = dDate,
                            installmentAmount = amt,
                            amountPaid = 0.0,
                            remainingAmount = amt,
                            status = "PENDING"
                        )
                    )
                }
            }
        }

        syncCloud { fs, uid ->
            fs.saveStudent(uid, student)
            dao.getPaymentsForStudentFlow(student.admissionNo).first().forEach { pay ->
                fs.saveFeePayment(uid, pay)
            }
            dao.getInstallmentsForStudentFlow(student.admissionNo).first().forEach { inst ->
                fs.saveFeeInstallment(uid, inst)
            }
        }
        logAudit(creatorId, "STAFF", "STUDENT_ADDED", "Registered student: ${student.name} (${student.admissionNo})")
    }

    suspend fun updateStudent(student: StudentEntity, modifierId: String = "ADMIN") {
        dao.updateStudent(student)
        syncCloud { fs, uid -> fs.saveStudent(uid, student) }
        logAudit(modifierId, "STAFF", "STUDENT_UPDATED", "Updated student: ${student.admissionNo}")
    }

    suspend fun deleteStudent(admissionNo: String, actorId: String = "ADMIN") {
        val student = dao.getStudentByAdmissionNo(admissionNo)
        dao.deleteStudent(admissionNo)
        dao.deleteInstallmentsForStudent(admissionNo)
        student?.userId?.takeIf { it.isNotBlank() }?.let { portalUserId ->
            dao.deleteNonAdminUser(portalUserId)
        }
        syncCloud { fs, uid -> fs.deleteStudent(uid, admissionNo) }
        logAudit(
            actorId,
            "ADMIN",
            "STUDENT_REMOVED",
            "Removed student who left institute: ${student?.name ?: admissionNo} ($admissionNo)"
        )
    }

    suspend fun recordFeePayment(payment: FeePaymentEntity, collectorId: String): Boolean {
        val student = dao.getStudentByAdmissionNo(payment.admissionNo) ?: return false
        val newPaid = student.paidFees + payment.amount
        val newPending = max(0.0, student.totalFees - newPaid)
        dao.insertPayment(payment)
        dao.updateStudentFees(student.admissionNo, newPaid, newPending)
        syncCloud { fs, uid ->
            fs.saveFeePayment(uid, payment)
            fs.saveStudent(uid, student.copy(paidFees = newPaid, pendingFees = newPending))
        }
        logAudit(collectorId, "STAFF", "FEE_PAYMENT_ADDED", "Recorded payment of ₹${payment.amount} for ${student.name}")
        return true
    }

    suspend fun recordInstallmentPayment(
        installmentId: Long,
        amount: Double,
        paymentDate: String,
        paymentMode: String,
        remarks: String,
        collectorId: String
    ): FeePaymentEntity? {
        val inst = dao.getInstallmentById(installmentId) ?: return null
        val student = dao.getStudentByAdmissionNo(inst.admissionNo) ?: return null

        val newInstPaid = inst.amountPaid + amount
        val newInstRemaining = max(0.0, inst.installmentAmount - newInstPaid)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val newInstStatus = when {
            newInstRemaining <= 0.0 -> "PAID"
            newInstPaid > 0.0 && inst.dueDate < todayStr -> "OVERDUE"
            newInstPaid > 0.0 -> "PARTIAL"
            inst.dueDate < todayStr -> "OVERDUE"
            else -> "PENDING"
        }

        val updatedInst = inst.copy(
            amountPaid = newInstPaid,
            remainingAmount = newInstRemaining,
            status = newInstStatus,
            paymentMode = paymentMode,
            lastPaymentDate = paymentDate,
            remarks = if (remarks.isNotBlank()) remarks else inst.remarks
        )
        dao.updateInstallment(updatedInst)

        val newStudentPaid = student.paidFees + amount
        val newStudentPending = max(0.0, student.totalFees - newStudentPaid)
        dao.updateStudentFees(student.admissionNo, newStudentPaid, newStudentPending)

        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
        val payment = FeePaymentEntity(
            receiptNo = receiptNo,
            admissionNo = student.admissionNo,
            studentName = student.name,
            amount = amount,
            paymentDate = paymentDate,
            paymentMode = paymentMode,
            paymentPlan = student.paymentPlan,
            installmentMonth = inst.monthName,
            courseName = student.courseName,
            nextDueDate = student.nextDueDate,
            collectedBy = collectorId,
            remarks = remarks.ifEmpty { "Installment for ${inst.monthName}" }
        )
        dao.insertPayment(payment)
        syncCloud { fs, uid ->
            fs.saveFeeInstallment(uid, updatedInst)
            fs.saveStudent(uid, student.copy(paidFees = newStudentPaid, pendingFees = newStudentPending))
            fs.saveFeePayment(uid, payment)
        }
        logAudit(collectorId, "STAFF", "INSTALLMENT_PAID", "Recorded installment ₹$amount for ${student.name} (${inst.monthName})")
        return payment
    }

    suspend fun addManualFeeRecord(
        studentName: String,
        admissionNo: String,
        course: String,
        amount: Double,
        mode: String,
        paymentType: String,
        installmentMonth: String,
        dueDate: String,
        remarks: String,
        collectorId: String
    ): FeePaymentEntity {
        var student = dao.getStudentByAdmissionNo(admissionNo)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        if (student == null) {
            val total = if (paymentType == "ONE-TIME") amount else amount * 4
            student = StudentEntity(
                admissionNo = admissionNo,
                name = studentName,
                contactNo = "9876543210",
                address = "London Academy Campus",
                courseName = course,
                batchName = "Active Batch",
                dateOfJoining = todayStr,
                courseDuration = "6 Months",
                totalFees = total,
                paidFees = amount,
                pendingFees = max(0.0, total - amount),
                paymentPlan = paymentType,
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "stu_" + admissionNo.lowercase().replace("-", "_"),
                nextDueDate = dueDate
            )
            dao.insertStudent(student)
        } else {
            val newPaid = student.paidFees + amount
            val newPending = max(0.0, student.totalFees - newPaid)
            dao.updateStudentFees(student.admissionNo, newPaid, newPending)
        }

        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
        val payment = FeePaymentEntity(
            receiptNo = receiptNo,
            admissionNo = admissionNo,
            studentName = studentName,
            amount = amount,
            paymentDate = todayStr,
            paymentMode = mode,
            paymentPlan = paymentType,
            installmentMonth = installmentMonth,
            courseName = course,
            nextDueDate = dueDate,
            collectedBy = collectorId,
            remarks = remarks
        )
        dao.insertPayment(payment)

        // If an installment month is provided, also create or update installment entity
        if (installmentMonth.isNotBlank()) {
            val status = if (dueDate.isNotEmpty() && dueDate < todayStr) "OVERDUE" else "PAID"
            val installment = FeeInstallmentEntity(
                admissionNo = admissionNo,
                studentName = studentName,
                courseName = course,
                monthName = installmentMonth,
                installmentNumber = 1,
                dueDate = dueDate.ifEmpty { todayStr },
                installmentAmount = amount,
                amountPaid = amount,
                remainingAmount = 0.0,
                status = status,
                paymentMode = mode,
                lastPaymentDate = todayStr,
                remarks = remarks
            )
            dao.insertInstallment(installment)
        }

        syncCloud { fs, uid ->
            dao.getStudentByAdmissionNo(admissionNo)?.let { fs.saveStudent(uid, it) }
            fs.saveFeePayment(uid, payment)
            if (installmentMonth.isNotBlank()) {
                dao.getInstallmentsForStudentFlow(admissionNo).first().forEach { fs.saveFeeInstallment(uid, it) }
            }
        }
        logAudit(collectorId, "STAFF", "MANUAL_FEE_RECORD", "Added manual fee record of ₹$amount for $studentName ($paymentType)")
        return payment
    }

    suspend fun updateFeePayment(updatedPayment: FeePaymentEntity, modifierId: String) {
        val oldPayment = dao.getPaymentById(updatedPayment.id) ?: return
        dao.updatePayment(updatedPayment)
        val diff = updatedPayment.amount - oldPayment.amount
        if (diff != 0.0) {
            val student = dao.getStudentByAdmissionNo(updatedPayment.admissionNo)
            if (student != null) {
                val newPaid = max(0.0, student.paidFees + diff)
                val newPending = max(0.0, student.totalFees - newPaid)
                dao.updateStudentFees(student.admissionNo, newPaid, newPending)
            }
        }
        syncCloud { fs, uid ->
            fs.saveFeePayment(uid, updatedPayment)
            dao.getStudentByAdmissionNo(updatedPayment.admissionNo)?.let { fs.saveStudent(uid, it) }
        }
        logAudit(modifierId, "ADMIN", "FEE_PAYMENT_EDITED", "Edited receipt ${updatedPayment.receiptNo} to ₹${updatedPayment.amount}")
    }

    suspend fun deleteFeePayment(paymentId: Long, modifierId: String) {
        val payment = dao.getPaymentById(paymentId) ?: return
        dao.deletePayment(paymentId)
        val student = dao.getStudentByAdmissionNo(payment.admissionNo)
        if (student != null) {
            val newPaid = max(0.0, student.paidFees - payment.amount)
            val newPending = max(0.0, student.totalFees - newPaid)
            dao.updateStudentFees(student.admissionNo, newPaid, newPending)
        }
        syncCloud { fs, uid ->
            fs.deleteFeePayment(uid, payment.receiptNo)
            dao.getStudentByAdmissionNo(payment.admissionNo)?.let { fs.saveStudent(uid, it) }
        }
        logAudit(modifierId, "ADMIN", "FEE_PAYMENT_DELETED", "Deleted receipt ${payment.receiptNo} of ₹${payment.amount} for ${payment.studentName}")
    }

    fun getInstallmentsForStudent(admissionNo: String): Flow<List<FeeInstallmentEntity>> =
        dao.getInstallmentsForStudentFlow(admissionNo)

    suspend fun getStudentByUserId(userId: String): StudentEntity? = dao.getStudentByUserId(userId)

    suspend fun getEmployeeById(employeeId: String): EmployeeEntity? = dao.getEmployeeById(employeeId)

    suspend fun getStudentAttendanceByDate(admissionNo: String, date: String): StudentAttendanceEntity? =
        dao.getStudentAttendanceByDate(admissionNo, date)

    suspend fun saveSelfStudentAttendance(record: StudentAttendanceEntity, existingId: Long?) {
        if (existingId != null) {
            dao.updateStudentAttendance(record.copy(id = existingId))
        } else {
            dao.insertStudentAttendance(record)
        }
        syncCloud { fs, uid -> fs.saveStudentAttendance(uid, record) }
    }

    suspend fun saveSelfEmployeeAttendance(record: EmployeeAttendanceEntity, existingId: Long?) {
        if (existingId != null) {
            dao.updateEmployeeAttendance(record.copy(id = existingId))
        } else {
            dao.insertEmployeeAttendance(record)
        }
        syncCloud { fs, uid -> fs.saveEmployeeAttendance(uid, record) }
    }

    suspend fun upsertBranchFromCloud(branch: BranchEntity) = dao.insertBranch(branch)
    suspend fun upsertCourseFromCloud(course: CourseEntity) = dao.insertCourse(course)
    suspend fun upsertEmployeeFromCloud(employee: EmployeeEntity) {
        dao.insertEmployee(employee)
        if (dao.getUserById(employee.employeeId) == null) {
            dao.insertUser(
                UserEntity(
                    userId = employee.employeeId,
                    password = if (employee.department.equals("Sub-Admin", ignoreCase = true)) "sub123" else "emp123",
                    name = employee.name,
                    role = if (employee.department.equals("Sub-Admin", ignoreCase = true)) UserRole.SUB_ADMIN else UserRole.EMPLOYEE,
                    contactNo = employee.contactNo,
                    email = employee.email
                )
            )
        }
    }

    suspend fun upsertStudentFromCloud(student: StudentEntity) {
        dao.insertStudent(student)
        if (student.userId.isNotBlank() && dao.getUserById(student.userId) == null) {
            dao.insertUser(
                UserEntity(
                    userId = student.userId,
                    password = "stud123",
                    name = student.name,
                    role = UserRole.STUDENT,
                    contactNo = student.contactNo
                )
            )
        }
    }

    suspend fun syncBranchesFromCloud(cloudBranches: List<BranchEntity>, pruneDeleted: Boolean) {
        cloudBranches.forEach { dao.insertBranch(it) }
        if (pruneDeleted) {
            val cloudNames = cloudBranches.map { it.branchName }.toSet()
            dao.getAllBranchesFlow().first().forEach { local ->
                if (local.branchName !in cloudNames) {
                    dao.deleteBranch(local.branchName)
                }
            }
        }
    }

    suspend fun syncCoursesFromCloud(cloudCourses: List<CourseEntity>, pruneDeleted: Boolean) {
        cloudCourses.forEach { dao.insertCourse(it) }
        if (pruneDeleted) {
            val cloudNames = cloudCourses.map { it.courseName }.toSet()
            dao.getAllCoursesFlow().first().forEach { local ->
                if (local.courseName !in cloudNames) {
                    dao.deleteCourse(local.courseName)
                }
            }
        }
    }

    suspend fun syncEmployeesFromCloud(cloudEmployees: List<EmployeeEntity>, pruneDeleted: Boolean) {
        cloudEmployees.forEach { upsertEmployeeFromCloud(it) }
        if (pruneDeleted) {
            val cloudIds = cloudEmployees.map { it.employeeId }.toSet()
            dao.getAllEmployeesFlow().first().forEach { local ->
                if (local.employeeId !in cloudIds) {
                    dao.deleteEmployee(local.employeeId)
                    dao.deleteNonAdminUser(local.employeeId)
                }
            }
        }
    }

    suspend fun syncStudentsFromCloud(cloudStudents: List<StudentEntity>, pruneDeleted: Boolean) {
        cloudStudents.forEach { upsertStudentFromCloud(it) }
        if (pruneDeleted) {
            val cloudAdmissions = cloudStudents.map { it.admissionNo }.toSet()
            dao.getAllStudentsFlow().first().forEach { local ->
                if (local.admissionNo !in cloudAdmissions) {
                    dao.deleteStudent(local.admissionNo)
                    dao.deleteInstallmentsForStudent(local.admissionNo)
                    if (local.userId.isNotBlank()) {
                        dao.deleteNonAdminUser(local.userId)
                    }
                }
            }
        }
    }

    suspend fun syncHolidaysFromCloud(cloudHolidays: List<HolidayEntity>, pruneDeleted: Boolean) {
        cloudHolidays.forEach { upsertHolidayFromCloud(it) }
        if (pruneDeleted) {
            val cloudKeys = cloudHolidays.map { "${it.date}|${it.holidayName}" }.toSet()
            dao.getAllHolidaysFlow().first().forEach { local ->
                if ("${local.date}|${local.holidayName}" !in cloudKeys) {
                    dao.deleteHoliday(local.id)
                }
            }
        }
    }

    suspend fun upsertOfficeSettingsFromCloud(settings: OfficeSettingsEntity) = dao.insertOrUpdateOfficeSettings(settings)

    suspend fun upsertEmployeeAttendanceFromCloud(record: EmployeeAttendanceEntity) {
        val existing = dao.getTodayEmployeeAttendance(record.employeeId, record.date)
        if (existing != null) {
            dao.updateEmployeeAttendance(record.copy(id = existing.id))
        } else {
            dao.insertEmployeeAttendance(record)
        }
    }

    suspend fun upsertStudentAttendanceFromCloud(record: StudentAttendanceEntity) {
        val existing = dao.getStudentAttendanceByDate(record.admissionNo, record.date)
        if (existing != null) {
            dao.updateStudentAttendance(record.copy(id = existing.id))
        } else {
            dao.insertStudentAttendance(record)
        }
    }

    suspend fun upsertFeePaymentFromCloud(payment: FeePaymentEntity) {
        val existing = dao.getPaymentByReceiptNo(payment.receiptNo)
        if (existing != null) {
            dao.updatePayment(payment.copy(id = existing.id))
        } else {
            dao.insertPayment(payment)
        }
    }

    suspend fun upsertFeeInstallmentFromCloud(installment: FeeInstallmentEntity) {
        val existing = dao.getInstallmentByNaturalKey(
            installment.admissionNo,
            installment.installmentNumber,
            installment.monthName
        )
        if (existing != null) {
            dao.updateInstallment(installment.copy(id = existing.id))
        } else {
            dao.insertInstallment(installment)
        }
    }

    suspend fun upsertHolidayFromCloud(holiday: HolidayEntity) {
        val existing = dao.getHolidayByNaturalKey(holiday.date, holiday.holidayName)
        if (existing == null) {
            dao.insertHoliday(holiday)
        }
    }

    suspend fun upsertNotificationFromCloud(notification: AppNotificationEntity) {
        val existing = dao.getNotificationByNaturalKey(notification.title, notification.date, notification.time)
        if (existing == null) {
            dao.insertNotification(notification)
        }
    }

    suspend fun syncInitialWorkspaceToCloud() {
        syncCloud { fs, uid ->
            if (fs.observeBranches(uid).first().isEmpty()) {
                dao.getAllBranchesFlow().first().forEach { fs.saveBranch(uid, it) }
            }
            if (fs.observeCourses(uid).first().isEmpty()) {
                dao.getAllCoursesFlow().first().forEach { fs.saveCourse(uid, it) }
            }
            if (fs.observeEmployees(uid).first().isEmpty()) {
                dao.getAllEmployeesFlow().first().forEach { fs.saveEmployee(uid, it) }
            }
            if (fs.observeStudents(uid).first().isEmpty()) {
                dao.getAllStudentsFlow().first().forEach { fs.saveStudent(uid, it) }
            }
            if (fs.observeOfficeSettings(uid).first() == null) {
                dao.getOfficeSettingsOnce()?.let { fs.saveOfficeSettings(uid, it) }
            }
            if (fs.observeHolidays(uid).first().isEmpty()) {
                dao.getAllHolidaysFlow().first().forEach { fs.saveHoliday(uid, it) }
            }
            if (fs.observeNotifications(uid).first().isEmpty()) {
                dao.getAllNotificationsFlow().first().forEach { fs.saveNotification(uid, it) }
            }
            if (fs.observeFeePayments(uid).first().isEmpty()) {
                dao.getAllPaymentsFlow().first().forEach { fs.saveFeePayment(uid, it) }
            }
            if (fs.observeFeeInstallments(uid).first().isEmpty()) {
                dao.getAllInstallmentsFlow().first().forEach { fs.saveFeeInstallment(uid, it) }
            }
            if (fs.observeEmployeeAttendance(uid).first().isEmpty()) {
                dao.getAllEmployeeAttendanceFlow().first().take(50).forEach { fs.saveEmployeeAttendance(uid, it) }
            }
            if (fs.observeStudentAttendance(uid).first().isEmpty()) {
                dao.getAllStudentAttendanceFlow().first().take(50).forEach { fs.saveStudentAttendance(uid, it) }
            }
        }
    }

    suspend fun forceSyncAllToCloud(): Boolean {
        val fs = firestoreRepo ?: return false
        val uid = firebaseUid?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            dao.getAllBranchesFlow().first().forEach { fs.saveBranch(uid, it) }
            dao.getAllCoursesFlow().first().forEach { fs.saveCourse(uid, it) }
            dao.getAllEmployeesFlow().first().forEach { fs.saveEmployee(uid, it) }
            dao.getAllStudentsFlow().first().forEach { fs.saveStudent(uid, it) }
            dao.getOfficeSettingsOnce()?.let { fs.saveOfficeSettings(uid, it) }
            dao.getAllHolidaysFlow().first().forEach { fs.saveHoliday(uid, it) }
            dao.getAllNotificationsFlow().first().forEach { fs.saveNotification(uid, it) }
            dao.getAllPaymentsFlow().first().forEach { fs.saveFeePayment(uid, it) }
            dao.getAllInstallmentsFlow().first().forEach { fs.saveFeeInstallment(uid, it) }
            dao.getAllEmployeeAttendanceFlow().first().take(50).forEach { fs.saveEmployeeAttendance(uid, it) }
            dao.getAllStudentAttendanceFlow().first().take(50).forEach { fs.saveStudentAttendance(uid, it) }
            true
        }.getOrDefault(false)
    }

    suspend fun markStudentAttendance(attendance: StudentAttendanceEntity, teacherId: String) {
        val existing = dao.getStudentAttendanceByDate(attendance.admissionNo, attendance.date)
        if (existing != null) {
            dao.updateStudentAttendance(attendance.copy(id = existing.id))
        } else {
            dao.insertStudentAttendance(attendance)
        }
        syncCloud { fs, uid -> fs.saveStudentAttendance(uid, attendance) }
        logAudit(teacherId, "TEACHER", "STUDENT_ATTENDANCE_MARKED", "Marked ${attendance.status} for ${attendance.admissionNo}")
    }

    suspend fun markBatchStudentAttendance(
        records: List<StudentAttendanceEntity>,
        actorId: String,
        actorRole: String,
        batchLabel: String
    ) {
        if (records.isEmpty()) return
        records.forEach { attendance ->
            val existing = dao.getStudentAttendanceByDate(attendance.admissionNo, attendance.date)
            if (existing != null) {
                dao.updateStudentAttendance(attendance.copy(id = existing.id))
            } else {
                dao.insertStudentAttendance(attendance)
            }
        }
        syncCloud { fs, uid ->
            records.forEach { fs.saveStudentAttendance(uid, it) }
        }
        logAudit(
            actorId,
            actorRole,
            "BATCH_ATTENDANCE_MARKED",
            "Marked attendance for ${records.size} students in $batchLabel on ${records.first().date}"
        )
    }

    suspend fun clockOutEmployee(employeeId: String, date: String, checkOutTimeStr: String): EmployeeAttendanceEntity? {
        val record = dao.getTodayEmployeeAttendance(employeeId, date) ?: return null
        val settings = getOfficeSettings()
        val reqMins = max(60, (settings.standardDailyHours * 60).toInt())
        val workedMins = computeDurationMinutes(record.time, checkOutTimeStr)
        val updated = record.copy(
            checkOutTime = checkOutTimeStr,
            workedMinutes = workedMins,
            requiredMinutes = reqMins,
            overtimeMinutes = 0
        )
        dao.updateEmployeeAttendance(updated)
        syncCloud { fs, uid -> fs.saveEmployeeAttendance(uid, updated) }
        logAudit(employeeId, "EMPLOYEE", "ATTENDANCE_CLOCK_OUT", "Clocked out at $checkOutTimeStr (${workedMins}m worked)")
        return updated
    }

    suspend fun clockOutStudent(admissionNo: String, date: String, checkOutTimeStr: String): StudentAttendanceEntity? {
        val record = dao.getStudentAttendanceByDate(admissionNo, date) ?: return null
        val settings = getOfficeSettings()
        val reqMins = max(30, (settings.standardStudentDailyHours * 60).toInt())
        val startTime = record.checkInTime.ifBlank { checkOutTimeStr }
        val workedMins = computeDurationMinutes(startTime, checkOutTimeStr)
        val updated = record.copy(
            checkInTime = startTime,
            checkOutTime = checkOutTimeStr,
            workedMinutes = workedMins,
            requiredMinutes = reqMins,
            overtimeMinutes = 0
        )
        dao.updateStudentAttendance(updated)
        syncCloud { fs, uid -> fs.saveStudentAttendance(uid, updated) }
        logAudit(admissionNo, "STUDENT", "STUDENT_CLOCK_OUT", "Checked out at $checkOutTimeStr (${workedMins}m attended)")
        return updated
    }

    suspend fun saveAdminEmployeeAttendance(
        employee: EmployeeEntity,
        date: String,
        status: String,
        checkInTime: String,
        checkOutTime: String,
        notes: String,
        adminId: String = "ADMIN"
    ): EmployeeAttendanceEntity {
        val settings = getOfficeSettings()
        val reqMins = max(60, (settings.standardDailyHours * 60).toInt())
        val normalizedIn = normalizeTime(checkInTime)
        val normalizedOut = if (checkOutTime.isBlank() || status == "LEAVE" || status == "ABSENT") "" else normalizeTime(checkOutTime)
        val workedMins = if (status == "LEAVE" || status == "ABSENT") {
            0
        } else if (normalizedOut.isNotBlank()) {
            computeDurationMinutes(normalizedIn, normalizedOut)
        } else {
            0
        }
        val existing = dao.getTodayEmployeeAttendance(employee.employeeId, date)
        val record = if (existing != null) {
            existing.copy(
                time = if (status == "LEAVE" || status == "ABSENT") "00:00:00" else normalizedIn,
                status = status,
                checkOutTime = normalizedOut,
                workedMinutes = workedMins,
                requiredMinutes = reqMins,
                overtimeMinutes = 0,
                notes = notes.trim()
            )
        } else {
            EmployeeAttendanceEntity(
                employeeId = employee.employeeId,
                employeeName = employee.name,
                department = employee.department,
                date = date,
                time = if (status == "LEAVE" || status == "ABSENT") "00:00:00" else normalizedIn,
                status = status,
                latitude = settings.latitude,
                longitude = settings.longitude,
                distanceMeters = 2.0,
                selfieUri = if (status == "LEAVE" || status == "ABSENT") "" else "admin_verified.jpg",
                isMockLocation = false,
                deviceModel = "Admin Console",
                checkOutTime = normalizedOut,
                workedMinutes = workedMins,
                requiredMinutes = reqMins,
                overtimeMinutes = 0,
                notes = notes.trim()
            )
        }
        if (existing != null) {
            dao.updateEmployeeAttendance(record)
        } else {
            dao.insertEmployeeAttendance(record)
        }
        syncCloud { fs, uid -> fs.saveEmployeeAttendance(uid, record) }
        logAudit(adminId, "ADMIN", "EMPLOYEE_ATTENDANCE_SAVED", "Saved $status for ${employee.name} on $date (${workedMins}m)")
        return record
    }

    suspend fun saveAdminStudentAttendance(
        student: StudentEntity,
        date: String,
        status: String,
        checkInTime: String,
        checkOutTime: String,
        notes: String,
        adminId: String = "ADMIN"
    ): StudentAttendanceEntity {
        val settings = getOfficeSettings()
        val reqMins = max(30, (settings.standardStudentDailyHours * 60).toInt())
        val normalizedIn = normalizeTime(checkInTime)
        val normalizedOut = if (checkOutTime.isBlank() || status == "LEAVE" || status == "ABSENT") "" else normalizeTime(checkOutTime)
        val workedMins = if (status == "LEAVE" || status == "ABSENT") {
            0
        } else if (normalizedOut.isNotBlank()) {
            computeDurationMinutes(normalizedIn, normalizedOut)
        } else {
            0
        }
        val existing = dao.getStudentAttendanceByDate(student.admissionNo, date)
        val record = if (existing != null) {
            existing.copy(
                status = status,
                markedByUserId = adminId,
                checkInTime = if (status == "LEAVE" || status == "ABSENT") "" else normalizedIn,
                checkOutTime = normalizedOut,
                workedMinutes = workedMins,
                requiredMinutes = reqMins,
                overtimeMinutes = 0,
                notes = notes.trim()
            )
        } else {
            StudentAttendanceEntity(
                admissionNo = student.admissionNo,
                studentName = student.name,
                courseName = student.courseName,
                batchName = student.batchName,
                date = date,
                status = status,
                markedByUserId = adminId,
                checkInTime = if (status == "LEAVE" || status == "ABSENT") "" else normalizedIn,
                checkOutTime = normalizedOut,
                workedMinutes = workedMins,
                requiredMinutes = reqMins,
                overtimeMinutes = 0,
                notes = notes.trim()
            )
        }
        if (existing != null) {
            dao.updateStudentAttendance(record)
        } else {
            dao.insertStudentAttendance(record)
        }
        syncCloud { fs, uid -> fs.saveStudentAttendance(uid, record) }
        logAudit(adminId, "ADMIN", "STUDENT_ATTENDANCE_SAVED", "Saved $status for ${student.name} on $date (${workedMins}m)")
        return record
    }

    private fun normalizeTime(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.matches(Regex("^\\d{2}:\\d{2}:\\d{2}$")) -> trimmed
            trimmed.matches(Regex("^\\d{2}:\\d{2}$")) -> "$trimmed:00"
            trimmed.matches(Regex("^\\d:\\d{2}$")) -> "0$trimmed:00"
            trimmed.matches(Regex("^\\d:\\d{2}:\\d{2}$")) -> "0$trimmed"
            else -> "09:00:00"
        }
    }

    fun computeDurationMinutes(startTime: String, endTime: String): Int {
        if (startTime.isBlank() || endTime.isBlank()) return 0
        return try {
            val sParts = startTime.split(":").mapNotNull { it.toIntOrNull() }
            val eParts = endTime.split(":").mapNotNull { it.toIntOrNull() }
            if (sParts.size < 2 || eParts.size < 2) return 0
            val startMins = sParts[0] * 60 + sParts[1]
            val endMins = eParts[0] * 60 + eParts[1]
            max(0, endMins - startMins)
        } catch (e: Exception) {
            0
        }
    }

    fun getEmployeeAttendanceHistory(employeeId: String): Flow<List<EmployeeAttendanceEntity>> =
        dao.getEmployeeAttendanceHistoryFlow(employeeId)

    fun getStudentAttendanceHistory(admissionNo: String): Flow<List<StudentAttendanceEntity>> =
        dao.getStudentAttendanceHistoryFlow(admissionNo)

    fun getStudentPayments(admissionNo: String): Flow<List<FeePaymentEntity>> =
        dao.getPaymentsForStudentFlow(admissionNo)

    fun getStudentsByTeacher(teacherId: String): Flow<List<StudentEntity>> =
        dao.getStudentsByTeacherFlow(teacherId)

    suspend fun getTodayEmployeeAttendance(employeeId: String, date: String): EmployeeAttendanceEntity? =
        dao.getTodayEmployeeAttendance(employeeId, date)

    suspend fun getOfficeSettings(): OfficeSettingsEntity {
        return dao.getOfficeSettingsOnce() ?: OfficeSettingsEntity().also {
            dao.insertOrUpdateOfficeSettings(it)
        }
    }

    suspend fun updateOfficeSettings(settings: OfficeSettingsEntity) {
        dao.insertOrUpdateOfficeSettings(settings)
        syncCloud { fs, uid -> fs.saveOfficeSettings(uid, settings) }
        logAudit("ADMIN", "ADMIN", "SETTINGS_UPDATED", "Updated office GPS location/radius")
    }

    suspend fun insertHoliday(holiday: HolidayEntity) {
        dao.insertHoliday(holiday)
        syncCloud { fs, uid -> fs.saveHoliday(uid, holiday) }
        logAudit("ADMIN", "ADMIN", "HOLIDAY_ADDED", "Added holiday: ${holiday.holidayName}")
    }

    suspend fun deleteHoliday(holidayId: Long) {
        val target = dao.getAllHolidaysFlow().first().firstOrNull { it.id == holidayId }
        dao.deleteHoliday(holidayId)
        if (target != null) {
            syncCloud { fs, uid -> fs.deleteHoliday(uid, target) }
        }
        logAudit("ADMIN", "ADMIN", "HOLIDAY_DELETED", "Deleted holiday ID: $holidayId")
    }

    suspend fun sendNotification(notification: AppNotificationEntity) {
        dao.insertNotification(notification)
        syncCloud { fs, uid -> fs.saveNotification(uid, notification) }
        logAudit(notification.sentBy, "ADMIN", "NOTIFICATION_SENT", "Notification: ${notification.title} to ${notification.targetType}")
    }

    suspend fun updateEmployeeCategory(category: EmployeeCategoryEntity) {
        dao.insertCategory(category)
        logAudit("ADMIN", "ADMIN", "CATEGORY_PERMISSION_UPDATED", "Updated permissions for ${category.categoryName}")
    }

    suspend fun logAudit(userId: String, role: String, action: String, details: String) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val dateStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)
        val log = AuditLogEntity(
            userId = userId,
            role = role,
            action = action,
            details = details,
            date = dateStr,
            time = timeStr
        )
        dao.insertAuditLog(log)
        syncCloud { fs, uid ->
            fs.logAudit(
                userId = uid,
                actorId = userId,
                role = role,
                action = action,
                details = details,
                date = dateStr,
                time = timeStr,
                deviceInfo = log.deviceInfo
            )
        }
    }

    // Distance calculation using standard Haversine formula
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    // Check if a date is Sunday (Weekly Off) or a configured Holiday
    fun getHolidayReasonForDate(
        date: Date = Date(),
        settings: OfficeSettingsEntity = OfficeSettingsEntity(),
        holidays: List<HolidayEntity> = emptyList()
    ): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)
        val holiday = holidays.firstOrNull { it.date == dateStr }
        if (holiday != null) {
            return "Holiday: ${holiday.holidayName} (${holiday.date})"
        }
        val cal = Calendar.getInstance().apply { time = date }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) {
            return "Sunday Weekly Off"
        }
        if (settings.weeklyOff == "SAT_SUN" && dow == Calendar.SATURDAY) {
            return "Saturday Weekly Off"
        }
        return null
    }

    // Working Days Calculation strictly from employee's DOJ
    // Excludes Sundays (or Saturday+Sunday if configured) and Holidays!
    fun calculateWorkingDays(
        dojString: String,
        targetMonthDate: Date = Date(),
        weeklyOff: String = "SUNDAY",
        holidays: List<HolidayEntity> = emptyList()
    ): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val doj = try {
            sdf.parse(dojString) ?: targetMonthDate
        } catch (e: Exception) {
            targetMonthDate
        }

        val cal = Calendar.getInstance()
        cal.time = targetMonthDate
        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)

        // Find start date for the month
        val startCal = Calendar.getInstance()
        startCal.set(targetYear, targetMonth, 1, 0, 0, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val dojCal = Calendar.getInstance()
        dojCal.time = doj
        dojCal.set(Calendar.HOUR_OF_DAY, 0)
        dojCal.set(Calendar.MINUTE, 0)
        dojCal.set(Calendar.SECOND, 0)
        dojCal.set(Calendar.MILLISECOND, 0)

        // If DOJ is in the same month/year, start from DOJ day!
        // If DOJ is after this month, working days is 0.
        if (dojCal.after(cal) && (dojCal.get(Calendar.MONTH) != targetMonth || dojCal.get(Calendar.YEAR) != targetYear)) {
            return 0
        }

        val actualStartCal = if (dojCal.after(startCal)) dojCal else startCal

        // End date is min(current date, end of target month)
        val endCal = Calendar.getInstance()
        endCal.set(targetYear, targetMonth, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val today = Calendar.getInstance()
        val effectiveEndCal = if (targetYear == today.get(Calendar.YEAR) && targetMonth == today.get(Calendar.MONTH)) {
            today
        } else {
            endCal
        }

        val holidayDateSet = holidays.map { it.date }.toSet()

        var workingDays = 0
        val cursor = actualStartCal.clone() as Calendar

        while (!cursor.after(effectiveEndCal)) {
            val dayOfWeek = cursor.get(Calendar.DAY_OF_WEEK)
            val isSunday = dayOfWeek == Calendar.SUNDAY
            val isSaturday = dayOfWeek == Calendar.SATURDAY
            val isWeekendOff = if (weeklyOff == "SAT_SUN") (isSunday || isSaturday) else isSunday

            val dateStr = sdf.format(cursor.time)
            val isHoliday = holidayDateSet.contains(dateStr)

            if (!isWeekendOff && !isHoliday) {
                workingDays++
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }

        return max(1, workingDays)
    }

    fun calculateWorkingDaysInRange(
        dojString: String,
        startDateString: String,
        endDateString: String,
        weeklyOff: String = "SUNDAY",
        holidays: List<HolidayEntity> = emptyList()
    ): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val doj = try { sdf.parse(dojString) } catch (_: Exception) { null }
        val start = try { sdf.parse(startDateString) } catch (_: Exception) { null } ?: return 0
        val end = try { sdf.parse(endDateString) } catch (_: Exception) { null } ?: return 0

        val startCal = Calendar.getInstance().apply {
            time = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dojCal = if (doj != null) Calendar.getInstance().apply {
            time = doj
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        } else null

        val actualStart = if (dojCal != null && dojCal.after(startCal)) dojCal else startCal
        val endCal = Calendar.getInstance().apply {
            time = end
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        if (actualStart.after(endCal)) return 0

        val holidayDateSet = holidays.map { it.date }.toSet()
        var workingDays = 0
        val cursor = actualStart.clone() as Calendar
        while (!cursor.after(endCal)) {
            val dayOfWeek = cursor.get(Calendar.DAY_OF_WEEK)
            val isSunday = dayOfWeek == Calendar.SUNDAY
            val isSaturday = dayOfWeek == Calendar.SATURDAY
            val isWeekendOff = if (weeklyOff == "SAT_SUN") (isSunday || isSaturday) else isSunday
            val dateStr = sdf.format(cursor.time)
            if (!isWeekendOff && !holidayDateSet.contains(dateStr)) {
                workingDays++
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return max(1, workingDays)
    }

    fun buildEmployeeWorkReport(
        employee: EmployeeEntity,
        monthFilter: String,
        allAttendance: List<EmployeeAttendanceEntity>,
        settings: OfficeSettingsEntity,
        holidays: List<HolidayEntity>
    ): EmployeeWorkReport {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val stdDailyMins = max(60, (settings.standardDailyHours * 60).toInt())

        val empAllRecords = allAttendance.filter { it.employeeId == employee.employeeId }
        val filteredRecords = if (monthFilter == "ALL" || monthFilter.isBlank()) {
            empAllRecords
        } else {
            empAllRecords.filter { it.date.startsWith(monthFilter) }
        }.sortedByDescending { it.date }

        val distinctByDate = filteredRecords.groupBy { it.date }.mapValues { it.value.first() }.values.toList()

        val workingDays = if (monthFilter == "ALL" || monthFilter.isBlank()) {
            val earliestRecordDate = empAllRecords.minByOrNull { it.date }?.date
            if (earliestRecordDate != null) {
                val monthStart = earliestRecordDate.take(7) + "-01"
                calculateWorkingDaysInRange(employee.dateOfJoining, monthStart, todayStr, settings.weeklyOff, holidays)
            } else {
                calculateWorkingDays(employee.dateOfJoining, Date(), settings.weeklyOff, holidays)
            }
        } else {
            val monthDate = try { monthSdf.parse(monthFilter) ?: Date() } catch (_: Exception) { Date() }
            calculateWorkingDays(employee.dateOfJoining, monthDate, settings.weeklyOff, holidays)
        }

        val presentDays = distinctByDate.count { it.status == "PRESENT" }
        val halfDays = distinctByDate.count { it.status == "HALF_DAY" }
        val daysAttended = distinctByDate.count { it.status == "PRESENT" || it.status == "HALF_DAY" }
        val leaveDays = distinctByDate.count { it.status == "LEAVE" }
        val explicitAbsent = distinctByDate.count { it.status == "ABSENT" }
        val unrecordedWorkingDays = max(0, workingDays - (presentDays + halfDays + leaveDays + explicitAbsent))
        val absentDays = explicitAbsent + unrecordedWorkingDays

        val fullHoursCompletedDays = distinctByDate.count { rec ->
            val req = if (rec.requiredMinutes > 0) rec.requiredMinutes else stdDailyMins
            (rec.status == "PRESENT" || rec.status == "HALF_DAY") && rec.workedMinutes >= req
        }
        val shortHoursDays = distinctByDate.count { rec ->
            val req = if (rec.requiredMinutes > 0) rec.requiredMinutes else stdDailyMins
            (rec.status == "PRESENT" || rec.status == "HALF_DAY") && rec.checkOutTime.isNotBlank() && rec.workedMinutes < req
        }
        val totalWorkedMinutes = distinctByDate.sumOf { it.workedMinutes }

        val effectivePresentScore = presentDays + (halfDays * 0.5)
        val percentage = if (workingDays > 0) {
            min(100.0, (effectivePresentScore / workingDays) * 100.0)
        } else 0.0

        return EmployeeWorkReport(
            employee = employee,
            monthFilter = monthFilter,
            totalWorkingDays = workingDays,
            daysAttended = daysAttended,
            presentDays = presentDays,
            halfDays = halfDays,
            leaveDays = leaveDays,
            absentDays = absentDays,
            fullHoursCompletedDays = fullHoursCompletedDays,
            shortHoursDays = shortHoursDays,
            totalWorkedMinutes = totalWorkedMinutes,
            standardDailyMinutes = stdDailyMins,
            attendancePercentage = percentage,
            records = filteredRecords
        )
    }

    fun buildStudentWorkReport(
        student: StudentEntity,
        monthFilter: String,
        allStudentAttendance: List<StudentAttendanceEntity>,
        allPayments: List<FeePaymentEntity>,
        allInstallments: List<FeeInstallmentEntity>,
        settings: OfficeSettingsEntity,
        holidays: List<HolidayEntity>
    ): StudentWorkReport {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val stdDailyMins = max(30, (settings.standardStudentDailyHours * 60).toInt())

        val stuAllRecords = allStudentAttendance.filter { it.admissionNo == student.admissionNo }
        val filteredRecords = if (monthFilter == "ALL" || monthFilter.isBlank()) {
            stuAllRecords
        } else {
            stuAllRecords.filter { it.date.startsWith(monthFilter) }
        }.sortedByDescending { it.date }

        val distinctByDate = filteredRecords.groupBy { it.date }.mapValues { it.value.first() }.values.toList()

        val classDays = if (monthFilter == "ALL" || monthFilter.isBlank()) {
            val earliestRecordDate = stuAllRecords.minByOrNull { it.date }?.date
            if (earliestRecordDate != null) {
                val monthStart = earliestRecordDate.take(7) + "-01"
                calculateWorkingDaysInRange(student.dateOfJoining, monthStart, todayStr, settings.weeklyOff, holidays)
            } else {
                calculateWorkingDays(student.dateOfJoining, Date(), settings.weeklyOff, holidays)
            }
        } else {
            val monthDate = try { monthSdf.parse(monthFilter) ?: Date() } catch (_: Exception) { Date() }
            calculateWorkingDays(student.dateOfJoining, monthDate, settings.weeklyOff, holidays)
        }

        val presentDays = distinctByDate.count { it.status == "PRESENT" }
        val halfDays = distinctByDate.count { it.status == "HALF_DAY" }
        val daysAttended = distinctByDate.count { it.status == "PRESENT" || it.status == "HALF_DAY" }
        val leaveDays = distinctByDate.count { it.status == "LEAVE" }
        val explicitAbsent = distinctByDate.count { it.status == "ABSENT" }
        val unrecordedClassDays = max(0, classDays - (presentDays + halfDays + leaveDays + explicitAbsent))
        val absentDays = explicitAbsent + unrecordedClassDays

        val fullHoursCompletedDays = distinctByDate.count { rec ->
            val req = if (rec.requiredMinutes > 0) rec.requiredMinutes else stdDailyMins
            (rec.status == "PRESENT" || rec.status == "HALF_DAY") && rec.workedMinutes >= req
        }
        val shortHoursDays = distinctByDate.count { rec ->
            val req = if (rec.requiredMinutes > 0) rec.requiredMinutes else stdDailyMins
            (rec.status == "PRESENT" || rec.status == "HALF_DAY") && rec.checkOutTime.isNotBlank() && rec.workedMinutes < req
        }
        val totalWorkedMinutes = distinctByDate.sumOf { it.workedMinutes }

        val effectivePresentScore = presentDays + (halfDays * 0.5)
        val percentage = if (classDays > 0) {
            min(100.0, (effectivePresentScore / classDays) * 100.0)
        } else 0.0

        val studentAllPayments = allPayments.filter { it.admissionNo == student.admissionNo }.sortedByDescending { it.paymentDate }
        val filteredPayments = if (monthFilter == "ALL" || monthFilter.isBlank()) {
            studentAllPayments
        } else {
            studentAllPayments.filter { it.paymentDate.startsWith(monthFilter) }
        }
        val studentInstallments = allInstallments.filter { it.admissionNo == student.admissionNo }
        val paidInstCount = studentInstallments.count { it.status == "PAID" }

        return StudentWorkReport(
            student = student,
            monthFilter = monthFilter,
            totalClassDays = classDays,
            daysAttended = daysAttended,
            presentDays = presentDays,
            halfDays = halfDays,
            leaveDays = leaveDays,
            absentDays = absentDays,
            fullHoursCompletedDays = fullHoursCompletedDays,
            shortHoursDays = shortHoursDays,
            totalWorkedMinutes = totalWorkedMinutes,
            standardDailyMinutes = stdDailyMins,
            attendancePercentage = percentage,
            feePaymentCount = filteredPayments.size,
            totalFeesPaid = student.paidFees,
            periodFeesPaid = filteredPayments.sumOf { it.amount },
            pendingFees = student.pendingFees,
            paidInstallmentsCount = paidInstCount,
            totalInstallmentsCount = studentInstallments.size,
            payments = filteredPayments,
            records = filteredRecords
        )
    }

    // Pre-populate realistic seed data
    suspend fun prepopulateIfNeeded() {
        val existingAdmin = dao.getUserById("admin")
        if (existingAdmin != null) {
            seedSubAdminEmployeeIfMissing()
            seedBranchesIfEmpty()
            seedCoursesIfEmpty()
            seedInstallmentsIfEmpty()
            seedEmployeeAttendanceIfEmpty()
            seedStudentAttendanceIfEmpty()
            return
        }

        // 1. Users
        val users = listOf(
            UserEntity("admin", "admin123", "Super Admin", UserRole.ADMIN, "9876543210", "admin@londonacademy.edu"),
            UserEntity("subadmin1", "sub123", "Vikram Malhotra", UserRole.SUB_ADMIN, "9876543211", "subadmin@londonacademy.edu"),
            UserEntity("trainer1", "teach123", "Rahul Sharma", UserRole.EMPLOYEE, "9876543212", "rahul.sharma@londonacademy.edu"),
            UserEntity("trainer2", "teach123", "Pooja Verma", UserRole.EMPLOYEE, "9876543213", "pooja.verma@londonacademy.edu"),
            UserEntity("officeboy1", "staff123", "Ramesh Kumar", UserRole.EMPLOYEE, "9876543214", "ramesh@londonacademy.edu"),
            UserEntity("cleaning1", "staff123", "Sunita Devi", UserRole.EMPLOYEE, "9876543215", "sunita@londonacademy.edu"),
            UserEntity("student1", "stud123", "Aman Gupta", UserRole.STUDENT, "9876543216", "aman@example.com"),
            UserEntity("student2", "stud123", "Sneha Patel", UserRole.STUDENT, "9876543217", "sneha@example.com"),
            UserEntity("student3", "stud123", "Karan Johar", UserRole.STUDENT, "9876543218", "karan@example.com"),
            UserEntity("student4", "stud123", "Rahul Kumar", UserRole.STUDENT, "9876543219", "rahul.k@example.com"),
            UserEntity("student5", "stud123", "Amit Kumar", UserRole.STUDENT, "9876543220", "amit.k@example.com"),
            UserEntity("student6", "stud123", "Priya Sharma", UserRole.STUDENT, "9876543221", "priya.s@example.com"),
            UserEntity("student7", "stud123", "Rohit Verma", UserRole.STUDENT, "9876543222", "rohit.v@example.com"),
            UserEntity("student8", "stud123", "Neha Singh", UserRole.STUDENT, "9876543223", "neha.s@example.com")
        )
        users.forEach { dao.insertUser(it) }

        // 2. Categories
        val categories = listOf(
            EmployeeCategoryEntity("Sub-Admin", canManageStudents = false, canViewFees = true),
            EmployeeCategoryEntity("Web Development Trainer", canManageStudents = true, canViewFees = false),
            EmployeeCategoryEntity("Digital Marketing Trainer", canManageStudents = true, canViewFees = false),
            EmployeeCategoryEntity("Teaching Staff", canManageStudents = true, canViewFees = false),
            EmployeeCategoryEntity("Video Editing Trainer", canManageStudents = true, canViewFees = false),
            EmployeeCategoryEntity("Computer Trainer", canManageStudents = true, canViewFees = false),
            EmployeeCategoryEntity("Office Boy", canManageStudents = false, canViewFees = false),
            EmployeeCategoryEntity("Cleaning Staff", canManageStudents = false, canViewFees = false),
            EmployeeCategoryEntity("Other Staff", canManageStudents = false, canViewFees = false)
        )
        categories.forEach { dao.insertCategory(it) }

        // 3. Employees (including Sub-Admin, Teachers, Office Boy, Cleaning Staff)
        val employees = listOf(
            EmployeeEntity("subadmin1", "Vikram Malhotra", "9876543211", "subadmin@londonacademy.edu", "Sub-Admin", "Main Campus (Delhi)", "2024-01-15", false, true, true),
            EmployeeEntity("trainer1", "Rahul Sharma", "9876543212", "rahul.sharma@londonacademy.edu", "Web Development Trainer", "Main Campus (Delhi)", "2024-03-10", true, false, true),
            EmployeeEntity("trainer2", "Pooja Verma", "9876543213", "pooja.verma@londonacademy.edu", "Digital Marketing Trainer", "South Extension Branch", "2024-05-15", true, false, true),
            EmployeeEntity("officeboy1", "Ramesh Kumar", "9876543214", "ramesh@londonacademy.edu", "Office Boy", "Main Campus (Delhi)", "2024-06-01", false, false, true),
            EmployeeEntity("cleaning1", "Sunita Devi", "9876543215", "sunita@londonacademy.edu", "Cleaning Staff", "Noida Branch", "2024-07-15", false, false, true)
        )
        employees.forEach { dao.insertEmployee(it) }

        // 4. Branches, Courses & Students
        seedBranchesIfEmpty()
        seedCoursesIfEmpty()
        seedStudentsAndInstallments()

        // 6. Settings
        val settings = OfficeSettingsEntity(
            id = 1,
            officeName = "London Academy Campus",
            latitude = 28.6139,
            longitude = 77.2090,
            allowedRadiusMeters = 10.0,
            weeklyOff = "SUNDAY"
        )
        dao.insertOrUpdateOfficeSettings(settings)

        // 7. Holidays
        val holidays = listOf(
            HolidayEntity(0, "New Year's Day", "2026-01-01", "Annual Holiday"),
            HolidayEntity(0, "Republic Day", "2026-01-26", "National Holiday"),
            HolidayEntity(0, "Holi Festival", "2026-03-04", "Festival of Colors"),
            HolidayEntity(0, "Independence Day", "2026-08-15", "National Holiday"),
            HolidayEntity(0, "Gandhi Jayanti", "2026-10-02", "National Holiday"),
            HolidayEntity(0, "Diwali Festival", "2026-11-08", "Festival of Lights")
        )
        holidays.forEach { dao.insertHoliday(it) }

        // 8. Notifications
        val notifications = listOf(
            AppNotificationEntity(
                0,
                "September Fee Reminder",
                "Students with pending September installments are requested to clear dues before the 15th.",
                "Fee Reminder",
                "ALL_STUDENTS",
                "",
                "2026-09-02",
                "10:00:00",
                "Finance Dept"
            ),
            AppNotificationEntity(
                0,
                "Welcome to London Academy Connect",
                "Complete student and staff institutional management portal is now active.",
                "Office Update",
                "ALL_STUDENTS",
                "",
                "2026-09-01",
                "09:00:00",
                "Super Admin"
            )
        )
        notifications.forEach { dao.insertNotification(it) }
    }

    suspend fun seedSubAdminEmployeeIfMissing() {
        val existing = dao.getEmployeeById("subadmin1")
        if (existing == null) {
            dao.insertCategory(EmployeeCategoryEntity("Sub-Admin", canManageStudents = false, canViewFees = true))
            dao.insertEmployee(
                EmployeeEntity(
                    employeeId = "subadmin1",
                    name = "Vikram Malhotra",
                    contactNo = "9876543211",
                    email = "subadmin@londonacademy.edu",
                    department = "Sub-Admin",
                    branchName = "Main Campus (Delhi)",
                    dateOfJoining = "2024-01-15",
                    canManageStudents = false,
                    canViewFees = true,
                    canMarkAttendance = true
                )
            )
        }
    }

    suspend fun seedCoursesIfEmpty() {
        if (dao.getCoursesCount() > 0) return
        val defaults = listOf(
            CourseEntity("Full Stack Web Development", "6 Months", 50000.0, "ADMIN"),
            CourseEntity("Digital Marketing Masterclass", "6 Months", 35000.0, "ADMIN"),
            CourseEntity("Video Editing & Animation Pro", "6 Months", 40000.0, "ADMIN"),
            CourseEntity("Computer Fundamentals & Office Tools", "3 Months", 18000.0, "ADMIN"),
            CourseEntity("Data Analytics with Python", "9 Months", 45000.0, "ADMIN"),
            CourseEntity("UI/UX Design Masterclass", "6 Months", 35000.0, "ADMIN"),
            CourseEntity("Python & Data Science", "9 Months", 45000.0, "ADMIN"),
            CourseEntity("UI/UX Design & Branding", "6 Months", 30000.0, "ADMIN")
        )
        defaults.forEach { dao.insertCourse(it) }
    }

    suspend fun addCourse(course: CourseEntity, creatorId: String): Boolean {
        val trimmed = course.courseName.trim()
        if (trimmed.isBlank()) return false
        val existing = dao.getCourseByName(trimmed)
        if (existing != null) return false
        val normalized = course.copy(courseName = trimmed)
        dao.insertCourse(normalized)
        syncCloud { fs, uid -> fs.saveCourse(uid, normalized) }
        logAudit(creatorId, "STAFF", "COURSE_ADDED", "Added course: $trimmed")
        return true
    }

    suspend fun deleteCourse(courseName: String, modifierId: String) {
        dao.deleteCourse(courseName)
        syncCloud { fs, uid -> fs.deleteCourse(uid, courseName) }
        logAudit(modifierId, "STAFF", "COURSE_DELETED", "Removed course: $courseName")
    }

    suspend fun seedBranchesIfEmpty() {
        if (dao.getBranchesCount() > 0) return
        val defaults = listOf(
            BranchEntity("Main Campus (Delhi)", "Green Park, Sector 5, New Delhi", "9876543210", "ADMIN"),
            BranchEntity("South Extension Branch", "Mall Road, South Ext, New Delhi", "9876543291", "ADMIN"),
            BranchEntity("Noida Branch", "Sector 15, Noida", "9876543292", "ADMIN"),
            BranchEntity("Gurugram Branch", "Civil Lines, Gurugram", "9876543293", "ADMIN")
        )
        defaults.forEach { dao.insertBranch(it) }
    }

    suspend fun addBranch(branch: BranchEntity, creatorId: String): Boolean {
        val trimmed = branch.branchName.trim()
        if (trimmed.isBlank()) return false
        val existing = dao.getBranchByName(trimmed)
        if (existing != null) return false
        val normalized = branch.copy(branchName = trimmed)
        dao.insertBranch(normalized)
        syncCloud { fs, uid -> fs.saveBranch(uid, normalized) }
        logAudit(creatorId, "STAFF", "BRANCH_ADDED", "Added branch: $trimmed")
        return true
    }

    suspend fun deleteBranch(branchName: String, modifierId: String) {
        dao.deleteBranch(branchName)
        syncCloud { fs, uid -> fs.deleteBranch(uid, branchName) }
        logAudit(modifierId, "STAFF", "BRANCH_DELETED", "Removed branch: $branchName")
    }

    private suspend fun seedStudentsAndInstallments() {
        val students = listOf(
            StudentEntity(
                admissionNo = "LA10025",
                name = "Rahul Kumar",
                contactNo = "9876543219",
                address = "Flat 402, Royal Palms, New Delhi",
                fatherName = "Suresh Kumar",
                motherName = "Meena Devi",
                courseName = "Full Stack Web Development",
                branchName = "Main Campus (Delhi)",
                batchName = "Morning Batch A",
                batchTiming = "09:00 AM - 10:00 AM",
                dateOfJoining = "2026-05-10",
                courseDuration = "12 Months",
                totalFees = 60000.0,
                paidFees = 35000.0,
                pendingFees = 25000.0,
                paymentPlan = "MONTHLY",
                installmentCount = 12,
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "student4",
                nextDueDate = "2026-09-10",
                monthlyFeeAmount = 5000.0
            ),
            StudentEntity(
                admissionNo = "LA10026",
                name = "Amit Kumar",
                contactNo = "9876543220",
                address = "12 Mall Road, South Ext, New Delhi",
                fatherName = "Rajesh Kumar",
                motherName = "Sunita Devi",
                courseName = "Digital Marketing Masterclass",
                branchName = "South Extension Branch",
                batchName = "Evening Batch B",
                batchTiming = "05:00 PM - 06:00 PM",
                dateOfJoining = "2026-06-01",
                courseDuration = "7 Months",
                totalFees = 35000.0,
                paidFees = 17000.0,
                pendingFees = 18000.0,
                paymentPlan = "MONTHLY",
                installmentCount = 6,
                assignedTeacherId = "trainer2",
                assignedTeacherName = "Pooja Verma",
                userId = "student5",
                nextDueDate = "2026-09-15",
                monthlyFeeAmount = 5000.0
            ),
            StudentEntity(
                admissionNo = "LA10027",
                name = "Priya Sharma",
                contactNo = "9876543221",
                address = "304 Green Avenue, Noida",
                fatherName = "Anil Sharma",
                motherName = "Kavita Sharma",
                courseName = "UI/UX Design & Branding",
                branchName = "Noida Branch",
                batchName = "Weekend Fastrack",
                batchTiming = "10:00 AM - 11:00 AM",
                dateOfJoining = "2026-07-01",
                courseDuration = "6 Months",
                totalFees = 30000.0,
                paidFees = 10000.0,
                pendingFees = 20000.0,
                paymentPlan = "MONTHLY",
                installmentCount = 6,
                assignedTeacherId = "trainer2",
                assignedTeacherName = "Pooja Verma",
                userId = "student6",
                nextDueDate = "2026-09-08",
                monthlyFeeAmount = 5000.0
            ),
            StudentEntity(
                admissionNo = "LA10028",
                name = "Rohit Verma",
                contactNo = "9876543222",
                address = "88 Civil Lines, Gurugram",
                fatherName = "Manoj Verma",
                motherName = "Rekha Verma",
                courseName = "Full Stack Web Development",
                branchName = "Gurugram Branch",
                batchName = "Morning Batch A",
                batchTiming = "09:00 AM - 10:00 AM",
                dateOfJoining = "2026-08-01",
                courseDuration = "6 Months",
                totalFees = 40000.0,
                paidFees = 40000.0,
                pendingFees = 0.0,
                paymentPlan = "ONE-TIME",
                installmentCount = 1,
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "student7",
                nextDueDate = "",
                monthlyFeeAmount = 0.0
            ),
            StudentEntity(
                admissionNo = "LA10029",
                name = "Neha Singh",
                contactNo = "9876543223",
                address = "52 Vasant Kunj, New Delhi",
                fatherName = "Vikram Singh",
                motherName = "Anita Singh",
                courseName = "Python & Data Science",
                branchName = "Main Campus (Delhi)",
                batchName = "Morning Batch B",
                batchTiming = "11:00 AM - 12:00 PM",
                dateOfJoining = "2026-06-15",
                courseDuration = "9 Months",
                totalFees = 45000.0,
                paidFees = 20000.0,
                pendingFees = 25000.0,
                paymentPlan = "MONTHLY",
                installmentCount = 9,
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "student8",
                nextDueDate = "2026-10-05",
                monthlyFeeAmount = 5000.0
            ),
            StudentEntity(
                admissionNo = "STU-2024-001",
                name = "Aman Gupta",
                contactNo = "9876543216",
                address = "42 Green Park, Sector 5, New Delhi",
                fatherName = "Rakesh Gupta",
                motherName = "Seema Gupta",
                courseName = "Full Stack Web Development",
                branchName = "Main Campus (Delhi)",
                batchName = "Morning Batch A",
                batchTiming = "09:00 AM - 10:00 AM",
                dateOfJoining = "2026-04-10",
                courseDuration = "6 Months",
                totalFees = 50000.0,
                paidFees = 30000.0,
                pendingFees = 20000.0,
                paymentPlan = "PART PAYMENT",
                installmentCount = 3,
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "student1",
                nextDueDate = "2026-09-30",
                monthlyFeeAmount = 10000.0
            ),
            StudentEntity(
                admissionNo = "STU-2024-002",
                name = "Sneha Patel",
                contactNo = "9876543217",
                address = "18 Lakeview Colony, Bangalore",
                fatherName = "Harish Patel",
                motherName = "Nita Patel",
                courseName = "Digital Marketing Masterclass",
                branchName = "South Extension Branch",
                batchName = "Evening Batch B",
                batchTiming = "05:00 PM - 06:00 PM",
                dateOfJoining = "2026-06-01",
                courseDuration = "3 Months",
                totalFees = 35000.0,
                paidFees = 35000.0,
                pendingFees = 0.0,
                paymentPlan = "ONE-TIME",
                installmentCount = 1,
                assignedTeacherId = "trainer2",
                assignedTeacherName = "Pooja Verma",
                userId = "student2",
                nextDueDate = "",
                monthlyFeeAmount = 0.0
            ),
            StudentEntity(
                admissionNo = "STU-2024-003",
                name = "Karan Johar",
                contactNo = "9876543218",
                address = "104 Sunrise Heights, Mumbai",
                fatherName = "Yash Johar",
                motherName = "Hiroo Johar",
                courseName = "Full Stack Web Development",
                branchName = "Noida Branch",
                batchName = "Morning Batch A",
                batchTiming = "11:00 AM - 12:00 PM",
                dateOfJoining = "2026-07-15",
                courseDuration = "6 Months",
                totalFees = 50000.0,
                paidFees = 15000.0,
                pendingFees = 35000.0,
                paymentPlan = "PART PAYMENT",
                installmentCount = 6,
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "student3",
                nextDueDate = "2026-09-25",
                monthlyFeeAmount = 7000.0
            )
        )
        students.forEach { dao.insertStudent(it) }

        // Fee Installments
        val installments = listOf(
            // Rahul Kumar: 12 months x 5000
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "June 2026", 1, "2026-06-10", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-06-09"),
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "July 2026", 2, "2026-07-10", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-07-10"),
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "August 2026", 3, "2026-08-10", 5000.0, 5000.0, 0.0, "PAID", "Bank Transfer", "2026-08-08"),
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "September 2026", 4, "2026-09-10", 5000.0, 2000.0, 3000.0, "OVERDUE", "UPI", "2026-09-05", "Partial payment made; due date passed"),
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "October 2026", 5, "2026-10-10", 5000.0, 0.0, 5000.0, "PENDING", "", ""),
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "November 2026", 6, "2026-11-10", 5000.0, 0.0, 5000.0, "PENDING", "", ""),
            FeeInstallmentEntity(0, "LA10025", "Rahul Kumar", "Full Stack Web Development", "December 2026", 7, "2026-12-10", 5000.0, 0.0, 5000.0, "PENDING", "", ""),

            // Amit Kumar
            FeeInstallmentEntity(0, "LA10026", "Amit Kumar", "Digital Marketing Masterclass", "June 2026", 1, "2026-06-15", 5000.0, 5000.0, 0.0, "PAID", "Cash", "2026-06-15"),
            FeeInstallmentEntity(0, "LA10026", "Amit Kumar", "Digital Marketing Masterclass", "July 2026", 2, "2026-07-15", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-07-14"),
            FeeInstallmentEntity(0, "LA10026", "Amit Kumar", "Digital Marketing Masterclass", "August 2026", 3, "2026-08-15", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-08-15"),
            FeeInstallmentEntity(0, "LA10026", "Amit Kumar", "Digital Marketing Masterclass", "September 2026", 4, "2026-09-15", 5000.0, 2000.0, 3000.0, "PARTIAL", "Cash", "2026-09-02", "Paid 2000, balance 3000 due 15 Sep"),
            FeeInstallmentEntity(0, "LA10026", "Amit Kumar", "Digital Marketing Masterclass", "October 2026", 5, "2026-10-15", 5000.0, 0.0, 5000.0, "PENDING", "", ""),

            // Priya Sharma
            FeeInstallmentEntity(0, "LA10027", "Priya Sharma", "UI/UX Design & Branding", "July 2026", 1, "2026-07-08", 5000.0, 5000.0, 0.0, "PAID", "Card", "2026-07-07"),
            FeeInstallmentEntity(0, "LA10027", "Priya Sharma", "UI/UX Design & Branding", "August 2026", 2, "2026-08-08", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-08-08"),
            FeeInstallmentEntity(0, "LA10027", "Priya Sharma", "UI/UX Design & Branding", "September 2026", 3, "2026-09-08", 5000.0, 0.0, 5000.0, "OVERDUE", "", "", "Due date 8 Sep has passed"),
            FeeInstallmentEntity(0, "LA10027", "Priya Sharma", "UI/UX Design & Branding", "October 2026", 4, "2026-10-08", 5000.0, 0.0, 5000.0, "PENDING", "", ""),

            // Karan Johar
            FeeInstallmentEntity(0, "STU-2024-003", "Karan Johar", "Full Stack Web Development", "July 2026", 1, "2026-07-25", 7000.0, 7000.0, 0.0, "PAID", "Cash", "2026-07-25"),
            FeeInstallmentEntity(0, "STU-2024-003", "Karan Johar", "Full Stack Web Development", "August 2026", 2, "2026-08-25", 7000.0, 8000.0, 0.0, "PAID", "UPI", "2026-08-24"),
            FeeInstallmentEntity(0, "STU-2024-003", "Karan Johar", "Full Stack Web Development", "September 2026", 3, "2026-09-25", 7000.0, 0.0, 7000.0, "PENDING", "", "", "Next due date 25 Sep"),
            FeeInstallmentEntity(0, "STU-2024-003", "Karan Johar", "Full Stack Web Development", "October 2026", 4, "2026-10-25", 7000.0, 0.0, 7000.0, "PENDING", "", ""),

            // Neha Singh
            FeeInstallmentEntity(0, "LA10029", "Neha Singh", "Python & Data Science", "July 2026", 1, "2026-07-05", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-07-05"),
            FeeInstallmentEntity(0, "LA10029", "Neha Singh", "Python & Data Science", "August 2026", 2, "2026-08-05", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-08-05"),
            FeeInstallmentEntity(0, "LA10029", "Neha Singh", "Python & Data Science", "September 2026", 3, "2026-09-05", 5000.0, 5000.0, 0.0, "PAID", "UPI", "2026-09-04"),
            FeeInstallmentEntity(0, "LA10029", "Neha Singh", "Python & Data Science", "October 2026", 4, "2026-10-05", 5000.0, 0.0, 5000.0, "PENDING", "", ""),

            // Aman Gupta (Part payment plan installment tracking)
            FeeInstallmentEntity(0, "STU-2024-001", "Aman Gupta", "Full Stack Web Development", "September 2026", 1, "2026-09-30", 10000.0, 0.0, 10000.0, "PENDING", "", "")
        )
        dao.insertInstallments(installments)

        // Payments / Receipts
        val payments = listOf(
            FeePaymentEntity(0, "REC-100201", "LA10025", "Rahul Kumar", 20000.0, "2026-05-10", "Bank Transfer", "MONTHLY", "May 2026", "Full Stack Web Development", "2026-06-10", "admin", "Admission deposit"),
            FeePaymentEntity(0, "REC-100202", "LA10025", "Rahul Kumar", 5000.0, "2026-06-09", "UPI", "MONTHLY", "June 2026", "Full Stack Web Development", "2026-07-10", "admin", "June installment"),
            FeePaymentEntity(0, "REC-100203", "LA10025", "Rahul Kumar", 5000.0, "2026-07-10", "UPI", "MONTHLY", "July 2026", "Full Stack Web Development", "2026-08-10", "admin", "July installment"),
            FeePaymentEntity(0, "REC-100204", "LA10025", "Rahul Kumar", 3000.0, "2026-08-08", "Bank Transfer", "MONTHLY", "August 2026", "Full Stack Web Development", "2026-09-10", "admin", "August installment"),
            FeePaymentEntity(0, "REC-100205", "LA10025", "Rahul Kumar", 2000.0, "2026-09-05", "UPI", "MONTHLY", "September 2026", "Full Stack Web Development", "2026-09-10", "admin", "Partial September payment"),
            FeePaymentEntity(0, "REC-100210", "LA10026", "Amit Kumar", 15000.0, "2026-06-01", "Bank Transfer", "MONTHLY", "June 2026", "Digital Marketing Masterclass", "2026-07-15", "subadmin1", "Admission + Advance"),
            FeePaymentEntity(0, "REC-100211", "LA10026", "Amit Kumar", 2000.0, "2026-09-02", "Cash", "MONTHLY", "September 2026", "Digital Marketing Masterclass", "2026-09-15", "subadmin1", "September partial"),
            FeePaymentEntity(0, "REC-100220", "LA10027", "Priya Sharma", 10000.0, "2026-07-01", "Card", "MONTHLY", "July 2026", "UI/UX Design & Branding", "2026-08-08", "trainer2", "Admission + 1st Inst"),
            FeePaymentEntity(0, "REC-100230", "LA10028", "Rohit Verma", 40000.0, "2026-08-01", "UPI", "ONE-TIME", "August 2026", "Full Stack Web Development", "", "admin", "Full one-time fee"),
            FeePaymentEntity(0, "REC-100240", "LA10029", "Neha Singh", 15000.0, "2026-06-15", "UPI", "MONTHLY", "June 2026", "Python & Data Science", "2026-07-05", "admin", "Admission deposit"),
            FeePaymentEntity(0, "REC-100241", "LA10029", "Neha Singh", 5000.0, "2026-09-04", "UPI", "MONTHLY", "September 2026", "Python & Data Science", "2026-10-05", "admin", "September installment"),
            FeePaymentEntity(0, "REC-100231", "STU-2024-001", "Aman Gupta", 15000.0, "2026-04-10", "UPI", "PART PAYMENT", "April 2026", "Full Stack Web Development", "2026-06-10", "admin", "Initial admission fee"),
            FeePaymentEntity(0, "REC-100289", "STU-2024-001", "Aman Gupta", 15000.0, "2026-06-12", "Card", "PART PAYMENT", "June 2026", "Full Stack Web Development", "2026-09-30", "admin", "Second payment"),
            FeePaymentEntity(0, "REC-100340", "STU-2024-002", "Sneha Patel", 35000.0, "2026-06-01", "Bank Transfer", "ONE-TIME", "June 2026", "Digital Marketing Masterclass", "", "subadmin1", "Full one-time payment"),
            FeePaymentEntity(0, "REC-100410", "STU-2024-003", "Karan Johar", 15000.0, "2026-07-15", "Cash", "MONTHLY", "July 2026", "Full Stack Web Development", "2026-09-25", "trainer1", "First installment")
        )
        payments.forEach { dao.insertPayment(it) }

        seedEmployeeAttendanceIfEmpty()
        seedStudentAttendanceIfEmpty()
    }

    private suspend fun seedInstallmentsIfEmpty() {
        if (dao.getInstallmentById(1) == null) {
            seedStudentsAndInstallments()
        }
    }

    private suspend fun seedEmployeeAttendanceIfEmpty() {
        if (dao.getEmployeeAttendanceCount() > 0) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        val holidayDates = setOf("2026-01-01", "2026-01-26", "2026-03-04", "2026-08-15", "2026-10-02", "2026-11-08")

        // Collect past 38 working days up to yesterday
        val workingDates = mutableListOf<String>()
        val cursor = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        var lookback = 0
        while (workingDates.size < 32 && lookback < 50) {
            val dow = cursor.get(Calendar.DAY_OF_WEEK)
            val dateStr = sdf.format(cursor.time)
            if (dow != Calendar.SUNDAY && !holidayDates.contains(dateStr)) {
                workingDates.add(dateStr)
            }
            cursor.add(Calendar.DAY_OF_MONTH, -1)
            lookback++
        }
        val orderedDates = workingDates.reversed()

        val staffProfiles = listOf(
            Triple("trainer1", "Rahul Sharma", "Web Development Trainer"),
            Triple("trainer2", "Pooja Verma", "Digital Marketing Trainer"),
            Triple("officeboy1", "Ramesh Kumar", "Office Boy"),
            Triple("cleaning1", "Sunita Devi", "Cleaning Staff")
        )

        val records = mutableListOf<EmployeeAttendanceEntity>()
        staffProfiles.forEachIndexed { empIdx, (empId, empName, dept) ->
            orderedDates.forEachIndexed { dayIdx, dateStr ->
                val seedPattern = (dayIdx + empIdx * 3) % 14
                val (status, inTime, outTime, workedMins, note) = when {
                    seedPattern == 5 -> SeedAttendanceTuple("LEAVE", "00:00:00", "", 0, "Approved Personal Leave")
                    empIdx == 3 && seedPattern == 11 -> SeedAttendanceTuple("ABSENT", "00:00:00", "", 0, "Unplanned Absence")
                    seedPattern == 9 -> SeedAttendanceTuple("HALF_DAY", "09:00:00", "13:30:00", 270, "Half-day permission")
                    seedPattern == 10 -> SeedAttendanceTuple("PRESENT", "09:45:00", "17:00:00", 435, "Arrived 45m late")
                    else -> SeedAttendanceTuple("PRESENT", "09:00:00", "17:00:00", 480, "Regular shift completed")
                }

                records.add(
                    EmployeeAttendanceEntity(
                        employeeId = empId,
                        employeeName = empName,
                        department = dept,
                        date = dateStr,
                        time = inTime,
                        status = status,
                        latitude = 28.6139,
                        longitude = 77.2090,
                        distanceMeters = if (status == "LEAVE" || status == "ABSENT") 0.0 else 4.0,
                        selfieUri = if (status == "LEAVE" || status == "ABSENT") "" else "selfie_verified.jpg",
                        isMockLocation = false,
                        deviceModel = "London Academy Biometric/GPS",
                        checkOutTime = outTime,
                        workedMinutes = workedMins,
                        requiredMinutes = 480,
                        overtimeMinutes = 0,
                        notes = note
                    )
                )
            }
        }
        records.forEach { dao.insertEmployeeAttendance(it) }
    }

    private suspend fun seedStudentAttendanceIfEmpty() {
        if (dao.getStudentAttendanceCount() > 0) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val holidayDates = setOf("2026-01-01", "2026-01-26", "2026-03-04", "2026-08-15", "2026-10-02", "2026-11-08")

        val workingDates = mutableListOf<String>()
        val cursor = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        var lookback = 0
        while (workingDates.size < 28 && lookback < 45) {
            val dow = cursor.get(Calendar.DAY_OF_WEEK)
            val dateStr = sdf.format(cursor.time)
            if (dow != Calendar.SUNDAY && !holidayDates.contains(dateStr)) {
                workingDates.add(dateStr)
            }
            cursor.add(Calendar.DAY_OF_MONTH, -1)
            lookback++
        }
        val orderedDates = workingDates.reversed()

        val studentProfiles = listOf(
            listOf("LA10025", "Rahul Kumar", "Full Stack Web Development", "Morning Batch A", "trainer1"),
            listOf("LA10026", "Amit Kumar", "Digital Marketing Masterclass", "Evening Batch B", "trainer2"),
            listOf("LA10027", "Priya Sharma", "UI/UX Design & Branding", "Weekend Fastrack", "trainer2"),
            listOf("LA10028", "Rohit Verma", "Full Stack Web Development", "Morning Batch A", "trainer1"),
            listOf("LA10029", "Neha Singh", "Python & Data Science", "Morning Batch B", "trainer1"),
            listOf("STU-2024-001", "Aman Gupta", "Full Stack Web Development", "Morning Batch A", "trainer1"),
            listOf("STU-2024-002", "Sneha Patel", "Digital Marketing Masterclass", "Evening Batch B", "trainer2"),
            listOf("STU-2024-003", "Karan Johar", "Full Stack Web Development", "Morning Batch A", "trainer1")
        )

        val records = mutableListOf<StudentAttendanceEntity>()
        studentProfiles.forEachIndexed { stuIdx, info ->
            val (admNo, sName, course, batch, teacherId) = info
            orderedDates.forEachIndexed { dayIdx, dateStr ->
                val pattern = (dayIdx + stuIdx * 2) % 12
                val (status, inTime, outTime, workedMins, note) = when {
                    pattern == 4 -> SeedAttendanceTuple("LEAVE", "", "", 0, "Approved Student Leave")
                    stuIdx % 2 == 1 && pattern == 10 -> SeedAttendanceTuple("ABSENT", "", "", 0, "Absent without notice")
                    pattern == 8 -> SeedAttendanceTuple("HALF_DAY", "10:00:00", "11:00:00", 60, "Attended partial session")
                    pattern == 9 -> SeedAttendanceTuple("PRESENT", "10:30:00", "12:00:00", 90, "Joined 30m late")
                    else -> SeedAttendanceTuple("PRESENT", "10:00:00", "12:00:00", 120, "Full 2h class completed")
                }

                records.add(
                    StudentAttendanceEntity(
                        admissionNo = admNo,
                        studentName = sName,
                        courseName = course,
                        batchName = batch,
                        date = dateStr,
                        status = status,
                        markedByUserId = teacherId,
                        checkInTime = inTime,
                        checkOutTime = outTime,
                        workedMinutes = workedMins,
                        requiredMinutes = 120,
                        overtimeMinutes = 0,
                        notes = note
                    )
                )
            }
        }
        records.forEach { dao.insertStudentAttendance(it) }
    }

    private data class SeedAttendanceTuple(
        val status: String,
        val inTime: String,
        val outTime: String,
        val workedMins: Int,
        val note: String
    )
}

