package net.sovereign.components.catalog

data class Catalog(
    var identifier: String,
    var displayTitle: String = identifier.replaceFirstChar { it.uppercase() },
    var authorization: String? = null,

    val sections: MutableList<CatalogSection> = mutableListOf(),

    var acquireEnabled: Boolean = true,
    var liquidateEnabled: Boolean = false,
    var barterEnabled: Boolean = false,

    var reductionActive: Boolean = false,
    var reductionPercent: Double = 0.0,
    var reductionWindowStart: String? = null,
    var reductionWindowDurationSec: Int = 0,
    var reductionWindowEnd: String? = null
) {

    val defaultTransactionMode: TransactionMode
        get() = when {
            acquireEnabled -> TransactionMode.ACQUIRE
            liquidateEnabled -> TransactionMode.LIQUIDATE
            barterEnabled -> TransactionMode.BARTER
            else -> TransactionMode.ACQUIRE
        }

    val enabledModes: List<TransactionMode>
        get() = buildList {
            if (acquireEnabled) add(TransactionMode.ACQUIRE)
            if (liquidateEnabled) add(TransactionMode.LIQUIDATE)
            if (barterEnabled) add(TransactionMode.BARTER)
        }

    fun cycleMode(current: TransactionMode): TransactionMode {
        val modes = enabledModes
        if (modes.size <= 1) return current
        val currentIndex = modes.indexOf(current)
        return modes[(currentIndex + 1) % modes.size]
    }

    fun isModeEnabled(mode: TransactionMode): Boolean = when (mode) {
        TransactionMode.ACQUIRE -> acquireEnabled
        TransactionMode.LIQUIDATE -> liquidateEnabled
        TransactionMode.BARTER -> barterEnabled
    }
}
