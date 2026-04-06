package net.sovereign.core.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocaleManagerTest {

    @Test
    fun `test variables do not bleed colors into surrounding text`() {

        val mm = MiniMessage.miniMessage()

        fun simulatedResolve(template: String, vararg replacements: Pair<String, String>): Component {
            var body = template
            val resolvers = mutableListOf<net.kyori.adventure.text.minimessage.tag.resolver.TagResolver>()
            for ((placeholder, value) in replacements) {
                val cleanKey = placeholder.trim('%', '<', '>')
                body = body.replace("%$cleanKey%", "<$cleanKey>")
                resolvers.add(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(cleanKey, mm.deserialize(value)))
            }
            return mm.deserialize(body, *resolvers.toTypedArray())
        }

        val template = "<green>Successfully acquired %listing% for %amount%."

        val coloredVariable = "<gold>Excalibur"
        val price = "$50.00"

        val resultComponent = simulatedResolve(
            template,
            "%listing%" to coloredVariable,
            "%amount%" to price
        )

        val serialized = mm.serialize(resultComponent)

        val hasResetOrCorrectColor = serialized.contains("</gold>") ||
                                     serialized.contains("<green> for") ||
                                     serialized.contains("<!color> for")

        assertTrue(hasResetOrCorrectColor, "The color MUST be reset after the variable!")
    }
}
