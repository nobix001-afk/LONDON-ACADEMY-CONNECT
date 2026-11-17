package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StudentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchStudentAttendanceDialog(
    viewModel: EduViewModel,
    assignedStudents: List<StudentEntity> = emptyList(),
    showScopeToggle: Boolean = false,
    onDismiss: () -> Unit
) {
    val allStudents by viewModel.allStudents.collectAsState()
    val allStudentAttendance by viewModel.allStudentAttendance.collectAsState()
    val officeSettings by viewModel.officeSettings.collectAsState()
    val allHolidays by viewModel.allHolidays.collectAsState()

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedDate by remember { mutableStateOf(today) }
    var onlyAssigned by remember { mutableStateOf(showScopeToggle && assignedStudents.isNotEmpty()) }
    var selectedBatch by remember { mutableStateOf("ALL") }
    var selectedTiming by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val holidayReason = remember(selectedDate, officeSettings, allHolidays) {
        viewModel.getHolidayReasonForDateString(selectedDate)
    }

    val scopedStudents = remember(allStudents, assignedStudents, onlyAssigned) {
        if (onlyAssigned) assignedStudents else allStudents
    }

    val batchOptions = remember(scopedStudents) {
        listOf("ALL") + scopedStudents.map { it.batchName.ifBlank { "General Batch" } }.distinct().sorted()
    }
    val timingOptions = remember(scopedStudents, selectedBatch) {
        val source = if (selectedBatch == "ALL") {
            scopedStudents
        } else {
            scopedStudents.filter { it.batchName.ifBlank { "General Batch" } == selectedBatch }
        }
        val timings = source.map { it.batchTiming.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        if (timings.isNotEmpty()) listOf("ALL") + timings else emptyList()
    }

    LaunchedEffect(batchOptions) {
        if (selectedBatch !in batchOptions) selectedBatch = "ALL"
    }
    LaunchedEffect(timingOptions) {
        if (selectedTiming != "ALL" && selectedTiming !in timingOptions) selectedTiming = "ALL"
    }

    val filteredStudents = remember(scopedStudents, selectedBatch, selectedTiming, searchQuery) {
        scopedStudents.filter { stu ->
            val stuBatch = stu.batchName.ifBlank { "General Batch" }
            val matchesBatch = selectedBatch == "ALL" || stuBatch == selectedBatch
            val matchesTiming = selectedTiming == "ALL" || stu.batchTiming.trim() == selectedTiming
            val matchesQuery = searchQuery.isBlank() ||
                stu.name.contains(searchQuery, ignoreCase = true) ||
                stu.admissionNo.contains(searchQuery, ignoreCase = true) ||
                stu.courseName.contains(searchQuery, ignoreCase = true)
            matchesBatch && matchesTiming && matchesQuery
        }
    }

    val existingByAdm = remember(allStudentAttendance, selectedDate) {
        allStudentAttendance
            .filter { it.date == selectedDate.trim() }
            .associateBy { it.admissionNo }
    }

    val statuses = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(selectedDate, allStudents, existingByAdm) {
        allStudents.forEach { stu ->
            statuses[stu.admissionNo] = existingByAdm[stu.admissionNo]?.status ?: "PRESENT"
        }
    }

    val presentCount = filteredStudents.count { (statuses[it.admissionNo] ?: "PRESENT") == "PRESENT" }
    val absentCount = filteredStudents.count { (statuses[it.admissionNo] ?: "PRESENT") == "ABSENT" }
    val activeBatchLabel = buildString {
        append(if (selectedBatch == "ALL") "All Batches" else selectedBatch)
        if (selectedTiming != "ALL") append(" • $selectedTiming")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batch-Wise Student Attendance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Select batch & timing to mark daily student attendance",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_batch_attendance_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date & Teacher Scope Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_attendance_date_input")
                    )
                    if (showScopeToggle) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = onlyAssigned,
                                onClick = { onlyAssigned = true },
                                label = { Text("My Students (${assignedStudents.size})", fontSize = 10.sp) },
                                modifier = Modifier.testTag("batch_scope_my_students")
                            )
                            FilterChip(
                                selected = !onlyAssigned,
                                onClick = { onlyAssigned = false },
                                label = { Text("All Campus (${allStudents.size})", fontSize = 10.sp) },
                                modifier = Modifier.testTag("batch_scope_all_students")
                            )
                        }
                    }
                }

                // Sunday / Weekly Off Informational Banner (Attendance Open)
                if (holidayReason != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_attendance_holiday_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "Weekly Off",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$holidayReason • Batch Attendance Open",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Batch Name Chips
                Text(
                    text = "Select Batch:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(batchOptions) { batch ->
                        val count = if (batch == "ALL") {
                            scopedStudents.size
                        } else {
                            scopedStudents.count { it.batchName.ifBlank { "General Batch" } == batch }
                        }
                        FilterChip(
                            selected = selectedBatch == batch,
                            onClick = { selectedBatch = batch },
                            label = {
                                Text(
                                    text = if (batch == "ALL") "All Batches ($count)" else "$batch ($count)",
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = if (selectedBatch == batch) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.testTag("batch_filter_chip_${batch.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // 1-Hour Batch Timing Chips
                if (timingOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Batch Timing (1-Hr Slot):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(timingOptions) { timing ->
                            FilterChip(
                                selected = selectedTiming == timing,
                                onClick = { selectedTiming = timing },
                                label = {
                                    Text(
                                        text = if (timing == "ALL") "All Timings" else timing,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.testTag("timing_filter_chip_${timing.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search inside batch
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Filter by student name or admission #") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_attendance_search")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Batch Controls ("All Present" / "All Absent")
                if (filteredStudents.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                filteredStudents.forEach { statuses[it.admissionNo] = "PRESENT" }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("batch_mark_all_present"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Present (${filteredStudents.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                filteredStudents.forEach { statuses[it.admissionNo] = "ABSENT" }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("batch_mark_all_absent"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Absent", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$activeBatchLabel • $presentCount Present, $absentCount Absent (${filteredStudents.size} total)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Student List in Batch (No Fee Info Shown)
                if (filteredStudents.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            icon = Icons.Default.Groups,
                            message = "No students match this batch filter"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredStudents, key = { it.admissionNo }) { student ->
                            val currentStatus = statuses[student.admissionNo] ?: "PRESENT"
                            val existingRecord = existingByAdm[student.admissionNo]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("batch_student_card_${student.admissionNo}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = "Adm: ${student.admissionNo} • ${student.courseName}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = buildString {
                                                    append(student.batchName.ifBlank { "General Batch" })
                                                    if (student.batchTiming.isNotBlank()) append(" (${student.batchTiming})")
                                                    append(" • ${student.branchName}")
                                                },
                                                fontSize = 10.sp,
                                                color = SecondaryTeal,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (existingRecord != null) {
                                            Surface(
                                                color = SuccessGreen.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Saved: ${existingRecord.status}",
                                                    fontSize = 9.sp,
                                                    color = SuccessGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            "PRESENT" to "Present",
                                            "ABSENT" to "Absent",
                                            "HALF_DAY" to "Half",
                                            "LEAVE" to "Leave"
                                        ).forEach { (statusKey, label) ->
                                            FilterChip(
                                                selected = currentStatus == statusKey,
                                                onClick = { statuses[student.admissionNo] = statusKey },
                                                label = { Text(label, fontSize = 10.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("batch_status_${student.admissionNo}_$statusKey")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.markBatchStudentAttendance(
                            students = filteredStudents,
                            statuses = statuses,
                            date = selectedDate,
                            batchLabel = activeBatchLabel,
                            onSuccess = onDismiss
                        )
                    },
                    enabled = filteredStudents.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_batch_attendance_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalDarkRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Batch Attendance (${filteredStudents.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
