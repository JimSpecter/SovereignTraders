package net.sovereign.core.config

import net.sovereign.core.SovereignCore
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class NpcManager(private val plugin: SovereignCore) {

    private val configFile: File = File(plugin.dataFolder, "npc.yml")
    private var configuration: FileConfiguration

    init {
        plugin.saveResource("npc.yml", false)
        configuration = YamlConfiguration.loadConfiguration(configFile)
        migrateLegacyValues()
    }

    fun reload() {
        configuration = YamlConfiguration.loadConfiguration(configFile)
        migrateLegacyValues()
    }

    val defaultEntityType: String
        get() = configuration.getString("defaults.entity-type", "PLAYER") ?: "PLAYER"

    val defaultLookCloseRadius: Double
        get() = configuration.getDouble("defaults.look-close-radius", 5.0)

    val defaultInteractionWidth: Float
        get() = configuration.getDouble("defaults.interaction-width", 0.8).toFloat()

    val defaultInteractionHeight: Float
        get() = configuration.getDouble("defaults.interaction-height", 1.8).toFloat()

    val defaultHologram: List<String>
        get() = configuration.getStringList("defaults.hologram").ifEmpty {
            listOf(
                "<dark_gray><st>                    </st>",
                "<yellow>ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴛʀᴀᴅᴇ",
                "<dark_gray><st>                    </st>"
            )
        }

    data class NpcPreset(
        val name: String,
        val entityType: String,
        val skin: String?,
        val hologram: List<String>,
        val lookCloseRadius: Double,
        val interactionWidth: Float,
        val interactionHeight: Float,
        val catalog: String?
    )

    fun listPresets(): Set<String> {
        return configuration.getConfigurationSection("presets")?.getKeys(false) ?: emptySet()
    }

    fun resolvePreset(name: String): NpcPreset? {
        val section: ConfigurationSection =
            configuration.getConfigurationSection("presets.$name") ?: return null

        return NpcPreset(
            name = name,
            entityType = section.getString("entity-type") ?: defaultEntityType,
            skin = section.getString("skin"),
            hologram = section.getStringList("hologram").ifEmpty { defaultHologram },
            lookCloseRadius = if (section.contains("look-close-radius"))
                section.getDouble("look-close-radius") else defaultLookCloseRadius,
            interactionWidth = if (section.contains("interaction-width"))
                section.getDouble("interaction-width").toFloat() else defaultInteractionWidth,
            interactionHeight = if (section.contains("interaction-height"))
                section.getDouble("interaction-height").toFloat() else defaultInteractionHeight,
            catalog = section.getString("catalog")
        )
    }

    data class VendorConfig(
        val entityType: String,
        val skin: String?,
        val hologram: List<String>,
        val lookCloseRadius: Double,
        val interactionWidth: Float,
        val interactionHeight: Float,
        val catalog: String?,
        val world: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float,
        val equipment: Map<String, String>
    )

    fun saveVendor(uuid: java.util.UUID, config: VendorConfig) {
        val path = "vendors.$uuid"
        configuration.set("$path.entity-type", config.entityType)
        configuration.set("$path.skin", config.skin)
        configuration.set("$path.hologram", config.hologram)
        configuration.set("$path.look-close-radius", config.lookCloseRadius)
        configuration.set("$path.interaction-width", config.interactionWidth.toDouble())
        configuration.set("$path.interaction-height", config.interactionHeight.toDouble())
        configuration.set("$path.catalog", config.catalog)
        configuration.set("$path.location.world", config.world)
        configuration.set("$path.location.x", config.x)
        configuration.set("$path.location.y", config.y)
        configuration.set("$path.location.z", config.z)
        configuration.set("$path.location.yaw", config.yaw.toDouble())
        configuration.set("$path.location.pitch", config.pitch.toDouble())

        configuration.set("$path.equipment", null)
        for ((slot, b64) in config.equipment) {
            configuration.set("$path.equipment.$slot", b64)
        }
        configuration.save(configFile)
    }

    fun removeVendor(uuid: java.util.UUID) {
        configuration.set("vendors.$uuid", null)
        configuration.save(configFile)
    }

    fun loadAllVendors(): Map<java.util.UUID, VendorConfig> {
        val result = mutableMapOf<java.util.UUID, VendorConfig>()
        val section = configuration.getConfigurationSection("vendors") ?: return result
        for (key in section.getKeys(false)) {
            val uuid = try { java.util.UUID.fromString(key) } catch (_: Exception) { continue }
            val vendorSection = section.getConfigurationSection(key) ?: continue
            val config = parseVendorSection(vendorSection) ?: continue
            result[uuid] = config
        }
        return result
    }

    fun getVendorConfig(uuid: java.util.UUID): VendorConfig? {
        val section = configuration.getConfigurationSection("vendors.$uuid") ?: return null
        return parseVendorSection(section)
    }

    private fun parseVendorSection(section: ConfigurationSection): VendorConfig? {
        val locSection = section.getConfigurationSection("location") ?: return null
        val equipMap = mutableMapOf<String, String>()
        val equipSection = section.getConfigurationSection("equipment")
        if (equipSection != null) {
            for (slot in equipSection.getKeys(false)) {
                val b64 = equipSection.getString(slot) ?: continue
                equipMap[slot] = b64
            }
        }
        return VendorConfig(
            entityType = section.getString("entity-type") ?: defaultEntityType,
            skin = section.getString("skin"),
            hologram = section.getStringList("hologram").ifEmpty { defaultHologram },
            lookCloseRadius = if (section.contains("look-close-radius"))
                section.getDouble("look-close-radius") else defaultLookCloseRadius,
            interactionWidth = if (section.contains("interaction-width"))
                section.getDouble("interaction-width").toFloat() else defaultInteractionWidth,
            interactionHeight = if (section.contains("interaction-height"))
                section.getDouble("interaction-height").toFloat() else defaultInteractionHeight,
            catalog = section.getString("catalog"),
            world = locSection.getString("world") ?: "world",
            x = locSection.getDouble("x"),
            y = locSection.getDouble("y"),
            z = locSection.getDouble("z"),
            yaw = locSection.getDouble("yaw").toFloat(),
            pitch = locSection.getDouble("pitch").toFloat(),
            equipment = equipMap
        )
    }

    private fun migrateLegacyValues() {
        var converted = 0

        for (key in configuration.getKeys(true)) {
            val value = configuration.getString(key)
            if (value != null && LegacyMigrationUtil.containsLegacy(value)) {
                configuration.set(key, LegacyMigrationUtil.legacyToMiniMessage(value))
                converted++
                continue
            }

            val list = configuration.getStringList(key)
            if (list.isNotEmpty() && list.any { LegacyMigrationUtil.containsLegacy(it) }) {
                val convertedList = list.map { line ->
                    if (LegacyMigrationUtil.containsLegacy(line)) LegacyMigrationUtil.legacyToMiniMessage(line) else line
                }
                configuration.set(key, convertedList)
                converted++
            }
        }

        if (converted > 0) {
            configuration.save(configFile)
            plugin.logger.info("Auto-converted $converted legacy-formatted value(s) to MiniMessage in npc.yml")
        }
    }
}
