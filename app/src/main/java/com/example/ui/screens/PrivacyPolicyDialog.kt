package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PrimaryBlue

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Data Protection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Camera & Live Selfie Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "London Academy Connect uses device camera access exclusively for live selfie verification during daily attendance marking. For document uploads (such as Aadhaar images), the zero-permission Android Photo Picker is used without requesting broad device storage access. Camera access is never triggered in the background.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. Location & GPS Geofence Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Foreground location (GPS) is queried solely at the instant a user marks attendance to confirm physical presence within the configured campus perimeter. Background location tracking is never enabled or requested.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Google Sign-In & Cloud Firestore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Authentication is secured via Google Sign-In (Credential Manager) and Firebase Authentication. Institutional records (branches, courses, staff, students, attendance, fees, and notices) are isolated per authenticated user account in Google Cloud Firestore and protected by zero-trust security rules.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "4. Role Isolation & Audit Governance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Personal contact details, fee transactions, and admission records follow strict role-based separation across Admin, Sub-Admin, Teacher, Support Staff, and Student portals. Immutable audit logs record administrative updates for accountability.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand & Agree")
                }
            }
        }
    }
}
