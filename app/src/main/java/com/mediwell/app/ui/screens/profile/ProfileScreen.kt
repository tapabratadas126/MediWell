package com.mediwell.app.ui.screens.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.model.AllergyRecord
import com.mediwell.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val profile   by viewModel.profile.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var editingAllergy by remember { mutableStateOf<AllergyRecord?>(null) }
    var showAllergyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is ProfileSaveState.Success -> snackbarHostState.showSnackbar(s.message)
            is ProfileSaveState.Error   -> snackbarHostState.showSnackbar(s.message)
            else -> Unit
        }
        if (saveState !is ProfileSaveState.Idle) viewModel.clearSaveState()
    }

    if (showAllergyDialog) {
        AllergyEditorDialog(
            initial = editingAllergy,
            onDismiss = {
                showAllergyDialog = false
                editingAllergy = null
            },
            onSave = { name, reaction, severity ->
                viewModel.saveAllergy(editingAllergy?.id, name, reaction, severity)
                showAllergyDialog = false
                editingAllergy = null
            }
        )
    }

    Scaffold(
        containerColor = Ivory,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Health Profile", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator(color = ForestGreen)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Personal information section ──────────────────────────────────
            ProfileSection(title = "Personal Information", icon = Icons.Default.Person) {
                ProfileTextField(
                    label   = "Full Name",
                    value   = profile.fullName ?: "",
                    onValueChange = viewModel::updateFullName
                )
                Spacer(Modifier.height(10.dp))
                ProfileTextField(
                    label   = "Phone Number",
                    value   = profile.phone ?: "",
                    onValueChange = viewModel::updatePhone
                )
                Spacer(Modifier.height(10.dp))
                ProfileTextField(
                    label   = "Date of Birth (YYYY-MM-DD)",
                    value   = profile.dateOfBirth ?: "",
                    onValueChange = viewModel::updateDateOfBirth
                )
                Spacer(Modifier.height(10.dp))
                ProfileTextField(
                    label   = "City / District",
                    value   = profile.city ?: "",
                    onValueChange = viewModel::updateCity
                )
            }

            // ── Known Allergies (Saved directly & checked in Safety Reviews) ──
            ProfileSection(title = "Known Allergies", icon = Icons.Default.WarningAmber) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Saved allergies are automatically checked during every medication safety review.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            editingAllergy = null
                            showAllergyDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SageGreen,
                            contentColor = ForestGreen
                        )
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (allergies.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Ivory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Not provided — no allergies recorded yet. If you have any medicine or ingredient allergies, tap Add above.",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allergies.forEach { record ->
                            AllergyItemRow(
                                record = record,
                                onEdit = {
                                    editingAllergy = record
                                    showAllergyDialog = true
                                },
                                onDelete = { viewModel.deleteAllergy(record) }
                            )
                        }
                    }
                }
            }

            // ── Health information section ─────────────────────────────────────
            ProfileSection(title = "Health Information", icon = Icons.Default.HealthAndSafety) {
                ChipListEditor(
                    sectionLabel = "Medical Conditions",
                    notKnownHint = "Tap + to add a condition. Leave empty only if genuinely not known.",
                    items        = profile.medicalConditions,
                    onAdd        = viewModel::addCondition,
                    onRemove     = viewModel::removeCondition
                )
                Spacer(Modifier.height(14.dp))
                ChipListEditor(
                    sectionLabel = "Kidney-Related Conditions",
                    notKnownHint = "Leave empty only if genuinely not known.",
                    items        = profile.kidneyConditions,
                    onAdd        = viewModel::addKidneyCondition,
                    onRemove     = viewModel::removeKidneyCondition
                )
                Spacer(Modifier.height(14.dp))
                ChipListEditor(
                    sectionLabel = "Liver-Related Conditions",
                    notKnownHint = "Leave empty only if genuinely not known.",
                    items        = profile.liverConditions,
                    onAdd        = viewModel::addLiverCondition,
                    onRemove     = viewModel::removeLiverCondition
                )
                Spacer(Modifier.height(14.dp))
                ChipListEditor(
                    sectionLabel = "Other Current Medicines",
                    notKnownHint = "List any medicines not entered in the prescription checker.",
                    items        = profile.otherMedicines,
                    onAdd        = viewModel::addOtherMedicine,
                    onRemove     = viewModel::removeOtherMedicine
                )
            }

            // ── Privacy & consent section ─────────────────────────────────────
            ProfileSection(title = "Privacy & Consent", icon = Icons.Default.Lock) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Allow health data use for safety analysis",
                            fontWeight = FontWeight.Medium,
                            fontSize   = 13.sp,
                            color      = TextPrimary
                        )
                        Text(
                            "Your data is used only to generate your personal medication safety report.",
                            fontSize = 11.sp,
                            color    = TextTertiary
                        )
                    }
                    Switch(
                        checked  = profile.consentDataUse,
                        onCheckedChange = viewModel::updateConsentDataUse,
                        colors   = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = ForestGreen)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Allow sharing with a healthcare professional",
                            fontWeight = FontWeight.Medium,
                            fontSize   = 13.sp,
                            color      = TextPrimary
                        )
                        Text(
                            "Only you can initiate sharing. No data is shared without your explicit action.",
                            fontSize = 11.sp,
                            color    = TextTertiary
                        )
                    }
                    Switch(
                        checked  = profile.consentShareProfessional,
                        onCheckedChange = viewModel::updateConsentShare,
                        colors   = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = ForestGreen)
                    )
                }
            }

            // ── Not-provided notice ───────────────────────────────────────────
            Surface(shape = RoundedCornerShape(10.dp), color = InfoBlueBg, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "ℹ️  Unanswered fields are shown as \"Not provided\". They are never interpreted as negative medical history.",
                    modifier  = Modifier.padding(14.dp),
                    fontSize  = 11.sp,
                    color     = InfoBlue,
                    lineHeight = 15.sp
                )
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled  = saveState !is ProfileSaveState.Saving,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                if (saveState is ProfileSaveState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Text("Save Profile", color = White, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AllergyItemRow(
    record: AllergyRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SageGreen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.allergenName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ForestGreen
                    )
                    if (record.severity.isNotBlank() && record.severity != "Not specified") {
                        Spacer(Modifier.width(8.dp))
                        val (bg, fg) = when (record.severity.lowercase()) {
                            "severe"   -> CriticalRedBg to CriticalRed
                            "moderate" -> ReviewAmberBg to ReviewAmber
                            else       -> White to ForestGreen
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = bg) {
                            Text(
                                record.severity,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = fg
                            )
                        }
                    }
                }
                if (!record.reaction.isNullOrBlank()) {
                    Text(
                        "Reaction: ${record.reaction}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Edit allergy", tint = ForestGreen, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, "Delete allergy", tint = CriticalRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllergyEditorDialog(
    initial: AllergyRecord?,
    onDismiss: () -> Unit,
    onSave: (name: String, reaction: String, severity: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.allergenName ?: "") }
    var reaction by remember { mutableStateOf(initial?.reaction ?: "") }
    var severity by remember { mutableStateOf(initial?.severity ?: "Not specified") }
    val severityOptions = listOf("Mild", "Moderate", "Severe", "Not specified")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                if (initial == null) "Add Allergy" else "Edit Allergy",
                fontWeight = FontWeight.SemiBold,
                color = ForestGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Allergen / Medicine Name *") },
                    placeholder = { Text("e.g., Penicillin, Sulfa, Ibuprofen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reaction,
                    onValueChange = { reaction = it },
                    label = { Text("Reported Reaction (optional)") },
                    placeholder = { Text("e.g., Rash, hives, swelling") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Severity", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    severityOptions.forEach { opt ->
                        FilterChip(
                            selected = severity == opt,
                            onClick = { severity = opt },
                            label = { Text(opt, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, reaction, severity) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Save", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ForestGreen)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForestGreen,
            focusedLabelColor  = ForestGreen
        )
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChipListEditor(
    sectionLabel: String,
    notKnownHint: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Text(sectionLabel, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSecondary)
    Spacer(Modifier.height(6.dp))

    if (items.isEmpty()) {
        Text(
            "Not provided — $notKnownHint",
            fontSize = 11.sp,
            color    = TextTertiary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                InputChip(
                    selected = false,
                    onClick  = { onRemove(item) },
                    label    = { Text(item, fontSize = 12.sp) },
                    trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.size(14.dp)) },
                    colors   = InputChipDefaults.inputChipColors(
                        containerColor = SageGreen,
                        labelColor     = ForestGreen
                    )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value         = input,
            onValueChange = { input = it },
            placeholder   = { Text("Add…", fontSize = 12.sp) },
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            shape         = RoundedCornerShape(8.dp),
            textStyle     = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForestGreen,
                focusedLabelColor  = ForestGreen
            )
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = {
                onAdd(input)
                input = ""
            }
        ) {
            Icon(Icons.Default.Add, "Add", tint = ForestGreen)
        }
    }
}
