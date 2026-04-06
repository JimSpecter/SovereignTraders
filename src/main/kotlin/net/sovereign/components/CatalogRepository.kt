package net.sovereign.components

import net.sovereign.core.SovereignCore
import net.sovereign.components.catalog.*
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class CatalogRepository(private val plugin: SovereignCore) {

    private val registeredCatalogs = mutableMapOf<String, Catalog>()
    private val catalogsDir: File = File(plugin.dataFolder, "catalogs")

    init {
        if (!catalogsDir.exists()) catalogsDir.mkdirs()
        loadAll()
    }

    fun resolve(identifier: String): Catalog? = registeredCatalogs[identifier.lowercase()]

    fun listIdentifiers(): List<String> = registeredCatalogs.keys.toList()

    fun register(catalog: Catalog) {
        registeredCatalogs[catalog.identifier.lowercase()] = catalog
        persistSingle(catalog)
    }

    fun remove(identifier: String) {
        registeredCatalogs.remove(identifier.lowercase())
        val file = File(catalogsDir, "${identifier.lowercase()}.yml")
        if (file.exists()) file.delete()
    }

    fun createDefault(identifier: String): Catalog {
        val catalog = Catalog(
            identifier = identifier.lowercase(),
            displayTitle = identifier.replaceFirstChar { it.uppercase() },
            acquireEnabled = true,
            liquidateEnabled = true,
            barterEnabled = false
        )

        catalog.sections.add(
            CatalogSection(
                identifier = "main",
                rowCount = 4
            )
        )
        register(catalog)
        return catalog
    }

    fun persistAll() {
        for ((_, catalog) in registeredCatalogs) {
            persistSingle(catalog)
        }
    }

    fun loadAll() {
        registeredCatalogs.clear()
        val files = catalogsDir.listFiles { f -> f.extension == "yml" } ?: return

        for (file in files) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val catalog = deserializeCatalog(config)
                if (catalog != null) {
                    registeredCatalogs[catalog.identifier.lowercase()] = catalog
                }
            } catch (ex: Exception) {
                SovereignCore.broadcast("<red>Failed to load catalog from ${file.name}: ${ex.message}")
            }
        }
        SovereignCore.broadcast("<green>Loaded ${registeredCatalogs.size} catalogs.")
        deduplicateUids()
    }

    fun globalMaxUid(): Int {
        var maxUid = 0
        for ((_, catalog) in registeredCatalogs) {
            for (section in catalog.sections) {
                for (mode in TransactionMode.entries) {
                    for (listing in section.listingsForMode(mode)) {
                        if (listing != null && listing.uid > maxUid) maxUid = listing.uid
                    }
                }
            }
        }
        return maxUid
    }

    fun generateNextGlobalUid(): Int {
        return globalMaxUid() + 1
    }

    private fun deduplicateUids() {
        data class UidOccurrence(
            val catalogId: String,
            val sectionIndex: Int,
            val mode: TransactionMode,
            val slot: Int,
            val listing: Listing
        )

        val uidOccurrences = mutableMapOf<Int, MutableList<UidOccurrence>>()

        for ((catalogId, catalog) in registeredCatalogs) {
            for ((sectionIndex, section) in catalog.sections.withIndex()) {
                for (mode in TransactionMode.entries) {
                    val listings = section.listingsForMode(mode)
                    for ((slot, listing) in listings.withIndex()) {
                        if (listing != null) {
                            uidOccurrences.getOrPut(listing.uid) { mutableListOf() }
                                .add(UidOccurrence(catalogId, sectionIndex, mode, slot, listing))
                        }
                    }
                }
            }
        }

        val migrations = mutableMapOf<Int, Int>()
        val affectedCatalogs = mutableSetOf<String>()

        for ((uid, occurrences) in uidOccurrences) {

            val catalogGroups = occurrences.groupBy { it.catalogId }
            if (catalogGroups.size <= 1) continue

            val catalogsInOrder = catalogGroups.keys.toList()
            for (i in 1 until catalogsInOrder.size) {
                val conflictingCatalogId = catalogsInOrder[i]
                val conflictingOccurrences = catalogGroups[conflictingCatalogId]!!

                val newUid = if (migrations.containsValue(uid)) {

                    globalMaxUid() + 1
                } else {

                    val existingNewUid = migrations.entries
                        .firstOrNull { it.key == uid }?.value
                    existingNewUid ?: (globalMaxUid() + 1)
                }

                for (occurrence in conflictingOccurrences) {
                    val catalog = registeredCatalogs[occurrence.catalogId]!!
                    val section = catalog.sections[occurrence.sectionIndex]
                    val listings = section.listingsForMode(occurrence.mode)

                    listings[occurrence.slot] = occurrence.listing.copy(uid = newUid)
                }

                if (!migrations.containsKey(uid)) {
                    migrations[uid] = newUid
                }
                affectedCatalogs.add(conflictingCatalogId)
            }
        }

        if (migrations.isEmpty()) return

        for (catalogId in affectedCatalogs) {
            val catalog = registeredCatalogs[catalogId] ?: continue
            persistSingle(catalog)
        }

        for ((oldUid, newUid) in migrations) {
            plugin.pricingModule.onUidMigrated(oldUid, newUid)
        }

        val migrationLog = migrations.entries.joinToString(", ") { "${it.key}→${it.value}" }
        SovereignCore.broadcast("<yellow>UID deduplication: reassigned ${migrations.size} colliding UIDs: $migrationLog")
    }

    private fun persistSingle(catalog: Catalog) {
        val file = File(catalogsDir, "${catalog.identifier}.yml")
        val config = YamlConfiguration()
        serializeCatalog(config, catalog)
        config.save(file)
    }

    private fun serializeCatalog(config: YamlConfiguration, catalog: Catalog) {
        config.set("identifier", catalog.identifier)
        config.set("display-title", catalog.displayTitle)
        config.set("authorization", catalog.authorization)
        config.set("modes.acquire", catalog.acquireEnabled)
        config.set("modes.liquidate", catalog.liquidateEnabled)
        config.set("modes.barter", catalog.barterEnabled)

        config.set("reduction.active", catalog.reductionActive)
        config.set("reduction.percent", catalog.reductionPercent)
        config.set("reduction.window-start", catalog.reductionWindowStart)
        config.set("reduction.window-duration", catalog.reductionWindowDurationSec)
        config.set("reduction.window-end", catalog.reductionWindowEnd)

        for ((idx, section) in catalog.sections.withIndex()) {
            val sectionPath = "sections.$idx"
            config.set("$sectionPath.identifier", section.identifier)
            config.set("$sectionPath.row-count", section.rowCount)
            config.set("$sectionPath.authorization", section.authorization)

            serializeListings(config, "$sectionPath.acquire-listings", section.acquireListings)
            serializeListings(config, "$sectionPath.liquidate-listings", section.liquidateListings)
            serializeListings(config, "$sectionPath.barter-listings", section.barterListings)
        }
    }

    private fun serializeListings(config: YamlConfiguration, basePath: String, listings: Array<Listing?>) {
        for ((slot, listing) in listings.withIndex()) {
            if (listing == null) continue
            val path = "$basePath.$slot"
            config.set("$path.uid", listing.uid)
            config.set("$path.type", listing.type.configKey)
            config.set("$path.material", listing.materialId)
            config.set("$path.quantity", listing.stackQuantity)
            if (listing.itemStack != null) {
                config.set("$path.item-stack", listing.itemStack)
            }
            config.set("$path.label", listing.label)
            config.set("$path.annotations", listing.annotations)
            config.set("$path.show-annotations", listing.showAnnotations)
            config.set("$path.acquire-cost", listing.acquireCost)
            config.set("$path.liquidate-reward", listing.liquidateReward)
            config.set("$path.show-cost", listing.showCost)

            config.set("$path.reduction.active", listing.reductionActive)
            config.set("$path.reduction.percent", listing.reductionPercent)
            config.set("$path.reduction.override-cost", listing.reductionOverrideCost)
            config.set("$path.reduction.window-start", listing.reductionWindowStart)
            config.set("$path.reduction.window-duration", listing.reductionWindowDurationSec)
            config.set("$path.reduction.window-end", listing.reductionWindowEnd)
            config.set("$path.reduction.show-cost", listing.showReductionCost)
            config.set("$path.reduction.show-start", listing.showReductionStart)
            config.set("$path.reduction.show-duration", listing.showReductionDuration)
            config.set("$path.reduction.show-end", listing.showReductionEnd)

            config.set("$path.quota.limit", listing.quotaLimit)
            config.set("$path.quota.reset-interval", listing.quotaResetIntervalSec)
            config.set("$path.quota.show-progress", listing.showQuotaProgress)
            config.set("$path.quota.show-timer", listing.showQuotaTimer)

            config.set("$path.authorization", listing.authorization)
            config.set("$path.drop-on-overflow", listing.dropOnOverflow)
            config.set("$path.partial-liquidation", listing.liquidatePartialAllowed)
            config.set("$path.broadcast", listing.broadcastOnTransaction)
            config.set("$path.broadcast-template", listing.broadcastTemplate)

            if (listing.directives.isNotEmpty()) {
                config.set("$path.directive-run-mode", listing.directiveRunMode.name)
                for ((dIdx, directive) in listing.directives.withIndex()) {
                    config.set("$path.directives.$dIdx.command", directive.command)
                    config.set("$path.directives.$dIdx.executor", directive.executor.name)
                }
            }

            if (listing.barterRequirements.isNotEmpty()) {
                for ((bIdx, component) in listing.barterRequirements.withIndex()) {
                    config.set("$path.barter-requirements.$bIdx.material", component.materialId)
                    config.set("$path.barter-requirements.$bIdx.quantity", component.quantity)
                    config.set("$path.barter-requirements.$bIdx.label", component.label)
                }
            }
            if (listing.barterReceivables.isNotEmpty()) {
                for ((bIdx, component) in listing.barterReceivables.withIndex()) {
                    config.set("$path.barter-receivables.$bIdx.material", component.materialId)
                    config.set("$path.barter-receivables.$bIdx.quantity", component.quantity)
                    config.set("$path.barter-receivables.$bIdx.label", component.label)
                }
            }
        }
    }

    private fun deserializeCatalog(config: YamlConfiguration): Catalog? {
        val identifier = config.getString("identifier") ?: return null
        val catalog = Catalog(
            identifier = identifier,
            displayTitle = config.getString("display-title", identifier) ?: identifier,
            authorization = config.getString("authorization"),
            acquireEnabled = config.getBoolean("modes.acquire", true),
            liquidateEnabled = config.getBoolean("modes.liquidate", false),
            barterEnabled = config.getBoolean("modes.barter", false)
        )

        val reductionSection = config.getConfigurationSection("reduction")
        if (reductionSection != null) {
            catalog.reductionActive = reductionSection.getBoolean("active", false)
            catalog.reductionPercent = reductionSection.getDouble("percent", 0.0)
            catalog.reductionWindowStart = reductionSection.getString("window-start")
            catalog.reductionWindowDurationSec = reductionSection.getInt("window-duration", 0)
            catalog.reductionWindowEnd = reductionSection.getString("window-end")
        }

        val sectionsSection = config.getConfigurationSection("sections") ?: return catalog
        for (key in sectionsSection.getKeys(false)) {
            val sectionConfig = sectionsSection.getConfigurationSection(key) ?: continue
            val rowCount = sectionConfig.getInt("row-count", 4)
            val section = CatalogSection(
                identifier = sectionConfig.getString("identifier", "section-$key") ?: "section-$key",
                rowCount = rowCount,
                authorization = sectionConfig.getString("authorization")
            )

            deserializeListings(sectionConfig, "acquire-listings", section.acquireListings)
            deserializeListings(sectionConfig, "liquidate-listings", section.liquidateListings)
            deserializeListings(sectionConfig, "barter-listings", section.barterListings)

            catalog.sections.add(section)
        }

        return catalog
    }

    private fun deserializeListings(sectionConfig: ConfigurationSection, key: String, target: Array<Listing?>) {
        val listingsSection = sectionConfig.getConfigurationSection(key) ?: return
        for (slotKey in listingsSection.getKeys(false)) {
            val slot = slotKey.toIntOrNull() ?: continue
            if (slot >= target.size) continue
            val listingConfig = listingsSection.getConfigurationSection(slotKey) ?: continue
            target[slot] = deserializeListing(listingConfig)
        }
    }

    private fun deserializeListing(config: ConfigurationSection): Listing {
        val listing = Listing(
            uid = config.getInt("uid", 0),
            type = ListingType.fromConfigKey(config.getString("type", "static") ?: "static"),
            materialId = config.getString("material", "STONE") ?: "STONE",
            stackQuantity = config.getInt("quantity", 1),
            itemStack = config.getItemStack("item-stack"),
            label = config.getString("label"),
            annotations = config.getStringList("annotations").toMutableList(),
            showAnnotations = config.getBoolean("show-annotations", true),
            acquireCost = config.getDouble("acquire-cost", 0.0),
            liquidateReward = config.getDouble("liquidate-reward", 0.0),
            showCost = config.getBoolean("show-cost", true)
        )

        val reductionSection = config.getConfigurationSection("reduction")
        if (reductionSection != null) {
            listing.reductionActive = reductionSection.getBoolean("active", false)
            listing.reductionPercent = reductionSection.getDouble("percent", 0.0)
            listing.reductionOverrideCost = reductionSection.getDouble("override-cost", 0.0)
            listing.reductionWindowStart = reductionSection.getString("window-start")
            listing.reductionWindowDurationSec = reductionSection.getInt("window-duration", 0)
            listing.reductionWindowEnd = reductionSection.getString("window-end")
            listing.showReductionCost = reductionSection.getBoolean("show-cost", false)
            listing.showReductionStart = reductionSection.getBoolean("show-start", false)
            listing.showReductionDuration = reductionSection.getBoolean("show-duration", false)
            listing.showReductionEnd = reductionSection.getBoolean("show-end", false)
        }

        val quotaSection = config.getConfigurationSection("quota")
        if (quotaSection != null) {
            listing.quotaLimit = quotaSection.getInt("limit", 0)
            listing.quotaResetIntervalSec = quotaSection.getInt("reset-interval", 0)
            listing.showQuotaProgress = quotaSection.getBoolean("show-progress", false)
            listing.showQuotaTimer = quotaSection.getBoolean("show-timer", false)
        }

        listing.authorization = config.getString("authorization")
        listing.dropOnOverflow = config.getBoolean("drop-on-overflow", false)
        listing.liquidatePartialAllowed = config.getBoolean("partial-liquidation", false)
        listing.broadcastOnTransaction = config.getBoolean("broadcast", false)
        listing.broadcastTemplate = config.getString("broadcast-template", "") ?: ""

        listing.directiveRunMode = DirectiveRunMode.fromString(
            config.getString("directive-run-mode", "EXECUTE_ONLY") ?: "EXECUTE_ONLY"
        )
        val directivesSection = config.getConfigurationSection("directives")
        if (directivesSection != null) {
            for (dKey in directivesSection.getKeys(false)) {
                val dConfig = directivesSection.getConfigurationSection(dKey) ?: continue
                listing.directives.add(
                    Directive(
                        command = dConfig.getString("command", "") ?: "",
                        executor = DirectiveExecutor.fromString(
                            dConfig.getString("executor", "CONSOLE") ?: "CONSOLE"
                        )
                    )
                )
            }
        }

        val barterReqSection = config.getConfigurationSection("barter-requirements")
        if (barterReqSection != null) {
            for (bKey in barterReqSection.getKeys(false)) {
                val bConfig = barterReqSection.getConfigurationSection(bKey) ?: continue
                listing.barterRequirements.add(
                    BarterComponent(
                        materialId = bConfig.getString("material", "STONE") ?: "STONE",
                        quantity = bConfig.getInt("quantity", 1),
                        label = bConfig.getString("label")
                    )
                )
            }
        }
        val barterRecSection = config.getConfigurationSection("barter-receivables")
        if (barterRecSection != null) {
            for (bKey in barterRecSection.getKeys(false)) {
                val bConfig = barterRecSection.getConfigurationSection(bKey) ?: continue
                listing.barterReceivables.add(
                    BarterComponent(
                        materialId = bConfig.getString("material", "STONE") ?: "STONE",
                        quantity = bConfig.getInt("quantity", 1),
                        label = bConfig.getString("label")
                    )
                )
            }
        }

        return listing
    }
}
