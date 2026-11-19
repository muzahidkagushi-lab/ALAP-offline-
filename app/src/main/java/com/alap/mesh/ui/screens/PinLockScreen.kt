package com.alap.mesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Rose600
import com.alap.mesh.ui.theme.Sky600

@Composable
fun PinLockScreen(
    onVerifyPin: (String) -> Boolean
) {
    val palette = LocalAlapColors.current
    var entered by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun appendDigit(d: String) {
        if (entered.length >= 4) return
        isError = false
        val next = entered + d
        entered = next
        if (next.length == 4) {
            if (!onVerifyPin(next)) {
                isError = true
                entered = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AlapBrandBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "আলাপ PIN সুরক্ষা",
            color = palette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = HindSiliguriFontFamily
        )
        Text(
            text = if (isError) "ভুল PIN! আবার চেষ্টা করুন" else "অ্যাপে প্রবেশ করতে ৪-সংখ্যার PIN দিন",
            color = if (isError) Rose600 else palette.textCaption,
            fontSize = 12.sp,
            fontFamily = HindSiliguriFontFamily
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { idx ->
                val filled = idx < entered.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) Sky600 else palette.subtleSurface)
                        .border(1.5.dp, if (filled) Sky600 else palette.subtleBorder, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(palette.cardSurface)
                                    .border(1.dp, palette.cardBorder, CircleShape)
                                    .clickable {
                                        if (key == "DEL") {
                                            if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                        } else {
                                            appendDigit(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "মুছুন", tint = palette.textPrimary)
                                } else {
                                    Text(
                                        text = key,
                                        color = palette.textPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
