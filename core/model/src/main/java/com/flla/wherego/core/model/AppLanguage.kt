package com.flla.wherego.core.model

object AppLanguage {
    const val SYSTEM = "system"
    const val EN = "en-US"
    const val ID = "id-ID"

    fun parse(raw: String?): String = when (raw) {
        EN, ID -> raw
        else -> SYSTEM
    }

    /**
     * Resolves a stored preference to a tag we actually ship resources for.
     *
     * [deviceLanguages] is the device's ordered language list as `Locale.getLanguage()` values,
     * so Indonesian arrives as the legacy code `in`, not `id`.
     */
    fun resolve(tag: String, deviceLanguages: List<String>): String = when (parse(tag)) {
        EN -> EN
        ID -> ID
        else -> when (deviceLanguages.firstOrNull()?.lowercase()) {
            null, "" -> ID
            "in", "id" -> ID
            else -> EN
        }
    }
}
