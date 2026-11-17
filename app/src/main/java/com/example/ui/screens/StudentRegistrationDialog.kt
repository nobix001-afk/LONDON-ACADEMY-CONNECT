package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.EduViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRegistrationDialog(
    viewModel: EduViewModel,
    onDismiss: () -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState()
    val trainers = employees.filter {
        it.canManageStudents ||
            it.department.contains("Trainer", ignoreCase = true) ||
            it.department.contains("Teach", ignoreCase = true)
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())
    val defaultNextDue = remember {
        Calendar.getInstance().apply { add(Calendar.MONTH, 1) }.let { sdf.format(it.time) }
    }

    // 1. Student & Family Details
    var name by remember { mutableStateOf("") }
    var admissionNo by remember { mutableStateOf("STU-2026-${(100..999).random()}") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }

    val aadhaarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                aadhaarUri = uri
            }
        }
    )

    // 2. Course & Joining Details
    val dbCourses by viewModel.allCourses.collectAsState()
    val baseCourses = remember(dbCourses) {
        if (dbCourses.isNotEmpty()) {
            dbCourses.map { it.courseName }
        } else {
            listOf(
                "Full Stack Web Development",
                "Digital Marketing Masterclass",
                "Video Editing & Animation Pro",
                "Computer Fundamentals & Office Tools",
                "Data Analytics with Python",
                "UI/UX Design Masterclass"
            )
        }
    }
    val courseOptions = remember(baseCourses) { baseCourses + "Other / Custom Course" }
    var selectedCourseOption by remember { mutableStateOf(courseOptions.first()) }
    var customCourseName by remember { mutableStateOf("") }
    var courseMenuExpanded by remember { mutableStateOf(false) }
    var showManageCourses by remember { mutableStateOf(false) }

    LaunchedEffect(baseCourses) {
        if (selectedCourseOption != "Other / Custom Course" && selectedCourseOption !in baseCourses && baseCourses.isNotEmpty()) {
            selectedCourseOption = baseCourses.first()
        }
    }

    val dbBranches by viewModel.allBranches.collectAsState()
    val baseBranches = remember(dbBranches) {
        if (dbBranches.isNotEmpty()) {
            dbBranches.map { it.branchName }
        } else {
            listOf("Main Campus (Delhi)", "South Extension Branch", "Noida Branch", "Gurugram Branch")
        }
    }
    val branchOptions = remember(baseBranches) { baseBranches + "Other / Custom Branch" }
    var selectedBranchOption by remember { mutableStateOf(branchOptions.first()) }
    var customBranchName by remember { mutableStateOf("") }
    var branchMenuExpanded by remember { mutableStateOf(false) }
    var showManageBranches by remember { mutableStateOf(false) }

    LaunchedEffect(baseBranches) {
        if (selectedBranchOption != "Other / Custom Branch" && selectedBranchOption !in baseBranches && baseBranches.isNotEmpty()) {
            selectedBranchOption = baseBranches.first()
        }
    }

    val durationPresets = listOf(
        "1 Month", "2 Months", "3 Months", "4 Months",
        "5 Months", "6 Months", "7 Months", "8 Months",
        "9 Months", "10 Months", "11 Months", "12 Months (1 Year)"
    )
    var duration by remember { mutableStateOf("6 Months") }
    var doj by remember { mutableStateOf(today) }
    var batchName by remember { mutableStateOf("Morning Batch A") }
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
    var batchTiming by remember { mutableStateOf(batchTimingPresets[0]) }

    var selectedTrainer by remember { mutableStateOf(trainers.firstOrNull() ?: employees.firstOrNull()) }
    var trainerMenuExpanded by remember { mutableStateOf(false) }

    // 3. Fees & Paying Method
    var totalFeesText by remember { mutableStateOf("50000") }
    var initialPaidText by remember { mutableStateOf("10000") }

    val paymentPlans = listOf("ONE-TIME", "MONTHLY", "PART PAYMENT", "YEARLY")
    var selectedPlan by remember { mutableStateOf("PART PAYMENT") }
    val installmentPresets = listOf(2, 3, 4, 5, 6, 9, 12)
    var installmentCountText by remember { mutableStateOf("3") }
    var nextDueDate by remember { mutableStateOf(defaultNextDue) }
    var installmentAmountText by remember { mutableStateOf("") }

    // 4. Student Login Credentials
    var studentUserId by remember { mutableStateOf("") }
    var studentPassword by remember { mutableStateOf("stud123") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(admissionNo) {
        if (studentUserId.isEmpty() && admissionNo.isNotEmpty()) {
            studentUserId = admissionNo.lowercase().replace("-", "")
        }
    }

    val totalFees = totalFeesText.toDoubleOrNull() ?: 0.0
    val initialPaid = initialPaidText.toDoubleOrNull() ?: 0.0
    val pendingFees = max(0.0, totalFees - initialPaid)
    val effectiveInstallmentCount = when (selectedPlan) {
        "ONE-TIME", "YEARLY" -> 1
        else -> (installmentCountText.toIntOrNull() ?: 3).coerceIn(1, 36)
    }

    LaunchedEffect(pendingFees, effectiveInstallmentCount, selectedPlan) {
        if (selectedPlan == "MONTHLY" || selectedPlan == "PART PAYMENT") {
            val perInst = if (effectiveInstallmentCount > 0) (pendingFees / effectiveInstallmentCount) else pendingFees
            installmentAmountText = perInst.roundToInt().toString()
        } else {
            installmentAmountText = pendingFees.roundToInt().toString()
        }
    }

    val effectiveCourseName = if (selectedCourseOption == "Other / Custom Course") {
        customCourseName.trim()
    } else {
        selectedCourseOption
    }
    val effectiveBranchName = if (selectedBranchOption == "Other / Custom Branch") {
        customBranchName.trim().ifBlank { "Main Campus (Delhi)" }
    } else {
        selectedBranchOption
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(4.dp)
                .testTag("student_registration_dialog"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Student",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "New Student Admission Form",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Complete student, course, family & fee details",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_student_registration_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Personal & Family Details
                    Text("1. Student & Family Details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Full Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_name"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = admissionNo,
                        onValueChange = { admissionNo = it },
                        label = { Text("Admission Number (Unique) *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_admission_no"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_father_name"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("Mother's Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mother_name"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Face3, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact Number *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_contact"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_address"),
                        singleLine = false,
                        maxLines = 2,
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )

                    // Aadhaar Card Image Upload
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Aadhaar Card Image",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                if (aadhaarUri != null) {
                                    Text(
                                        text = "Selected",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }

                            if (aadhaarUri != null) {
                                AsyncImage(
                                    model = aadhaarUri,
                                    contentDescription = "Selected Aadhaar Card Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
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
                                        aadhaarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("upload_aadhaar_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (aadhaarUri == null) "Upload Aadhaar Image" else "Change Image",
                                        fontSize = 12.sp
                                    )
                                }
                                if (aadhaarUri != null) {
                                    OutlinedButton(
                                        onClick = { aadhaarUri = null },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("remove_aadhaar_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Aadhaar Image", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 2. Course Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2. Course & Joining Details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                        TextButton(
                            onClick = { showManageCourses = true },
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("reg_add_course_button")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Course", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = courseMenuExpanded,
                        onExpandedChange = { courseMenuExpanded = !courseMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCourseOption,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Course Name *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_course_name")
                        )
                        ExposedDropdownMenu(
                            expanded = courseMenuExpanded,
                            onDismissRequest = { courseMenuExpanded = false }
                        ) {
                            courseOptions.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course) },
                                    onClick = {
                                        selectedCourseOption = course
                                        courseMenuExpanded = false
                                        dbCourses.firstOrNull { it.courseName == course }?.let { entity ->
                                            duration = entity.defaultDuration
                                            totalFeesText = entity.defaultFees.toInt().toString()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCourseOption == "Other / Custom Course") {
                        OutlinedTextField(
                            value = customCourseName,
                            onValueChange = { customCourseName = it },
                            label = { Text("Enter Custom Course Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_custom_course_name"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                        )
                    }

                    // Branch Picker + Add Branch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Branch Name *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { showManageBranches = true },
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("reg_add_branch_button")
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Branch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = branchMenuExpanded,
                        onExpandedChange = { branchMenuExpanded = !branchMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBranchOption,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Branch *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_branch_name")
                        )
                        ExposedDropdownMenu(
                            expanded = branchMenuExpanded,
                            onDismissRequest = { branchMenuExpanded = false }
                        ) {
                            branchOptions.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text(branch) },
                                    onClick = {
                                        selectedBranchOption = branch
                                        branchMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedBranchOption == "Other / Custom Branch") {
                        OutlinedTextField(
                            value = customBranchName,
                            onValueChange = { customBranchName = it },
                            label = { Text("Enter Custom Branch Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_custom_branch_name"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) }
                        )
                    }

                    // Duration quick presets + input
                    Text("Course Duration (1 Month - 1 Year):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(durationPresets) { preset ->
                            FilterChip(
                                selected = duration.equals(preset, ignoreCase = true),
                                onClick = { duration = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("Duration *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_course_duration"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = doj,
                            onValueChange = { doj = it },
                            label = { Text("Joining Date *") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_date_of_joining"),
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
                            placeholder = { Text("e.g. 09:00 AM - 10:00 AM") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_batch_timing"),
                            singleLine = true
                        )
                    }

                    Text("1-Hour Batch Timing Slots:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(batchTimingPresets) { slot ->
                            FilterChip(
                                selected = batchTiming.equals(slot, ignoreCase = true),
                                onClick = { batchTiming = slot },
                                label = { Text(slot, fontSize = 11.sp) },
                                modifier = Modifier.testTag("batch_timing_chip_${slot.take(5).replace(":", "")}")
                            )
                        }
                    }

                    // Assigned Trainer
                    ExposedDropdownMenuBox(
                        expanded = trainerMenuExpanded,
                        onExpandedChange = { trainerMenuExpanded = !trainerMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedTrainer?.let { "${it.name} (${it.department})" } ?: "Select Trainer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Teacher / Trainer *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trainerMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = trainerMenuExpanded,
                            onDismissRequest = { trainerMenuExpanded = false }
                        ) {
                            (if (trainers.isNotEmpty()) trainers else employees).forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.name} - ${emp.department}") },
                                    onClick = {
                                        selectedTrainer = emp
                                        trainerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 3. Fees & Paying Method
                    Text("3. Fees & Paying Method", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalFeesText,
                            onValueChange = { totalFeesText = it },
                            label = { Text("Total Fees (₹) *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_total_fees"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = initialPaidText,
                            onValueChange = { initialPaidText = it },
                            label = { Text("Initial Paid (₹)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_initial_paid"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Paying Method selector
                    Text("Paying Method:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(paymentPlans) { plan ->
                            FilterChip(
                                selected = selectedPlan == plan,
                                onClick = {
                                    selectedPlan = plan
                                    if (plan == "ONE-TIME" || plan == "YEARLY") {
                                        installmentCountText = "1"
                                    } else if (plan == "MONTHLY" && (installmentCountText.toIntOrNull() ?: 1) <= 1) {
                                        installmentCountText = "6"
                                    } else if (plan == "PART PAYMENT" && (installmentCountText.toIntOrNull() ?: 1) <= 1) {
                                        installmentCountText = "3"
                                    }
                                },
                                label = { Text(plan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("plan_chip_${plan.lowercase().replace(" ", "_").replace("-", "_")}")
                            )
                        }
                    }

                    if (selectedPlan == "MONTHLY" || selectedPlan == "PART PAYMENT") {
                        Text(
                            text = if (selectedPlan == "PART PAYMENT") "Part Payment Installments (2, 3, 4, 5, 6+):" else "Monthly Installment Count:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(installmentPresets) { count ->
                                FilterChip(
                                    selected = effectiveInstallmentCount == count,
                                    onClick = { installmentCountText = count.toString() },
                                    label = { Text("$count Inst.", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("installment_chip_$count")
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = installmentCountText,
                                onValueChange = { installmentCountText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("No. of Installments") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_installment_count"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = installmentAmountText,
                                onValueChange = { installmentAmountText = it },
                                label = { Text("Per Installment (₹)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = nextDueDate,
                            onValueChange = { nextDueDate = it },
                            label = { Text("First / Next Installment Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) }
                        )
                    }

                    // Auto calculated pending fees & plan summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pending Balance Fees:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("₹${pendingFees.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                            }
                            Text(
                                text = when (selectedPlan) {
                                    "ONE-TIME" -> "Paying Method: ONE-TIME Payment"
                                    "YEARLY" -> "Paying Method: YEARLY Payment"
                                    else -> "Paying Method: $selectedPlan ($effectiveInstallmentCount Installments • ₹${installmentAmountText.ifBlank { "0" }} / inst.)"
                                },
                                fontSize = 11.sp,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 4. Student Login Credentials
                    Text("4. Student Login Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)

                    OutlinedTextField(
                        value = studentUserId,
                        onValueChange = { studentUserId = it },
                        label = { Text("Student User ID *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_userid"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = studentPassword,
                        onValueChange = { studentPassword = it },
                        label = { Text("Password *") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || contact.isBlank() || admissionNo.isBlank() || studentUserId.isBlank() || effectiveCourseName.isBlank() || duration.isBlank() || doj.isBlank()) {
                            errorMessage = "Please fill all required fields marked with *"
                            return@Button
                        }
                        if (totalFees <= 0.0) {
                            errorMessage = "Total fees must be greater than 0"
                            return@Button
                        }
                        val trainer = selectedTrainer ?: trainers.firstOrNull() ?: employees.firstOrNull()
                        if (trainer == null) {
                            errorMessage = "No trainer available to assign"
                            return@Button
                        }
                        errorMessage = null
                        if (selectedCourseOption == "Other / Custom Course" && effectiveCourseName.isNotBlank()) {
                            viewModel.addCourse(effectiveCourseName, duration, totalFees)
                        }
                        if (selectedBranchOption == "Other / Custom Branch" && customBranchName.isNotBlank()) {
                            viewModel.addBranch(effectiveBranchName)
                        }
                        val savedAadhaarPath = aadhaarUri?.let { viewModel.saveAadhaarImage(it, admissionNo) } ?: ""
                        viewModel.registerStudent(
                            name = name,
                            contact = contact,
                            address = address,
                            fatherName = fatherName,
                            motherName = motherName,
                            aadhaarCardUri = savedAadhaarPath,
                            admissionNo = admissionNo,
                            courseName = effectiveCourseName,
                            branchName = effectiveBranchName,
                            batchName = batchName,
                            batchTiming = batchTiming,
                            doj = doj,
                            duration = duration,
                            totalFees = totalFees,
                            paidFees = initialPaid,
                            paymentPlan = selectedPlan,
                            installmentCount = effectiveInstallmentCount,
                            assignedTeacherId = trainer.employeeId,
                            assignedTeacherName = trainer.name,
                            userId = studentUserId,
                            userPass = studentPassword,
                            monthlyAmount = installmentAmountText.toDoubleOrNull() ?: 0.0,
                            nextDueDate = nextDueDate
                        ) { success, msg ->
                            if (success) {
                                onDismiss()
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_student_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save & Add Student", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showManageCourses) {
        ManageCoursesDialog(
            viewModel = viewModel,
            onCourseAdded = { addedName ->
                selectedCourseOption = addedName
                showManageCourses = false
            },
            onDismiss = { showManageCourses = false }
        )
    }

    if (showManageBranches) {
        ManageBranchesDialog(
            viewModel = viewModel,
            onBranchAdded = { addedBranch ->
                selectedBranchOption = addedBranch
                showManageBranches = false
            },
            onDismiss = { showManageBranches = false }
        )
    }
}
