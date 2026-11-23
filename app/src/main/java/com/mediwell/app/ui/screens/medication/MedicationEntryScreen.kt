package com.mediwell.app.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.ui.components.AppTopBar
import com.mediwell.app.ui.theme.OutlineBorder
import com.mediwell.app.ui.theme.TealContainer
import com.mediwell.app.ui.theme.TealOnContainer
import com.mediwell.app.ui.theme.TealPrimary
import com.mediwell.app.ui.theme.VerifiedGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEntryScreen(
    viewModel: MedicationViewModel,
    onRunSafetyCheck: () -> Unit,
    onSignOut: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val form by viewModel.form.collectAsState()
    val entryState by viewModel.entryState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(entryState) {
        when (entryState) {
            is MedicationEntryState.Success -> {
                snackbarHostState.showSnackbar((entryState as MedicationEntryState.Success).message)
                viewModel.clearState()
            }
            is MedicationEntryState.Error -> {
                snackbarHostState.showSnackbar((entryState as MedicationEntryState.Error).message)
                viewModel.clearState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "My Medications",
                actions = {
                    Text(
                        text = "Sign out",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clickable { onSignOut() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Explanation
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TealContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Your Active Prescriptions",
                            style = MaterialTheme.typography.titleMedium,
                            color = TealOnContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter each drug name, dose, frequency, and route. Mediwell normalizes names via RxNorm & checks for duplicates & interactions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TealOnContainer
                        )
                    }
                }
            }

            // Quick Example Chips
            item {
                Column {
                    Text(
                        text = "Quick Clinical Prototypes:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip("Warfarin 5mg") { viewModel.setQuickFill("Warfarin", "5 mg", "Once daily evening", "Oral") }
                        PresetChip("Aspirin 75mg") { viewModel.setQuickFill("Aspirin", "75 mg", "Once daily", "Oral") }
                        PresetChip("Crocin 650mg") { viewModel.setQuickFill("Crocin", "650 mg", "Every 6 hours as needed", "Oral") }
                        PresetChip("Dolo 650mg") { viewModel.setQuickFill("Dolo", "650 mg", "Thrice daily", "Oral") }
                        PresetChip("Simvastatin 20mg") { viewModel.setQuickFill("Simvastatin", "20 mg", "At bedtime", "Oral") }
                        PresetChip("Clarithromycin 500mg") { viewModel.setQuickFill("Clarithromycin", "500 mg", "Twice daily", "Oral") }
                    }
                }
            }

            // Form Entry Card
            item {
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
                        Text(
                            text = "Manual Medication Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Medication Name Field
                        OutlinedTextField(
                            value = form.name,
                            onValueChange = { viewModel.updateName(it) },
                            label = { Text("Medication Name (Brand or Generic)") },
                            placeholder = { Text("e.g. Warfarin, Dolo, Lipitor") },
                            leadingIcon = {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = TealPrimary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dose Field
                        OutlinedTextField(
                            value = form.dose,
                            onValueChange = { viewModel.updateDose(it) },
                            label = { Text("Dose & Strength") },
                            placeholder = { Text("e.g. 500 mg, 1 tablet, 10 ml") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Frequency Dropdown
                        FrequencyDropdown(
                            selected = form.frequency,
                            onSelect = { viewModel.updateFrequency(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Route Dropdown
                        RouteDropdown(
                            selected = form.route,
                            onSelect = { viewModel.updateRoute(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add Button
                        Button(
                            onClick = { viewModel.addMedication() },
                            enabled = form.isValid && entryState !is MedicationEntryState.Normalizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            if (entryState is MedicationEntryState.Normalizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Normalizing via RxNorm...")
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to Medication List")
                            }
                        }
                    }
                }
            }

            // Active Medication List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Medications (${medications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Medication Cards
            if (medications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No medications entered yet.\nAdd at least 2 medications to check for interactions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(medications, key = { it.id }) { med ->
                    MedicationCard(
                        item = med,
                        onDelete = { viewModel.deleteMedication(med.id) }
                    )
                }
            }

            // Run Safety Check CTA
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRunSafetyCheck,
                    enabled = medications.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (medications.size >= 2) "Run Safety Review (${medications.size} Meds)" else "Review Medication Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MedicationCard(
    item: MedicationItem,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.rawName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.matchedRxcui != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "RxCUI Resolved",
                            tint = VerifiedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (item.matchedRxcui != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "RxCUI: ${item.matchedRxcui} • Ingredient: ${item.normalizedIngredient ?: item.rawName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6E787C)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove",
                    tint = Color(0xFF8C9396)
                )
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color = Color(0xFFE9F1F3), shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TealPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("Once daily", "Twice daily", "Thrice daily", "Four times daily", "Every 6 hours as needed", "Once weekly", "As directed")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Frequency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteDropdown(selected: String, onSelect: (String) -> Unit) {
    val routes = listOf("Oral", "Inhalation", "Topical", "Sublingual", "Subcutaneous", "Intramuscular", "Ophthalmic")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Route") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            routes.forEach { route ->
                DropdownMenuItem(
                    text = { Text(route) },
                    onClick = {
                        onSelect(route)
                        expanded = false
                    }
                )
            }
        }
    }
}
