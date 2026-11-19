package com.alap.mesh.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Palette
val AlapBlue = Color(0xFF4FA8EE)
val AlapMint = Color(0xFF2EC495)
val AlapDark = Color(0xFF0B192C)
val AlapDarkCard = Color(0xFF13233A)
val AlapDarkSubtle = Color(0xFF1E293B)
val AlapBgLight = Color(0xFFF8FAFC)

// Slate Neutrals
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

// Accent & Semantic Tokens
val Emerald300 = Color(0xFF6EE7B7)
val Emerald400 = Color(0xFF34D399)
val Emerald500 = Color(0xFF10B981)
val Emerald600 = Color(0xFF059669)
val Emerald700 = Color(0xFF047857)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50 = Color(0xFFECFDF5)

val Sky300 = Color(0xFF7DD3FC)
val Sky400 = Color(0xFF38BDF8)
val Sky500 = Color(0xFF0EA5E9)
val Sky600 = Color(0xFF0284C7)
val Sky700 = Color(0xFF0369A1)
val Sky200 = Color(0xFFBAE6FD)
val Sky100 = Color(0xFFE0F2FE)
val Sky50 = Color(0xFFF0F9FF)

val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0D9488)
val Teal700 = Color(0xFF0F766E)
val Teal100 = Color(0xFFCCFBF1)
val Teal50 = Color(0xFFF0FDFA)

val Indigo500 = Color(0xFF6366F1)
val Indigo200 = Color(0xFFC7D2FE)

val Rose600 = Color(0xFFE11D48)
val Rose200 = Color(0xFFFECDD3)
val Rose100 = Color(0xFFFFE4E6)
val Rose50 = Color(0xFFFFF1F2)

val Amber700 = Color(0xFFB45309)
val Amber200 = Color(0xFFFDE68A)
val Amber50 = Color(0xFFFFFBEB)

// Brand Gradients
val AlapBrandBrush = Brush.linearGradient(
    colors = listOf(AlapBlue, AlapMint),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val AlapSoftBrush = Brush.linearGradient(
    colors = listOf(AlapBlue.copy(alpha = 0.14f), AlapMint.copy(alpha = 0.14f)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)
