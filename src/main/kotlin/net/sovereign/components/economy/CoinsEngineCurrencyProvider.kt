package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player

class CoinsEngineCurrencyProvider(private val plugin: SovereignCore) : CurrencyProvider {

    private val apiClass: Class<*>?
    private val currencyObj: Any?
    override val isReady: Boolean
    override val providerName: String

    init {
        var tempApiClass: Class<*>? = null
        var tempCurrency: Any? = null
        var ready = false
        val currencyId = plugin.settingsManager.configuration
            .getString("general.coins-engine-currency", "coins") ?: "coins"

        try {
            tempApiClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI")

            val getCurrency = tempApiClass.getMethod("getCurrency", String::class.java)
            tempCurrency = getCurrency.invoke(null, currencyId)

            if (tempCurrency != null) {
                ready = true
            } else {
                plugin.logger.warning("CoinsEngine currency '$currencyId' not found — provider disabled.")
            }
        } catch (_: Exception) {
            plugin.logger.warning("CoinsEngine API not available — provider disabled.")
        }

        apiClass = tempApiClass
        currencyObj = tempCurrency
        isReady = ready
        providerName = "CoinsEngine"
    }

    override fun balanceOf(player: Player): Double {
        if (apiClass == null || currencyObj == null) return 0.0
        return try {

            val currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency")
            val method = apiClass.getMethod("getBalance", Player::class.java, currencyClass)
            (method.invoke(null, player, currencyObj) as? Double) ?: 0.0
        } catch (_: Exception) { 0.0 }
    }

    override fun withdraw(player: Player, amount: Double): Boolean {
        if (apiClass == null || currencyObj == null) return false
        return try {

            val currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency")
            val method = apiClass.getMethod("removeBalance", Player::class.java, currencyClass, Double::class.java)
            method.invoke(null, player, currencyObj, amount)
            true
        } catch (_: Exception) { false }
    }

    override fun deposit(player: Player, amount: Double): Boolean {
        if (apiClass == null || currencyObj == null) return false
        return try {

            val currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency")
            val method = apiClass.getMethod("addBalance", Player::class.java, currencyClass, Double::class.java)
            method.invoke(null, player, currencyObj, amount)
            true
        } catch (_: Exception) { false }
    }

    override fun hasBalance(player: Player, amount: Double): Boolean {
        return balanceOf(player) >= amount
    }
}
