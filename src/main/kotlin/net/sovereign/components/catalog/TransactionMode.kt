package net.sovereign.components.catalog

enum class TransactionMode(val configKey: String, val displayName: String) {
    ACQUIRE("acquire", "ᴀᴄǫᴜɪʀᴇ"),
    LIQUIDATE("liquidate", "ʟɪǫᴜɪᴅᴀᴛᴇ"),
    BARTER("barter", "ʙᴀʀᴛᴇʀ");

    companion object {
        fun fromConfigKey(key: String): TransactionMode? {
            return entries.firstOrNull { it.configKey.equals(key, ignoreCase = true) }
        }
    }
}
