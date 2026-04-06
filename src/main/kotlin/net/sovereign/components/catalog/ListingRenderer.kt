package net.sovereign.components.catalog

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.sovereign.components.economy.TransactionProcessor
import net.sovereign.core.SovereignCore
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

object ListingRenderer {

    private val mm = MiniMessage.miniMessage()

    val LISTING_UID_KEY: NamespacedKey by lazy {
        NamespacedKey(SovereignCore.instance.plugin, "listing_uid")
    }

    fun render(listing: Listing, mode: TransactionMode, plugin: SovereignCore, catalogDiscountPercent: Double = 0.0, viewerId: java.util.UUID? = null): ItemStack {
        val item = if (listing.itemStack != null) {
            val cloned = listing.itemStack!!.clone()
            cloned.amount = listing.stackQuantity
            cloned
        } else {
            val material = Material.matchMaterial(listing.materialId) ?: Material.STONE
            ItemStack(material, listing.stackQuantity)
        }

        if (listing.label != null) {
            val nameComponent = mm.deserialize(listing.label!!)
                .decoration(TextDecoration.ITALIC, false)
             item.setData(DataComponentTypes.CUSTOM_NAME, nameComponent)
        } else if (listing.itemStack == null && !item.hasData(DataComponentTypes.CUSTOM_NAME)) {
            val displayName = item.type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
            val nameComponent = mm.deserialize(displayName)
                .decoration(TextDecoration.ITALIC, false)
            item.setData(DataComponentTypes.CUSTOM_NAME, nameComponent)
        }

        val existingLore = item.getData(DataComponentTypes.LORE)?.lines() ?: emptyList()
        val economyLore = buildLoreComponents(listing, mode, plugin, catalogDiscountPercent, viewerId)
        if (existingLore.isNotEmpty() || economyLore.isNotEmpty()) {
            item.setData(DataComponentTypes.LORE, ItemLore.lore(existingLore + economyLore))
        }

        tagWithListingUid(item, listing.uid)

        if (listing.stackQuantity > item.type.maxStackSize) {
            item.setData(DataComponentTypes.MAX_STACK_SIZE, listing.stackQuantity)
        }

        if (listing.reductionActive || catalogDiscountPercent > 0.0) {
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        }

        return item
    }

    fun renderNavButton(material: Material, label: String): ItemStack {
        val item = ItemStack(material, 1)
        val nameComponent = mm.deserialize(label)
            .decoration(TextDecoration.ITALIC, false)
        item.setData(DataComponentTypes.CUSTOM_NAME, nameComponent)

        return item
    }

    private fun buildLoreComponents(listing: Listing, mode: TransactionMode, plugin: SovereignCore, catalogDiscountPercent: Double = 0.0, viewerId: java.util.UUID? = null): List<Component> {
        val locale = plugin.localeManager
        val settings = plugin.settingsManager
        val gui = plugin.guiConfigManager
        val lore = mutableListOf<Component>()

        val dynMultiplier = plugin.pricingModule.multiplierFor(listing.uid)

        lore += Component.empty()

        if (listing.showCost) {
            when (mode) {
                TransactionMode.ACQUIRE -> {
                    val cost = TransactionProcessor.computeCost(
                        listing.acquireCost, listing.stackQuantity, listing.stackQuantity,
                        listing.reductionActive, listing.reductionOverrideCost,
                        dynMultiplier, catalogDiscountPercent
                    )
                    lore += buildCostLine(
                        locale.acquireCostFormat,
                        locale.formatCurrency(cost),
                        NamedTextColor.GREEN
                    )

                    if ((listing.reductionActive && listing.showReductionCost) || catalogDiscountPercent > 0.0) {
                        val original = TransactionProcessor.computeCost(
                            listing.acquireCost, listing.stackQuantity, listing.stackQuantity,
                            false, 0.0,
                            dynMultiplier, 0.0
                        )
                        lore += Component.text("  ${locale.formatCurrency(original)}", NamedTextColor.GRAY)
                            .decoration(TextDecoration.STRIKETHROUGH, true)
                            .decoration(TextDecoration.ITALIC, false)
                    }
                }

                TransactionMode.LIQUIDATE -> {
                    val reward = TransactionProcessor.computeCost(
                        listing.liquidateReward, listing.stackQuantity, listing.stackQuantity,
                        dynamicMultiplier = dynMultiplier,
                        catalogDiscountPercent = catalogDiscountPercent
                    )
                    lore += buildCostLine(
                        locale.liquidateRewardFormat,
                        locale.formatCurrency(reward),
                        NamedTextColor.GOLD
                    )
                }

                TransactionMode.BARTER -> {
                    if (listing.barterRequirements.isNotEmpty()) {
                        lore += mm.deserialize(gui.barterRequiredLabel)
                            .decoration(TextDecoration.ITALIC, false)
                        listing.barterRequirements.forEach { req ->
                            val reqLabel = req.label ?: req.materialId.lowercase().replace('_', ' ')
                            lore += Component.text("  ${req.quantity}x ", NamedTextColor.WHITE)
                                .append(Component.text(reqLabel, NamedTextColor.YELLOW))
                                .decoration(TextDecoration.ITALIC, false)
                        }
                    }
                    if (listing.barterReceivables.isNotEmpty()) {
                        lore += mm.deserialize(gui.barterReceiveLabel)
                            .decoration(TextDecoration.ITALIC, false)
                        listing.barterReceivables.forEach { rec ->
                            val recLabel = rec.label ?: rec.materialId.lowercase().replace('_', ' ')
                            lore += Component.text("  ${rec.quantity}x ", NamedTextColor.WHITE)
                                .append(Component.text(recLabel, NamedTextColor.AQUA))
                                .decoration(TextDecoration.ITALIC, false)
                        }
                    }
                }
            }
        }

        if (plugin.pricingModule.showTrend && mode != TransactionMode.BARTER) {
            val trend = plugin.pricingModule.trendIndicator(dynMultiplier)
            val trendColor = when (trend) {
                "↑" -> NamedTextColor.RED
                "↓" -> NamedTextColor.GREEN
                else -> NamedTextColor.GRAY
            }
            lore += Component.text("  $trend ᴍᴀʀᴋᴇᴛ ᴛʀᴇɴᴅ", trendColor)
                .decoration(TextDecoration.ITALIC, false)
        }

        if (listing.reductionActive || catalogDiscountPercent > 0.0) {
            if ((listing.showReductionDuration && listing.reductionWindowDurationSec > 0) || catalogDiscountPercent > 0.0) {
                lore += Component.empty()
                val badgeToUse = if (catalogDiscountPercent > 0.0) {
                    plugin.guiConfigManager.catalogReductionBadge
                } else {
                    plugin.guiConfigManager.reductionBadge
                }
                lore += mm.deserialize(badgeToUse)
                    .decoration(TextDecoration.ITALIC, false)
            }
        }

        if (listing.showQuotaProgress && listing.quotaLimit > 0) {
            lore += Component.empty()
            val currentUsage = if (viewerId != null) plugin.quotaLedger.getUsage(viewerId, listing.uid) else 0
            val progressText = locale.resolveFragment(
                "display-metadata.quota-progress",
                "%current%" to currentUsage.toString(),
                "%maximum%" to listing.quotaLimit.toString()
            )
            lore += progressText
                .decoration(TextDecoration.ITALIC, false)

            if (listing.showQuotaTimer && listing.quotaResetIntervalSec > 0) {
                val remaining = plugin.quotaEnforcer.getTimeRemaining(listing.uid)
                if (remaining > 0) {
                    val timerText = locale.resolveFragment(
                        "display-metadata.quota-remaining",
                        "%remaining%" to formatDuration(remaining)
                    )
                    lore += timerText
                        .decoration(TextDecoration.ITALIC, false)
                }
            }
        }

        if (listing.showAnnotations && listing.annotations.isNotEmpty()) {
            lore += Component.empty()
            listing.annotations.forEach { line ->
                lore += mm.deserialize(line)
                    .decoration(TextDecoration.ITALIC, false)
            }
        }

        lore += Component.empty()
        val clickHint = when (mode) {
            TransactionMode.ACQUIRE -> gui.clickAcquire
            TransactionMode.LIQUIDATE -> gui.clickLiquidate
            TransactionMode.BARTER -> gui.clickBarter
        }
        lore += mm.deserialize(clickHint)
            .decoration(TextDecoration.ITALIC, false)

        if (mode != TransactionMode.BARTER && listing.type != ListingType.STATIC
            && plugin.guiConfigManager.quantitySelectorEnabled) {
            lore += mm.deserialize(plugin.guiConfigManager.shiftClickHint)
                .decoration(TextDecoration.ITALIC, false)
        }

        return lore
    }

    private fun buildCostLine(template: String, amount: String, color: NamedTextColor): Component {
        val resolved = template.replace("%amount%", amount)
        return mm.deserialize(resolved)
            .colorIfAbsent(color)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun tagWithListingUid(item: ItemStack, uid: Int) {

        item.editMeta { meta ->
            meta.persistentDataContainer.set(
                LISTING_UID_KEY,
                org.bukkit.persistence.PersistentDataType.INTEGER,
                uid
            )
        }
    }

    fun readListingUid(item: ItemStack): Int? {
        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer
        return if (pdc.has(LISTING_UID_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            pdc.get(LISTING_UID_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)
        } else null
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "-"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (secs > 0 || isEmpty()) append("${secs}s")
        }.trim()
    }

}
