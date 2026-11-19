package com.alap.mesh.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alap.mesh.data.AlapSettings
import com.alap.mesh.data.local.ConversationEntity
import com.alap.mesh.data.local.FileTransferEntity
import com.alap.mesh.data.local.MeshPeerEntity
import com.alap.mesh.data.local.MessageEntity
import com.alap.mesh.mesh.AlapMeshRepository
import com.alap.mesh.mesh.HardwarePermissionStatus
import com.alap.mesh.mesh.PermissionHelper
import com.alap.mesh.mesh.ble.BleRadioState
import com.alap.mesh.service.AlapMeshService
import com.alap.mesh.util.BanglaFormatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRouteStage {
    SPLASH,
    ONBOARDING,
    MAIN_APP
}

enum class MainTab {
    CHATS,
    RADAR,
    FILES,
    PROFILE,
    SETTINGS
}

enum class ChatFilter {
    ALL,
    DIRECT,
    GROUP,
    FILES
}

enum class FileFilter {
    ALL,
    SENT,
    RECEIVED
}

enum class RadarRadioMode {
    BLE,
    WIFI_BOOST
}

class AlapViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AlapMeshRepository.getInstance(application.applicationContext)

    val settings: StateFlow<AlapSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlapSettings()
    )

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val fileTransfers: StateFlow<List<FileTransferEntity>> = repository.fileTransfersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val meshPeers: StateFlow<List<MeshPeerEntity>> = repository.meshPeersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val radioState: StateFlow<BleRadioState> = repository.radioStateFlow

    private val _routeStage = MutableStateFlow(AppRouteStage.SPLASH)
    val routeStage: StateFlow<AppRouteStage> = _routeStage.asStateFlow()

    private val _isReplayOnboarding = MutableStateFlow(false)

    private val _activeTab = MutableStateFlow(MainTab.CHATS)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _chatFilter = MutableStateFlow(ChatFilter.ALL)
    val chatFilter: StateFlow<ChatFilter> = _chatFilter.asStateFlow()

    private val _fileFilter = MutableStateFlow(FileFilter.ALL)
    val fileFilter: StateFlow<FileFilter> = _fileFilter.asStateFlow()

    private val _radarMode = MutableStateFlow(RadarRadioMode.BLE)
    val radarMode: StateFlow<RadarRadioMode> = _radarMode.asStateFlow()

    private val _selectedPeerId = MutableStateFlow<String?>(null)
    val selectedPeerId: StateFlow<String?> = _selectedPeerId.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _permissionStatus = MutableStateFlow(
        PermissionHelper.getHardwarePermissionStatus(application.applicationContext)
    )
    val permissionStatus: StateFlow<HardwarePermissionStatus> = _permissionStatus.asStateFlow()

    private val _radarPulseTrigger = MutableStateFlow(0)
    val radarPulseTrigger: StateFlow<Int> = _radarPulseTrigger.asStateFlow()

    private var toastJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.initialize()
            val initialSettings = repository.settingsFlow.first()
            if (initialSettings.appLockEnabled) {
                _isLocked.value = true
            }
            if (initialSettings.onboardingCompleted) {
                repository.startMeshEngine()
                if (PermissionHelper.hasBluetoothPermissions(application.applicationContext) &&
                    PermissionHelper.isBluetoothEnabled(application.applicationContext)
                ) {
                    AlapMeshService.startOrUpdate(application.applicationContext, 0)
                }
            }
        }
    }

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        repository.observeMessages(conversationId)

    fun showToast(message: String) {
        toastJob?.cancel()
        _toastMessage.value = message
        toastJob = viewModelScope.launch {
            delay(2600)
            _toastMessage.value = null
        }
    }

    fun onSplashFinished() {
        viewModelScope.launch {
            val completed = repository.settingsFlow.first().onboardingCompleted
            _routeStage.value = if (completed && !_isReplayOnboarding.value) {
                AppRouteStage.MAIN_APP
            } else {
                AppRouteStage.ONBOARDING
            }
        }
    }

    fun replaySplashFromSettings() {
        _isReplayOnboarding.value = false
        _routeStage.value = AppRouteStage.SPLASH
    }

    fun replayOnboardingFromSettings() {
        _isReplayOnboarding.value = true
        _routeStage.value = AppRouteStage.ONBOARDING
    }

    fun completeOnboardingAndEnterApp(permissionsGranted: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setOnboardingCompleted(true)
            _isReplayOnboarding.value = false
            refreshPermissions()
            repository.startMeshEngine()
            if (permissionsGranted) {
                AlapMeshService.startOrUpdate(getApplication(), 0)
            }
            _routeStage.value = AppRouteStage.MAIN_APP
            _activeTab.value = MainTab.CHATS
            showToast(
                if (permissionsGranted) "ব্লুটুথ মেশ রেডিও চালু হয়েছে!"
                else "অফলাইন মেশ প্রস্তুত! পূর্ণ স্ক্যানের জন্য সেটিংস থেকে অনুমতি দিন"
            )
        }
    }

    fun refreshPermissions() {
        _permissionStatus.value = PermissionHelper.getHardwarePermissionStatus(getApplication())
    }

    fun switchTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun setChatFilter(filter: ChatFilter) {
        _chatFilter.value = filter
    }

    fun setFileFilter(filter: FileFilter) {
        _fileFilter.value = filter
    }

    fun setRadarMode(mode: RadarRadioMode) {
        _radarMode.value = mode
        showToast(
            if (mode == RadarRadioMode.BLE) "ব্লুটুথ লো-পাওয়ার মেশ স্ক্যান সক্রিয়"
            else "হাই-স্পিড লিংক ধাপ ২-এ Nearby Connections দিয়ে সক্রিয় হবে"
        )
    }

    fun selectRadarNode(nodeId: String) {
        _selectedPeerId.value = nodeId
    }

    fun triggerRadarScan() {
        viewModelScope.launch {
            _radarPulseTrigger.value += 1
            val count = repository.rescanPeers()
            showToast("আশেপাশের মেশ নোড স্ক্যান সম্পন্ন (${toBanglaDigits(count)}টি নোড)")
        }
    }

    fun openChatRoom(conversationId: String) {
        _activeChatId.value = conversationId
        viewModelScope.launch {
            repository.markConversationRead(conversationId)
        }
    }

    fun openChatWithPeer(peer: MeshPeerEntity) {
        viewModelScope.launch {
            val convId = repository.openOrCreatePeerConversation(peer)
            _activeChatId.value = convId
        }
    }

    fun closeChatRoom() {
        _activeChatId.value = null
    }

    fun sendChatMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            repository.sendMessage(conversationId, text)
        }
    }

    fun sendMeshPing() {
        viewModelScope.launch {
            _radarPulseTrigger.value += 1
            val msg = repository.sendMeshPing()
            showToast(msg)
        }
    }

    fun sendSosBroadcast(note: String = "") {
        viewModelScope.launch {
            repository.sendSosBroadcast(note)
            showToast("সর্বোচ্চ হপ সীমায় SOS জরুরি বার্তা মেশে পাঠানো হয়েছে!")
        }
    }

    fun sendPickedFile(uri: Uri, peerName: String = "কাছাকাছি মেশ ডিভাইস", conversationId: String? = null) {
        viewModelScope.launch {
            val msg = repository.sendFileFromUri(uri, peerName, conversationId)
            showToast(msg)
        }
    }

    fun updateDisplayName(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.dataStore.updateDisplayName(clean)
            repository.startMeshEngine()
            showToast("প্রোফাইল নাম আপডেট করা হয়েছে")
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch {
            repository.dataStore.updateStatusText(status)
            showToast("স্ট্যাটাস: $status")
        }
    }

    fun updateBleInterval(interval: String) {
        viewModelScope.launch {
            repository.dataStore.setBleScanInterval(interval)
            repository.startMeshEngine()
            showToast("ব্লুটুথ স্ক্যান ইন্টারভাল আপডেট হয়েছে")
        }
    }

    fun toggleWifiBoost(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setWifiBoostEnabled(enabled)
            showToast("হাই-স্পিড লিংক পছন্দ ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে")
        }
    }

    fun updateMaxHopLimit(hops: Int) {
        viewModelScope.launch {
            repository.dataStore.setMaxHopLimit(hops)
            showToast("সর্বোচ্চ মেশ হপ ${toBanglaDigits(hops)} ধাপে নির্ধারিত হয়েছে")
        }
    }

    fun toggleRelayMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setRelayModeEnabled(enabled)
            showToast("রিলে / হপ নোড মোড ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"}")
        }
    }

    fun toggleAutoDownload(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setAutoDownloadMedia(enabled)
            showToast("অটো-ডাউনলোড মিডিয়া ${if (enabled) "চালু" else "বন্ধ"}")
        }
    }

    fun updateTransferMode(mode: String) {
        viewModelScope.launch {
            repository.dataStore.setDefaultTransferMode(mode)
            showToast("ডিফল্ট ট্রান্সফার মোড পরিবর্তন হয়েছে")
        }
    }

    fun toggleHopIndicator(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setShowHopIndicator(enabled)
            showToast("মেসেজ হপ-পাথ ইন্ডিকেটর ${if (enabled) "চালু" else "বন্ধ"}")
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            val msg = repository.clearCacheDirectory()
            showToast(msg)
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setAppLockEnabled(enabled)
            showToast("অ্যাপ লক / PIN সুরক্ষা ${if (enabled) "চালু (ডিফল্ট PIN: 1234)" else "বন্ধ"}")
        }
    }

    fun updateAppPin(newPin: String) {
        if (newPin.length != 4) return
        viewModelScope.launch {
            repository.dataStore.setAppPinCode(newPin)
            showToast("নতুন ৪-সংখ্যার PIN সংরক্ষিত হয়েছে")
        }
    }

    fun unlockWithPin(entered: String): Boolean {
        return if (entered == settings.value.appPinCode) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun toggleStealthMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setStealthModeEnabled(enabled)
            repository.startMeshEngine()
            showToast("Stealth মোড ${if (enabled) "চালু (পরিচয় গোপন, রিলে সক্রিয়)" else "বন্ধ"}")
        }
    }

    fun rotateNodeId() {
        viewModelScope.launch {
            val newId = repository.dataStore.rotateNodeId()
            repository.startMeshEngine()
            showToast("নতুন মেশ Node ID তৈরি হয়েছে: $newId")
        }
    }

    fun updateLanguageMode(lang: String) {
        viewModelScope.launch {
            repository.dataStore.setLanguageMode(lang)
            showToast("ভাষা মোড আপডেট করা হয়েছে")
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setDarkThemeEnabled(enabled)
            showToast(if (enabled) "ডার্ক থিম সক্রিয় হয়েছে" else "লাইট থিম সক্রিয় হয়েছে")
        }
    }

    fun toggleAnimations(enabled: Boolean) {
        viewModelScope.launch {
            repository.dataStore.setAnimationsEnabled(enabled)
            showToast("অ্যানিমেশন ${if (enabled) "চালু" else "বন্ধ"}")
        }
    }

    fun runDiagnosticsTest() {
        viewModelScope.launch {
            val result = repository.runDiagnosticsTest()
            showToast(result)
        }
    }

    fun getPublicFingerprint(nodeId: String): String = repository.getPublicFingerprint(nodeId)

    companion object {
        fun toBanglaDigits(num: Int): String = BanglaFormatters.toBanglaDigits(num)
    }
}
