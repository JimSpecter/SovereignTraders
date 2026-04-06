package net.sovereign.display

import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import net.sovereign.core.SovereignCore
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType as BukkitEntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class VendorRegistry(private val plugin: SovereignCore) {

    companion object {
        val VENDOR_TAG_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "sovereign_vendor")
        }
        val CATALOG_LINK_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "catalog_link")
        }
        val SKIN_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "vendor_skin")
        }
        val ENTITY_TYPE_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "vendor_entity_type")
        }
        val HOLOGRAM_LINES_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "vendor_hologram")
        }
        val LOOK_RADIUS_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "vendor_look_radius")
        }
        val EQUIPMENT_KEY: NamespacedKey by lazy {
            NamespacedKey(SovereignCore.instance.plugin, "vendor_equipment")
        }
    }

    private val nextEntityId = AtomicInteger(2_050_000_000)

    private val activeVendors = mutableMapOf<UUID, VendorAssembly>()

    fun allAssemblies(): List<VendorAssembly> = activeVendors.values.toList()

    fun getAssembly(interactionId: UUID): VendorAssembly? = activeVendors[interactionId]

    fun isVendorInteraction(entityId: UUID): Boolean = activeVendors.containsKey(entityId)

    fun resolveLinkedCatalog(interactionId: UUID): String? {
        return activeVendors[interactionId]?.linkedCatalog
    }

    fun spawnVendor(
        location: Location,
        spawner: Player,
        skinName: String? = null,
        entityType: EntityType = EntityTypes.PLAYER,
        hologramLines: List<String> = listOf("<dark_gray><st>                    </st>", "<yellow>ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴛʀᴀᴅᴇ", "<dark_gray><st>                    </st>"),
        equipment: Map<EquipmentSlot, ItemStack?> = emptyMap(),
        lookCloseRadius: Double = 5.0,
        interactionWidth: Float = 0.8f,
        interactionHeight: Float = 1.8f,
        autoLinkCatalog: String? = null
    ): UUID {
        val world = location.world ?: throw IllegalStateException("World is null")

        val interaction = world.spawnEntity(location, BukkitEntityType.INTERACTION) as Interaction
        interaction.interactionWidth = interactionWidth
        interaction.interactionHeight = interactionHeight
        interaction.isResponsive = true
        interaction.isPersistent = true
        interaction.persistentDataContainer.set(VENDOR_TAG_KEY, PersistentDataType.BYTE, 1)

        if (skinName != null) {
            interaction.persistentDataContainer.set(SKIN_KEY, PersistentDataType.STRING, skinName)
        }
        val entityTypeName = resolveEntityTypeName(entityType)
        interaction.persistentDataContainer.set(ENTITY_TYPE_KEY, PersistentDataType.STRING, entityTypeName)
        interaction.persistentDataContainer.set(
            HOLOGRAM_LINES_KEY, PersistentDataType.STRING,
            hologramLines.joinToString("||")
        )
        interaction.persistentDataContainer.set(
            LOOK_RADIUS_KEY, PersistentDataType.DOUBLE, lookCloseRadius
        )
        persistEquipment(interaction, equipment)

        val fakeEntityId = if (entityType == EntityTypes.PLAYER) {
            FakePlayerRenderer.spawn(location, skinName, interaction.uniqueId)
        } else {
            val id = nextEntityId.getAndIncrement()
            FakeEntityRenderer.broadcastSpawn(id, entityType, location)
            id
        }

        val assembly = VendorAssembly(
            interactionId = interaction.uniqueId,
            fakeEntityId = fakeEntityId,
            entityType = entityType,
            skinName = skinName,
            hologramLines = hologramLines.toMutableList(),
            lookCloseRadius = lookCloseRadius,
            interactionWidth = interactionWidth,
            interactionHeight = interactionHeight,
            equipment = equipment.toMutableMap()
        )
        activeVendors[interaction.uniqueId] = assembly
        assembly.cachedLocation = location.clone()

        HologramRenderer.allocate(assembly)
        if (hologramLines.isNotEmpty()) {
            for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                HologramRenderer.showTo(assembly, location, player)
            }
        }

        if (equipment.isNotEmpty()) {
            net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
                EquipmentRenderer.broadcastAll(assembly)
            }, 12L)
        }

        if (autoLinkCatalog != null) {
            interaction.persistentDataContainer.set(
                CATALOG_LINK_KEY, PersistentDataType.STRING, autoLinkCatalog
            )
            assembly.linkedCatalog = autoLinkCatalog
        }

        persistVendorToConfig(assembly)

        return interaction.uniqueId
    }

    fun linkNearestVendor(origin: Location, radius: Double, catalogId: String): Boolean {
        val nearest = findNearestVendor(origin, radius) ?: return false
        nearest.linkedCatalog = catalogId

        val world = origin.world ?: return false
        val entity = world.getEntity(nearest.interactionId)
        entity?.persistentDataContainer?.set(
            CATALOG_LINK_KEY, PersistentDataType.STRING, catalogId
        )

        persistVendorToConfig(nearest)
        return true
    }

    data class RemovedVendor(
        val location: Location,
        val skinName: String?,
        val linkedCatalog: String?,
        val entityTypeName: String,
        val hologramLines: List<String>,
        val lookCloseRadius: Double,
        val equipment: Map<EquipmentSlot, ItemStack?>
    )

    fun removeNearestVendor(origin: Location, radius: Double): RemovedVendor? {
        val nearest = findNearestVendor(origin, radius) ?: return null
        val world = origin.world ?: return null

        val entity = world.getEntity(nearest.interactionId)
        val location = entity?.location ?: origin
        val removed = RemovedVendor(
            location, nearest.skinName, nearest.linkedCatalog,
            resolveEntityTypeName(nearest.entityType),
            nearest.hologramLines.toList(),
            nearest.lookCloseRadius,
            nearest.equipment.toMap()
        )

        if (nearest.entityType == EntityTypes.PLAYER) {
            FakePlayerRenderer.despawn(nearest.fakeEntityId)
        } else {
            FakeEntityRenderer.despawnAll(nearest.fakeEntityId)
        }

        HologramRenderer.despawnAll(nearest)

        entity?.remove()
        activeVendors.remove(nearest.interactionId)

        plugin.npcManager.removeVendor(nearest.interactionId)
        return removed
    }

    fun undoRemove(data: RemovedVendor): Boolean {
        val entityType = resolveEntityType(data.entityTypeName)
        spawnVendor(
            data.location,

            data.location.world?.players?.firstOrNull() ?: return false,
            skinName = data.skinName,
            entityType = entityType,
            hologramLines = data.hologramLines,
            equipment = data.equipment,
            lookCloseRadius = data.lookCloseRadius
        )

        if (data.linkedCatalog != null) {
            linkNearestVendor(data.location, 1.0, data.linkedCatalog)
        }
        return true
    }

    fun despawnAll() {
        for ((_, assembly) in activeVendors) {
            if (assembly.entityType == EntityTypes.PLAYER) {
                FakePlayerRenderer.despawn(assembly.fakeEntityId)
            } else {
                FakeEntityRenderer.despawnAll(assembly.fakeEntityId)
            }
            HologramRenderer.despawnAll(assembly)

        }
        activeVendors.clear()
    }

    fun scanAndRecover() {
        for (world in plugin.server.worlds) {
            for (entity in world.entities) {
                if (entity is Interaction) {
                    recoverSingleEntity(entity)
                }
            }
        }
        SovereignCore.broadcast("<gray>Recovered ${activeVendors.size} vendor entities from currently loaded world data.")
    }

    fun recoverSingleEntity(entity: Interaction): Boolean {
        if (!entity.persistentDataContainer.has(VENDOR_TAG_KEY, PersistentDataType.BYTE)) return false
        if (activeVendors.containsKey(entity.uniqueId)) return false

        val pdc = entity.persistentDataContainer

        val catalogLink = pdc.get(CATALOG_LINK_KEY, PersistentDataType.STRING)
        val skin = pdc.get(SKIN_KEY, PersistentDataType.STRING)
        val entityTypeName = pdc.get(ENTITY_TYPE_KEY, PersistentDataType.STRING) ?: "PLAYER"
        val entityType = resolveEntityType(entityTypeName)
        val hologramRaw = pdc.get(HOLOGRAM_LINES_KEY, PersistentDataType.STRING)
        val lines = if (hologramRaw.isNullOrBlank()) mutableListOf()
        else hologramRaw.split("||").toMutableList()
        val lookRadius = if (pdc.has(LOOK_RADIUS_KEY, PersistentDataType.DOUBLE))
            pdc.get(LOOK_RADIUS_KEY, PersistentDataType.DOUBLE) ?: 5.0
        else 5.0
        val equipmentMap = loadEquipment(entity)

        val fakeEntityId = if (entityType == EntityTypes.PLAYER) {
            FakePlayerRenderer.spawn(entity.location, skin, entity.uniqueId)
        } else {
            val id = nextEntityId.getAndIncrement()
            FakeEntityRenderer.broadcastSpawn(id, entityType, entity.location)
            id
        }

        val assembly = VendorAssembly(
            interactionId = entity.uniqueId,
            fakeEntityId = fakeEntityId,
            entityType = entityType,
            skinName = skin,
            hologramLines = lines,
            lookCloseRadius = lookRadius,
            interactionWidth = entity.interactionWidth,
            interactionHeight = entity.interactionHeight,
            linkedCatalog = catalogLink,
            equipment = equipmentMap
        )
        activeVendors[entity.uniqueId] = assembly
        assembly.cachedLocation = entity.location.clone()

        HologramRenderer.allocate(assembly)
        if (lines.isNotEmpty()) {
            for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                HologramRenderer.showTo(assembly, entity.location, player)
            }
        }

        if (equipmentMap.isNotEmpty()) {
            net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
                EquipmentRenderer.broadcastAll(assembly)
            }, 12L)
        }

        if (plugin.npcManager.getVendorConfig(entity.uniqueId) == null) {
            persistVendorToConfig(assembly)
        }

        return true
    }

    fun showVendorsTo(player: Player) {
        for ((_, assembly) in activeVendors) {

            if (assembly.entityType == EntityTypes.PLAYER) {
                FakePlayerRenderer.showTo(assembly.fakeEntityId, player)
            } else {
                val loc = resolveLocation(assembly) ?: continue
                FakeEntityRenderer.showTo(assembly.fakeEntityId, assembly.entityType, loc, player)
            }

            val loc = resolveLocation(assembly)
            if (loc != null && assembly.hologramLines.isNotEmpty()) {
                HologramRenderer.showTo(assembly, loc, player)
            }

            if (assembly.equipment.isNotEmpty()) {
                net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
                    EquipmentRenderer.showTo(assembly, player)
                }, 12L)
            }
        }
    }

    private fun findNearestVendor(origin: Location, radius: Double): VendorAssembly? {
        val world = origin.world ?: return null
        var closest: VendorAssembly? = null
        var closestDist = radius * radius

        for ((_, assembly) in activeVendors) {
            val entity = world.getEntity(assembly.interactionId) ?: continue
            val dist = entity.location.distanceSquared(origin)
            if (dist < closestDist) {
                closestDist = dist
                closest = assembly
            }
        }
        return closest
    }

    private fun resolveLocation(assembly: VendorAssembly): Location? {
        val cached = assembly.cachedLocation
        if (cached != null) return cached

        for (world in plugin.server.worlds) {
            val entity = world.getEntity(assembly.interactionId)
            if (entity != null) {
                assembly.cachedLocation = entity.location
                return entity.location
            }
        }
        return null
    }

    internal fun resolveEntityTypeName(entityType: EntityType): String {
        return when (entityType) {
            EntityTypes.PLAYER -> "PLAYER"
            EntityTypes.VILLAGER -> "VILLAGER"
            EntityTypes.ZOMBIE -> "ZOMBIE"
            EntityTypes.SKELETON -> "SKELETON"
            EntityTypes.WANDERING_TRADER -> "WANDERING_TRADER"
            EntityTypes.PIGLIN -> "PIGLIN"
            EntityTypes.WITCH -> "WITCH"
            EntityTypes.IRON_GOLEM -> "IRON_GOLEM"
            EntityTypes.SNOW_GOLEM -> "SNOW_GOLEM"
            EntityTypes.ENDERMAN -> "ENDERMAN"
            EntityTypes.BLAZE -> "BLAZE"
            EntityTypes.ALLAY -> "ALLAY"
            EntityTypes.FOX -> "FOX"
            EntityTypes.CAT -> "CAT"
            EntityTypes.WOLF -> "WOLF"
            else -> "PLAYER"
        }
    }

    internal fun resolveEntityType(name: String): EntityType {
        return when (name.uppercase()) {
            "PLAYER" -> EntityTypes.PLAYER
            "VILLAGER" -> EntityTypes.VILLAGER
            "ZOMBIE" -> EntityTypes.ZOMBIE
            "SKELETON" -> EntityTypes.SKELETON
            "WANDERING_TRADER" -> EntityTypes.WANDERING_TRADER
            "PIGLIN" -> EntityTypes.PIGLIN
            "WITCH" -> EntityTypes.WITCH
            "IRON_GOLEM" -> EntityTypes.IRON_GOLEM
            "SNOW_GOLEM" -> EntityTypes.SNOW_GOLEM
            "ENDERMAN" -> EntityTypes.ENDERMAN
            "BLAZE" -> EntityTypes.BLAZE
            "ALLAY" -> EntityTypes.ALLAY
            "FOX" -> EntityTypes.FOX
            "CAT" -> EntityTypes.CAT
            "WOLF" -> EntityTypes.WOLF
            else -> EntityTypes.PLAYER
        }
    }

    fun persistVendorToConfig(assembly: VendorAssembly) {
        val loc = resolveLocation(assembly) ?: return
        val equipMap = mutableMapOf<String, String>()
        for ((slot, item) in assembly.equipment) {
            if (item == null || item.type == Material.AIR) continue
            val bytes = item.serializeAsBytes()
            equipMap[slot.name] = java.util.Base64.getEncoder().encodeToString(bytes)
        }

        val config = net.sovereign.core.config.NpcManager.VendorConfig(
            entityType = resolveEntityTypeName(assembly.entityType),
            skin = assembly.skinName,
            hologram = assembly.hologramLines.toList(),
            lookCloseRadius = assembly.lookCloseRadius,
            interactionWidth = assembly.interactionWidth,
            interactionHeight = assembly.interactionHeight,
            catalog = assembly.linkedCatalog,
            world = loc.world?.name ?: "world",
            x = loc.x,
            y = loc.y,
            z = loc.z,
            yaw = loc.yaw,
            pitch = loc.pitch,
            equipment = equipMap
        )
        plugin.npcManager.saveVendor(assembly.interactionId, config)
    }

    fun syncFromConfig() {
        val configs = plugin.npcManager.loadAllVendors()
        var synced = 0

        for ((uuid, config) in configs) {
            val assembly = activeVendors[uuid] ?: continue
            val world = plugin.server.getWorld(config.world) ?: continue
            val interaction = world.getEntity(uuid) as? Interaction ?: continue
            val loc = interaction.location
            val pdc = interaction.persistentDataContainer
            var changed = false

            val newEntityType = resolveEntityType(config.entityType)
            val skinChanged = config.skin != assembly.skinName
            val typeChanged = newEntityType != assembly.entityType

            if (typeChanged || skinChanged) {

                if (assembly.entityType == EntityTypes.PLAYER) {
                    FakePlayerRenderer.despawn(assembly.fakeEntityId)
                } else {
                    FakeEntityRenderer.despawnAll(assembly.fakeEntityId)
                }

                val newFakeId = if (newEntityType == EntityTypes.PLAYER) {
                    FakePlayerRenderer.spawn(loc, config.skin, uuid)
                } else {
                    val id = nextEntityId.getAndIncrement()
                    FakeEntityRenderer.broadcastSpawn(id, newEntityType, loc)
                    id
                }
                assembly.fakeEntityId = newFakeId
                assembly.entityType = newEntityType
                assembly.skinName = config.skin

                pdc.set(ENTITY_TYPE_KEY, PersistentDataType.STRING, config.entityType)
                if (config.skin != null) {
                    pdc.set(SKIN_KEY, PersistentDataType.STRING, config.skin)
                } else {
                    pdc.remove(SKIN_KEY)
                }

                if (assembly.equipment.isNotEmpty()) {
                    net.sovereign.core.scheduler.SovereignScheduler.runTaskLater(plugin.plugin, Runnable {
                        EquipmentRenderer.broadcastAll(assembly)
                    }, 12L)
                }
                changed = true
            }

            if (config.hologram != assembly.hologramLines) {
                HologramRenderer.despawnAll(assembly)
                assembly.hologramLines.clear()
                assembly.hologramLines.addAll(config.hologram)
                assembly.hologramEntityIds.clear()
                HologramRenderer.allocate(assembly)
                for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                    HologramRenderer.showTo(assembly, loc, player)
                }
                pdc.set(HOLOGRAM_LINES_KEY, PersistentDataType.STRING,
                    config.hologram.joinToString("||")
                )
                changed = true
            }

            if (config.lookCloseRadius != assembly.lookCloseRadius) {
                assembly.lookCloseRadius = config.lookCloseRadius
                pdc.set(LOOK_RADIUS_KEY, PersistentDataType.DOUBLE, config.lookCloseRadius)
                changed = true
            }

            if (config.interactionWidth != assembly.interactionWidth) {
                assembly.interactionWidth = config.interactionWidth
                interaction.interactionWidth = config.interactionWidth
                changed = true
            }
            if (config.interactionHeight != assembly.interactionHeight) {
                assembly.interactionHeight = config.interactionHeight
                interaction.interactionHeight = config.interactionHeight
                changed = true
            }

            if (config.catalog != assembly.linkedCatalog) {
                assembly.linkedCatalog = config.catalog
                if (config.catalog != null) {
                    pdc.set(CATALOG_LINK_KEY, PersistentDataType.STRING, config.catalog)
                } else {
                    pdc.remove(CATALOG_LINK_KEY)
                }
                changed = true
            }

            val newEquipment = loadEquipmentFromMap(config.equipment)
            if (newEquipment != assembly.equipment) {
                assembly.equipment.clear()
                assembly.equipment.putAll(newEquipment)
                persistEquipment(interaction, assembly.equipment)
                EquipmentRenderer.broadcastAll(assembly)
                changed = true
            }

            if (changed) synced++
        }

        if (synced > 0) {
            SovereignCore.broadcast("<green>Synced <yellow>$synced <green>vendor(s) from npc.yml.")
        }
    }

    private fun loadEquipmentFromMap(map: Map<String, String>): MutableMap<com.github.retrooper.packetevents.protocol.player.EquipmentSlot, ItemStack?> {
        val result = mutableMapOf<com.github.retrooper.packetevents.protocol.player.EquipmentSlot, ItemStack?>()
        for ((slotName, b64) in map) {
            try {
                val slot = com.github.retrooper.packetevents.protocol.player.EquipmentSlot.valueOf(slotName)
                val bytes = java.util.Base64.getDecoder().decode(b64)
                result[slot] = ItemStack.deserializeBytes(bytes)
            } catch (_: Exception) {  }
        }
        return result
    }

    private fun persistEquipment(interaction: Interaction, equipment: Map<EquipmentSlot, ItemStack?>) {
        if (equipment.isEmpty()) return
        val parts = equipment.entries.mapNotNull { (slot, item) ->
            if (item == null || item.type == Material.AIR) return@mapNotNull null
            val bytes = item.serializeAsBytes()
            val b64 = java.util.Base64.getEncoder().encodeToString(bytes)
            "${slot.name}:$b64"
        }
        if (parts.isNotEmpty()) {
            interaction.persistentDataContainer.set(
                EQUIPMENT_KEY, PersistentDataType.STRING, parts.joinToString("|")
            )
        }
    }

    private fun loadEquipment(interaction: Interaction): MutableMap<EquipmentSlot, ItemStack?> {
        val map = mutableMapOf<EquipmentSlot, ItemStack?>()
        val raw = interaction.persistentDataContainer.get(EQUIPMENT_KEY, PersistentDataType.STRING) ?: return map
        for (part in raw.split("|")) {
            val colon = part.indexOf(':')
            if (colon == -1) continue
            val slotName = part.substring(0, colon)
            val b64 = part.substring(colon + 1)
            try {
                val slot = EquipmentSlot.valueOf(slotName)
                val bytes = java.util.Base64.getDecoder().decode(b64)
                val item = ItemStack.deserializeBytes(bytes)
                map[slot] = item
            } catch (_: Exception) {

            }
        }
        return map
    }
}
