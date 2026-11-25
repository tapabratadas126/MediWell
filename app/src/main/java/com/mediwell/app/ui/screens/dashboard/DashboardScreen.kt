package com.mediwell.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onUploadRx: () -> Unit,
    onViewProfile: () -> Unit,
    onViewReport: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Ivory,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MEDCHCK",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, "Sign out", tint = TextTertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ForestGreen)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Welcome section ───────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ForestGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Welcome back",
                        color = White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                    Text(
                        state.displayName.ifBlank { "Patient" },
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your medication safety dashboard",
                        color = White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${state.activeMedicationCount}",
                    label    = "Active Medicines",
                    icon     = Icons.Default.Medication,
                    iconTint = ForestGreen,
                    bgColor  = SageGreen
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${state.attentionItemCount}",
                    label    = "Needs Attention",
                    icon     = Icons.Default.Warning,
                    iconTint = ReviewAmber,
                    bgColor  = ReviewAmberBg
                )
            }

            // ── Profile completion ────────────────────────────────────────────
            if (!state.profileComplete) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningOrangeBg,
                    modifier = Modifier.fillMaxWidth().clickable { onViewProfile() }
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            null,
                            tint = WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Complete your health profile",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 13.sp,
                                color      = WarningOrange
                            )
                            Text(
                                "Adding allergies and conditions improves your safety analysis",
                                fontSize = 11.sp,
                                color    = TextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = WarningOrange)
                    }
                }
            }

            // ── Quick actions ─────────────────────────────────────────────────
            Text(
                "Quick Actions",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.DocumentScanner,
                    label     = "Upload Prescription",
                    onClick   = onUploadRx
                )
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.Person,
                    label     = "Health Profile",
                    onClick   = onViewProfile
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.Assessment,
                    label     = "View Reports",
                    onClick   = onViewReport
                )
                QuickActionCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.Medication,
                    label     = "My Medicines",
                    onClick   = { /* navigated via bottom nav */ }
                )
            }

            // ── Disclaimer ────────────────────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(10.dp),
                color  = SageGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "⚕ Always consult a qualified pharmacist or doctor before changing or stopping any medication.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 11.sp,
                    color    = ForestGreen,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color
) {
    Surface(shape = RoundedCornerShape(14.dp), color = bgColor, modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary)
            Text(label, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = White,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SageGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = ForestGreen, modifier = Modifier.size(22.dp))
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}
