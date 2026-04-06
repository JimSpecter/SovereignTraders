package net.sovereign.core.config

import net.sovereign.core.SovereignCore
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class SettingsManager(private val plugin: SovereignCore) {

    private val configFile: File = File(plugin.dataFolder, "config.yml")
    var configuration: FileConfiguration private set

    init {
        plugin.saveResource("config.yml", false)
        configuration = YamlConfiguration.loadConfiguration(configFile)
        migrateLegacyValues()
    }

    fun reload() {
        configuration = YamlConfiguration.loadConfiguration(configFile)
        migrateLegacyValues()
    }

    val economyProvider: String
        get() = configuration.getString("general.economy-provider", "vault") ?: "vault"

    val dynamicPricingEnabled: Boolean
        get() = configuration.getBoolean("dynamic-pricing.enabled", false)

    val dynamicPricingSensitivity: Double
        get() = configuration.getDouble("dynamic-pricing.sensitivity", 0.005)

    val dynamicPricingFloor: Double
        get() = configuration.getDouble("dynamic-pricing.floor-multiplier", 0.5)

    val dynamicPricingCeiling: Double
        get() = configuration.getDouble("dynamic-pricing.ceiling-multiplier", 2.0)

    val dynamicPricingDecayRate: Double
        get() = configuration.getDouble("dynamic-pricing.decay-rate", 0.02)

    val dynamicPricingDecayIntervalSeconds: Int
        get() = configuration.getInt("dynamic-pricing.decay-interval-seconds", 900)

    val dynamicPricingShowTrend: Boolean
        get() = configuration.getBoolean("dynamic-pricing.show-trend-indicator", true)

    val dynamicPricingBuyWeight: Double
        get() = configuration.getDouble("dynamic-pricing.buy-pressure-weight", 1.0)

    val dynamicPricingSellWeight: Double
        get() = configuration.getDouble("dynamic-pricing.sell-pressure-weight", 1.0)

    val dynamicPricingDemandBaseline: Int
        get() = configuration.getInt("dynamic-pricing.demand-baseline", 20)

    val dynamicPricingPersistentTracking: Boolean
        get() = configuration.getBoolean("dynamic-pricing.persistent-tracking", true)

    val dynamicPricingMaxHistoryEntries: Int
        get() = configuration.getInt("dynamic-pricing.max-history-entries", 50)

    val dynamicPricingSnapshotOnDecayTick: Boolean
        get() = configuration.getBoolean("dynamic-pricing.snapshot-on-decay-tick", true)

    val transactionLogEnabled: Boolean
        get() = configuration.getBoolean("transaction-log.enabled", true)

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
            plugin.logger.info("Auto-converted $converted legacy-formatted config value(s) to MiniMessage in config.yml")
        }
    }
}
