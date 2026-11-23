package com.mediwell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.util.Config

/**
 * MANDATORY CLINICAL GUARDRAIL:
 * Fixed, non-editable referral banner directing the patient to a pharmacist or doctor.
 * Appears at the conclusion of every report. Cannot be dismissed or edited.
 */
@Composable
fun ReferralBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF0F7F6),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFBCE0DA),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.LocalPharmacy,
                contentDescription = "Clinical Referral Notice",
                tint = Color(0xFF0D5C75),
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Professional Medical Consultation",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF0D5C75),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Config.FIXED_REFERRAL_DISCLAIMER,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2C3E44),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
