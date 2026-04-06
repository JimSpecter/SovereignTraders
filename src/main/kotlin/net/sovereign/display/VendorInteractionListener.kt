package net.sovereign.display

import net.sovereign.core.SovereignCore
import net.sovereign.components.catalog.TransactionMode
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType

class VendorInteractionListener(private val plugin: SovereignCore) : Listener {

    @EventHandler
    fun onVendorInteract(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (entity !is Interaction) return

        if (!entity.persistentDataContainer.has(VendorRegistry.VENDOR_TAG_KEY, PersistentDataType.BYTE)) return

        event.isCancelled = true
        val player = event.player

        val catalogId = entity.persistentDataContainer
            .get(VendorRegistry.CATALOG_LINK_KEY, PersistentDataType.STRING)

        if (catalogId.isNullOrBlank()) {
            plugin.localeManager.dispatch(player, "vendor.unlinked")
            return
        }

        val catalog = plugin.catalogRepository.resolve(catalogId)
        if (catalog == null) {
            plugin.localeManager.dispatch(player, "catalog.not-found", "%input%" to catalogId)
            return
        }

        val defaultMode = catalog.defaultTransactionMode

        plugin.catalogSessionManager.openSession(player, catalog, 0, defaultMode)
    }

    @EventHandler
    fun onEntitiesLoad(event: org.bukkit.event.world.EntitiesLoadEvent) {
        for (entity in event.entities) {
            if (entity is Interaction) {
                plugin.vendorRegistry.recoverSingleEntity(entity)
            }
        }
    }
}
