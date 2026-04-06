package net.sovereign.components.citizens

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CitizensBridgeManagerTest {

    private lateinit var mappings: CitizensBridgeMapping

    @BeforeEach
    fun setup() {
        mappings = CitizensBridgeMapping()
    }

    @Test
    fun `link stores npc id to catalog id mapping`() {
        mappings.link(42, "weapons")
        assertEquals("weapons", mappings.resolve(42))
    }

    @Test
    fun `resolve returns null for unlinked npc`() {
        assertNull(mappings.resolve(999))
    }

    @Test
    fun `unlink removes mapping`() {
        mappings.link(10, "armor")
        mappings.unlink(10)
        assertNull(mappings.resolve(10))
    }

    @Test
    fun `unlink on non-existent npc does not throw`() {
        assertDoesNotThrow { mappings.unlink(777) }
    }

    @Test
    fun `link overwrites previous mapping for same npc`() {
        mappings.link(5, "food")
        mappings.link(5, "potions")
        assertEquals("potions", mappings.resolve(5))
    }

    @Test
    fun `isLinked returns true for linked npc`() {
        mappings.link(1, "tools")
        assertTrue(mappings.isLinked(1))
    }

    @Test
    fun `isLinked returns false for unlinked npc`() {
        assertFalse(mappings.isLinked(99))
    }

    @Test
    fun `allMappings returns all entries`() {
        mappings.link(1, "a")
        mappings.link(2, "b")
        mappings.link(3, "c")
        val all = mappings.allMappings()
        assertEquals(3, all.size)
        assertEquals("a", all[1])
        assertEquals("b", all[2])
        assertEquals("c", all[3])
    }

    @Test
    fun `allMappings returns empty map when nothing linked`() {
        assertTrue(mappings.allMappings().isEmpty())
    }

    @Test
    fun `multiple links and unlinks produce correct state`() {
        mappings.link(1, "alpha")
        mappings.link(2, "beta")
        mappings.link(3, "gamma")
        mappings.unlink(2)
        mappings.link(1, "delta")

        assertEquals("delta", mappings.resolve(1))
        assertNull(mappings.resolve(2))
        assertEquals("gamma", mappings.resolve(3))
        assertEquals(2, mappings.allMappings().size)
    }
}
