package net.sovereign.components.audit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class TransactionLoggerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var logger: TransactionLogger

    @BeforeEach
    fun setUp() {
        logger = TransactionLogger(tempDir)
    }

    @Test
    fun `formatEntry produces pipe-delimited line with all fields`() {
        val entry = TransactionEntry(
            timestamp = Instant.parse("2026-03-13T00:00:00Z"),
            playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            playerName = "TestPlayer",
            catalog = "weapons",
            listingUid = 42,
            listingLabel = "Diamond Sword",
            mode = "ACQUIRE",
            quantity = 5,
            amount = 250.0,
            result = "SUCCESS"
        )

        val line = TransactionLogger.formatEntry(entry)
        val parts = line.split("|")

        assertEquals(10, parts.size, "Should have 10 pipe-delimited fields")
        assertEquals("2026-03-13T00:00:00Z", parts[0])
        assertEquals("00000000-0000-0000-0000-000000000001", parts[1])
        assertEquals("TestPlayer", parts[2])
        assertEquals("weapons", parts[3])
        assertEquals("42", parts[4])
        assertEquals("Diamond Sword", parts[5])
        assertEquals("ACQUIRE", parts[6])
        assertEquals("5", parts[7])
        assertEquals("250.0", parts[8])
        assertEquals("SUCCESS", parts[9])
    }

    @Test
    fun `formatEntry escapes pipe characters in label`() {
        val entry = TransactionEntry(
            timestamp = Instant.parse("2026-03-13T00:00:00Z"),
            playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            playerName = "TestPlayer",
            catalog = "shop",
            listingUid = 1,
            listingLabel = "Item|With|Pipes",
            mode = "ACQUIRE",
            quantity = 1,
            amount = 10.0,
            result = "SUCCESS"
        )

        val line = TransactionLogger.formatEntry(entry)
        assertFalse(line.split("|").size > 10, "Pipe chars in label must be sanitized")
    }

    @Test
    fun `formatEntry handles null label gracefully`() {
        val entry = TransactionEntry(
            timestamp = Instant.parse("2026-03-13T00:00:00Z"),
            playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            playerName = "TestPlayer",
            catalog = "shop",
            listingUid = 7,
            listingLabel = null,
            mode = "LIQUIDATE",
            quantity = 3,
            amount = 75.0,
            result = "SUCCESS"
        )

        val line = TransactionLogger.formatEntry(entry)
        val parts = line.split("|")
        assertEquals(10, parts.size)
        assertEquals("", parts[5], "Null label should produce empty string")
    }

    @Test
    fun `log creates file and writes header on first entry`() {
        val entry = createTestEntry()
        logger.log(entry)

        val logFile = File(tempDir, "transactions.log")
        assertTrue(logFile.exists(), "Log file should be created")

        val lines = logFile.readLines()
        assertTrue(lines.size >= 2, "Should have header + entry")
        assertTrue(lines[0].startsWith("#"), "First line should be a header comment")
    }

    @Test
    fun `log appends entries without duplicating header`() {
        val entry1 = createTestEntry(playerName = "Alice")
        val entry2 = createTestEntry(playerName = "Bob")

        logger.log(entry1)
        logger.log(entry2)

        val logFile = File(tempDir, "transactions.log")
        val lines = logFile.readLines()

        val headerCount = lines.count { it.startsWith("#") }
        assertEquals(1, headerCount, "Header should appear exactly once")

        val dataLines = lines.filter { !it.startsWith("#") && it.isNotBlank() }
        assertEquals(2, dataLines.size, "Should have exactly 2 data entries")
    }

    @Test
    fun `log writes to correct file path`() {
        logger.log(createTestEntry())

        val logFile = File(tempDir, "transactions.log")
        assertTrue(logFile.exists())
        assertTrue(logFile.length() > 0, "File should have content")
    }

    @Test
    fun `multiple entries maintain correct order`() {
        val entry1 = createTestEntry(playerName = "First", amount = 100.0)
        val entry2 = createTestEntry(playerName = "Second", amount = 200.0)
        val entry3 = createTestEntry(playerName = "Third", amount = 300.0)

        logger.log(entry1)
        logger.log(entry2)
        logger.log(entry3)

        val logFile = File(tempDir, "transactions.log")
        val dataLines = logFile.readLines().filter { !it.startsWith("#") && it.isNotBlank() }

        assertEquals(3, dataLines.size)
        assertTrue(dataLines[0].contains("First"))
        assertTrue(dataLines[1].contains("Second"))
        assertTrue(dataLines[2].contains("Third"))
    }

    @Test
    fun `log handles zero amount`() {
        val entry = createTestEntry(amount = 0.0)
        logger.log(entry)

        val logFile = File(tempDir, "transactions.log")
        val dataLines = logFile.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
        assertEquals(1, dataLines.size)
        assertTrue(dataLines[0].contains("0.0"))
    }

    @Test
    fun `log handles barter mode with descriptive amount`() {
        val entry = createTestEntry(mode = "BARTER", amount = 0.0, result = "SUCCESS")
        logger.log(entry)

        val logFile = File(tempDir, "transactions.log")
        val dataLines = logFile.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
        assertEquals(1, dataLines.size)
        assertTrue(dataLines[0].contains("BARTER"))
    }

    @Test
    fun `log handles directive mode`() {
        val entry = createTestEntry(mode = "DIRECTIVE", result = "SUCCESS")
        logger.log(entry)

        val logFile = File(tempDir, "transactions.log")
        val dataLines = logFile.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
        assertTrue(dataLines[0].contains("DIRECTIVE"))
    }

    @Test
    fun `log handles failed transactions`() {
        val entry = createTestEntry(result = "INSUFFICIENT_FUNDS")
        logger.log(entry)

        val logFile = File(tempDir, "transactions.log")
        val dataLines = logFile.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
        assertTrue(dataLines[0].contains("INSUFFICIENT_FUNDS"))
    }

    @Test
    fun `parseEntry round-trips correctly`() {
        val original = createTestEntry()
        val line = TransactionLogger.formatEntry(original)
        val parsed = TransactionLogger.parseEntry(line)

        assertNotNull(parsed)
        assertEquals(original.playerUuid, parsed!!.playerUuid)
        assertEquals(original.playerName, parsed.playerName)
        assertEquals(original.catalog, parsed.catalog)
        assertEquals(original.listingUid, parsed.listingUid)
        assertEquals(original.mode, parsed.mode)
        assertEquals(original.quantity, parsed.quantity)
        assertEquals(original.amount, parsed.amount, 0.001)
        assertEquals(original.result, parsed.result)
    }

    @Test
    fun `parseEntry returns null for malformed line`() {
        val parsed = TransactionLogger.parseEntry("this|is|not|enough|fields")
        assertNull(parsed, "Should return null for lines with wrong field count")
    }

    @Test
    fun `parseEntry returns null for header line`() {
        val parsed = TransactionLogger.parseEntry("# TIMESTAMP|PLAYER_UUID|...")
        assertNull(parsed, "Should return null for comment lines")
    }

    private fun createTestEntry(
        playerName: String = "TestPlayer",
        amount: Double = 100.0,
        mode: String = "ACQUIRE",
        result: String = "SUCCESS"
    ) = TransactionEntry(
        timestamp = Instant.now(),
        playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        playerName = playerName,
        catalog = "test-catalog",
        listingUid = 1,
        listingLabel = "Test Item",
        mode = mode,
        quantity = 1,
        amount = amount,
        result = result
    )
}
