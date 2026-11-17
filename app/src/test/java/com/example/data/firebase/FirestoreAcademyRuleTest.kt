package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BranchEntity
import com.example.data.model.CourseEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.FeePaymentEntity
import com.example.data.model.OfficeSettingsEntity
import com.example.data.model.StudentAttendanceEntity
import com.example.data.model.StudentEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreAcademyRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andCrudForBranchesCoursesStudentsAttendance_succeedForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreAcademyRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(
                userId = uid,
                displayName = "Alice Admin",
                email = ALICE_EMAIL,
                activePortalRole = "ADMIN"
            )
        }
        assertTrue(profileRes.isSuccess)

        val branchName = "Branch_${UUID.randomUUID().toString().take(8)}"
        val branchRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveBranch(
                uid,
                BranchEntity(
                    branchName = branchName,
                    address = "Sector 5, New Delhi",
                    contactNo = "9876543210",
                    addedBy = "ADMIN"
                )
            )
        }
        assertTrue(branchRes.isSuccess)

        val observedBranches = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeBranches(uid).first { list -> list.any { it.branchName == branchName } }
        }
        assertTrue(observedBranches.any { it.branchName == branchName })

        val courseName = "Course_${UUID.randomUUID().toString().take(8)}"
        val courseRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveCourse(
                uid,
                CourseEntity(
                    courseName = courseName,
                    defaultDuration = "12 Months",
                    defaultFees = 45000.0,
                    addedBy = "ADMIN"
                )
            )
        }
        assertTrue(courseRes.isSuccess)

        val empId = "emp_${UUID.randomUUID().toString().take(8)}"
        val empRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveEmployee(
                uid,
                EmployeeEntity(
                    employeeId = empId,
                    name = "Rahul Sharma",
                    contactNo = "9876543212",
                    email = "rahul@londonacademy.edu",
                    department = "Teaching Staff",
                    branchName = branchName,
                    dateOfJoining = "2026-01-10"
                )
            )
        }
        assertTrue(empRes.isSuccess)

        val admNo = "LA_${UUID.randomUUID().toString().take(8)}"
        val stuRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveStudent(
                uid,
                StudentEntity(
                    admissionNo = admNo,
                    name = "Aman Gupta",
                    contactNo = "9876543216",
                    address = "Green Park",
                    courseName = courseName,
                    branchName = branchName,
                    batchName = "Morning Batch A",
                    dateOfJoining = "2026-11-01",
                    courseDuration = "6 Months",
                    totalFees = 45000.0,
                    paidFees = 15000.0,
                    pendingFees = 30000.0,
                    paymentPlan = "PART PAYMENT",
                    installmentCount = 3,
                    assignedTeacherId = empId,
                    assignedTeacherName = "Rahul Sharma",
                    userId = "stu_$admNo"
                )
            )
        }
        assertTrue(stuRes.isSuccess)

        val attRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveStudentAttendance(
                uid,
                StudentAttendanceEntity(
                    admissionNo = admNo,
                    studentName = "Aman Gupta",
                    courseName = courseName,
                    batchName = "Morning Batch A",
                    date = "2026-11-15",
                    status = "PRESENT",
                    markedByUserId = empId,
                    checkInTime = "10:00:00",
                    checkOutTime = "12:00:00",
                    workedMinutes = 120,
                    requiredMinutes = 120,
                    notes = "Sunday Weekly Off • Attendance Open"
                )
            )
        }
        assertTrue(attRes.isSuccess)

        val payRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveFeePayment(
                uid,
                FeePaymentEntity(
                    receiptNo = "REC-${UUID.randomUUID().toString().take(8)}",
                    admissionNo = admNo,
                    studentName = "Aman Gupta",
                    amount = 15000.0,
                    paymentDate = "2026-11-15",
                    paymentMode = "UPI",
                    paymentPlan = "PART PAYMENT",
                    collectedBy = "ADMIN"
                )
            )
        }
        assertTrue(payRes.isSuccess)

        val settingsRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveOfficeSettings(uid, OfficeSettingsEntity())
        }
        assertTrue(settingsRes.isSuccess)

        val auditRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.logAudit(
                userId = uid,
                actorId = "admin",
                role = "ADMIN",
                action = "TEST_LOG",
                details = "Rule verification audit entry",
                date = "2026-11-15",
                time = "10:00:00"
            )
        }
        assertTrue(auditRes.isSuccess)
    }

    @Test
    fun getStudentByAdmissionNo_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreAcademyRepository(firestore)
        val admNo = "LA_${UUID.randomUUID().toString().take(8)}"

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveStudent(
                aliceUid,
                StudentEntity(
                    admissionNo = admNo,
                    name = "Alice Private Student",
                    contactNo = "9876543216",
                    address = "Delhi",
                    courseName = "Full Stack",
                    branchName = "Main Campus (Delhi)",
                    batchName = "Batch A",
                    dateOfJoining = "2026-11-01",
                    courseDuration = "6 Months",
                    totalFees = 50000.0,
                    paidFees = 10000.0,
                    pendingFees = 40000.0,
                    paymentPlan = "MONTHLY",
                    installmentCount = 6,
                    assignedTeacherId = "trainer1",
                    assignedTeacherName = "Rahul Sharma",
                    userId = "stu_private"
                )
            )
        }
        assertTrue(saveRes.isSuccess)

        // Switch authenticated user to Bob and attempt to read Alice's student record
        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreAcademyRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getStudentByAdmissionNo(aliceUid, admNo)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeStudents_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreAcademyRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeStudents("some_user_uid").first()
            }
            fail("Expected PERMISSION_DENIED when observing students while signed out")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected nested FirebaseFirestoreException, got $e", e)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
