package net.sovereign.components.citizens

import net.sovereign.core.SovereignCore
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class CitizensBridgeManager(private val plugin: SovereignCore) {

    val mapping = CitizensBridgeMapping()
    private val dataFile = File(plugin.dataFolder, "citizens-bridge.yml")

    init {
        load()
    }

    fun load() {
        mapping.clear()
        if (!dataFile.exists()) return
        val config = YamlConfiguration.loadConfiguration(dataFile)
        val linksSection = config.getConfigurationSection("links") ?: return
        val loaded = mutableMapOf<Int, String>()
        for (key in linksSection.getKeys(false)) {
            val npcId = key.toIntOrNull() ?: continue
            val catalogId = linksSection.getString(key) ?: continue
            loaded[npcId] = catalogId
        }
        mapping.loadFrom(loaded)
    }

    fun flush() {
        val config = YamlConfiguration()
        val all = mapping.allMappings()
        for ((npcId, catalogId) in all) {
            config.set("links.$npcId", catalogId)
        }
        dataFile.parentFile.mkdirs()
        config.save(dataFile)
    }

    fun link(npcId: Int, catalogId: String) {
        mapping.link(npcId, catalogId)
        flush()
    }

    fun unlink(npcId: Int): Boolean {
        if (!mapping.isLinked(npcId)) return false
        mapping.unlink(npcId)
        flush()
        return true
    }

    fun resolve(npcId: Int): String? = mapping.resolve(npcId)

    fun isLinked(npcId: Int): Boolean = mapping.isLinked(npcId)

    fun allMappings(): Map<Int, String> = mapping.allMappings()
}
