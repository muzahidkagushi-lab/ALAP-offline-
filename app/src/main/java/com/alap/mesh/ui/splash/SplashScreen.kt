package com.alap.mesh.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.ui.components.AlapLogoVector
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AlapEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val RippleEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun SplashScreen(
    animationsEnabled: Boolean = true,
    onFinish: () -> Unit
) {
    val logoAnim = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val titleAnim = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val taglineAnim = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val progressAnim = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val exitAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            launch {
                logoAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 900, easing = AlapEaseOut)
                )
            }
            launch {
                progressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2500, easing = AlapEaseOut)
                )
            }
            launch {
                delay(700)
                titleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = AlapEaseOut)
                )
            }
            launch {
                delay(950)
                taglineAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = AlapEaseOut)
                )
            }
            delay(2800)
            exitAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = LinearEasing)
            )
        } else {
            delay(1200)
        }
        onFinish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashRipples")
    val r1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = RippleEase),
            repeatMode = RepeatMode.Restart
        ),
        label = "r1"
    )
    val r2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 0, easing = RippleEase),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(700)
        ),
        label = "r2"
    )
    val r3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 0, easing = RippleEase),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(1400)
        ),
        label = "r3"
    )

    val t = logoAnim.value
    val tlOffset: Offset
    val brOffset: Offset
    val bubbleScale: Float
    val bubbleAlpha: Float
    if (t <= 0.65f) {
        val p = (t / 0.65f).coerceIn(0f, 1f)
        val dx = -34f + (37f * p)
        val dy = -34f + (37f * p)
        tlOffset = Offset(dx, dy)
        brOffset = Offset(-dx, -dy)
        bubbleScale = 0.82f + (0.20f * p)
        bubbleAlpha = p
    } else {
        val p = ((t - 0.65f) / 0.35f).coerceIn(0f, 1f)
        val dx = 3f * (1f - p)
        val dy = 3f * (1f - p)
        tlOffset = Offset(dx, dy)
        brOffset = Offset(-dx, -dy)
        bubbleScale = 1.02f - (0.02f * p)
        bubbleAlpha = 1f
    }

    val exitScale = 1f + (0.05f * exitAnim.value)
    val exitAlpha = 1f - exitAnim.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = exitScale
                scaleY = exitScale
                alpha = exitAlpha
            }
            .background(AlapBrandBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
    ) {
        if (animationsEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = 85.dp.toPx()
                listOf(r1, r2, r3).forEach { prog ->
                    val scale = 0.45f + (2.35f * prog)
                    val radius = baseRadius * scale
                    val alpha = 0.7f * (1f - prog)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f * (1f - prog)),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f * alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "নেটিভ অফলাইন মেশ নেটওয়ার্ক",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = HindSiliguriFontFamily
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .shadow(16.dp, RoundedCornerShape(30.dp))
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(30.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AlapLogoVector(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        tlOffset = tlOffset,
                        tlScale = bubbleScale,
                        tlAlpha = bubbleAlpha,
                        brOffset = brOffset,
                        brScale = bubbleScale,
                        brAlpha = bubbleAlpha
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = Color.White
                            )
                        ) {
                            append("আলাপ ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        ) {
                            append("ALAP")
                        }
                    },
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAnim.value
                        translationY = 18.dp.toPx() * (1f - titleAnim.value)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "অফলাইন মেশ কমিউনিকেশন অ্যাপ",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = HindSiliguriFontFamily,
                    modifier = Modifier.graphicsLayer {
                        alpha = taglineAnim.value
                        translationY = 18.dp.toPx() * (1f - taglineAnim.value)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Bluetooth LE Mesh • Offline Multi-Hop",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSansFontFamily,
                    modifier = Modifier.graphicsLayer {
                        alpha = taglineAnim.value
                        translationY = 18.dp.toPx() * (1f - taglineAnim.value)
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(220.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim.value.coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("splash_skip_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "এখনই এগিয়ে যান",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFontFamily,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}
