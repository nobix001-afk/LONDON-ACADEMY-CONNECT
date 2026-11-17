package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.ExcelExporter
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

enum class AdminTab(val title: String) {
    OVERVIEW("Overview"),
    EMPLOYEES("Staff"),
    STUDENTS("Students"),
    ATTENDANCE("Attendance"),
    FEES("Fees & Pending Fees"),
    NOTICES("Notices"),
    REPORTS("Reports"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: EduViewModel) {
    var currentTab by remember { mutableStateOf(AdminTab.OVERVIEW) }

    var showStudentDialog by remember { mutableStateOf(false) }
    var showFeeDialog by remember { mutableStateOf(false) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showSendNoticeDialog by remember { mutableStateOf(false) }
    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var showManageCoursesDialog by remember { mutableStateOf(false) }
    var showManageBranchesDialog by remember { mutableStateOf(false) }
    var showBatchAttendanceDialog by remember { mutableStateOf(false) }
    var inspectingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var inspectingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var employeeToRemove by remember { mutableStateOf<EmployeeEntity?>(null) }
    var studentToRemove by remember { mutableStateOf<StudentEntity?>(null) }
    var reportSubTab by remember { mutableIntStateOf(0) } // 0 = Staff, 1 = Students, 2 = Exports & Logs
    var selectedReportMonth by remember { mutableStateOf(viewModel.currentMonthKey()) }

    val context = LocalContext.current
    val employees by viewModel.allEmployees.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val dbCourses by viewModel.allCourses.collectAsState()
    val dbBranches by viewModel.allBranches.collectAsState()
    val employeeAttendance by viewModel.allEmployeeAttendance.collectAsState()
    val payments by viewModel.allPayments.collectAsState()
    val notifications by viewModel.allNotifications.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val totalFees = students.sumOf { it.totalFees }
    val paidFees = students.sumOf { it.paidFees }
    val pendingFees = students.sumOf { it.pendingFees }

    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdfDate.format(Date())
    val todayPresent = employeeAttendance.count { it.date == today && it.status == "PRESENT" }

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
                            Text("London Academy Connect", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Super Administrator Console", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFFFB4BE))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                listOf(
                    AdminTab.OVERVIEW to Icons.Default.Dashboard,
                    AdminTab.EMPLOYEES to Icons.Default.Badge,
                    AdminTab.STUDENTS to Icons.Default.School,
                    AdminTab.FEES to Icons.Default.Payments,
                    AdminTab.SETTINGS to Icons.Default.Settings
                ).forEach { (tab, icon) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            when (currentTab) {
                AdminTab.STUDENTS -> {
                    FloatingActionButton(
                        onClick = { showStudentDialog = true },
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add_student")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Student")
                    }
                }
                AdminTab.EMPLOYEES -> {
                    FloatingActionButton(
                        onClick = { showAddEmployeeDialog = true },
                        containerColor = SecondaryTeal,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add_employee")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Add Employee")
                    }
                }
                AdminTab.FEES -> {
                    // Handled inside PendingFeesScreen with + Add Fee Record and Export actions
                }
                AdminTab.NOTICES -> {
                    FloatingActionButton(
                        onClick = { showSendNoticeDialog = true },
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_send_notice")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Send Notice")
                    }
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Secondary category selector row for extended tabs
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                edgePadding = 12.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = { Text(tab.title, fontSize = 12.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Tab contents
            when (currentTab) {
                AdminTab.OVERVIEW -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(title = "Key Institutional Metrics")
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Total Staff",
                                    value = "${employees.size}",
                                    subtitle = "$todayPresent Present Today",
                                    icon = Icons.Default.Badge,
                                    accentColor = SecondaryTeal,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentTab = AdminTab.EMPLOYEES }
                                )
                                StatCard(
                                    title = "Students",
                                    value = "${students.size}",
                                    subtitle = "Active Enrollment",
                                    icon = Icons.Default.School,
                                    accentColor = PrimaryBlue,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentTab = AdminTab.STUDENTS }
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Fee Collected",
                                    value = "₹${(paidFees / 1000).toInt()}k",
                                    subtitle = "Total: ₹${(totalFees / 1000).toInt()}k",
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = SuccessGreen,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentTab = AdminTab.FEES }
                                )
                                StatCard(
                                    title = "Pending Due",
                                    value = "₹${(pendingFees / 1000).toInt()}k",
                                    subtitle = "${students.count { it.pendingFees > 0 }} Students Pending",
                                    icon = Icons.Default.Warning,
                                    accentColor = TertiaryAmber,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentTab = AdminTab.FEES }
                                )
                            }
                        }
                        item {
                            SectionHeader(
                                title = "Quick Actions",
                                subtitle = "Manage day-to-day administrative operations"
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showStudentDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Student", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { showFeeDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fee Pay", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { showSendNoticeDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Notice", fontSize = 11.sp)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showManageCoursesDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_add_course_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Courses (${dbCourses.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { showManageBranchesDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_add_branch_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Branches (${dbBranches.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Button(
                                    onClick = { showBatchAttendanceDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_quick_batch_attendance_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalDarkRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Batch-Wise Student Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        reportSubTab = 0
                                        currentTab = AdminTab.REPORTS
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("overview_staff_reports_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Staff Full Reports", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        reportSubTab = 1
                                        currentTab = AdminTab.REPORTS
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("overview_student_reports_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Student Full Reports", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        item {
                            SectionHeader(title = "Recent Fee Receipts")
                        }
                        if (payments.isEmpty()) {
                            item { EmptyStateView(message = "No payments recorded yet") }
                        } else {
                            items(payments.take(4)) { p ->
                                FeeReceiptCard(p)
                            }
                        }
                    }
                }

                AdminTab.EMPLOYEES -> {
                    var searchEmp by remember { mutableStateOf("") }
                    val filteredEmployees = employees.filter {
                        it.name.contains(searchEmp, ignoreCase = true) ||
                        it.department.contains(searchEmp, ignoreCase = true) ||
                        it.employeeId.contains(searchEmp, ignoreCase = true) ||
                        it.branchName.contains(searchEmp, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            FilledTonalButton(
                                onClick = { showManageBranchesDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_employees_add_branch_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add / Manage Branch Names", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = searchEmp,
                                onValueChange = { searchEmp = it },
                                label = { Text("Search staff by name, dept or branch") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true
                            )
                        }
                        item {
                            SectionHeader(
                                title = "Staff Members (${filteredEmployees.size})",
                                subtitle = "Tap any employee to view full hours, extra time & leave report",
                                actionText = "Full Reports →",
                                onAction = {
                                    reportSubTab = 0
                                    currentTab = AdminTab.REPORTS
                                }
                            )
                        }
                        items(filteredEmployees) { emp ->
                            val stats = viewModel.getEmployeeAttendanceStats(emp)
                            EmployeeAdminCard(
                                employee = emp,
                                workingDays = stats["workingDays"] as? Int ?: 0,
                                presentDays = stats["daysAttended"] as? Int ?: (stats["presentDays"] as? Int ?: 0),
                                absentDays = stats["absentDays"] as? Int ?: 0,
                                halfDays = stats["halfDays"] as? Int ?: 0,
                                percentage = stats["percentage"] as? Double ?: 0.0,
                                fullHoursDays = stats["fullHoursCompletedDays"] as? Int ?: 0,
                                totalWorkedLabel = viewModel.formatMinutes(stats["totalWorkedMinutes"] as? Int ?: 0),
                                leaveDays = stats["leaveDays"] as? Int ?: 0,
                                onClick = { inspectingEmployee = emp },
                                onRemove = { employeeToRemove = emp }
                            )
                        }
                    }
                }

                AdminTab.STUDENTS -> {
                    var searchStudent by remember { mutableStateOf("") }
                    var selectedCourseFilter by remember { mutableStateOf("All") }
                    val courses = listOf("All") + (dbCourses.map { it.courseName } + students.map { it.courseName }).distinct()

                    val filteredStudents = students.filter {
                        (selectedCourseFilter == "All" || it.courseName == selectedCourseFilter) &&
                        (it.name.contains(searchStudent, ignoreCase = true) ||
                         it.admissionNo.contains(searchStudent, ignoreCase = true) ||
                         it.courseName.contains(searchStudent, ignoreCase = true))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showBatchAttendanceDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_students_batch_attendance_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalDarkRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Batch-Wise Student Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { showManageCoursesDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_students_add_course_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Course", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = { showManageBranchesDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_students_add_branch_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Branch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = searchStudent,
                                onValueChange = { searchStudent = it },
                                label = { Text("Search by student name or admission #") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true
                            )
                        }
                        item {
                            ScrollableTabRow(
                                selectedTabIndex = courses.indexOf(selectedCourseFilter).coerceAtLeast(0),
                                edgePadding = 0.dp,
                                divider = {}
                            ) {
                                courses.forEach { c ->
                                    FilterChip(
                                        selected = selectedCourseFilter == c,
                                        onClick = { selectedCourseFilter = c },
                                        label = { Text(c, fontSize = 11.sp) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                        item {
                            SectionHeader(
                                title = "Enrolled Students (${filteredStudents.size})",
                                subtitle = "Tap any student for complete attendance, hours, leave & fee payment report",
                                actionText = "Full Reports →",
                                onAction = {
                                    reportSubTab = 1
                                    currentTab = AdminTab.REPORTS
                                }
                            )
                        }
                        items(filteredStudents) { student ->
                            val stuReport = viewModel.getStudentWorkReport(student, viewModel.currentMonthKey())
                            StudentAdminCard(
                                student = student,
                                daysAttended = stuReport.daysAttended,
                                fullHoursDays = stuReport.fullHoursCompletedDays,
                                leaveDays = stuReport.leaveDays,
                                paymentCount = stuReport.payments.size,
                                onClick = { inspectingStudent = student },
                                onRemove = { studentToRemove = student }
                            )
                        }
                    }
                }

                AdminTab.ATTENDANCE -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showBatchAttendanceDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_attendance_batch_button"),
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
                        }
                        item {
                            SectionHeader(
                                title = "GPS & Shift Attendance Logs",
                                subtitle = "Coordinates verified within ${officeSettings?.allowedRadiusMeters?.toInt() ?: 10}m geofence • Tap for staff report",
                                actionText = "Staff Reports →",
                                onAction = {
                                    reportSubTab = 0
                                    currentTab = AdminTab.REPORTS
                                }
                            )
                        }
                        if (employeeAttendance.isEmpty()) {
                            item {
                                EmptyStateView(message = "No employee attendance records found")
                            }
                        } else {
                            items(employeeAttendance) { att ->
                                val matchedEmp = employees.firstOrNull { it.employeeId == att.employeeId }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = matchedEmp != null) {
                                            matchedEmp?.let { inspectingEmployee = it }
                                        },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(att.employeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = "${att.department} | ${att.date} (${att.time.take(5)}${if (att.checkOutTime.isNotEmpty()) " - ${att.checkOutTime.take(5)}" else ""})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (att.workedMinutes > 0) {
                                                Text(
                                                    text = "⏱ Worked: ${viewModel.formatMinutes(att.workedMinutes)}",
                                                    fontSize = 11.sp,
                                                    color = PrimaryBlue,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = "📍 Distance: ${att.distanceMeters.toInt()}m ${if (att.isMockLocation) "⚠️ Fake Mock GPS" else "✓ Verified GPS"}",
                                                fontSize = 11.sp,
                                                color = if (att.isMockLocation) ErrorRed else SuccessGreen
                                            )
                                        }
                                        StatusPill(att.status)
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.FEES -> {
                    PendingFeesScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AdminTab.NOTICES -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Targeted Announcements",
                                subtitle = "Offers, discounts, holidays, and fee alerts sent to students & employees",
                                actionText = "+ New Notice",
                                onAction = { showSendNoticeDialog = true }
                            )
                        }
                        if (notifications.isEmpty()) {
                            item { EmptyStateView(message = "No notices dispatched yet") }
                        } else {
                            items(notifications) { notif ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Surface(
                                                color = PrimaryBlue.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    notif.category,
                                                    color = PrimaryBlue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(notif.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Target: ${notif.targetType}", fontSize = 10.sp, color = Navy700)
                                            Text("${notif.date} ${notif.time}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.REPORTS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = reportSubTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Tab(
                                selected = reportSubTab == 0,
                                onClick = { reportSubTab = 0 },
                                text = { Text("Staff Reports", fontSize = 12.sp, fontWeight = if (reportSubTab == 0) FontWeight.Bold else FontWeight.Normal) }
                            )
                            Tab(
                                selected = reportSubTab == 1,
                                onClick = { reportSubTab = 1 },
                                text = { Text("Student Reports", fontSize = 12.sp, fontWeight = if (reportSubTab == 1) FontWeight.Bold else FontWeight.Normal) }
                            )
                            Tab(
                                selected = reportSubTab == 2,
                                onClick = { reportSubTab = 2 },
                                text = { Text("Exports & Logs", fontSize = 12.sp, fontWeight = if (reportSubTab == 2) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (reportSubTab) {
                                0 -> {
                                    EmployeeWorkReportsSection(
                                        viewModel = viewModel,
                                        selectedMonth = selectedReportMonth,
                                        onMonthChange = { selectedReportMonth = it },
                                        onInspectEmployee = { inspectingEmployee = it }
                                    )
                                }
                                1 -> {
                                    StudentWorkReportsSection(
                                        viewModel = viewModel,
                                        selectedMonth = selectedReportMonth,
                                        onMonthChange = { selectedReportMonth = it },
                                        onInspectStudent = { inspectingStudent = it }
                                    )
                                }
                                else -> {
                                    SectionHeader(title = "Institutional Reports & Exports")

                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Employee Attendance & Work Hours (Excel)", fontWeight = FontWeight.Bold)
                                            Text("Export all staff check-ins, check-outs, worked hours, extra time, and leaves as an .xlsx Excel workbook.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    val file = viewModel.exportAttendanceReportExcel(context)
                                                    ExcelExporter.shareExcelFile(context, file, "Share Attendance Excel Report")
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp).testTag("export_attendance_excel_btn"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Export Attendance Excel (.xlsx)")
                                            }
                                        }
                                    }

                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Fee Collection & Outstanding Report (Excel)", fontWeight = FontWeight.Bold)
                                            Text("Export comprehensive student fee installments, payments, mode, and receipts as an .xlsx Excel workbook.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    val file = viewModel.exportFeeReportExcel(context)
                                                    ExcelExporter.shareExcelFile(context, file, "Share Fee Excel Report")
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp).testTag("export_fee_excel_btn"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Export Fee Report Excel (.xlsx)")
                                            }
                                        }
                                    }

                                    SectionHeader(title = "Recent System Audit Logs")
                                    auditLogs.take(10).forEach { log ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("${log.role}: ${log.action}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${log.date} ${log.time}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.SETTINGS -> {
                    AdminSettingsTab(
                        viewModel = viewModel,
                        onAddHoliday = { showAddHolidayDialog = true }
                    )
                }
            }
        }
    }

    if (showStudentDialog) {
        StudentRegistrationDialog(viewModel = viewModel, onDismiss = { showStudentDialog = false })
    }

    if (showFeeDialog) {
        CollectFeeDialog(viewModel = viewModel, onDismiss = { showFeeDialog = false })
    }

    if (showAddEmployeeDialog) {
        AddEmployeeDialog(viewModel = viewModel, onDismiss = { showAddEmployeeDialog = false })
    }

    if (showSendNoticeDialog) {
        SendNotificationDialog(viewModel = viewModel, onDismiss = { showSendNoticeDialog = false })
    }

    if (showAddHolidayDialog) {
        AddHolidayDialog(viewModel = viewModel, onDismiss = { showAddHolidayDialog = false })
    }

    if (showManageCoursesDialog) {
        ManageCoursesDialog(viewModel = viewModel, onDismiss = { showManageCoursesDialog = false })
    }

    if (showManageBranchesDialog) {
        ManageBranchesDialog(viewModel = viewModel, onDismiss = { showManageBranchesDialog = false })
    }

    if (showBatchAttendanceDialog) {
        BatchStudentAttendanceDialog(
            viewModel = viewModel,
            showScopeToggle = false,
            onDismiss = { showBatchAttendanceDialog = false }
        )
    }

    inspectingEmployee?.let { emp ->
        EmployeeDetailReportDialog(
            employee = emp,
            viewModel = viewModel,
            initialMonthFilter = selectedReportMonth,
            onRemove = { employeeToRemove = emp },
            onDismiss = { inspectingEmployee = null }
        )
    }

    inspectingStudent?.let { stu ->
        StudentDetailReportDialog(
            student = stu,
            viewModel = viewModel,
            initialMonthFilter = selectedReportMonth,
            onRemove = { studentToRemove = stu },
            onDismiss = { inspectingStudent = null }
        )
    }

    employeeToRemove?.let { emp ->
        AlertDialog(
            onDismissRequest = { employeeToRemove = null },
            icon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ErrorRed) },
            title = { Text("Remove Teacher / Staff?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove ${emp.name} (${emp.department}, ID: ${emp.employeeId}) from London Academy?\n\nTheir active staff profile and portal login will be removed and synced to Firebase.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeEmployee(emp) {
                            if (inspectingEmployee?.employeeId == emp.employeeId) {
                                inspectingEmployee = null
                            }
                            employeeToRemove = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_remove_employee_button")
                ) {
                    Text("Remove Staff", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { employeeToRemove = null },
                    modifier = Modifier.testTag("cancel_remove_employee_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    studentToRemove?.let { stu ->
        AlertDialog(
            onDismissRequest = { studentToRemove = null },
            icon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ErrorRed) },
            title = { Text("Remove Student?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove ${stu.name} (Adm #${stu.admissionNo}, ${stu.courseName}) who left the institute?\n\nTheir student profile, pending fee installments, and portal login will be removed and synced to Firebase.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeStudent(stu) {
                            if (inspectingStudent?.admissionNo == stu.admissionNo) {
                                inspectingStudent = null
                            }
                            studentToRemove = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_remove_student_button")
                ) {
                    Text("Remove Student", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { studentToRemove = null },
                    modifier = Modifier.testTag("cancel_remove_student_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmployeeAdminCard(
    employee: EmployeeEntity,
    workingDays: Int,
    presentDays: Int,
    absentDays: Int,
    halfDays: Int,
    percentage: Double,
    fullHoursDays: Int = 0,
    totalWorkedLabel: String = "0m",
    leaveDays: Int = 0,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SecondaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            employee.name.take(1),
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${employee.department} • ${employee.branchName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${percentage.toInt()}% Att.",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DOJ and working days + hours/leaves breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Joining Date (DOJ):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(employee.dateOfJoining, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Attended: $presentDays / $workingDays d", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        Text("Leaves: $leaveDays | Abs: $absentDays", fontSize = 11.sp, color = ErrorRed)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Full Hours Met: $fullHoursDays d", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue)
                        Text("Total Worked: $totalWorkedLabel", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SecondaryTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ID: ${employee.employeeId} • ${employee.contactNo}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onRemove != null) {
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("remove_employee_${employee.employeeId}"),
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Remove ${employee.name}", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (onClick != null) {
                        Text("Report →", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAdminCard(
    student: StudentEntity,
    daysAttended: Int = 0,
    fullHoursDays: Int = 0,
    leaveDays: Int = 0,
    paymentCount: Int = 0,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = buildString {
                            append("${student.courseName} • ${student.batchName}")
                            if (student.batchTiming.isNotBlank()) append(" (${student.batchTiming})")
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Branch: ${student.branchName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryTeal
                    )
                }
                StatusPill(student.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Adm #: ${student.admissionNo}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                Text("Contact: ${student.contactNo}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration: ${student.courseDuration} • DOJ: ${student.dateOfJoining}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatPayingMethodLabel(student.paymentPlan, student.installmentCount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SecondaryTeal)
            }
            if (student.fatherName.isNotBlank() || student.motherName.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "Father: ${student.fatherName.ifBlank { "—" }} • Mother: ${student.motherName.ifBlank { "—" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onClick != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Attended: $daysAttended d • Full Hrs: $fullHoursDays d", fontSize = 11.sp, color = Navy700)
                    Text("Leaves: $leaveDays d", fontSize = 11.sp, color = SecondaryTeal)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fees pill summary
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total: ₹${student.totalFees.toInt()}", fontSize = 11.sp)
                    Text("Paid: ₹${student.paidFees.toInt()} (${paymentCount}x)", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                    Text(
                        "Due: ₹${student.pendingFees.toInt()}",
                        fontSize = 11.sp,
                        color = if (student.pendingFees > 0) ErrorRed else SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onClick != null || onRemove != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRemove != null) {
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("remove_student_${student.admissionNo}"),
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Remove ${student.name}", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    if (onClick != null) {
                        Text("View Report & Payments →", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeeReceiptCard(payment: FeePaymentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(payment.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${payment.admissionNo} • ${payment.paymentDate} • ${payment.paymentMode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Receipt: ${payment.receiptNo}", fontSize = 10.sp, color = PrimaryBlue)
            }
            Text(
                "₹${payment.amount.toInt()}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SuccessGreen
            )
        }
    }
}

@Composable
fun AdminSettingsTab(
    viewModel: EduViewModel,
    onAddHoliday: () -> Unit
) {
    val officeSettings by viewModel.officeSettings.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var officeName by remember(officeSettings) { mutableStateOf(officeSettings?.officeName ?: "Main Campus") }
    var latText by remember(officeSettings) { mutableStateOf(officeSettings?.latitude?.toString() ?: "28.6139") }
    var lonText by remember(officeSettings) { mutableStateOf(officeSettings?.longitude?.toString() ?: "77.2090") }
    var radiusText by remember(officeSettings) { mutableStateOf(officeSettings?.allowedRadiusMeters?.toString() ?: "10.0") }
    var weeklyOff by remember(officeSettings) { mutableStateOf(officeSettings?.weeklyOff ?: "SUNDAY") }
    var empHoursText by remember(officeSettings) { mutableStateOf(officeSettings?.standardDailyHours?.toString() ?: "8.0") }
    var stuHoursText by remember(officeSettings) { mutableStateOf(officeSettings?.standardStudentDailyHours?.toString() ?: "2.0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            title = "Office GPS Location & Working Hours",
            subtitle = "Configures geofence perimeter & standard daily shift/study hours"
        )

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = officeName,
                    onValueChange = { officeName = it },
                    label = { Text("Office / Campus Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it },
                    label = { Text("Allowed Radius (meters) - Default: 10m") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = empHoursText,
                        onValueChange = { empHoursText = it },
                        label = { Text("Staff Shift (hrs/day)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stuHoursText,
                        onValueChange = { stuHoursText = it },
                        label = { Text("Student Class (hrs/day)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Quick test helper: Set Office GPS to Current Location
                Button(
                    onClick = { viewModel.setOfficeToCurrentLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use My Current GPS as Office Location")
                }

                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 28.6139
                        val lon = lonText.toDoubleOrNull() ?: 77.2090
                        val rad = radiusText.toDoubleOrNull() ?: 10.0
                        val empHrs = empHoursText.toDoubleOrNull() ?: 8.0
                        val stuHrs = stuHoursText.toDoubleOrNull() ?: 2.0
                        viewModel.updateOfficeSettings(officeName, lat, lon, rad, weeklyOff, empHrs, stuHrs)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Geofence & Hours Settings")
                }
            }
        }

        SectionHeader(title = "Weekly Off Configuration")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = weeklyOff == "SUNDAY",
                onClick = {
                    weeklyOff = "SUNDAY"
                    val lat = latText.toDoubleOrNull() ?: 28.6139
                    val lon = lonText.toDoubleOrNull() ?: 77.2090
                    val rad = radiusText.toDoubleOrNull() ?: 10.0
                    viewModel.updateOfficeSettings(officeName, lat, lon, rad, "SUNDAY")
                },
                label = { Text("Sunday Only") }
            )
            FilterChip(
                selected = weeklyOff == "SAT_SUN",
                onClick = {
                    weeklyOff = "SAT_SUN"
                    val lat = latText.toDoubleOrNull() ?: 28.6139
                    val lon = lonText.toDoubleOrNull() ?: 77.2090
                    val rad = radiusText.toDoubleOrNull() ?: 10.0
                    viewModel.updateOfficeSettings(officeName, lat, lon, rad, "SAT_SUN")
                },
                label = { Text("Saturday & Sunday") }
            )
        }

        SectionHeader(
            title = "Holiday Calendar (${holidays.size})",
            actionText = "+ Add Holiday",
            onAction = onAddHoliday
        )

        holidays.forEach { h ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(h.holidayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${h.date} • ${h.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.deleteHoliday(h.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Dialogs for Admin
@Composable
fun AddEmployeeDialog(viewModel: EduViewModel, onDismiss: () -> Unit) {
    val categories by viewModel.allCategories.collectAsState()
    val departments = categories.map { it.categoryName }.ifEmpty {
        listOf("Teaching Staff", "Web Development Trainer", "Digital Marketing Trainer", "Office Boy", "Cleaning Staff", "Other Staff")
    }
    val dbBranches by viewModel.allBranches.collectAsState()
    val branchOptions = remember(dbBranches) {
        val base = if (dbBranches.isNotEmpty()) dbBranches.map { it.branchName } else listOf("Main Campus (Delhi)", "South Extension Branch", "Noida Branch", "Gurugram Branch")
        base + "Other / Custom Branch"
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())

    var empId by remember { mutableStateOf("EMP-${(100..999).random()}") }
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf(departments[0]) }
    var selectedBranchOption by remember { mutableStateOf(branchOptions.first()) }
    var customBranchName by remember { mutableStateOf("") }
    var showManageBranches by remember { mutableStateOf(false) }
    var doj by remember { mutableStateOf(today) }
    var initialPass by remember { mutableStateOf("emp123") }
    var canManageStudents by remember { mutableStateOf(true) }
    val canViewFees = false

    LaunchedEffect(branchOptions) {
        if (selectedBranchOption != "Other / Custom Branch" && selectedBranchOption !in branchOptions && branchOptions.isNotEmpty()) {
            selectedBranchOption = branchOptions.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add New Employee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = empId, onValueChange = { empId = it }, label = { Text("Employee ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Number *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())

                Text("Department / Category:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                departments.take(4).forEach { dept ->
                    FilterChip(
                        selected = selectedDept == dept,
                        onClick = {
                            selectedDept = dept
                            val isSupport = dept.contains("Office", ignoreCase = true) || dept.contains("Cleaning", ignoreCase = true)
                            canManageStudents = !isSupport
                        },
                        label = { Text(dept, fontSize = 11.sp) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Branch Name *:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = { showManageBranches = true },
                        modifier = Modifier.testTag("emp_dialog_add_branch_button")
                    ) {
                        Text("+ Add Branch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                branchOptions.forEach { branch ->
                    FilterChip(
                        selected = selectedBranchOption == branch,
                        onClick = { selectedBranchOption = branch },
                        label = { Text(branch, fontSize = 11.sp) }
                    )
                }
                if (selectedBranchOption == "Other / Custom Branch") {
                    OutlinedTextField(
                        value = customBranchName,
                        onValueChange = { customBranchName = it },
                        label = { Text("Enter Custom Branch Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(value = doj, onValueChange = { doj = it }, label = { Text("Date of Joining (DOJ)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = initialPass, onValueChange = { initialPass = it }, label = { Text("Initial Login Password") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canManageStudents, onCheckedChange = { canManageStudents = it })
                    Text("Can Mark Batch Attendance", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && contact.isNotBlank()) {
                            val effectiveBranch = if (selectedBranchOption == "Other / Custom Branch") {
                                customBranchName.trim().ifBlank { "Main Campus (Delhi)" }
                            } else {
                                selectedBranchOption
                            }
                            if (selectedBranchOption == "Other / Custom Branch" && customBranchName.isNotBlank()) {
                                viewModel.addBranch(effectiveBranch)
                            }
                            viewModel.addEmployee(
                                employeeId = empId,
                                name = name,
                                contact = contact,
                                email = email,
                                department = selectedDept,
                                branchName = effectiveBranch,
                                doj = doj,
                                initialPass = initialPass,
                                canManageStudents = canManageStudents,
                                canViewFees = canViewFees
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Employee Account")
                }
            }
        }
    }

    if (showManageBranches) {
        ManageBranchesDialog(
            viewModel = viewModel,
            onBranchAdded = { addedBranch ->
                selectedBranchOption = addedBranch
                showManageBranches = false
            },
            onDismiss = { showManageBranches = false }
        )
    }
}

@Composable
fun SendNotificationDialog(viewModel: EduViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val categories = listOf("Offer", "Discount", "Holiday", "Fee Reminder", "Office Update", "Announcement", "Event")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    val targets = listOf("ALL_STUDENTS", "ALL_EMPLOYEES", "SUB_ADMIN", "INDIVIDUAL")
    var selectedTarget by remember { mutableStateOf(targets[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dispatch Targeted Notification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notice Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Notice Content / Message *") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                Text("Category:", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { c ->
                        FilterChip(
                            selected = selectedCategory == c,
                            onClick = { selectedCategory = c },
                            label = { Text(c, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Audience Target:", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    targets.forEach { t ->
                        FilterChip(
                            selected = selectedTarget == t,
                            onClick = { selectedTarget = t },
                            label = { Text(t.replace("_", " "), fontSize = 9.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (title.isNotBlank() && message.isNotBlank()) {
                            viewModel.sendNotification(
                                title = title,
                                message = message,
                                category = selectedCategory,
                                targetType = selectedTarget
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Broadcast Notification")
                }
            }
        }
    }
}

@Composable
fun AddHolidayDialog(viewModel: EduViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-") }
    var desc by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Institutional Holiday", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Holiday Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (name.isNotBlank() && date.isNotBlank()) {
                            viewModel.addHoliday(name, date, desc)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Holiday")
                }
            }
        }
    }
}
