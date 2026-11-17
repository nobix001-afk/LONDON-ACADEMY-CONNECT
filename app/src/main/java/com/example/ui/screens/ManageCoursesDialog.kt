package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.viewmodel.EduViewModel

@Composable
fun ManageCoursesDialog(
    viewModel: EduViewModel,
    onCourseAdded: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val courses by viewModel.allCourses.collectAsState()

    var newCourseName by remember { mutableStateOf("") }
    var defaultDuration by remember { mutableStateOf("6 Months") }
    var defaultFeesText by remember { mutableStateOf("35000") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val durationPresets = listOf(
        "1 Month", "2 Months", "3 Months", "4 Months",
        "5 Months", "6 Months", "7 Months", "8 Months",
        "9 Months", "10 Months", "11 Months", "12 Months (1 Year)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
                .testTag("manage_courses_dialog"),
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Manage Courses",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Manage Academy Courses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add or manage course names for admissions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_manage_courses_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Add New Course Form
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Add New Course",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        OutlinedTextField(
                            value = newCourseName,
                            onValueChange = {
                                newCourseName = it
                                errorMsg = null
                            },
                            label = { Text("Course Name *") },
                            placeholder = { Text("e.g. Tally Prime & GST / Spoken English") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_course_name"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) }
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(durationPresets) { preset ->
                                FilterChip(
                                    selected = defaultDuration.equals(preset, ignoreCase = true),
                                    onClick = { defaultDuration = preset },
                                    label = { Text(preset, fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = defaultDuration,
                                onValueChange = { defaultDuration = it },
                                label = { Text("Default Duration") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_new_course_duration"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = defaultFeesText,
                                onValueChange = { defaultFeesText = it },
                                label = { Text("Default Fees (₹)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_new_course_fees"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        if (errorMsg != null) {
                            Text(
                                text = errorMsg!!,
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                val trimmed = newCourseName.trim()
                                if (trimmed.isBlank()) {
                                    errorMsg = "Please enter a course name."
                                    return@Button
                                }
                                val fees = defaultFeesText.toDoubleOrNull() ?: 35000.0
                                viewModel.addCourse(
                                    courseName = trimmed,
                                    defaultDuration = defaultDuration,
                                    defaultFees = fees
                                ) { success, msg ->
                                    if (success) {
                                        newCourseName = ""
                                        errorMsg = null
                                        onCourseAdded?.invoke(trimmed)
                                    } else {
                                        errorMsg = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_add_course_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Course Name", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Available Courses (${courses.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(courses, key = { it.courseName }) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("course_item_${course.courseName}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.courseName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${course.defaultDuration} • ₹${course.defaultFees.toInt()} • By ${course.addedBy}",
                                        fontSize = 11.sp,
                                        color = SecondaryTeal
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCourse(course.courseName) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_course_${course.courseName}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete ${course.courseName}",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
