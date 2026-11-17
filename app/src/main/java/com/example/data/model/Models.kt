package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    SUB_ADMIN,
    EMPLOYEE,
    STUDENT
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val password: String,
    val name: String,
    val role: UserRole,
    val contactNo: String,
    val email: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val employeeId: String,
    val name: String,
    val contactNo: String,
    val email: String = "",
    val department: String, // e.g., "Web Development Trainer", "Digital Marketing Trainer", "Office Boy", etc.
    val branchName: String = "Main Campus (Delhi)",
    val dateOfJoining: String, // YYYY-MM-DD
    val canManageStudents: Boolean = false,
    val canViewFees: Boolean = false,
    val canMarkAttendance: Boolean = true,
    val photoUri: String = ""
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val admissionNo: String,
    val name: String,
    val contactNo: String,
    val address: String,
    val fatherName: String = "",
    val motherName: String = "",
    val studentPhotoUri: String = "",
    val aadhaarCardUri: String = "",
    val courseName: String,
    val branchName: String = "Main Campus (Delhi)",
    val batchName: String = "Batch A",
    val batchTiming: String = "",
    val dateOfJoining: String, // YYYY-MM-DD
    val courseDuration: String, // e.g., "6 Months"
    val totalFees: Double,
    val paidFees: Double,
    val pendingFees: Double,
    val paymentPlan: String, // "ONE-TIME", "MONTHLY", "PART PAYMENT", "YEARLY"
    val installmentCount: Int = 1, // e.g., 1, 2, 3, 6, 12
    val assignedTeacherId: String,
    val assignedTeacherName: String,
    val userId: String,
    val nextDueDate: String = "",
    val monthlyFeeAmount: Double = 0.0,
    val status: String = "ACTIVE"
)

@Entity(tableName = "employee_attendance")
data class EmployeeAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val employeeName: String,
    val department: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm:ss (Check-in time)
    val status: String, // "PRESENT", "ABSENT", "HALF_DAY", "LEAVE"
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val selfieUri: String = "",
    val isMockLocation: Boolean = false,
    val deviceModel: String = "Android Device",
    val checkOutTime: String = "", // HH:mm:ss
    val workedMinutes: Int = 0, // Total minutes worked
    val requiredMinutes: Int = 480, // Standard shift minutes (default 8 hrs)
    val overtimeMinutes: Int = 0, // Extra minutes worked beyond requiredMinutes
    val notes: String = ""
)

@Entity(tableName = "student_attendance")
data class StudentAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String,
    val studentName: String,
    val courseName: String,
    val batchName: String,
    val date: String, // YYYY-MM-DD
    val status: String, // "PRESENT", "ABSENT", "HALF_DAY", "LEAVE"
    val markedByUserId: String,
    val checkInTime: String = "", // HH:mm:ss
    val checkOutTime: String = "", // HH:mm:ss
    val workedMinutes: Int = 0, // Study/class minutes attended
    val requiredMinutes: Int = 120, // Standard class/study minutes per day (default 2 hrs)
    val overtimeMinutes: Int = 0, // Extra practice/lab/study minutes
    val notes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val selfieUri: String = "",
    val isMockLocation: Boolean = false,
    val deviceModel: String = ""
)

@Entity(tableName = "fee_payments")
data class FeePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNo: String,
    val admissionNo: String,
    val studentName: String,
    val amount: Double,
    val paymentDate: String, // YYYY-MM-DD
    val paymentMode: String, // "Cash", "UPI", "Card", "Bank Transfer", etc.
    val paymentPlan: String,
    val installmentMonth: String = "",
    val courseName: String = "",
    val nextDueDate: String = "",
    val collectedBy: String,
    val remarks: String = ""
)

@Entity(tableName = "fee_installments")
data class FeeInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String,
    val studentName: String,
    val courseName: String,
    val monthName: String, // e.g., "September 2026"
    val installmentNumber: Int = 1,
    val dueDate: String, // YYYY-MM-DD
    val installmentAmount: Double,
    val amountPaid: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val status: String = "PENDING", // "PAID", "PARTIAL", "PENDING", "OVERDUE"
    val paymentMode: String = "",
    val lastPaymentDate: String = "",
    val remarks: String = ""
)

@Entity(tableName = "notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String, // "Offer", "Discount", "Holiday", "Office Update", "Fee Reminder", "Announcement", "Event"
    val targetType: String, // "ALL_STUDENTS", "ALL_EMPLOYEES", "COURSE", "BATCH", "TEACHER", "EMPLOYEE_CATEGORY", "SUB_ADMIN"
    val targetValue: String = "", // specific course, employee category, or ID
    val date: String,
    val time: String,
    val sentBy: String = "Admin"
)

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holidayName: String,
    val date: String, // YYYY-MM-DD
    val description: String = ""
)

@Entity(tableName = "office_settings")
data class OfficeSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val officeName: String = "Main Campus",
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val allowedRadiusMeters: Double = 10.0,
    val weeklyOff: String = "SUNDAY", // "SUNDAY" or "SAT_SUN"
    val standardDailyHours: Double = 8.0,
    val standardStudentDailyHours: Double = 2.0
)

@Entity(tableName = "employee_categories")
data class EmployeeCategoryEntity(
    @PrimaryKey val categoryName: String,
    val canManageStudents: Boolean = false,
    val canViewFees: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val role: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String,
    val time: String,
    val deviceInfo: String = "Android Device"
)

data class EmployeeWorkReport(
    val employee: EmployeeEntity,
    val monthFilter: String,
    val totalWorkingDays: Int,
    val daysAttended: Int,
    val presentDays: Int,
    val halfDays: Int,
    val leaveDays: Int,
    val absentDays: Int,
    val fullHoursCompletedDays: Int,
    val shortHoursDays: Int,
    val totalWorkedMinutes: Int,
    val standardDailyMinutes: Int,
    val attendancePercentage: Double,
    val records: List<EmployeeAttendanceEntity>
)

data class StudentWorkReport(
    val student: StudentEntity,
    val monthFilter: String,
    val totalClassDays: Int,
    val daysAttended: Int,
    val presentDays: Int,
    val halfDays: Int,
    val leaveDays: Int,
    val absentDays: Int,
    val fullHoursCompletedDays: Int,
    val shortHoursDays: Int,
    val totalWorkedMinutes: Int,
    val standardDailyMinutes: Int,
    val attendancePercentage: Double,
    val feePaymentCount: Int,
    val totalFeesPaid: Double,
    val periodFeesPaid: Double,
    val pendingFees: Double,
    val paidInstallmentsCount: Int,
    val totalInstallmentsCount: Int,
    val payments: List<FeePaymentEntity>,
    val records: List<StudentAttendanceEntity>
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val courseName: String,
    val defaultDuration: String = "6 Months",
    val defaultFees: Double = 35000.0,
    val addedBy: String = "ADMIN",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val branchName: String,
    val address: String = "",
    val contactNo: String = "",
    val addedBy: String = "ADMIN",
    val createdAt: Long = System.currentTimeMillis()
)

