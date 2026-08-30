package com.flla.wherego.core.i18n

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.flla.wherego.core.model.AppLanguage
import java.util.Locale

object AppLocale {
    private const val PREFS = "wherego_locale"
    private const val KEY_TAG = "tag"

    @Volatile
    private var cachedTag: String? = null

    /** Device language codes, ordered, as `Locale.getLanguage()` values. */
    fun deviceLanguages(configuration: Configuration): List<String> {
        val locales = configuration.locales
        return List(locales.size()) { locales[it].language }
    }

    fun locale(tag: String, deviceLanguages: List<String>): Locale =
        Locale.forLanguageTag(AppLanguage.resolve(tag, deviceLanguages))

    fun load(context: Context): String {
        cachedTag?.let { return it }
        val tag = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, AppLanguage.SYSTEM) ?: AppLanguage.SYSTEM
        cachedTag = tag
        return tag
    }

    fun persist(context: Context, tag: String) {
        if (cachedTag == tag) return
        cachedTag = tag
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .apply()
    }

    /**
     * Recreate so Dialog / ModalBottomSheet windows pick up the locale. Those
     * windows read the Activity context, not Compose's `LocalContext`.
     */
    fun applyAndRecreateIfNeeded(activity: Activity, tag: String) {
        if (load(activity) == tag) return
        persist(activity, tag)
        activity.recreate()
    }

    /** For `attachBaseContext` and non-Compose callers (workers, notifications). */
    fun context(base: Context, tag: String): Context {
        val configuration = base.resources.configuration
        val locale = locale(tag, deviceLanguages(configuration))
        Locale.setDefault(locale)
        val localized = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(localized)
    }
}

/**
 * Keeps [base] (the Activity) in the wrapper chain so Hilt's `hiltViewModel()` can still
 * find it, while [getResources] serves the locale-overridden copy.
 * `createConfigurationContext` alone returns a bare `ContextImpl` and crashes navigation.
 */
private class LocaleContext(
    base: Context,
    private val override: Context,
) : ContextWrapper(base) {
    override fun getResources(): Resources = override.resources
    override fun getAssets(): AssetManager = override.assets
}

@Composable
fun ProvideAppLanguage(tag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(tag, configuration) {
        val locale = AppLocale.locale(tag, AppLocale.deviceLanguages(configuration))
        val overridden = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        LocaleContext(base, base.createConfigurationContext(overridden))
    }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content,
    )
}
