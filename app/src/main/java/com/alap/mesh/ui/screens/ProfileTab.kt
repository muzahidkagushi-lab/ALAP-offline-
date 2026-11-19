package com.alap.mesh.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.data.AlapSettings
import com.alap.mesh.ui.components.AlapSurfaceCard
import com.alap.mesh.ui.components.BrandGradientButton
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.Amber50
import com.alap.mesh.ui.theme.Amber700
import com.alap.mesh.ui.theme.Emerald100
import com.alap.mesh.ui.theme.Emerald50
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Sky200
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Teal600
import com.alap.mesh.util.BanglaFormatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileTab(
    settings: AlapSettings,
    fingerprint: String,
    onNavigateToSettings: () -> Unit,
    onSaveDisplayName: (String) -> Unit,
    onSelectStatus: (String) -> Unit
) {
    val palette = LocalAlapColors.current
    var nameDraft by remember(settings.displayName) { mutableStateOf(settings.displayName) }
    var showQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "মেশ প্রোফাইল",
                color = palette.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToSettings() }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .testTag("profile_to_settings_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সেটিংস",
                    color = Sky600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Sky600, modifier = Modifier.size(14.dp))
            }
        }

        // Identity Card (corner 24dp)
        AlapSurfaceCard(cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(AlapBrandBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = settings.displayName.take(1).ifBlank { "আ" },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.displayName,
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NODE ID: ${settings.nodeId.ifBlank { "..." }}",
                            color = Sky600,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Emerald100)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = settings.statusText,
                                color = Emerald700,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFontFamily
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.subtleSurface)
                            .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
                            .clickable { showQrDialog = true }
                            .testTag("profile_qr_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = "QR ভেরিফিকেশন", tint = Sky600, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "অফলাইন মেশ কমিউনিকেশন নোড • ভ্রমণ ও জরুরি যোগাযোগে ইন্টারনেট ছাড়াই যুক্ত।",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = HindSiliguriFontFamily,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.divider)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ডিসপ্লে নাম পরিবর্তন করুন",
                    color = palette.textPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.subtleSurface)
                            .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            textStyle = TextStyle(
                                color = palette.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFontFamily
                            ),
                            cursorBrush = SolidColor(Sky600),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )
                    }

                    BrandGradientButton(
                        text = "সেভ",
                        onClick = { onSaveDisplayName(nameDraft) },
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("profile_save_button")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip("Available Offline", Emerald700, Emerald50, Emerald100) {
                        onSelectStatus("Available Offline • Relay Active")
                    }
                    StatusChip("Relay Node", Sky700, Sky50, Sky200) {
                        onSelectStatus("Relay / Hop Bridge Only")
                    }
                    StatusChip("File Boost", Amber700, Amber50, Color(0xFFFDE68A)) {
                        onSelectStatus("Busy • File Transfer Mode")
                    }
                }
            }
        }

        // 3-Column Real Mesh Stats Grid (zero hardcoded numbers)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                icon = Icons.Rounded.Hub,
                value = BanglaFormatters.toBanglaDigits(settings.relayedMessageCount),
                label = "হপ রিলে মেসেজ",
                valueColor = Sky600,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Rounded.FolderShared,
                value = BanglaFormatters.toBanglaDigits(settings.sharedFilesCount),
                label = "শেয়ার করা ফাইল",
                valueColor = Emerald600,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Rounded.SwapVert,
                value = "${BanglaFormatters.toBanglaDigits(settings.txPacketCount)}/${BanglaFormatters.toBanglaDigits(settings.rxPacketCount)}",
                label = "মোট TX / RX প্যাকেট",
                valueColor = Teal600,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = palette.cardSurface,
            title = {
                Text(
                    text = "নোড আইডি ও QR তথ্য",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily,
                    color = palette.textPrimary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val grid = 13
                            val cell = size.width / grid
                            val hashSeed = (settings.nodeId + fingerprint).hashCode()
                            for (r in 0 until grid) {
                                for (c in 0 until grid) {
                                    val isCorner = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                                    val bit = ((hashSeed xor (r * 31 + c * 17)) and 1) == 0
                                    if (isCorner || bit) {
                                        drawRect(
                                            color = Color(0xFF0F172A),
                                            topLeft = Offset(c * cell, r * cell),
                                            size = Size(cell * 0.9f, cell * 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = settings.nodeId.ifBlank { "..." },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily,
                        color = Sky600,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ধাপ ২-এ X25519 আসল পাবলিক কী QR যুক্ত হবে ($fingerprint)",
                        fontSize = 11.sp,
                        fontFamily = HindSiliguriFontFamily,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("বন্ধ করুন", fontFamily = HindSiliguriFontFamily)
                }
            }
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSansFontFamily
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlapColors.current
    AlapSurfaceCard(cornerRadius = 16.dp, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = HindSiliguriFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = palette.textCaption,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = HindSiliguriFontFamily,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
