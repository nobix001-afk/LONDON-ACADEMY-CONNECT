package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExcelExporter
import com.example.data.model.FeeInstallmentEntity
import com.example.data.model.FeePaymentEntity
import com.example.data.model.StudentEntity
import com.example.data.model.UserRole
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TertiaryAmber
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class FeeFilterStatus { ALL, OVERDUE, PARTIAL, PENDING, PAID }
enum class FeeFilterPlan { ALL, MONTHLY, ONE_TIME, PART_PAYMENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingFeesScreen(
    viewModel: EduViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val allInstallments by viewModel.allInstallments.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()

    // Dialog states
    var showAddFeeRecordDialog by remember { mutableStateOf(false) }
    var payingInstallment by remember { mutableStateOf<FeeInstallmentEntity?>(null) }
    var selectedStudentForHistory by remember { mutableStateOf<StudentEntity?>(null) }
    var viewingReceipt by remember { mutableStateOf<FeePaymentEntity?>(null) }
    var editingPayment by remember { mutableStateOf<FeePaymentEntity?>(null) }

    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(FeeFilterStatus.ALL) }
    var planFilter by remember { mutableStateOf(FeeFilterPlan.ALL) }
    var selectedSection by remember { mutableIntStateOf(0) } // 0: This Month Pending, 1: Overdue Fees, 2: All Installments, 3: Receipts Ledger

    val currentMonthName = "September 2026"
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { sdfDate.format(Date()) }

    // Dashboard Calculations
    val totalStudentsCount = allStudents.size
    val studentsWithPending = allStudents.filter { it.pendingFees > 0 }
    val pendingStudentsCount = studentsWithPending.size
    val totalPendingAmount = allStudents.sumOf { it.pendingFees }

    val thisMonthInstallments = allInstallments.filter { it.monthName.contains("September", ignoreCase = true) }
    val thisMonthPendingInstallments = thisMonthInstallments.filter { it.status != "PAID" }
    val thisMonthPendingAmount = thisMonthInstallments.sumOf { it.remainingAmount }
    val thisMonthCollection = allPayments.filter { it.paymentDate.startsWith("2026-09") || it.installmentMonth.contains("September", ignoreCase = true) }
        .sumOf { it.amount }

    val oneTimePendingFees = allStudents.filter { it.paymentPlan == "ONE-TIME" }.sumOf { it.pendingFees }
    val monthlyInstallmentsPending = allInstallments.filter { it.status != "PAID" }.sumOf { it.remainingAmount }

    // Overdue installments calculation: due date < today and status != PAID
    val overdueInstallments = allInstallments.filter {
        it.status == "OVERDUE" || (it.status != "PAID" && it.dueDate.isNotEmpty() && it.dueDate < todayDateStr)
    }

    // Filtered lists
    val filteredThisMonth = remember(thisMonthInstallments, searchQuery, statusFilter, planFilter) {
        thisMonthInstallments.filter { inst ->
            val matchesQuery = searchQuery.isBlank() ||
                    inst.studentName.contains(searchQuery, ignoreCase = true) ||
                    inst.admissionNo.contains(searchQuery, ignoreCase = true) ||
                    inst.courseName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (statusFilter) {
                FeeFilterStatus.ALL -> true
                FeeFilterStatus.OVERDUE -> inst.status == "OVERDUE"
                FeeFilterStatus.PARTIAL -> inst.status == "PARTIAL"
                FeeFilterStatus.PENDING -> inst.status == "PENDING"
                FeeFilterStatus.PAID -> inst.status == "PAID"
            }
            matchesQuery && matchesStatus
        }
    }

    val filteredInstallments = remember(allInstallments, searchQuery, statusFilter) {
        allInstallments.filter { inst ->
            val matchesQuery = searchQuery.isBlank() ||
                    inst.studentName.contains(searchQuery, ignoreCase = true) ||
                    inst.admissionNo.contains(searchQuery, ignoreCase = true) ||
                    inst.courseName.contains(searchQuery, ignoreCase = true) ||
                    inst.monthName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (statusFilter) {
                FeeFilterStatus.ALL -> true
                FeeFilterStatus.OVERDUE -> inst.status == "OVERDUE"
                FeeFilterStatus.PARTIAL -> inst.status == "PARTIAL"
                FeeFilterStatus.PENDING -> inst.status == "PENDING"
                FeeFilterStatus.PAID -> inst.status == "PAID"
            }
            matchesQuery && matchesStatus
        }
    }

    val filteredPayments = remember(allPayments, searchQuery) {
        allPayments.filter { pay ->
            searchQuery.isBlank() ||
                    pay.studentName.contains(searchQuery, ignoreCase = true) ||
                    pay.admissionNo.contains(searchQuery, ignoreCase = true) ||
                    pay.receiptNo.contains(searchQuery, ignoreCase = true) ||
                    pay.courseName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pending_fees_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action Bar with "+ Add Fee Record" and "Export Report"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fees & Pending Fees",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "London Academy Fee Collection & Installments Tracker",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Export Excel (.xlsx) Button
                    OutlinedButton(
                        onClick = {
                            val file = viewModel.exportFeeLedgerExcel(context, allInstallments, allPayments)
                            ExcelExporter.shareExcelFile(context, file, "Export London Academy Fees Excel (.xlsx)")
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("btn_export_fee_excel")
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 12.sp)
                    }

                    // + Add Fee Record Button
                    Button(
                        onClick = { showAddFeeRecordDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_add_fee_record")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Fee Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 1. PENDING FEES DASHBOARD TOP METRIC CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary Highlight Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        title = "Pending Students",
                        value = "$pendingStudentsCount / $totalStudentsCount",
                        subtext = "Have pending fee balances",
                        accentColor = TertiaryAmber,
                        icon = Icons.Default.PeopleOutline,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        title = "This Month Pending",
                        value = "₹${thisMonthPendingAmount.toInt()}",
                        subtext = "${thisMonthPendingInstallments.size} installments due",
                        accentColor = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        title = "Total Pending",
                        value = "₹${totalPendingAmount.toInt()}",
                        subtext = "Across all enrolled courses",
                        accentColor = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        title = "This Month Collection",
                        value = "₹${thisMonthCollection.toInt()}",
                        subtext = "Collected in September",
                        accentColor = SuccessGreen,
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Breakdown Mini Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("One-Time Pending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${oneTimePendingFees.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Monthly Pending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${monthlyInstallmentsPending.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TertiaryAmber)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Overdue Dues", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            Text("${overdueInstallments.size} Due", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Section Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("This Month's Pending", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (thisMonthPendingInstallments.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge { Text("${thisMonthPendingInstallments.size}") }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Overdue Fees", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (overdueInstallments.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.error) { Text("${overdueInstallments.size}") }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = { Text("All Installments (${allInstallments.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    text = { Text("Payment Receipts (${allPayments.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Search Field and Status Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Student, Admission No, Course...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_pending_fees"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Status Chips
                if (selectedSection in 0..2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeeFilterStatus.values().forEach { status ->
                            FilterChip(
                                selected = statusFilter == status,
                                onClick = { statusFilter = status },
                                label = { Text(status.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // SECTION CONTENT
        when (selectedSection) {
            0 -> {
                // Section 1: This Month's Pending Installments
                if (filteredThisMonth.isEmpty()) {
                    item {
                        EmptyPendingState(message = "No pending installments found for September 2026.")
                    }
                } else {
                    items(filteredThisMonth, key = { it.id }) { inst ->
                        val student = allStudents.firstOrNull { it.admissionNo == inst.admissionNo }
                        StudentInstallmentCard(
                            installment = inst,
                            student = student,
                            onAddPayment = { payingInstallment = inst },
                            onViewHistory = {
                                student?.let { selectedStudentForHistory = it }
                            },
                            onSendReminder = {
                                student?.let {
                                    viewModel.sendFeeReminder(it, inst.monthName, inst.remainingAmount)
                                }
                            }
                        )
                    }
                }
            }
            1 -> {
                // Section 2: Overdue Fees
                if (overdueInstallments.isEmpty()) {
                    item {
                        EmptyPendingState(message = "Excellent! No overdue fees recorded.")
                    }
                } else {
                    items(overdueInstallments, key = { it.id }) { inst ->
                        val student = allStudents.firstOrNull { it.admissionNo == inst.admissionNo }
                        StudentInstallmentCard(
                            installment = inst,
                            student = student,
                            isUrgentOverdue = true,
                            onAddPayment = { payingInstallment = inst },
                            onViewHistory = {
                                student?.let { selectedStudentForHistory = it }
                            },
                            onSendReminder = {
                                student?.let {
                                    viewModel.sendFeeReminder(it, inst.monthName, inst.remainingAmount)
                                }
                            }
                        )
                    }
                }
            }
            2 -> {
                // Section 3: All Installments
                if (filteredInstallments.isEmpty()) {
                    item {
                        EmptyPendingState(message = "No installments match the filter.")
                    }
                } else {
                    items(filteredInstallments, key = { it.id }) { inst ->
                        val student = allStudents.firstOrNull { it.admissionNo == inst.admissionNo }
                        StudentInstallmentCard(
                            installment = inst,
                            student = student,
                            onAddPayment = { payingInstallment = inst },
                            onViewHistory = {
                                student?.let { selectedStudentForHistory = it }
                            },
                            onSendReminder = {
                                student?.let {
                                    viewModel.sendFeeReminder(it, inst.monthName, inst.remainingAmount)
                                }
                            }
                        )
                    }
                }
            }
            3 -> {
                // Section 4: Payment Receipts Ledger
                if (filteredPayments.isEmpty()) {
                    item {
                        EmptyPendingState(message = "No payment receipts found.")
                    }
                } else {
                    items(filteredPayments, key = { it.id }) { pay ->
                        val student = allStudents.firstOrNull { it.admissionNo == pay.admissionNo }
                        ReceiptLedgerCard(
                            payment = pay,
                            student = student,
                            isAdmin = currentUser?.role == UserRole.ADMIN,
                            onViewReceipt = { viewingReceipt = pay },
                            onEditReceipt = { editingPayment = pay }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddFeeRecordDialog) {
        AddFeeRecordDialog(
            viewModel = viewModel,
            onSuccess = { receipt ->
                showAddFeeRecordDialog = false
                viewingReceipt = receipt
            },
            onDismiss = { showAddFeeRecordDialog = false }
        )
    }

    payingInstallment?.let { inst ->
        val student = allStudents.firstOrNull { it.admissionNo == inst.admissionNo }
        FeePaymentDialog(
            viewModel = viewModel,
            installment = inst,
            student = student,
            onPaymentSuccess = { receipt ->
                payingInstallment = null
                viewingReceipt = receipt
            },
            onDismiss = { payingInstallment = null }
        )
    }

    selectedStudentForHistory?.let { student ->
        StudentFeeHistoryDialog(
            student = student,
            viewModel = viewModel,
            onAddPaymentForInstallment = { inst ->
                selectedStudentForHistory = null
                payingInstallment = inst
            },
            onViewReceipt = { receipt ->
                viewingReceipt = receipt
            },
            onDismiss = { selectedStudentForHistory = null }
        )
    }

    viewingReceipt?.let { receipt ->
        val student = allStudents.firstOrNull { it.admissionNo == receipt.admissionNo }
        FeeReceiptDialog(
            payment = receipt,
            student = student,
            onDismiss = { viewingReceipt = null }
        )
    }

    editingPayment?.let { payment ->
        EditFeePaymentDialog(
            payment = payment,
            viewModel = viewModel,
            onDismiss = { editingPayment = null }
        )
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Navy900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtext, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StudentInstallmentCard(
    installment: FeeInstallmentEntity,
    student: StudentEntity?,
    isUrgentOverdue: Boolean = false,
    onAddPayment: () -> Unit,
    onViewHistory: () -> Unit,
    onSendReminder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installment_card_${installment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgentOverdue || installment.status == "OVERDUE") {
                MaterialTheme.colorScheme.error.copy(alpha = 0.04f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUrgentOverdue || installment.status == "OVERDUE") MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Student Name, Admission No, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(installment.studentName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Navy900)
                    Text(
                        "${installment.admissionNo} • ${installment.courseName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when (installment.status) {
                        "PAID" -> SuccessGreen.copy(alpha = 0.12f)
                        "PARTIAL" -> TertiaryAmber.copy(alpha = 0.15f)
                        "OVERDUE" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> PrimaryBlue.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = installment.status,
                        color = when (installment.status) {
                            "PAID" -> SuccessGreen
                            "PARTIAL" -> TertiaryAmber
                            "OVERDUE" -> MaterialTheme.colorScheme.error
                            else -> PrimaryBlue
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Installment Month, Due Date, Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Month / Plan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(installment.monthName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Due: ${installment.dueDate}", fontSize = 10.sp, color = if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Installment", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${installment.installmentAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Paid: ₹${installment.amountPaid.toInt()}", fontSize = 10.sp, color = SuccessGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₹${installment.remainingAmount.toInt()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (installment.remainingAmount > 0) {
                            if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error else TertiaryAmber
                        } else SuccessGreen
                    )
                    if (student != null) {
                        Text("Total Left: ₹${student.pendingFees.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (installment.status != "PAID") {
                    Button(
                        onClick = onAddPayment,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                            .testTag("btn_add_payment_${installment.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (installment.status == "OVERDUE") MaterialTheme.colorScheme.error else PrimaryBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedIconButton(
                        onClick = onSendReminder,
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Send Reminder", tint = TertiaryAmber, modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fee Ledger", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ReceiptLedgerCard(
    payment: FeePaymentEntity,
    student: StudentEntity?,
    isAdmin: Boolean,
    onViewReceipt: () -> Unit,
    onEditReceipt: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(payment.receiptNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(payment.paymentMode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(payment.studentName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "Adm: ${payment.admissionNo} • ${payment.paymentDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.remarks.isNotEmpty()) {
                    Text("Note: ${payment.remarks}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("₹${payment.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SuccessGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAdmin) {
                        IconButton(onClick = onEditReceipt, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                        }
                    }
                    IconButton(onClick = onViewReceipt, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Receipt, contentDescription = "View Receipt", tint = PrimaryBlue, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPendingState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
