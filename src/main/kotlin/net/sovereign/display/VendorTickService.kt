package net.sovereign.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import net.sovereign.core.SovereignCore
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.math.atan2
import kotlin.math.sqrt

class VendorTickService(private val plugin: SovereignCore) {

    private var task: Any? = null

    fun start() {
        if (task != null) return

        task = net.sovereign.core.scheduler.SovereignScheduler.runTaskTimer(plugin.plugin, Runnable {
            tick()
        }, 5L, 5L)
    }

    fun stop() {
        net.sovereign.core.scheduler.SovereignScheduler.cancelTask(task)
        task = null
    }

    private fun tick() {
        val vendors = plugin.vendorRegistry.allAssemblies()
        if (vendors.isEmpty()) return

        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return

        val packetManager = PacketEvents.getAPI().playerManager

        for (assembly in vendors) {
            if (assembly.lookCloseRadius <= 0.0) continue

            val vendorLoc = assembly.cachedLocation ?: continue
            val radiusSq = assembly.lookCloseRadius * assembly.lookCloseRadius

            for (player in onlinePlayers) {
                if (player.world != vendorLoc.world) continue

                val playerLoc = player.eyeLocation
                val distSq = vendorLoc.distanceSquared(playerLoc)

                val (yaw, pitch) = if (distSq <= radiusSq) {
                    calculateLookAngles(vendorLoc, playerLoc)
                } else {

                    vendorLoc.yaw to vendorLoc.pitch
                }

                val rotationPacket = WrapperPlayServerEntityRotation(
                    assembly.fakeEntityId, yaw, pitch, true
                )
                packetManager.sendPacket(player, rotationPacket)

                val headLookPacket = WrapperPlayServerEntityHeadLook(
                    assembly.fakeEntityId, yaw
                )
                packetManager.sendPacket(player, headLookPacket)
            }
        }
    }

    companion object {

        private const val EYE_HEIGHT = 1.62
    }

    private fun calculateLookAngles(from: Location, to: Location): Pair<Float, Float> {
        val dx = to.x - from.x
        val dy = to.y - (from.y + EYE_HEIGHT)
        val dz = to.z - from.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(-atan2(dy, horizontalDist)).toFloat()

        return yaw to pitch
    }
}
