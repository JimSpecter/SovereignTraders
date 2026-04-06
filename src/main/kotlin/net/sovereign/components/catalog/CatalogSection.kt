package net.sovereign.components.catalog

data class CatalogSection(
    var identifier: String,
    val rowCount: Int = 4,
    var authorization: String? = null,

    val acquireListings: Array<Listing?> = arrayOfNulls(rowCount * 9),
    val liquidateListings: Array<Listing?> = arrayOfNulls(rowCount * 9),
    val barterListings: Array<Listing?> = arrayOfNulls(rowCount * 9)
) {

    fun listingsForMode(mode: TransactionMode): Array<Listing?> = when (mode) {
        TransactionMode.ACQUIRE -> acquireListings
        TransactionMode.LIQUIDATE -> liquidateListings
        TransactionMode.BARTER -> barterListings
    }

    val capacity: Int get() = rowCount * 9

    fun occupiedSlots(mode: TransactionMode): Int {
        return listingsForMode(mode).count { it != null }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CatalogSection) return false
        return identifier == other.identifier
    }

    override fun hashCode(): Int = identifier.hashCode()
}
