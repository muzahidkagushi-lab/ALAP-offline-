package com.alap.mesh.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alap.mesh.data.AlapSettings
import com.alap.mesh.mesh.HardwarePermissionStatus
import com.alap.mesh.ui.AlapViewModel
import com.alap.mesh.ui.MainTab
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.LocalAlapColors
import com.alap.mesh.ui.theme.Sky600

private data class NavTabItem(
    val tab: MainTab,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
)

private val NavItems = listOf(
    NavTabItem(MainTab.CHATS, "চ্যাট", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chats"),
    NavTabItem(MainTab.RADAR, "রাডার", Icons.Rounded.Sensors, Icons.Outlined.Sensors, "nav_radar"),
    NavTabItem(MainTab.FILES, "ফাইলস", Icons.Rounded.Folder, Icons.Outlined.Folder, "nav_files"),
    NavTabItem(MainTab.PROFILE, "প্রোফাইল", Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile"),
    NavTabItem(MainTab.SETTINGS, "সেটিংস", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun MainAppShell(
    viewModel: AlapViewModel,
    settings: AlapSettings,
    permissionStatus: HardwarePermissionStatus
) {
    val palette = LocalAlapColors.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val meshPeers by viewModel.meshPeers.collectAsStateWithLifecycle()
    val fileTransfers by viewModel.fileTransfers.collectAsStateWithLifecycle()
    val chatFilter by viewModel.chatFilter.collectAsStateWithLifecycle()
    val fileFilter by viewModel.fileFilter.collectAsStateWithLifecycle()
    val radarMode by viewModel.radarMode.collectAsStateWithLifecycle()
    val selectedPeerId by viewModel.selectedPeerId.collectAsStateWithLifecycle()
    val radarPulseTrigger by viewModel.radarPulseTrigger.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()

    if (activeTab != MainTab.CHATS && activeChatId == null) {
        BackHandler { viewModel.switchTab(MainTab.CHATS) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = palette.navBarSurface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NavItems.forEach { item ->
                        val selected = activeTab == item.tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.switchTab(item.tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.label,
                                    tint = if (selected) Sky600 else palette.textCaption
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    color = if (selected) Sky600 else palette.textCaption,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = HindSiliguriFontFamily
                                )
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .then(if (!useRail) Modifier else Modifier.navigationBarsPadding())
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { 8.dp.value.toInt() })
                                .togetherWith(fadeOut(tween(180)))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp),
                        label = "mainTabContent"
                    ) { tab ->
                        when (tab) {
                            MainTab.CHATS -> ChatsTab(
                                conversations = conversations,
                                peers = meshPeers,
                                activeFilter = chatFilter,
                                onSelectFilter = viewModel::setChatFilter,
                                onOpenConversation = viewModel::openChatRoom,
                                onOpenPeerChat = viewModel::openChatWithPeer,
                                onNavigateToRadar = { viewModel.switchTab(MainTab.RADAR) },
                                onSendMeshPing = viewModel::sendMeshPing,
                                onSendSosBroadcast = viewModel::sendSosBroadcast
                            )
                            MainTab.RADAR -> RadarTab(
                                peers = meshPeers,
                                localNodeId = settings.nodeId,
                                radioMode = radarMode,
                                selectedPeerId = selectedPeerId,
                                pulseTrigger = radarPulseTrigger,
                                onSelectRadioMode = viewModel::setRadarMode,
                                onRescan = viewModel::triggerRadarScan,
                                onSelectPeer = viewModel::selectRadarNode,
                                onStartChatWithPeer = viewModel::openChatWithPeer
                            )
                            MainTab.FILES -> FilesTab(
                                files = fileTransfers,
                                activeFilter = fileFilter,
                                onSelectFilter = viewModel::setFileFilter,
                                onPickFileToSend = { uri -> viewModel.sendPickedFile(uri) }
                            )
                            MainTab.PROFILE -> ProfileTab(
                                settings = settings,
                                fingerprint = viewModel.getPublicFingerprint(settings.nodeId),
                                onNavigateToSettings = { viewModel.switchTab(MainTab.SETTINGS) },
                                onSaveDisplayName = viewModel::updateDisplayName,
                                onSelectStatus = viewModel::updateStatus
                            )
                            MainTab.SETTINGS -> SettingsTab(
                                settings = settings,
                                permissionStatus = permissionStatus,
                                onBleIntervalChange = viewModel::updateBleInterval,
                                onWifiBoostChange = viewModel::toggleWifiBoost,
                                onMaxHopChange = viewModel::updateMaxHopLimit,
                                onRelayModeChange = viewModel::toggleRelayMode,
                                onAutoDownloadChange = viewModel::toggleAutoDownload,
                                onTransferModeChange = viewModel::updateTransferMode,
                                onHopIndicatorChange = viewModel::toggleHopIndicator,
                                onClearCache = viewModel::clearOfflineCache,
                                onAppLockChange = viewModel::toggleAppLock,
                                onUpdatePin = viewModel::updateAppPin,
                                onStealthChange = viewModel::toggleStealthMode,
                                onRotateNodeId = viewModel::rotateNodeId,
                                onLanguageChange = viewModel::updateLanguageMode,
                                onDarkThemeChange = viewModel::toggleDarkTheme,
                                onAnimationsChange = viewModel::toggleAnimations,
                                onReplaySplash = viewModel::replaySplashFromSettings,
                                onReplayOnboarding = viewModel::replayOnboardingFromSettings,
                                onRunDiagnostics = viewModel::runDiagnosticsTest
                            )
                        }
                    }
                }

                if (!useRail) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.navBarSurface)
                            .border(1.dp, palette.navBarBorder)
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavItems.forEach { item ->
                            val selected = activeTab == item.tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.switchTab(item.tab) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag(item.testTag)
                            ) {
                                Icon(
                                    imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.label,
                                    tint = if (selected) Sky600 else palette.textCaption,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    color = if (selected) Sky600 else palette.textCaption,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = HindSiliguriFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Chat Room Overlay
        val currentConv = conversations.find { it.id == activeChatId }
        AnimatedVisibility(
            visible = currentConv != null,
            enter = fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 3 },
            exit = fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { it / 3 }
        ) {
            if (currentConv != null) {
                val roomMessages by viewModel.observeMessages(currentConv.id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                ChatRoomScreen(
                    conversation = currentConv,
                    messages = roomMessages,
                    localNodeId = settings.nodeId,
                    showHopIndicator = settings.showHopIndicator,
                    onBack = viewModel::closeChatRoom,
                    onSendMessage = { text -> viewModel.sendChatMessage(currentConv.id, text) },
                    onSendPickedUri = { uri ->
                        viewModel.sendPickedFile(uri, peerName = currentConv.name, conversationId = currentConv.id)
                    }
                )
            }
        }
    }
}
