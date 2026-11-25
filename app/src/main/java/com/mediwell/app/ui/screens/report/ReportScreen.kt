package com.mediwell.app.ui.screens.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.model.*
import com.mediwell.app.ui.components.AppTopBar
import com.mediwell.app.ui.components.ReferralBanner
import com.mediwell.app.ui.components.SeverityBadge
import com.mediwell.app.ui.theme.*

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
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh review", tint = ForestGreen)
                    }
                }
            )
        },
        containerColor = Ivory
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = White,
                contentColor = ForestGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Patient View", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Clinician View", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (val state = uiState) {
                is ReportUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ForestGreen)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Reviewing your medicines and safety details...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
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
                                tint = CriticalRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = CriticalRed,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.runAnalysisAndGenerateReport() },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
                                Text("Try Again", color = White)
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Summary Header Card
            val hasFindings = report.totalFindings > 0
            Surface(
                color = if (hasFindings) ReviewAmberBg else VerifiedGreenBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasFindings) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (hasFindings) ReviewAmber else VerifiedGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hasFindings) "${report.totalFindings} Safety Notice(s) Found"
                                   else "No Known Issues Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (hasFindings) ReviewAmber else VerifiedGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Checked ${report.totalMedicationsChecked} medicine(s) against available references.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Allergy Assessment Status Banner (always explicit: never treats blank as "no allergies")
        item {
            val allergyAssessed = report.allergiesAssessedCount > 0
            Surface(
                color = if (allergyAssessed) SageGreen else InfoBlueBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (allergyAssessed) Icons.Default.VerifiedUser else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (allergyAssessed) ForestGreen else InfoBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Allergy Check",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (allergyAssessed) ForestGreen else InfoBlue
                        )
                        Text(
                            text = report.allergyAssessmentNote,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Crisp, Structured Findings List
        if (report.findings.isEmpty()) {
            item {
                Surface(
                    color = White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No known interaction matches found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "None of your current medicines matched known interaction warnings in our reference checks. Always confirm new or changed medicines with your pharmacist or doctor.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(report.findings, key = { it.findingKey }) { finding ->
                PatientFindingItemCard(finding)
            }
        }

        // Per-Medicine Reference Information (Organized & Expandable)
        if (report.medicationDetails.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Medicine Reference Guides",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "General information from official product labels — follow your personal prescription instructions.",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            items(report.medicationDetails, key = { it.medicineName }) { medInfo ->
                MedicationLabelCard(medInfo)
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
    var expanded by remember { mutableStateOf(false) }

    val (sevBg, sevFg) = when {
        finding.severityBadge.contains("High", ignoreCase = true) -> CriticalRedBg to CriticalRed
        finding.severityBadge.contains("Warning", ignoreCase = true) -> ReviewAmberBg to ReviewAmber
        else -> InfoBlueBg to InfoBlue
    }

    val (statusBg, statusFg) = when {
        finding.confidenceBadge.contains("Verified", ignoreCase = true) -> VerifiedGreenBg to VerifiedGreen
        finding.confidenceBadge.contains("Not Assessed", ignoreCase = true) -> InfoBlueBg to InfoBlue
        else -> ReviewAmberBg to ReviewAmber
    }

    Surface(
        color = White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = sevBg) {
                    Text(
                        text = finding.severityBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sevFg
                    )
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                    Text(
                        text = finding.confidenceBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Crisp structured bullet points
            FindingBulletRow(
                label = "Issue",
                value = finding.issue.ifBlank { finding.title },
                boldValue = true
            )
            FindingBulletRow(
                label = "Medicines involved",
                value = finding.involvedMedications.joinToString(", ").ifBlank { "Not specified" }
            )
            FindingBulletRow(
                label = "Why it matters",
                value = finding.plainLanguageExplanation
            )
            FindingBulletRow(
                label = "What to do",
                value = finding.whatToDo
            )
            FindingBulletRow(
                label = "Status & source",
                value = "${finding.confidenceBadge} · ${finding.sourceLabel}"
            )

            // Expandable additional details
            if (finding.additionalDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide additional details" else "Show additional details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreen
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Ivory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = finding.additionalDetails,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindingBulletRow(
    label: String,
    value: String,
    boldValue: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ForestGreen,
            modifier = Modifier.padding(end = 6.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = if (boldValue) FontWeight.SemiBold else FontWeight.Normal,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun MedicationLabelCard(info: MedicationLabelInfo) {
    var showWarnings by remember { mutableStateOf(false) }
    var showAgeRestrictions by remember { mutableStateOf(false) }

    Surface(
        color = White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = info.medicineName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
                Surface(shape = RoundedCornerShape(6.dp), color = SageGreen) {
                    Text(
                        text = "Reference Guide",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreen
                    )
                }
            }

            // 1. General uses
            ReferenceSectionBlock(
                title = "General uses",
                body = info.purpose
            )

            // 2. Official label instructions (clearly separated from personal Rx)
            Column {
                Text(
                    text = "Official label instructions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "General reference only — always follow your personal prescription",
                    fontSize = 11.sp,
                    color = ReviewAmber,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "• How to take: ${info.administrationGuide}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "• Typical adult label range: ${info.typicalDosage}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            // 3. Expandable Precautions and warnings
            ExpandableReferenceSection(
                title = "Precautions and warnings",
                expanded = showWarnings,
                onToggle = { showWarnings = !showWarnings },
                body = info.warningsAndPrecautions,
                accentColor = CriticalRed
            )

            // 4. Expandable Important age-related restrictions
            ExpandableReferenceSection(
                title = "Important age-related restrictions",
                expanded = showAgeRestrictions,
                onToggle = { showAgeRestrictions = !showAgeRestrictions },
                body = info.ageRestrictions,
                accentColor = ForestGreen
            )

            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            // 5. Source and update information
            Text(
                text = "Source: ${info.source} · ${info.lastUpdated}",
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun ReferenceSectionBlock(title: String, body: String) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "• $body",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ExpandableReferenceSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    body: String,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = "• $body",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
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
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Generated at: ${report.generatedAt}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        // Recorded Allergies in Clinician View
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Patient Recorded Allergies (${report.recordedAllergies.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (report.recordedAllergies.isEmpty()) {
                        Text(
                            text = "No allergies recorded (Not assessed — do not assume negative).",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    } else {
                        report.recordedAllergies.forEach { allergy ->
                            Text(
                                text = "• ${allergy.allergenName} (${allergy.severity})${allergy.reaction?.let { " — $it" } ?: ""}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Medication Table with RxCUI codes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
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
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${med.dose ?: ""} ${med.frequency ?: ""}",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                        HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                    }
                }
            }
        }

        // Formal Findings
        item {
            Text(
                text = "Pharmacological & Allergy Findings (${report.findings.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(report.findings) { finding ->
            ClinicianFindingCard(finding)
        }

        item {
            ReferralBanner()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ClinicianFindingCard(finding: ClinicianFindingItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = White),
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
                text = "Mechanism / Clinical Detail:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Text(
                text = finding.mechanism,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Source: ${finding.sourceCitation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f)
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
