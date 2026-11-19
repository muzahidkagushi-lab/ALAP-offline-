package com.alap.mesh.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.alapDataStore: DataStore<Preferences> by preferencesDataStore(name = "alap_preferences_v2")

data class AlapSettings(
    val onboardingCompleted: Boolean = false,
    val displayName: String = "ব্যবহারকারী",
    val nodeId: String = "",
    val statusText: String = "Available Offline • Relay Active",
    val bleScanInterval: String = "Balanced (3s)",
    val wifiBoostEnabled: Boolean = true,
    val maxHopLimit: Int = 4,
    val relayModeEnabled: Boolean = true,
    val autoDownloadMedia: Boolean = true,
    val defaultTransferMode: String = "Auto (Smart)",
    val showHopIndicator: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appPinCode: String = "1234",
    val stealthModeEnabled: Boolean = false,
    val languageMode: String = "বাংলা + English",
    val darkThemeEnabled: Boolean = false,
    val animationsEnabled: Boolean = true,
    val relayedMessageCount: Int = 0,
    val sharedFilesCount: Int = 0,
    val txPacketCount: Int = 0,
    val rxPacketCount: Int = 0
)

class DataStoreManager internal constructor(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.alapDataStore)

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val NODE_ID = stringPreferencesKey("node_id")
        val STATUS_TEXT = stringPreferencesKey("status_text")
        val BLE_SCAN_INTERVAL = stringPreferencesKey("ble_scan_interval")
        val WIFI_BOOST_ENABLED = booleanPreferencesKey("wifi_boost_enabled")
        val MAX_HOP_LIMIT = intPreferencesKey("max_hop_limit")
        val RELAY_MODE_ENABLED = booleanPreferencesKey("relay_mode_enabled")
        val AUTO_DOWNLOAD_MEDIA = booleanPreferencesKey("auto_download_media")
        val DEFAULT_TRANSFER_MODE = stringPreferencesKey("default_transfer_mode")
        val SHOW_HOP_INDICATOR = booleanPreferencesKey("show_hop_indicator")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_PIN_CODE = stringPreferencesKey("app_pin_code")
        val STEALTH_MODE_ENABLED = booleanPreferencesKey("stealth_mode_enabled")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val RELAYED_COUNT = intPreferencesKey("relayed_count")
        val SHARED_FILES_COUNT = intPreferencesKey("shared_files_count")
        val TX_PACKET_COUNT = intPreferencesKey("tx_packet_count")
        val RX_PACKET_COUNT = intPreferencesKey("rx_packet_count")
    }

    val settingsFlow: Flow<AlapSettings> = store.data.map { prefs ->
        AlapSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME] ?: "ব্যবহারকারী",
            nodeId = prefs[Keys.NODE_ID] ?: "",
            statusText = prefs[Keys.STATUS_TEXT] ?: "Available Offline • Relay Active",
            bleScanInterval = prefs[Keys.BLE_SCAN_INTERVAL] ?: "Balanced (3s)",
            wifiBoostEnabled = prefs[Keys.WIFI_BOOST_ENABLED] ?: true,
            maxHopLimit = prefs[Keys.MAX_HOP_LIMIT] ?: 4,
            relayModeEnabled = prefs[Keys.RELAY_MODE_ENABLED] ?: true,
            autoDownloadMedia = prefs[Keys.AUTO_DOWNLOAD_MEDIA] ?: true,
            defaultTransferMode = prefs[Keys.DEFAULT_TRANSFER_MODE] ?: "Auto (Smart)",
            showHopIndicator = prefs[Keys.SHOW_HOP_INDICATOR] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appPinCode = prefs[Keys.APP_PIN_CODE] ?: "1234",
            stealthModeEnabled = prefs[Keys.STEALTH_MODE_ENABLED] ?: false,
            languageMode = prefs[Keys.LANGUAGE_MODE] ?: "বাংলা + English",
            darkThemeEnabled = prefs[Keys.DARK_THEME_ENABLED] ?: false,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            relayedMessageCount = prefs[Keys.RELAYED_COUNT] ?: 0,
            sharedFilesCount = prefs[Keys.SHARED_FILES_COUNT] ?: 0,
            txPacketCount = prefs[Keys.TX_PACKET_COUNT] ?: 0,
            rxPacketCount = prefs[Keys.RX_PACKET_COUNT] ?: 0
        )
    }

    suspend fun ensureNodeIdInitialized(): String {
        val existing = store.data.first()[Keys.NODE_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = generateRandomNodeId()
        store.edit { prefs ->
            if (prefs[Keys.NODE_ID].isNullOrBlank()) {
                prefs[Keys.NODE_ID] = generated
            }
        }
        return store.data.first()[Keys.NODE_ID] ?: generated
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateDisplayName(name: String) {
        store.edit { it[Keys.DISPLAY_NAME] = name.trim() }
    }

    suspend fun updateStatusText(status: String) {
        store.edit { it[Keys.STATUS_TEXT] = status }
    }

    suspend fun rotateNodeId(): String {
        val newId = generateRandomNodeId()
        store.edit { it[Keys.NODE_ID] = newId }
        return newId
    }

    internal suspend fun setNodeIdForTest(nodeId: String) {
        store.edit { it[Keys.NODE_ID] = nodeId }
    }

    suspend fun setBleScanInterval(interval: String) {
        store.edit { it[Keys.BLE_SCAN_INTERVAL] = interval }
    }

    suspend fun setWifiBoostEnabled(enabled: Boolean) {
        store.edit { it[Keys.WIFI_BOOST_ENABLED] = enabled }
    }

    suspend fun setMaxHopLimit(hops: Int) {
        store.edit { it[Keys.MAX_HOP_LIMIT] = hops.coerceIn(1, 6) }
    }

    suspend fun setRelayModeEnabled(enabled: Boolean) {
        store.edit { it[Keys.RELAY_MODE_ENABLED] = enabled }
    }

    suspend fun setAutoDownloadMedia(enabled: Boolean) {
        store.edit { it[Keys.AUTO_DOWNLOAD_MEDIA] = enabled }
    }

    suspend fun setDefaultTransferMode(mode: String) {
        store.edit { it[Keys.DEFAULT_TRANSFER_MODE] = mode }
    }

    suspend fun setShowHopIndicator(show: Boolean) {
        store.edit { it[Keys.SHOW_HOP_INDICATOR] = show }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        store.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppPinCode(pin: String) {
        store.edit { it[Keys.APP_PIN_CODE] = pin }
    }

    suspend fun setStealthModeEnabled(enabled: Boolean) {
        store.edit { it[Keys.STEALTH_MODE_ENABLED] = enabled }
    }

    suspend fun setLanguageMode(lang: String) {
        store.edit { it[Keys.LANGUAGE_MODE] = lang }
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        store.edit { it[Keys.DARK_THEME_ENABLED] = enabled }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        store.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun incrementTxPacket(by: Int = 1) {
        store.edit {
            it[Keys.TX_PACKET_COUNT] = (it[Keys.TX_PACKET_COUNT] ?: 0) + by
        }
    }

    suspend fun incrementRxPacket(by: Int = 1) {
        store.edit {
            it[Keys.RX_PACKET_COUNT] = (it[Keys.RX_PACKET_COUNT] ?: 0) + by
        }
    }

    suspend fun incrementRelayedCount(by: Int = 1) {
        store.edit {
            it[Keys.RELAYED_COUNT] = (it[Keys.RELAYED_COUNT] ?: 0) + by
        }
    }

    suspend fun incrementSharedFilesCount(by: Int = 1) {
        store.edit {
            it[Keys.SHARED_FILES_COUNT] = (it[Keys.SHARED_FILES_COUNT] ?: 0) + by
        }
    }

    companion object {
        fun generateRandomNodeId(): String {
            val hex = UUID.randomUUID().toString().replace("-", "").uppercase()
            return "ALAP-${hex.substring(0, 4)}-${hex.substring(4, 7)}"
        }
    }
}
