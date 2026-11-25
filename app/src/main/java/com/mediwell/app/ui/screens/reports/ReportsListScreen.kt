package com.mediwell.app.ui.screens.reports

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.repository.ReportRepository
import com.mediwell.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsListScreen(
    userId: String,
    reportRepository: ReportRepository,
    onViewReport: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Ivory,
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Icon(
                    Icons.Default.Assessment,
                    null,
                    tint     = TextTertiary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Reports History", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your safety reports will appear here after you run a medication check.",
                    fontSize   = 13.sp,
                    color      = TextTertiary,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick  = onViewReport,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Run New Safety Check")
                }
            }
        }
    }
}
