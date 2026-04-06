package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object TransactionProcessor {

    fun computeCost(
        baseCost: Double,
        stackSize: Int,
        quantity: Int,
        discountActive: Boolean = false,
        discountPrice: Double = 0.0,
        dynamicMultiplier: Double = 1.0,
        catalogDiscountPercent: Double = 0.0
    ): Double {
        val catalogAdjusted = if (catalogDiscountPercent > 0.0) {
            baseCost * (1.0 - (catalogDiscountPercent / 100.0).coerceIn(0.0, 1.0))
        } else {
            baseCost
        }
        val effectiveBase = if (discountActive && discountPrice > 0.0) discountPrice else catalogAdjusted
        val dynamicBase = effectiveBase * dynamicMultiplier
        return (dynamicBase / stackSize.coerceAtLeast(1)) * quantity
    }

    fun executeAcquisition(
        player: Player,
        listing: ItemStack,
        quantity: Int,
        unitCost: Double,
        plugin: SovereignCore
    ): TransactionResult {
        val totalCost = unitCost * quantity
        val bridge = plugin.currencyBridge

        if (!bridge.hasBalance(player, totalCost)) {
            val deficit = bridge.deficit(player, totalCost)
            return TransactionResult.InsufficientFunds(deficit)
        }

        if (!bridge.withdraw(player, totalCost)) {
            return TransactionResult.InsufficientFunds(totalCost)
        }

        val clone = listing.clone().apply { amount = quantity }
        val overflow = player.inventory.addItem(clone)
        if (overflow.isNotEmpty()) {
            overflow.values.forEach { leftover ->
                player.inventory.removeItem(leftover)
            }
            bridge.deposit(player, totalCost)
            return TransactionResult.InventoryFull
        }

        return TransactionResult.Success(totalCost)
    }

    fun executeLiquidation(
        player: Player,
        listing: ItemStack,
        quantity: Int,
        unitReward: Double,
        plugin: SovereignCore,
        partialAllowed: Boolean = false
    ): TransactionResult {
        val available = countMatchingItems(player.inventory.storageContents, listing)
        val effectiveQty = resolvePartialQuantity(quantity, available, partialAllowed)

        if (effectiveQty < 0) {
            return TransactionResult.InsufficientMaterials
        }

        val totalReward = unitReward * effectiveQty

        val target = listing.clone().apply { amount = effectiveQty }
        player.inventory.removeItem(target)

        val deposited = plugin.currencyBridge.deposit(player, totalReward)
        return if (deposited) {
            TransactionResult.Success(totalReward)
        } else {
            TransactionResult.LiquidationFailed("Economy provider rejected deposit.")
        }
    }

    fun resolvePartialQuantity(requested: Int, available: Int, partialAllowed: Boolean): Int {
        if (available >= requested) return requested
        if (!partialAllowed || available <= 0) return -1
        return available
    }

    fun countMatchingItems(contents: Array<ItemStack?>, target: ItemStack?): Int {
        if (target == null) return 0
        var count = 0
        for (stack in contents) {
            if (stack != null && stack.isSimilar(target)) {
                count += stack.amount
            }
        }
        return count
    }
}

sealed class TransactionResult {
    data class Success(val amount: Double) : TransactionResult()
    data class InsufficientFunds(val deficit: Double) : TransactionResult()
    data object InsufficientMaterials : TransactionResult()
    data object InventoryFull : TransactionResult()
    data class LiquidationFailed(val reason: String) : TransactionResult()
}
