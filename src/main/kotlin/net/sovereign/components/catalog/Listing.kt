package net.sovereign.components.catalog

data class Listing(
    val uid: Int,
    val type: ListingType,

    var label: String? = null,
    var annotations: MutableList<String> = mutableListOf(),
    var showAnnotations: Boolean = true,
    var materialId: String = "STONE",
    var stackQuantity: Int = 1,
    var itemStack: org.bukkit.inventory.ItemStack? = null,

    var acquireCost: Double = 0.0,
    var liquidateReward: Double = 0.0,
    var showCost: Boolean = true,

    var reductionActive: Boolean = false,
    var reductionPercent: Double = 0.0,
    var reductionOverrideCost: Double = 0.0,
    var reductionWindowStart: String? = null,
    var reductionWindowDurationSec: Int = 0,
    var reductionWindowEnd: String? = null,
    var showReductionCost: Boolean = false,
    var showReductionStart: Boolean = false,
    var showReductionDuration: Boolean = false,
    var showReductionEnd: Boolean = false,

    var quotaLimit: Int = 0,
    var quotaResetIntervalSec: Int = 0,
    var showQuotaProgress: Boolean = false,
    var showQuotaTimer: Boolean = false,

    var authorization: String? = null,

    var dropOnOverflow: Boolean = false,
    var liquidatePartialAllowed: Boolean = false,
    var broadcastOnTransaction: Boolean = false,
    var broadcastTemplate: String = "",

    var barterReceivables: MutableList<BarterComponent> = mutableListOf(),
    var barterRequirements: MutableList<BarterComponent> = mutableListOf(),

    var directives: MutableList<Directive> = mutableListOf(),
    var directiveRunMode: DirectiveRunMode = DirectiveRunMode.EXECUTE_ONLY
)

enum class ListingType(val configKey: String) {
    TRADABLE("tradable"),
    DIRECTIVE("directive"),
    STATIC("static"),
    BARTER("barter");

    companion object {
        fun fromConfigKey(key: String): ListingType {
            return entries.firstOrNull { it.configKey.equals(key, ignoreCase = true) } ?: STATIC
        }
    }
}

data class BarterComponent(
    val materialId: String,
    val quantity: Int,
    val label: String? = null
)

data class Directive(
    val command: String,
    val executor: DirectiveExecutor = DirectiveExecutor.CONSOLE
)

enum class DirectiveExecutor {
    CONSOLE, PLAYER;

    companion object {
        fun fromString(value: String): DirectiveExecutor {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CONSOLE
        }
    }
}

enum class DirectiveRunMode {
    EXECUTE_ONLY,
    ACQUIRE_AND_EXECUTE,
    ACQUIRE_AND_RETAIN;

    companion object {
        fun fromString(value: String): DirectiveRunMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXECUTE_ONLY
        }
    }
}
