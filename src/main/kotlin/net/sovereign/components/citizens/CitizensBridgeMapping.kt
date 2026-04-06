package net.sovereign.components.citizens

class CitizensBridgeMapping {

    private val npcToCatalog = mutableMapOf<Int, String>()

    fun link(npcId: Int, catalogId: String) {
        npcToCatalog[npcId] = catalogId
    }

    fun unlink(npcId: Int) {
        npcToCatalog.remove(npcId)
    }

    fun resolve(npcId: Int): String? = npcToCatalog[npcId]

    fun isLinked(npcId: Int): Boolean = npcToCatalog.containsKey(npcId)

    fun allMappings(): Map<Int, String> = npcToCatalog.toMap()

    fun clear() {
        npcToCatalog.clear()
    }

    fun loadFrom(map: Map<Int, String>) {
        npcToCatalog.clear()
        npcToCatalog.putAll(map)
    }
}
