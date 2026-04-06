package net.sovereign.core.scheduler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SovereignSchedulerTest {

    @Test
    fun `test it delegates to Bukkit scheduler when not Folia`() {
        SovereignScheduler.IS_FOLIA = false
        val scheduler = SovereignScheduler.determineScheduler()
        assertTrue(scheduler is BukkitTaskScheduler)
    }

    @Test
    fun `test it delegates to Folia scheduler when Folia`() {
        SovereignScheduler.IS_FOLIA = true
        val scheduler = SovereignScheduler.determineScheduler()
        assertTrue(scheduler is FoliaTaskScheduler)
    }
}
