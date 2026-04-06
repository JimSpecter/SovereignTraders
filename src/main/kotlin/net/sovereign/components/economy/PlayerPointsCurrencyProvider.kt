package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player

class PlayerPointsCurrencyProvider(private val plugin: SovereignCore) : CurrencyProvider {

    private val api: Any?
    override val isReady: Boolean
    override val providerName: String

    init {
        var tempApi: Any? = null
        var ready = false

        try {

            val ppClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints")
            val getInstance = ppClass.getMethod("getInstance")
            val ppInstance = getInstance.invoke(null)
            if (ppInstance != null) {
                val getAPI = ppInstance::class.java.getMethod("getAPI")
                tempApi = getAPI.invoke(ppInstance)
                ready = tempApi != null
            }
        } catch (_: Exception) {
            plugin.logger.warning("PlayerPoints API not available — provider disabled.")
        }

        api = tempApi
        isReady = ready
        providerName = "PlayerPoints"
    }

    override fun balanceOf(player: Player): Double {
        if (api == null) return 0.0
        return try {

            val method = api::class.java.getMethod("look", java.util.UUID::class.java)
            (method.invoke(api, player.uniqueId) as? Int)?.toDouble() ?: 0.0
        } catch (_: Exception) { 0.0 }
    }

    override fun withdraw(player: Player, amount: Double): Boolean {
        if (api == null) return false
        return try {

            val method = api::class.java.getMethod("take", java.util.UUID::class.java, Int::class.java)
            (method.invoke(api, player.uniqueId, amount.toInt()) as? Boolean) ?: false
        } catch (_: Exception) { false }
    }

    override fun deposit(player: Player, amount: Double): Boolean {
        if (api == null) return false
        return try {

            val method = api::class.java.getMethod("give", java.util.UUID::class.java, Int::class.java)
            (method.invoke(api, player.uniqueId, amount.toInt()) as? Boolean) ?: false
        } catch (_: Exception) { false }
    }

    override fun hasBalance(player: Player, amount: Double): Boolean {
        return balanceOf(player) >= amount
    }
}
