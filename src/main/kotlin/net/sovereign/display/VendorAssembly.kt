package net.sovereign.display

import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class VendorAssembly(

    val interactionId: UUID,

    var fakeEntityId: Int,

    var entityType: EntityType = EntityTypes.PLAYER,

    var skinName: String? = null,

    val equipment: MutableMap<EquipmentSlot, ItemStack?> = mutableMapOf(),

    val hologramLines: MutableList<String> = mutableListOf(),

    val hologramEntityIds: MutableList<Int> = mutableListOf(),

    var lookCloseRadius: Double = 5.0,

    var interactionWidth: Float = 0.8f,

    var interactionHeight: Float = 1.8f,

    var linkedCatalog: String? = null
) {
    
    var cachedLocation: Location? = null
}
