package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.FeePaymentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(viewModel: EduViewModel) {
    var selectedStudentTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Attendance, 2: Fees, 3: Notices
    var showAttendanceDialog by remember { mutableStateOf(false) }

    val currentStudent by viewModel.currentStudent.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val allInstallments by viewModel.allInstallments.collectAsState()
    val allStudentAttendance by viewModel.allStudentAttendance.collectAsState()
    val notifications by viewModel.allNotifications.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()
    val allHolidays by viewModel.allHolidays.collectAsState()
    var viewingReceipt by remember { mutableStateOf<FeePaymentEntity?>(null) }

    val student = currentStudent
    if (student == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No student profile found for this user account.")
        }
        return
    }

    val myPayments = allPayments.filter { it.admissionNo == student.admissionNo }
    val myInstallments = allInstallments.filter { it.admissionNo == student.admissionNo }.sortedBy { it.installmentNumber }
    val myAttendance = allStudentAttendance.filter { it.admissionNo == student.admissionNo }
    val myNotices = notifications.filter {
        it.targetType == "ALL_STUDENTS" ||
        it.targetType == "COURSE" && it.targetValue == student.courseName ||
        it.targetType == "BATCH" && it.targetValue == student.batchName
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayStudentAttendance = remember(myAttendance, todayStr) {
        myAttendance.firstOrNull { it.date == todayStr }
    }
    val holidayReason = remember(officeSettings, allHolidays) {
        viewModel.getTodayHolidayReason()
    }

    val presentDays = myAttendance.count { it.status == "PRESENT" }
    val halfDays = myAttendance.count { it.status == "HALF_DAY" }
    val totalRecords = myAttendance.size
    val attendancePercentage = if (totalRecords > 0) {
        ((presentDays + (halfDays * 0.5)) / totalRecords) * 100.0
    } else {
        100.0
    }

    val tabs = listOf("Overview", "Attendance", "Fees", "Notices")

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
                            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Adm #${student.admissionNo} • London Academy", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("student_logout_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedStudentTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedStudentTab == idx,
                        onClick = { selectedStudentTab = idx },
                        text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedStudentTab == idx) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedStudentTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Profile Banner Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            student.name.take(1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Course: ${student.courseName}", fontSize = 12.sp, color = Navy700)
                                        Text("Batch: ${student.batchName} (${student.courseDuration})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Trainer: ${student.assignedTeacherName}", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        item {
                            DailyGpsPhotoAttendanceCard(
                                roleLabel = "Student (${student.courseName})",
                                todayStatus = todayStudentAttendance?.status,
                                checkInTime = todayStudentAttendance?.checkInTime ?: "",
                                checkOutTime = todayStudentAttendance?.checkOutTime ?: "",
                                distanceMeters = todayStudentAttendance?.distanceMeters ?: 0.0,
                                latitude = todayStudentAttendance?.latitude ?: 0.0,
                                longitude = todayStudentAttendance?.longitude ?: 0.0,
                                selfieUri = todayStudentAttendance?.selfieUri ?: "",
                                workedMinutes = todayStudentAttendance?.workedMinutes ?: 0,
                                allowedRadiusMeters = officeSettings?.allowedRadiusMeters ?: 10.0,
                                holidayReason = holidayReason,
                                markButtonTestTag = "button_mark_student_attendance",
                                clockOutButtonTestTag = "button_clock_out_student",
                                formatMinutes = { viewModel.formatMinutes(it) },
                                onMarkAttendance = { showAttendanceDialog = true },
                                onClockOut = { viewModel.clockOutStudentToday() }
                            )
                        }

                        item {
                            SectionHeader(title = "Academic & Fee Summary")
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Fee Balance",
                                    value = "₹${student.pendingFees.toInt()}",
                                    subtitle = if (student.pendingFees > 0) "Next due: ${student.nextDueDate.ifEmpty { "Pending" }}" else "Fully Paid",
                                    icon = Icons.Default.CurrencyRupee,
                                    accentColor = if (student.pendingFees > 0) TertiaryAmber else SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "Attendance",
                                    value = "${attendancePercentage.toInt()}%",
                                    subtitle = "$presentDays / $totalRecords Sessions",
                                    icon = Icons.Default.FactCheck,
                                    accentColor = if (attendancePercentage >= 75) SuccessGreen else TertiaryAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            SectionHeader(title = "Course & Admission Details")
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Admission Number:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(student.admissionNo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Date of Joining:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(student.dateOfJoining, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Payment Plan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(student.paymentPlan, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Course Fee:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${student.totalFees.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Amount Paid:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${student.paidFees.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Contact Number:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(student.contactNo, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Recent Notices")
                        }
                        if (myNotices.isEmpty()) {
                            item { EmptyStateView(message = "No notices for your batch at this time") }
                        } else {
                            items(myNotices.take(2)) { n ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(n.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(n.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${n.date} • By ${n.sentBy}", fontSize = 10.sp, color = Navy700)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val stuReport = viewModel.getStudentWorkReport(student, "ALL")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            DailyGpsPhotoAttendanceCard(
                                roleLabel = "Student (${student.courseName})",
                                todayStatus = todayStudentAttendance?.status,
                                checkInTime = todayStudentAttendance?.checkInTime ?: "",
                                checkOutTime = todayStudentAttendance?.checkOutTime ?: "",
                                distanceMeters = todayStudentAttendance?.distanceMeters ?: 0.0,
                                latitude = todayStudentAttendance?.latitude ?: 0.0,
                                longitude = todayStudentAttendance?.longitude ?: 0.0,
                                selfieUri = todayStudentAttendance?.selfieUri ?: "",
                                workedMinutes = todayStudentAttendance?.workedMinutes ?: 0,
                                allowedRadiusMeters = officeSettings?.allowedRadiusMeters ?: 10.0,
                                holidayReason = holidayReason,
                                markButtonTestTag = "button_mark_student_attendance_tab",
                                clockOutButtonTestTag = "button_clock_out_student_tab",
                                formatMinutes = { viewModel.formatMinutes(it) },
                                onMarkAttendance = { showAttendanceDialog = true },
                                onClockOut = { viewModel.clockOutStudentToday() }
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Days Attended",
                                    value = "${stuReport.daysAttended} / ${stuReport.totalClassDays}",
                                    subtitle = "${stuReport.fullHoursCompletedDays} Full Hours Met",
                                    icon = Icons.Default.EventAvailable,
                                    accentColor = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "Leaves / Absent",
                                    value = "${stuReport.leaveDays} L / ${stuReport.absentDays} A",
                                    subtitle = "${stuReport.absentDays} Absent Days",
                                    icon = Icons.Default.EventBusy,
                                    accentColor = ErrorRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            SectionHeader(
                                title = "Class Attendance Logs",
                                subtitle = "Self Google GPS + Photo & Instructor-marked study sessions"
                            )
                        }
                        if (myAttendance.isEmpty()) {
                            item { EmptyStateView(message = "No attendance sessions logged yet") }
                        } else {
                            items(myAttendance) { att ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (att.selfieUri.isNotBlank()) {
                                            AttendancePhotoThumbnail(
                                                selfieUri = att.selfieUri,
                                                modifier = Modifier.size(42.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(att.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${att.courseName} • ${att.batchName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (att.checkInTime.isNotBlank()) {
                                                Text(
                                                    text = "Login: ${att.checkInTime.take(5)}${if (att.checkOutTime.isNotBlank()) " • Log Out: ${att.checkOutTime.take(5)}" else ""}${if (att.latitude != 0.0 || att.longitude != 0.0) " • GPS ${att.distanceMeters.toInt()}m" else ""}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (att.workedMinutes > 0) {
                                                Text(
                                                    text = "⏱ Study: ${viewModel.formatMinutes(att.workedMinutes)}",
                                                    fontSize = 11.sp,
                                                    color = PrimaryBlue,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            if (att.selfieUri.isNotBlank()) {
                                                Text("Photo + Google GPS Verified", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                            } else if (att.notes.isNotBlank()) {
                                                Text(att.notes, fontSize = 10.sp, color = Navy700)
                                            }
                                        }
                                        StatusPill(att.status)
                                    }
                                }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "Total Paid",
                                    value = "₹${student.paidFees.toInt()}",
                                    subtitle = "Against ₹${student.totalFees.toInt()}",
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "Pending Due",
                                    value = "₹${student.pendingFees.toInt()}",
                                    subtitle = student.nextDueDate.ifEmpty { "No pending dues" },
                                    icon = Icons.Default.Pending,
                                    accentColor = if (student.pendingFees > 0) ErrorRed else SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (myInstallments.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Installment Schedule & Status (${myInstallments.size})")
                            }
                            items(myInstallments) { inst ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(inst.monthName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Due: ${inst.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Installment: ₹${inst.installmentAmount.toInt()}", fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            StatusPill(inst.status)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                if (inst.remainingAmount > 0) "Left: ₹${inst.remainingAmount.toInt()}" else "Fully Paid",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (inst.remainingAmount > 0) TertiaryAmber else SuccessGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Payment Receipts (${myPayments.size})")
                        }
                        if (myPayments.isEmpty()) {
                            item { EmptyStateView(message = "No payment receipts found") }
                        } else {
                            items(myPayments) { p ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingReceipt = p },
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
                                            Text(p.receiptNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                                            Text("${p.installmentMonth} • ${p.paymentDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Mode: ${p.paymentMode}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${p.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = SuccessGreen)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("View Receipt >", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Student Notices & Announcements",
                                subtitle = "Updates from administration and teaching faculty"
                            )
                        }
                        if (myNotices.isEmpty()) {
                            item { EmptyStateView(message = "No announcements at this time") }
                        } else {
                            items(myNotices) { n ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Surface(
                                                color = PrimaryBlue.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    n.category,
                                                    color = PrimaryBlue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(n.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${n.date} at ${n.time} • Sent by ${n.sentBy}", fontSize = 10.sp, color = Navy700)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAttendanceDialog) {
        AttendanceMarkingDialog(
            viewModel = viewModel,
            onDismiss = { showAttendanceDialog = false }
        )
    }

    viewingReceipt?.let { receipt ->
        FeeReceiptDialog(
            payment = receipt,
            student = student,
            onDismiss = { viewingReceipt = null }
        )
    }
}
