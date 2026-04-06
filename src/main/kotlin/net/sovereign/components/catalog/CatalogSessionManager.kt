package net.sovereign.components.catalog

import net.bitbylogic.menus.Menu
import net.bitbylogic.menus.MenuFlag
import net.bitbylogic.menus.data.MenuData
import net.bitbylogic.menus.item.MenuItem
import net.bitbylogic.menus.listener.MenuListener
import net.sovereign.core.SovereignCore
import net.sovereign.components.economy.TransactionProcessor
import net.sovereign.components.economy.TransactionResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class CatalogSessionManager(private val plugin: SovereignCore) {

    private val activeSessions = mutableMapOf<UUID, BrowsingSession>()
    private val reopeningPlayers = mutableSetOf<UUID>()

    val menuListener = SovereignMenuListener(plugin)

    data class BrowsingSession(
        val playerId: UUID,
        val catalog: Catalog,
        var sectionIndex: Int,
        var mode: TransactionMode,
        @Transient var inventory: org.bukkit.inventory.Inventory? = null,
        @Transient var quotaTimerSlots: List<Pair<Int, Listing>> = emptyList()
    )

    private var refreshTask: Any? = null

    init {
        startTimerRefresh()
    }

    private fun startTimerRefresh() {
        refreshTask = net.sovereign.core.scheduler.SovereignScheduler.runTaskTimer(plugin.plugin, Runnable {
            for ((_, session) in activeSessions) {
                val inv = session.inventory ?: continue
                val slots = session.quotaTimerSlots
                if (slots.isEmpty()) continue
                val player = Bukkit.getPlayer(session.playerId) ?: continue
                if (player.openInventory.topInventory !== inv) continue

                val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0
                for ((slot, listing) in slots) {
                    val updatedItem = ListingRenderer.render(listing, session.mode, plugin, catalogDiscount, session.playerId)
                    inv.setItem(slot, updatedItem)
                }
            }
        }, 20L, 20L)
    }

    fun shutdown() {
        if (refreshTask != null) {
            net.sovereign.core.scheduler.SovereignScheduler.cancelTask(refreshTask)
            refreshTask = null
        }
    }

    fun openSession(player: Player, catalog: Catalog, sectionIndex: Int, mode: TransactionMode) {
        if (catalog.sections.isEmpty()) {
            plugin.localeManager.dispatch(player, "errors.empty-catalog-sections")
            return
        }

        val clampedIndex = sectionIndex.coerceIn(0, catalog.sections.size - 1)
        val section = catalog.sections[clampedIndex]

        if (catalog.authorization != null && !player.hasPermission(catalog.authorization!!)) {
            plugin.localeManager.dispatch(player, "general.restricted-access")
            return
        }
        if (section.authorization != null && !player.hasPermission(section.authorization!!)) {
            plugin.localeManager.dispatch(player, "general.restricted-access")
            return
        }

        reopeningPlayers.add(player.uniqueId)
        val session = BrowsingSession(player.uniqueId, catalog, clampedIndex, mode)
        activeSessions[player.uniqueId] = session

        val (menu, inventory) = buildCatalogMenu(session, section)
        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            player.openInventory(inventory)
        }, 1L)

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            reopeningPlayers.remove(player.uniqueId)
        }, 5L)
    }

    private fun buildCatalogMenu(session: BrowsingSession, section: CatalogSection): Pair<Menu, org.bukkit.inventory.Inventory> {
        val gui = plugin.guiConfigManager
        val locale = plugin.localeManager

        val totalSlots = section.capacity + 9
        val title = locale.resolveFragment(
            "catalog.header",
            "%catalog%" to session.catalog.displayTitle,
            "%section%" to section.identifier,
            "%mode%" to session.mode.displayName
        )

        val menuData = MenuData()
            .withFlag(MenuFlag.DISABLE_TITLE_UPDATE)
            .withFlag(MenuFlag.DISABLE_UPDATES)
            .withMaxInventories(0)
            .withCloseAction { event ->
                val player = event.player as? Player ?: return@withCloseAction

                if (reopeningPlayers.contains(player.uniqueId)) return@withCloseAction
                activeSessions.remove(player.uniqueId)
            }

        val menu = Menu("catalog-${session.catalog.displayTitle}", "catalog", totalSlots, menuData)

        val inventory = Bukkit.createInventory(menu, totalSlots, title)

        val listings = section.listingsForMode(session.mode)
        val quotaSlots = mutableListOf<Pair<Int, Listing>>()
        for (slot in listings.indices) {
            val listing = listings[slot] ?: continue
            val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0
            val displayItem = ListingRenderer.render(listing, session.mode, plugin, catalogDiscount, session.playerId)

            if (listing.showQuotaTimer && listing.quotaResetIntervalSec > 0) {
                quotaSlots.add(slot to listing)
            }

            val menuItem = MenuItem("listing-$slot")
                .item(displayItem)
                .withSlot(slot)
                .withSourceInventory(inventory)
                .withAction { event ->
                    val player = event.whoClicked as? Player ?: return@withAction
                    val activeSession = activeSessions[player.uniqueId] ?: return@withAction
                    handleListingClick(player, activeSession, listing, event.isShiftClick)
                }
            menu.addItem(menuItem)
            inventory.setItem(slot, displayItem)
        }
        session.inventory = inventory
        session.quotaTimerSlots = quotaSlots

        val navRowStart = section.capacity

        for (offset in gui.retreatSlots) {
            val navStack = ListingRenderer.renderNavButton(
                Material.matchMaterial(gui.retreatMaterial) ?: Material.EMERALD_BLOCK,
                gui.retreatLabel
            )
            val retreatItem = MenuItem("retreat-$offset")
                .item(navStack)
                .withSlot(navRowStart + offset)
                .withSourceInventory(inventory)
                .withAction { event ->
                    val player = event.whoClicked as? Player ?: return@withAction
                    val s = activeSessions[player.uniqueId] ?: return@withAction
                    if (s.sectionIndex > 0) {
                        s.sectionIndex--
                        openSession(player, s.catalog, s.sectionIndex, s.mode)
                    }
                }
            menu.addItem(retreatItem)
            inventory.setItem(navRowStart + offset, navStack)
        }

        for (offset in gui.advanceSlots) {
            val navStack = ListingRenderer.renderNavButton(
                Material.matchMaterial(gui.advanceMaterial) ?: Material.EMERALD_BLOCK,
                gui.advanceLabel
            )
            val advanceItem = MenuItem("advance-$offset")
                .item(navStack)
                .withSlot(navRowStart + offset)
                .withSourceInventory(inventory)
                .withAction { event ->
                    val player = event.whoClicked as? Player ?: return@withAction
                    val s = activeSessions[player.uniqueId] ?: return@withAction
                    if (s.sectionIndex < s.catalog.sections.size - 1) {
                        s.sectionIndex++
                        openSession(player, s.catalog, s.sectionIndex, s.mode)
                    }
                }
            menu.addItem(advanceItem)
            inventory.setItem(navRowStart + offset, navStack)
        }

        for (offset in gui.dismissSlots) {
            val navStack = ListingRenderer.renderNavButton(
                Material.matchMaterial(gui.dismissMaterial) ?: Material.REDSTONE_BLOCK,
                gui.dismissLabel
            )
            val dismissItem = MenuItem("dismiss-$offset")
                .item(navStack)
                .withSlot(navRowStart + offset)
                .withSourceInventory(inventory)
                .withAction { event -> event.whoClicked.closeInventory() }
            menu.addItem(dismissItem)
            inventory.setItem(navRowStart + offset, navStack)
        }

        val toggleMaterial = when (session.mode) {
            TransactionMode.ACQUIRE -> Material.matchMaterial(gui.acquireMaterial) ?: Material.GOLDEN_APPLE
            TransactionMode.LIQUIDATE -> Material.matchMaterial(gui.liquidateMaterial) ?: Material.STONE
            TransactionMode.BARTER -> Material.matchMaterial(gui.barterMaterial) ?: Material.REDSTONE_BLOCK
        }
        val toggleLabel = when (session.mode) {
            TransactionMode.ACQUIRE -> gui.acquireLabel
            TransactionMode.LIQUIDATE -> gui.liquidateLabel
            TransactionMode.BARTER -> gui.barterLabel
        }

        val toggleStack = ListingRenderer.renderNavButton(toggleMaterial, toggleLabel)
        val toggleItem = MenuItem("mode-toggle")
            .item(toggleStack)
            .withSlot(navRowStart + gui.modeToggleSlot)
            .withSourceInventory(inventory)
            .withAction { event ->
                val player = event.whoClicked as? Player ?: return@withAction
                val s = activeSessions[player.uniqueId] ?: return@withAction
                s.mode = s.catalog.cycleMode(s.mode)
                openSession(player, s.catalog, s.sectionIndex, s.mode)
            }
        menu.addItem(toggleItem)
        inventory.setItem(navRowStart + gui.modeToggleSlot, toggleStack)

        val fillerMat = Material.matchMaterial(gui.catalogFillerMaterial) ?: Material.BLACK_STAINED_GLASS_PANE
        val fillerStack = ItemStack(fillerMat).also {
            it.editMeta { meta -> meta.displayName(net.kyori.adventure.text.Component.text(" ")) }
        }
        for (i in navRowStart until totalSlots) {
            if (inventory.getItem(i) == null) {
                val filler = MenuItem("filler-$i")
                    .item(fillerStack.clone())
                    .withSlot(i)
                    .withSourceInventory(inventory)
                menu.addItem(filler)
                inventory.setItem(i, fillerStack.clone())
            }
        }

        return Pair(menu, inventory)
    }

    private fun openConfirmationGui(player: Player, session: BrowsingSession, listing: Listing, quantity: Int) {
        val gui = plugin.guiConfigManager
        val locale = plugin.localeManager
        val mm = MiniMessage.miniMessage()
        val dynMultiplier = plugin.pricingModule.multiplierFor(listing.uid)
        val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0

        val costString = when {
            session.mode == TransactionMode.BARTER -> "ʙᴀʀᴛᴇʀ"
            session.mode == TransactionMode.ACQUIRE -> locale.formatCurrency(
                TransactionProcessor.computeCost(
                    listing.acquireCost, listing.stackQuantity, quantity,
                    listing.reductionActive, listing.reductionOverrideCost,
                    dynMultiplier, catalogDiscount
                )
            )
            else -> locale.formatCurrency(
                TransactionProcessor.computeCost(
                    listing.liquidateReward, listing.stackQuantity, quantity,
                    dynamicMultiplier = dynMultiplier,
                    catalogDiscountPercent = catalogDiscount
                )
            )
        }

        val rawLabel = listing.label ?: listing.materialId

        val itemLabel = mm.stripTags(rawLabel)

        val title = mm.deserialize(
            gui.confirmationTitle
                .replace("%item%", itemLabel)
                .replace("%cost%", costString)
                .replace("%mode%", session.mode.displayName)
        )

        val menuData = MenuData()
            .withFlag(MenuFlag.DISABLE_TITLE_UPDATE)
            .withFlag(MenuFlag.DISABLE_UPDATES)
            .withMaxInventories(0)

        val menu = Menu("confirm-${player.uniqueId}", "confirmation", 9, menuData)
        val inventory = Bukkit.createInventory(menu, 9, title)

        val previewItem = ListingRenderer.render(listing, session.mode, plugin, catalogDiscount, player.uniqueId).clone()
        previewItem.amount = quantity.coerceIn(1, previewItem.type.maxStackSize)

        val existingLore = previewItem.getData(DataComponentTypes.LORE)?.lines() ?: emptyList()
        val confirmLoreLines = mutableListOf<Component>()
        confirmLoreLines.addAll(existingLore)
        confirmLoreLines.add(Component.empty())
        confirmLoreLines.add(mm.deserialize("<gray>ᴍᴏᴅᴇ: <white>${session.mode.displayName}"))
        if (session.mode != TransactionMode.BARTER) {
            confirmLoreLines.add(mm.deserialize("<gray>ᴄᴏsᴛ: <yellow>$$costString"))
        }
        confirmLoreLines.add(Component.empty())
        confirmLoreLines.add(mm.deserialize("<green>ᴄʟɪᴄᴋ ᴛʜᴇ ɢʀᴇᴇɴ ᴘᴀɴᴇ ᴛᴏ ᴄᴏɴꜰɪʀᴍ"))
        confirmLoreLines.add(mm.deserialize("<red>ᴄʟɪᴄᴋ ᴛʜᴇ ʀᴇᴅ ᴘᴀɴᴇ ᴛᴏ ᴄᴀɴᴄᴇʟ"))
        previewItem.setData(DataComponentTypes.LORE, ItemLore.lore(confirmLoreLines))

        val previewMenuItem = MenuItem("preview")
            .item(previewItem)
            .withSlot(4)
            .withSourceInventory(inventory)
        menu.addItem(previewMenuItem)
        inventory.setItem(4, previewItem)

        val confirmMat = Material.matchMaterial(gui.confirmationConfirmMaterial) ?: Material.LIME_STAINED_GLASS_PANE
        val confirmStack = ListingRenderer.renderNavButton(confirmMat, gui.confirmationConfirmLabel)
        for (slot in 0..2) {
            val confirmItem = MenuItem("confirm-$slot")
                .item(confirmStack.clone())
                .withSlot(slot)
                .withSourceInventory(inventory)
                .withAction { event ->
                    val clicker = event.whoClicked as? Player ?: return@withAction
                    clicker.closeInventory()
                    executeTransaction(clicker, session, listing, quantity)
                }
            menu.addItem(confirmItem)
            inventory.setItem(slot, confirmStack.clone())
        }

        val cancelMat = Material.matchMaterial(gui.confirmationCancelMaterial) ?: Material.RED_STAINED_GLASS_PANE
        val cancelStack = ListingRenderer.renderNavButton(cancelMat, gui.confirmationCancelLabel)
        for (slot in 6..8) {
            val cancelItem = MenuItem("cancel-$slot")
                .item(cancelStack.clone())
                .withSlot(slot)
                .withSourceInventory(inventory)
                .withAction { event ->
                    val clicker = event.whoClicked as? Player ?: return@withAction
                    clicker.closeInventory()
                    locale.dispatch(clicker, "operations.confirmation-cancelled")
                }
            menu.addItem(cancelItem)
            inventory.setItem(slot, cancelStack.clone())
        }

        val fillerMat = Material.matchMaterial(gui.confirmationFillerMaterial) ?: Material.BLACK_STAINED_GLASS_PANE
        val fillerStack = ItemStack(fillerMat).also {
            it.editMeta { meta -> meta.displayName(Component.text(" ")) }
        }
        for (slot in listOf(3, 5)) {
            val filler = MenuItem("filler-$slot")
                .item(fillerStack.clone())
                .withSlot(slot)
                .withSourceInventory(inventory)
            menu.addItem(filler)
            inventory.setItem(slot, fillerStack.clone())
        }

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            player.openInventory(inventory)
        }, 1L)
    }

    private fun openQuantitySelectorGui(player: Player, session: BrowsingSession, listing: Listing) {
        val gui = plugin.guiConfigManager
        val mm = MiniMessage.miniMessage()
        val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0

        val previewItem = ListingRenderer.render(listing, session.mode, plugin, catalogDiscount, player.uniqueId).clone()
        val presets = QuantityPresets.resolve(listing.stackQuantity, previewItem.type.maxStackSize)

        val rawLabel = listing.label ?: listing.materialId
        val itemLabel = mm.stripTags(rawLabel)
        val title = mm.deserialize(
            gui.quantitySelectorTitle
                .replace("%item%", itemLabel)
                .replace("%mode%", session.mode.displayName)
        )

        val menuData = MenuData()
            .withFlag(MenuFlag.DISABLE_TITLE_UPDATE)
            .withFlag(MenuFlag.DISABLE_UPDATES)
            .withMaxInventories(0)

        val menu = Menu("quantity-${player.uniqueId}", "quantity", 27, menuData)
        val inventory = Bukkit.createInventory(menu, 27, title)

        val startSlot = 13 - (presets.size / 2)

        presets.forEachIndexed { index, quantity ->
            val slot = startSlot + index
            
            val displayListing = listing.copy(stackQuantity = quantity)
            val displayItem = ListingRenderer.render(displayListing, session.mode, plugin, catalogDiscount, player.uniqueId)
            displayItem.amount = quantity

            val presetItem = MenuItem("quantity-$quantity")
                .item(displayItem)
                .withSlot(slot)
                .withSourceInventory(inventory)
                .withAction { event ->
                    val clicker = event.whoClicked as? Player ?: return@withAction
                    clicker.closeInventory()
                    proceedWithQuantity(clicker, session, listing, quantity)
                }
            menu.addItem(presetItem)
            inventory.setItem(slot, displayItem)
        }

        val cancelMat = Material.matchMaterial(gui.quantitySelectorCancelMaterial) ?: Material.RED_STAINED_GLASS_PANE
        val cancelStack = ListingRenderer.renderNavButton(cancelMat, gui.quantitySelectorCancelLabel)
        val cancelItem = MenuItem("cancel-quantity")
            .item(cancelStack)
            .withSlot(22)
            .withSourceInventory(inventory)
            .withAction { event ->
                val clicker = event.whoClicked as? Player ?: return@withAction
                val activeSession = activeSessions[clicker.uniqueId] ?: return@withAction
                openSession(clicker, activeSession.catalog, activeSession.sectionIndex, activeSession.mode)
            }
        menu.addItem(cancelItem)
        inventory.setItem(22, cancelStack)

        val fillerMat = Material.matchMaterial(gui.quantitySelectorFillerMaterial) ?: Material.BLACK_STAINED_GLASS_PANE
        val fillerStack = ItemStack(fillerMat).also {
            it.editMeta { meta -> meta.displayName(Component.text(" ")) }
        }
        for (i in 0 until 27) {
            if (inventory.getItem(i) == null) {
                val filler = MenuItem("filler-$i")
                    .item(fillerStack.clone())
                    .withSlot(i)
                    .withSourceInventory(inventory)
                menu.addItem(filler)
                inventory.setItem(i, fillerStack.clone())
            }
        }

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            player.openInventory(inventory)
        }, 1L)
    }

    private fun executeTransaction(player: Player, session: BrowsingSession, listing: Listing, quantity: Int) {
        when (listing.type) {
            ListingType.STATIC -> return
            ListingType.TRADABLE -> handleTradableListing(player, session, listing, quantity)
            ListingType.DIRECTIVE -> handleDirectiveListing(player, session, listing)
            ListingType.BARTER -> handleBarterListing(player, session, listing)
        }
    }

    private fun handleListingClick(player: Player, session: BrowsingSession, listing: Listing, isShiftClick: Boolean = false) {
        val locale = plugin.localeManager

        if (listing.authorization != null && !player.hasPermission(listing.authorization!!)) {
            locale.dispatch(player, "operations.restricted", "%action%" to session.mode.displayName)
            return
        }

        if (listing.quotaLimit > 0) {
            val currentUsage = plugin.quotaLedger.getUsage(player.uniqueId, listing.uid)
            if (currentUsage >= listing.quotaLimit) {
                val remaining = plugin.quotaEnforcer.getTimeRemaining(listing.uid)
                locale.dispatch(
                    player, "operations.quota-exhausted",
                    "%remaining%" to formatDuration(remaining)
                )
                return
            }
        }

        if (listing.type == ListingType.STATIC) return

        if (isShiftClick && plugin.guiConfigManager.quantitySelectorEnabled &&
            listing.type == ListingType.TRADABLE && session.mode != TransactionMode.BARTER) {
            openQuantitySelectorGui(player, session, listing)
            return
        }

        proceedWithQuantity(player, session, listing, listing.stackQuantity)
    }

    private fun proceedWithQuantity(player: Player, session: BrowsingSession, listing: Listing, quantity: Int) {
        if (plugin.guiConfigManager.confirmationEnabled) {
            openConfirmationGui(player, session, listing, quantity)
        } else {
            executeTransaction(player, session, listing, quantity)
        }
    }

    private fun handleTradableListing(player: Player, session: BrowsingSession, listing: Listing, quantity: Int) {
        val baseItem = listing.itemStack?.clone()?.apply { amount = 1 }
            ?: ItemStack(Material.matchMaterial(listing.materialId) ?: return, 1)

        when (session.mode) {
            TransactionMode.ACQUIRE -> {
                val dynMultiplier = plugin.pricingModule.multiplierFor(listing.uid)
                val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0
                val unitCost = TransactionProcessor.computeCost(
                    listing.acquireCost, listing.stackQuantity, 1,
                    listing.reductionActive, listing.reductionOverrideCost,
                    dynMultiplier, catalogDiscount
                )
                val result = TransactionProcessor.executeAcquisition(
                    player, baseItem, quantity, unitCost, plugin
                )
                handleResult(player, result, listing, session.mode)
            }

            TransactionMode.LIQUIDATE -> {
                val dynMultiplier = plugin.pricingModule.multiplierFor(listing.uid)
                val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0
                val unitReward = TransactionProcessor.computeCost(
                    listing.liquidateReward, listing.stackQuantity, 1,
                    dynamicMultiplier = dynMultiplier,
                    catalogDiscountPercent = catalogDiscount
                )
                val result = TransactionProcessor.executeLiquidation(
                    player, baseItem, quantity, unitReward, plugin,
                    partialAllowed = listing.liquidatePartialAllowed
                )
                handleResult(player, result, listing, session.mode)
            }

            TransactionMode.BARTER -> handleBarterListing(player, session, listing)
        }
    }

    private fun handleDirectiveListing(player: Player, session: BrowsingSession, listing: Listing) {
        val locale = plugin.localeManager

        if (listing.directiveRunMode != DirectiveRunMode.EXECUTE_ONLY) {
            val dynMultiplier = plugin.pricingModule.multiplierFor(listing.uid)
            val catalogDiscount = if (session.catalog.reductionActive) session.catalog.reductionPercent else 0.0
            val cost = TransactionProcessor.computeCost(
                listing.acquireCost, listing.stackQuantity, listing.stackQuantity,
                listing.reductionActive, listing.reductionOverrideCost,
                dynMultiplier, catalogDiscount
            )
            if (!plugin.currencyBridge.hasBalance(player, cost)) {
                val deficit = plugin.currencyBridge.deficit(player, cost)
                locale.dispatch(
                    player, "operations.insufficient-balance",
                    "%deficit%" to locale.formatCurrency(deficit)
                )
                return
            }
            
            if (!plugin.currencyBridge.withdraw(player, cost)) {
                locale.dispatch(
                    player, "operations.insufficient-balance",
                    "%deficit%" to locale.formatCurrency(cost)
                )
                return
            }

            if (listing.directiveRunMode == DirectiveRunMode.ACQUIRE_AND_RETAIN) {
                val itemTemplate = listing.itemStack?.clone()?.apply { amount = 1 }
                    ?: Material.matchMaterial(listing.materialId)?.let { ItemStack(it) }

                if (itemTemplate == null) {
                    plugin.currencyBridge.deposit(player, cost)
                    return
                }

                val clone = itemTemplate.clone().apply { amount = listing.stackQuantity }
                val overflow = player.inventory.addItem(clone)
                if (overflow.isNotEmpty()) {
                    val overflowCount = overflow.values.sumOf { it.amount }
                    val addedCount = listing.stackQuantity - overflowCount
                    if (addedCount > 0) {
                        val undo = itemTemplate.clone().apply { amount = addedCount }
                        player.inventory.removeItem(undo)
                    }
                    plugin.currencyBridge.deposit(player, cost)
                    locale.dispatch(player, "operations.inventory-full")
                    return
                }
            }
        }

        for (directive in listing.directives) {
            val resolvedCommand = directive.command.replace("%player%", player.name)
            when (directive.executor) {
                DirectiveExecutor.CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand)
                DirectiveExecutor.PLAYER -> player.performCommand(resolvedCommand)
            }
        }

        if (listing.quotaLimit > 0) {
            plugin.quotaLedger.recordUsage(player.uniqueId, listing.uid)
        }
    }

    private fun handleBarterListing(player: Player, session: BrowsingSession, listing: Listing) {
        val locale = plugin.localeManager

        for (requirement in listing.barterRequirements) {
            val reqMaterial = Material.matchMaterial(requirement.materialId) ?: continue
            val available = TransactionProcessor.countMatchingItems(
                player.inventory.storageContents,
                ItemStack(reqMaterial)
            )
            if (available < requirement.quantity) {
                locale.dispatch(player, "operations.insufficient-materials", "%action%" to "barter")
                return
            }
        }

        for (requirement in listing.barterRequirements) {
            val reqMaterial = Material.matchMaterial(requirement.materialId) ?: continue
            val target = ItemStack(reqMaterial).apply { amount = requirement.quantity }
            player.inventory.removeItem(target)
        }

        for (receivable in listing.barterReceivables) {
            val recvMaterial = Material.matchMaterial(receivable.materialId) ?: continue
            val overflow = player.inventory.addItem(ItemStack(recvMaterial, receivable.quantity))
            if (overflow.isNotEmpty() && listing.dropOnOverflow) {
                overflow.values.forEach { leftover ->
                    player.world.dropItemNaturally(player.location, leftover)
                }
            }
        }

        if (listing.quotaLimit > 0) {
            plugin.quotaLedger.recordUsage(player.uniqueId, listing.uid)
        }

        locale.dispatch(player, "operations.barter-confirmed")
        
        if (listing.broadcastOnTransaction && listing.broadcastTemplate.isNotBlank()) {
            val listingLabel = listing.label ?: listing.materialId
            val msg = listing.broadcastTemplate
                .replace("%player%", player.name)
                .replace("%listing%", listingLabel)
                .replace("%amount%", "Barter items")
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg))
        }
    }

    private fun handleResult(player: Player, result: TransactionResult, listing: Listing, mode: TransactionMode) {
        val locale = plugin.localeManager
        val listingLabel = listing.label ?: listing.materialId

        when (result) {
            is TransactionResult.Success -> {
                if (listing.quotaLimit > 0) {
                    plugin.quotaLedger.recordUsage(player.uniqueId, listing.uid)
                }

                if (plugin.pricingModule.isEnabled) {
                    when (mode) {
                        TransactionMode.ACQUIRE -> plugin.pricingModule.recordBuy(listing.uid)
                        TransactionMode.LIQUIDATE -> plugin.pricingModule.recordSell(listing.uid)
                        TransactionMode.BARTER -> {}
                    }
                }

                val msgKey = when (mode) {
                    TransactionMode.ACQUIRE -> "operations.acquisition-confirmed"
                    TransactionMode.LIQUIDATE -> "operations.liquidation-confirmed"
                    TransactionMode.BARTER -> "operations.barter-confirmed"
                }
                locale.dispatch(
                    player, msgKey,
                    "%listing%" to listingLabel,
                    "%amount%" to locale.formatCurrency(result.amount)
                )

                if (listing.broadcastOnTransaction && listing.broadcastTemplate.isNotBlank()) {
                    val msg = listing.broadcastTemplate
                        .replace("%player%", player.name)
                        .replace("%listing%", listingLabel)
                        .replace("%amount%", locale.formatCurrency(result.amount))
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg))
                }
            }

            is TransactionResult.InsufficientFunds -> {
                locale.dispatch(
                    player, "operations.insufficient-balance",
                    "%deficit%" to locale.formatCurrency(result.deficit)
                )
            }

            is TransactionResult.InsufficientMaterials -> {
                locale.dispatch(
                    player, "operations.insufficient-materials",
                    "%action%" to mode.displayName
                )
            }

            is TransactionResult.InventoryFull -> {
                locale.dispatch(player, "operations.inventory-full")
            }

            is TransactionResult.LiquidationFailed -> {
                locale.dispatch(
                    player, "operations.liquidation-failed",
                    "%reason%" to result.reason
                )
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "-"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (secs > 0) append("${secs}s")
        }.trim()
    }
}
