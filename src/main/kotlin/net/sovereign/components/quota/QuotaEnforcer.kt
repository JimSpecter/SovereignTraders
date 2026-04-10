package net.sovereign.components.quota

import net.sovereign.core.SovereignCore
import net.sovereign.components.catalog.ListingType
import java.util.concurrent.ConcurrentHashMap

class QuotaEnforcer(private val plugin: SovereignCore, private val ledger: QuotaLedger) {

    private val registeredIntervals = ConcurrentHashMap<Int, Int>()

    private val countdownTimers = ConcurrentHashMap<Int, Int>()

    private var globalTick: Int = 0

    init {
        startTickLoop()
    }

    fun registerInterval(listingUid: Int, intervalSeconds: Int) {
        if (intervalSeconds > 0) {
            registeredIntervals[listingUid] = intervalSeconds
            countdownTimers[listingUid] = intervalSeconds
        } else {
            registeredIntervals.remove(listingUid)
            countdownTimers.remove(listingUid)
        }
    }

    fun unregisterInterval(listingUid: Int) {
        registeredIntervals.remove(listingUid)
        countdownTimers.remove(listingUid)
    }

    fun getTimeRemaining(listingUid: Int): Int {
        return countdownTimers[listingUid] ?: -1
    }

    fun syncFromCatalogs() {
        registeredIntervals.clear()
        countdownTimers.clear()

        for (catalogId in plugin.catalogRepository.listIdentifiers()) {
            val catalog = plugin.catalogRepository.resolve(catalogId) ?: continue
            for (section in catalog.sections) {
                for (mode in net.sovereign.components.catalog.TransactionMode.entries) {
                    for (listing in section.listingsForMode(mode)) {
                        if (listing != null && listing.quotaResetIntervalSec > 0) {
                            registerInterval(listing.uid, listing.quotaResetIntervalSec)
                        }
                    }
                }
            }
        }
        if (registeredIntervals.isNotEmpty()) {
            SovereignCore.broadcast("<gray>Quota enforcer: <white>${registeredIntervals.size} <gray>active interval(s) registered.")
        }
    }

    private var task: Any? = null

    fun stop() {
        if (task != null) {
            net.sovereign.core.scheduler.SovereignScheduler.cancelTask(task)
            task = null
        }
    }

    fun start() {
        startTickLoop()
    }

    private fun startTickLoop() {
        task = net.sovereign.core.scheduler.SovereignScheduler.runTaskTimer(plugin.plugin, Runnable {
            globalTick++

            val expired = mutableListOf<Int>()
            for ((listingUid, interval) in registeredIntervals) {
                val remaining = countdownTimers[listingUid]
                if (remaining == null) {
                    countdownTimers[listingUid] = interval
                    continue
                }

                val newRemaining = remaining - 1
                if (newRemaining <= 0) {
                    expired.add(listingUid)
                    countdownTimers[listingUid] = interval
                } else {
                    countdownTimers[listingUid] = newRemaining
                }
            }

            for (listingUid in expired) {
                ledger.resetUsageForListing(listingUid)
            }
        }, 20L, 20L)
    }
}
