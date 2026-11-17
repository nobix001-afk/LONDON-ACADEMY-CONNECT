package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(viewModel: EduViewModel) {
    val currentEmp by viewModel.currentEmployee.collectAsState()
    val attendanceList by viewModel.allEmployeeAttendance.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val allStudentAttendance by viewModel.allStudentAttendance.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val holidayReason = remember(officeSettings, holidays) {
        viewModel.getTodayHolidayReason()
    }

    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showMarkStudentDialog by remember { mutableStateOf(false) }
    var showStudentReportsDialog by remember { mutableStateOf(false) }
    var inspectingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedBatchFilter by remember { mutableStateOf("ALL") }

    val emp = currentEmp
    if (emp == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No employee record found. Please login again.")
        }
        return
    }

    val stats = viewModel.getEmployeeAttendanceStats(emp)
    val workingDays = stats["workingDays"] as? Int ?: 0
    val presentDays = stats["daysAttended"] as? Int ?: (stats["presentDays"] as? Int ?: 0)
    val absentDays = stats["absentDays"] as? Int ?: 0
    val halfDays = stats["halfDays"] as? Int ?: 0
    val leaveDays = stats["leaveDays"] as? Int ?: 0
    val fullHoursDays = stats["fullHoursCompletedDays"] as? Int ?: 0
    val percentage = stats["percentage"] as? Double ?: 0.0

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())

    val myAttendanceHistory = attendanceList.filter { it.employeeId == emp.employeeId }
    val todayAttendance = myAttendanceHistory.firstOrNull { it.date == today }

    val assignedStudents = students.filter { it.assignedTeacherId == emp.employeeId }
    val batchOptions = remember(assignedStudents) {
        listOf("ALL") + assignedStudents.map { it.batchName.ifBlank { "General Batch" } }.distinct().sorted()
    }
    LaunchedEffect(batchOptions) {
        if (selectedBatchFilter !in batchOptions) selectedBatchFilter = "ALL"
    }
    val filteredAssignedStudents = remember(assignedStudents, selectedBatchFilter) {
        if (selectedBatchFilter == "ALL") assignedStudents
        else assignedStudents.filter { it.batchName.ifBlank { "General Batch" } == selectedBatchFilter }
    }
    val canManage = emp.canManageStudents

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_london_academy_logo),
                                    contentDescription = "London Academy Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("${emp.department} • London Academy", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("employee_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFFFB4BE))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Attendance Action Card (Teachers, Office Boy, Cleaning Staff, etc.)
            item {
                DailyGpsPhotoAttendanceCard(
                    roleLabel = emp.department,
                    todayStatus = todayAttendance?.status,
                    checkInTime = todayAttendance?.time ?: "",
                    checkOutTime = todayAttendance?.checkOutTime ?: "",
                    distanceMeters = todayAttendance?.distanceMeters ?: 0.0,
                    latitude = todayAttendance?.latitude ?: 0.0,
                    longitude = todayAttendance?.longitude ?: 0.0,
                    selfieUri = todayAttendance?.selfieUri ?: "",
                    workedMinutes = todayAttendance?.workedMinutes ?: 0,
                    allowedRadiusMeters = officeSettings?.allowedRadiusMeters ?: 10.0,
                    holidayReason = holidayReason,
                    markButtonTestTag = "button_mark_employee_attendance",
                    clockOutButtonTestTag = "button_clock_out_employee",
                    formatMinutes = { viewModel.formatMinutes(it) },
                    onMarkAttendance = { showAttendanceDialog = true },
                    onClockOut = { viewModel.clockOutToday() }
                )
            }

            // Attendance Statistics strictly based on DOJ
            item {
                SectionHeader(
                    title = "Monthly Attendance Metrics",
                    subtitle = "Working days strictly calculated from your Date of Joining (${emp.dateOfJoining})"
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Working Days",
                        value = "$workingDays",
                        subtitle = "From DOJ (${emp.dateOfJoining})",
                        icon = Icons.Default.CalendarMonth,
                        accentColor = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Attendance %",
                        value = "${percentage.toInt()}%",
                        subtitle = "Score for this month",
                        icon = Icons.Default.TrendingUp,
                        accentColor = if (percentage >= 75) SuccessGreen else TertiaryAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Present",
                        value = "$presentDays",
                        subtitle = "+$halfDays Half • $fullHoursDays Full Hrs",
                        icon = Icons.Default.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Leaves / Absent",
                        value = "$leaveDays L / $absentDays A",
                        subtitle = "Chhuti & absent days",
                        icon = Icons.Default.EventBusy,
                        accentColor = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Teacher / Trainer Section (Batch-wise Attendance & Academic Tracking — No Fee Info / No Add Student)
            if (canManage) {
                item {
                    SectionHeader(
                        title = "My Batch Students (${filteredAssignedStudents.size})",
                        subtitle = "Batch-wise student list and class attendance"
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showMarkStudentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("teacher_batch_attendance_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalDarkRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark Batch-Wise Student Attendance", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { showStudentReportsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("teacher_student_reports_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Student Attendance Reports", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (batchOptions.size > 1) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(batchOptions) { batch ->
                                FilterChip(
                                    selected = selectedBatchFilter == batch,
                                    onClick = { selectedBatchFilter = batch },
                                    label = {
                                        Text(
                                            text = if (batch == "ALL") "All Batches (${assignedStudents.size})" else batch,
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.testTag("teacher_batch_filter_${batch.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }

                if (filteredAssignedStudents.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.PeopleOutline,
                            message = "No students assigned to this batch yet"
                        )
                    }
                } else {
                    items(filteredAssignedStudents, key = { it.admissionNo }) { student ->
                        val stuReport = remember(student, allStudentAttendance, officeSettings, holidays) {
                            viewModel.getStudentWorkReport(student, viewModel.currentMonthKey())
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_student_card_${student.admissionNo}")
                                .clickable { inspectingStudent = student },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = buildString {
                                                append("${student.courseName} • ${student.batchName}")
                                                if (student.batchTiming.isNotBlank()) append(" (${student.batchTiming})")
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Branch: ${student.branchName}",
                                            fontSize = 10.sp,
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    StatusPill(student.status)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = PrimaryBlue.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Attendance: ${stuReport.attendancePercentage.toInt()}% (${stuReport.daysAttended}/${stuReport.totalClassDays} d)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                        Text(
                                            text = "${stuReport.leaveDays}L • ${stuReport.absentDays}A",
                                            fontSize = 11.sp,
                                            color = if (stuReport.absentDays > 0) ErrorRed else SuccessGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Adm #: ${student.admissionNo}", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                    Text("Attendance Report →", fontSize = 11.sp, color = RoyalDarkRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Employee Attendance History
            item {
                SectionHeader(title = "My Attendance History")
            }

            if (myAttendanceHistory.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.EventNote,
                        message = "No check-ins recorded yet"
                    )
                }
            } else {
                items(myAttendanceHistory) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (record.selfieUri.isNotEmpty()) {
                                AttendancePhotoThumbnail(
                                    selfieUri = record.selfieUri,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "Login: ${record.time.take(5)}${if (record.checkOutTime.isNotEmpty()) " • Log Out: ${record.checkOutTime.take(5)}" else ""} • GPS ${record.distanceMeters.toInt()}m",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (record.workedMinutes > 0) {
                                    Text(
                                        text = "⏱ Worked: ${viewModel.formatMinutes(record.workedMinutes)}",
                                        fontSize = 10.sp,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (record.selfieUri.isNotEmpty()) {
                                    Text("Photo + Google GPS Verified", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                                }
                            }
                            StatusPill(record.status)
                        }
                    }
                }
            }
        }
    }

    if (showAttendanceDialog) {
        AttendanceMarkingDialog(viewModel = viewModel, onDismiss = { showAttendanceDialog = false })
    }

    if (showMarkStudentDialog) {
        BatchStudentAttendanceDialog(
            viewModel = viewModel,
            assignedStudents = assignedStudents,
            showScopeToggle = true,
            onDismiss = { showMarkStudentDialog = false }
        )
    }

    if (showStudentReportsDialog) {
        TeacherStudentAttendanceReportsDialog(
            viewModel = viewModel,
            assignedStudents = assignedStudents,
            onInspectStudent = { stu -> inspectingStudent = stu },
            onDismiss = { showStudentReportsDialog = false }
        )
    }

    inspectingStudent?.let { stu ->
        StudentDetailReportDialog(
            student = stu,
            viewModel = viewModel,
            initialMonthFilter = viewModel.currentMonthKey(),
            hideFees = true,
            onDismiss = { inspectingStudent = null }
        )
    }
}

@Composable
fun TeacherStudentAttendanceReportsDialog(
    viewModel: EduViewModel,
    assignedStudents: List<StudentEntity>,
    onInspectStudent: (StudentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val allStudents by viewModel.allStudents.collectAsState()
    var selectedMonth by remember { mutableStateOf(viewModel.currentMonthKey()) }
    var onlyAssigned by remember { mutableStateOf(assignedStudents.isNotEmpty()) }

    val scopedStudents = remember(onlyAssigned, assignedStudents, allStudents) {
        if (onlyAssigned) assignedStudents else allStudents
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("teacher_student_reports_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = Navy900,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Student Attendance Reports",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Batch-wise attendance, study hours & daily logs",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_teacher_student_reports")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = onlyAssigned,
                            onClick = { onlyAssigned = true },
                            label = { Text("My Students (${assignedStudents.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("teacher_report_scope_mine")
                        )
                        FilterChip(
                            selected = !onlyAssigned,
                            onClick = { onlyAssigned = false },
                            label = { Text("All Campus (${allStudents.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("teacher_report_scope_all")
                        )
                    }

                    StudentWorkReportsSection(
                        viewModel = viewModel,
                        selectedMonth = selectedMonth,
                        onMonthChange = { selectedMonth = it },
                        onInspectStudent = onInspectStudent,
                        studentsOverride = scopedStudents,
                        hideFees = true
                    )
                }
            }
        }
    }
}
