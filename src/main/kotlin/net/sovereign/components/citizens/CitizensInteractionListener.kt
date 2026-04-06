package net.sovereign.components.citizens

import net.citizensnpcs.api.event.NPCRightClickEvent
import net.sovereign.core.SovereignCore
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CitizensInteractionListener(private val plugin: SovereignCore) : Listener {

    @EventHandler
    fun onCitizensNpcRightClick(event: NPCRightClickEvent) {
        val bridge = plugin.citizensBridgeManager ?: return
        val npcId = event.npc.id
        val catalogId = bridge.resolve(npcId) ?: return

        event.isCancelled = true
        val player = event.clicker

        val catalog = plugin.catalogRepository.resolve(catalogId)
        if (catalog == null) {
            plugin.localeManager.dispatch(player, "citizen.catalog-not-found", "%catalog%" to catalogId)
            return
        }

        val defaultMode = catalog.defaultTransactionMode
        plugin.catalogSessionManager.openSession(player, catalog, 0, defaultMode)
    }
}
