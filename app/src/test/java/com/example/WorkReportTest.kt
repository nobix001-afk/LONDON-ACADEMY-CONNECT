package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ExcelExporter
import com.example.data.ExcelExporter.ExcelSheet
import com.example.data.model.*
import com.example.data.repository.EduRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkReportTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EduRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EduRepository(db.appDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `employee work report accurately counts attended, full hours, and leaves`() = runBlocking {
        val employee = EmployeeEntity(
            employeeId = "emp_test_1",
            name = "Test Trainer",
            contactNo = "9999999999",
            department = "Web Development Trainer",
            dateOfJoining = "2026-11-01"
        )
        val settings = OfficeSettingsEntity(standardDailyHours = 8.0, weeklyOff = "SUNDAY")
        val attendance = listOf(
            // Day 1: 8h 00m (exact full hours, no OT)
            EmployeeAttendanceEntity(
                employeeId = "emp_test_1", employeeName = "Test Trainer", department = "Web",
                date = "2026-11-02", time = "09:00:00", checkOutTime = "17:00:00",
                status = "PRESENT", latitude = 0.0, longitude = 0.0, distanceMeters = 2.0,
                workedMinutes = 480, requiredMinutes = 480, overtimeMinutes = 0
            ),
            // Day 2: 9h 30m (full hours + 90m OT)
            EmployeeAttendanceEntity(
                employeeId = "emp_test_1", employeeName = "Test Trainer", department = "Web",
                date = "2026-11-03", time = "09:00:00", checkOutTime = "18:30:00",
                status = "PRESENT", latitude = 0.0, longitude = 0.0, distanceMeters = 2.0,
                workedMinutes = 570, requiredMinutes = 480, overtimeMinutes = 90
            ),
            // Day 3: 6h 30m (short hours)
            EmployeeAttendanceEntity(
                employeeId = "emp_test_1", employeeName = "Test Trainer", department = "Web",
                date = "2026-11-04", time = "10:00:00", checkOutTime = "16:30:00",
                status = "PRESENT", latitude = 0.0, longitude = 0.0, distanceMeters = 2.0,
                workedMinutes = 390, requiredMinutes = 480, overtimeMinutes = 0
            ),
            // Day 4: Half day (4h)
            EmployeeAttendanceEntity(
                employeeId = "emp_test_1", employeeName = "Test Trainer", department = "Web",
                date = "2026-11-05", time = "09:00:00", checkOutTime = "13:00:00",
                status = "HALF_DAY", latitude = 0.0, longitude = 0.0, distanceMeters = 2.0,
                workedMinutes = 240, requiredMinutes = 480, overtimeMinutes = 0
            ),
            // Day 5: Leave
            EmployeeAttendanceEntity(
                employeeId = "emp_test_1", employeeName = "Test Trainer", department = "Web",
                date = "2026-11-06", time = "00:00:00", checkOutTime = "",
                status = "LEAVE", latitude = 0.0, longitude = 0.0, distanceMeters = 0.0,
                workedMinutes = 0, requiredMinutes = 480, overtimeMinutes = 0
            )
        )

        val report = repository.buildEmployeeWorkReport(
            employee = employee,
            monthFilter = "2026-11",
            allAttendance = attendance,
            settings = settings,
            holidays = emptyList()
        )

        assertEquals(4, report.daysAttended)
        assertEquals(3, report.presentDays)
        assertEquals(1, report.halfDays)
        assertEquals(1, report.leaveDays)
        assertEquals(2, report.fullHoursCompletedDays)
        assertEquals(2, report.shortHoursDays)
        assertEquals(1680, report.totalWorkedMinutes)
    }

    @Test
    fun `student work report accurately counts attendance, study hours, leaves, and fee payments`() = runBlocking {
        val student = StudentEntity(
            admissionNo = "LA999",
            name = "Test Student",
            contactNo = "9876543210",
            address = "Campus",
            courseName = "Full Stack",
            dateOfJoining = "2026-11-01",
            courseDuration = "6 Months",
            totalFees = 30000.0,
            paidFees = 15000.0,
            pendingFees = 15000.0,
            paymentPlan = "MONTHLY",
            assignedTeacherId = "trainer1",
            assignedTeacherName = "Rahul Sharma",
            userId = "stu_test"
        )
        val settings = OfficeSettingsEntity(standardStudentDailyHours = 2.0, weeklyOff = "SUNDAY")
        val attendance = listOf(
            StudentAttendanceEntity(
                admissionNo = "LA999", studentName = "Test Student", courseName = "Full Stack",
                batchName = "Batch A", date = "2026-11-02", status = "PRESENT", markedByUserId = "trainer1",
                checkInTime = "10:00:00", checkOutTime = "12:00:00",
                workedMinutes = 120, requiredMinutes = 120, overtimeMinutes = 0
            ),
            StudentAttendanceEntity(
                admissionNo = "LA999", studentName = "Test Student", courseName = "Full Stack",
                batchName = "Batch A", date = "2026-11-03", status = "PRESENT", markedByUserId = "trainer1",
                checkInTime = "10:00:00", checkOutTime = "13:00:00",
                workedMinutes = 180, requiredMinutes = 120, overtimeMinutes = 60
            ),
            StudentAttendanceEntity(
                admissionNo = "LA999", studentName = "Test Student", courseName = "Full Stack",
                batchName = "Batch A", date = "2026-11-04", status = "LEAVE", markedByUserId = "trainer1",
                workedMinutes = 0, requiredMinutes = 120, overtimeMinutes = 0
            )
        )
        val payments = listOf(
            FeePaymentEntity(
                receiptNo = "REC-1", admissionNo = "LA999", studentName = "Test Student",
                amount = 10000.0, paymentDate = "2026-11-01", paymentMode = "UPI",
                paymentPlan = "MONTHLY", collectedBy = "admin"
            ),
            FeePaymentEntity(
                receiptNo = "REC-2", admissionNo = "LA999", studentName = "Test Student",
                amount = 5000.0, paymentDate = "2026-11-10", paymentMode = "Cash",
                paymentPlan = "MONTHLY", collectedBy = "admin"
            )
        )

        val report = repository.buildStudentWorkReport(
            student = student,
            monthFilter = "2026-11",
            allStudentAttendance = attendance,
            allPayments = payments,
            allInstallments = emptyList(),
            settings = settings,
            holidays = emptyList()
        )

        assertEquals(2, report.daysAttended)
        assertEquals(2, report.fullHoursCompletedDays)
        assertEquals(1, report.leaveDays)
        assertEquals(2, report.feePaymentCount)
        assertEquals(15000.0, report.periodFeesPaid, 0.01)
        assertEquals(15000.0, report.pendingFees, 0.01)
    }

    @Test
    fun `computeDurationMinutes calculates shift minutes accurately`() {
        assertEquals(480, repository.computeDurationMinutes("09:00:00", "17:00:00"))
        assertEquals(555, repository.computeDurationMinutes("09:15", "18:30"))
        assertEquals(0, repository.computeDurationMinutes("17:00", "09:00"))
    }

    @Test
    fun `insertStudent persists family and course details and generates exact installment count`() = runBlocking {
        val partPayStudent = StudentEntity(
            admissionNo = "STU-PART-3",
            name = "Aarav Verma",
            contactNo = "9811223344",
            address = "Sector 15, Noida",
            fatherName = "Rajesh Verma",
            motherName = "Sunita Verma",
            courseName = "Full Stack Web Development",
            batchName = "Morning Batch A",
            dateOfJoining = "2026-11-01",
            courseDuration = "6 Months",
            totalFees = 45000.0,
            paidFees = 15000.0,
            pendingFees = 30000.0,
            paymentPlan = "PART PAYMENT",
            installmentCount = 3,
            assignedTeacherId = "emp1",
            assignedTeacherName = "Rahul Sharma",
            userId = "stupart3",
            nextDueDate = "2026-12-01",
            monthlyFeeAmount = 10000.0
        )

        val monthlyStudent = StudentEntity(
            admissionNo = "STU-MON-6",
            name = "Neha Gupta",
            contactNo = "9876501234",
            address = "Indirapuram",
            fatherName = "Suresh Gupta",
            motherName = "Kavita Gupta",
            courseName = "Digital Marketing Masterclass",
            batchName = "Evening Batch B",
            dateOfJoining = "2026-11-05",
            courseDuration = "6 Months",
            totalFees = 36000.0,
            paidFees = 6000.0,
            pendingFees = 30000.0,
            paymentPlan = "MONTHLY",
            installmentCount = 6,
            assignedTeacherId = "emp2",
            assignedTeacherName = "Priya Nair",
            userId = "stumon6",
            nextDueDate = "2026-12-05",
            monthlyFeeAmount = 5000.0
        )

        repository.insertStudent(partPayStudent, "stud123", "subadmin1")
        repository.insertStudent(monthlyStudent, "stud123", "subadmin1")

        val savedPart = db.appDao().getStudentByAdmissionNo("STU-PART-3")
        assertNotNull(savedPart)
        assertEquals("Rajesh Verma", savedPart!!.fatherName)
        assertEquals("Sunita Verma", savedPart.motherName)
        assertEquals("6 Months", savedPart.courseDuration)
        assertEquals("2026-11-01", savedPart.dateOfJoining)
        assertEquals("PART PAYMENT", savedPart.paymentPlan)
        assertEquals(3, savedPart.installmentCount)

        val partInstallments = db.appDao().getInstallmentsForStudentFlow("STU-PART-3").first()
        assertEquals(3, partInstallments.size)
        assertEquals(30000.0, partInstallments.sumOf { it.installmentAmount }, 0.01)

        val monthlyInstallments = db.appDao().getInstallmentsForStudentFlow("STU-MON-6").first()
        assertEquals(6, monthlyInstallments.size)
        assertEquals(30000.0, monthlyInstallments.sumOf { it.installmentAmount }, 0.01)
    }

    @Test
    fun `insertStudent persists 4 and 5 installments, batchTiming, and aadhaarCardUri`() = runBlocking {
        val fourInstStudent = StudentEntity(
            admissionNo = "STU-PART-4",
            name = "Riya Malhotra",
            contactNo = "9811001122",
            address = "Lajpat Nagar",
            fatherName = "Anil Malhotra",
            motherName = "Pooja Malhotra",
            aadhaarCardUri = "/data/user/0/com.example/files/aadhaar/aadhaar_STU-PART-4.jpg",
            courseName = "UI/UX Design Masterclass",
            batchName = "Afternoon Batch",
            batchTiming = "11:00 AM - 12:00 PM",
            dateOfJoining = "2026-11-10",
            courseDuration = "6 Months",
            totalFees = 40000.0,
            paidFees = 8000.0,
            pendingFees = 32000.0,
            paymentPlan = "PART PAYMENT",
            installmentCount = 4,
            assignedTeacherId = "emp2",
            assignedTeacherName = "Pooja Verma",
            userId = "sturia4",
            nextDueDate = "2026-12-10",
            monthlyFeeAmount = 8000.0
        )

        val fiveInstStudent = StudentEntity(
            admissionNo = "STU-PART-5",
            name = "Kabir Mehta",
            contactNo = "9899887766",
            address = "Rohini",
            fatherName = "Deepak Mehta",
            motherName = "Seema Mehta",
            aadhaarCardUri = "/data/user/0/com.example/files/aadhaar/aadhaar_STU-PART-5.jpg",
            courseName = "Data Analytics with Python",
            batchName = "Evening Batch",
            batchTiming = "05:00 PM - 06:00 PM",
            dateOfJoining = "2026-11-12",
            courseDuration = "9 Months",
            totalFees = 50000.0,
            paidFees = 10000.0,
            pendingFees = 40000.0,
            paymentPlan = "PART PAYMENT",
            installmentCount = 5,
            assignedTeacherId = "emp1",
            assignedTeacherName = "Rahul Sharma",
            userId = "stukabir5",
            nextDueDate = "2026-12-12",
            monthlyFeeAmount = 8000.0
        )

        repository.insertStudent(fourInstStudent, "stud123", "subadmin1")
        repository.insertStudent(fiveInstStudent, "stud123", "subadmin1")

        val saved4 = db.appDao().getStudentByAdmissionNo("STU-PART-4")!!
        assertEquals("11:00 AM - 12:00 PM", saved4.batchTiming)
        assertEquals("/data/user/0/com.example/files/aadhaar/aadhaar_STU-PART-4.jpg", saved4.aadhaarCardUri)
        assertEquals(4, saved4.installmentCount)

        val inst4 = db.appDao().getInstallmentsForStudentFlow("STU-PART-4").first()
        assertEquals(4, inst4.size)
        assertEquals(32000.0, inst4.sumOf { it.installmentAmount }, 0.01)

        val saved5 = db.appDao().getStudentByAdmissionNo("STU-PART-5")!!
        assertEquals("05:00 PM - 06:00 PM", saved5.batchTiming)
        assertEquals(5, saved5.installmentCount)

        val inst5 = db.appDao().getInstallmentsForStudentFlow("STU-PART-5").first()
        assertEquals(5, inst5.size)
        assertEquals(40000.0, inst5.sumOf { it.installmentAmount }, 0.01)
    }

    @Test
    fun `addCourse persists new course names, blocks duplicates, and allows deletion`() = runBlocking {
        val addedFirst = repository.addCourse(
            CourseEntity(
                courseName = "Tally Prime & GST",
                defaultDuration = "3 Months",
                defaultFees = 15000.0,
                addedBy = "Vikram Malhotra (SUB_ADMIN)"
            ),
            creatorId = "subadmin1"
        )
        val addedDuplicate = repository.addCourse(
            CourseEntity(
                courseName = "tally prime & gst",
                defaultDuration = "4 Months",
                defaultFees = 18000.0,
                addedBy = "Super Admin (ADMIN)"
            ),
            creatorId = "admin"
        )
        val addedSecond = repository.addCourse(
            CourseEntity(
                courseName = "Spoken English Pro",
                defaultDuration = "6 Months",
                defaultFees = 20000.0,
                addedBy = "Super Admin (ADMIN)"
            ),
            creatorId = "admin"
        )

        assertEquals(true, addedFirst)
        assertEquals(false, addedDuplicate)
        assertEquals(true, addedSecond)

        val courses = db.appDao().getAllCoursesFlow().first()
        assertEquals(2, courses.size)
        assertEquals("Spoken English Pro", courses[0].courseName)
        assertEquals("Tally Prime & GST", courses[1].courseName)

        repository.deleteCourse("Tally Prime & GST", "admin")
        val remaining = db.appDao().getAllCoursesFlow().first()
        assertEquals(1, remaining.size)
        assertEquals("Spoken English Pro", remaining[0].courseName)
    }

    @Test
    fun `addBranch persists branches, prevents case-insensitive duplicates, and links staff and student branches with 1 to 12 month durations`() = runBlocking {
        val addedFirst = repository.addBranch(
            BranchEntity(
                branchName = "Laxmi Nagar Branch",
                address = "Near Metro Gate 2, Delhi",
                contactNo = "9811220011",
                addedBy = "Super Admin (ADMIN)"
            ),
            creatorId = "admin"
        )
        val addedDuplicate = repository.addBranch(
            BranchEntity(
                branchName = "laxmi nagar branch",
                address = "Duplicate",
                addedBy = "Sub Admin"
            ),
            creatorId = "subadmin1"
        )
        val addedSecond = repository.addBranch(
            BranchEntity(
                branchName = "Dwarka Sector 12 Branch",
                address = "Dwarka, Delhi",
                addedBy = "Vikram Malhotra (SUB_ADMIN)"
            ),
            creatorId = "subadmin1"
        )

        assertEquals(true, addedFirst)
        assertEquals(false, addedDuplicate)
        assertEquals(true, addedSecond)

        val branches = db.appDao().getAllBranchesFlow().first()
        assertEquals(2, branches.size)

        val staff = EmployeeEntity(
            employeeId = "EMP-BR-1",
            name = "Ankit Verma",
            contactNo = "9871112233",
            department = "Teaching Staff",
            branchName = "Laxmi Nagar Branch",
            dateOfJoining = "2026-11-15"
        )
        repository.insertEmployee(staff, "emp123")
        val savedStaff = db.appDao().getEmployeeById("EMP-BR-1")
        assertNotNull(savedStaff)
        assertEquals("Laxmi Nagar Branch", savedStaff!!.branchName)

        val oneMonthStudent = StudentEntity(
            admissionNo = "STU-1M",
            name = "Karan Joshi",
            contactNo = "9810011223",
            address = "Laxmi Nagar",
            courseName = "Crash Course",
            courseDuration = "1 Month",
            branchName = "Laxmi Nagar Branch",
            dateOfJoining = "2026-11-15",
            totalFees = 5000.0,
            paidFees = 5000.0,
            pendingFees = 0.0,
            paymentPlan = "ONETIME",
            assignedTeacherId = "EMP-BR-1",
            assignedTeacherName = "Ankit Verma",
            userId = "stukaran1m"
        )
        val twelveMonthStudent = StudentEntity(
            admissionNo = "STU-12M",
            name = "Simran Kaur",
            contactNo = "9810044556",
            address = "Dwarka",
            courseName = "Diploma in Software Engineering",
            courseDuration = "12 Months (1 Year)",
            branchName = "Dwarka Sector 12 Branch",
            dateOfJoining = "2026-11-15",
            totalFees = 60000.0,
            paidFees = 10000.0,
            pendingFees = 50000.0,
            paymentPlan = "MONTHLY",
            installmentCount = 12,
            assignedTeacherId = "EMP-BR-1",
            assignedTeacherName = "Ankit Verma",
            userId = "stusimran12m"
        )

        repository.insertStudent(oneMonthStudent, "stud123", "subadmin1")
        repository.insertStudent(twelveMonthStudent, "stud123", "admin")

        val saved1M = db.appDao().getStudentByAdmissionNo("STU-1M")!!
        val saved12M = db.appDao().getStudentByAdmissionNo("STU-12M")!!
        assertEquals("1 Month", saved1M.courseDuration)
        assertEquals("Laxmi Nagar Branch", saved1M.branchName)
        assertEquals("12 Months (1 Year)", saved12M.courseDuration)
        assertEquals("Dwarka Sector 12 Branch", saved12M.branchName)

        repository.deleteBranch("Dwarka Sector 12 Branch", "admin")
        val remainingBranches = db.appDao().getAllBranchesFlow().first()
        assertEquals(1, remainingBranches.size)
        assertEquals("Laxmi Nagar Branch", remainingBranches[0].branchName)
    }

    @Test
    fun `getHolidayReasonForDate detects Sunday weekly off and custom holidays`() {
        val settings = OfficeSettingsEntity(weeklyOff = "SUNDAY")
        val calSunday = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 10, 0, 0) // 2026-11-15 is a Sunday
        }
        val calMonday = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 16, 10, 0, 0) // 2026-11-16 is a Monday
        }

        val sundayReason = repository.getHolidayReasonForDate(calSunday.time, settings, emptyList())
        val mondayReason = repository.getHolidayReasonForDate(calMonday.time, settings, emptyList())
        val holidayReason = repository.getHolidayReasonForDate(
            calMonday.time,
            settings,
            listOf(HolidayEntity(1, "Institute Foundation Day", "2026-11-16"))
        )

        assertNotNull(sundayReason)
        assertEquals(true, sundayReason!!.contains("Sunday", ignoreCase = true))
        assertEquals(null, mondayReason)
        assertNotNull(holidayReason)
        assertEquals(true, holidayReason!!.contains("Institute Foundation Day"))
    }

    @Test
    fun `student GPS photo attendance and subadmin self attendance persist and calculate check-out hours`() = runBlocking {
        repository.seedSubAdminEmployeeIfMissing()
        val subAdminEmp = db.appDao().getEmployeeById("subadmin1")
        assertNotNull(subAdminEmp)
        assertEquals("Sub-Admin", subAdminEmp!!.department)

        val stuAtt = StudentAttendanceEntity(
            admissionNo = "LA-GPS-1",
            studentName = "Aman Gupta",
            courseName = "Full Stack Web Development",
            batchName = "Batch A",
            date = "2026-11-16",
            status = "PRESENT",
            markedByUserId = "SELF_GPS_PHOTO",
            checkInTime = "09:00:00",
            requiredMinutes = 120,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 4.2,
            selfieUri = "/data/user/0/com.example/files/selfies/selfie_LA-GPS-1.jpg",
            isMockLocation = false,
            deviceModel = "Pixel 8"
        )
        db.appDao().insertStudentAttendance(stuAtt)

        val clockedOut = repository.clockOutStudent("LA-GPS-1", "2026-11-16", "11:30:00")
        assertNotNull(clockedOut)
        assertEquals(150, clockedOut!!.workedMinutes)
        assertEquals(0, clockedOut.overtimeMinutes)
        assertEquals(28.6139, clockedOut.latitude, 0.0001)
        assertEquals(77.2090, clockedOut.longitude, 0.0001)
        assertEquals("/data/user/0/com.example/files/selfies/selfie_LA-GPS-1.jpg", clockedOut.selfieUri)
    }

    @Test
    fun `markBatchStudentAttendance persists batch records and teachers cannot view student fees`() = runBlocking {
        repository.prepopulateIfNeeded()

        val trainer1 = db.appDao().getEmployeeById("trainer1")
        val trainer2 = db.appDao().getEmployeeById("trainer2")
        val subadmin = db.appDao().getEmployeeById("subadmin1")
        assertNotNull(trainer1)
        assertNotNull(trainer2)
        assertNotNull(subadmin)
        assertEquals(false, trainer1!!.canViewFees)
        assertEquals(false, trainer2!!.canViewFees)
        assertEquals(true, subadmin!!.canViewFees)

        val records = listOf(
            StudentAttendanceEntity(
                admissionNo = "LA10025",
                studentName = "Rahul Kumar",
                courseName = "Full Stack Web Development",
                batchName = "Morning Batch A",
                date = "2026-11-16",
                status = "PRESENT",
                markedByUserId = "trainer1",
                checkInTime = "10:00:00",
                checkOutTime = "12:00:00",
                workedMinutes = 120,
                requiredMinutes = 120
            ),
            StudentAttendanceEntity(
                admissionNo = "STU-2024-001",
                studentName = "Aman Gupta",
                courseName = "Full Stack Web Development",
                batchName = "Morning Batch A",
                date = "2026-11-16",
                status = "ABSENT",
                markedByUserId = "trainer1",
                workedMinutes = 0,
                requiredMinutes = 120
            )
        )
        repository.markBatchStudentAttendance(
            records = records,
            actorId = "trainer1",
            actorRole = "EMPLOYEE",
            batchLabel = "Morning Batch A"
        )

        val rahulAtt = db.appDao().getStudentAttendanceByDate("LA10025", "2026-11-16")
        val amanAtt = db.appDao().getStudentAttendanceByDate("STU-2024-001", "2026-11-16")
        assertNotNull(rahulAtt)
        assertNotNull(amanAtt)
        assertEquals("PRESENT", rahulAtt!!.status)
        assertEquals("ABSENT", amanAtt!!.status)
        assertEquals("Morning Batch A", rahulAtt.batchName)
    }

    @Test
    fun `sunday weekly off allows self login-logout attendance and batch attendance`() = runBlocking {
        val settings = OfficeSettingsEntity(weeklyOff = "SUNDAY", standardDailyHours = 8.0, standardStudentDailyHours = 2.0)
        val calSunday = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 9, 0, 0) // Sunday
        }
        val reason = repository.getHolidayReasonForDate(calSunday.time, settings, emptyList())
        assertEquals("Sunday Weekly Off", reason)

        // Employee Sunday Login & Log Out
        db.appDao().insertEmployeeAttendance(
            EmployeeAttendanceEntity(
                employeeId = "trainer1",
                employeeName = "Rahul Sharma",
                department = "Teaching Staff",
                date = "2026-11-15",
                time = "09:30:00",
                status = "PRESENT",
                latitude = 28.6139,
                longitude = 77.2090,
                distanceMeters = 3.0,
                selfieUri = "/data/user/0/com.example/files/selfies/sunday.jpg",
                notes = "Google GPS + Photo Attendance Login (Sunday Weekly Off)"
            )
        )
        val loggedOutEmp = repository.clockOutEmployee("trainer1", "2026-11-15", "13:30:00")
        assertNotNull(loggedOutEmp)
        assertEquals("09:30:00", loggedOutEmp!!.time)
        assertEquals("13:30:00", loggedOutEmp.checkOutTime)
        assertEquals(240, loggedOutEmp.workedMinutes)

        // Batch Attendance on Sunday
        repository.markBatchStudentAttendance(
            records = listOf(
                StudentAttendanceEntity(
                    admissionNo = "LA-SUN-1",
                    studentName = "Sunday Batch Student",
                    courseName = "Full Stack Web Development",
                    batchName = "Sunday Special Batch",
                    date = "2026-11-15",
                    status = "PRESENT",
                    markedByUserId = "trainer1",
                    checkInTime = "10:00:00",
                    checkOutTime = "12:00:00",
                    workedMinutes = 120,
                    requiredMinutes = 120
                )
            ),
            actorId = "trainer1",
            actorRole = "EMPLOYEE",
            batchLabel = "Sunday Special Batch"
        )
        val savedSunStu = db.appDao().getStudentAttendanceByDate("LA-SUN-1", "2026-11-15")
        assertNotNull(savedSunStu)
        assertEquals("PRESENT", savedSunStu!!.status)
    }

    @Test
    fun `deleteEmployee and deleteStudent remove active records, installments, and portal logins while preserving admin`() = runBlocking {
        repository.prepopulateIfNeeded()

        // Confirm seeded trainer1 and student1 exist before removal
        assertNotNull(db.appDao().getEmployeeById("trainer1"))
        assertNotNull(db.appDao().getUserById("trainer1"))
        assertNotNull(db.appDao().getStudentByAdmissionNo("LA10025"))
        assertNotNull(db.appDao().getUserById("student4"))
        assertEquals(true, db.appDao().getInstallmentsForStudentFlow("LA10025").first().isNotEmpty())

        // Remove teacher who left institute
        repository.deleteEmployee("trainer1", actorId = "admin")
        assertEquals(null, db.appDao().getEmployeeById("trainer1"))
        assertEquals(null, db.appDao().getUserById("trainer1"))

        // Safety check: deleting non-admin user never removes admin
        db.appDao().deleteNonAdminUser("admin")
        assertNotNull(db.appDao().getUserById("admin"))

        // Remove student who left institute
        repository.deleteStudent("LA10025", actorId = "admin")
        assertEquals(null, db.appDao().getStudentByAdmissionNo("LA10025"))
        assertEquals(null, db.appDao().getUserById("student4"))
        assertEquals(0, db.appDao().getInstallmentsForStudentFlow("LA10025").first().size)
        // Historical payment receipts remain intact for audit/accounting
        assertEquals(true, db.appDao().getPaymentsForStudentFlow("LA10025").first().isNotEmpty())
    }

    @Test
    fun `ExcelExporter produces valid multi-sheet xlsx OOXML zip archive with escaped strings and numbers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sheets = listOf(
            ExcelSheet(
                name = "Staff: Summary [2026]/11?*",
                titleBanner = "London Academy & Partners <2026>",
                headers = listOf("Employee ID", "Name", "Worked Days", "Amount"),
                rows = listOf(
                    listOf("trainer1", "Rahul & Priya", 22, 45000.0)
                )
            ),
            ExcelSheet(
                name = "Fee Ledger",
                headers = listOf("Receipt No", "Student", "Paid"),
                rows = listOf(
                    listOf("REC-101", "Aman Verma", 15000.5)
                )
            )
        )

        val file = ExcelExporter.writeWorkbookToFile(context, "Test Report Export", sheets)
        assertTrue(file.exists())
        assertTrue(file.name.endsWith(".xlsx"))
        assertTrue(file.length() > 0)

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(file.readBytes())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))

        val sheet1 = entries["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet1.contains("London Academy &amp; Partners &lt;2026&gt;"))
        assertTrue(sheet1.contains("Rahul &amp; Priya"))
        assertTrue(sheet1.contains("""t="n"><v>22</v>"""))
        assertTrue(sheet1.contains("""t="n"><v>45000</v>"""))

        val sheet2 = entries["xl/worksheets/sheet2.xml"]!!
        assertTrue(sheet2.contains("""t="n"><v>15000.5</v>"""))

        val workbook = entries["xl/workbook.xml"]!!
        assertTrue(workbook.contains("Fee Ledger"))
    }

    @Test
    fun `cloud sync with pruneDeleted removes deleted employees and students while provisioning portal logins`() = runBlocking {
        repository.prepopulateIfNeeded()

        val cloudEmployees = listOf(
            EmployeeEntity(
                employeeId = "trainer1",
                name = "Rahul Sharma",
                contactNo = "9999999999",
                department = "Teaching Staff",
                dateOfJoining = "2026-01-01"
            ),
            EmployeeEntity(
                employeeId = "trainer_new_cloud",
                name = "Cloud Teacher",
                contactNo = "8888888888",
                department = "Teaching Staff",
                dateOfJoining = "2026-11-10"
            )
        )
        repository.syncEmployeesFromCloud(cloudEmployees, pruneDeleted = true)

        assertNotNull(db.appDao().getEmployeeById("trainer_new_cloud"))
        assertNotNull(db.appDao().getUserById("trainer_new_cloud"))
        assertEquals(null, db.appDao().getEmployeeById("trainer2"))
        assertEquals(null, db.appDao().getUserById("trainer2"))
        assertNotNull(db.appDao().getUserById("admin"))

        val cloudStudents = listOf(
            StudentEntity(
                admissionNo = "LA-CLOUD-1",
                name = "Cloud Student",
                contactNo = "7777777777",
                address = "Delhi",
                courseName = "Python",
                dateOfJoining = "2026-11-10",
                courseDuration = "6 Months",
                totalFees = 20000.0,
                paidFees = 5000.0,
                pendingFees = 15000.0,
                paymentPlan = "MONTHLY",
                assignedTeacherId = "trainer1",
                assignedTeacherName = "Rahul Sharma",
                userId = "stu_cloud_1"
            )
        )
        repository.syncStudentsFromCloud(cloudStudents, pruneDeleted = true)

        assertNotNull(db.appDao().getStudentByAdmissionNo("LA-CLOUD-1"))
        assertNotNull(db.appDao().getUserById("stu_cloud_1"))
        assertEquals(null, db.appDao().getStudentByAdmissionNo("LA10025"))
        assertEquals(null, db.appDao().getUserById("student4"))
    }
}
