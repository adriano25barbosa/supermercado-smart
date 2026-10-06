package com.example.supermercadosmart.data

import android.content.Context

/** Escolha do tema no menu ⋮. "Sistema" segue o modo escuro do celular. */
enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Escuro")
}

/** Guarda a escolha do tema nas preferências do app (fora do Room: não mexe no banco). */
object ThemePreference {
    private const val PREFS = "preferencias"
    private const val KEY_THEME = "tema"

    fun load(context: Context): ThemeMode {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        return ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}
