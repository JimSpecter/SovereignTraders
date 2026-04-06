package net.sovereign.components.audit

import java.io.File
import java.time.Instant
import java.util.UUID

data class TransactionEntry(
    val timestamp: Instant,
    val playerUuid: UUID,
    val playerName: String,
    val catalog: String,
    val listingUid: Int,
    val listingLabel: String?,
    val mode: String,        // ACQUIRE | LIQUIDATE | BARTER | DIRECTIVE
    val quantity: Int,
    val amount: Double,      // currency moved; 0.0 for barter
    val result: String       // SUCCESS | INSUFFICIENT_FUNDS | INSUFFICIENT_MATERIALS | INVENTORY_FULL | LIQUIDATION_FAILED
)

class TransactionLogger(private val dataFolder: File) {

    private val logFile: File = File(dataFolder, "transactions.log")
    private val lock = Any()
    private var headerWritten: Boolean = logFile.exists() && logFile.length() > 0

    
    fun log(entry: TransactionEntry) {
        synchronized(lock) {
            if (!logFile.parentFile.exists()) {
                logFile.parentFile.mkdirs()
            }

            logFile.appendText(buildString {
                if (!headerWritten) {
                    appendLine(HEADER)
                    headerWritten = true
                }
                appendLine(formatEntry(entry))
            })
        }
    }

    companion object {

        
        const val HEADER = "# TIMESTAMP|PLAYER_UUID|PLAYER_NAME|CATALOG|LISTING_UID|LISTING_LABEL|MODE|QUANTITY|AMOUNT|RESULT"

        private const val DELIMITER = "|"

        
        fun formatEntry(entry: TransactionEntry): String {
            fun sanitize(value: String): String = value.replace(DELIMITER, "/")

            return listOf(
                entry.timestamp.toString(),
                entry.playerUuid.toString(),
                sanitize(entry.playerName),
                sanitize(entry.catalog),
                entry.listingUid.toString(),
                sanitize(entry.listingLabel ?: ""),
                entry.mode,
                entry.quantity.toString(),
                entry.amount.toString(),
                entry.result
            ).joinToString(DELIMITER)
        }

        
        fun parseEntry(line: String): TransactionEntry? {
            if (line.isBlank() || line.startsWith("#")) return null
            val parts = line.split(DELIMITER)
            if (parts.size != 10) return null

            return try {
                TransactionEntry(
                    timestamp = Instant.parse(parts[0]),
                    playerUuid = UUID.fromString(parts[1]),
                    playerName = parts[2],
                    catalog = parts[3],
                    listingUid = parts[4].toInt(),
                    listingLabel = parts[5].ifEmpty { null },
                    mode = parts[6],
                    quantity = parts[7].toInt(),
                    amount = parts[8].toDouble(),
                    result = parts[9]
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
