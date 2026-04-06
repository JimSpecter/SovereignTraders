package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player

class GemsEconomyCurrencyProvider(private val plugin: SovereignCore) : CurrencyProvider {

    private val api: Any?
    override val isReady: Boolean
    override val providerName: String

    init {
        var tempApi: Any? = null
        var ready = false
        var name = "GemsEconomy"

        try {
            val apiClass = Class.forName("me.xanium.gemseconomy.api.GemsEconomyAPI")
            tempApi = apiClass.getDeclaredConstructor().newInstance()
            ready = true
        } catch (_: Exception) {
            plugin.logger.warning("GemsEconomy API not available — provider disabled.")
        }

        api = tempApi
        isReady = ready
        providerName = name
    }

    override fun balanceOf(player: Player): Double {
        if (api == null) return 0.0
        return try {
            val method = api::class.java.getMethod("getBalance", java.util.UUID::class.java)
            (method.invoke(api, player.uniqueId) as? Double) ?: 0.0
        } catch (_: Exception) { 0.0 }
    }

    override fun withdraw(player: Player, amount: Double): Boolean {
        if (api == null) return false
        return try {
            val method = api::class.java.getMethod("withdraw", java.util.UUID::class.java, Double::class.java)
            method.invoke(api, player.uniqueId, amount)
            true
        } catch (_: Exception) { false }
    }

    override fun deposit(player: Player, amount: Double): Boolean {
        if (api == null) return false
        return try {
            val method = api::class.java.getMethod("deposit", java.util.UUID::class.java, Double::class.java)
            method.invoke(api, player.uniqueId, amount)
            true
        } catch (_: Exception) { false }
    }

    override fun hasBalance(player: Player, amount: Double): Boolean {
        return balanceOf(player) >= amount
    }
}
