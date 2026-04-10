package net.sovereign.core

import net.sovereign.core.command.SovereignCommandExecutor
import net.sovereign.core.config.SettingsManager
import net.sovereign.core.config.GuiConfigManager
import net.sovereign.core.config.LocaleManager
import net.sovereign.core.config.NpcManager
import net.sovereign.display.VendorInteractionListener
import net.sovereign.display.VendorRegistry
import net.sovereign.display.VendorTickService
import net.sovereign.components.CatalogRepository
import net.sovereign.components.economy.CurrencyBridge
import net.sovereign.components.citizens.CitizensBridgeManager
import net.sovereign.components.citizens.CitizensInteractionListener
import net.sovereign.components.quota.QuotaLedger
import net.sovereign.components.quota.QuotaEnforcer
import net.sovereign.components.catalog.CatalogSessionManager
import net.sovereign.components.catalog.CatalogEditor
import net.sovereign.components.catalog.ReductionWindowScheduler
import net.sovereign.components.audit.TransactionLogger
import net.sovereign.pricing.PricingModule
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class SovereignCore(val plugin: JavaPlugin, val pricingModule: PricingModule) {

    lateinit var settingsManager: SettingsManager private set
    lateinit var guiConfigManager: GuiConfigManager private set
    lateinit var localeManager: LocaleManager private set
    lateinit var npcManager: NpcManager private set
    lateinit var currencyBridge: CurrencyBridge private set
    lateinit var catalogRepository: CatalogRepository private set
    lateinit var quotaLedger: QuotaLedger private set
    lateinit var quotaEnforcer: QuotaEnforcer private set
    lateinit var vendorRegistry: VendorRegistry private set
    lateinit var vendorTickService: VendorTickService private set
    lateinit var catalogSessionManager: CatalogSessionManager private set
    lateinit var reductionScheduler: ReductionWindowScheduler private set
    lateinit var catalogEditor: CatalogEditor private set
    lateinit var foliaLib: com.tcoded.folialib.FoliaLib private set
    var transactionLogger: TransactionLogger? = null
        private set
    var citizensBridgeManager: CitizensBridgeManager? = null
        private set

    val dataFolder get() = plugin.dataFolder
    val server get() = plugin.server
    val logger get() = plugin.logger
    val pluginMeta get() = plugin.pluginMeta

    fun getResource(name: String) = plugin.getResource(name)
    fun saveResource(name: String, replace: Boolean) = plugin.saveResource(name, replace)

    fun enable() {
        instance = this
        foliaLib = com.tcoded.folialib.FoliaLib(plugin)

        broadcast("<white>SovereignTraders <gray>v${pluginMeta.version} <gray>— Initialising...")

        settingsManager = SettingsManager(this)
        guiConfigManager = GuiConfigManager(this)
        localeManager = LocaleManager(this)
        npcManager = NpcManager(this)

        val chosenProvider = settingsManager.economyProvider.lowercase().trim()
        if (chosenProvider == "vault") {
            val vaultPlugin = server.pluginManager.getPlugin("Vault")
            if (vaultPlugin == null) {
                broadcast("<red>Economy provider is set to 'vault' but Vault is not installed. Shutting down.")
                logger.severe("Economy provider is set to 'vault' but Vault is not installed. Shutting down.")
                server.pluginManager.disablePlugin(plugin)
                return
            }
        }
        currencyBridge = CurrencyBridge(this)
        if (!currencyBridge.isReady) {
            broadcast("<red>Economy provider '$chosenProvider' failed to initialise. Shutting down.")
            logger.severe("Economy provider '$chosenProvider' failed to initialise. Shutting down.")
            server.pluginManager.disablePlugin(plugin)
            return
        }
        broadcast("<green>Economy provider linked: <yellow>${currencyBridge.providerName}")

        catalogRepository = CatalogRepository(this)
        quotaLedger = QuotaLedger(this)
        quotaEnforcer = QuotaEnforcer(this, quotaLedger)
        quotaEnforcer.syncFromCatalogs()
        reductionScheduler = ReductionWindowScheduler(this)
        pricingModule.start()
        if (!pricingModule.isEnabled) {
            broadcast("<gray>Dynamic pricing: <white>disabled")
        }

        if (settingsManager.transactionLogEnabled) {
            transactionLogger = TransactionLogger(dataFolder)
            broadcast("<green>Transaction logging: <yellow>enabled")
        } else {
            broadcast("<gray>Transaction logging: <white>disabled")
        }

        vendorRegistry = VendorRegistry(this)
        vendorTickService = VendorTickService(this)
        vendorTickService.start()
        val vendorListener = VendorInteractionListener(this)
        server.pluginManager.registerEvents(vendorListener, plugin)
        vendorRegistry.scanAndRecover()

        if (server.pluginManager.getPlugin("Citizens") != null) {
            try {
                citizensBridgeManager = CitizensBridgeManager(this)
                val citizensListener = CitizensInteractionListener(this)
                server.pluginManager.registerEvents(citizensListener, plugin)
                broadcast("<green>Citizens bridge enabled — <yellow>${citizensBridgeManager!!.allMappings().size} <green>NPC link(s) loaded.")
            } catch (ex: Exception) {
                broadcast("<yellow>Citizens detected but bridge failed to initialise: <gray>${ex.message}")
                citizensBridgeManager = null
            }
        }

        catalogSessionManager = CatalogSessionManager(this)
        catalogEditor = CatalogEditor(this)
        server.pluginManager.registerEvents(catalogSessionManager.menuListener, plugin)
        server.pluginManager.registerEvents(catalogEditor, plugin)

        val joinListener = JoinListener(this)
        server.pluginManager.registerEvents(joinListener, plugin)

        val devCommands: net.sovereign.core.command.DevCommandSupport? = try {
            Class.forName("net.sovereign.core.command.PremiumDevCommandSupport")
                .getConstructor(SovereignCore::class.java)
                .newInstance(this) as net.sovereign.core.command.DevCommandSupport
        } catch (_: Exception) {
            null
        }

        val commandExecutor = SovereignCommandExecutor(this, devCommands)
        plugin.lifecycleManager.registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                commandExecutor.buildCommandTree(),
                "Core command for SovereignTraders",
                listOf("st", "svt")
            )
        }

        broadcast("<green>SovereignTraders loaded successfully.")
    }

    fun disable() {
        pricingModule.stop()
        pricingModule.flush()
        if (::quotaEnforcer.isInitialized) quotaEnforcer.stop()
        if (::reductionScheduler.isInitialized) reductionScheduler.stop()
        if (::catalogSessionManager.isInitialized) catalogSessionManager.shutdown()
        if (::catalogRepository.isInitialized) catalogRepository.persistAll()
        if (::quotaLedger.isInitialized) quotaLedger.flush()
        citizensBridgeManager?.flush()
        if (::vendorTickService.isInitialized) vendorTickService.stop()
        if (::vendorRegistry.isInitialized) vendorRegistry.despawnAll()
        broadcast("<yellow>SovereignTraders shut down gracefully.")
    }

    fun reload() {
        broadcast("<yellow>Reloading SovereignTraders...")
        if (::catalogRepository.isInitialized) catalogRepository.persistAll()
        settingsManager.reload()
        guiConfigManager.reload()
        localeManager.reload()
        npcManager.reload()
        catalogRepository.loadAll()
        citizensBridgeManager?.load()

        pricingModule.stop()
        pricingModule.start()

        if (::quotaEnforcer.isInitialized) {
            quotaEnforcer.stop()
            quotaEnforcer.syncFromCatalogs()
            quotaEnforcer.start()
        }

        if (::reductionScheduler.isInitialized) {
            reductionScheduler.stop()
            reductionScheduler.syncFromCatalogs()
            reductionScheduler.start()
        }

        if (::vendorRegistry.isInitialized) vendorRegistry.syncFromConfig()
        broadcast("<green>Reload complete.")
    }

    companion object {
        lateinit var instance: SovereignCore private set

        val instanceOrNull: SovereignCore?
            get() = if (::instance.isInitialized) instance else null

        fun broadcast(message: String) {
            Bukkit.getConsoleSender().sendMessage(
                net.kyori.adventure.text.minimessage.MiniMessage
                    .miniMessage().deserialize("<white><bold>Sovereign</bold> <gray>| $message")
            )
        }
    }

    private class JoinListener(private val core: SovereignCore) : Listener {
        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(core.plugin, Runnable {
                core.vendorRegistry.showVendorsTo(event.player)
            }, 5L)
        }
    }
}
