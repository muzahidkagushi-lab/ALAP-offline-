package com.alap.mesh.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.BuildConfig
import com.alap.mesh.data.AlapSettings
import com.alap.mesh.mesh.HardwarePermissionStatus
import com.alap.mesh.mesh.PermissionHelper
import com.alap.mesh.ui.components.AlapSurfaceCard
import com.alap.mesh.ui.theme.AlapBrandBrush
import com.alap.mesh.ui.theme.AlapMint
import com.alap.mesh.ui.theme.Amber50
import com.alap.mesh.ui.theme.Amber700
import com.alap.mesh.ui.theme.Emerald100
import com.alap.mesh.ui.theme.Emerald50
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.Emerald700
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.PlusJakartaSansFontFamily
import com.alap.mesh.ui.theme.Rose100
import com.alap.mesh.ui.theme.Rose50
import com.alap.mesh.ui.theme.Rose600
import com.alap.mesh.ui.theme.Sky200
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Sky700
import com.alap.mesh.ui.theme.Slate900
import com.alap.mesh.util.BanglaFormatters

@Composable
fun SettingsTab(
    settings: AlapSettings,
    permissionStatus: HardwarePermissionStatus,
    onBleIntervalChange: (String) -> Unit,
    onWifiBoostChange: (Boolean) -> Unit,
    onMaxHopChange: (Int) -> Unit,
    onRelayModeChange: (Boolean) -> Unit,
    onAutoDownloadChange: (Boolean) -> Unit,
    onTransferModeChange: (String) -> Unit,
    onHopIndicatorChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onUpdatePin: (String) -> Unit,
    onStealthChange: (Boolean) -> Unit,
    onRotateNodeId: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onReplaySplash: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onRunDiagnostics: () -> Unit
) {
    val palette = LocalAlapColors.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showBatteryGuide by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    fun matches(keywords: String): Boolean {
        val q = searchQuery.trim().lowercase()
        return q.isEmpty() || keywords.lowercase().contains(q)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "সেটিংস (Settings)",
                color = palette.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )
            Text(
                text = "নেটওয়ার্ক, চ্যাট ও ফাইল, প্রাইভেসি, থিম, পারমিশন এবং অ্যাপ তথ্য",
                color = palette.textCaption,
                fontSize = 11.sp,
                fontFamily = HindSiliguriFontFamily
            )
        }

        // Search Box
        AlapSurfaceCard(cornerRadius = 16.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textPlaceholder, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "সেটিংস খুঁজুন (ব্লুটুথ, হটস্পট, হপ, পিন, থিম)...",
                            color = palette.textPlaceholder,
                            fontSize = 12.sp,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontFamily = HindSiliguriFontFamily
                        ),
                        cursorBrush = SolidColor(Sky600),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_search_input")
                    )
                }
            }
        }

        // 1. নেটওয়ার্ক ও মেশ কানেক্টিভিটি
        if (matches("ব্লুটুথ bluetooth ওয়াই-ফাই wifi হটস্পট hotspot মেশ হপ hop রিলে relay স্ক্যান")) {
            SettingsSectionCard(
                title = "১. নেটওয়ার্ক ও মেশ কানেক্টিভিটি",
                titleColor = Sky700,
                badge = "MESH RADIO"
            ) {
                SettingsDropdownRow(
                    title = "ব্লুটুথ (BLE) স্ক্যান ইন্টারভাল",
                    selectedLabel = settings.bleScanInterval,
                    accentColor = Sky600,
                    options = listOf(
                        "Ultra Low Power (8s)" to "Ultra Low Power (8s) — সর্বোচ্চ ব্যাটারি সাশ্রয়",
                        "Balanced (3s)" to "Balanced (3s) — আদর্শ গতি ও ব্যাটারি",
                        "Fast Discovery (1s)" to "Fast Discovery (1s) — দ্রুত ডিভাইস খোঁজা"
                    ),
                    onSelected = onBleIntervalChange
                )

                SettingsSwitchRow(
                    title = "Nearby Connections হাই-স্পিড লিংক (ধাপ ২)",
                    subtitle = "১-হপ বড় ফাইল ট্রান্সফারের জন্য ধাপ ২-এ সক্রিয় হবে",
                    checked = settings.wifiBoostEnabled,
                    onCheckedChange = onWifiBoostChange
                )

                // Max Mesh Hop Limit Segmented 1..6
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "সর্বোচ্চ মেশ হপ সীমা (Max Hop Limit)",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFontFamily
                        )
                        Text(
                            text = "${BanglaFormatters.toBanglaDigits(settings.maxHopLimit)} হপ",
                            color = Emerald600,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..6).forEach { hop ->
                            val selected = settings.maxHopLimit == hop
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (selected) Modifier.background(AlapBrandBrush)
                                        else Modifier
                                            .background(palette.subtleSurface)
                                            .border(1.dp, palette.subtleBorder, RoundedCornerShape(10.dp))
                                    )
                                    .clickable { onMaxHopChange(hop) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = BanglaFormatters.toBanglaDigits(hop),
                                    color = if (selected) Color.White else palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = HindSiliguriFontFamily
                                )
                            }
                        }
                    }
                }

                SettingsSwitchRow(
                    title = "রিলে / হপ নোড মোড (Relay Bridge)",
                    subtitle = "অন্যদের মেসেজ মধ্যবর্তী নোড হিসেবে পরবর্তী হপে পৌঁছে দিন",
                    checked = settings.relayModeEnabled,
                    onCheckedChange = onRelayModeChange
                )
            }
        }

        // 2. চ্যাট ও ফাইল শেয়ারিং
        if (matches("চ্যাট chat ফাইল file অটো ডাউনলোড cache ক্যাশ ট্রান্সফার মোড হপ পাথ")) {
            SettingsSectionCard(
                title = "২. চ্যাট ও ফাইল শেয়ারিং",
                titleColor = Emerald700,
                badge = "CHAT & FILES"
            ) {
                SettingsSwitchRow(
                    title = "Nearby হাই-স্পিড অটো-রিসিভ (ধাপ ২)",
                    subtitle = "ধাপ ২-এ ফাইল ট্রান্সফার ইঞ্জিন যুক্ত হলে সক্রিয় হবে",
                    checked = settings.autoDownloadMedia,
                    onCheckedChange = onAutoDownloadChange
                )

                SettingsDropdownRow(
                    title = "ডিফল্ট ট্রান্সফার মোড",
                    selectedLabel = settings.defaultTransferMode,
                    accentColor = Emerald600,
                    options = listOf(
                        "Auto (Smart)" to "Auto (Smart) — মেসেজে BLE, ফাইলে Nearby",
                        "BLE Mesh Only" to "BLE Mesh Only — শুধুমাত্র লো-পাওয়ার ব্লুটুথ",
                        "Wi-Fi / Hotspot Boost" to "Nearby Boost — ধাপ ২-এর জন্য অগ্রাধিকার"
                    ),
                    onSelected = onTransferModeChange
                )

                SettingsSwitchRow(
                    title = "মেসেজ হপ-পাথ ইন্ডিকেটর",
                    subtitle = "চ্যাটে মেসেজ কোন পথে/হপে গিয়েছে তা দেখান",
                    checked = settings.showHopIndicator,
                    onCheckedChange = onHopIndicatorChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "অফলাইন ক্যাশ ও অস্থায়ী ফাইল",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Rose50)
                            .border(1.dp, Rose100, RoundedCornerShape(12.dp))
                            .clickable { onClearCache() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "ক্যাশ পরিষ্কার করুন",
                            color = Rose600,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }
            }
        }

        // 3. নিরাপত্তা ও প্রাইভেসি
        if (matches("নিরাপত্তা security প্রাইভেসি privacy এনক্রিপশন encryption পিন pin লক lock নোড node id stealth")) {
            SettingsSectionCard(
                title = "৩. নিরাপত্তা ও প্রাইভেসি",
                titleColor = palette.textPrimary,
                badge = "PRIVACY"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "এন্ড-টু-এন্ড মেশ এনক্রিপশন",
                            color = palette.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                        Text(
                            text = "ধাপ ২-এ X25519 + Ed25519 এন্ড-টু-এন্ড এনক্রিপশন যুক্ত হবে",
                            color = palette.textCaption,
                            fontSize = 10.5.sp,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Amber50)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ধাপ ২",
                            color = Amber700,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }

                SettingsSwitchRow(
                    title = "অ্যাপ লক / PIN সুরক্ষা",
                    subtitle = "অ্যাপ ওপেন করার সময় PIN যাচাই (বর্তমান: ${settings.appPinCode})",
                    checked = settings.appLockEnabled,
                    onCheckedChange = onAppLockChange
                )

                if (settings.appLockEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "PIN পরিবর্তন করুন →",
                            color = Sky600,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily,
                            modifier = Modifier
                                .clickable { showPinDialog = true }
                                .padding(4.dp)
                        )
                    }
                }

                SettingsSwitchRow(
                    title = "রাডারে নিজের উপস্থিতি লুকান (Stealth)",
                    subtitle = "অন্যদের স্ক্যানে আপনার নাম গোপন রাখুন",
                    checked = settings.stealthModeEnabled,
                    onCheckedChange = onStealthChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মেশ Node ID রোটেশন",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.subtleSurface)
                            .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
                            .clickable { onRotateNodeId() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "নতুন Node ID তৈরি করুন",
                            color = palette.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }
            }
        }

        // 4. ভাষা, থিম ও অ্যানিমেশন
        if (matches("ভাষা language থিম theme ডার্ক dark লাইট light অ্যানিমেশন animation স্লাইড স্প্ল্যাশ")) {
            SettingsSectionCard(
                title = "৪. ভাষা, থিম ও অ্যানিমেশন",
                titleColor = Sky700,
                badge = "UI & MOTION"
            ) {
                SettingsDropdownRow(
                    title = "ভাষা মোড (Language)",
                    selectedLabel = settings.languageMode,
                    accentColor = Sky600,
                    options = listOf(
                        "বাংলা + English" to "বাংলা + English (টেকনিক্যাল টার্মসহ)",
                        "শুধুমাত্র বাংলা" to "শুধুমাত্র বাংলা",
                        "English" to "English"
                    ),
                    onSelected = onLanguageChange
                )

                SettingsSwitchRow(
                    title = "ডার্ক থিম (Dark Mode)",
                    subtitle = "কম আলোতে আরামদায়ক ডার্ক ইন্টারফেস",
                    checked = settings.darkThemeEnabled,
                    onCheckedChange = onDarkThemeChange,
                    testTag = "dark_theme_switch"
                )

                SettingsSwitchRow(
                    title = "স্প্ল্যাশ ও স্লাইড অ্যানিমেশন",
                    subtitle = "স্মুথ ট্রানজিশন ও রাডার অ্যানিমেশন",
                    checked = settings.animationsEnabled,
                    onCheckedChange = onAnimationsChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Sky50)
                            .border(1.dp, Sky200, RoundedCornerShape(12.dp))
                            .clickable { onReplaySplash() }
                            .padding(vertical = 9.dp)
                            .testTag("settings_replay_splash"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "স্প্ল্যাশ দেখুন",
                            color = Sky700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Emerald50)
                            .border(1.dp, Emerald100, RoundedCornerShape(12.dp))
                            .clickable { onReplayOnboarding() }
                            .padding(vertical = 9.dp)
                            .testTag("settings_replay_onboarding"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "৬ স্লাইড দেখুন",
                            color = Emerald700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }
            }
        }

        // 5. ডিভাইস পারমিশন ও ডায়াগনস্টিক্স
        if (matches("পারমিশন permission ডায়াগনস্টিক্স diagnostics লগ log ব্লুটুথ ওয়াইফাই লোকেশন স্টোরেজ ব্যাটারি")) {
            SettingsSectionCard(
                title = "৫. ডিভাইস পারমিশন ও ডায়াগনস্টিক্স",
                titleColor = Emerald700,
                badge = "HARDWARE"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionChipTile("Bluetooth", permissionStatus.bluetoothGranted, context, Modifier.weight(1f))
                        PermissionChipTile("Wi-Fi / Nearby", permissionStatus.wifiHotspotGranted, context, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionChipTile("Nearby / Loc", permissionStatus.nearbyLocationGranted, context, Modifier.weight(1f))
                        PermissionChipTile("Storage", permissionStatus.storageSafReady, context, Modifier.weight(1f))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.subtleSurface)
                        .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
                        .clickable { showBatteryGuide = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ব্যাটারি অপ্টিমাইজেশন গাইড (Xiaomi/Oppo/Samsung)",
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = palette.textCaption)) { append("TX প্যাকেট: ") }
                            withStyle(SpanStyle(color = palette.textPrimary, fontWeight = FontWeight.Bold)) {
                                append(BanglaFormatters.toBanglaDigits(settings.txPacketCount))
                            }
                            withStyle(SpanStyle(color = palette.textCaption)) { append(" • RX: ") }
                            withStyle(SpanStyle(color = palette.textPrimary, fontWeight = FontWeight.Bold)) {
                                append(BanglaFormatters.toBanglaDigits(settings.rxPacketCount))
                            }
                        },
                        fontSize = 11.sp,
                        fontFamily = HindSiliguriFontFamily
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate900)
                            .clickable { onRunDiagnostics() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("settings_run_diagnostics")
                    ) {
                        Text(
                            text = "মেশ টেস্ট চালান",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }
            }
        }

        // 6. অ্যাপ সম্পর্কে
        if (matches("সম্পর্কে about আলাপ alap ভার্সন version নেটিভ native")) {
            SettingsSectionCard(
                title = "৬. অ্যাপ সম্পর্কে",
                titleColor = palette.textPrimary,
                badge = "v${BuildConfig.VERSION_NAME} NATIVE"
            ) {
                Text(
                    text = "আলাপ (ALAP) — অফলাইন মেশ কমিউনিকেশন অ্যাপ",
                    color = palette.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Text(
                    text = "আলাপ হলো একটি নেটিভ অফলাইন মেসেজিং অ্যাপ, যা ইন্টারনেট ছাড়াই ব্লুটুথ (BLE) মাল্টি-হপ মেশের মাধ্যমে আশেপাশের ডিভাইসে বার্তা পৌঁছে দেয়।",
                    color = palette.textSecondary,
                    fontSize = 11.5.sp,
                    fontFamily = HindSiliguriFontFamily,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showBatteryGuide) {
        AlertDialog(
            onDismissRequest = { showBatteryGuide = false },
            containerColor = palette.cardSurface,
            title = {
                Text(
                    text = "ব্যাকগ্রাউন্ড মেশ সচল রাখার গাইড",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily,
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = "• Xiaomi / POCO (HyperOS/MIUI): App Info → Battery saver → No restrictions ও Autostart চালু করুন।\n• Oppo / Realme: App Info → Battery usage → Allow background activity চালু করুন।\n• Samsung (One UI): Battery → Unrestricted নির্বাচন করুন।",
                    fontSize = 12.sp,
                    fontFamily = HindSiliguriFontFamily,
                    color = palette.textSecondary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryGuide = false
                    PermissionHelper.openAppSettings(context)
                }) {
                    Text("অ্যাপ সেটিংস খুলুন", fontFamily = HindSiliguriFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuide = false }) {
                    Text("বন্ধ করুন", fontFamily = HindSiliguriFontFamily)
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = palette.cardSurface,
            title = {
                Text("নতুন ৪-সংখ্যার PIN দিন", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = HindSiliguriFontFamily)
            },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                    placeholder = { Text("যেমন: 4589") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput.length == 4) {
                        onUpdatePin(pinInput)
                        pinInput = ""
                        showPinDialog = false
                    }
                }) {
                    Text("সংরক্ষণ করুন", fontFamily = HindSiliguriFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("বাতিল", fontFamily = HindSiliguriFontFamily)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    titleColor: Color,
    badge: String,
    content: @Composable () -> Unit
) {
    val palette = LocalAlapColors.current
    AlapSurfaceCard(cornerRadius = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Text(
                    text = badge,
                    color = palette.textPlaceholder,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSansFontFamily
                )
            }
            HorizontalDivider(color = palette.divider)
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    val palette = LocalAlapColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )
            Text(
                text = subtitle,
                color = palette.textCaption,
                fontSize = 10.5.sp,
                fontFamily = HindSiliguriFontFamily
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AlapMint
            ),
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun SettingsDropdownRow(
    title: String,
    selectedLabel: String,
    accentColor: Color,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    val palette = LocalAlapColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = HindSiliguriFontFamily
            )
            Text(
                text = selectedLabel,
                color = accentColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSansFontFamily
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.subtleSurface)
                    .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = options.find { it.first == selectedLabel }?.second ?: selectedLabel,
                    color = palette.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = HindSiliguriFontFamily,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = palette.textCaption, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(palette.cardSurface)
            ) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = display,
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = HindSiliguriFontFamily
                            )
                        },
                        onClick = {
                            onSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionChipTile(
    label: String,
    granted: Boolean,
    context: Context,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlapColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.subtleSurface)
            .border(1.dp, palette.subtleBorder, RoundedCornerShape(12.dp))
            .clickable { PermissionHelper.openAppSettings(context) }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSansFontFamily
        )
        Text(
            text = if (granted) "GRANTED" else "DENIED",
            color = if (granted) Emerald600 else Rose600,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSansFontFamily
        )
    }
}
