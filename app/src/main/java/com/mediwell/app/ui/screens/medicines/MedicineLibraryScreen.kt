package com.mediwell.app.ui.screens.medicines

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineLibraryScreen(
    medications: List<MedicationItem>,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = medications.filter {
        query.isBlank() ||
        it.rawName.contains(query, ignoreCase = true) ||
        it.dose.contains(query, ignoreCase = true)
    }

    Scaffold(
        containerColor = Ivory,
        topBar = {
            TopAppBar(
                title = { Text("Medicine Library", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search medicines…") },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextTertiary) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreen,
                    focusedLabelColor  = ForestGreen
                )
            )
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Medication,
                            null,
                            tint     = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (query.isBlank()) "No medicines added yet"
                            else "No matches for \"$query\"",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                        if (query.isBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Upload a prescription or add medicines manually in the Rx tab",
                                color    = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered) { med ->
                        MedicineCard(med)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(med: MedicationItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SageGreen
            ) {
                Icon(
                    Icons.Default.Medication,
                    null,
                    tint     = ForestGreen,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    med.rawName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = TextPrimary
                )
                Text(
                    buildString {
                        if (med.dose.isNotBlank()) append(med.dose)
                        if (med.frequency.isNotBlank()) append(" · ${med.frequency}")
                        if (med.route.isNotBlank()) append(" · ${med.route}")
                    }.ifBlank { "No details provided" },
                    fontSize = 12.sp,
                    color    = TextTertiary
                )
                if (!med.matchedRxcui.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = VerifiedGreenBg
                    ) {
                        Text(
                            "Active ingredient confirmed",
                            modifier  = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize  = 10.sp,
                            color     = VerifiedGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
