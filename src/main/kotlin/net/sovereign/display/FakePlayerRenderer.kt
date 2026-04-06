package net.sovereign.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.sovereign.core.SovereignCore
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object FakePlayerRenderer {

    
    private const val SPAWN_DELAY_TICKS = 10L

    
    private const val TAB_REMOVE_DELAY_TICKS = 40L

    private val nextEntityId = AtomicInteger(2_000_000_000)

    private val activeProfiles = ConcurrentHashMap<Int, PlayerProfileData>()

    data class PlayerProfileData(
        val entityId: Int,
        val uuid: UUID,
        val profile: UserProfile,
        val location: Location
    )

    fun spawn(
        location: Location,
        skinName: String?,
        interactionId: UUID
    ): Int {
        val entityId = nextEntityId.getAndIncrement()
        val npcUuid = UUID.randomUUID()

        val textures = if (skinName != null) fetchSkinTextures(skinName) else null
        val profile = UserProfile(npcUuid, "\u00A7r")
        if (textures != null) {
            profile.textureProperties = listOf(textures)
        }

        val data = PlayerProfileData(entityId, npcUuid, profile, location.clone())
        activeProfiles[entityId] = data

        for (player in Bukkit.getOnlinePlayers()) {
            sendSpawnPackets(player, data)
        }

        return entityId
    }

    fun despawn(entityId: Int) {
        val data = activeProfiles.remove(entityId) ?: return
        val destroyPacket = WrapperPlayServerDestroyEntities(data.entityId)
        val removePacket = WrapperPlayServerPlayerInfoRemove(data.uuid)

        val teamName = "sv-${data.entityId}"
        val removeTeamPacket = WrapperPlayServerTeams(
            teamName,
            WrapperPlayServerTeams.TeamMode.REMOVE,
            java.util.Optional.empty(),
            emptyList<String>()
        )

        for (player in Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().playerManager.sendPacket(player, destroyPacket)
            PacketEvents.getAPI().playerManager.sendPacket(player, removePacket)
            PacketEvents.getAPI().playerManager.sendPacket(player, removeTeamPacket)
        }
    }

    fun showAllTo(player: Player) {
        for (data in activeProfiles.values) {
            sendSpawnPackets(player, data)
        }
    }

    fun showTo(entityId: Int, player: Player) {
        val data = activeProfiles[entityId] ?: return
        sendSpawnPackets(player, data)
    }

    fun getProfileData(entityId: Int): PlayerProfileData? = activeProfiles[entityId]

    private fun sendSpawnPackets(viewer: Player, data: PlayerProfileData) {
        val packetManager = PacketEvents.getAPI().playerManager

        val infoEntry = WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
            data.profile,
            false,
            0,
            GameMode.CREATIVE,
            null,
            null
        )
        val infoPacket = WrapperPlayServerPlayerInfoUpdate(
            java.util.EnumSet.of(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED
            ),
            infoEntry
        )
        packetManager.sendPacket(viewer, infoPacket)

        net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(SovereignCore.instance.plugin, Runnable {
            val loc = data.location
            val spawnPacket = WrapperPlayServerSpawnEntity(
                data.entityId,
                java.util.Optional.of(data.uuid),
                EntityTypes.PLAYER,
                Vector3d(loc.x, loc.y, loc.z),
                loc.pitch,
                loc.yaw,
                loc.yaw,
                0,
                java.util.Optional.empty()
            )
            packetManager.sendPacket(viewer, spawnPacket)

            val skinLayerMeta = listOf(
                EntityData(17, EntityDataTypes.BYTE, 0x7F.toByte())
            )
            val metaPacket = WrapperPlayServerEntityMetadata(data.entityId, skinLayerMeta)
            packetManager.sendPacket(viewer, metaPacket)

            val teamName = "sv-${data.entityId}"
            val teamInfo = WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(),
                Component.empty(),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                NamedTextColor.WHITE,
                WrapperPlayServerTeams.OptionData.NONE
            )
            val teamPacket = WrapperPlayServerTeams(
                teamName,
                WrapperPlayServerTeams.TeamMode.CREATE,
                teamInfo,
                data.profile.name
            )
            packetManager.sendPacket(viewer, teamPacket)

            net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(SovereignCore.instance.plugin, Runnable {
                val removeInfoPacket = WrapperPlayServerPlayerInfoRemove(data.uuid)
                packetManager.sendPacket(viewer, removeInfoPacket)
            }, TAB_REMOVE_DELAY_TICKS)
        }, SPAWN_DELAY_TICKS)
    }

    private fun fetchSkinTextures(username: String): TextureProperty? {
        return try {
            fetchViaPaperApi(username) ?: fetchViaMojangHttp(username)
        } catch (e: Exception) {
            SovereignCore.broadcast("<red>Failed to fetch skin for '$username': ${e.message}")
            null
        }
    }

    private fun fetchViaPaperApi(username: String): TextureProperty? {
        return try {
            val profile = Bukkit.createProfile(username)
            profile.complete(true)
            val textureProp = profile.properties.find { it.name == "textures" } ?: return null
            TextureProperty("textures", textureProp.value, textureProp.signature)
        } catch (_: NoSuchMethodError) {
            null
        }
    }

    private fun fetchViaMojangHttp(username: String): TextureProperty? {

        val uuidUrl = java.net.URI("https://api.mojang.com/users/profiles/minecraft/$username").toURL()
        val uuidJson = uuidUrl.readText()
        val uuidMatch = Regex("\"id\"\\s*:\\s*\"([a-f0-9]+)\"").find(uuidJson) ?: return null
        val rawUuid = uuidMatch.groupValues[1]

        val profileUrl = java.net.URI("https://sessionserver.mojang.com/session/minecraft/profile/$rawUuid?unsigned=false").toURL()
        val profileJson = profileUrl.readText()

        val valueMatch = Regex("\"value\"\\s*:\\s*\"([^\"]+)\"").find(profileJson) ?: return null
        val sigMatch = Regex("\"signature\"\\s*:\\s*\"([^\"]+)\"").find(profileJson)

        return TextureProperty("textures", valueMatch.groupValues[1], sigMatch?.groupValues?.get(1))
    }
}
