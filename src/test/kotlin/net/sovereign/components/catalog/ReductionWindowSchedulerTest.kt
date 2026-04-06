package net.sovereign.components.catalog

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReductionWindowSchedulerTest {

    @Test
    fun `parseWindowTime returns null for null input`() {
        assertNull(ReductionWindowScheduler.parseWindowTime(null))
    }

    @Test
    fun `parseWindowTime returns null for blank input`() {
        assertNull(ReductionWindowScheduler.parseWindowTime(""))
        assertNull(ReductionWindowScheduler.parseWindowTime("   "))
    }

    @Test
    fun `parseWindowTime returns null for malformed input`() {
        assertNull(ReductionWindowScheduler.parseWindowTime("not-a-date"))
        assertNull(ReductionWindowScheduler.parseWindowTime("yesterday"))
        assertNull(ReductionWindowScheduler.parseWindowTime("2026-13-45T99:99:99"))
    }

    @Test
    fun `parseWindowTime parses ISO-8601 instant string`() {
        val result = ReductionWindowScheduler.parseWindowTime("2026-03-15T10:00:00Z")
        assertNotNull(result)
        assertEquals(Instant.parse("2026-03-15T10:00:00Z"), result)
    }

    @Test
    fun `parseWindowTime parses ISO-8601 local datetime by assuming UTC`() {
        val result = ReductionWindowScheduler.parseWindowTime("2026-03-15T10:00:00")
        assertNotNull(result)
        assertEquals(Instant.parse("2026-03-15T10:00:00Z"), result)
    }

    @Test
    fun `parseWindowTime parses epoch millis as string`() {
        val epochMillis = Instant.parse("2026-03-15T10:00:00Z").toEpochMilli()
        val result = ReductionWindowScheduler.parseWindowTime(epochMillis.toString())
        assertNotNull(result)
        assertEquals(Instant.parse("2026-03-15T10:00:00Z"), result)
    }

    @Test
    fun `evaluateWindow returns null when both start and end are null`() {
        assertNull(ReductionWindowScheduler.evaluateWindow(null, null, Instant.now()))
    }

    @Test
    fun `evaluateWindow returns true when now is between start and end`() {
        val now = Instant.now()
        val start = now.minus(1, ChronoUnit.HOURS)
        val end = now.plus(1, ChronoUnit.HOURS)
        assertEquals(true, ReductionWindowScheduler.evaluateWindow(start, end, now))
    }

    @Test
    fun `evaluateWindow returns false when now is before start`() {
        val now = Instant.now()
        val start = now.plus(1, ChronoUnit.HOURS)
        val end = now.plus(2, ChronoUnit.HOURS)
        assertEquals(false, ReductionWindowScheduler.evaluateWindow(start, end, now))
    }

    @Test
    fun `evaluateWindow returns false when now is past end`() {
        val now = Instant.now()
        val start = now.minus(3, ChronoUnit.HOURS)
        val end = now.minus(1, ChronoUnit.HOURS)
        assertEquals(false, ReductionWindowScheduler.evaluateWindow(start, end, now))
    }

    @Test
    fun `evaluateWindow with start-only activates once started`() {
        val now = Instant.now()
        val start = now.minus(1, ChronoUnit.HOURS)
        assertEquals(true, ReductionWindowScheduler.evaluateWindow(start, null, now))
    }

    @Test
    fun `evaluateWindow with start-only stays inactive before start`() {
        val now = Instant.now()
        val start = now.plus(1, ChronoUnit.HOURS)
        assertEquals(false, ReductionWindowScheduler.evaluateWindow(start, null, now))
    }

    @Test
    fun `evaluateWindow with end-only stays active before end`() {
        val now = Instant.now()
        val end = now.plus(1, ChronoUnit.HOURS)
        assertEquals(true, ReductionWindowScheduler.evaluateWindow(null, end, now))
    }

    @Test
    fun `evaluateWindow with end-only deactivates after end`() {
        val now = Instant.now()
        val end = now.minus(1, ChronoUnit.HOURS)
        assertEquals(false, ReductionWindowScheduler.evaluateWindow(null, end, now))
    }

    @Test
    fun `evaluateWindow returns true at exact start boundary`() {
        val now = Instant.now()
        assertEquals(true, ReductionWindowScheduler.evaluateWindow(now, now.plus(1, ChronoUnit.HOURS), now))
    }

    @Test
    fun `evaluateWindow returns false at exact end boundary`() {
        val now = Instant.now()
        assertEquals(false, ReductionWindowScheduler.evaluateWindow(now.minus(1, ChronoUnit.HOURS), now, now))
    }

    @Test
    fun `shouldToggle returns null for listing with no window fields set`() {
        val listing = Listing(uid = 1, type = ListingType.TRADABLE)
        assertNull(ReductionWindowScheduler.shouldToggle(listing, Instant.now()))
    }

    @Test
    fun `shouldToggle returns true for listing inside active window`() {
        val now = Instant.now()
        val listing = Listing(
            uid = 1, type = ListingType.TRADABLE,
            reductionWindowStart = now.minus(1, ChronoUnit.HOURS).toString(),
            reductionWindowEnd = now.plus(1, ChronoUnit.HOURS).toString()
        )
        assertEquals(true, ReductionWindowScheduler.shouldToggle(listing, now))
    }

    @Test
    fun `shouldToggle returns false for listing outside window`() {
        val now = Instant.now()
        val listing = Listing(
            uid = 1, type = ListingType.TRADABLE,
            reductionWindowStart = now.plus(1, ChronoUnit.HOURS).toString(),
            reductionWindowEnd = now.plus(2, ChronoUnit.HOURS).toString()
        )
        assertEquals(false, ReductionWindowScheduler.shouldToggle(listing, now))
    }

    @Test
    fun `shouldToggle works with catalog fields`() {
        val now = Instant.now()
        val catalog = Catalog(
            identifier = "test",
            reductionWindowStart = now.minus(1, ChronoUnit.HOURS).toString(),
            reductionWindowEnd = now.plus(1, ChronoUnit.HOURS).toString()
        )
        assertEquals(true, ReductionWindowScheduler.shouldToggleCatalog(catalog, now))
    }

    @Test
    fun `shouldToggle catalog returns null when no windows set`() {
        val catalog = Catalog(identifier = "test")
        assertNull(ReductionWindowScheduler.shouldToggleCatalog(catalog, Instant.now()))
    }
}
