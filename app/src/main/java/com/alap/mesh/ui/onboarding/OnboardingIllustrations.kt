package com.alap.mesh.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.ui.components.AlapLogoVector
import com.alap.mesh.ui.theme.AlapBlue
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.AlapMint
import com.alap.mesh.ui.theme.Emerald100
import com.alap.mesh.ui.theme.Emerald300
import com.alap.mesh.ui.theme.Emerald400
import com.alap.mesh.ui.theme.Emerald50
import com.alap.mesh.ui.theme.Emerald500
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Rose100
import com.alap.mesh.ui.theme.Rose600
import com.alap.mesh.ui.theme.Sky100
import com.alap.mesh.ui.theme.Sky200
import com.alap.mesh.ui.theme.Sky400
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky500
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Slate100
import com.alap.mesh.ui.theme.Slate200
import com.alap.mesh.ui.theme.Slate400
import com.alap.mesh.ui.theme.Slate600
import com.alap.mesh.ui.theme.Slate700
import com.alap.mesh.ui.theme.Slate800
import com.alap.mesh.ui.theme.Slate900
import com.alap.mesh.ui.theme.Teal50
import com.alap.mesh.ui.theme.Teal600
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val CardBgBrush = Brush.linearGradient(
    colors = listOf(Sky50, Teal50.copy(alpha = 0.7f), Emerald50)
)

@Composable
fun Slide1MeshIllustration() {
    val transition = rememberInfiniteTransition(label = "slide1")
    val dashPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "dash"
    )
    val hopT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "hopDot"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBgBrush)
            .border(1.dp, Sky100, RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .border(1.dp, Rose100, CircleShape)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "ইন্টারনেট ছাড়াই",
                color = Rose600,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val sx = size.width / 300f
            val sy = size.height / 210f

            drawCircle(
                color = Sky200,
                radius = 82f * minOf(sx, sy),
                center = Offset(150f * sx, 112f * sy),
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            )

            val path1 = Path().apply {
                moveTo(60f * sx, 146f * sy)
                quadraticTo(105f * sx, 86f * sy, 150f * sx, 82f * sy)
            }
            val path2 = Path().apply {
                moveTo(150f * sx, 82f * sy)
                quadraticTo(195f * sx, 92f * sy, 240f * sx, 146f * sy)
            }
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), phase = -dashPhase)
            drawPath(
                path = path1,
                brush = AlapBrandBrush,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = dashEffect, cap = StrokeCap.Round)
            )
            drawPath(
                path = path2,
                brush = AlapBrandBrush,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = dashEffect, cap = StrokeCap.Round)
            )

            val dotPos = if (hopT < 0.5f) {
                val u = hopT * 2f
                val oneMinusU = 1f - u
                val x = oneMinusU * oneMinusU * 60f + 2 * oneMinusU * u * 105f + u * u * 150f
                val y = oneMinusU * oneMinusU * 146f + 2 * oneMinusU * u * 86f + u * u * 82f
                Offset(x * sx, y * sy)
            } else {
                val u = (hopT - 0.5f) * 2f
                val oneMinusU = 1f - u
                val x = oneMinusU * oneMinusU * 150f + 2 * oneMinusU * u * 195f + u * u * 240f
                val y = oneMinusU * oneMinusU * 82f + 2 * oneMinusU * u * 92f + u * u * 146f
                Offset(x * sx, y * sy)
            }
            drawCircle(color = Sky500, radius = 6.5.dp.toPx(), center = dotPos)

            fun drawPhoneNode(left: Float, top: Float, borderColor: Color) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(left * sx, top * sy),
                    size = Size(48f * sx, 52f * sy),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left * sx, top * sy),
                    size = Size(48f * sx, 52f * sy),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }

            drawPhoneNode(36f, 120f, AlapBlue)
            drawRoundRect(
                color = AlapBlue,
                topLeft = Offset(45f * sx, 134f * sy),
                size = Size(22f * sx, 8f * sy),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Sky100,
                topLeft = Offset(53f * sx, 147f * sy),
                size = Size(22f * sx, 8f * sy),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            drawPhoneNode(126f, 54f, AlapMint)
            drawPhoneNode(216f, 120f, AlapMint)
            drawRoundRect(
                color = AlapMint,
                topLeft = Offset(225f * sx, 135f * sy),
                size = Size(25f * sx, 9f * sy),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(x = w * 0.43f, y = h * 0.16f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Slate900)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HOP 1",
                        color = Emerald400,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSansFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = AlapMint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "আপনি",
                color = Sky600,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily,
                modifier = Modifier.offset(x = w * 0.16f, y = h * 0.83f)
            )

            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = AlapMint,
                modifier = Modifier
                    .offset(x = w * 0.77f, y = h * 0.72f)
                    .size(14.dp)
            )

            Text(
                text = "বন্ধু",
                color = Emerald600,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily,
                modifier = Modifier.offset(x = w * 0.77f, y = h * 0.83f)
            )
        }
    }
}

@Composable
fun Slide2ChatIllustration() {
    val transition = rememberInfiniteTransition(label = "slide2")
    val loopT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "chatLoop"
    )

    val msg1Alpha = when {
        loopT < 0.10f -> 0f
        loopT < 0.20f -> (loopT - 0.10f) / 0.10f
        loopT < 0.90f -> 1f
        else -> 1f - ((loopT - 0.90f) / 0.10f)
    }
    val msg2Alpha = when {
        loopT < 0.35f -> 0f
        loopT < 0.45f -> (loopT - 0.35f) / 0.10f
        loopT < 0.90f -> 1f
        else -> 1f - ((loopT - 0.90f) / 0.10f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Sky50, Color.White, Emerald50)))
            .border(1.dp, Sky100, RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Slate100, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AlapBrandBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "আ",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "অফলাইন গ্রুপ আড্ডা",
                    color = Slate800,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Emerald50)
                    .border(1.dp, Emerald100, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "OFFLINE MESH",
                    color = Emerald700,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFontFamily
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = msg1Alpha
                        translationY = (1f - msg1Alpha) * 8.dp.toPx()
                        scaleX = 0.96f + 0.04f * msg1Alpha
                        scaleY = 0.96f + 0.04f * msg1Alpha
                    }
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .background(Color.White)
                    .border(1.dp, Slate200, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "নেট নেই তবু মেসেজ যাচ্ছে?",
                    color = Slate700,
                    fontSize = 11.5.sp,
                    fontFamily = HindSiliguriFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = Sky600,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .graphicsLayer {
                        alpha = msg2Alpha
                        translationY = (1f - msg2Alpha) * 8.dp.toPx()
                        scaleX = 0.96f + 0.04f * msg2Alpha
                        scaleY = 0.96f + 0.04f * msg2Alpha
                    }
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .background(AlapBrandBrush)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "হ্যাঁ! ব্লুটুথ মেশে সরাসরি চ্যাট চলছে!",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = HindSiliguriFontFamily,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Slate200, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "অফলাইন মেসেজ লিখুন...",
                color = Slate400,
                fontSize = 11.sp,
                fontFamily = HindSiliguriFontFamily
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AlapBrandBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun Slide3FileShareIllustration() {
    val transition = rememberInfiniteTransition(label = "slide3")
    val streamT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "stream"
    )
    val barProg by transition.animateFloat(
        initialValue = 0.14f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Emerald50, Teal50.copy(alpha = 0.7f), Sky50)))
            .border(1.dp, Emerald100, RoundedCornerShape(24.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Emerald100, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Nearby P2P ফাইল শেয়ার",
                    color = Emerald700,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Sky100)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1-Hop Wi-Fi",
                    color = Sky700,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSansFontFamily
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Sky700,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .size(width = 50.dp, height = 62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(2.dp, Sky400, RoundedCornerShape(14.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Upload, contentDescription = null, tint = Sky600, modifier = Modifier.size(18.dp))
                Text("প্রেরক", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate700, fontFamily = HindSiliguriFontFamily)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth()) {
                    drawLine(
                        color = Emerald300,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = ((-36 + 72 * streamT)).dp.roundToPx(),
                                y = (-16).dp.roundToPx()
                            )
                        }
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AlapBrandBrush)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallMediaChip(Icons.Rounded.Image, "ছবি", Sky600)
                    SmallMediaChip(Icons.Rounded.Movie, "ভিডিও", Emerald600)
                    SmallMediaChip(Icons.Rounded.Description, "ফাইল", Teal600)
                }
            }

            Column(
                modifier = Modifier
                    .size(width = 50.dp, height = 62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(2.dp, AlapMint, RoundedCornerShape(14.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                Text("গ্রাহক", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate700, fontFamily = HindSiliguriFontFamily)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "সরাসরি ফাইল ট্রান্সফার",
                    color = Slate700,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Text(
                    text = "1-Hop Direct",
                    color = Emerald600,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFontFamily
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Slate100)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(barProg)
                        .clip(CircleShape)
                        .background(AlapBrandBrush)
                )
            }
        }
    }
}

@Composable
private fun SmallMediaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            fontFamily = HindSiliguriFontFamily
        )
    }
}

@Composable
fun Slide4HybridIllustration() {
    val transition = rememberInfiniteTransition(label = "slide4")
    val w1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2300, easing = LinearEasing)),
        label = "w1"
    )
    val w2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = LinearEasing),
            initialStartOffset = androidx.compose.animation.core.StartOffset(1150)
        ),
        label = "w2"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Sky50, Emerald50)))
            .border(1.dp, Emerald100, RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.56f)
            val minR = 22.dp.toPx()
            val maxR = 92.dp.toPx()

            listOf(w1, w2).forEach { prog ->
                val radius = minR + (maxR - minR) * prog
                val alpha = 0.85f * (1f - prog)
                drawCircle(
                    color = AlapBlue.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            drawLine(
                color = Sky400.copy(alpha = 0.7f),
                start = Offset(size.width * 0.24f, size.height * 0.20f),
                end = center,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            drawLine(
                color = AlapMint.copy(alpha = 0.7f),
                start = Offset(size.width * 0.76f, size.height * 0.20f),
                end = center,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            drawArc(
                color = AlapMint,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - 42.dp.toPx(), center.y - 42.dp.toPx()),
                size = Size(84.dp.toPx(), 84.dp.toPx()),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Sky100, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Bluetooth লো-পাওয়ার",
                    color = Sky700,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Emerald100, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Wi-Fi / Nearby বুস্ট",
                    color = Emerald700,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 12.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(AlapBrandBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "BLE+WiFi",
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSansFontFamily
            )
        }
    }
}

@Composable
fun Slide5NativeChecklistIllustration() {
    val transition = rememberInfiniteTransition(label = "slide5")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Slate900, Slate800, Color(0xFF042F2E))))
            .border(1.dp, Slate700, RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = 0.2f))
                    .border(1.dp, Emerald400.copy(alpha = 0.35f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Emerald300, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ORIGINAL NATIVE APP",
                    color = Emerald300,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFontFamily
                )
            }
            Text(
                text = "মেশ হার্ডওয়্যার চেকলিস্ট",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NativeCheckRow(
                icon = Icons.Rounded.Bluetooth,
                title = "ব্লুটুথ (Bluetooth)",
                subtitle = "Discovery & Low Power",
                tint = Sky400,
                scaleBoost = 1f + 0.015f * pulse
            )
            NativeCheckRow(
                icon = Icons.Rounded.Wifi,
                title = "ওয়াই-ফাই ও নেয়ারবাই",
                subtitle = "1-Hop Direct File Transfer",
                tint = Emerald400,
                scaleBoost = 1f + 0.015f * (1f - pulse)
            )
            NativeCheckRow(
                icon = Icons.Rounded.Hub,
                title = "মেশ রিলে ও ACK",
                subtitle = "Multi-Hop Binary Routing",
                tint = AlapMint,
                scaleBoost = 1f + 0.015f * pulse
            )
        }
    }
}

@Composable
private fun NativeCheckRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    scaleBoost: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scaleBoost
                scaleY = scaleBoost
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = HindSiliguriFontFamily)
                Text(subtitle, color = Slate400, fontSize = 9.5.sp, fontFamily = PlusJakartaSansFontFamily)
            }
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Emerald500.copy(alpha = 0.22f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "READY",
                color = Emerald300,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSansFontFamily
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
fun Slide6ReadyOrbitIllustration() {
    val transition = rememberInfiniteTransition(label = "slide6")
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "orbit"
    )
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ripple"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Sky50, Color.White, Emerald50)))
            .border(1.dp, Sky200, RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - 6.dp.toPx())
            val r = 46.dp.toPx() + 44.dp.toPx() * ripple
            drawCircle(
                color = AlapBlue.copy(alpha = 0.35f * (1f - ripple)),
                radius = r,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Sky200,
                radius = 66.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            )

            val orbitRadius = 66.dp.toPx()
            listOf(0f to AlapBlue, 120f to AlapMint, 240f to Sky600).forEach { (degOffset, col) ->
                val rad = (orbitAngle + degOffset) * (PI / 180.0)
                val x = center.x + (orbitRadius * cos(rad)).toFloat()
                val y = center.y + (orbitRadius * sin(rad)).toFloat()
                drawCircle(color = col, radius = 5.dp.toPx(), center = Offset(x, y))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Emerald100)
                .padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Emerald700, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("READY", color = Emerald700, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSansFontFamily)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp)
                .size(64.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(AlapBrandBrush)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            AlapLogoVector(modifier = Modifier.fillMaxSize(), color = Color.White)
        }

        Text(
            text = "নিচের বাটনে ট্যাপ করে অনুমতি সেট করুন ও অ্যাপে প্রবেশ করুন",
            color = Slate600,
            fontSize = 10.5.sp,
            fontFamily = HindSiliguriFontFamily,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
