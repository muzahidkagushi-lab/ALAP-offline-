package com.alap.mesh.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.data.local.MeshPeerEntity
import com.alap.mesh.ui.RadarRadioMode
import com.alap.mesh.ui.components.AlapSurfaceCard
import com.alap.mesh.ui.components.BrandGradientButton
import com.alap.mesh.ui.theme.AlapBlue
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.AlapMint
import com.alap.mesh.ui.theme.Emerald100
import com.alap.mesh.ui.theme.Emerald400
import com.alap.mesh.ui.theme.Emerald500
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.Indigo500
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Sky200
import com.alap.mesh.ui.theme.Sky300
import com.alap.mesh.ui.theme.Sky400
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky500
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Slate300
import com.alap.mesh.ui.theme.Slate800
import com.alap.mesh.ui.theme.Slate900
import com.alap.mesh.ui.theme.Teal500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarTab(
    peers: List<MeshPeerEntity>,
    localNodeId: String = "",
    radioMode: RadarRadioMode,
    selectedPeerId: String?,
    pulseTrigger: Int,
    onSelectRadioMode: (RadarRadioMode) -> Unit,
    onRescan: () -> Unit,
    onSelectPeer: (String) -> Unit,
    onStartChatWithPeer: (MeshPeerEntity) -> Unit
) {
    val palette = LocalAlapColors.current
    val selectedPeer = peers.find { it.nodeId == selectedPeerId } ?: peers.firstOrNull()
    var showHopSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "rScale"
    )
    val radarAlpha = (1f - ((radarScale - 0.6f) / 1.3f)).coerceIn(0f, 0.65f)

    val burstAnim by animateFloatAsState(
        targetValue = if (pulseTrigger % 2 == 0) 1f else 1.15f,
        animationSpec = tween(350),
        label = "burst"
    )

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "মেশ রাডার টপোলজি",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Text(
                    text = "ব্লুটুথ (BLE) স্ক্যান ও মাল্টি-হপ টপোলজি",
                    color = palette.textCaption,
                    fontSize = 11.sp,
                    fontFamily = HindSiliguriFontFamily
                )
            }

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Sky50)
                    .border(1.dp, Sky200, RoundedCornerShape(12.dp))
                    .clickable { onRescan() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("radar_rescan_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Sky700, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "রি-স্ক্যান",
                    color = Sky700,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
        }

        // Radio Mode Toggle Card
        AlapSurfaceCard(cornerRadius = 16.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isBle = radioMode == RadarRadioMode.BLE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isBle) Sky500 else palette.subtleSurface)
                        .clickable { onSelectRadioMode(RadarRadioMode.BLE) }
                        .padding(vertical = 9.dp)
                        .testTag("radar_mode_ble"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BLE লো-পাওয়ার",
                        color = if (isBle) Color.White else palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (!isBle) Modifier.background(AlapBrandBrush)
                            else Modifier.background(palette.subtleSurface)
                        )
                        .clickable { onSelectRadioMode(RadarRadioMode.WIFI_BOOST) }
                        .padding(vertical = 9.dp)
                        .testTag("radar_mode_wifi"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nearby লিংক (ধাপ ২)",
                        color = if (!isBle) Color.White else palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
            }
        }

        // Dark Radar Topology Panel (256dp, corner 24dp, Slate900)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Slate900)
                .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            // Top HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Emerald400)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE MESH TOPOLOGY",
                        color = Slate300,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Sky500.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (radioMode == RadarRadioMode.BLE) "BLE SCAN ACTIVE" else "NEARBY (STEP 2)",
                        color = Sky300,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily
                    )
                }
            }

            // Concentric Rings + Connecting Lines + Real Discovered Nodes Only
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 22.dp)
            ) {
                val density = LocalDensity.current.density
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()

                val p0 = peers.getOrNull(0)
                val p1 = peers.getOrNull(1)
                val p2 = peers.getOrNull(2)
                val p3 = peers.getOrNull(3)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 3 concentric radar rings
                    drawCircle(
                        color = Sky400.copy(alpha = 0.25f),
                        radius = 48.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Emerald400.copy(alpha = 0.20f),
                        radius = 80.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Sky400.copy(alpha = radarAlpha),
                        radius = 56.dp.toPx() * radarScale * burstAnim,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    val tl = Offset(size.width * 0.22f, size.height * 0.25f)
                    val tr = Offset(size.width * 0.78f, size.height * 0.28f)
                    val bl = Offset(size.width * 0.16f, size.height * 0.76f)
                    val br = Offset(size.width * 0.76f, size.height * 0.76f)
                    val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))

                    if (p0 != null) {
                        drawLine(
                            color = if (p0.isConnected) AlapMint else AlapBlue,
                            start = center,
                            end = tl,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = if (p0.isConnected) null else dashed
                        )
                    }
                    if (p1 != null) {
                        drawLine(
                            color = if (p1.isConnected) AlapMint else AlapBlue,
                            start = center,
                            end = tr,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = if (p1.isConnected) null else dashed
                        )
                    }
                    if (p2 != null) {
                        drawLine(
                            color = if (p2.isConnected) AlapMint else AlapBlue,
                            start = center,
                            end = bl,
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = if (p2.isConnected) null else dashed
                        )
                    }
                    if (p3 != null) {
                        drawLine(
                            color = if (p3.isConnected) AlapMint else AlapBlue,
                            start = center,
                            end = br,
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = if (p3.isConnected) null else dashed
                        )
                    }
                }

                // Center Self Node
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(AlapBrandBrush)
                            .border(2.dp, Color.White, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "আপনি",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = localNodeId.ifBlank { "..." },
                        color = Sky300,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily
                    )
                }

                if (p0 != null) {
                    RadarPeerBubble(
                        peer = p0,
                        nodeColor = Sky500,
                        nodeSize = 36.dp,
                        isSelected = p0.nodeId == selectedPeer?.nodeId,
                        label = "${p0.name.substringBefore(" ")} • ${p0.hopLabel}",
                        modifier = Modifier.offset(
                            x = (widthPx * 0.14f / density).dp,
                            y = (heightPx * 0.08f / density).dp
                        )
                    ) { onSelectPeer(p0.nodeId) }
                }

                if (p1 != null) {
                    RadarPeerBubble(
                        peer = p1,
                        nodeColor = Emerald500,
                        nodeSize = 40.dp,
                        isSelected = p1.nodeId == selectedPeer?.nodeId,
                        label = "${p1.name.substringBefore(" ")} • ${p1.hopLabel}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 16.dp)
                    ) { onSelectPeer(p1.nodeId) }
                }

                if (p2 != null) {
                    RadarPeerBubble(
                        peer = p2,
                        nodeColor = Teal500,
                        nodeSize = 32.dp,
                        isSelected = p2.nodeId == selectedPeer?.nodeId,
                        label = "${p2.name.substringBefore(" ")} • ${p2.hopLabel}",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 8.dp, start = 10.dp)
                    ) { onSelectPeer(p2.nodeId) }
                }

                if (p3 != null) {
                    RadarPeerBubble(
                        peer = p3,
                        nodeColor = Indigo500,
                        nodeSize = 32.dp,
                        isSelected = p3.nodeId == selectedPeer?.nodeId,
                        label = "${p3.name.substringBefore(" ")} • ${p3.hopLabel}",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp, end = 18.dp)
                    ) { onSelectPeer(p3.nodeId) }
                }
            }
        }

        // Selected Node Action Card OR Empty State Card
        if (selectedPeer == null) {
            AlapSurfaceCard(cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothSearching,
                        contentDescription = null,
                        tint = Sky600,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = "আশেপাশে এখনো কোনো আলাপ ডিভাইস নেই",
                        color = palette.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "কাছাকাছি অন্য ফোনে আলাপ অ্যাপ চালু রেখে \"রি-স্ক্যান\" চাপুন অথবা ব্লুটুথ ও লোকেশন চালু আছে কি না নিশ্চিত করুন।",
                        color = palette.textCaption,
                        fontSize = 11.5.sp,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedPeer.name,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = HindSiliguriFontFamily
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Emerald100)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = selectedPeer.hopLabel,
                                        color = Emerald700,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSansFontFamily
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "মাধ্যম: ${selectedPeer.linkType} • সিগন্যাল: ${selectedPeer.signalLabel}",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = HindSiliguriFontFamily
                            )
                            Text(
                                text = "স্ট্যাটাস: ${selectedPeer.statusText}",
                                color = Sky600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFontFamily
                            )
                            Text(
                                text = "পাথ: ${selectedPeer.pathSerialized.replace("|", " → ")}",
                                color = palette.textCaption,
                                fontSize = 10.5.sp,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrandGradientButton(
                            text = "মেসেজ পাঠান",
                            onClick = { onStartChatWithPeer(selectedPeer) },
                            cornerRadius = 12.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radar_message_peer_button")
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.subtleSurface)
                                .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
                                .clickable { showHopSheet = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AltRoute, contentDescription = null, tint = Sky600, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "হপ পাথ দেখুন",
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHopSheet && selectedPeer != null) {
        ModalBottomSheet(
            onDismissRequest = { showHopSheet = false },
            containerColor = palette.cardSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val steps = selectedPeer.pathSerialized.split("|")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPeer.name} — হপ পাথ",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    IconButton(onClick = { showHopSheet = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
                    }
                }
                steps.forEachIndexed { idx, s ->
                    Text(
                        text = "${idx + 1}. $s",
                        color = palette.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarPeerBubble(
    peer: MeshPeerEntity,
    nodeColor: Color,
    nodeSize: Dp,
    isSelected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(180),
        label = "nodeScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(nodeColor)
                .border(
                    width = if (isSelected) 2.5.dp else 1.5.dp,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = peer.avatarShort,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Slate800.copy(alpha = 0.9f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = label,
                color = Sky200,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = HindSiliguriFontFamily
            )
        }
    }
}
