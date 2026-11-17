package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.FeeInstallmentEntity
import com.example.data.model.FeePaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

fun formatPayingMethodLabel(paymentPlan: String, installmentCount: Int): String {
    return when (paymentPlan.uppercase()) {
        "ONE-TIME" -> "ONE-TIME"
        "YEARLY" -> "YEARLY"
        else -> if (installmentCount > 1) "$paymentPlan ($installmentCount Installments)" else paymentPlan
    }
}

private fun resolveImageModel(uriOrPath: String): Any? {
    if (uriOrPath.isBlank()) return null
    return if (uriOrPath.startsWith("/")) File(uriOrPath) else uriOrPath
}

@Composable
fun SubAdminStudentCard(
    student: StudentEntity,
    paymentCount: Int = 0,
    onViewDetails: () -> Unit,
    onCollectFee: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("subadmin_student_card_${student.admissionNo}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Student Name + Admission No + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Adm #: ${student.admissionNo} • Contact: ${student.contactNo}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
                StatusPill(student.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parent & Course Details
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Father: ${student.fatherName.ifBlank { "—" }}",
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Mother: ${student.motherName.ifBlank { "—" }}",
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Course: ${student.courseName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Branch: ${student.branchName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryTeal
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duration: ${student.courseDuration}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Joined: ${student.dateOfJoining}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Batch: ${student.batchName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Timing: ${student.batchTiming.ifBlank { "Not Set" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Paying Method & Aadhaar Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SecondaryTeal.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatPayingMethodLabel(student.paymentPlan, student.installmentCount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                val hasAadhaar = student.aadhaarCardUri.isNotBlank()
                Surface(
                    color = if (hasAadhaar) SuccessGreen.copy(alpha = 0.14f) else TertiaryAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (hasAadhaar) "Aadhaar: Uploaded" else "Aadhaar: Pending",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAadhaar) SuccessGreen else TertiaryAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total: ₹${student.totalFees.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Paid: ₹${student.paidFees.toInt()} (${paymentCount}x)",
                        fontSize = 11.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Pending: ₹${student.pendingFees.toInt()}",
                        fontSize = 11.sp,
                        color = if (student.pendingFees > 0) ErrorRed else SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCollectFee,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("subadmin_collect_fee_${student.admissionNo}")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Collect Fee", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("subadmin_view_details_${student.admissionNo}")
                ) {
                    Text("Full Details & Edit →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAdminStudentDetailsDialog(
    student: StudentEntity,
    installments: List<FeeInstallmentEntity>,
    payments: List<FeePaymentEntity>,
    viewModel: EduViewModel,
    onCollectFee: (StudentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showFullAadhaarPreview by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    // Editable fields initialized from student
    var name by remember(student) { mutableStateOf(student.name) }
    var fatherName by remember(student) { mutableStateOf(student.fatherName) }
    var motherName by remember(student) { mutableStateOf(student.motherName) }
    var contactNo by remember(student) { mutableStateOf(student.contactNo) }
    var address by remember(student) { mutableStateOf(student.address) }
    var courseName by remember(student) { mutableStateOf(student.courseName) }
    var branchName by remember(student) { mutableStateOf(student.branchName) }
    var courseDuration by remember(student) { mutableStateOf(student.courseDuration) }
    var dateOfJoining by remember(student) { mutableStateOf(student.dateOfJoining) }
    var batchName by remember(student) { mutableStateOf(student.batchName) }
    var batchTiming by remember(student) { mutableStateOf(student.batchTiming) }
    var totalFeesText by remember(student) { mutableStateOf(student.totalFees.toInt().toString()) }
    var paymentPlan by remember(student) { mutableStateOf(student.paymentPlan) }
    var installmentCountText by remember(student) { mutableStateOf(student.installmentCount.toString()) }
    var nextDueDate by remember(student) { mutableStateOf(student.nextDueDate) }
    var aadhaarPath by remember(student) { mutableStateOf(student.aadhaarCardUri) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dbCourses by viewModel.allCourses.collectAsState()
    val dbBranches by viewModel.allBranches.collectAsState()
    val durationPresets = listOf(
        "1 Month", "2 Months", "3 Months", "4 Months",
        "5 Months", "6 Months", "7 Months", "8 Months",
        "9 Months", "10 Months", "11 Months", "12 Months (1 Year)"
    )
    val paymentPlans = listOf("ONE-TIME", "MONTHLY", "PART PAYMENT", "YEARLY")
    val installmentPresets = listOf(2, 3, 4, 5, 6, 9, 12)
    val batchTimingPresets = listOf(
        "09:00 AM - 10:00 AM",
        "10:00 AM - 11:00 AM",
        "11:00 AM - 12:00 PM",
        "12:00 PM - 01:00 PM",
        "02:00 PM - 03:00 PM",
        "03:00 PM - 04:00 PM",
        "04:00 PM - 05:00 PM",
        "05:00 PM - 06:00 PM",
        "06:00 PM - 07:00 PM"
    )

    val viewModeAadhaarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateStudentAadhaar(student, uri) { savedPath ->
                    aadhaarPath = savedPath
                }
            }
        }
    )

    val editModeAadhaarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                aadhaarPath = viewModel.saveAadhaarImage(uri, student.admissionNo)
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
                .testTag("subadmin_student_details_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) "Edit Student Details" else student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Admission No: ${student.admissionNo}",
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isEditing) {
                            IconButton(
                                onClick = { showRemoveConfirm = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("subadmin_remove_student_button")
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Remove Student", tint = ErrorRed)
                            }
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("edit_student_details_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Student Details", tint = PrimaryBlue)
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_student_details_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isEditing) {
                        // View Mode
                        DetailSectionCard(title = "Student & Family Details", icon = Icons.Default.Person) {
                            DetailRow("Student Name", student.name)
                            DetailRow("Admission No", student.admissionNo)
                            DetailRow("Father's Name", student.fatherName.ifBlank { "Not Provided" })
                            DetailRow("Mother's Name", student.motherName.ifBlank { "Not Provided" })
                            DetailRow("Contact No", student.contactNo)
                            DetailRow("Address", student.address.ifBlank { "Not Provided" })
                            DetailRow("Portal User ID", student.userId)
                        }

                        DetailSectionCard(title = "Course, Branch, Batch & Timing", icon = Icons.Default.School) {
                            DetailRow("Course Name", student.courseName)
                            DetailRow("Branch Name", student.branchName, valueColor = SecondaryTeal)
                            DetailRow("Duration", student.courseDuration)
                            DetailRow("Date of Joining", student.dateOfJoining)
                            DetailRow("Batch Name", student.batchName)
                            DetailRow("Batch Timing", student.batchTiming.ifBlank { "Not Set" }, valueColor = PrimaryBlue)
                            DetailRow("Assigned Trainer", student.assignedTeacherName)
                            DetailRow("Status", student.status)
                        }

                        // Aadhaar Card Document Section
                        DetailSectionCard(title = "Aadhaar Card Document", icon = Icons.Default.CreditCard) {
                            val currentModel = resolveImageModel(aadhaarPath.ifBlank { student.aadhaarCardUri })
                            if (currentModel != null) {
                                AsyncImage(
                                    model = currentModel,
                                    contentDescription = "Aadhaar Card Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showFullAadhaarPreview = true }
                                        .testTag("aadhaar_preview_image"),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "Tap image to view full size",
                                    fontSize = 10.sp,
                                    color = PrimaryBlue
                                )
                            } else {
                                Text(
                                    text = "No Aadhaar card image uploaded yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModeAadhaarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("details_upload_aadhaar_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentModel == null) "Upload Aadhaar Card Image" else "Replace Aadhaar Card Image",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        DetailSectionCard(title = "Fees & Paying Method", icon = Icons.Default.Payments) {
                            DetailRow("Total Fees", "₹${student.totalFees.toInt()}")
                            DetailRow("Paid Fees", "₹${student.paidFees.toInt()}", valueColor = SuccessGreen)
                            DetailRow(
                                "Pending Fees",
                                "₹${student.pendingFees.toInt()}",
                                valueColor = if (student.pendingFees > 0) ErrorRed else SuccessGreen
                            )
                            DetailRow(
                                "Paying Method",
                                formatPayingMethodLabel(student.paymentPlan, student.installmentCount),
                                valueColor = PrimaryBlue
                            )
                            if (student.installmentCount > 1) {
                                val perInst = if (student.monthlyFeeAmount > 0) student.monthlyFeeAmount else (student.pendingFees / student.installmentCount)
                                DetailRow("Approx. Per Installment", "₹${perInst.roundToInt()}")
                            }
                            if (student.nextDueDate.isNotBlank()) {
                                DetailRow("Next Due Date", student.nextDueDate)
                            }
                        }

                        if (installments.isNotEmpty()) {
                            DetailSectionCard(title = "Installment Schedule (${installments.size})", icon = Icons.Default.EventNote) {
                                installments.forEach { inst ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Inst. ${inst.installmentNumber} • ${inst.monthName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Due: ${inst.dueDate}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${inst.installmentAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                inst.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (inst.status == "PAID") SuccessGreen else TertiaryAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (payments.isNotEmpty()) {
                            DetailSectionCard(title = "Payment Receipts (${payments.size})", icon = Icons.Default.Receipt) {
                                payments.forEach { pay ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${pay.receiptNo} (${pay.paymentMode})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(pay.paymentDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("₹${pay.amount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                }
                            }
                        }
                    } else {
                        // Edit Mode
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Student Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fatherName,
                            onValueChange = { fatherName = it },
                            label = { Text("Father's Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = motherName,
                            onValueChange = { motherName = it },
                            label = { Text("Mother's Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactNo,
                            onValueChange = { contactNo = it },
                            label = { Text("Contact No *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        if (dbCourses.isNotEmpty()) {
                            Text("Select Saved Course:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(dbCourses) { course ->
                                    FilterChip(
                                        selected = courseName.equals(course.courseName, ignoreCase = true),
                                        onClick = {
                                            courseName = course.courseName
                                            courseDuration = course.defaultDuration
                                        },
                                        label = { Text(course.courseName, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = { courseName = it },
                            label = { Text("Course Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (dbBranches.isNotEmpty()) {
                            Text("Select Branch:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(dbBranches) { branch ->
                                    FilterChip(
                                        selected = branchName.equals(branch.branchName, ignoreCase = true),
                                        onClick = { branchName = branch.branchName },
                                        label = { Text(branch.branchName, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Duration (1 Month - 1 Year):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(durationPresets) { preset ->
                                FilterChip(
                                    selected = courseDuration.equals(preset, ignoreCase = true),
                                    onClick = { courseDuration = preset },
                                    label = { Text(preset, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = courseDuration,
                                onValueChange = { courseDuration = it },
                                label = { Text("Duration *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dateOfJoining,
                                onValueChange = { dateOfJoining = it },
                                label = { Text("Joining Date *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = batchName,
                                onValueChange = { batchName = it },
                                label = { Text("Batch Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = batchTiming,
                                onValueChange = { batchTiming = it },
                                label = { Text("Batch Timing") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }

                        Text("1-Hour Batch Timing Slots:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(batchTimingPresets) { slot ->
                                FilterChip(
                                    selected = batchTiming.equals(slot, ignoreCase = true),
                                    onClick = { batchTiming = slot },
                                    label = { Text(slot, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = totalFeesText,
                            onValueChange = { totalFeesText = it },
                            label = { Text("Total Fees (₹) *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Text("Paying Method:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(paymentPlans) { plan ->
                                FilterChip(
                                    selected = paymentPlan == plan,
                                    onClick = {
                                        paymentPlan = plan
                                        if (plan == "ONE-TIME" || plan == "YEARLY") {
                                            installmentCountText = "1"
                                        } else if ((installmentCountText.toIntOrNull() ?: 1) <= 1) {
                                            installmentCountText = if (plan == "MONTHLY") "6" else "3"
                                        }
                                    },
                                    label = { Text(plan, fontSize = 11.sp) }
                                )
                            }
                        }

                        if (paymentPlan == "MONTHLY" || paymentPlan == "PART PAYMENT") {
                            Text("Installments (2, 3, 4, 5, 6+):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(installmentPresets) { count ->
                                    FilterChip(
                                        selected = installmentCountText.toIntOrNull() == count,
                                        onClick = { installmentCountText = count.toString() },
                                        label = { Text("$count Inst.", fontSize = 11.sp) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = installmentCountText,
                                    onValueChange = { installmentCountText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Installment Count") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = nextDueDate,
                                    onValueChange = { nextDueDate = it },
                                    label = { Text("Next Due Date") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        // Aadhaar Image edit section
                        Text("Aadhaar Card Image:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        val editModel = resolveImageModel(aadhaarPath)
                        if (editModel != null) {
                            AsyncImage(
                                model = editModel,
                                contentDescription = "Aadhaar Card Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editModeAadhaarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editModel == null) "Upload Aadhaar Image" else "Change Aadhaar Image", fontSize = 12.sp)
                            }
                            if (editModel != null) {
                                OutlinedButton(
                                    onClick = { aadhaarPath = "" },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Aadhaar Image", tint = ErrorRed)
                                }
                            }
                        }

                        if (errorMsg != null) {
                            Text(errorMsg!!, color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val parsedTotal = totalFeesText.toDoubleOrNull()
                                if (name.isBlank() || contactNo.isBlank() || courseName.isBlank() || parsedTotal == null || parsedTotal <= 0) {
                                    errorMsg = "Please enter valid Name, Contact, Course & Total Fees."
                                    return@Button
                                }
                                val instCount = when (paymentPlan) {
                                    "ONE-TIME", "YEARLY" -> 1
                                    else -> (installmentCountText.toIntOrNull() ?: 3).coerceIn(1, 36)
                                }
                                val newPending = max(0.0, parsedTotal - student.paidFees)
                                val perInst = if (instCount > 0) newPending / instCount else newPending
                                val updated = student.copy(
                                    name = name.trim(),
                                    fatherName = fatherName.trim(),
                                    motherName = motherName.trim(),
                                    contactNo = contactNo.trim(),
                                    address = address.trim(),
                                    aadhaarCardUri = aadhaarPath.trim(),
                                    courseName = courseName.trim(),
                                    branchName = branchName.trim().ifBlank { "Main Campus (Delhi)" },
                                    courseDuration = courseDuration.trim(),
                                    dateOfJoining = dateOfJoining.trim(),
                                    batchName = batchName.trim(),
                                    batchTiming = batchTiming.trim(),
                                    totalFees = parsedTotal,
                                    pendingFees = newPending,
                                    paymentPlan = paymentPlan,
                                    installmentCount = instCount,
                                    monthlyFeeAmount = perInst,
                                    nextDueDate = nextDueDate.trim()
                                )
                                viewModel.updateStudentDetails(updated) {
                                    isEditing = false
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_student_details_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (student.pendingFees > 0) {
                            Button(
                                onClick = { onCollectFee(student) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dialog_collect_fee_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Collect Fee", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            icon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ErrorRed) },
            title = { Text("Remove Student?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove ${student.name} (Adm #${student.admissionNo}) who left the institute?\n\nTheir student record, pending installments, and portal login will be removed and synced to Firebase.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeStudent(student) {
                            showRemoveConfirm = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.testTag("subadmin_confirm_remove_student_button")
                ) {
                    Text("Remove Student", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFullAadhaarPreview) {
        val fullModel = resolveImageModel(aadhaarPath.ifBlank { student.aadhaarCardUri })
        if (fullModel != null) {
            Dialog(onDismissRequest = { showFullAadhaarPreview = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aadhaar Card - ${student.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(
                                onClick = { showFullAadhaarPreview = false },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Preview")
                            }
                        }
                        AsyncImage(
                            model = fullModel,
                            contentDescription = "Full Aadhaar Card Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp, max = 420.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.weight(0.58f)
        )
    }
}
