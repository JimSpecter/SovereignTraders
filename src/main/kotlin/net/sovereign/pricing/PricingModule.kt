package net.sovereign.pricing

import org.bukkit.entity.Player

interface PricingModule {
    val isEnabled: Boolean
    val showTrend: Boolean

    fun multiplierFor(listingUid: Int): Double
    fun recordBuy(listingUid: Int)
    fun recordSell(listingUid: Int)
    fun recordSnapshot(listingUid: Int, multiplier: Double, effectivePrice: Double, buyCount: Int, sellCount: Int)
    fun trendIndicator(multiplier: Double): String
    fun openDashboard(player: Player)
    fun onPeriodicTick()
    fun onUidMigrated(oldUid: Int, newUid: Int)
    fun flush()
    fun start()
    fun stop()
}
