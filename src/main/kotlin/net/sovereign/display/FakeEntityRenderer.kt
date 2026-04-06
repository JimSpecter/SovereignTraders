package net.sovereign.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FakeEntityRenderer {

    private val entityUuids = ConcurrentHashMap<Int, UUID>()

    fun showTo(entityId: Int, entityType: EntityType, location: Location, viewer: Player) {
        require(entityType != EntityTypes.PLAYER) {
            "Use FakePlayerRenderer for PLAYER entities"
        }

        val uuid = entityUuids.getOrPut(entityId) { UUID.randomUUID() }
        val spawnPacket = WrapperPlayServerSpawnEntity(
            entityId,
            Optional.of(uuid),
            entityType,
            Vector3d(location.x, location.y, location.z),
            location.pitch,
            location.yaw,
            location.yaw,
            0,
            Optional.empty()
        )
        PacketEvents.getAPI().playerManager.sendPacket(viewer, spawnPacket)
    }

    fun broadcastSpawn(entityId: Int, entityType: EntityType, location: Location) {
        for (player in Bukkit.getOnlinePlayers()) {
            showTo(entityId, entityType, location, player)
        }
    }

    fun despawnFor(entityId: Int, viewer: Player) {
        val destroyPacket = WrapperPlayServerDestroyEntities(entityId)
        PacketEvents.getAPI().playerManager.sendPacket(viewer, destroyPacket)
    }

    fun despawnAll(entityId: Int) {
        entityUuids.remove(entityId)
        val destroyPacket = WrapperPlayServerDestroyEntities(entityId)
        val packetManager = PacketEvents.getAPI().playerManager
        for (player in Bukkit.getOnlinePlayers()) {
            packetManager.sendPacket(player, destroyPacket)
        }
    }
}
