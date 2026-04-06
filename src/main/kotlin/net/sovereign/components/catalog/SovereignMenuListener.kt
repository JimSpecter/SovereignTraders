package net.sovereign.components.catalog

import net.bitbylogic.menus.Menu
import net.bitbylogic.menus.MenuFlag
import net.bitbylogic.menus.listener.MenuListener
import net.sovereign.core.SovereignCore
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import java.util.concurrent.ConcurrentHashMap

class SovereignMenuListener(private val plugin: SovereignCore) : MenuListener(plugin.plugin) {

    private val clickCooldowns = ConcurrentHashMap<String, Long>()

    private fun checkAndSetCooldown(key: String, millis: Long): Boolean {
        val now = System.currentTimeMillis()

        if (Math.random() < 0.05) {
            clickCooldowns.entries.removeIf { it.value < now }
        }

        val expiry = clickCooldowns[key] ?: 0L
        if (now < expiry) return true

        clickCooldowns[key] = now + millis
        return false
    }

    @EventHandler
    override fun onMenuClick(event: InventoryClickEvent) {
        val topInventory = net.bitbylogic.utils.inventory.InventoryUtil.getViewInventory(event, "getTopInventory") ?: return
        val bottomInventory = net.bitbylogic.utils.inventory.InventoryUtil.getViewInventory(event, "getBottomInventory")

        val player = event.whoClicked as? Player ?: return

        if (topInventory.holder !is Menu) {
            return
        }

        val menu = topInventory.holder as Menu

        if (event.click == org.bukkit.event.inventory.ClickType.NUMBER_KEY && event.clickedInventory === topInventory) {
            event.isCancelled = true
            return
        }

        if (event.isShiftClick && event.clickedInventory !== topInventory) {
            event.isCancelled = !menu.data.hasFlag(MenuFlag.ALLOW_INPUT)
            return
        }

        if (event.clickedInventory === bottomInventory) {
            menu.data.externalClickAction?.let { action ->
                val cdKey = "${menu.id}exc-${event.slot}-${player.uniqueId}"
                if (checkAndSetCooldown(cdKey, 200L)) {
                    event.isCancelled = !menu.data.hasFlag(MenuFlag.LOWER_INTERACTION)
                    return
                }
                action.onClick(event)
            }

            event.isCancelled = !menu.data.hasFlag(MenuFlag.LOWER_INTERACTION)
            return
        }

        if (event.clickedInventory !== topInventory) {
            return
        }

        menu.data.clickAction?.onClick(event)

        val itemInMenu = menu.getItem(topInventory, event.slot)
        if (itemInMenu.isEmpty &&
            (event.cursor == null || event.cursor.type == Material.AIR) &&
            menu.data.hasFlag(MenuFlag.ALLOW_REMOVAL)) {
            return
        }

        event.isCancelled = itemInMenu.isPresent || !menu.data.hasFlag(MenuFlag.ALLOW_INPUT)

        val clickedItemsCopy = java.util.ArrayList(menu.getItems(topInventory, event.slot))

        clickedItemsCopy.forEach { menuItem ->
            if (menuItem.viewRequirements.any { !it.canView(topInventory, menuItem, menu) }) {
                return@forEach
            }

            if (menuItem.clickRequirements.any { !it.canClick(player) }) {
                return@forEach
            }

            val cdKey = "${menuItem.id}-${event.slot}-${player.uniqueId}"
            val millis = menuItem.clickCooldownUnit.toMillis(menuItem.clickCooldownTime.toLong())

            if (!checkAndSetCooldown(cdKey, millis)) {
                menuItem.internalActions.forEach { (action, data) ->
                    action.action.onClick(event, data)
                }
                java.util.ArrayList(menuItem.actions).forEach { action ->
                    action.onClick(event)
                }
            }

            val sourceInventoriesCopy = java.util.ArrayList(menuItem.sourceInventories)

            sourceInventoriesCopy.forEach { inv ->
                val slotsCopy = java.util.ArrayList(menuItem.slots)
                slotsCopy.forEach { slot ->
                    inv.setItem(slot,
                        if (menuItem.itemUpdateProvider == null) menuItem.item?.clone()
                        else menuItem.itemUpdateProvider?.requestItem(menuItem)
                    )
                }
            }
        }
    }

    @EventHandler
    override fun onOpen(event: InventoryOpenEvent) {
        val inventory = event.inventory

        if (inventory.holder !is Menu) {
            return
        }

        val menu = inventory.holder as Menu
        menu.viewers.add(event.player.uniqueId)

    }

    @EventHandler
    override fun onClose(event: InventoryCloseEvent) {
        val inventory = event.inventory

        if (inventory.holder !is Menu) {
            return
        }

        val menu = inventory.holder as Menu
        menu.viewers.remove(event.player.uniqueId)

        if (event.viewers.none { it.uniqueId != event.player.uniqueId }) {
            menu.updateTask?.cancelTask()
            menu.titleUpdateTask?.cancel()
        }

        if (menu.data.closeAction == null) {
            return
        }

        val player = event.player as Player

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            player.updateInventory()
        }, 1)

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
            menu.data.closeAction?.onClose(event)
        }, 2)
    }
}
