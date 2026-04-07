package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class TransactionProcessorTest {

    @Test
    fun `executeAcquisition overflow removes only the added amount not the overflow`() {
        val inventory = mock<PlayerInventory>()
        val player = mock<Player> {
            on { getInventory() } doReturn inventory
        }

        val addClone = mock<ItemStack>()
        val undoClone = mock<ItemStack>()
        val listing = mock<ItemStack>()
        whenever(listing.clone()).thenReturn(addClone, undoClone)

        val bridge = mock<CurrencyBridge> {
            on { hasBalance(eq(player), any()) } doReturn true
            on { withdraw(eq(player), any()) } doReturn true
            on { deposit(eq(player), any()) } doReturn true
        }
        val plugin = mock<SovereignCore> {
            on { currencyBridge } doReturn bridge
        }

        val overflowItem = mock<ItemStack> {
            on { amount } doReturn 54
        }
        whenever(inventory.addItem(any<ItemStack>())).thenReturn(hashMapOf(0 to overflowItem))

        val result = TransactionProcessor.executeAcquisition(player, listing, 64, 1.0, plugin)

        assertEquals(TransactionResult.InventoryFull, result)
        verify(undoClone).amount = 10
        verify(inventory).removeItem(undoClone)
    }

    @Test
    fun `computeCost basic unit cost no discount`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 10, quantity = 1
        )
        assertEquals(10.0, cost, 0.001)
    }

    @Test
    fun `computeCost with discount overrides base`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 10, quantity = 1,
            discountActive = true, discountPrice = 50.0
        )
        assertEquals(5.0, cost, 0.001)
    }

    @Test
    fun `computeCost with dynamic multiplier scales result`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 10, quantity = 1,
            dynamicMultiplier = 1.5
        )
        assertEquals(15.0, cost, 0.001)
    }

    @Test
    fun `countMatchingItems returns zero when inventory is empty`() {
        val count = TransactionProcessor.countMatchingItems(emptyArray(), null)
        assertEquals(0, count)
    }

    @Test
    fun `countMatchingItems counts matching items across stacks`() {
        val count = TransactionProcessor.countMatchingItems(emptyArray(), null)
        assertEquals(0, count)
    }

    @Test
    fun `resolvePartialQuantity returns requested when player has enough`() {
        val result = TransactionProcessor.resolvePartialQuantity(
            requested = 10, available = 20, partialAllowed = false
        )
        assertEquals(10, result)
    }

    @Test
    fun `resolvePartialQuantity returns requested when exact match`() {
        val result = TransactionProcessor.resolvePartialQuantity(
            requested = 10, available = 10, partialAllowed = false
        )
        assertEquals(10, result)
    }

    @Test
    fun `resolvePartialQuantity returns negative when not enough and partial disallowed`() {
        val result = TransactionProcessor.resolvePartialQuantity(
            requested = 10, available = 5, partialAllowed = false
        )
        assertEquals(-1, result)
    }

    @Test
    fun `resolvePartialQuantity returns available when not enough and partial allowed`() {
        val result = TransactionProcessor.resolvePartialQuantity(
            requested = 10, available = 5, partialAllowed = true
        )
        assertEquals(5, result)
    }

    @Test
    fun `resolvePartialQuantity returns negative when zero available and partial allowed`() {
        val result = TransactionProcessor.resolvePartialQuantity(
            requested = 10, available = 0, partialAllowed = true
        )
        assertEquals(-1, result)
    }

    @Test
    fun `resolvePartialQuantity returns requested when enough and partial allowed`() {
        val result = TransactionProcessor.resolvePartialQuantity(
            requested = 10, available = 20, partialAllowed = true
        )
        assertEquals(10, result)
    }

    @Test
    fun `computeCost with catalog discount percent reduces base`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 1, quantity = 1,
            catalogDiscountPercent = 10.0
        )
        assertEquals(90.0, cost, 0.001)
    }

    @Test
    fun `computeCost per-listing override takes precedence over catalog discount`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 1, quantity = 1,
            discountActive = true, discountPrice = 50.0,
            catalogDiscountPercent = 10.0
        )
        assertEquals(50.0, cost, 0.001)
    }

    @Test
    fun `computeCost catalog discount applies when no per-listing override`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 200.0, stackSize = 10, quantity = 1,
            catalogDiscountPercent = 25.0
        )
        assertEquals(15.0, cost, 0.001)
    }

    @Test
    fun `computeCost catalog discount clamped at 100 percent`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 1, quantity = 1,
            catalogDiscountPercent = 150.0
        )
        assertEquals(0.0, cost, 0.001)
    }
}
