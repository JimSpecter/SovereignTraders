package net.sovereign.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object EquipmentRenderer {

    private val ALL_SLOTS = listOf(
        EquipmentSlot.MAIN_HAND,
        EquipmentSlot.OFF_HAND,
        EquipmentSlot.HELMET,
        EquipmentSlot.CHEST_PLATE,
        EquipmentSlot.LEGGINGS,
        EquipmentSlot.BOOTS
    )

    fun showTo(assembly: VendorAssembly, viewer: Player) {
        if (assembly.equipment.isEmpty()) return

        val equipmentList = buildEquipmentList(assembly)
        if (equipmentList.isEmpty()) return

        val packet = WrapperPlayServerEntityEquipment(assembly.fakeEntityId, equipmentList)
        PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
    }

    fun broadcastAll(assembly: VendorAssembly) {
        if (assembly.equipment.isEmpty()) return

        val equipmentList = buildEquipmentList(assembly)
        if (equipmentList.isEmpty()) return

        val packet = WrapperPlayServerEntityEquipment(assembly.fakeEntityId, equipmentList)
        val packetManager = PacketEvents.getAPI().playerManager
        for (player in Bukkit.getOnlinePlayers()) {
            packetManager.sendPacket(player, packet)
        }
    }

    private fun buildEquipmentList(assembly: VendorAssembly): List<Equipment> {
        return ALL_SLOTS.map { slot ->
            val bukkitStack = assembly.equipment[slot]
            val peStack = if (bukkitStack != null && !bukkitStack.type.isAir) {
                SpigotConversionUtil.fromBukkitItemStack(bukkitStack)
            } else {
                ItemStack.EMPTY
            }
            Equipment(slot, peStack)
        }
    }
}
