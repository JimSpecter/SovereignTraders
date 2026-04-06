package net.sovereign.components.economy

import org.bukkit.entity.Player

interface CurrencyProvider {

    val isReady: Boolean

    val providerName: String

    fun balanceOf(player: Player): Double

    fun withdraw(player: Player, amount: Double): Boolean

    fun deposit(player: Player, amount: Double): Boolean

    fun hasBalance(player: Player, amount: Double): Boolean
}
