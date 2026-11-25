package com.mediwell.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.ui.theme.*

enum class MedchckButtonVariant { Primary, Secondary, Ghost }

@Composable
fun MedchckButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MedchckButtonVariant = MedchckButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    when (variant) {
        MedchckButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(text, color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
        MedchckButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
        ) {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        MedchckButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(48.dp)
        ) {
            Text(text, color = ForestGreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    bgColor: Color = SageGreen,
    textColor: Color = ForestGreen,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
