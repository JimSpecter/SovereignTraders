package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player

class CurrencyBridge(private val plugin: SovereignCore) {

    private val provider: CurrencyProvider

    val isReady: Boolean get() = provider.isReady
    val providerName: String get() = provider.providerName

    init {
        val chosen = plugin.settingsManager.economyProvider.lowercase().trim()
        provider = when (chosen) {
            "vault"        -> VaultCurrencyProvider(plugin)
            "gemseconomy"  -> GemsEconomyCurrencyProvider(plugin)
            "coinsengine"  -> CoinsEngineCurrencyProvider(plugin)
            "playerpoints" -> PlayerPointsCurrencyProvider(plugin)
            else -> {
                plugin.logger.warning(
                    "Unknown economy-provider '$chosen' in config.yml — falling back to Vault."
                )
                VaultCurrencyProvider(plugin)
            }
        }
    }

    fun balanceOf(player: Player): Double = provider.balanceOf(player)

    fun withdraw(player: Player, amount: Double): Boolean = provider.withdraw(player, amount)

    fun deposit(player: Player, amount: Double): Boolean = provider.deposit(player, amount)

    fun hasBalance(player: Player, amount: Double): Boolean = provider.hasBalance(player, amount)

    fun deficit(player: Player, amount: Double): Double {
        val balance = balanceOf(player)
        return if (balance >= amount) 0.0 else amount - balance
    }
}
