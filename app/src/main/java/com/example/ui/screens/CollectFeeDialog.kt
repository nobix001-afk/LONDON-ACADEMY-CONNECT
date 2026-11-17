package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.model.StudentEntity
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectFeeDialog(
    viewModel: EduViewModel,
    preselectedStudent: StudentEntity? = null,
    onDismiss: () -> Unit
) {
    val students by viewModel.allStudents.collectAsState()
    var selectedStudent by remember { mutableStateOf(preselectedStudent ?: students.firstOrNull()) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    val paymentModes = listOf("UPI", "Cash", "Card", "Bank Transfer")
    var selectedMode by remember { mutableStateOf(paymentModes[0]) }

    var nextDueDate by remember { mutableStateOf("2026-10-15") }
    var remarks by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("collect_fee_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Collect Student Fee",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Student
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = !studentDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.name} (${it.admissionNo})" } ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Student") },
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
                                text = { Text("${s.name} - ${s.admissionNo} (Pending: ₹${s.pendingFees.toInt()})") },
                                onClick = {
                                    selectedStudent = s
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Student balance card
                if (selectedStudent != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Fees: ₹${selectedStudent!!.totalFees.toInt()}", fontSize = 12.sp)
                                Text("Paid: ₹${selectedStudent!!.paidFees.toInt()}", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pending Due:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("₹${selectedStudent!!.pendingFees.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fee_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Mode
                Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    paymentModes.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nextDueDate,
                    onValueChange = { nextDueDate = it },
                    label = { Text("Next Due Date (if any)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val s = selectedStudent
                        if (s == null) {
                            errorMessage = "Please select a student"
                            return@Button
                        }
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            errorMessage = "Please enter a valid payment amount"
                            return@Button
                        }
                        if (amt > s.pendingFees && s.pendingFees > 0) {
                            errorMessage = "Amount cannot exceed pending fee (₹${s.pendingFees.toInt()})"
                            return@Button
                        }
                        errorMessage = null
                        viewModel.recordFeePayment(
                            admissionNo = s.admissionNo,
                            amount = amt,
                            mode = selectedMode,
                            plan = s.paymentPlan,
                            remarks = remarks,
                            nextDueDate = nextDueDate
                        ) { success ->
                            if (success) onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_fee_payment_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Collect Fee & Generate Receipt", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
