package com.mediwell.app.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.model.ClinicianFindingItem
import com.mediwell.app.data.model.ClinicianMedicationItem
import com.mediwell.app.data.model.ClinicianViewReport
import com.mediwell.app.data.model.ConfidenceLevel
import com.mediwell.app.data.model.FindingSeverity
import com.mediwell.app.data.model.PatientFindingCard
import com.mediwell.app.data.model.PatientViewReport
import com.mediwell.app.ui.components.AppTopBar
import com.mediwell.app.ui.components.ConfidenceBadge
import com.mediwell.app.ui.components.ReferralBanner
import com.mediwell.app.ui.components.SeverityBadge
import com.mediwell.app.ui.theme.CriticalRed
import com.mediwell.app.ui.theme.TealContainer
import com.mediwell.app.ui.theme.TealOnContainer
import com.mediwell.app.ui.theme.TealPrimary
import com.mediwell.app.ui.theme.VerifiedGreen

@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedViewTab.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Safety Report",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { viewModel.runAnalysisAndGenerateReport() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-analyze")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // View Switcher (Patient View vs Clinician View)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Patient View (Plain Language)", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Clinician View (Codes & Citations)", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (val state = uiState) {
                is ReportUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TealPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Evaluating drug interactions & normalizing findings...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                is ReportUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.runAnalysisAndGenerateReport() }) {
                                Text("Retry Analysis")
                            }
                        }
                    }
                }
                is ReportUiState.Ready -> {
                    if (selectedTab == 0) {
                        PatientViewContent(report = state.patientView)
                    } else {
                        ClinicianViewContent(report = state.clinicianView)
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientViewContent(report: PatientViewReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Summary Header Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (report.totalFindings == 0) Color(0xFFE8F5E9) else TealContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (report.totalFindings == 0) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (report.totalFindings == 0) VerifiedGreen else TealOnContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (report.totalFindings == 0) "No Adverse Interactions Detected" else "${report.totalFindings} Medication Safety Notice(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (report.totalFindings == 0) VerifiedGreen else TealOnContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Checked ${report.totalMedicationsChecked} medications across verified clinical databases.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (report.totalFindings == 0) Color(0xFF2E7D32) else TealOnContainer
                        )
                    }
                }
            }
        }

        // List of Findings in Plain Language
        if (report.findings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "All clear in checked databases",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "None of your current medication pairs matched known high-risk interactions in the curated database or DDInter dataset.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(report.findings, key = { it.findingKey }) { finding ->
                PatientFindingItemCard(finding)
            }
        }

        // Fixed Non-Editable Referral Banner
        item {
            ReferralBanner()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PatientFindingItemCard(finding: PatientFindingCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = finding.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val conf = if (finding.confidenceBadge.contains("Verified", ignoreCase = true)) {
                    ConfidenceLevel.VERIFIED
                } else {
                    ConfidenceLevel.NEEDS_REVIEW
                }
                ConfidenceBadge(conf)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rephrased Plain Language Sentence
            Text(
                text = finding.plainLanguageExplanation,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1E292E),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFFEDF2F4), thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Involved: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = finding.involvedMedications.joinToString(" + "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TealPrimary
                )
            }
        }
    }
}

@Composable
private fun ClinicianViewContent(report: ClinicianViewReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Clinical Overview & Ontologies",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Generated at: ${report.generatedAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Medication Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Medication Concept Mappings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    report.medications.forEach { med ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = med.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Active: ${med.activeIngredients ?: "Not mapped"} • RxCUI: ${med.rxcui ?: "Unmatched"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555E62)
                                )
                            }
                            Text(
                                text = "${med.dose ?: ""} ${med.frequency ?: ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF0F3F4), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Formal Findings
        item {
            Text(
                text = "Pharmacological Findings (${report.findings.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(report.findings) { finding ->
            ClinicianFindingCard(finding)
        }

        // Fixed Non-Editable Referral Line
        item {
            ReferralBanner()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ClinicianFindingCard(finding: ClinicianFindingItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${finding.drugA} ↔ ${finding.drugB ?: "N/A"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val sev = FindingSeverity.from(finding.severity)
                SeverityBadge(sev)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Mechanism / Pathology:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = finding.mechanism,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2C3E44)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Source: ${finding.sourceCitation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7A858A)
                )
                Text(
                    text = "Confidence: ${finding.confidence}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (finding.confidence == "verified") VerifiedGreen else CriticalRed
                )
            }
        }
    }
}
