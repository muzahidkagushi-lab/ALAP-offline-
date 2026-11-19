package com.alap.mesh.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.alap.mesh.data.local.ConversationEntity
import com.alap.mesh.data.local.MessageEntity
import com.alap.mesh.mesh.protocol.BinaryMeshPacket
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.AlapMint
import com.alap.mesh.ui.theme.Emerald50
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Rose50
import com.alap.mesh.ui.theme.Rose600
import com.alap.mesh.ui.theme.Sky200
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Slate100
import com.alap.mesh.ui.theme.Slate700
import com.alap.mesh.ui.theme.Teal600
import com.alap.mesh.util.BanglaFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    localNodeId: String = "",
    showHopIndicator: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendPickedUri: (Uri) -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalAlapColors.current
    var composerText by remember { mutableStateOf("") }
    var showAttachStrip by remember { mutableStateOf(false) }
    var hopModalSteps by remember { mutableStateOf<List<String>?>(null) }
    val selfLabel = localNodeId.ifBlank { "আপনি" }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        showAttachStrip = false
        if (uri != null) onSendPickedUri(uri)
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        showAttachStrip = false
        if (uri != null) onSendPickedUri(uri)
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        showAttachStrip = false
        if (uri != null) onSendPickedUri(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Chat Room Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.navBarSurface)
                .border(1.dp, palette.navBarBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.subtleSurface)
                        .clickable { onBack() }
                        .testTag("chat_room_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "পিছনে যান",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AlapBrandBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.avatarShort,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation.name,
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${conversation.hopBadge} • BLE Multi-Hop",
                        color = Emerald600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (palette.isDark) Sky600.copy(alpha = 0.2f) else Sky50)
                    .border(1.dp, Sky200, RoundedCornerShape(12.dp))
                    .clickable {
                        val lastWithPath = messages.lastOrNull { it.hopPathSerialized.isNotBlank() }
                        hopModalSteps = lastWithPath?.hopPathSerialized?.split("|")
                            ?: listOf("আপনি ($selfLabel)", "${conversation.name} (${conversation.hopBadge})")
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("chat_hop_path_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AltRoute,
                    contentDescription = null,
                    tint = Sky600,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "হপ পাথ",
                    color = Sky700,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
        }

        // Messages Thread
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Sky600,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "এখনো কোনো মেসেজ নেই",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    Text(
                        text = "নিচের বক্স থেকে অফলাইন ব্লুটুথ মেশে প্রথম বার্তা পাঠান।",
                        color = palette.textCaption,
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubbleItem(
                        message = msg,
                        showHopIndicator = showHopIndicator,
                        onClickHopInfo = {
                            hopModalSteps = if (msg.hopPathSerialized.isNotBlank()) {
                                msg.hopPathSerialized.split("|")
                            } else {
                                listOf("আপনি ($selfLabel)", "${conversation.name} (BLE Mesh)")
                            }
                        }
                    )
                }
            }
        }

        // Attachment Strip (Slide Up)
        AnimatedVisibility(
            visible = showAttachStrip,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.cardSurface)
                    .border(1.dp, palette.cardBorder)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ফাইল শেয়ারিং ধাপ ২-এ Nearby Connections দিয়ে যুক্ত হবে (১-হপ সরাসরি)",
                    color = palette.textCaption,
                    fontSize = 10.sp,
                    fontFamily = HindSiliguriFontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachOptionTile(Icons.Rounded.Image, "ছবি", Sky700, Sky50) {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    AttachOptionTile(Icons.Rounded.Movie, "ভিডিও", Emerald700, Emerald50) {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                    AttachOptionTile(Icons.Rounded.AudioFile, "অডিও", Teal600, Color(0xFFF0FDFA)) {
                        docPicker.launch(arrayOf("audio/*"))
                    }
                    AttachOptionTile(Icons.Rounded.Description, "ফাইল", Slate700, Slate100) {
                        docPicker.launch(arrayOf("*/*"))
                    }
                }
            }
        }

        if (composerText.length > 400) {
            Text(
                text = "${BanglaFormatters.toBanglaDigits(composerText.length)}/${BanglaFormatters.toBanglaDigits(BinaryMeshPacket.MAX_TEXT_CHARS)} অক্ষর",
                color = if (composerText.length >= BinaryMeshPacket.MAX_TEXT_CHARS) Rose600 else palette.textCaption,
                fontSize = 10.sp,
                fontFamily = HindSiliguriFontFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                textAlign = TextAlign.End
            )
        }

        // Offline Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.navBarSurface)
                .border(1.dp, palette.navBarBorder)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.subtleSurface)
                    .clickable { showAttachStrip = !showAttachStrip }
                    .testTag("chat_attach_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "ফাইল বা মিডিয়া পাঠান",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.subtleSurface)
                    .border(1.dp, palette.subtleBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (composerText.isEmpty()) {
                    Text(
                        text = "অফলাইন মেসেজ লিখুন...",
                        color = palette.textPlaceholder,
                        fontSize = 12.5.sp,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
                BasicTextField(
                    value = composerText,
                    onValueChange = { next ->
                        composerText = if (next.length > BinaryMeshPacket.MAX_TEXT_CHARS) {
                            next.take(BinaryMeshPacket.MAX_TEXT_CHARS)
                        } else {
                            next
                        }
                    },
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 12.5.sp,
                        fontFamily = HindSiliguriFontFamily
                    ),
                    cursorBrush = SolidColor(Sky600),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_composer_input")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AlapBrandBrush)
                    .clickable {
                        if (composerText.isNotBlank()) {
                            onSendMessage(composerText)
                            composerText = ""
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("chat_send_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "পাঠান",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Hop Path Modal Bottom Sheet
    if (hopModalSteps != null) {
        ModalBottomSheet(
            onDismissRequest = { hopModalSteps = null },
            containerColor = palette.cardSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মেশ রাউটিং ও হপ পাথ (Hop Path)",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    IconButton(onClick = { hopModalSteps = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = palette.textCaption)
                    }
                }

                Text(
                    text = "ইন্টারনেট ছাড়াই মেসেজটি নিচের মেশ ধাপগুলোর মাধ্যমে গন্তব্যে পৌঁছেছে:",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = HindSiliguriFontFamily
                )

                val steps = hopModalSteps.orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.subtleSurface)
                        .border(1.dp, palette.subtleBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    steps.forEachIndexed { idx, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == 0 || idx == steps.lastIndex) AlapBrandBrush else SolidColor(Sky600)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFontFamily
                                    )
                                }
                                if (idx < steps.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(18.dp)
                                            .background(AlapMint.copy(alpha = 0.5f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                color = palette.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFontFamily,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: MessageEntity,
    showHopIndicator: Boolean,
    onClickHopInfo: () -> Unit
) {
    val palette = LocalAlapColors.current
    val isMe = message.isMe
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 285.dp)
                .clip(bubbleShape)
                .then(
                    when {
                        message.isSos -> Modifier
                            .background(Rose50)
                            .border(1.5.dp, Rose600, bubbleShape)
                        isMe -> Modifier.background(AlapBrandBrush)
                        else -> Modifier
                            .background(palette.cardSurface)
                            .border(1.dp, palette.cardBorder, bubbleShape)
                    }
                )
                .clickable { onClickHopInfo() }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            if (message.isFile) {
                val fileIcon = when (message.fileType) {
                    "video" -> Icons.Rounded.Movie
                    "image" -> Icons.Rounded.Image
                    "audio" -> Icons.Rounded.AudioFile
                    else -> Icons.Rounded.Description
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMe) Color.White.copy(alpha = 0.2f) else Sky50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = null,
                            tint = if (isMe) Color.White else Sky600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = message.fileName.ifBlank { message.text },
                            color = if (isMe) Color.White else palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSansFontFamily
                        )
                        if (message.fileSize.isNotBlank()) {
                            Text(
                                text = message.fileSize,
                                color = if (isMe) Color.White.copy(alpha = 0.85f) else palette.textCaption,
                                fontSize = 10.sp,
                                fontFamily = PlusJakartaSansFontFamily
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (message.isSos) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Rose600,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = message.text,
                        color = when {
                            message.isSos -> Rose600
                            isMe -> Color.White
                            else -> palette.textPrimary
                        },
                        fontSize = 12.5.sp,
                        fontWeight = if (message.isSos) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = HindSiliguriFontFamily,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = buildString {
                append(message.timeLabel)
                if (showHopIndicator && message.hopMeta.isNotBlank()) {
                    append(" • ")
                    append(message.hopMeta)
                }
            },
            color = palette.textPlaceholder,
            fontSize = 9.5.sp,
            fontFamily = HindSiliguriFontFamily,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun AttachOptionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = HindSiliguriFontFamily)
    }
}
