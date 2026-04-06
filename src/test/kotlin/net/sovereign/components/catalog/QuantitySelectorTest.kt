package net.sovereign.components.catalog

import net.sovereign.components.economy.TransactionProcessor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class QuantitySelectorTest {

    @Test
    fun `computeCost scales linearly with requested quantity`() {
        val singleUnit = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 10, quantity = 1
        )
        val eightUnits = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 10, quantity = 8
        )
        assertEquals(singleUnit * 8, eightUnits, 0.001)
    }

    @Test
    fun `computeCost for full stack equals base cost`() {
        val fullStack = TransactionProcessor.computeCost(
            baseCost = 100.0, stackSize = 10, quantity = 10
        )
        assertEquals(100.0, fullStack, 0.001)
    }

    @Test
    fun `computeCost for 64 units from stack of 1`() {
        val cost = TransactionProcessor.computeCost(
            baseCost = 50.0, stackSize = 1, quantity = 64
        )
        assertEquals(50.0 * 64, cost, 0.001)
    }

    @Test
    fun `resolveQuantityPresets returns clamped values`() {
        val presets = QuantityPresets.resolve(stackQuantity = 5, maxStackSize = 64)
        assertTrue(presets.contains(1))
        assertTrue(presets.contains(5))
        assertTrue(presets.all { it <= 64 })
        assertEquals(presets, presets.sorted())
        assertEquals(presets.size, presets.toSet().size)
    }

    @Test
    fun `resolveQuantityPresets for stack of 1 includes bulk tiers`() {
        val presets = QuantityPresets.resolve(stackQuantity = 1, maxStackSize = 64)
        assertTrue(presets.contains(1))
        assertTrue(presets.contains(16))
        assertTrue(presets.contains(32))
        assertTrue(presets.contains(64))
    }

    @Test
    fun `resolveQuantityPresets for large stack respects max`() {
        val presets = QuantityPresets.resolve(stackQuantity = 32, maxStackSize = 64)
        assertTrue(presets.contains(1))
        assertTrue(presets.contains(32))
        assertTrue(presets.contains(64))
        assertTrue(presets.all { it <= 64 })
    }

    @Test
    fun `resolveQuantityPresets for unstackable items`() {
        val presets = QuantityPresets.resolve(stackQuantity = 1, maxStackSize = 1)
        assertEquals(listOf(1), presets)
    }

    @Test
    fun `resolveQuantityPresets deduplicates and sorts`() {
        val presets = QuantityPresets.resolve(stackQuantity = 16, maxStackSize = 64)
        assertEquals(presets.size, presets.toSet().size)
        assertEquals(presets, presets.sorted())
    }
}
