package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun ManageBranchesDialog(
    viewModel: EduViewModel,
    onBranchAdded: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val branches by viewModel.allBranches.collectAsState()

    var newBranchName by remember { mutableStateOf("") }
    var branchAddress by remember { mutableStateOf("") }
    var branchContact by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
                .testTag("manage_branches_dialog"),
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
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = "Manage Branches",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Manage Academy Branches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add or manage branch names for staff & students",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_manage_branches_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Add New Branch Form
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
                            text = "Add New Branch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        OutlinedTextField(
                            value = newBranchName,
                            onValueChange = {
                                newBranchName = it
                                errorMsg = null
                            },
                            label = { Text("Branch Name *") },
                            placeholder = { Text("e.g. Laxmi Nagar Branch / West Delhi") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_branch_name"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) }
                        )
                        OutlinedTextField(
                            value = branchAddress,
                            onValueChange = { branchAddress = it },
                            label = { Text("Branch Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_branch_address"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = branchContact,
                            onValueChange = { branchContact = it },
                            label = { Text("Branch Contact No") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_branch_contact"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )

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
                                val trimmed = newBranchName.trim()
                                if (trimmed.isBlank()) {
                                    errorMsg = "Please enter a branch name."
                                    return@Button
                                }
                                viewModel.addBranch(
                                    branchName = trimmed,
                                    address = branchAddress,
                                    contactNo = branchContact
                                ) { success, msg ->
                                    if (success) {
                                        newBranchName = ""
                                        branchAddress = ""
                                        branchContact = ""
                                        errorMsg = null
                                        onBranchAdded?.invoke(trimmed)
                                    } else {
                                        errorMsg = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_add_branch_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Branch Name", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Available Branches (${branches.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(branches, key = { it.branchName }) { branch ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("branch_item_${branch.branchName}"),
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
                                        text = branch.branchName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    val sub = listOfNotNull(
                                        branch.address.takeIf { it.isNotBlank() },
                                        branch.contactNo.takeIf { it.isNotBlank() },
                                        "By ${branch.addedBy}"
                                    ).joinToString(" • ")
                                    Text(
                                        text = sub,
                                        fontSize = 11.sp,
                                        color = SecondaryTeal
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteBranch(branch.branchName) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_branch_${branch.branchName}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete ${branch.branchName}",
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
