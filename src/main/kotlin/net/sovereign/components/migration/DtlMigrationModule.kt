package net.sovereign.components.migration

import net.sovereign.core.SovereignCore
import net.sovereign.components.CatalogRepository
import net.sovereign.components.catalog.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.command.CommandSender
import net.kyori.adventure.text.minimessage.MiniMessage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DtlMigrationModule(private val plugin: SovereignCore) {

    private val mm = MiniMessage.miniMessage()
    private var uidCounter = 1

    fun migrate(sender: CommandSender): MigrationReport {
        val report = MigrationReport()
        uidCounter = 1

        val pluginsDir = plugin.dataFolder.parentFile
        val candidates = listOf(
            "dtlTradersPlus", "dtlTraders",
            "dtltradersplus", "dtltraders"
        )

        var dtlFolder: File? = null
        for (name in candidates) {
            val f = File(pluginsDir, name)
            if (f.exists() && f.isDirectory) {
                dtlFolder = f
                break
            }
        }

        if (dtlFolder == null) {
            msg(sender, "<red>ɴᴏ ᴅᴛʟᴛʀᴀᴅᴇʀs / ᴅᴛʟᴛʀᴀᴅᴇʀsᴘʟᴜs ᴅᴀᴛᴀ ꜰᴏʟᴅᴇʀ ꜰᴏᴜɴᴅ ɪɴ <yellow>${pluginsDir.absolutePath}")
            msg(sender, "<gray>ᴇxᴘᴇᴄᴛᴇᴅ ᴏɴᴇ ᴏꜰ: ${candidates.joinToString()}")
            report.error = "DTL data folder not found"
            return report
        }

        msg(sender, "<green>ꜰᴏᴜɴᴅ ᴅᴛʟ ᴅᴀᴛᴀ ꜰᴏʟᴅᴇʀ: <yellow>${dtlFolder.name}")
        report.sourceFolderName = dtlFolder.name

        val shopsDir = File(dtlFolder, "shops")
        val legacyGuisFile = File(dtlFolder, "guis.yml")

        val shopConfigs = mutableMapOf<String, YamlConfiguration>()

        if (shopsDir.exists() && shopsDir.isDirectory) {

            val shopFolders = shopsDir.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (folder in shopFolders) {
                val ymlFile = File(folder, "${folder.name}.yml")
                if (ymlFile.exists()) {
                    try {
                        val config = YamlConfiguration.loadConfiguration(ymlFile)
                        shopConfigs[folder.name.lowercase()] = config
                    } catch (ex: Exception) {
                        msg(sender, "<red>ꜰᴀɪʟᴇᴅ ᴛᴏ ᴘᴀʀsᴇ ${ymlFile.absolutePath}: <gray>${ex.message}")
                        report.errors++
                    }
                }
            }
            msg(sender, "<green>ᴅᴇᴛᴇᴄᴛᴇᴅ ᴍᴏᴅᴇʀɴ ᴘᴇʀ-sʜᴏᴘ ꜰᴏʀᴍᴀᴛ. ꜰᴏᴜɴᴅ <yellow>${shopConfigs.size} <green>sʜᴏᴘ ꜰɪʟᴇs.")
        }

        if (legacyGuisFile.exists() && shopConfigs.isEmpty()) {

            try {
                val config = YamlConfiguration.loadConfiguration(legacyGuisFile)
                for (key in config.getKeys(false)) {
                    val singleShopConfig = YamlConfiguration()
                    val section = config.getConfigurationSection(key)
                    if (section != null) {

                        for (subKey in section.getKeys(true)) {
                            singleShopConfig.set("$key.$subKey", section.get(subKey))
                        }
                        shopConfigs[key.lowercase()] = singleShopConfig
                    }
                }
                msg(sender, "<green>ᴅᴇᴛᴇᴄᴛᴇᴅ ʟᴇɢᴀᴄʏ ɢᴜɪs.ʏᴍʟ ꜰᴏʀᴍᴀᴛ. ꜰᴏᴜɴᴅ <yellow>${shopConfigs.size} <green>sʜᴏᴘs.")
            } catch (ex: Exception) {
                msg(sender, "<red>Failed to parse ${legacyGuisFile.absolutePath}: <gray>${ex.message}")
                report.error = "Failed to parse guis.yml"
                return report
            }
        }

        if (shopConfigs.isEmpty()) {
            msg(sender, "<red>ɴᴏ sʜᴏᴘ ᴅᴀᴛᴀ ꜰᴏᴜɴᴅ ɪɴ <yellow>${dtlFolder.name}")
            report.error = "No shops found"
            return report
        }

        val dtlConfig = File(dtlFolder, "config.yml")
        val dtlConfigYml = if (dtlConfig.exists()) YamlConfiguration.loadConfiguration(dtlConfig) else null

        for ((shopId, shopYml) in shopConfigs) {
            try {

                val rootKey = shopYml.getKeys(false).firstOrNull() ?: continue
                val shopSection = shopYml.getConfigurationSection(rootKey) ?: continue

                val catalog = convertShop(shopId, shopSection, report)
                plugin.catalogRepository.register(catalog)
                report.catalogsMigrated++
                msg(sender, "  <gray>✓ <white>ᴍɪɢʀᴀᴛᴇᴅ sʜᴏᴘ <yellow>$shopId <white>→ ᴄᴀᴛᴀʟᴏɢ <green>${catalog.identifier} " +
                        "<gray>(${catalog.sections.size} sᴇᴄᴛɪᴏɴs)")
            } catch (ex: Exception) {
                report.errors++
                msg(sender, "  <red>✗ <white>ꜰᴀɪʟᴇᴅ ᴛᴏ ᴍɪɢʀᴀᴛᴇ sʜᴏᴘ <yellow>$shopId<white>: <gray>${ex.message}")
                plugin.logger.warning("Migration error for shop $shopId: ${ex.stackTraceToString()}")
            }
        }

        try {
            val backupName = "${dtlFolder.name}_pre-sovereign-backup"
            val backupDir = File(pluginsDir, backupName)
            if (!backupDir.exists()) {
                dtlFolder.copyRecursively(backupDir)
                msg(sender, "<green>ᴏʀɪɢɪɴᴀʟ ᴅᴛʟ ᴅᴀᴛᴀ ʙᴀᴄᴋᴇᴅ ᴜᴘ ᴛᴏ <yellow>$backupName")
            } else {
                msg(sender, "<yellow>ʙᴀᴄᴋᴜᴘ ꜰᴏʟᴅᴇʀ ᴀʟʀᴇᴀᴅʏ ᴇxɪsᴛs: <gray>$backupName")
            }
        } catch (ex: Exception) {
            msg(sender, "<yellow>ᴡᴀʀɴɪɴɢ: ᴄᴏᴜʟᴅ ɴᴏᴛ ᴄʀᴇᴀᴛᴇ ʙᴀᴄᴋᴜᴘ — <gray>${ex.message}")
        }

        plugin.catalogRepository.persistAll()

        msg(sender, "")
        msg(sender, "<white><bold>ᴍɪɢʀᴀᴛɪᴏɴ ᴄᴏᴍᴘʟᴇᴛᴇ</bold>")
        msg(sender, "<green>ᴄᴀᴛᴀʟᴏɢs ᴄʀᴇᴀᴛᴇᴅ: <yellow>${report.catalogsMigrated}")
        msg(sender, "<green>sᴇᴄᴛɪᴏɴs ᴄʀᴇᴀᴛᴇᴅ: <yellow>${report.sectionsMigrated}")
        msg(sender, "<green>ʟɪsᴛɪɴɢs ᴍɪɢʀᴀᴛᴇᴅ: <yellow>${report.listingsMigrated}")
        if (report.errors > 0) {
            msg(sender, "<red>ᴇʀʀᴏʀs ᴇɴᴄᴏᴜɴᴛᴇʀᴇᴅ: <yellow>${report.errors}")
        }
        msg(sender, "<gray>ᴜsᴇ <yellow>/sovereign catalog list <gray>ᴛᴏ ᴠᴇʀɪꜰʏ.")

        return report
    }

    private fun convertShop(shopId: String, section: ConfigurationSection, report: MigrationReport): Catalog {
        val title = stripLegacyColors(section.getString("title", shopId) ?: shopId)
        val permission = section.getString("permission")?.takeIf { it != "none" && it.isNotBlank() }
        val defaultShop = section.getString("default-shop", "buy") ?: "buy"
        val buyEnabled = section.getBoolean("buy-shop-enabled", true)
        val sellEnabled = section.getBoolean("sell-shop-enabled", true)
        val tradeEnabled = section.getBoolean("trade-shop-enabled", true)

        val catalog = Catalog(
            identifier = sanitizeIdentifier(shopId),
            displayTitle = title,
            authorization = permission,
            acquireEnabled = buyEnabled,
            liquidateEnabled = sellEnabled,
            barterEnabled = tradeEnabled
        )

        val pagesSection = section.getConfigurationSection("pages")
        if (pagesSection != null) {
            val pageKeys = pagesSection.getKeys(false).toList()
                .sortedBy { extractPageIndex(it) }

            for (pageKey in pageKeys) {
                val pageSection = pagesSection.getConfigurationSection(pageKey) ?: continue
                try {
                    val catalogSection = convertPage(pageSection, report)
                    catalog.sections.add(catalogSection)
                    report.sectionsMigrated++
                } catch (ex: Exception) {
                    report.errors++
                    plugin.logger.warning("Failed to convert page $pageKey in shop $shopId: ${ex.message}")
                }
            }
        }

        if (catalog.sections.isEmpty()) {
            catalog.sections.add(CatalogSection(identifier = "main", rowCount = 4))
        }

        return catalog
    }

    private fun convertPage(pageSection: ConfigurationSection, report: MigrationReport): CatalogSection {
        val size = pageSection.getInt("size", 54)

        val rowCount = (size / 9).coerceIn(1, 5)
        val pageName = stripLegacyColors(pageSection.getString("page-name", "main") ?: "main")
        val pagePermission = pageSection.getString("page-permission")
            ?.takeIf { it != "none" && it.isNotBlank() }

        val section = CatalogSection(
            identifier = sanitizeIdentifier(pageName),
            rowCount = rowCount,
            authorization = pagePermission
        )

        convertItemSlots(pageSection, "buy-items", section.acquireListings, TransactionMode.ACQUIRE, report)

        convertItemSlots(pageSection, "sell-items", section.liquidateListings, TransactionMode.LIQUIDATE, report)

        convertItemSlots(pageSection, "trade-items", section.barterListings, TransactionMode.BARTER, report)

        return section
    }

    private fun convertItemSlots(
        pageSection: ConfigurationSection,
        itemsKey: String,
        target: Array<Listing?>,
        mode: TransactionMode,
        report: MigrationReport
    ) {
        val itemsSection = pageSection.getConfigurationSection(itemsKey) ?: return

        for (slotKey in itemsSection.getKeys(false)) {
            val itemSection = itemsSection.getConfigurationSection(slotKey) ?: continue
            val slot = extractSlotIndex(slotKey)
            if (slot < 0 || slot >= target.size) continue

            try {
                val type = itemSection.getString("type", "tradable")?.lowercase() ?: "tradable"
                val listing = when (type) {
                    "tradable" -> convertTradableItem(itemSection, mode)
                    "static" -> convertStaticItem(itemSection)
                    "commands" -> convertCommandsItem(itemSection, mode)
                    "trade" -> convertTradeItem(itemSection)
                    else -> convertTradableItem(itemSection, mode)
                }
                target[slot] = listing
                report.listingsMigrated++
            } catch (ex: Exception) {
                report.errors++
                plugin.logger.warning("Failed to convert item at slot $slotKey ($itemsKey): ${ex.message}")
            }
        }
    }

    private fun convertTradableItem(section: ConfigurationSection, mode: TransactionMode): Listing {
        val uid = nextUid()
        val materialId = resolveItemMaterial(section)
        val stackQuantity = resolveItemQuantity(section)
        val actualItem = resolveActualItemStack(section)
        val displayName = section.getString("display-name")?.let { stripLegacyColors(it) }
        val description = section.getStringList("description").map { stripLegacyColors(it) }.toMutableList()
        val tradePrice = section.getDouble("trade-price", 0.0)

        val listing = Listing(
            uid = uid,
            type = ListingType.TRADABLE,
            materialId = materialId,
            stackQuantity = stackQuantity,
            itemStack = actualItem,
            label = displayName,
            annotations = description,
            showAnnotations = section.getBoolean("show-description", true),
            showCost = section.getBoolean("show-price", true),

            acquireCost = if (mode == TransactionMode.ACQUIRE) tradePrice else 0.0,
            liquidateReward = if (mode == TransactionMode.LIQUIDATE) tradePrice else 0.0,

            reductionActive = section.getBoolean("Discount.discount-enabled", false),
            reductionPercent = section.getDouble("Discount.discount-percentage", 0.0),
            reductionOverrideCost = section.getDouble("Discount.discount-price", 0.0),
            reductionWindowStart = section.getString("Discount.discount-start"),
            reductionWindowDurationSec = section.getInt("Discount.duration", 0),
            reductionWindowEnd = section.getString("Discount.discount-end"),
            showReductionCost = section.getBoolean("Discount.show-discount-price", false),
            showReductionStart = section.getBoolean("Discount.show-discount-start", false),
            showReductionDuration = section.getBoolean("Discount.show-discount-duration", false),
            showReductionEnd = section.getBoolean("Discount.show-discount-end", false),

            quotaLimit = section.getInt("trade-limit", 0).let { if (it < 0) 0 else it },
            quotaResetIntervalSec = section.getInt("limit-reset-seconds", 0).let { if (it < 0) 0 else it },
            showQuotaProgress = section.getBoolean("show-trade-limit", false),
            showQuotaTimer = section.getBoolean("show-limit-time", false),

            authorization = section.getString("permission")?.takeIf { it.isNotBlank() },
            dropOnOverflow = section.getBoolean("drop-item-on-full-inventory", false),
            liquidatePartialAllowed = section.getBoolean("sell-all-when-not-enough-items", false),
            broadcastOnTransaction = section.getBoolean("show-broadcast-message", false),
            broadcastTemplate = stripLegacyColors(section.getString("broadcast-message", "") ?: "")
        )

        val cmdSection = section.getConfigurationSection("on-buy-sell-commands")
        if (cmdSection != null) {
            for (cmdKey in cmdSection.getKeys(false)) {
                val cmdConfig = cmdSection.getConfigurationSection(cmdKey) ?: continue
                val executor = cmdConfig.getString("executor", "CONSOLE") ?: "CONSOLE"
                val command = cmdConfig.getString("command", "") ?: ""
                listing.directives.add(
                    Directive(
                        command = command,
                        executor = DirectiveExecutor.fromString(mapExecutor(executor))
                    )
                )
            }
        }

        return listing
    }

    private fun convertStaticItem(section: ConfigurationSection): Listing {
        val uid = nextUid()
        val materialId = resolveItemMaterial(section)
        val stackQuantity = resolveItemQuantity(section)
        val actualItem = resolveActualItemStack(section)
        val displayName = section.getString("display-name")?.let { stripLegacyColors(it) }
        val description = section.getStringList("description").map { stripLegacyColors(it) }.toMutableList()

        return Listing(
            uid = uid,
            type = ListingType.STATIC,
            materialId = materialId,
            stackQuantity = stackQuantity,
            itemStack = actualItem,
            label = displayName,
            annotations = description,
            showAnnotations = section.getBoolean("show-description", true),
            acquireCost = 0.0,
            liquidateReward = 0.0,
            showCost = false
        )
    }

    private fun convertCommandsItem(section: ConfigurationSection, mode: TransactionMode): Listing {
        val uid = nextUid()
        val materialId = resolveItemMaterial(section)
        val stackQuantity = resolveItemQuantity(section)
        val actualItem = resolveActualItemStack(section)
        val displayName = section.getString("display-name")?.let { stripLegacyColors(it) }
        val description = section.getStringList("description").map { stripLegacyColors(it) }.toMutableList()
        val tradePrice = section.getDouble("trade-price", 0.0)
        val runMode = section.getString("run-mode", "BUY_AND_RUN") ?: "BUY_AND_RUN"

        val listing = Listing(
            uid = uid,
            type = ListingType.DIRECTIVE,
            materialId = materialId,
            stackQuantity = stackQuantity,
            itemStack = actualItem,
            label = displayName,
            annotations = description,
            showAnnotations = section.getBoolean("show-description", true),
            showCost = section.getBoolean("show-price", true),
            acquireCost = tradePrice,
            liquidateReward = 0.0,

            reductionActive = section.getBoolean("Discount.discount-enabled", false),
            reductionPercent = section.getDouble("Discount.discount-percentage", 0.0),
            reductionOverrideCost = section.getDouble("Discount.discount-price", 0.0),
            reductionWindowStart = section.getString("Discount.discount-start"),
            reductionWindowDurationSec = section.getInt("Discount.duration", 0),
            reductionWindowEnd = section.getString("Discount.discount-end"),
            showReductionCost = section.getBoolean("Discount.show-discount-price", false),
            showReductionStart = section.getBoolean("Discount.show-discount-start", false),
            showReductionDuration = section.getBoolean("Discount.show-discount-duration", false),
            showReductionEnd = section.getBoolean("Discount.show-discount-end", false),

            quotaLimit = section.getInt("trade-limit", 0).let { if (it < 0) 0 else it },
            quotaResetIntervalSec = section.getInt("limit-reset-seconds", 0).let { if (it < 0) 0 else it },
            showQuotaProgress = section.getBoolean("show-trade-limit", false),
            showQuotaTimer = section.getBoolean("show-limit-time", false),

            authorization = section.getString("permission")?.takeIf { it.isNotBlank() },
            dropOnOverflow = section.getBoolean("drop-item-on-full-inventory", false),
            liquidatePartialAllowed = section.getBoolean("sell-all-when-not-enough-items", false),
            broadcastOnTransaction = section.getBoolean("show-broadcast-message", false),
            broadcastTemplate = stripLegacyColors(section.getString("broadcast-message", "") ?: ""),

            directiveRunMode = mapRunMode(runMode)
        )

        val cmdSection = section.getConfigurationSection("commands")
        if (cmdSection != null) {
            for (cmdKey in cmdSection.getKeys(false)) {
                val cmdConfig = cmdSection.getConfigurationSection(cmdKey) ?: continue
                val executor = cmdConfig.getString("executor", "CONSOLE") ?: "CONSOLE"
                val command = cmdConfig.getString("command", "") ?: ""
                listing.directives.add(
                    Directive(
                        command = command,
                        executor = DirectiveExecutor.fromString(mapExecutor(executor))
                    )
                )
            }
        }

        return listing
    }

    private fun convertTradeItem(section: ConfigurationSection): Listing {
        val uid = nextUid()
        val materialId = resolveItemMaterial(section)
        val stackQuantity = resolveItemQuantity(section)
        val actualItem = resolveActualItemStack(section)
        val displayName = section.getString("display-name")?.let { stripLegacyColors(it) }
        val description = section.getStringList("description").map { stripLegacyColors(it) }.toMutableList()
        val tradePrice = section.getDouble("trade-price", 0.0)

        val listing = Listing(
            uid = uid,
            type = ListingType.BARTER,
            materialId = materialId,
            stackQuantity = stackQuantity,
            itemStack = actualItem,
            label = displayName,
            annotations = description,
            showAnnotations = section.getBoolean("show-description", true),
            showCost = tradePrice > 0.0,
            acquireCost = tradePrice,
            liquidateReward = 0.0,

            quotaLimit = section.getInt("trade-limit", 0).let { if (it < 0) 0 else it },
            quotaResetIntervalSec = section.getInt("limit-reset-seconds", 0).let { if (it < 0) 0 else it },
            showQuotaProgress = section.getBoolean("show-trade-limit", false),
            showQuotaTimer = section.getBoolean("show-limit-time", false),

            authorization = section.getString("permission")?.takeIf { it.isNotBlank() },
            dropOnOverflow = section.getBoolean("drop-item-on-full-inventory", false),
            broadcastOnTransaction = section.getBoolean("show-broadcast-message", false),
            broadcastTemplate = stripLegacyColors(section.getString("broadcast-message", "") ?: "")
        )

        val obtainableSection = section.getConfigurationSection("obtainable-items")
        if (obtainableSection != null) {
            for (key in obtainableSection.getKeys(false)) {
                val itemConfig = obtainableSection.getConfigurationSection(key) ?: continue
                val itemStack = itemConfig.getItemStack("item") ?: continue
                listing.barterReceivables.add(
                    BarterComponent(
                        materialId = itemStack.type.name,
                        quantity = itemStack.amount,
                        label = itemStack.itemMeta?.let {
                            @Suppress("DEPRECATION")
                            if (it.hasDisplayName()) stripLegacyColors(it.displayName) else null
                        }
                    )
                )
            }
        }

        val neededSection = section.getConfigurationSection("needed-items")
        if (neededSection != null) {
            for (key in neededSection.getKeys(false)) {
                val itemConfig = neededSection.getConfigurationSection(key) ?: continue
                val itemStack = itemConfig.getItemStack("item") ?: continue
                listing.barterRequirements.add(
                    BarterComponent(
                        materialId = itemStack.type.name,
                        quantity = itemStack.amount,
                        label = itemStack.itemMeta?.let {
                            @Suppress("DEPRECATION")
                            if (it.hasDisplayName()) stripLegacyColors(it.displayName) else null
                        }
                    )
                )
            }
        }

        val cmdSection = section.getConfigurationSection("on-trade-commands")
        if (cmdSection != null) {
            for (cmdKey in cmdSection.getKeys(false)) {
                val cmdConfig = cmdSection.getConfigurationSection(cmdKey) ?: continue
                val executor = cmdConfig.getString("executor", "CONSOLE") ?: "CONSOLE"
                val command = cmdConfig.getString("command", "") ?: ""
                listing.directives.add(
                    Directive(
                        command = command,
                        executor = DirectiveExecutor.fromString(mapExecutor(executor))
                    )
                )
            }
        }

        return listing
    }

    private fun resolveItemMaterial(section: ConfigurationSection): String {

        if (section.isItemStack("item")) {
            val itemStack = section.getItemStack("item")
            if (itemStack != null) return itemStack.type.name
        }

        val itemSection = section.getConfigurationSection("item")
        if (itemSection != null) {

            val type = itemSection.getString("type")
                ?: itemSection.getString("material")
                ?: itemSection.getString("v")
            if (type != null) return type.uppercase().replace(" ", "_")
        }

        return "STONE"
    }

    private fun resolveActualItemStack(section: ConfigurationSection): org.bukkit.inventory.ItemStack? {
        if (section.isItemStack("item")) {
            return section.getItemStack("item")
        }

        val itemSec = section.getConfigurationSection("item")
        if (itemSec != null) {
            val typeStr = itemSec.getString("type")?.uppercase()?.replace(" ", "_") ?: "STONE"
            val material = org.bukkit.Material.matchMaterial(typeStr) ?: org.bukkit.Material.STONE

            var amount = itemSec.getInt("amount", 1)
            if (amount < 1) amount = 1
            if (amount > material.maxStackSize) amount = material.maxStackSize

            val item = org.bukkit.inventory.ItemStack(material, amount)

            if (itemSec.contains("damage")) {
                var damage = itemSec.getInt("damage", 0)
                if (damage > 0) {
                    val meta = item.itemMeta
                    if (meta is org.bukkit.inventory.meta.Damageable) {
                        meta.damage = damage
                        item.itemMeta = meta
                    }
                }
            }

            item.editMeta { meta ->
                if (itemSec.contains("name")) {
                    val nameStr = itemSec.getString("name") ?: ""

                    val cmp = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(nameStr)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                    meta.displayName(cmp)
                }

                if (itemSec.contains("lore")) {
                    val loreStrs = itemSec.getStringList("lore")
                    val loreCmps = loreStrs.map {
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(it)
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                    }
                    meta.lore(loreCmps)
                }
            }
            return item
        }

        return null
    }

    private fun resolveItemQuantity(section: ConfigurationSection): Int {
        if (section.isItemStack("item")) {
            val itemStack = section.getItemStack("item")
            if (itemStack != null) return itemStack.amount
        }
        val itemSection = section.getConfigurationSection("item")
        if (itemSection != null) {
            return itemSection.getInt("amount", 1).coerceAtLeast(1)
        }
        return 1
    }

    private fun stripLegacyColors(input: String): String {
        return net.sovereign.core.config.LegacyMigrationUtil.legacyToMiniMessage(input.replace("§", "&"))
    }

    private fun mapRunMode(dtlRunMode: String): DirectiveRunMode = when (dtlRunMode.uppercase()) {
        "RUN_ONLY" -> DirectiveRunMode.EXECUTE_ONLY
        "BUY_AND_RUN" -> DirectiveRunMode.ACQUIRE_AND_EXECUTE
        "BUY_AND_KEEP" -> DirectiveRunMode.ACQUIRE_AND_RETAIN
        else -> DirectiveRunMode.EXECUTE_ONLY
    }

    private fun mapExecutor(dtlExecutor: String): String = when (dtlExecutor.uppercase()) {
        "CONSOLE" -> "CONSOLE"
        "PLAYER" -> "PLAYER"
        else -> "CONSOLE"
    }

    private fun extractPageIndex(key: String): Int {
        return key.removePrefix("page-").toIntOrNull() ?: 0
    }

    private fun extractSlotIndex(key: String): Int {
        return key.removePrefix("item-").toIntOrNull() ?: -1
    }

    private fun sanitizeIdentifier(input: String): String {

        var clean = input.replace(Regex("[§&][0-9a-fk-or]", RegexOption.IGNORE_CASE), "")

        clean = clean.replace(Regex("[\\s_]+"), "-")

        clean = clean.replace(Regex("[^a-zA-Z0-9-]"), "")

        clean = clean.lowercase().trim('-')
        return clean.ifEmpty { "migrated-${nextUid()}" }
    }

    private fun nextUid(): Int = uidCounter++

    private fun msg(sender: CommandSender, text: String) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» $text"))
    }

    data class MigrationReport(
        var sourceFolderName: String = "",
        var catalogsMigrated: Int = 0,
        var sectionsMigrated: Int = 0,
        var listingsMigrated: Int = 0,
        var errors: Int = 0,
        var error: String? = null
    )
}
