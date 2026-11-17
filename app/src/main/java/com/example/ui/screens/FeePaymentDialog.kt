package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.FeeInstallmentEntity
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
fun FeePaymentDialog(
    viewModel: EduViewModel,
    installment: FeeInstallmentEntity,
    student: StudentEntity? = null,
    onPaymentSuccess: (FeePaymentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var paymentDate by remember { mutableStateOf(sdfDate.format(Date())) }

    // Prepopulate payment amount with remaining amount
    var amountText by remember { mutableStateOf(installment.remainingAmount.toInt().toString()) }

    val defaultModes = listOf("UPI", "Cash", "Bank Transfer", "Card", "Other")
    var selectedMode by remember { mutableStateOf("UPI") }
    var customModeText by remember { mutableStateOf("") }

    var remarks by remember { mutableStateOf("Payment for ${installment.monthName} installment") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val newRemaining = max(0.0, installment.remainingAmount - enteredAmount)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("fee_payment_dialog"),
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
                                Icon(Icons.Default.Payment, contentDescription = null, tint = PrimaryBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Collect Installment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${installment.monthName} • Due: ${installment.dueDate}",
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

                // Pre-populated Student Info Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(installment.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Adm No: ${installment.admissionNo}", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                            }
                            Surface(
                                color = when (installment.status) {
                                    "OVERDUE" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    "PARTIAL" -> TertiaryAmber.copy(alpha = 0.15f)
                                    else -> PrimaryBlue.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = installment.status,
                                    color = when (installment.status) {
                                        "OVERDUE" -> MaterialTheme.colorScheme.error
                                        "PARTIAL" -> TertiaryAmber
                                        else -> PrimaryBlue
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(installment.courseName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Financial breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Installment", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${installment.installmentAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Already Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${installment.amountPaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Pending Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₹${installment.remainingAmount.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error else TertiaryAmber
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Date
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("Payment Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount to Collect (₹) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_installment_payment_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        TextButton(onClick = { amountText = installment.remainingAmount.toInt().toString() }) {
                            Text("Full", fontSize = 11.sp)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live recalculation box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (newRemaining == 0.0) SuccessGreen.copy(alpha = 0.08f) else TertiaryAmber.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (newRemaining == 0.0) "✓ Will fully settle installment (PAID)" else "Partial payment: balance ₹${newRemaining.toInt()} remaining",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (newRemaining == 0.0) SuccessGreen else TertiaryAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Mode Selection
                Text("Mode of Payment *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    defaultModes.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode, fontSize = 11.sp) }
                        )
                    }
                }

                if (selectedMode == "Other") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customModeText,
                        onValueChange = { customModeText = it },
                        label = { Text("Specify Payment Mode (e.g. Cheque, Scholarship)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Remarks
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Reference Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            errorMessage = "Please enter a valid payment amount"
                            return@Button
                        }
                        if (amt > installment.remainingAmount) {
                            errorMessage = "Amount cannot exceed pending due of ₹${installment.remainingAmount.toInt()}"
                            return@Button
                        }
                        val finalMode = if (selectedMode == "Other" && customModeText.isNotBlank()) customModeText else selectedMode
                        errorMessage = null
                        isSubmitting = true
                        viewModel.payInstallment(
                            installmentId = installment.id,
                            amount = amt,
                            mode = finalMode,
                            paymentDate = paymentDate,
                            remarks = remarks
                        ) { receipt ->
                            isSubmitting = false
                            if (receipt != null) {
                                onPaymentSuccess(receipt)
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_installment_payment"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Payment & Generate Receipt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
