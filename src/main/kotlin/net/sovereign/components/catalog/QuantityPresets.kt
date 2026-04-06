package net.sovereign.components.catalog

object QuantityPresets {

    private val TIERS = listOf(1, 8, 16, 32, 64)

    
    fun resolve(stackQuantity: Int, maxStackSize: Int): List<Int> {
        if (maxStackSize <= 1) return listOf(1)

        val options = mutableSetOf<Int>()
        options.add(1)
        options.add(stackQuantity.coerceIn(1, maxStackSize))

        for (tier in TIERS) {
            if (tier in 1..maxStackSize) {
                options.add(tier)
            }
        }

        return options.sorted()
    }
}
