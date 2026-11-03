package com.example.varsivo.util

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Switches the whole app between English, isiZulu and Afrikaans. Android then loads the matching
 * res/values, res/values-zu or res/values-af strings and redraws every open screen.
 *
 * Reference: Android Developers (2024) *Per-app language preferences*
 * https://developer.android.com/guide/topics/resources/app-languages
 */
object AppLanguage {

    /** "English" / "isiZulu" / "Afrikaans" (as stored in settings) -> language tag. */
    fun tagFor(language: String): String = when (language) {
        "isiZulu" -> "zu"
        "Afrikaans" -> "af"
        else -> "en"
    }

    /** Applies [language] to the app. Does nothing if it is already the current language. */
    fun apply(language: String) {
        val tag = tagFor(language)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == tag) return
        Log.i("AppLanguage", "Switching app language to $language ($tag)")
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
