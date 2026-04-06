package net.sovereign.core.command

import net.sovereign.core.SovereignCore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class SovereignCommandExecutorTest {

    @Test
    fun `buildCommandTree uses traders as root literal`() {
        val plugin = mock<SovereignCore>()
        val executor = SovereignCommandExecutor(plugin)

        val root = executor.buildCommandTree()

        assertEquals("traders", root.literal)
    }
}