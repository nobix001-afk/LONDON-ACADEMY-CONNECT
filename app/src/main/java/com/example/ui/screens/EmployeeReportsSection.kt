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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExcelExporter
import com.example.data.model.EmployeeAttendanceEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeWorkReport
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun EmployeeWorkReportsSection(
    viewModel: EduViewModel,
    selectedMonth: String,
    onMonthChange: (String) -> Unit,
    onInspectEmployee: (EmployeeEntity) -> Unit
) {
    val context = LocalContext.current
    val employees by viewModel.allEmployees.collectAsState()
    val allAttendance by viewModel.allEmployeeAttendance.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()

    val availableMonths = remember(allAttendance) { viewModel.getAvailableReportMonths() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredEmployees = remember(employees, searchQuery) {
        employees.filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.department.contains(searchQuery, ignoreCase = true) ||
                it.employeeId.contains(searchQuery, ignoreCase = true) ||
                it.branchName.contains(searchQuery, ignoreCase = true)
        }
    }

    val reports = remember(filteredEmployees, selectedMonth, allAttendance, holidays, officeSettings) {
        filteredEmployees.map { emp -> viewModel.getEmployeeWorkReport(emp, selectedMonth) }
    }

    val totalAttended = reports.sumOf { it.daysAttended }
    val totalFullHoursDays = reports.sumOf { it.fullHoursCompletedDays }
    val totalLeaves = reports.sumOf { it.leaveDays }
    val totalAbsent = reports.sumOf { it.absentDays }
    val stdDailyHrs = officeSettings?.standardDailyHours ?: 8.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Employee Complete Work & Attendance Report",
            subtitle = "Standard Shift: ${stdDailyHrs}h/day • Attendance, Full Working Hours & Leaves"
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

        // Search Staff
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search employee by name, role, or ID") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_employee_reports")
        )

        // Team Summary Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Team Summary (${reports.size} Staff)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PrimaryBlue
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportMiniStat(
                        label = "Attended",
                        value = "$totalAttended d",
                        sub = "Total days came",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMiniStat(
                        label = "Full Hours",
                        value = "$totalFullHoursDays d",
                        sub = "${stdDailyHrs}h+ completed",
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

        // Export Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val file = viewModel.exportEmployeeSummaryExcel(context, selectedMonth)
                    ExcelExporter.shareExcelFile(context, file, "Share Staff Summary Excel (.xlsx)")
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_staff_summary_excel_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Summary Excel", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    val file = viewModel.exportAttendanceReportExcel(context, monthFilter = selectedMonth)
                    ExcelExporter.shareExcelFile(context, file, "Share Daily Attendance Excel (.xlsx)")
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_staff_daily_excel_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Daily Log Excel", fontSize = 12.sp)
            }
        }

        if (reports.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Badge,
                message = "No matching employees found"
            )
        } else {
            reports.forEach { report ->
                EmployeeFullReportCard(
                    report = report,
                    viewModel = viewModel,
                    onClick = { onInspectEmployee(report.employee) }
                )
            }
        }
    }
}

@Composable
fun EmployeeFullReportCard(
    report: EmployeeWorkReport,
    viewModel: EduViewModel,
    onClick: () -> Unit
) {
    val emp = report.employee
    val hoursCompliance = if (report.daysAttended > 0) {
        (report.fullHoursCompletedDays.toFloat() / report.daysAttended.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_report_card_${emp.employeeId}")
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
                            .background(SecondaryTeal.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            emp.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${emp.department} • ID: ${emp.employeeId} • ${emp.branchName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            // 3-metric complete breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReportMiniStat(
                    label = "Days Came",
                    value = "${report.daysAttended}/${report.totalWorkingDays}",
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

            // Shift hours completion progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Full Working Hours Completion (${report.fullHoursCompletedDays}/${report.daysAttended} attended days)",
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
                Text("DOJ: ${emp.dateOfJoining} • Short hrs: ${report.shortHoursDays}d", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "View Day-by-Day Report →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun ReportMiniStat(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.09f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            Text(sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun EmployeeDetailReportDialog(
    employee: EmployeeEntity,
    viewModel: EduViewModel,
    initialMonthFilter: String,
    onRemove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val allAttendance by viewModel.allEmployeeAttendance.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()

    var selectedMonth by remember { mutableStateOf(initialMonthFilter) }
    var editingRecord by remember { mutableStateOf<EmployeeAttendanceEntity?>(null) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    val availableMonths = remember(allAttendance) { viewModel.getAvailableReportMonths() }
    val report = remember(employee, selectedMonth, allAttendance, holidays, officeSettings) {
        viewModel.getEmployeeWorkReport(employee, selectedMonth)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("employee_detail_report_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = PrimaryBlue,
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
                                    Text(employee.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("${employee.department} • ${employee.employeeId}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                                    Text("DOJ: ${employee.dateOfJoining} • Shift: ${viewModel.formatMinutes(report.standardDailyMinutes)}/day", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
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
                    // Month filter
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

                    // 2x2 Summary KPI Grid answering all user questions
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailKpiBox(
                                    title = "Days Attended",
                                    value = "${report.daysAttended} / ${report.totalWorkingDays}",
                                    subtitle = "${report.presentDays} Full • ${report.halfDays} Half (${report.attendancePercentage.toInt()}%)",
                                    icon = Icons.Default.EventAvailable,
                                    accent = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                DetailKpiBox(
                                    title = "Working Hours Pure",
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
                                    val file = viewModel.exportAttendanceReportExcel(
                                        context = context,
                                        monthFilter = selectedMonth,
                                        employeeId = employee.employeeId
                                    )
                                    ExcelExporter.shareExcelFile(context, file, "Share ${employee.name} Excel Report")
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dialog_share_employee_excel_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Excel", fontSize = 12.sp)
                            }
                        }
                        if (onRemove != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onRemove,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_remove_employee_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove Teacher / Staff (Left Institute)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Day-by-Day Attendance & Shift Hours (${report.records.size})",
                            subtitle = "Tap any row to edit login/log out time or leave"
                        )
                    }

                    if (report.records.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.History,
                                message = "No attendance logs in this period"
                            )
                        }
                    } else {
                        items(report.records) { rec ->
                            EmployeeDailyRecordCard(
                                record = rec,
                                viewModel = viewModel,
                                fallbackReqMins = report.standardDailyMinutes,
                                onClick = { editingRecord = rec }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEntryDialog || editingRecord != null) {
        AdminMarkEmployeeAttendanceDialog(
            employee = employee,
            viewModel = viewModel,
            existingRecord = editingRecord,
            onDismiss = {
                showAddEntryDialog = false
                editingRecord = null
            }
        )
    }
}

@Composable
fun DetailKpiBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun EmployeeDailyRecordCard(
    record: EmployeeAttendanceEntity,
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
                        if (isPresent) {
                            val outStr = record.checkOutTime.ifEmpty { "Logged In (No Log Out)" }
                            Text(
                                text = "Login: ${record.time.take(5)}  •  Log Out: ${if (record.checkOutTime.isNotEmpty()) outStr.take(5) else outStr}",
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
                            text = "Worked: ${viewModel.formatMinutes(record.workedMinutes)} / ${viewModel.formatMinutes(reqMins)}",
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
fun AdminMarkEmployeeAttendanceDialog(
    employee: EmployeeEntity,
    viewModel: EduViewModel,
    existingRecord: EmployeeAttendanceEntity? = null,
    onDismiss: () -> Unit
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var date by remember { mutableStateOf(existingRecord?.date ?: today) }
    var status by remember { mutableStateOf(existingRecord?.status ?: "PRESENT") }
    var checkIn by remember { mutableStateOf(existingRecord?.time?.take(5)?.takeIf { it != "00:00" } ?: "09:00") }
    var checkOut by remember { mutableStateOf(existingRecord?.checkOutTime?.take(5) ?: "17:00") }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }

    val settings by viewModel.officeSettings.collectAsState()
    val reqMins = max(60, ((settings?.standardDailyHours ?: 8.0) * 60).toInt())
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
                        Text("Log / Edit Staff Shift", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(employee.name, fontSize = 12.sp, color = PrimaryBlue)
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
                        color = PrimaryBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Worked: ${viewModel.formatMinutes(previewMins)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text(
                                text = if (previewMins >= reqMins) "✓ Full Shift Met" else "Short Shift",
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
                        viewModel.saveAdminEmployeeAttendance(
                            employee = employee,
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
                    Text("Save Attendance Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatDateWithDay(dateStr: String): String {
    return try {
        val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outSdf = SimpleDateFormat("dd MMM yyyy (EEE)", Locale.getDefault())
        val d = inSdf.parse(dateStr)
        if (d != null) outSdf.format(d) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}
