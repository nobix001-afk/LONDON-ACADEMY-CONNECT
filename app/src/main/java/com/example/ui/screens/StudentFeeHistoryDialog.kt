package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.FeeInstallmentEntity
import com.example.data.model.FeePaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TertiaryAmber
import com.example.ui.viewmodel.EduViewModel

@Composable
fun StudentFeeHistoryDialog(
    student: StudentEntity,
    viewModel: EduViewModel,
    onAddPaymentForInstallment: (FeeInstallmentEntity) -> Unit,
    onViewReceipt: (FeePaymentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val allInstallments by viewModel.allInstallments.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()

    val studentInstallments = remember(allInstallments, student.admissionNo) {
        allInstallments.filter { it.admissionNo == student.admissionNo }
            .sortedBy { it.installmentNumber }
    }

    val studentPayments = remember(allPayments, student.admissionNo) {
        allPayments.filter { it.admissionNo == student.admissionNo }
            .sortedByDescending { it.id }
    }

    // Calculate current month (September 2026) metrics
    val currentMonthInstallment = studentInstallments.firstOrNull { it.monthName.contains("September", ignoreCase = true) }
    val currentMonthDue = currentMonthInstallment?.installmentAmount ?: student.monthlyFeeAmount
    val currentMonthPaid = currentMonthInstallment?.amountPaid ?: 0.0
    val currentMonthPending = currentMonthInstallment?.remainingAmount ?: (currentMonthDue - currentMonthPaid).coerceAtLeast(0.0)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Installments, 1: Receipts

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("student_fee_history_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${student.admissionNo} • ${student.courseName}",
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The 6 Mandatory Overview Metrics Cards
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Overall Account Status",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 1: Total Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBox(label = "Total Course Fee", value = "₹${student.totalFees.toInt()}", modifier = Modifier.weight(1f))
                            MetricBox(label = "Total Paid", value = "₹${student.paidFees.toInt()}", color = SuccessGreen, modifier = Modifier.weight(1f))
                            MetricBox(
                                label = "Total Pending",
                                value = "₹${student.pendingFees.toInt()}",
                                color = if (student.pendingFees > 0) MaterialTheme.colorScheme.error else SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "Current Month (September 2026)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Current Month Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBox(label = "Month Due", value = "₹${currentMonthDue.toInt()}", modifier = Modifier.weight(1f))
                            MetricBox(label = "Month Paid", value = "₹${currentMonthPaid.toInt()}", color = SuccessGreen, modifier = Modifier.weight(1f))
                            MetricBox(
                                label = "Month Pending",
                                value = "₹${currentMonthPending.toInt()}",
                                color = if (currentMonthPending > 0) TertiaryAmber else SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Monthly Installments (${studentInstallments.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Payment Receipts (${studentPayments.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content
                if (selectedTab == 0) {
                    if (studentInstallments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No installment schedule found.\nStudent may be enrolled under One-Time plan.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(studentInstallments) { inst ->
                                InstallmentItemCard(
                                    installment = inst,
                                    onPayClick = { onAddPaymentForInstallment(inst) }
                                )
                            }
                        }
                    }
                } else {
                    if (studentPayments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No receipts generated yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(studentPayments) { pay ->
                                ReceiptItemCard(
                                    payment = pay,
                                    onViewReceipt = { onViewReceipt(pay) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun InstallmentItemCard(
    installment: FeeInstallmentEntity,
    onPayClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        ),
        color = if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(installment.monthName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when (installment.status) {
                            "PAID" -> SuccessGreen.copy(alpha = 0.12f)
                            "PARTIAL" -> TertiaryAmber.copy(alpha = 0.15f)
                            "OVERDUE" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            else -> PrimaryBlue.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = installment.status,
                            color = when (installment.status) {
                                "PAID" -> SuccessGreen
                                "PARTIAL" -> TertiaryAmber
                                "OVERDUE" -> MaterialTheme.colorScheme.error
                                else -> PrimaryBlue
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Due: ${installment.dueDate} • Total: ₹${installment.installmentAmount.toInt()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (installment.amountPaid > 0) {
                    Text(
                        text = "Paid: ₹${installment.amountPaid.toInt()} • Remaining: ₹${installment.remainingAmount.toInt()}",
                        fontSize = 11.sp,
                        color = if (installment.remainingAmount > 0) TertiaryAmber else SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (installment.status != "PAID") {
                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error else PrimaryBlue
                    )
                ) {
                    Text("Add Payment", fontSize = 11.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settled", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ReceiptItemCard(
    payment: FeePaymentEntity,
    onViewReceipt: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(payment.receiptNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                Text("Date: ${payment.paymentDate} • ${payment.paymentMode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (payment.installmentMonth.isNotEmpty()) {
                    Text("Month: ${payment.installmentMonth}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₹${payment.amount.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SuccessGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedIconButton(onClick = onViewReceipt, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Receipt, contentDescription = "View Receipt", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
