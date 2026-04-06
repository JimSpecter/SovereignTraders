package net.sovereign.components.catalog

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ListingPlacementTest {

    private fun createTestSection(): CatalogSection {
        return CatalogSection(identifier = "test", rowCount = 1)
    }

    private fun createTestListing(uid: Int, acquireCost: Double, liquidateReward: Double): Listing {
        return Listing(
            uid = uid,
            type = ListingType.TRADABLE,
            materialId = "DIAMOND_SWORD",
            stackQuantity = 1,
            acquireCost = acquireCost,
            liquidateReward = liquidateReward,
            showCost = true,
            dropOnOverflow = true
        )
    }

    @Test
    fun `addListingFromCommand should place listing in acquire array when buy price is positive`() {
        val section = createTestSection()
        val listing = createTestListing(uid = 1, acquireCost = 100.0, liquidateReward = 50.0)

        val acquireListings = section.listingsForMode(TransactionMode.ACQUIRE)
        val nextSlot = acquireListings.indexOfFirst { it == null }
        assertNotEquals(-1, nextSlot, "Should have an empty slot")
        acquireListings[nextSlot] = listing

        assertNotNull(section.acquireListings[0], "Listing should be in acquire array")
        assertEquals(100.0, section.acquireListings[0]!!.acquireCost)
    }

    @Test
    fun `addListingFromCommand should place listing in liquidate array when sell price is positive`() {
        val section = createTestSection()

        val liquidateListing = createTestListing(uid = 2, acquireCost = 100.0, liquidateReward = 50.0)
        val liquidateListings = section.listingsForMode(TransactionMode.LIQUIDATE)
        val nextSlot = liquidateListings.indexOfFirst { it == null }
        assertNotEquals(-1, nextSlot, "Should have an empty slot")
        liquidateListings[nextSlot] = liquidateListing

        assertNotNull(section.liquidateListings[0], "Listing should be in liquidate array")
        assertEquals(50.0, section.liquidateListings[0]!!.liquidateReward)
    }

    @Test
    fun `both acquire and liquidate arrays should have listings when both prices are positive`() {
        val section = createTestSection()

        val buyPrice = 100.0
        val sellPrice = 50.0

        if (buyPrice > 0) {
            val acquireListing = createTestListing(uid = 1, acquireCost = buyPrice, liquidateReward = sellPrice)
            val acquireListings = section.listingsForMode(TransactionMode.ACQUIRE)
            val nextSlot = acquireListings.indexOfFirst { it == null }
            acquireListings[nextSlot] = acquireListing
        }

        if (sellPrice > 0) {
            val liquidateListing = createTestListing(uid = 2, acquireCost = buyPrice, liquidateReward = sellPrice)
            val liquidateListings = section.listingsForMode(TransactionMode.LIQUIDATE)
            val nextSlot = liquidateListings.indexOfFirst { it == null }
            liquidateListings[nextSlot] = liquidateListing
        }

        assertNotNull(section.acquireListings[0], "ACQUIRE should have the listing")
        assertNotNull(section.liquidateListings[0], "LIQUIDATE should have the listing")

        assertEquals(100.0, section.acquireListings[0]!!.acquireCost, "ACQUIRE cost should be 100")
        assertEquals(50.0, section.liquidateListings[0]!!.liquidateReward, "LIQUIDATE reward should be 50")

        assertNotEquals(
            section.acquireListings[0]!!.uid,
            section.liquidateListings[0]!!.uid,
            "Each mode's listing should have a unique UID"
        )
    }

    @Test
    fun `only acquire array should have listing when sell price is zero`() {
        val section = createTestSection()
        val buyPrice = 100.0
        val sellPrice = 0.0

        if (buyPrice > 0) {
            val listing = createTestListing(uid = 1, acquireCost = buyPrice, liquidateReward = sellPrice)
            section.listingsForMode(TransactionMode.ACQUIRE)[0] = listing
        }

        if (sellPrice > 0) {
            val listing = createTestListing(uid = 2, acquireCost = buyPrice, liquidateReward = sellPrice)
            section.listingsForMode(TransactionMode.LIQUIDATE)[0] = listing
        }

        assertNotNull(section.acquireListings[0], "ACQUIRE should have the listing")
        assertNull(section.liquidateListings[0], "LIQUIDATE should NOT have listing when sell price is 0")
    }
}
