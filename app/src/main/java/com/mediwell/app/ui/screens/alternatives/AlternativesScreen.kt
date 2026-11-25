package com.mediwell.app.ui.screens.alternatives

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.ui.components.StatusBadge
import com.mediwell.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativesScreen(
    medicineName: String = "",
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Ivory,
        topBar = {
            TopAppBar(
                title = { Text("Affordable Alternatives", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ForestGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = WarningOrangeBg, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "⚕ Always consult a qualified pharmacist or doctor before switching to a different brand or generic.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = WarningOrange,
                        lineHeight = 16.sp
                    )
                }
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = White, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CompareArrows, null, tint = ForestGreen, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (medicineName.isBlank()) "Select a medicine from your library"
                            else "Alternatives for $medicineName",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        StatusBadge("Active ingredient reference check", bgColor = SageGreen, textColor = ForestGreen)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Generic equivalence and local pricing information will appear here when connected to your active prescription.",
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}
