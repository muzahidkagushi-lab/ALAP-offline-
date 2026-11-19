package com.alap.mesh.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.ui.components.AlapLogoVector
import com.alap.mesh.ui.components.BrandGradientButton
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.Emerald100
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Sky100
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Slate100
import com.alap.mesh.ui.theme.Slate200
import com.alap.mesh.ui.theme.Slate300
import com.alap.mesh.ui.theme.Slate400
import com.alap.mesh.ui.theme.Slate600
import com.alap.mesh.ui.theme.Slate700
import com.alap.mesh.ui.theme.Slate800
import com.alap.mesh.ui.theme.Slate900
import com.alap.mesh.ui.theme.Teal100
import com.alap.mesh.ui.theme.Teal700
import kotlinx.coroutines.launch

private data class OnboardingSlideContent(
    val chipText: String,
    val chipBg: Color,
    val chipTextColor: Color,
    val title: String,
    val description: String
)

private val SlideContents = listOf(
    OnboardingSlideContent(
        chipText = "অফলাইন মেশ নেটওয়ার্ক",
        chipBg = Sky100,
        chipTextColor = Sky700,
        title = "ইন্টারনেট ছাড়াই অবাধ যোগাযোগ",
        description = "মোবাইল ডেটা বা ইন্টারনেট ছাড়াই আশেপাশের ডিভাইস যুক্ত হয়ে মেশ নেটওয়ার্ক তৈরি করে। সরাসরি রেঞ্জে না থাকলেও মধ্যবর্তী ডিভাইসের (hop) মাধ্যমে মেসেজ পৌঁছে যায়।"
    ),
    OnboardingSlideContent(
        chipText = "অফলাইন চ্যাটিং",
        chipBg = Sky100,
        chipTextColor = Sky700,
        title = "প্রাইভেট ও গ্রুপ চ্যাটিং সম্পূর্ণ অফলাইনে",
        description = "ইন্টারনেট ছাড়াই কাছাকাছি বন্ধু বা টিমের সাথে রিয়েল-টাইম চ্যাটিং করুন। আসল ACK ও হপ (hop) স্ট্যাটাসও দেখতে পাবেন সহজে।"
    ),
    OnboardingSlideContent(
        chipText = "ফাইল শেয়ারিং",
        chipBg = Emerald100,
        chipTextColor = Emerald700,
        title = "সরাসরি রেঞ্জে ফাইল শেয়ার",
        description = "ইন্টারনেট ছাড়াই কাছাকাছি ডিভাইসে (১-হপ) ছবি, ভিডিও ও ডকুমেন্ট পাঠান। বড় ফাইল শেয়ারের জন্য হাই-স্পিড লিংক ব্যবহৃত হয়।"
    ),
    OnboardingSlideContent(
        chipText = "হাইব্রিড কানেক্টিভিটি",
        chipBg = Teal100,
        chipTextColor = Teal700,
        title = "ব্লুটুথ দিয়ে সংযোগ ও মেশ রিলে",
        description = "কাছাকাছি ডিভাইস খোঁজা ও কম পাওয়ারে মেসেজ রিলেতে Bluetooth Low Energy, আর বড় ফাইলে উচ্চ-গতির লোকাল লিংক ব্যবহৃত হয়।"
    ),
    OnboardingSlideContent(
        chipText = "অরিজিনাল নেটিভ অ্যাপ",
        chipBg = Slate200,
        chipTextColor = Slate800,
        title = "নেটিভ নিয়ন্ত্রণ ও সার্ভারবিহীন স্থাপত্য",
        description = "কোনো সার্ভার, ট্র্যাকার বা ইন্টারনেট ছাড়াই সম্পূর্ণ স্থানীয়ভাবে Bluetooth ও ব্যাকগ্রাউন্ড মেশ সার্ভিস পরিচালিত হয়।"
    ),
    OnboardingSlideContent(
        chipText = "শুরু করতে প্রস্তুত",
        chipBg = Emerald100,
        chipTextColor = Emerald700,
        title = "এখনই শুরু করুন আলাপ (ALAP)",
        description = "ব্লুটুথ ও নেয়ারবাই অনুমতি দিয়ে আশেপাশের ডিভাইসের সাথে সংযুক্ত হোন এবং অফলাইনে মেসেজ আদান-প্রদান করুন।"
    )
)

private val BanglaNumbers = listOf("১", "২", "৩", "৪", "৫", "৬")

@Composable
fun OnboardingScreen(
    onOpenPermissionsSheet: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { SlideContents.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastSlide = currentPage == SlideContents.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AlapBrandBrush)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AlapLogoVector(modifier = Modifier.fillMaxSize(), color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Slate800, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = HindSiliguriFontFamily)) {
                            append("আলাপ ")
                        }
                        withStyle(SpanStyle(color = Sky600, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = PlusJakartaSansFontFamily)) {
                            append("ALAP")
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Slate100)
                        .border(1.dp, Slate200, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${BanglaNumbers[currentPage]} / ৬",
                        color = Slate600,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .alpha(if (isLastSlide) 0f else 1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isLastSlide) {
                            scope.launch { pagerState.animateScrollToPage(SlideContents.lastIndex) }
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "এড়িয়ে যান",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val item = SlideContents[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                when (page) {
                    0 -> Slide1MeshIllustration()
                    1 -> Slide2ChatIllustration()
                    2 -> Slide3FileShareIllustration()
                    3 -> Slide4HybridIllustration()
                    4 -> Slide5NativeChecklistIllustration()
                    else -> Slide6ReadyOrbitIllustration()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(item.chipBg)
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.chipText,
                            color = item.chipTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        color = Slate900,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.description,
                        color = Slate600,
                        fontSize = 13.sp,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlideContents.indices.forEach { idx ->
                    val isSelected = idx == currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(220),
                        label = "dotW"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.background(AlapBrandBrush)
                                else Modifier.background(Slate300)
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Slate100)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("onboarding_prev_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "পেছনে",
                            tint = Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!isLastSlide) {
                    BrandGradientButton(
                        text = "পরবর্তী",
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        },
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_next_button")
                    )
                } else {
                    BrandGradientButton(
                        text = "আলাপ শুরু করুন (Start)",
                        onClick = onOpenPermissionsSheet,
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_start_button")
                    )
                }
            }
        }
    }
}
