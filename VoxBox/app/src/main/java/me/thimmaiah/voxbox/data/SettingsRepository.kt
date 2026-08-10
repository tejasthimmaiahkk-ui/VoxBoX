package me.thimmaiah.voxbox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.thimmaiah.voxbox.session.CaptureNotePolicy
import me.thimmaiah.voxbox.session.NoteDetail
import me.thimmaiah.voxbox.ui.theme.VbAccent

enum class VbThemeMode { System, Dark, Light }

enum class VbExportFormat(val label: String) {
    Zip("Markdown + assets (.zip)"),
    MarkdownOnly("Markdown only"),
}

/**
 * Everything the app remembers between launches that is not a note.
 *
 * Capture interval and threshold live here as *defaults*. The capture screen can override them
 * for a single session without writing back, so trying a different sensitivity in one lecture
 * does not silently change every future one.
 */
data class VbSettings(
    val theme: VbThemeMode = VbThemeMode.System,
    val accent: VbAccent = VbAccent.Blue,
    val readingSize: Int = 1,
    val readingSerif: Boolean = false,
    val focusMode: Boolean = false,
    val defaultPolicy: CaptureNotePolicy = CaptureNotePolicy.RUNNABLE,
    val defaultDetail: NoteDetail = NoteDetail.CONCISE,
    val captureIntervalSec: Int = 8,
    val changeThresholdPct: Int = 8,
    val keepRawFrames: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val exportFormat: VbExportFormat = VbExportFormat.Zip,
    val exportIncludeFlags: Boolean = true,
    val onboarded: Boolean = false,
    val consentAcknowledged: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("vb_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<VbSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun setTheme(mode: VbThemeMode) = put(Keys.theme, mode.name)
    suspend fun setAccent(accent: VbAccent) = put(Keys.accent, accent.name)
    suspend fun setReadingSize(index: Int) = put(Keys.readingSize, index.coerceIn(0, 3))
    suspend fun setReadingSerif(serif: Boolean) = put(Keys.readingSerif, serif)
    suspend fun setFocusMode(focus: Boolean) = put(Keys.focusMode, focus)
    suspend fun setDefaultPolicy(policy: CaptureNotePolicy) = put(Keys.policy, policy.name)
    suspend fun setDefaultDetail(detail: NoteDetail) = put(Keys.detail, detail.name)
    suspend fun setCaptureInterval(seconds: Int) = put(Keys.interval, seconds.coerceIn(2, 30))
    suspend fun setChangeThreshold(percent: Int) = put(Keys.threshold, percent.coerceIn(2, 30))
    suspend fun setKeepRawFrames(keep: Boolean) = put(Keys.keepRawFrames, keep)
    suspend fun setKeepScreenAwake(keep: Boolean) = put(Keys.keepAwake, keep)
    suspend fun setExportFormat(format: VbExportFormat) = put(Keys.exportFormat, format.name)
    suspend fun setExportIncludeFlags(include: Boolean) = put(Keys.exportFlags, include)
    suspend fun setOnboarded(done: Boolean) = put(Keys.onboarded, done)
    suspend fun setConsentAcknowledged(done: Boolean) = put(Keys.consent, done)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val readingSize = intPreferencesKey("reading_size")
        val readingSerif = booleanPreferencesKey("reading_serif")
        val focusMode = booleanPreferencesKey("focus_mode")
        val policy = stringPreferencesKey("default_policy")
        val detail = stringPreferencesKey("default_detail")
        val interval = intPreferencesKey("capture_interval_sec")
        val threshold = intPreferencesKey("change_threshold_pct")
        val keepRawFrames = booleanPreferencesKey("keep_raw_frames")
        val keepAwake = booleanPreferencesKey("keep_screen_awake")
        val exportFormat = stringPreferencesKey("export_format")
        val exportFlags = booleanPreferencesKey("export_include_flags")
        val onboarded = booleanPreferencesKey("onboarded")
        val consent = booleanPreferencesKey("consent_acknowledged")
    }

    private fun Preferences.toSettings(): VbSettings {
        val defaults = VbSettings()
        return VbSettings(
            theme = enumOr(this[Keys.theme], defaults.theme),
            accent = enumOr(this[Keys.accent], defaults.accent),
            readingSize = this[Keys.readingSize] ?: defaults.readingSize,
            readingSerif = this[Keys.readingSerif] ?: defaults.readingSerif,
            focusMode = this[Keys.focusMode] ?: defaults.focusMode,
            defaultPolicy = enumOr(this[Keys.policy], defaults.defaultPolicy),
            defaultDetail = enumOr(this[Keys.detail], defaults.defaultDetail),
            captureIntervalSec = this[Keys.interval] ?: defaults.captureIntervalSec,
            changeThresholdPct = this[Keys.threshold] ?: defaults.changeThresholdPct,
            keepRawFrames = this[Keys.keepRawFrames] ?: defaults.keepRawFrames,
            keepScreenAwake = this[Keys.keepAwake] ?: defaults.keepScreenAwake,
            exportFormat = enumOr(this[Keys.exportFormat], defaults.exportFormat),
            exportIncludeFlags = this[Keys.exportFlags] ?: defaults.exportIncludeFlags,
            onboarded = this[Keys.onboarded] ?: defaults.onboarded,
            consentAcknowledged = this[Keys.consent] ?: defaults.consentAcknowledged,
        )
    }
}

/** Falls back to the default when a stored name no longer matches an enum constant. */
private inline fun <reified T : Enum<T>> enumOr(stored: String?, fallback: T): T =
    stored?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback
