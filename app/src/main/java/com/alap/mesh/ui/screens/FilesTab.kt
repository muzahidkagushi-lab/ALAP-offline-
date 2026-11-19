package com.alap.mesh.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.data.local.FileTransferEntity
import com.alap.mesh.ui.FileFilter
import com.alap.mesh.ui.components.AlapSurfaceCard
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.Amber50
import com.alap.mesh.ui.theme.Amber700
import com.alap.mesh.ui.theme.Emerald50
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Teal600
import com.alap.mesh.util.BanglaFormatters

@Composable
fun FilesTab(
    files: List<FileTransferEntity>,
    activeFilter: FileFilter,
    onSelectFilter: (FileFilter) -> Unit,
    onPickFileToSend: (Uri) -> Unit
) {
    val palette = LocalAlapColors.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPickFileToSend(uri)
    }

    val filteredFiles = remember(files, activeFilter) {
        files.filter { f ->
            when (activeFilter) {
                FileFilter.ALL -> true
                FileFilter.SENT -> f.direction == "sent"
                FileFilter.RECEIVED -> f.direction == "received"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "অফলাইন ফাইল হাব",
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    Text(
                        text = "১-হপ সরাসরি হাই-স্পিড ফাইল আদান-প্রদান (ধাপ ২-এ সক্রিয় হবে)",
                        color = palette.textCaption,
                        fontSize = 11.sp,
                        fontFamily = HindSiliguriFontFamily
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AlapBrandBrush)
                        .clickable { filePicker.launch(arrayOf("*/*")) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("files_send_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ফাইল পাঠান",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
            }
        }

        // Honest Step 1 Capability Card (No fake 342 MB / 38.5 MB/s numbers)
        item {
            AlapSurfaceCard(cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = Sky600, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ফাইল শেয়ারিং ইঞ্জিন (ধাপ ২)",
                                color = palette.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (palette.isDark) Sky600.copy(alpha = 0.2f) else Sky50)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STEP 2",
                                color = Sky600,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSansFontFamily
                            )
                        }
                    }

                    Text(
                        text = "ধাপ ১-এ শুধুমাত্র রিয়েল BLE মাল্টি-হপ টেক্সট মেসেজিং চালু আছে। আসল ১-হপ Nearby Connections ফাইল ট্রান্সফার (Downloads/ALAP/-এ সংরক্ষণসহ) ধাপ ২-এ যুক্ত হবে।",
                        color = palette.textSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguriFontFamily,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Filter Buttons (All / Sent / Received)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileFilterBtn("সব ফাইল", activeFilter == FileFilter.ALL, Modifier.weight(1f)) { onSelectFilter(FileFilter.ALL) }
                FileFilterBtn("পাঠানো", activeFilter == FileFilter.SENT, Modifier.weight(1f)) { onSelectFilter(FileFilter.SENT) }
                FileFilterBtn("প্রাপ্ত", activeFilter == FileFilter.RECEIVED, Modifier.weight(1f)) { onSelectFilter(FileFilter.RECEIVED) }
            }
        }

        if (filteredFiles.isEmpty()) {
            item {
                AlapSurfaceCard(cornerRadius = 16.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = Sky600,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "এখনো কোনো ফাইল ট্রান্সফার রেকর্ড নেই",
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ধাপ ২-এ বাস্তব ফাইল আদান-প্রদান শুরু হলে এখানে আসল গতি ও অগ্রগতি দেখাবে।",
                            color = palette.textCaption,
                            fontSize = 11.5.sp,
                            fontFamily = HindSiliguriFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredFiles, key = { it.id }) { file ->
                FileTransferCard(file)
            }
        }
    }
}

@Composable
private fun FileFilterBtn(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAlapColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(AlapBrandBrush)
                else Modifier
                    .background(palette.cardSurface)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = HindSiliguriFontFamily
        )
    }
}

@Composable
private fun FileTransferCard(file: FileTransferEntity) {
    val palette = LocalAlapColors.current
    val (icon, tint, tileBg) = when (file.fileType) {
        "video" -> Triple(Icons.Rounded.Movie, Sky700, Sky50)
        "image" -> Triple(Icons.Rounded.Image, Emerald700, Emerald50)
        "audio" -> Triple(Icons.Rounded.AudioFile, Amber700, Amber50)
        else -> Triple(Icons.Rounded.Description, Teal600, Color(0xFFF0FDFA))
    }
    val progressAnim by animateFloatAsState(
        targetValue = (file.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "fileProgress"
    )

    AlapSurfaceCard(cornerRadius = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tileBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = file.category, tint = tint, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = palette.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (file.direction == "sent") Icons.Rounded.NorthEast else Icons.Rounded.SouthWest,
                            contentDescription = null,
                            tint = if (file.direction == "sent") Sky600 else Emerald600,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${if (file.direction == "sent") "প্রেরক" else "গ্রাহক"}: ${file.peerName} • ${file.sizeText}",
                            color = palette.textCaption,
                            fontSize = 10.5.sp,
                            fontFamily = HindSiliguriFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (palette.isDark) Emerald600.copy(alpha = 0.2f) else Emerald50)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${file.medium} • ${file.speedText}",
                            color = Emerald700,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (file.progressPercent >= 100) "সম্পন্ন" else "${BanglaFormatters.toBanglaDigits(file.progressPercent)}%",
                        color = Emerald600,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    if (file.progressPercent >= 100) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (file.progressPercent < 100) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(palette.subtleSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim)
                            .clip(CircleShape)
                            .background(AlapBrandBrush)
                    )
                }
            }
        }
    }
}
