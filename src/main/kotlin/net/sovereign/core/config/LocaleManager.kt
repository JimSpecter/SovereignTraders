package net.sovereign.core.config

import net.sovereign.core.SovereignCore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class LocaleManager(private val plugin: SovereignCore) {

    private val localeFile: File = File(plugin.dataFolder, "messages.yml")
    private var locale: FileConfiguration
    private val mm = MiniMessage.miniMessage()

    init {
        plugin.saveResource("messages.yml", false)
        locale = YamlConfiguration.loadConfiguration(localeFile)
        migrateLegacyValues()
    }

    fun reload() {
        locale = YamlConfiguration.loadConfiguration(localeFile)
        migrateLegacyValues()
    }

    fun raw(path: String, vararg replacements: Pair<String, String>): String {
        var message = locale.getString(path, "") ?: ""
        for ((placeholder, value) in replacements) {
            val cleanKey = placeholder.trim('%', '<', '>')
            message = message.replace("<$cleanKey>", value)
            message = message.replace("%$cleanKey%", value)
        }
        return message
    }

    fun resolveFragment(path: String, vararg replacements: Pair<String, String>): Component {
        var body = locale.getString(path, "") ?: ""

        val resolvers = mutableListOf<net.kyori.adventure.text.minimessage.tag.resolver.TagResolver>()
        for ((placeholder, value) in replacements) {
            val cleanKey = placeholder.trim('%', '<', '>')

            body = body.replace("%$cleanKey%", "<$cleanKey>")

            val replacementComponent = try {
                mm.deserialize(value)
            } catch (e: Exception) {
                net.kyori.adventure.text.Component.text(value)
            }
            resolvers.add(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(cleanKey, replacementComponent))
        }

        return mm.deserialize(body, *resolvers.toTypedArray())
    }

    fun resolve(path: String, vararg replacements: Pair<String, String>): Component {
        val prefix = locale.getString("general.tag", "") ?: ""
        var body = locale.getString(path, "") ?: ""

        val resolvers = mutableListOf<net.kyori.adventure.text.minimessage.tag.resolver.TagResolver>()
        for ((placeholder, value) in replacements) {
            val cleanKey = placeholder.trim('%', '<', '>')
            body = body.replace("%$cleanKey%", "<$cleanKey>")
            val replacementComponent = try {
                mm.deserialize(value)
            } catch (e: Exception) {
                net.kyori.adventure.text.Component.text(value)
            }
            resolvers.add(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(cleanKey, replacementComponent))
        }

        return mm.deserialize("$prefix$body", *resolvers.toTypedArray())
    }

    fun dispatch(recipient: Player, path: String, vararg replacements: Pair<String, String>) {
        val component = resolve(path, *replacements)
        recipient.sendMessage(component)
    }

    fun formatCurrency(amount: Double): String {
        val pattern = raw("general.currency-pattern").ifEmpty { "#0.00" }
        return java.text.DecimalFormat(pattern).format(amount)
    }

    val acquireCostFormat: String
        get() = raw("general.acquire-cost-format")

    val liquidateRewardFormat: String
        get() = raw("general.liquidate-reward-format")

    private fun migrateLegacyValues() {
        var converted = 0

        for (key in locale.getKeys(true)) {
            val value = locale.getString(key) ?: continue
            if (!LegacyMigrationUtil.containsLegacy(value)) continue

            val miniMessageValue = LegacyMigrationUtil.legacyToMiniMessage(value)
            locale.set(key, miniMessageValue)
            converted++
        }

        if (converted > 0) {
            locale.save(localeFile)
            plugin.logger.info("Auto-converted $converted legacy-formatted message(s) to MiniMessage in messages.yml")
        }
    }
}
