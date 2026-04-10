package net.sovereign.core

import net.sovereign.pricing.NoOpPricingModule
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin

class SovereignFreePlugin : JavaPlugin() {

    override fun onEnable() {
        Metrics(this, 30709)
        val core = SovereignCore(this, NoOpPricingModule())
        core.enable()
    }

    override fun onDisable() {
        SovereignCore.instanceOrNull?.disable()
    }
}
