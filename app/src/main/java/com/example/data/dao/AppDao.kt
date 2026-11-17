package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password AND isActive = 1 LIMIT 1")
    suspend fun login(userId: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId AND contactNo = :contactNo LIMIT 1")
    suspend fun getUserByContact(userId: String, contactNo: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET password = :newPassword WHERE userId = :userId")
    suspend fun updatePassword(userId: String, newPassword: String)

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("DELETE FROM users WHERE userId = :userId AND role != 'ADMIN'")
    suspend fun deleteNonAdminUser(userId: String)

    // Employees
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployeesFlow(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getEmployeeById(employeeId: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE employeeId = :employeeId")
    suspend fun deleteEmployee(employeeId: String)

    // Students
    @Query("SELECT * FROM students ORDER BY admissionNo ASC")
    fun getAllStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE admissionNo = :admissionNo LIMIT 1")
    suspend fun getStudentByAdmissionNo(admissionNo: String): StudentEntity?

    @Query("SELECT * FROM students WHERE userId = :userId LIMIT 1")
    suspend fun getStudentByUserId(userId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE assignedTeacherId = :teacherId")
    fun getStudentsByTeacherFlow(teacherId: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET paidFees = :paidFees, pendingFees = :pendingFees WHERE admissionNo = :admissionNo")
    suspend fun updateStudentFees(admissionNo: String, paidFees: Double, pendingFees: Double)

    @Query("DELETE FROM students WHERE admissionNo = :admissionNo")
    suspend fun deleteStudent(admissionNo: String)

    // Employee Attendance
    @Query("SELECT * FROM employee_attendance ORDER BY id DESC")
    fun getAllEmployeeAttendanceFlow(): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId ORDER BY date DESC, time DESC")
    fun getEmployeeAttendanceHistoryFlow(employeeId: String): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId AND date = :date LIMIT 1")
    suspend fun getTodayEmployeeAttendance(employeeId: String, date: String): EmployeeAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployeeAttendance(attendance: EmployeeAttendanceEntity)

    @Update
    suspend fun updateEmployeeAttendance(attendance: EmployeeAttendanceEntity)

    @Query("SELECT COUNT(*) FROM employee_attendance")
    suspend fun getEmployeeAttendanceCount(): Int

    // Student Attendance
    @Query("SELECT * FROM student_attendance ORDER BY id DESC")
    fun getAllStudentAttendanceFlow(): Flow<List<StudentAttendanceEntity>>

    @Query("SELECT * FROM student_attendance WHERE admissionNo = :admissionNo ORDER BY date DESC")
    fun getStudentAttendanceHistoryFlow(admissionNo: String): Flow<List<StudentAttendanceEntity>>

    @Query("SELECT * FROM student_attendance WHERE date = :date AND courseName = :courseName")
    suspend fun getStudentAttendanceByCourseAndDate(date: String, courseName: String): List<StudentAttendanceEntity>

    @Query("SELECT * FROM student_attendance WHERE admissionNo = :admissionNo AND date = :date LIMIT 1")
    suspend fun getStudentAttendanceByDate(admissionNo: String, date: String): StudentAttendanceEntity?

    @Query("SELECT COUNT(*) FROM student_attendance")
    suspend fun getStudentAttendanceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentAttendance(attendance: StudentAttendanceEntity)

    @Update
    suspend fun updateStudentAttendance(attendance: StudentAttendanceEntity)

    // Fee Payments
    @Query("SELECT * FROM fee_payments ORDER BY id DESC")
    fun getAllPaymentsFlow(): Flow<List<FeePaymentEntity>>

    @Query("SELECT * FROM fee_payments WHERE admissionNo = :admissionNo ORDER BY id DESC")
    fun getPaymentsForStudentFlow(admissionNo: String): Flow<List<FeePaymentEntity>>

    @Query("SELECT * FROM fee_payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): FeePaymentEntity?

    @Query("SELECT * FROM fee_payments WHERE receiptNo = :receiptNo LIMIT 1")
    suspend fun getPaymentByReceiptNo(receiptNo: String): FeePaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: FeePaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: FeePaymentEntity)

    @Query("DELETE FROM fee_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    // Fee Installments
    @Query("SELECT * FROM fee_installments ORDER BY dueDate ASC, id ASC")
    fun getAllInstallmentsFlow(): Flow<List<FeeInstallmentEntity>>

    @Query("SELECT * FROM fee_installments WHERE admissionNo = :admissionNo ORDER BY installmentNumber ASC, dueDate ASC")
    fun getInstallmentsForStudentFlow(admissionNo: String): Flow<List<FeeInstallmentEntity>>

    @Query("SELECT * FROM fee_installments WHERE monthName = :monthName ORDER BY dueDate ASC")
    fun getInstallmentsForMonthFlow(monthName: String): Flow<List<FeeInstallmentEntity>>

    @Query("SELECT * FROM fee_installments WHERE id = :id LIMIT 1")
    suspend fun getInstallmentById(id: Long): FeeInstallmentEntity?

    @Query("SELECT * FROM fee_installments WHERE admissionNo = :admissionNo AND installmentNumber = :installmentNumber AND monthName = :monthName LIMIT 1")
    suspend fun getInstallmentByNaturalKey(admissionNo: String, installmentNumber: Int, monthName: String): FeeInstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: FeeInstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<FeeInstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: FeeInstallmentEntity)

    @Query("DELETE FROM fee_installments WHERE id = :id")
    suspend fun deleteInstallment(id: Long)

    @Query("DELETE FROM fee_installments WHERE admissionNo = :admissionNo")
    suspend fun deleteInstallmentsForStudent(admissionNo: String)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotificationsFlow(): Flow<List<AppNotificationEntity>>

    @Query("SELECT * FROM notifications WHERE title = :title AND date = :date AND time = :time LIMIT 1")
    suspend fun getNotificationByNaturalKey(title: String, date: String, time: String): AppNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity)

    // Holidays
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidaysFlow(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE date = :date AND holidayName = :holidayName LIMIT 1")
    suspend fun getHolidayByNaturalKey(date: String, holidayName: String): HolidayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: HolidayEntity)

    @Query("DELETE FROM holidays WHERE id = :holidayId")
    suspend fun deleteHoliday(holidayId: Long)

    // Office Settings
    @Query("SELECT * FROM office_settings WHERE id = 1 LIMIT 1")
    fun getOfficeSettingsFlow(): Flow<OfficeSettingsEntity?>

    @Query("SELECT * FROM office_settings WHERE id = 1 LIMIT 1")
    suspend fun getOfficeSettingsOnce(): OfficeSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOfficeSettings(settings: OfficeSettingsEntity)

    // Employee Categories
    @Query("SELECT * FROM employee_categories")
    fun getAllCategoriesFlow(): Flow<List<EmployeeCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: EmployeeCategoryEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 200")
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Courses
    @Query("SELECT * FROM courses ORDER BY courseName ASC")
    fun getAllCoursesFlow(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE LOWER(courseName) = LOWER(:courseName) LIMIT 1")
    suspend fun getCourseByName(courseName: String): CourseEntity?

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCoursesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE courseName = :courseName")
    suspend fun deleteCourse(courseName: String)

    // Branches
    @Query("SELECT * FROM branches ORDER BY branchName ASC")
    fun getAllBranchesFlow(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE LOWER(branchName) = LOWER(:branchName) LIMIT 1")
    suspend fun getBranchByName(branchName: String): BranchEntity?

    @Query("SELECT COUNT(*) FROM branches")
    suspend fun getBranchesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Query("DELETE FROM branches WHERE branchName = :branchName")
    suspend fun deleteBranch(branchName: String)
}
