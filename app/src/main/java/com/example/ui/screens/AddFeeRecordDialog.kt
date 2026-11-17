package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FeePaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TertiaryAmber
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeeRecordDialog(
    viewModel: EduViewModel,
    preselectedStudent: StudentEntity? = null,
    onSuccess: (FeePaymentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val students by viewModel.allStudents.collectAsState()
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { sdfDate.format(Date()) }

    var selectedStudent by remember { mutableStateOf(preselectedStudent) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    // 1. Student Name
    var studentName by remember { mutableStateOf(preselectedStudent?.name ?: "") }
    // 2. Date
    var recordDate by remember { mutableStateOf(todayStr) }
    // 3. Course
    var courseName by remember { mutableStateOf(preselectedStudent?.courseName ?: "Full Stack Web Development") }
    // 4. Fees Amount
    var amountText by remember { mutableStateOf("") }
    // 5. Mode of Payment
    val modes = listOf("Cash", "UPI", "Bank Transfer", "Card", "Other")
    var selectedMode by remember { mutableStateOf("UPI") }
    var customModeText by remember { mutableStateOf("") }
    // 6. Admission Number
    var admissionNo by remember {
        mutableStateOf(preselectedStudent?.admissionNo ?: "LA${System.currentTimeMillis().toString().takeLast(5)}")
    }
    // 7. Payment Type
    val paymentTypes = listOf("ONE-TIME", "MONTHLY", "PART PAYMENT", "YEARLY")
    var selectedPaymentType by remember { mutableStateOf(preselectedStudent?.paymentPlan ?: "MONTHLY") }
    // 8. Installment Month
    val monthNames = listOf(
        "September 2026",
        "October 2026",
        "November 2026",
        "December 2026",
        "January 2027",
        "August 2026",
        "July 2026"
    )
    var selectedMonth by remember { mutableStateOf("September 2026") }
    // 9. Due Date
    var dueDate by remember { mutableStateOf(preselectedStudent?.nextDueDate?.ifEmpty { "2026-09-25" } ?: "2026-09-25") }
    // 10. Remarks
    var remarks by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("add_fee_record_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AddCard, contentDescription = null, tint = PrimaryBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Manual Pending Fee Entry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "London Academy Accounts Ledger",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Select from Existing Students
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = !studentDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.name} (${it.admissionNo})" } ?: if (studentName.isNotEmpty()) "$studentName ($admissionNo)" else "Select Existing Student (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Choose Student or Enter Manually Below") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${s.name} • ${s.admissionNo}", fontWeight = FontWeight.SemiBold)
                                        Text("${s.courseName} | Due: ₹${s.pendingFees.toInt()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedStudent = s
                                    studentName = s.name
                                    admissionNo = s.admissionNo
                                    courseName = s.courseName
                                    selectedPaymentType = s.paymentPlan
                                    if (s.nextDueDate.isNotEmpty()) dueDate = s.nextDueDate
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field 1 & 6: Student Name & Admission Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = {
                            studentName = it
                            selectedStudent = null
                        },
                        label = { Text("Student Name *") },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_fee_student_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = admissionNo,
                        onValueChange = { admissionNo = it },
                        label = { Text("Admission No *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fee_admission_no"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field 2 & 9: Record Date & Due Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = recordDate,
                        onValueChange = { recordDate = it },
                        label = { Text("Payment Date *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Next Due Date") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field 3: Course
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 7: Payment Type
                Text("Payment Type *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    paymentTypes.forEach { type ->
                        FilterChip(
                            selected = selectedPaymentType == type,
                            onClick = { selectedPaymentType = type },
                            label = { Text(type, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field 8: Installment Month
                Text("Installment Month *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    monthNames.take(3).forEach { m ->
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(m.split(" ")[0], fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field 4: Fees Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Fees Amount (₹) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fee_record_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 5: Mode of Payment
                Text("Mode of Payment *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { m ->
                        FilterChip(
                            selected = selectedMode == m,
                            onClick = { selectedMode = m },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }

                if (selectedMode == "Other") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customModeText,
                        onValueChange = { customModeText = it },
                        label = { Text("Custom Mode (e.g. Cheque, DD, Scholarship)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field 10: Remarks
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Transaction ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (studentName.isBlank()) {
                            errorMessage = "Please enter student name"
                            return@Button
                        }
                        if (admissionNo.isBlank()) {
                            errorMessage = "Please enter admission number"
                            return@Button
                        }
                        if (courseName.isBlank()) {
                            errorMessage = "Please enter course name"
                            return@Button
                        }
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            errorMessage = "Please enter a valid fees amount"
                            return@Button
                        }
                        val finalMode = if (selectedMode == "Other" && customModeText.isNotBlank()) customModeText else selectedMode

                        errorMessage = null
                        isSubmitting = true
                        viewModel.addFeeRecord(
                            studentName = studentName.trim(),
                            admissionNo = admissionNo.trim(),
                            course = courseName.trim(),
                            amount = amt,
                            mode = finalMode,
                            paymentType = selectedPaymentType,
                            installmentMonth = selectedMonth,
                            dueDate = dueDate,
                            remarks = remarks
                        ) { receipt ->
                            isSubmitting = false
                            onSuccess(receipt)
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_fee_record"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Fee Record & Generate Receipt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
