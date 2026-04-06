package net.sovereign.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object HologramRenderer {

    private val nextEntityId = AtomicInteger(2_100_000_000)

    private const val LINE_SPACING = 0.3

    private const val BASE_OFFSET = 2.1

    private val mm = MiniMessage.miniMessage()

    fun allocate(assembly: VendorAssembly) {
        if (assembly.hologramEntityIds.size != assembly.hologramLines.size) {
            assembly.hologramEntityIds.clear()
            repeat(assembly.hologramLines.size) {
                assembly.hologramEntityIds.add(nextEntityId.getAndIncrement())
            }
        }
    }

    fun showTo(assembly: VendorAssembly, location: Location, viewer: Player) {
        val packetManager = PacketEvents.getAPI().playerManager

        if (assembly.hologramEntityIds.isNotEmpty()) {
            val destroyPacket = WrapperPlayServerDestroyEntities(*assembly.hologramEntityIds.toIntArray())
            packetManager.sendPacket(viewer, destroyPacket)
        }

        val lineCount = assembly.hologramEntityIds.size
        for ((index, entityId) in assembly.hologramEntityIds.withIndex()) {
            val lineText = assembly.hologramLines.getOrNull(index) ?: continue

            val reverseIndex = lineCount - 1 - index
            val y = location.y + BASE_OFFSET + (reverseIndex * LINE_SPACING)

            val uuid = UUID.randomUUID()
            val spawnPacket = WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.ARMOR_STAND,
                Vector3d(location.x, y, location.z),
                0f,
                0f,
                0f,
                0,
                Optional.empty()
            )
            packetManager.sendPacket(viewer, spawnPacket)

            val component = mm.deserialize(lineText)

            val metadata = listOf(

                EntityData(0, EntityDataTypes.BYTE, (0x20).toByte()),

                EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(component)),

                EntityData(3, EntityDataTypes.BOOLEAN, true),

                EntityData(15, EntityDataTypes.BYTE, (0x10).toByte())
            )
            val metaPacket = WrapperPlayServerEntityMetadata(entityId, metadata)
            packetManager.sendPacket(viewer, metaPacket)
        }
    }

    fun despawnAll(assembly: VendorAssembly) {
        if (assembly.hologramEntityIds.isEmpty()) return
        val destroyPacket = WrapperPlayServerDestroyEntities(*assembly.hologramEntityIds.toIntArray())
        val packetManager = PacketEvents.getAPI().playerManager
        for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
            packetManager.sendPacket(player, destroyPacket)
        }

    }

    fun despawnFor(assembly: VendorAssembly, viewer: Player) {
        if (assembly.hologramEntityIds.isEmpty()) return
        val destroyPacket = WrapperPlayServerDestroyEntities(*assembly.hologramEntityIds.toIntArray())
        PacketEvents.getAPI().playerManager.sendPacket(viewer, destroyPacket)
    }
}
