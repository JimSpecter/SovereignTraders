package net.sovereign.pricing

import org.bukkit.entity.Player

class NoOpPricingModule : PricingModule {
    override val isEnabled: Boolean = false
    override val showTrend: Boolean = false

    override fun multiplierFor(listingUid: Int): Double = 1.0
    override fun recordBuy(listingUid: Int) {}
    override fun recordSell(listingUid: Int) {}
    override fun recordSnapshot(listingUid: Int, multiplier: Double, effectivePrice: Double, buyCount: Int, sellCount: Int) {}
    override fun trendIndicator(multiplier: Double): String = "—"
    override fun openDashboard(player: Player) {}
    override fun onPeriodicTick() {}
    override fun onUidMigrated(oldUid: Int, newUid: Int) {}
    override fun flush() {}
    override fun start() {}
    override fun stop() {}
}
