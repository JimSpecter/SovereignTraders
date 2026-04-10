package net.sovereign.components.catalog

import net.sovereign.core.SovereignCore
import net.sovereign.core.scheduler.SovereignScheduler
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

class ReductionWindowScheduler(private val plugin: SovereignCore) {

    private var task: Any? = null

    
    var trackedWindows: Int = 0
        private set

    init {
        syncFromCatalogs()
        startTickLoop()
    }

    fun stop() {
        if (task != null) {
            SovereignScheduler.cancelTask(task)
            task = null
        }
    }

    fun start() {
        startTickLoop()
    }

    fun syncFromCatalogs() {
        var count = 0
        for (catalogId in plugin.catalogRepository.listIdentifiers()) {
            val catalog = plugin.catalogRepository.resolve(catalogId) ?: continue
            if (catalog.reductionWindowStart != null || catalog.reductionWindowEnd != null) {
                count++
            }
            for (section in catalog.sections) {
                for (mode in TransactionMode.entries) {
                    for (listing in section.listingsForMode(mode)) {
                        if (listing != null &&
                            (listing.reductionWindowStart != null || listing.reductionWindowEnd != null)
                        ) {
                            count++
                        }
                    }
                }
            }
        }
        trackedWindows = count
        if (trackedWindows > 0) {
            SovereignCore.broadcast("<gray>Reduction scheduler: <white>$trackedWindows <gray>window(s) tracked.")
        }
    }

    
    private fun startTickLoop() {
        task = SovereignScheduler.runTaskTimer(plugin.plugin, Runnable {
            if (trackedWindows == 0) return@Runnable
            val now = Instant.now()
            var dirty = false

            for (catalogId in plugin.catalogRepository.listIdentifiers()) {
                val catalog = plugin.catalogRepository.resolve(catalogId) ?: continue

                val catalogDesired = shouldToggleCatalog(catalog, now)
                if (catalogDesired != null && catalog.reductionActive != catalogDesired) {
                    catalog.reductionActive = catalogDesired
                    dirty = true
                    SovereignCore.broadcast(
                        "<gray>Reduction window: catalog <white>${catalog.identifier} <gray>→ " +
                                if (catalogDesired) "<green>active" else "<red>inactive"
                    )
                }

                for (section in catalog.sections) {
                    for (mode in TransactionMode.entries) {
                        for (listing in section.listingsForMode(mode)) {
                            if (listing == null) continue
                            val desired = shouldToggle(listing, now) ?: continue
                            if (listing.reductionActive != desired) {
                                listing.reductionActive = desired
                                dirty = true
                            }
                        }
                    }
                }
            }

            if (dirty) {
                plugin.catalogRepository.persistAll()
            }
        }, 100L, 100L) // 5 second interval (100 ticks)
    }

    companion object {

        
        fun parseWindowTime(value: String?): Instant? {
            if (value.isNullOrBlank()) return null

            val trimmed = value.trim()

            trimmed.toLongOrNull()?.let { millis ->
                return Instant.ofEpochMilli(millis)
            }

            try {
                return Instant.parse(trimmed)
            } catch (_: DateTimeParseException) {
            }

            try {
                val ldt = LocalDateTime.parse(trimmed)
                return ldt.toInstant(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {
            }

            return null
        }

        
        fun evaluateWindow(start: Instant?, end: Instant?, now: Instant): Boolean? {
            if (start == null && end == null) return null

            val afterStart = start == null || !now.isBefore(start)
            val beforeEnd = end == null || now.isBefore(end)

            return afterStart && beforeEnd
        }

        
        fun shouldToggle(listing: Listing, now: Instant): Boolean? {
            val start = parseWindowTime(listing.reductionWindowStart)
            val end = parseWindowTime(listing.reductionWindowEnd)
            return evaluateWindow(start, end, now)
        }

        
        fun shouldToggleCatalog(catalog: Catalog, now: Instant): Boolean? {
            val start = parseWindowTime(catalog.reductionWindowStart)
            val end = parseWindowTime(catalog.reductionWindowEnd)
            return evaluateWindow(start, end, now)
        }
    }
}
