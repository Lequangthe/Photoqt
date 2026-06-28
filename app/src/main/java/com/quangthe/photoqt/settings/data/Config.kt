package com.quangthe.photoqt.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quangthe.photoqt.settings.domain.models.StartPage
import com.quangthe.photoqt.settings.domain.models.SystemDesignEnum
import com.quangthe.photoqt.telemetry.domain.TelemetryEnabledByDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photoqt_preferences")

class Config(context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val dataStore = context.dataStore

    private val cache = mutableMapOf<String, Any?>()

    init {
        runBlocking {
            try {
                val prefs = dataStore.data.first()
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
            } catch (_: Exception) {
                // Ignore errors during init
            }
        }

        coroutineScope.launch {
            dataStore.data.collect { prefs ->
                synchronized(cache) {
                    cache.clear()
                    prefs.asMap().forEach { (key, value) ->
                        cache[key.name] = value
                    }
                }
            }
        }
    }

    val values: Map<String, *>
        get() = synchronized(cache) { cache.toMap() }

    val valuesFlow: Flow<Map<String, *>> = dataStore.data.map { prefs ->
        prefs.asMap().mapKeys { it.key.name }
    }

    var systemFirstStart: Boolean
        get() = getBoolean(SYSTEM_FIRST_START, SYSTEM_FIRST_START_DEFAULT)
        set(value) = putBoolean(SYSTEM_FIRST_START, value)

    var systemLastFeatureVersionCode: Int
        get() = getInt(SYSTEM_LAST_FEATURE_VERSION_CODE, SYSTEM_LAST_FEATURE_VERSION_CODE_DEFAULT)
        set(value) = putInt(SYSTEM_LAST_FEATURE_VERSION_CODE, value)

    var systemDesign: SystemDesignEnum
        get() = SystemDesignEnum.fromValue(getString(SYSTEM_DESIGN, SYSTEM_DESIGN_DEFAULT.value))
        set(value) = putString(SYSTEM_DESIGN, value.value)

    var galleryStartPage: StartPage
        get() = StartPage.fromValue(getString(GALLERY_START_PAGE, GALLERY_START_PAGE_DEFAULT.value))
        set(value) = putString(GALLERY_START_PAGE, value.value)

    var securityAllowScreenshots: Boolean
        get() = getBoolean(SECURITY_ALLOW_SCREENSHOTS, SECURITY_ALLOW_SCREENSHOTS_DEFAULT)
        set(value) = putBoolean(SECURITY_ALLOW_SCREENSHOTS, value)

    var securityLockTimeout: Int
        get() = getIntFromString(SECURITY_LOCK_TIMEOUT, SECURITY_LOCK_TIMEOUT_DEFAULT)
        set(value) = putString(SECURITY_LOCK_TIMEOUT, value.toString())

    var securityDialLaunchCode: String?
        get() = getString(SECURITY_DIAL_LAUNCH_CODE, SECURITY_DIAL_LAUNCH_CODE_DEFAULT)
        set(value) = putString(SECURITY_DIAL_LAUNCH_CODE, value!!)

    var deleteImportedFiles: Boolean
        get() = getBoolean(ADVANCED_DELETE_IMPORTED_FILES, ADVANCED_DELETE_IMPORTED_FILES_DEFAULT)
        set(value) = putBoolean(ADVANCED_DELETE_IMPORTED_FILES, value)

    var deleteExportedFiles: Boolean
        get() = getBoolean(ADVANCED_DELETE_EXPORTED_FILES, ADVANCED_DELETE_EXPORTED_FILES_DEFAULT)
        set(value) = putBoolean(ADVANCED_DELETE_EXPORTED_FILES, value)

    var timestampLastRecoveryStart: Long
        get() = getLong(TIMESTAMP_LAST_RECOVERY_START, TIMESTAMP_LAST_RECOVERY_START_DEFAULT)
        set(value) = putLong(TIMESTAMP_LAST_RECOVERY_START, value)

    var biometricAuthenticationEnabled: Boolean
        get() = getBoolean(SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED, SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED_DEFAULT)
        set(value) = putBoolean(SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED, value)

    var imageViewerLoopVideos: Boolean
        get() = getBoolean(IMAGE_VIEWER_LOOP_VIDEO, IMAGE_VIEWER_LOOP_VIDEO_DEFAULT)
        set(value) = putBoolean(IMAGE_VIEWER_LOOP_VIDEO, value)

    var imageViewerMuteVideoPlayer: Boolean
        get() = getBoolean(IMAGE_VIEWER_MUTE_VIDEO_PLAYER, IMAGE_VIEWER_MUTE_VIDEO_PLAYER_DEFAULT)
        set(value) = putBoolean(IMAGE_VIEWER_MUTE_VIDEO_PLAYER, value)

    var imageViewerPlaybackSpeed: Float
        get() = getFloat(IMAGE_VIEWER_PLAYBACK_SPEED, IMAGE_VIEWER_PLAYBACK_SPEED_DEFAULT)
        set(value) = putFloat(IMAGE_VIEWER_PLAYBACK_SPEED, value)

    var telemetryEnabled: Boolean
        get() = getBoolean(TELEMETRY_ENABLED, TelemetryEnabledByDefault)
        set(value) = putBoolean(TELEMETRY_ENABLED, value)

    var telemetryAskedForOptIn: Boolean
        get() = getBoolean(TELEMETRY_ASKED_FOR_OPT_IN, TELEMETRY_ASKED_FOR_OPT_IN_DEFAULT)
        set(value) = putBoolean(TELEMETRY_ASKED_FOR_OPT_IN, value)

    // In memory flags
    var justFinishedSetup: Boolean = false

    // --- Legacy

    var legacyCurrentlyMigrating: Boolean
        get() = getBoolean("legacy^currentlyMigrating", false)
        set(value) = putBoolean("legacy^currentlyMigrating", value)

    @Deprecated("Only needed for migration")
    var legacyPasswordHash: String?
        get() = getString(SECURITY_PASSWORD, SECURITY_PASSWORD_DEFAULT)
        set(value) = putString(SECURITY_PASSWORD, value)

    @Deprecated("Only needed for migration")
    var legacyUserSalt: String?
        get() = getString("user^salt", null)
        set(value) = putString("user^salt", value)

    // --- helpers

    fun getString(key: String, default: String?): String? {
        synchronized(cache) { return cache[key] as? String ?: default }
    }

    fun getInt(key: String, default: Int): Int {
        synchronized(cache) { return (cache[key] as? Int) ?: default }
    }

    fun getIntFromString(key: String, default: Int): Int {
        synchronized(cache) {
            return (cache[key] as? String)?.toIntOrNull()
                ?: (cache[key] as? Int)
                ?: default
        }
    }

    fun getLong(key: String, default: Long): Long {
        synchronized(cache) { return (cache[key] as? Long) ?: default }
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        synchronized(cache) { return (cache[key] as? Boolean) ?: default }
    }

    fun getFloat(key: String, default: Float): Float {
        synchronized(cache) { return (cache[key] as? Float) ?: default }
    }

    fun putString(key: String, value: String?) {
        synchronized(cache) { cache[key] = value }
        coroutineScope.launch {
            dataStore.edit { prefs ->
                if (value != null) prefs[stringPreferencesKey(key)] = value
                else prefs.remove(stringPreferencesKey(key))
            }
        }
    }

    fun putInt(key: String, value: Int) {
        synchronized(cache) { cache[key] = value }
        coroutineScope.launch {
            dataStore.edit { prefs ->
                prefs[intPreferencesKey(key)] = value
            }
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        synchronized(cache) { cache[key] = value }
        coroutineScope.launch {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(key)] = value
            }
        }
    }

    fun putLong(key: String, value: Long) {
        synchronized(cache) { cache[key] = value }
        coroutineScope.launch {
            dataStore.edit { prefs ->
                prefs[longPreferencesKey(key)] = value
            }
        }
    }

    fun putFloat(key: String, value: Float) {
        synchronized(cache) { cache[key] = value }
        coroutineScope.launch {
            dataStore.edit { prefs ->
                prefs[floatPreferencesKey(key)] = value
            }
        }
    }

    companion object {
        const val FILE_NAME = "photoqt_preferences"

        const val SYSTEM_FIRST_START = "system^firstStart"
        const val SYSTEM_FIRST_START_DEFAULT = true

        const val SYSTEM_LAST_FEATURE_VERSION_CODE = "system^lastFeatureVersionCode"
        const val SYSTEM_LAST_FEATURE_VERSION_CODE_DEFAULT = 0

        const val SYSTEM_DESIGN = "system^design"
        val SYSTEM_DESIGN_DEFAULT = SystemDesignEnum.System

        const val GALLERY_START_PAGE = "gallery^startPage"
        val GALLERY_START_PAGE_DEFAULT = StartPage.AllFiles

        const val SECURITY_ALLOW_SCREENSHOTS = "security^allowScreenshots"
        const val SECURITY_ALLOW_SCREENSHOTS_DEFAULT = false

        const val SECURITY_PASSWORD = "security^password"
        const val SECURITY_PASSWORD_DEFAULT = ""

        const val SECURITY_LOCK_TIMEOUT = "security^lockTimeout"
        const val SECURITY_LOCK_TIMEOUT_DEFAULT = 300000

        const val SECURITY_DIAL_LAUNCH_CODE = "security^dialLaunchCode"
        const val SECURITY_DIAL_LAUNCH_CODE_DEFAULT = "1337"

        const val ADVANCED_DELETE_IMPORTED_FILES = "advanced^deleteImportedFiles"
        const val ADVANCED_DELETE_IMPORTED_FILES_DEFAULT = false

        const val ADVANCED_DELETE_EXPORTED_FILES = "advanced^deleteExportedFiles"
        const val ADVANCED_DELETE_EXPORTED_FILES_DEFAULT = false

        const val TIMESTAMP_LAST_RECOVERY_START = "internal^timestampLastRecoveryStart"
        const val TIMESTAMP_LAST_RECOVERY_START_DEFAULT = 0L

        const val SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED = "security^biometricAuthenticationEnabled"
        const val SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED_DEFAULT = false

        const val IMAGE_VIEWER_LOOP_VIDEO = "imageViewer^loopVideo"
        const val IMAGE_VIEWER_LOOP_VIDEO_DEFAULT = false

        const val IMAGE_VIEWER_MUTE_VIDEO_PLAYER = "imageViewer^muteVideoPlayer"
        const val IMAGE_VIEWER_MUTE_VIDEO_PLAYER_DEFAULT = false

        const val IMAGE_VIEWER_PLAYBACK_SPEED = "imageViewer^playbackSpeed"
        const val IMAGE_VIEWER_PLAYBACK_SPEED_DEFAULT = 1f

        const val TELEMETRY_ENABLED = "telemetry^enabled"

        const val TELEMETRY_ASKED_FOR_OPT_IN = "telemetry^askedForOptIn"
        const val TELEMETRY_ASKED_FOR_OPT_IN_DEFAULT = false
    }
}