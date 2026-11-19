package com.alap.mesh.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.data.local.ConversationEntity
import com.alap.mesh.data.local.MeshPeerEntity
import com.alap.mesh.ui.AlapViewModel
import com.alap.mesh.ui.ChatFilter
import com.alap.mesh.ui.components.AlapSurfaceCard
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.Amber700
import com.alap.mesh.ui.theme.Emerald500
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Rose100
import com.alap.mesh.ui.theme.Rose50
import com.alap.mesh.ui.theme.Rose600
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.util.BanglaFormatters

@Composable
fun ChatsTab(
    conversations: List<ConversationEntity>,
    peers: List<MeshPeerEntity>,
    activeFilter: ChatFilter,
    onSelectFilter: (ChatFilter) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenPeerChat: (MeshPeerEntity) -> Unit,
    onNavigateToRadar: () -> Unit,
    onSendMeshPing: () -> Unit,
    onSendSosBroadcast: (String) -> Unit
) {
    val palette = LocalAlapColors.current
    var showSosDialog by remember { mutableStateOf(false) }
    var sosNote by remember { mutableStateOf("") }

    val filteredConversations = remember(conversations, activeFilter) {
        conversations.filter { conv ->
            when (activeFilter) {
                ChatFilter.ALL -> true
                ChatFilter.DIRECT -> conv.type == "direct"
                ChatFilter.GROUP -> conv.type == "group" || conv.type == "channel"
                ChatFilter.FILES -> conv.hasFile
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pingDot")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "dotScale"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "আলাপ চ্যাট",
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = palette.textCaption)) {
                                append("অফলাইন মেশে ")
                            }
                            withStyle(SpanStyle(color = if (peers.isNotEmpty()) Emerald600 else palette.textSecondary, fontWeight = FontWeight.Bold)) {
                                append("${AlapViewModel.toBanglaDigits(peers.size)}টি ডিভাইস")
                            }
                            withStyle(SpanStyle(color = palette.textCaption)) {
                                append(if (peers.isNotEmpty()) " সক্রিয়" else " খুঁজে পাওয়া গেছে")
                            }
                        },
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguriFontFamily
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Rose50)
                            .border(1.dp, Rose100, RoundedCornerShape(12.dp))
                            .clickable { showSosDialog = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("sos_broadcast_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "SOS জরুরি বার্তা",
                            tint = Rose600,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SOS",
                            color = Rose600,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSansFontFamily
                        )
                    }

                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AlapBrandBrush)
                            .clickable { onSendMeshPing() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("mesh_ping_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "মেশ পিং",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }
            }
        }

        // Near-Me / Mesh Strip Card
        item {
            AlapSurfaceCard(cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer {
                                        scaleX = dotScale
                                        scaleY = dotScale
                                    }
                                    .clip(CircleShape)
                                    .background(if (peers.isNotEmpty()) Emerald500 else Sky600)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "কাছাকাছি মেশ নোড (Near-Me)",
                                color = palette.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToRadar() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("near_me_map_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ম্যাপ দেখুন",
                                color = Sky600,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Sky600,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (peers.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.subtleSurface)
                                .clickable { onNavigateToRadar() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BluetoothSearching,
                                contentDescription = null,
                                tint = Sky600,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "আশেপাশে এখনো কোনো আলাপ ডিভাইস নেই। রাডার থেকে রি-স্ক্যান করুন।",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(peers, key = { it.nodeId }) { peer ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(58.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenPeerChat(peer) }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AlapBrandBrush)
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = peer.avatarShort,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = HindSiliguriFontFamily
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = peer.name.substringBefore(" "),
                                        color = palette.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = HindSiliguriFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = peer.hopLabel,
                                        color = if (peer.isConnected) Emerald600 else Sky600,
                                        fontSize = 9.sp,
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

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilterChip("সব (All)", activeFilter == ChatFilter.ALL) { onSelectFilter(ChatFilter.ALL) }
                ChatFilterChip("প্রাইভেট", activeFilter == ChatFilter.DIRECT) { onSelectFilter(ChatFilter.DIRECT) }
                ChatFilterChip("গ্রুপ", activeFilter == ChatFilter.GROUP) { onSelectFilter(ChatFilter.GROUP) }
                ChatFilterChip("ফাইলস", activeFilter == ChatFilter.FILES) { onSelectFilter(ChatFilter.FILES) }
            }
        }

        if (filteredConversations.isEmpty()) {
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
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Sky600,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "এই ফিল্টারে কোনো কথোপকথন নেই",
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "কাছাকাছি কোনো ডিভাইস সংযুক্ত হলে এখানে স্বয়ংক্রিয়ভাবে চ্যাট শুরু করতে পারবেন।",
                            color = palette.textCaption,
                            fontSize = 11.5.sp,
                            fontFamily = HindSiliguriFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredConversations, key = { it.id }) { conv ->
                AlapSurfaceCard(
                    cornerRadius = 16.dp,
                    onClick = { onOpenConversation(conv.id) },
                    modifier = Modifier.testTag("conversation_card_${conv.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AlapBrandBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conv.avatarShort,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.name,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = HindSiliguriFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = conv.lastMessageTime,
                                    color = palette.textPlaceholder,
                                    fontSize = 10.sp,
                                    fontFamily = PlusJakartaSansFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = conv.lastMessage,
                                color = palette.textSecondary,
                                fontSize = 12.sp,
                                fontFamily = HindSiliguriFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (palette.isDark) Sky600.copy(alpha = 0.2f) else Sky50)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = conv.hopBadge,
                                        color = Sky600,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFontFamily
                                    )
                                }

                                val isAcked = conv.deliveryStatus.contains("পৌঁছেছে") || conv.deliveryStatus.contains("প্রাপ্ত")
                                val isQueued = conv.deliveryStatus.contains("কিউ") || conv.deliveryStatus.contains("হচ্ছে")
                                val statusColor = when {
                                    isAcked -> Emerald600
                                    isQueued -> Amber700
                                    else -> Sky600
                                }
                                val statusIcon = when {
                                    isAcked -> Icons.Rounded.DoneAll
                                    isQueued -> Icons.Rounded.Schedule
                                    else -> Icons.Rounded.Done
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = conv.deliveryStatus,
                                        color = statusColor,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = HindSiliguriFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (conv.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AlapBrandBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = BanglaFormatters.toBanglaDigits(conv.unreadCount),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = HindSiliguriFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = palette.cardSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Rose600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SOS জরুরি মেশ ব্রডকাস্ট",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        color = palette.textPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ইন্টারনেট ছাড়াই সর্বোচ্চ হপ সীমা ব্যবহার করে আশেপাশের সব আলাপ ডিভাইসে জরুরি সতর্কবার্তা পাঠান।",
                        fontSize = 12.sp,
                        fontFamily = HindSiliguriFontFamily,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = sosNote,
                        onValueChange = { sosNote = it },
                        placeholder = {
                            Text("ঐচ্ছিক অবস্থান বা বার্তা লিখুন...", fontSize = 12.sp, fontFamily = HindSiliguriFontFamily)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendSosBroadcast(sosNote)
                        sosNote = ""
                        showSosDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("SOS পাঠান", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = HindSiliguriFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("বাতিল", fontFamily = HindSiliguriFontFamily)
                }
            }
        )
    }
}

@Composable
private fun ChatFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAlapColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(AlapBrandBrush)
                else Modifier
                    .background(palette.cardSurface)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
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
