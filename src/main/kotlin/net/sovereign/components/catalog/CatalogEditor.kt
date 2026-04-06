package net.sovereign.components.catalog

import net.bitbylogic.menus.Menu
import net.bitbylogic.menus.MenuFlag
import net.bitbylogic.menus.data.MenuData
import net.bitbylogic.menus.item.MenuItem
import net.sovereign.core.SovereignCore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

class CatalogEditor(private val plugin: SovereignCore) : Listener {

    private val editorSessions = mutableMapOf<UUID, EditorSession>()
    private val chatInputSessions = mutableMapOf<UUID, ChatInputRequest>()
    private val bulkQuotaSessions = mutableMapOf<UUID, BulkQuotaRequest>()
    private val reopeningEditors = mutableSetOf<UUID>()
    private val mm = MiniMessage.miniMessage()

    data class EditorSession(
        val playerId: UUID,
        val catalog: Catalog,
        var sectionIndex: Int,
        var mode: TransactionMode
    )

    data class ChatInputRequest(
        val playerId: UUID,
        val catalog: Catalog,
        val sectionIndex: Int,
        val mode: TransactionMode,
        val slot: Int,
        val listing: Listing,
        val phase: InputPhase
    )

    data class BulkQuotaRequest(
        val playerId: UUID,
        val catalog: Catalog,
        val sectionIndex: Int,
        val mode: TransactionMode,
        var phase: InputPhase,
        var pendingLimit: Int = 0
    )

    enum class InputPhase {
        ACQUIRE_COST,
        LIQUIDATE_REWARD,
        QUOTA_LIMIT,
        QUOTA_RESET_INTERVAL
    }

    fun openEditor(player: Player, catalog: Catalog, sectionIndex: Int = 0, mode: TransactionMode = TransactionMode.ACQUIRE) {
        if (catalog.sections.isEmpty()) {
            msg(player, "<yellow>ᴛʜɪs ᴄᴀᴛᴀʟᴏɢ ʜᴀs ɴᴏ sᴇᴄᴛɪᴏɴs.")
            return
        }

        val clampedIndex = sectionIndex.coerceIn(0, catalog.sections.size - 1)

        reopeningEditors.add(player.uniqueId)
        val session = EditorSession(player.uniqueId, catalog, clampedIndex, mode)
        editorSessions[player.uniqueId] = session

        val section = catalog.sections[clampedIndex]
        val (menu, inventory) = buildEditorMenu(session, section)

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            player.openInventory(inventory)
        }, 1L)
        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            reopeningEditors.remove(player.uniqueId)
        }, 5L)
    }

    private fun buildEditorMenu(session: EditorSession, section: CatalogSection): Pair<Menu, org.bukkit.inventory.Inventory> {
        val totalSlots = section.capacity + 9
        val gui = plugin.guiConfigManager

        val titleTemplate = gui.editorTitle
            .replace("%catalog%", "<catalog>")
            .replace("%mode%", "<mode>")

        val title = mm.deserialize(
            titleTemplate,
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("catalog", mm.deserialize(session.catalog.displayTitle)),
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("mode", session.mode.displayName)
        )

        val menuData = MenuData()
            .withFlag(MenuFlag.DISABLE_TITLE_UPDATE)
            .withFlag(MenuFlag.DISABLE_UPDATES)
            .withFlag(MenuFlag.LOWER_INTERACTION)
            .withMaxInventories(0)
            .withCloseAction { event ->
                val player = event.player as? Player ?: return@withCloseAction
                if (reopeningEditors.contains(player.uniqueId)) return@withCloseAction

                if (chatInputSessions.containsKey(player.uniqueId)) return@withCloseAction
                editorSessions.remove(player.uniqueId)
            }

        val menu = Menu("editor-${session.catalog.identifier}", "editor", totalSlots, menuData)
        val inventory = Bukkit.createInventory(menu, totalSlots, title)

        val listings = section.listingsForMode(session.mode)

        for (slot in 0 until section.capacity) {
            val listing = listings[slot]

            if (listing != null) {

                val displayItem = renderEditorItem(listing, session.mode)
                val menuItem = MenuItem("editor-$slot")
                    .item(displayItem)
                    .withSlot(slot)
                    .withSourceInventory(inventory)
                    .withAction { event ->
                        val player = event.whoClicked as? Player ?: return@withAction
                        val s = editorSessions[player.uniqueId] ?: return@withAction

                        if (event.isRightClick) {

                            listings[slot] = null
                            plugin.catalogRepository.persistAll()
                            msg(player, "<red>ʀᴇᴍᴏᴠᴇᴅ ʟɪsᴛɪɴɢ ꜰʀᴏᴍ sʟᴏᴛ $slot.")
                            openEditor(player, s.catalog, s.sectionIndex, s.mode)
                        } else if (event.isShiftClick) {

                            startQuotaEdit(player, s, slot, listing)
                        } else {

                            startPriceEdit(player, s, slot, listing)
                        }
                    }
                menu.addItem(menuItem)
                inventory.setItem(slot, displayItem)
            } else {

                val placeholder = buildPlaceholder()
                val menuItem = MenuItem("empty-$slot")
                    .item(placeholder)
                    .withSlot(slot)
                    .withSourceInventory(inventory)
                    .withAction { event ->
                        val player = event.whoClicked as? Player ?: return@withAction
                        val s = editorSessions[player.uniqueId] ?: return@withAction

                        val cursorItem = event.cursor
                        if (cursorItem.type == Material.AIR) {
                            msg(player, "<yellow>ᴅʀᴀɢ ᴀɴ ɪᴛᴇᴍ ꜰʀᴏᴍ ʏᴏᴜʀ ɪɴᴠᴇɴᴛᴏʀʏ ᴛᴏ ᴛʜɪs sʟᴏᴛ!")
                            return@withAction
                        }
                        addFromCursor(player, s, slot, cursorItem.clone())
                    }
                menu.addItem(menuItem)
                inventory.setItem(slot, placeholder)
            }
        }

        val controlStart = section.capacity

        val toggleMat = when (session.mode) {
            TransactionMode.ACQUIRE -> Material.matchMaterial(gui.acquireMaterial) ?: Material.GOLDEN_APPLE
            TransactionMode.LIQUIDATE -> Material.matchMaterial(gui.liquidateMaterial) ?: Material.STONE
            TransactionMode.BARTER -> Material.matchMaterial(gui.barterMaterial) ?: Material.REDSTONE_BLOCK
        }
        val modeNameRaw = gui.editorModeName.replace("%mode%", session.mode.displayName)
        val toggleStack = buildControlItem(toggleMat, modeNameRaw, gui.editorModeLore)
        val toggleItem = MenuItem("editor-mode")
            .item(toggleStack)
            .withSlot(controlStart + 0)
            .withSourceInventory(inventory)
            .withAction { event ->
                val player = event.whoClicked as? Player ?: return@withAction
                val s = editorSessions[player.uniqueId] ?: return@withAction
                s.mode = s.catalog.cycleMode(s.mode)
                openEditor(player, s.catalog, s.sectionIndex, s.mode)
            }
        menu.addItem(toggleItem)
        inventory.setItem(controlStart + 0, toggleStack)

        val quotaMat = Material.matchMaterial(gui.editorQuotaMaterial) ?: Material.CLOCK
        val quotaStack = buildControlItem(quotaMat, gui.editorQuotaName, gui.editorQuotaLore)
        val quotaItem = MenuItem("editor-quota-bulk")
            .item(quotaStack)
            .withSlot(controlStart + 2)
            .withSourceInventory(inventory)
            .withAction { event ->
                val player = event.whoClicked as? Player ?: return@withAction
                val s = editorSessions[player.uniqueId] ?: return@withAction
                startBulkQuotaEdit(player, s)
            }
        menu.addItem(quotaItem)
        inventory.setItem(controlStart + 2, quotaStack)

        val saveMat = Material.matchMaterial(gui.editorSaveMaterial) ?: Material.EMERALD
        val saveStack = buildControlItem(saveMat, gui.editorSaveName, gui.editorSaveLore)
        val saveItem = MenuItem("editor-save")
            .item(saveStack)
            .withSlot(controlStart + 4)
            .withSourceInventory(inventory)
            .withAction { event ->
                val player = event.whoClicked as? Player ?: return@withAction
                plugin.catalogRepository.persistAll()
                editorSessions.remove(player.uniqueId)
                player.closeInventory()
                msg(player, "<green>ᴄᴀᴛᴀʟᴏɢ sᴀᴠᴇᴅ.")
            }
        menu.addItem(saveItem)
        inventory.setItem(controlStart + 4, saveStack)

        val fillerMat = Material.matchMaterial(gui.editorFillerMaterial) ?: Material.GRAY_STAINED_GLASS_PANE
        val filler = ItemStack(fillerMat).also {
            it.editMeta { meta -> meta.displayName(Component.text(" ")) }
        }
        for (i in controlStart until totalSlots) {
            if (inventory.getItem(i) == null) {
                val fillerItem = MenuItem("ctrl-filler-$i")
                    .item(filler.clone())
                    .withSlot(i)
                    .withSourceInventory(inventory)
                menu.addItem(fillerItem)
                inventory.setItem(i, filler.clone())
            }
        }

        return Pair(menu, inventory)
    }

    private fun addFromCursor(player: Player, session: EditorSession, slot: Int, cursorItem: ItemStack) {
        val section = session.catalog.sections[session.sectionIndex]
        val listings = section.listingsForMode(session.mode)

        val uid = generateNextUid(session.catalog)
        val itemDisplayName = resolveItemName(cursorItem)

        val listing = Listing(
            uid = uid,
            type = ListingType.TRADABLE,
            materialId = cursorItem.type.name,
            stackQuantity = cursorItem.amount,
            itemStack = cursorItem.clone(),
            label = if (cursorItem.hasItemMeta() && cursorItem.itemMeta.hasDisplayName()) {
                mm.serialize(cursorItem.itemMeta.displayName()!!)
            } else null,
            showCost = true,
            dropOnOverflow = true
        )

        listings[slot] = listing
        plugin.catalogRepository.persistAll()

        msg(player, "<green>ᴀᴅᴅᴇᴅ <white>$itemDisplayName <green>ᴛᴏ sʟᴏᴛ <white>$slot<green>.")

        when (session.mode) {
            TransactionMode.ACQUIRE -> {

                startSmartPriceEdit(player, session, slot, listing, InputPhase.ACQUIRE_COST, itemDisplayName)
            }
            TransactionMode.LIQUIDATE -> {

                startSmartPriceEdit(player, session, slot, listing, InputPhase.LIQUIDATE_REWARD, itemDisplayName)
            }
            TransactionMode.BARTER -> {

                msg(player, "<gray>ʙᴀʀᴛᴇʀ ɪᴛᴇᴍs ᴜsᴇ ᴍᴀᴛᴇʀɪᴀʟ ʀᴇǫᴜɪʀᴇᴍᴇɴᴛs — ɴᴏ ᴘʀɪᴄᴇ ɴᴇᴇᴅᴇᴅ.")
                openEditor(player, session.catalog, session.sectionIndex, session.mode)
            }
        }
    }

    private fun startPriceEdit(player: Player, session: EditorSession, slot: Int, listing: Listing) {
        val itemDisplayName = resolveItemName(listing.itemStack ?: ItemStack(Material.matchMaterial(listing.materialId) ?: Material.STONE))
        val phase = when (session.mode) {
            TransactionMode.ACQUIRE -> InputPhase.ACQUIRE_COST
            TransactionMode.LIQUIDATE -> InputPhase.LIQUIDATE_REWARD
            TransactionMode.BARTER -> {
                msg(player, "<gray>ʙᴀʀᴛᴇʀ ɪᴛᴇᴍs ᴜsᴇ ᴍᴀᴛᴇʀɪᴀʟ ʀᴇǫᴜɪʀᴇᴍᴇɴᴛs — ɴᴏ ᴘʀɪᴄᴇ ᴛᴏ ᴇᴅɪᴛ.")
                return
            }
        }
        startSmartPriceEdit(player, session, slot, listing, phase, itemDisplayName)
    }

    private fun startQuotaEdit(player: Player, session: EditorSession, slot: Int, listing: Listing) {
        val itemDisplayName = resolveItemName(listing.itemStack ?: ItemStack(Material.matchMaterial(listing.materialId) ?: Material.STONE))
        startSmartPriceEdit(player, session, slot, listing, InputPhase.QUOTA_LIMIT, itemDisplayName)
    }

    private fun startBulkQuotaEdit(player: Player, session: EditorSession) {
        val section = session.catalog.sections[session.sectionIndex]
        val listings = section.listingsForMode(session.mode)
        val firstListing = listings.firstOrNull { it != null }
        if (firstListing == null) {
            msg(player, "<yellow>ɴᴏ ʟɪsᴛɪɴɢs ɪɴ ᴛʜɪs ᴍᴏᴅᴇ ᴛᴏ ᴄᴏɴꜰɪɢᴜʀᴇ.")
            return
        }
        bulkQuotaSessions[player.uniqueId] = BulkQuotaRequest(
            playerId = player.uniqueId,
            catalog = session.catalog,
            sectionIndex = session.sectionIndex,
            mode = session.mode,
            phase = InputPhase.QUOTA_LIMIT
        )
        player.closeInventory()
        msg(player, "<aqua><bold>ʙᴜʟᴋ ǫᴜᴏᴛᴀ</bold> <gray>» <white>sᴇᴛ ǫᴜᴏᴛᴀ ꜰᴏʀ ᴀʟʟ ʟɪsᴛɪɴɢs ɪɴ <yellow>${session.mode.displayName} <white>ᴍᴏᴅᴇ")
        msg(player, "<yellow>ᴛʏᴘᴇ ᴛʜᴇ <white>ǫᴜᴏᴛᴀ ʟɪᴍɪᴛ <gray>(ᴍᴀx ᴛʀᴀɴsᴀᴄᴛɪᴏɴs, ᴏʀ <white>0 <gray>ᴛᴏ ᴅɪsᴀʙʟᴇ, ᴏʀ <red>ᴄᴀɴᴄᴇʟ<gray>):")
    }

    private fun startSmartPriceEdit(
        player: Player,
        session: EditorSession,
        slot: Int,
        listing: Listing,
        phase: InputPhase,
        itemDisplayName: String
    ) {
        chatInputSessions[player.uniqueId] = ChatInputRequest(
            playerId = player.uniqueId,
            catalog = session.catalog,
            sectionIndex = session.sectionIndex,
            mode = session.mode,
            slot = slot,
            listing = listing,
            phase = phase
        )

        player.closeInventory()
        when (phase) {
            InputPhase.ACQUIRE_COST -> {
                msg(player, "<aqua><bold>ᴘʀɪᴄᴇ ᴇᴅɪᴛᴏʀ</bold> <gray>» <white>$itemDisplayName <gray>(sʟᴏᴛ $slot)")
                msg(player, "<yellow>ᴛʏᴘᴇ ᴛʜᴇ <white>ʙᴜʏ ᴘʀɪᴄᴇ <yellow>ꜰᴏʀ <white>$itemDisplayName <gray>(ᴏʀ <red>ᴄᴀɴᴄᴇʟ<gray>):")
            }
            InputPhase.LIQUIDATE_REWARD -> {
                msg(player, "<aqua><bold>ᴘʀɪᴄᴇ ᴇᴅɪᴛᴏʀ</bold> <gray>» <white>$itemDisplayName <gray>(sʟᴏᴛ $slot)")
                msg(player, "<yellow>ᴛʏᴘᴇ ᴛʜᴇ <white>sᴇʟʟ ᴘʀɪᴄᴇ <yellow>ꜰᴏʀ <white>$itemDisplayName <gray>(ᴏʀ <red>ᴄᴀɴᴄᴇʟ<gray>):")
            }
            InputPhase.QUOTA_LIMIT -> {
                msg(player, "<aqua><bold>ǫᴜᴏᴛᴀ ᴇᴅɪᴛᴏʀ</bold> <gray>» <white>$itemDisplayName <gray>(sʟᴏᴛ $slot)")
                msg(player, "<yellow>ᴛʏᴘᴇ ᴛʜᴇ <white>ǫᴜᴏᴛᴀ ʟɪᴍɪᴛ <gray>(ᴍᴀx ᴛʀᴀɴsᴀᴄᴛɪᴏɴs, ᴏʀ <white>0 <gray>ᴛᴏ ᴅɪsᴀʙʟᴇ, ᴏʀ <red>ᴄᴀɴᴄᴇʟ<gray>):")
            }
            InputPhase.QUOTA_RESET_INTERVAL -> {
                msg(player, "<aqua><bold>ǫᴜᴏᴛᴀ ᴇᴅɪᴛᴏʀ</bold> <gray>» <white>$itemDisplayName <gray>(sʟᴏᴛ $slot)")
                msg(player, "<yellow>ᴛʏᴘᴇ ᴛʜᴇ <white>ʀᴇsᴇᴛ ɪɴᴛᴇʀᴠᴀʟ <yellow>ɪɴ sᴇᴄᴏɴᴅs <gray>(ᴏʀ <white>0 <gray>ꜰᴏʀ ɴᴏ ʀᴇsᴇᴛ, ᴏʀ <red>ᴄᴀɴᴄᴇʟ<gray>):")
            }
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val bulkRequest = bulkQuotaSessions[event.player.uniqueId]
        if (bulkRequest != null) {
            event.isCancelled = true
            handleBulkQuotaChat(event.player, event.message.trim(), bulkRequest)
            return
        }

        val request = chatInputSessions[event.player.uniqueId] ?: return
        event.isCancelled = true

        val input = event.message.trim()
        val player = event.player

        if (input.equals("cancel", ignoreCase = true)) {
            chatInputSessions.remove(player.uniqueId)
            net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                msg(player, "<red>ᴇᴅɪᴛɪɴɢ ᴄᴀɴᴄᴇʟʟᴇᴅ.")
                openEditor(player, request.catalog, request.sectionIndex, request.mode)
            })
            return
        }

        when (request.phase) {
            InputPhase.ACQUIRE_COST, InputPhase.LIQUIDATE_REWARD -> {
                val value = input.toDoubleOrNull()
                if (value == null || value < 0) {
                    msg(player, "<red>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴛʀʏ ᴀɢᴀɪɴ ᴏʀ ᴛʏᴘᴇ <yellow>ᴄᴀɴᴄᴇʟ<red>:")
                    return
                }
                if (request.phase == InputPhase.ACQUIRE_COST) {
                    request.listing.acquireCost = value
                } else {
                    request.listing.liquidateReward = value
                }
                chatInputSessions.remove(player.uniqueId)
                net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                    plugin.catalogRepository.persistAll()
                    val label = if (request.phase == InputPhase.ACQUIRE_COST) "ʙᴜʏ ᴘʀɪᴄᴇ" else "sᴇʟʟ ᴘʀɪᴄᴇ"
                    msg(player, "<green>$label sᴇᴛ ᴛᴏ <white>$value<green>. ʟɪsᴛɪɴɢ sᴀᴠᴇᴅ!")
                    openEditor(player, request.catalog, request.sectionIndex, request.mode)
                })
            }

            InputPhase.QUOTA_LIMIT -> {
                val value = input.toIntOrNull()
                if (value == null || value < 0) {
                    msg(player, "<red>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ ᴀ ᴡʜᴏʟᴇ ɴᴜᴍʙᴇʀ ≥ 0 ᴏʀ ᴛʏᴘᴇ <yellow>ᴄᴀɴᴄᴇʟ<red>:")
                    return
                }
                request.listing.quotaLimit = value
                if (value > 0) {
                    request.listing.showQuotaProgress = true
                } else {
                    request.listing.showQuotaProgress = false
                    request.listing.showQuotaTimer = false
                    request.listing.quotaResetIntervalSec = 0
                }
                chatInputSessions.remove(player.uniqueId)

                if (value > 0) {
                    val itemDisplayName = resolveItemName(request.listing.itemStack ?: ItemStack(Material.matchMaterial(request.listing.materialId) ?: Material.STONE))
                    net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                        msg(player, "<green>ǫᴜᴏᴛᴀ ʟɪᴍɪᴛ sᴇᴛ ᴛᴏ <white>$value<green>.")
                        val session = EditorSession(player.uniqueId, request.catalog, request.sectionIndex, request.mode)
                        startSmartPriceEdit(player, session, request.slot, request.listing, InputPhase.QUOTA_RESET_INTERVAL, itemDisplayName)
                    })
                } else {
                    net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                        plugin.catalogRepository.persistAll()
                        plugin.quotaEnforcer.unregisterInterval(request.listing.uid)
                        msg(player, "<green>ǫᴜᴏᴛᴀ ᴅɪsᴀʙʟᴇᴅ ꜰᴏʀ ᴛʜɪs ʟɪsᴛɪɴɢ.")
                        openEditor(player, request.catalog, request.sectionIndex, request.mode)
                    })
                }
            }

            InputPhase.QUOTA_RESET_INTERVAL -> {
                val value = input.toIntOrNull()
                if (value == null || value < 0) {
                    msg(player, "<red>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ sᴇᴄᴏɴᴅs ≥ 0 ᴏʀ ᴛʏᴘᴇ <yellow>ᴄᴀɴᴄᴇʟ<red>:")
                    return
                }
                request.listing.quotaResetIntervalSec = value
                request.listing.showQuotaTimer = value > 0
                chatInputSessions.remove(player.uniqueId)

                net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                    plugin.catalogRepository.persistAll()
                    if (value > 0) {
                        plugin.quotaEnforcer.registerInterval(request.listing.uid, value)
                    } else {
                        plugin.quotaEnforcer.unregisterInterval(request.listing.uid)
                    }
                    val intervalLabel = if (value > 0) formatDuration(value) else "ɴᴏ ʀᴇsᴇᴛ"
                    msg(player, "<green>ʀᴇsᴇᴛ ɪɴᴛᴇʀᴠᴀʟ sᴇᴛ ᴛᴏ <white>$intervalLabel<green>. ǫᴜᴏᴛᴀ sᴀᴠᴇᴅ!")
                    openEditor(player, request.catalog, request.sectionIndex, request.mode)
                })
            }
        }
    }

    private fun handleBulkQuotaChat(player: Player, input: String, request: BulkQuotaRequest) {
        if (input.equals("cancel", ignoreCase = true)) {
            bulkQuotaSessions.remove(player.uniqueId)
            net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                msg(player, "<red>ʙᴜʟᴋ ǫᴜᴏᴛᴀ ᴄᴀɴᴄᴇʟʟᴇᴅ.")
                openEditor(player, request.catalog, request.sectionIndex, request.mode)
            })
            return
        }

        val value = input.toIntOrNull()
        if (value == null || value < 0) {
            msg(player, "<red>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ ᴀ ᴡʜᴏʟᴇ ɴᴜᴍʙᴇʀ ≥ 0 ᴏʀ ᴛʏᴘᴇ <yellow>ᴄᴀɴᴄᴇʟ<red>:")
            return
        }

        when (request.phase) {
            InputPhase.QUOTA_LIMIT -> {
                request.pendingLimit = value
                if (value > 0) {
                    request.phase = InputPhase.QUOTA_RESET_INTERVAL
                    msg(player, "<green>ʟɪᴍɪᴛ sᴇᴛ ᴛᴏ <white>$value<green>.")
                    msg(player, "<yellow>ɴᴏᴡ ᴛʏᴘᴇ ᴛʜᴇ <white>ʀᴇsᴇᴛ ɪɴᴛᴇʀᴠᴀʟ <yellow>ɪɴ sᴇᴄᴏɴᴅs <gray>(ᴏʀ <white>0 <gray>ꜰᴏʀ ɴᴏ ʀᴇsᴇᴛ, ᴏʀ <red>ᴄᴀɴᴄᴇʟ<gray>):")
                } else {
                    bulkQuotaSessions.remove(player.uniqueId)
                    net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                        applyBulkQuota(request.catalog, request.sectionIndex, request.mode, 0, 0)
                        plugin.catalogRepository.persistAll()
                        plugin.quotaEnforcer.syncFromCatalogs()
                        msg(player, "<green>ǫᴜᴏᴛᴀ ᴅɪsᴀʙʟᴇᴅ ꜰᴏʀ ᴀʟʟ ʟɪsᴛɪɴɢs ɪɴ <yellow>${request.mode.displayName}<green>.")
                        openEditor(player, request.catalog, request.sectionIndex, request.mode)
                    })
                }
            }

            InputPhase.QUOTA_RESET_INTERVAL -> {
                bulkQuotaSessions.remove(player.uniqueId)
                val limit = request.pendingLimit
                val interval = value
                net.sovereign.core.scheduler.SovereignScheduler.runTask(plugin.plugin, Runnable {
                    applyBulkQuota(request.catalog, request.sectionIndex, request.mode, limit, interval)
                    plugin.catalogRepository.persistAll()
                    plugin.quotaEnforcer.syncFromCatalogs()
                    val intervalLabel = if (interval > 0) formatDuration(interval) else "ɴᴏ ʀᴇsᴇᴛ"
                    msg(player, "<green>ᴀʟʟ ʟɪsᴛɪɴɢs ɪɴ <yellow>${request.mode.displayName} <green>→ ǫᴜᴏᴛᴀ <white>$limit<green>, ʀᴇsᴇᴛ <white>$intervalLabel<green>.")
                    openEditor(player, request.catalog, request.sectionIndex, request.mode)
                })
            }

            else -> {
                bulkQuotaSessions.remove(player.uniqueId)
            }
        }
    }

    private fun applyBulkQuota(catalog: Catalog, sectionIndex: Int, mode: TransactionMode, limit: Int, interval: Int) {
        val section = catalog.sections[sectionIndex]
        val listings = section.listingsForMode(mode)
        for (listing in listings) {
            if (listing != null) {
                listing.quotaLimit = limit
                listing.quotaResetIntervalSec = interval
                listing.showQuotaProgress = limit > 0
                listing.showQuotaTimer = limit > 0 && interval > 0
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        chatInputSessions.remove(playerId)
        editorSessions.remove(playerId)
        bulkQuotaSessions.remove(playerId)
    }

    fun addListingFromCommand(player: Player, catalogId: String, buyPrice: Double, sellPrice: Double) {
        val catalog = plugin.catalogRepository.resolve(catalogId)
        if (catalog == null) {
            msg(player, "<red>ᴄᴀᴛᴀʟᴏɢ '<yellow>$catalogId<red>' ɴᴏᴛ ꜰᴏᴜɴᴅ.")
            return
        }

        val handItem = player.inventory.itemInMainHand
        if (handItem.type == Material.AIR) {
            msg(player, "<yellow>ʜᴏʟᴅ ᴀɴ ɪᴛᴇᴍ ɪɴ ʏᴏᴜʀ ʜᴀɴᴅ!")
            return
        }

        if (catalog.sections.isEmpty()) {
            msg(player, "<red>ᴄᴀᴛᴀʟᴏɢ ʜᴀs ɴᴏ sᴇᴄᴛɪᴏɴs.")
            return
        }

        val section = catalog.sections[0]
        val label = if (handItem.hasItemMeta() && handItem.itemMeta.hasDisplayName()) {
            mm.serialize(handItem.itemMeta.displayName()!!)
        } else null
        val itemLabel = label ?: handItem.type.name.lowercase().replace('_', ' ')

        var addedAny = false

        if (buyPrice > 0) {
            val acquireListings = section.listingsForMode(TransactionMode.ACQUIRE)
            val nextSlot = acquireListings.indexOfFirst { it == null }
            if (nextSlot == -1) {
                msg(player, "<red>ɴᴏ ᴇᴍᴘᴛʏ ᴀᴄǫᴜɪʀᴇ sʟᴏᴛs ᴀᴠᴀɪʟᴀʙʟᴇ!")
            } else {
                val acquireListing = Listing(
                    uid = generateNextUid(catalog),
                    type = ListingType.TRADABLE,
                    materialId = handItem.type.name,
                    stackQuantity = handItem.amount,
                    itemStack = handItem.clone(),
                    label = label,
                    acquireCost = buyPrice,
                    liquidateReward = sellPrice,
                    showCost = true,
                    dropOnOverflow = true
                )
                acquireListings[nextSlot] = acquireListing
                msg(player, "<green>ᴀᴅᴅᴇᴅ <white>$itemLabel <green>ᴛᴏ <yellow>$catalogId <green>ᴀᴄǫᴜɪʀᴇ (sʟᴏᴛ $nextSlot)")
                msg(player, "<gray>  ʙᴜʏ: <white>$buyPrice")
                addedAny = true
            }
        }

        if (sellPrice > 0) {
            val liquidateListings = section.listingsForMode(TransactionMode.LIQUIDATE)
            val nextSlot = liquidateListings.indexOfFirst { it == null }
            if (nextSlot == -1) {
                msg(player, "<red>ɴᴏ ᴇᴍᴘᴛʏ ʟɪǫᴜɪᴅᴀᴛᴇ sʟᴏᴛs ᴀᴠᴀɪʟᴀʙʟᴇ!")
            } else {
                val liquidateListing = Listing(
                    uid = generateNextUid(catalog),
                    type = ListingType.TRADABLE,
                    materialId = handItem.type.name,
                    stackQuantity = handItem.amount,
                    itemStack = handItem.clone(),
                    label = label,
                    acquireCost = buyPrice,
                    liquidateReward = sellPrice,
                    showCost = true,
                    dropOnOverflow = true
                )
                liquidateListings[nextSlot] = liquidateListing
                msg(player, "<green>ᴀᴅᴅᴇᴅ <white>$itemLabel <green>ᴛᴏ <yellow>$catalogId <green>ʟɪǫᴜɪᴅᴀᴛᴇ (sʟᴏᴛ $nextSlot)")
                msg(player, "<gray>  sᴇʟʟ: <white>$sellPrice")
                addedAny = true
            }
        }

        if (!addedAny) {
            msg(player, "<red>ɴᴏᴛʜɪɴɢ ᴀᴅᴅᴇᴅ — ʙᴏᴛʜ ᴘʀɪᴄᴇs ᴀʀᴇ ᴢᴇʀᴏ ᴏʀ ɴᴏ sʟᴏᴛs ᴀᴠᴀɪʟᴀʙʟᴇ.")
            return
        }

        plugin.catalogRepository.persistAll()
    }

    private fun generateNextUid(catalog: Catalog): Int {

        return plugin.catalogRepository.generateNextGlobalUid()
    }

    private fun renderEditorItem(listing: Listing, mode: TransactionMode): ItemStack {
        val item = if (listing.itemStack != null) {
            listing.itemStack!!.clone().apply { amount = listing.stackQuantity }
        } else {
            val material = Material.matchMaterial(listing.materialId) ?: Material.STONE
            ItemStack(material, listing.stackQuantity)
        }
        val gui = plugin.guiConfigManager

        item.editMeta { meta ->
            if (listing.label != null) {
                meta.displayName(mm.deserialize(listing.label!!).decoration(TextDecoration.ITALIC, false))
            } else if (listing.itemStack == null && !meta.hasDisplayName()) {
                val name = item.type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false))
            }

            val existingLore = meta.lore() ?: emptyList()
            val quotaLabel = if (listing.quotaLimit > 0) listing.quotaLimit.toString() else "ᴏꜰꜰ"
            val resetLabel = if (listing.quotaResetIntervalSec > 0) formatDuration(listing.quotaResetIntervalSec) else "ɴᴏɴᴇ"
            val newLore = gui.editorListingLore.map { line ->
                val resolved = line
                    .replace("%uid%", listing.uid.toString())
                    .replace("%buy%", listing.acquireCost.toString())
                    .replace("%sell%", listing.liquidateReward.toString())
                    .replace("%quota%", quotaLabel)
                    .replace("%reset%", resetLabel)
                if (resolved.isEmpty()) net.kyori.adventure.text.Component.empty()
                else mm.deserialize(resolved).decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(existingLore + newLore)
        }
        return item
    }

    private fun buildPlaceholder(): ItemStack {
        val gui = plugin.guiConfigManager
        val mat = Material.matchMaterial(gui.editorPlaceholderMaterial) ?: Material.LIME_STAINED_GLASS_PANE
        val item = ItemStack(mat)
        item.editMeta { meta ->
            meta.displayName(
                mm.deserialize(gui.editorPlaceholderName)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(gui.editorPlaceholderLore.map {
                mm.deserialize(it).decoration(TextDecoration.ITALIC, false)
            })
        }
        return item
    }

    private fun buildControlItem(material: Material, name: String, vararg lore: String): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false))
            meta.lore(lore.map { mm.deserialize(it).decoration(TextDecoration.ITALIC, false) })
        }
        return item
    }

    private fun resolveItemName(item: ItemStack): String {
        if (item.hasItemMeta() && item.itemMeta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(item.itemMeta.displayName()!!)
        }
        return item.type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 || isEmpty()) append("${seconds}s")
        }.trim()
    }

    private fun msg(player: Player, text: String) {
        val prefix = plugin.localeManager.raw("general.tag")
        player.sendMessage(
            mm.deserialize("$prefix$text")
        )
    }
}
