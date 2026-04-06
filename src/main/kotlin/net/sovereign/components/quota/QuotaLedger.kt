package net.sovereign.components.quota

import net.sovereign.core.SovereignCore
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class QuotaLedger(private val plugin: SovereignCore) {

    private val ledger = ConcurrentHashMap<UUID, MutableMap<Int, Int>>()

    private val ledgerFile: File = File(plugin.dataFolder, "quota-ledger.yml")

    init {
        load()
    }

    fun getUsage(playerId: UUID, listingUid: Int): Int {
        return ledger[playerId]?.get(listingUid) ?: 0
    }

    fun recordUsage(playerId: UUID, listingUid: Int, amount: Int = 1) {
        val playerQuotas = ledger.getOrPut(playerId) { mutableMapOf() }
        playerQuotas[listingUid] = (playerQuotas[listingUid] ?: 0) + amount
        scheduleFlush()
    }

    fun resetUsageForListing(listingUid: Int) {
        for ((_, playerQuotas) in ledger) {
            playerQuotas.remove(listingUid)
        }
        scheduleFlush()
    }

    fun resetPlayerUsage(playerId: UUID) {
        ledger.remove(playerId)
        scheduleFlush()
    }

    fun flush() {
        val config = YamlConfiguration()
        for ((playerId, quotas) in ledger) {
            for ((listingUid, usage) in quotas) {
                config.set("$playerId.$listingUid", usage)
            }
        }
        config.save(ledgerFile)
    }

    private fun load() {
        if (!ledgerFile.exists()) return
        val config = YamlConfiguration.loadConfiguration(ledgerFile)
        for (playerKey in config.getKeys(false)) {
            val playerId = try { UUID.fromString(playerKey) } catch (_: Exception) { continue }
            val playerSection = config.getConfigurationSection(playerKey) ?: continue
            val quotas = mutableMapOf<Int, Int>()
            for (listingKey in playerSection.getKeys(false)) {
                val listingUid = listingKey.toIntOrNull() ?: continue
                quotas[listingUid] = playerSection.getInt(listingKey)
            }
            ledger[playerId] = quotas
        }
    }

    private var flushScheduled = false

    private fun scheduleFlush() {
        if (flushScheduled) return
        flushScheduled = true
        net.sovereign.core.scheduler.SovereignScheduler.runTaskLaterAsynchronously(plugin.plugin, Runnable {
            flush()
            flushScheduled = false
        }, 100L)
    }
}
