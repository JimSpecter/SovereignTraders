package net.sovereign.core.config

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object LegacyMigrationUtil {

    private val LEGACY_PATTERN = Regex("&(?:[0-9a-fk-or]|#[0-9a-fA-F]{6})")

    private val LEGACY_SERIALIZER: LegacyComponentSerializer =
        LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build()

    private val mm = MiniMessage.miniMessage()

    fun containsLegacy(value: String): Boolean = LEGACY_PATTERN.containsMatchIn(value)

    fun legacyToMiniMessage(legacy: String): String {
        val placeholders = mutableListOf<String>()
        val safeLegacy = legacy.replace(Regex("%[^%]+%")) { match ->
            placeholders.add(match.value)
            "\u0000PLACEHOLDER_${placeholders.size - 1}\u0000"
        }

        val component = LEGACY_SERIALIZER.deserialize(safeLegacy)
        var miniMessage = mm.serialize(component)

        for ((index, placeholder) in placeholders.withIndex()) {
            miniMessage = miniMessage.replace("\u0000PLACEHOLDER_$index\u0000", placeholder)
        }

        return miniMessage
    }
}
