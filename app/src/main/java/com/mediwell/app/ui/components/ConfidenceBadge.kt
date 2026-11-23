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
import com.mediwell.app.data.model.ConfidenceLevel
import com.mediwell.app.ui.theme.ReviewAmber
import com.mediwell.app.ui.theme.ReviewAmberBg
import com.mediwell.app.ui.theme.VerifiedGreen
import com.mediwell.app.ui.theme.VerifiedGreenBg

@Composable
fun ConfidenceBadge(
    confidence: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (confidence) {
        ConfidenceLevel.VERIFIED -> Triple(VerifiedGreenBg, VerifiedGreen, "Verified")
        ConfidenceLevel.NEEDS_REVIEW -> Triple(ReviewAmberBg, ReviewAmber, "Needs Review")
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
