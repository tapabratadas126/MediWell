package com.mediwell.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── MEDCHCK Brand Tokens ──────────────────────────────────────────────────────
val Ivory           = Color(0xFFF7F6F1) // page background
val ForestGreen     = Color(0xFF243F36) // primary brand colour
val ForestGreenDark = Color(0xFF1A2E27) // pressed / darker shade
val White           = Color(0xFFFFFFFF)
val BorderGray      = Color(0xFFE4E5DF) // subtle borders
val SageGreen       = Color(0xFFE9EFE9) // secondary surfaces / chips
val SageGreenDark   = Color(0xFFD0DBCF) // hover state on sage

// Legacy aliases kept so existing screens compile without changes
val TealPrimary        = ForestGreen
val TealOnPrimary      = White
val TealContainer      = SageGreen
val TealOnContainer    = ForestGreen
val SlateSecondary     = Color(0xFF4A6358)
val SlateOnSecondary   = White
val BackgroundLight    = Ivory
val SurfaceLight       = White
val TextPrimary        = Color(0xFF1A1C1A)
val TextSecondary      = Color(0xFF3D4B3F)
val TextTertiary       = Color(0xFF6B7A6D)
val OutlineBorder      = BorderGray

// Status / Severity Tokens
val VerifiedGreen      = Color(0xFF1B5E20)
val VerifiedGreenBg    = Color(0xFFE8F5E9)
val ReviewAmber        = Color(0xFFE65100)
val ReviewAmberBg      = Color(0xFFFFF3E0)
val CriticalRed        = Color(0xFFB71C1C)
val CriticalRedBg      = Color(0xFFFFEBEE)
val InfoBlue           = Color(0xFF0277BD)
val InfoBlueBg         = Color(0xFFE1F5FE)
val WarningOrange      = Color(0xFFF57C00)
val WarningOrangeBg    = Color(0xFFFFF8E1)
