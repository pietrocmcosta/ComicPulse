package com.example.comicaggregator.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    fun resolveLocale(languageCode: String): Locale {
        return when (languageCode) {
            "pt" -> Locale("pt")
            "en" -> Locale("en")
            else -> Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()
        }
    }

    @Suppress("DEPRECATION")
    fun applyLocale(context: Context, languageCode: String): Configuration {
        val locale = resolveLocale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return config
    }
}
