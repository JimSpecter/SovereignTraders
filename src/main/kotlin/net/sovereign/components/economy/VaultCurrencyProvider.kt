package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.bukkit.plugin.RegisteredServiceProvider

class VaultCurrencyProvider(plugin: SovereignCore) : CurrencyProvider {

    private val economy: Economy?
    override val isReady: Boolean
    override val providerName: String

    init {
        val registration: RegisteredServiceProvider<Economy>? =
            plugin.server.servicesManager.getRegistration(Economy::class.java)
        economy = registration?.provider
        isReady = economy != null
        providerName = economy?.name ?: "None"
        if (economy == null) {
            plugin.logger.warning("Vault is installed but no Economy service is registered — provider disabled.")
        }
    }

    override fun balanceOf(player: Player): Double {
        return economy?.getBalance(player) ?: 0.0
    }

    override fun withdraw(player: Player, amount: Double): Boolean {
        if (economy == null) return false
        val response = economy.withdrawPlayer(player, amount)
        return response.transactionSuccess()
    }

    override fun deposit(player: Player, amount: Double): Boolean {
        if (economy == null) return false
        val response = economy.depositPlayer(player, amount)
        return response.transactionSuccess()
    }

    override fun hasBalance(player: Player, amount: Double): Boolean {
        return economy?.has(player, amount) ?: false
    }
}
