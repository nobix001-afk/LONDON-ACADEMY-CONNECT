package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.R
import com.example.data.model.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAdminDashboardScreen(viewModel: EduViewModel) {
    var currentSubTab by remember { mutableIntStateOf(0) }
    var showStudentRegistration by remember { mutableStateOf(false) }
    var showManageCourses by remember { mutableStateOf(false) }
    var showManageBranches by remember { mutableStateOf(false) }
    var showCollectFee by remember { mutableStateOf(false) }
    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showBatchStudentAttendance by remember { mutableStateOf(false) }
    var preselectedStudentForFee by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForDetails by remember { mutableStateOf<StudentEntity?>(null) }

    val currentUser by viewModel.currentUser.collectAsState()
    val employees by viewModel.allEmployees.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val payments by viewModel.allPayments.collectAsState()
    val installments by viewModel.allInstallments.collectAsState()
    val notifications by viewModel.allNotifications.collectAsState()
    val allEmployeeAttendance by viewModel.allEmployeeAttendance.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()
    val allHolidays by viewModel.allHolidays.collectAsState()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val subAdminId = currentUser?.userId ?: "subadmin1"
    val myTodayAttendance = remember(allEmployeeAttendance, subAdminId, todayStr) {
        allEmployeeAttendance.firstOrNull { it.employeeId == subAdminId && it.date == todayStr }
    }
    val myAttendanceHistory = remember(allEmployeeAttendance, subAdminId) {
        allEmployeeAttendance.filter { it.employeeId == subAdminId }
    }
    val holidayReason = remember(officeSettings, allHolidays) {
        viewModel.getTodayHolidayReason()
    }

    val tabTitles = listOf("Overview", "Students", "Staff Attendance", "Fees & Pending Fees")

    BackHandler(enabled = currentSubTab != 0) {
        currentSubTab = 0
    }

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
                            Text("Sub-Admin Student & Ops Portal", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("subadmin_logout_button")
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
        floatingActionButton = {
            if (currentSubTab == 0 || currentSubTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showStudentRegistration = true },
                    containerColor = RoyalDarkRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Student") },
                    text = { Text("Add Student", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("subadmin_add_student_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = currentSubTab,
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = currentSubTab == index,
                        onClick = { currentSubTab = index },
                        modifier = Modifier.testTag("subadmin_tab_$index"),
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (currentSubTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (currentSubTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DailyGpsPhotoAttendanceCard(
                                roleLabel = "Sub-Admin (${currentUser?.name ?: "Sub-Admin"})",
                                todayStatus = myTodayAttendance?.status,
                                checkInTime = myTodayAttendance?.time ?: "",
                                checkOutTime = myTodayAttendance?.checkOutTime ?: "",
                                distanceMeters = myTodayAttendance?.distanceMeters ?: 0.0,
                                latitude = myTodayAttendance?.latitude ?: 0.0,
                                longitude = myTodayAttendance?.longitude ?: 0.0,
                                selfieUri = myTodayAttendance?.selfieUri ?: "",
                                workedMinutes = myTodayAttendance?.workedMinutes ?: 0,
                                allowedRadiusMeters = officeSettings?.allowedRadiusMeters ?: 10.0,
                                holidayReason = holidayReason,
                                markButtonTestTag = "button_mark_subadmin_attendance",
                                clockOutButtonTestTag = "button_clock_out_subadmin",
                                formatMinutes = { viewModel.formatMinutes(it) },
                                onMarkAttendance = { showAttendanceDialog = true },
                                onClockOut = { viewModel.clockOutToday() }
                            )
                        }
                        item {
                            SectionHeader(title = "Sub-Admin Operations")
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Total Students",
                                    value = "${students.size}",
                                    subtitle = "Tap for full details",
                                    icon = Icons.Default.School,
                                    accentColor = PrimaryBlue,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentSubTab = 1 }
                                )
                                StatCard(
                                    title = "Teachers",
                                    value = "${employees.count { it.canManageStudents }}",
                                    subtitle = "Teaching staff",
                                    icon = Icons.Default.Badge,
                                    accentColor = SecondaryTeal,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentSubTab = 2 }
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Fee Collections",
                                    value = "₹${(students.sumOf { it.paidFees } / 1000).toInt()}k",
                                    subtitle = "Recorded to date",
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = SuccessGreen,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentSubTab = 3 }
                                )
                                StatCard(
                                    title = "Pending Fees",
                                    value = "₹${(students.sumOf { it.pendingFees } / 1000).toInt()}k",
                                    subtitle = "Outstanding dues",
                                    icon = Icons.Default.Pending,
                                    accentColor = TertiaryAmber,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentSubTab = 3 }
                                )
                            }
                        }
                        item {
                            SectionHeader(title = "Quick Sub-Admin Actions")
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showStudentRegistration = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("subadmin_quick_add_student_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Student Form", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            preselectedStudentForFee = null
                                            showCollectFee = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("subadmin_quick_collect_fee_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Collect Fee", fontSize = 12.sp)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { showManageCourses = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("subadmin_quick_add_course_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Courses", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = { showManageBranches = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("subadmin_quick_add_branch_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Branches", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = { showBatchStudentAttendance = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("subadmin_quick_batch_attendance_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalDarkRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Batch-Wise Student Attendance", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        item {
                            SectionHeader(
                                title = "Recent Student Admissions",
                                subtitle = "Name, Adm #, Course, Parents & Paying Method",
                                actionText = "View All (${students.size})",
                                onAction = { currentSubTab = 1 }
                            )
                        }
                        items(students.take(3), key = { it.admissionNo }) { student ->
                            val paymentCount = payments.count { it.admissionNo == student.admissionNo }
                            SubAdminStudentCard(
                                student = student,
                                paymentCount = paymentCount,
                                onViewDetails = { selectedStudentForDetails = student },
                                onCollectFee = {
                                    preselectedStudentForFee = student
                                    showCollectFee = true
                                }
                            )
                        }
                        item {
                            SectionHeader(title = "Active Notifications")
                        }
                        items(notifications.take(3)) { n ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(n.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(n.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    var search by remember { mutableStateOf("") }
                    var selectedPlanFilter by remember { mutableStateOf("ALL") }
                    var selectedBatchFilter by remember { mutableStateOf("ALL") }
                    val planFilters = listOf("ALL", "ONE-TIME", "MONTHLY", "PART PAYMENT", "YEARLY")
                    val batchFilters = remember(students) {
                        listOf("ALL") + students.map { it.batchName.ifBlank { "General Batch" } }.distinct().sorted()
                    }

                    val filtered = students.filter { stu ->
                        val matchesQuery = search.isBlank() ||
                            stu.name.contains(search, ignoreCase = true) ||
                            stu.admissionNo.contains(search, ignoreCase = true) ||
                            stu.courseName.contains(search, ignoreCase = true) ||
                            stu.fatherName.contains(search, ignoreCase = true) ||
                            stu.motherName.contains(search, ignoreCase = true) ||
                            stu.contactNo.contains(search, ignoreCase = true) ||
                            stu.batchTiming.contains(search, ignoreCase = true) ||
                            stu.batchName.contains(search, ignoreCase = true) ||
                            stu.branchName.contains(search, ignoreCase = true)
                        val matchesPlan = selectedPlanFilter == "ALL" ||
                            stu.paymentPlan.equals(selectedPlanFilter, ignoreCase = true)
                        val matchesBatch = selectedBatchFilter == "ALL" ||
                            stu.batchName.ifBlank { "General Batch" } == selectedBatchFilter
                        matchesQuery && matchesPlan && matchesBatch
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Student Details Directory (${filtered.size})",
                                subtitle = "Tap any student to view full profile or edit",
                                actionText = "+ Add Student",
                                onAction = { showStudentRegistration = true }
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showBatchStudentAttendance = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("subadmin_students_batch_attendance_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalDarkRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Batch-Wise Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { showManageCourses = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("subadmin_students_add_course_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Course", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = { showManageBranches = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("subadmin_students_add_branch_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Branch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        if (batchFilters.size > 1) {
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(batchFilters) { batch ->
                                        FilterChip(
                                            selected = selectedBatchFilter == batch,
                                            onClick = { selectedBatchFilter = batch },
                                            label = { Text(if (batch == "ALL") "All Batches" else batch, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                label = { Text("Search name, adm no, course, parents, phone") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("subadmin_student_search"),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (search.isNotEmpty()) {
                                        IconButton(onClick = { search = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(planFilters) { plan ->
                                    FilterChip(
                                        selected = selectedPlanFilter == plan,
                                        onClick = { selectedPlanFilter = plan },
                                        label = { Text(plan, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text("No matching students found", fontWeight = FontWeight.SemiBold)
                                        Button(onClick = { showStudentRegistration = true }) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add New Student")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filtered, key = { it.admissionNo }) { student ->
                                val paymentCount = payments.count { it.admissionNo == student.admissionNo }
                                SubAdminStudentCard(
                                    student = student,
                                    paymentCount = paymentCount,
                                    onViewDetails = { selectedStudentForDetails = student },
                                    onCollectFee = {
                                        preselectedStudentForFee = student
                                        showCollectFee = true
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            DailyGpsPhotoAttendanceCard(
                                roleLabel = "Sub-Admin (${currentUser?.name ?: "Sub-Admin"})",
                                todayStatus = myTodayAttendance?.status,
                                checkInTime = myTodayAttendance?.time ?: "",
                                checkOutTime = myTodayAttendance?.checkOutTime ?: "",
                                distanceMeters = myTodayAttendance?.distanceMeters ?: 0.0,
                                latitude = myTodayAttendance?.latitude ?: 0.0,
                                longitude = myTodayAttendance?.longitude ?: 0.0,
                                selfieUri = myTodayAttendance?.selfieUri ?: "",
                                workedMinutes = myTodayAttendance?.workedMinutes ?: 0,
                                allowedRadiusMeters = officeSettings?.allowedRadiusMeters ?: 10.0,
                                holidayReason = holidayReason,
                                markButtonTestTag = "button_mark_subadmin_attendance_tab",
                                clockOutButtonTestTag = "button_clock_out_subadmin_tab",
                                formatMinutes = { viewModel.formatMinutes(it) },
                                onMarkAttendance = { showAttendanceDialog = true },
                                onClockOut = { viewModel.clockOutToday() }
                            )
                        }
                        if (myAttendanceHistory.isNotEmpty()) {
                            item {
                                SectionHeader(title = "My Recent GPS & Photo Logs")
                            }
                            items(myAttendanceHistory.take(5)) { rec ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (rec.selfieUri.isNotBlank()) {
                                            AttendancePhotoThumbnail(
                                                selfieUri = rec.selfieUri,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(rec.date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = "Login: ${rec.time.take(5)}${if (rec.checkOutTime.isNotEmpty()) " • Log Out: ${rec.checkOutTime.take(5)}" else ""} • GPS ${rec.distanceMeters.toInt()}m",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusPill(rec.status)
                                    }
                                }
                            }
                        }
                        item {
                            SectionHeader(
                                title = "Staff Attendance by Joining Date",
                                subtitle = "Accurate working days calculated from individual Date of Joining (DOJ)"
                            )
                        }
                        items(employees) { emp ->
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
                                leaveDays = stats["leaveDays"] as? Int ?: 0
                            )
                        }
                    }
                }
                3 -> {
                    PendingFeesScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showStudentRegistration) {
        StudentRegistrationDialog(
            viewModel = viewModel,
            onDismiss = { showStudentRegistration = false }
        )
    }

    if (showManageCourses) {
        ManageCoursesDialog(
            viewModel = viewModel,
            onDismiss = { showManageCourses = false }
        )
    }

    if (showManageBranches) {
        ManageBranchesDialog(
            viewModel = viewModel,
            onDismiss = { showManageBranches = false }
        )
    }

    if (showCollectFee) {
        CollectFeeDialog(
            viewModel = viewModel,
            preselectedStudent = preselectedStudentForFee,
            onDismiss = {
                showCollectFee = false
                preselectedStudentForFee = null
            }
        )
    }

    if (showAttendanceDialog) {
        AttendanceMarkingDialog(
            viewModel = viewModel,
            onDismiss = { showAttendanceDialog = false }
        )
    }

    if (showBatchStudentAttendance) {
        BatchStudentAttendanceDialog(
            viewModel = viewModel,
            showScopeToggle = false,
            onDismiss = { showBatchStudentAttendance = false }
        )
    }

    selectedStudentForDetails?.let { stu ->
        val freshStudent = students.firstOrNull { it.admissionNo == stu.admissionNo } ?: stu
        val stuInstallments = installments.filter { it.admissionNo == freshStudent.admissionNo }
            .sortedBy { it.installmentNumber }
        val stuPayments = payments.filter { it.admissionNo == freshStudent.admissionNo }
        SubAdminStudentDetailsDialog(
            student = freshStudent,
            installments = stuInstallments,
            payments = stuPayments,
            viewModel = viewModel,
            onCollectFee = { target ->
                selectedStudentForDetails = null
                preselectedStudentForFee = target
                showCollectFee = true
            },
            onDismiss = { selectedStudentForDetails = null }
        )
    }
}
