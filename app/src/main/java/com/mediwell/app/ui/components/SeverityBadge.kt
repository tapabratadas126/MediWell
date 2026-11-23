package com.mediwell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.data.model.FindingSeverity
import com.mediwell.app.ui.theme.CriticalRed
import com.mediwell.app.ui.theme.CriticalRedBg
import com.mediwell.app.ui.theme.InfoBlue
import com.mediwell.app.ui.theme.InfoBlueBg
import com.mediwell.app.ui.theme.ReviewAmber
import com.mediwell.app.ui.theme.ReviewAmberBg

@Composable
fun SeverityBadge(
    severity: FindingSeverity,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (severity) {
        FindingSeverity.CRITICAL -> Triple(CriticalRedBg, CriticalRed, "High Risk")
        FindingSeverity.WARNING -> Triple(ReviewAmberBg, ReviewAmber, "Warning")
        FindingSeverity.INFO -> Triple(InfoBlueBg, InfoBlue, "Info")
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
