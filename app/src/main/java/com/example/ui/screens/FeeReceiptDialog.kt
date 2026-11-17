package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.ExcelExporter
import com.example.data.ExcelExporter.ExcelSheet
import com.example.data.model.FeePaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun FeeReceiptDialog(
    payment: FeePaymentEntity,
    student: StudentEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val totalCourseFee = student?.totalFees ?: 0.0
    val totalPaid = student?.paidFees ?: payment.amount
    val pendingFee = student?.pendingFees ?: 0.0

    val shareReceiptText = """
        *LONDON ACADEMY CONNECT*
        Official Fee Receipt: ${payment.receiptNo}
        ------------------------------------------
        Student Name: ${payment.studentName}
        Admission No: ${payment.admissionNo}
        Course: ${payment.courseName.ifEmpty { student?.courseName ?: "London Academy Course" }}
        Installment Month: ${payment.installmentMonth.ifEmpty { "General Fee" }}
        Payment Date: ${payment.paymentDate}
        ------------------------------------------
        Amount Paid: ₹${payment.amount.toInt()}
        Payment Mode: ${payment.paymentMode}
        Plan: ${payment.paymentPlan}
        ------------------------------------------
        Total Course Fee: ₹${totalCourseFee.toInt()}
        Total Paid to Date: ₹${totalPaid.toInt()}
        Remaining Balance: ₹${pendingFee.toInt()}
        Collected By: ${payment.collectedBy}
        Remarks: ${payment.remarks.ifEmpty { "None" }}
        ------------------------------------------
        London Academy Connect • Official Account Record
    """.trimIndent()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("fee_receipt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PAYMENT CONFIRMED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Official Receipt Document Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_london_academy_logo),
                                        contentDescription = "London Academy Crest",
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "LONDON ACADEMY CONNECT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Navy900
                                )
                                Text(
                                    "OFFICIAL PAYMENT RECEIPT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Receipt Metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Receipt Number", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.receiptNo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Payment Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.paymentDate, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amount Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SuccessGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("AMOUNT RECEIVED", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                                Text(
                                    "₹${payment.amount.toInt()}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen
                                )
                                Text(
                                    "Mode: ${payment.paymentMode} • ${payment.paymentPlan}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Student Details Grid
                        ReceiptDetailRow(label = "Student Name", value = payment.studentName)
                        ReceiptDetailRow(label = "Admission No", value = payment.admissionNo)
                        ReceiptDetailRow(label = "Course", value = payment.courseName.ifEmpty { student?.courseName ?: "Course Enrolled" })
                        if (payment.installmentMonth.isNotEmpty()) {
                            ReceiptDetailRow(label = "Installment Month", value = payment.installmentMonth)
                        }
                        if (payment.nextDueDate.isNotEmpty()) {
                            ReceiptDetailRow(label = "Next Due Date", value = payment.nextDueDate)
                        }
                        if (payment.remarks.isNotEmpty()) {
                            ReceiptDetailRow(label = "Remarks / Note", value = payment.remarks)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Overall Balance Summary
                        ReceiptDetailRow(label = "Total Course Fee", value = "₹${totalCourseFee.toInt()}", isBold = false)
                        ReceiptDetailRow(label = "Total Paid (Cumulative)", value = "₹${totalPaid.toInt()}", valueColor = SuccessGreen)
                        ReceiptDetailRow(
                            label = "Remaining Balance Due",
                            value = "₹${pendingFee.toInt()}",
                            valueColor = if (pendingFee > 0) MaterialTheme.colorScheme.error else SuccessGreen
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recorded by: ${payment.collectedBy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Verified Stamp ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sheet = ExcelSheet(
                                    name = "Fee Receipt",
                                    titleBanner = "London Academy Connect - Official Fee Receipt (${payment.receiptNo})",
                                    headers = listOf("Field", "Details"),
                                    rows = listOf(
                                        listOf("Receipt No", payment.receiptNo),
                                        listOf("Student Name", payment.studentName),
                                        listOf("Admission No", payment.admissionNo),
                                        listOf("Course", payment.courseName.ifEmpty { student?.courseName ?: "London Academy Course" }),
                                        listOf("Installment Month", payment.installmentMonth.ifEmpty { "General Fee" }),
                                        listOf("Payment Date", payment.paymentDate),
                                        listOf("Amount Paid (Rs)", payment.amount),
                                        listOf("Payment Mode", payment.paymentMode),
                                        listOf("Plan", payment.paymentPlan),
                                        listOf("Total Course Fee (Rs)", totalCourseFee),
                                        listOf("Total Paid to Date (Rs)", totalPaid),
                                        listOf("Remaining Balance (Rs)", pendingFee),
                                        listOf("Next Due Date", payment.nextDueDate.ifEmpty { "-" }),
                                        listOf("Collected By", payment.collectedBy),
                                        listOf("Remarks", payment.remarks.ifEmpty { "None" })
                                    )
                                )
                                val file = ExcelExporter.writeWorkbookToFile(
                                    context = context,
                                    fileNamePrefix = "Fee_Receipt_${payment.receiptNo}",
                                    sheets = listOf(sheet)
                                )
                                ExcelExporter.shareExcelFile(context, file, "Share Fee Receipt Excel (.xlsx)")
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_receipt_excel_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel (.xlsx)", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareReceiptText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share London Academy Receipt")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("share_receipt_text_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Text", fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("receipt_done_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Done", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailRow(
    label: String,
    value: String,
    isBold: Boolean = true,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}
