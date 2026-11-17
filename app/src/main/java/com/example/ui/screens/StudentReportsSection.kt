package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExcelExporter
import com.example.data.model.FeePaymentEntity
import com.example.data.model.StudentAttendanceEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWorkReport
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun StudentWorkReportsSection(
    viewModel: EduViewModel,
    selectedMonth: String,
    onMonthChange: (String) -> Unit,
    onInspectStudent: (StudentEntity) -> Unit,
    studentsOverride: List<StudentEntity>? = null,
    hideFees: Boolean = false
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val students = studentsOverride ?: allStudents
    val allStudentAttendance by viewModel.allStudentAttendance.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val allInstallments by viewModel.allInstallments.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()

    val availableMonths = remember(allStudentAttendance, allPayments) { viewModel.getAvailableReportMonths() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBatchFilter by remember { mutableStateOf("ALL") }

    val batchFilters = remember(students) {
        listOf("ALL") + students.map { it.batchName.ifBlank { "General Batch" } }.distinct().sorted()
    }
    LaunchedEffect(batchFilters) {
        if (selectedBatchFilter !in batchFilters) selectedBatchFilter = "ALL"
    }

    val filteredStudents = remember(students, searchQuery, selectedBatchFilter) {
        students.filter { stu ->
            val matchesBatch = selectedBatchFilter == "ALL" ||
                stu.batchName.ifBlank { "General Batch" } == selectedBatchFilter
            val matchesQuery = searchQuery.isBlank() ||
                stu.name.contains(searchQuery, ignoreCase = true) ||
                stu.admissionNo.contains(searchQuery, ignoreCase = true) ||
                stu.courseName.contains(searchQuery, ignoreCase = true) ||
                stu.batchName.contains(searchQuery, ignoreCase = true) ||
                stu.branchName.contains(searchQuery, ignoreCase = true)
            matchesBatch && matchesQuery
        }
    }

    val reports = remember(filteredStudents, selectedMonth, allStudentAttendance, allPayments, allInstallments, holidays, officeSettings) {
        filteredStudents.map { stu -> viewModel.getStudentWorkReport(stu, selectedMonth) }
    }

    val totalAttended = reports.sumOf { it.daysAttended }
    val totalFullHoursDays = reports.sumOf { it.fullHoursCompletedDays }
    val totalLeaves = reports.sumOf { it.leaveDays }
    val totalAbsent = reports.sumOf { it.absentDays }
    val totalPaymentsCount = reports.sumOf { it.feePaymentCount }
    val totalPeriodPaid = reports.sumOf { it.periodFeesPaid }
    val stdDailyHrs = officeSettings?.standardStudentDailyHours ?: 2.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = if (hideFees) "Student Attendance & Study Hours Report" else "Student Complete Attendance, Hours & Fee Report",
            subtitle = if (hideFees) {
                "Daily Class Target: ${stdDailyHrs}h/day • Attendance, Batch, Study Hours & Leaves"
            } else {
                "Daily Class Target: ${stdDailyHrs}h/day • Attendance, Study Hours, Leaves & Payments"
            }
        )

        // Month Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableMonths) { (key, label) ->
                FilterChip(
                    selected = selectedMonth == key,
                    onClick = { onMonthChange(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    leadingIcon = if (selectedMonth == key) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }

        // Batch Filter Row
        if (batchFilters.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(batchFilters) { batch ->
                    FilterChip(
                        selected = selectedBatchFilter == batch,
                        onClick = { selectedBatchFilter = batch },
                        label = { Text(if (batch == "ALL") "All Batches" else batch, fontSize = 11.sp) },
                        modifier = Modifier.testTag("report_batch_filter_${batch.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        // Search Student
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search student by name, admission #, batch, or course") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_student_reports")
        )

        // Overall Student Cohort Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cohort Summary (${reports.size} Students)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SecondaryTeal
                    )
                    if (!hideFees) {
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$totalPaymentsCount Payments (₹${totalPeriodPaid.toInt()})",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportMiniStat(
                        label = "Attended",
                        value = "$totalAttended d",
                        sub = "Days came",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMiniStat(
                        label = "Hours Met",
                        value = "$totalFullHoursDays d",
                        sub = "${stdDailyHrs}h+ study",
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMiniStat(
                        label = "Leaves/Abs",
                        value = "$totalLeaves / $totalAbsent",
                        sub = "Chhuti / Abs",
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Export Student Summary Excel (.xlsx)
        Button(
            onClick = {
                val file = if (hideFees) {
                    viewModel.exportTeacherStudentAttendanceSummaryExcel(context, filteredStudents, selectedMonth)
                } else {
                    viewModel.exportStudentSummaryExcel(context, selectedMonth)
                }
                ExcelExporter.shareExcelFile(
                    context,
                    file,
                    if (hideFees) "Share Student Attendance Excel (.xlsx)" else "Share Student Complete Excel (.xlsx)"
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export_student_summary_excel_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (hideFees) "Export Student Attendance Excel (.xlsx)" else "Export Student Complete Excel (.xlsx)",
                fontSize = 12.sp
            )
        }

        if (reports.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.School,
                message = "No matching students found"
            )
        } else {
            reports.forEach { report ->
                StudentFullReportCard(
                    report = report,
                    viewModel = viewModel,
                    hideFees = hideFees,
                    onClick = { onInspectStudent(report.student) }
                )
            }
        }
    }
}

@Composable
fun StudentFullReportCard(
    report: StudentWorkReport,
    viewModel: EduViewModel,
    hideFees: Boolean = false,
    onClick: () -> Unit
) {
    val stu = report.student
    val hoursCompliance = if (report.daysAttended > 0) {
        (report.fullHoursCompletedDays.toFloat() / report.daysAttended.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_report_card_${stu.admissionNo}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stu.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(stu.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${stu.courseName} • Adm #${stu.admissionNo} • ${stu.branchName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(
                    color = if (report.attendancePercentage >= 75) SuccessGreen.copy(alpha = 0.14f) else TertiaryAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${report.attendancePercentage.toInt()}% Att.",
                        color = if (report.attendancePercentage >= 75) SuccessGreen else TertiaryAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3-metric attendance & study hours row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReportMiniStat(
                    label = "Days Came",
                    value = "${report.daysAttended}/${report.totalClassDays}",
                    sub = "${report.presentDays}P • ${report.halfDays}H",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                ReportMiniStat(
                    label = "Hours Met",
                    value = "${report.fullHoursCompletedDays} d",
                    sub = viewModel.formatMinutes(report.totalWorkedMinutes),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                ReportMiniStat(
                    label = "Leaves",
                    value = "${report.leaveDays} d",
                    sub = "${report.absentDays} Abs",
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!hideFees) {
                // Fee payment days strip ("kitne din pay kiya hai")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paid ${report.feePaymentCount}x (${if (report.monthFilter == "ALL") "All" else "Period"}: ₹${report.periodFeesPaid.toInt()})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Navy800
                            )
                        }
                        Text(
                            text = if (report.pendingFees > 0) "Due: ₹${report.pendingFees.toInt()}" else "✓ Paid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.pendingFees > 0) ErrorRed else SuccessGreen
                        )
                    }
                }
            }

            // Class Hours Completion Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Study Hours Completed (${report.fullHoursCompletedDays}/${report.daysAttended} attended days)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(hoursCompliance * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                LinearProgressIndicator(
                    progress = { hoursCompliance },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = PrimaryBlue.copy(alpha = 0.12f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${stu.batchName} • Short: ${report.shortHoursDays}d", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (hideFees) "View Attendance Report →" else "Full Report & Payments →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun StudentDetailReportDialog(
    student: StudentEntity,
    viewModel: EduViewModel,
    initialMonthFilter: String,
    hideFees: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val allStudentAttendance by viewModel.allStudentAttendance.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val allInstallments by viewModel.allInstallments.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()

    var selectedMonth by remember { mutableStateOf(initialMonthFilter) }
    var detailTab by remember { mutableIntStateOf(0) } // 0 = Attendance & Hours, 1 = Fee Payment Days
    var editingRecord by remember { mutableStateOf<StudentAttendanceEntity?>(null) }
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var viewingReceipt by remember { mutableStateOf<FeePaymentEntity?>(null) }

    val availableMonths = remember(allStudentAttendance, allPayments) { viewModel.getAvailableReportMonths() }
    val report = remember(student, selectedMonth, allStudentAttendance, allPayments, allInstallments, holidays, officeSettings) {
        viewModel.getStudentWorkReport(student, selectedMonth)
    }
    val allTimePaymentsCount = remember(allPayments, student) {
        allPayments.count { it.admissionNo == student.admissionNo }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("student_detail_report_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = SecondaryTeal,
                    contentColor = Color.White
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(student.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("${student.courseName} • Adm #${student.admissionNo}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                                    Text("Batch: ${student.batchName} • Daily Target: ${viewModel.formatMinutes(report.standardDailyMinutes)}/day", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close Report", tint = Color.White)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month Filter
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableMonths) { (key, label) ->
                                FilterChip(
                                    selected = selectedMonth == key,
                                    onClick = { selectedMonth = key },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // 2x2 Attendance & Hours KPI Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailKpiBox(
                                    title = "Days Attended",
                                    value = "${report.daysAttended} / ${report.totalClassDays}",
                                    subtitle = "${report.presentDays} Full • ${report.halfDays} Half (${report.attendancePercentage.toInt()}%)",
                                    icon = Icons.Default.EventAvailable,
                                    accent = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                DetailKpiBox(
                                    title = "Study Hours Pure",
                                    value = "${report.fullHoursCompletedDays} Days",
                                    subtitle = "Total: ${viewModel.formatMinutes(report.totalWorkedMinutes)} • ${report.shortHoursDays} Short",
                                    icon = Icons.Default.Schedule,
                                    accent = PrimaryBlue,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailKpiBox(
                                    title = "Chhuti / Absent",
                                    value = "${report.leaveDays} Leaves",
                                    subtitle = "${report.absentDays} Unplanned / Absent Days",
                                    icon = Icons.Default.EventBusy,
                                    accent = ErrorRed,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (!hideFees) {
                                // Fee Payment Summary KPI Card ("kitne din pay kiya hai")
                                DetailKpiBox(
                                    title = "Kitne Din Fee Pay Kiya (Payment Summary)",
                                    value = "${report.feePaymentCount} Payment(s) in Period • $allTimePaymentsCount Total",
                                    subtitle = "Paid in Period: ₹${report.periodFeesPaid.toInt()} | Total Paid: ₹${report.totalFeesPaid.toInt()} | Pending Due: ₹${report.pendingFees.toInt()} (${report.paidInstallmentsCount}/${report.totalInstallmentsCount} Installments)",
                                    icon = Icons.Default.Payments,
                                    accent = if (report.pendingFees > 0) TertiaryAmber else SuccessGreen,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Actions Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddEntryDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log / Edit Day", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val file = if (hideFees) {
                                        viewModel.exportTeacherStudentAttendanceDetailExcel(context, report)
                                    } else {
                                        viewModel.exportStudentDetailExcel(context, report)
                                    }
                                    ExcelExporter.shareExcelFile(
                                        context,
                                        file,
                                        "Share ${student.name} Excel Report"
                                    )
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dialog_share_student_excel_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Excel", fontSize = 12.sp)
                            }
                        }
                        if (onRemove != null && !hideFees) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onRemove,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_remove_student_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove Student (Left Institute)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Sub-tabs: Daily Attendance & Hours (vs Fee Payment Days if not hidden)
                    if (!hideFees) {
                        item {
                            TabRow(
                                selectedTabIndex = detailTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Tab(
                                    selected = detailTab == 0,
                                    onClick = { detailTab = 0 },
                                    text = { Text("Attendance & Hours (${report.records.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                )
                                Tab(
                                    selected = detailTab == 1,
                                    onClick = { detailTab = 1 },
                                    text = { Text("Fee Paid Days (${report.payments.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                    } else {
                        item {
                            SectionHeader(title = "Daily Attendance & Study Hours (${report.records.size})")
                        }
                    }

                    if (detailTab == 0 || hideFees) {
                        if (report.records.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.History,
                                    message = "No student attendance logs in this period"
                                )
                            }
                        } else {
                            items(report.records) { rec ->
                                StudentDailyRecordCard(
                                    record = rec,
                                    viewModel = viewModel,
                                    fallbackReqMins = report.standardDailyMinutes,
                                    onClick = { editingRecord = rec }
                                )
                            }
                        }
                    } else {
                        if (report.payments.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.ReceiptLong,
                                    message = "No fee payments recorded in this period (Total All-Time: $allTimePaymentsCount)"
                                )
                            }
                        } else {
                            items(report.payments) { pay ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingReceipt = pay },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(formatDateWithDay(pay.paymentDate), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${pay.receiptNo} • ${pay.installmentMonth.ifEmpty { pay.paymentPlan }}", fontSize = 11.sp, color = PrimaryBlue)
                                            Text("Mode: ${pay.paymentMode} • By ${pay.collectedBy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (pay.remarks.isNotBlank()) {
                                                Text(pay.remarks, fontSize = 10.sp, color = Navy700)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${pay.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuccessGreen)
                                            Text("View Receipt >", fontSize = 10.sp, color = PrimaryBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEntryDialog || editingRecord != null) {
        AdminMarkStudentAttendanceDialog(
            student = student,
            viewModel = viewModel,
            existingRecord = editingRecord,
            onDismiss = {
                showAddEntryDialog = false
                editingRecord = null
            }
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

@Composable
private fun StudentDailyRecordCard(
    record: StudentAttendanceEntity,
    viewModel: EduViewModel,
    fallbackReqMins: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val reqMins = if (record.requiredMinutes > 0) record.requiredMinutes else fallbackReqMins
    val isPresent = record.status == "PRESENT" || record.status == "HALF_DAY"
    val fullMet = isPresent && record.workedMinutes >= reqMins
    val isShort = isPresent && record.checkOutTime.isNotBlank() && record.workedMinutes < reqMins
    val shortBy = max(0, reqMins - record.workedMinutes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (record.selfieUri.isNotBlank()) {
                        AttendancePhotoThumbnail(
                            selfieUri = record.selfieUri,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column {
                        Text(formatDateWithDay(record.date), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (isPresent && record.checkInTime.isNotBlank()) {
                            val outStr = record.checkOutTime.ifEmpty { "Logged In" }
                            Text(
                                text = "Login: ${record.checkInTime.take(5)}  •  Log Out: ${if (record.checkOutTime.isNotEmpty()) outStr.take(5) else outStr}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (record.latitude != 0.0 || record.longitude != 0.0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val uri = Uri.parse(
                                        "https://maps.google.com/?q=${record.latitude},${record.longitude}"
                                    )
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                    runCatching { context.startActivity(mapIntent) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Google GPS",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "GPS ${record.distanceMeters.toInt()}m • View Google Map",
                                    fontSize = 10.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                StatusPill(record.status)
            }

            if (isPresent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Study: ${viewModel.formatMinutes(record.workedMinutes)} / ${viewModel.formatMinutes(reqMins)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (fullMet) {
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "✓ Hours Complete",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    } else if (isShort) {
                        Surface(
                            color = TertiaryAmber.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⏳ Short ${viewModel.formatMinutes(shortBy)}",
                                color = TertiaryAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (record.notes.isNotBlank()) {
                Text(
                    text = "Note: ${record.notes}",
                    fontSize = 11.sp,
                    color = Navy700
                )
            }
        }
    }
}

@Composable
fun AdminMarkStudentAttendanceDialog(
    student: StudentEntity,
    viewModel: EduViewModel,
    existingRecord: StudentAttendanceEntity? = null,
    onDismiss: () -> Unit
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var date by remember { mutableStateOf(existingRecord?.date ?: today) }
    var status by remember { mutableStateOf(existingRecord?.status ?: "PRESENT") }
    var checkIn by remember { mutableStateOf(existingRecord?.checkInTime?.take(5)?.ifBlank { "10:00" } ?: "10:00") }
    var checkOut by remember { mutableStateOf(existingRecord?.checkOutTime?.take(5)?.ifBlank { "12:00" } ?: "12:00") }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }

    val settings by viewModel.officeSettings.collectAsState()
    val reqMins = max(30, ((settings?.standardStudentDailyHours ?: 2.0) * 60).toInt())
    val previewMins = if (status == "PRESENT" || status == "HALF_DAY") {
        viewModel.computeDurationMinutes(checkIn, checkOut)
    } else 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Log / Edit Student Session", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${student.name} (${student.admissionNo})", fontSize = 12.sp, color = SecondaryTeal)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("PRESENT", "HALF_DAY", "LEAVE", "ABSENT").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.replace("_", " "), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (status == "PRESENT" || status == "HALF_DAY") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = checkIn,
                            onValueChange = { checkIn = it },
                            label = { Text("Login Time (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = checkOut,
                            onValueChange = { checkOut = it },
                            label = { Text("Log Out Time (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        color = SecondaryTeal.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Study: ${viewModel.formatMinutes(previewMins)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text(
                                text = if (previewMins >= reqMins) "✓ Hours Complete" else "Short Hours",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (previewMins >= reqMins) SuccessGreen else TertiaryAmber
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Leave Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Button(
                    onClick = {
                        viewModel.saveAdminStudentAttendance(
                            student = student,
                            date = date.trim(),
                            status = status,
                            checkInTime = checkIn,
                            checkOutTime = checkOut,
                            notes = notes,
                            onComplete = onDismiss
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Student Attendance", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
