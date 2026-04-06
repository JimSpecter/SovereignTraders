package net.sovereign.core.command

import net.sovereign.core.SovereignCore
import net.sovereign.components.catalog.TransactionMode
import net.sovereign.components.migration.DtlMigrationModule
import net.sovereign.display.VendorRegistry
import net.sovereign.display.HologramRenderer
import net.sovereign.display.EquipmentRenderer
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class SovereignCommandExecutor(private val plugin: SovereignCore, private val devCommands: DevCommandSupport? = null) {

    private val mm = MiniMessage.miniMessage()

    private val lastRemoved = mutableMapOf<UUID, VendorRegistry.RemovedVendor>()

    fun buildCommandTree(): com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> {
        val catalogSuggestions = com.mojang.brigadier.suggestion.SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> { _, builder ->
            plugin.catalogRepository.listIdentifiers().forEach { builder.suggest(it) }
            builder.buildFuture()
        }

        val root = io.papermc.paper.command.brigadier.Commands.literal("traders")
            .executes { ctx ->
                onCommand(ctx.source.sender, "traders", emptyArray())
                com.mojang.brigadier.Command.SINGLE_SUCCESS
            }
            .then(io.papermc.paper.command.brigadier.Commands.literal("help")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("help"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                .then(io.papermc.paper.command.brigadier.Commands.argument("topic", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests { _, builder ->
                        listOf("catalog", "listing", "vendor", "admin", "market", "citizen").forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }
                    .executes { ctx ->
                        val topic = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "topic")
                        onCommand(ctx.source.sender, "traders", arrayOf("help", topic))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("reload")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("reload"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("catalog")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("catalog"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                .then(io.papermc.paper.command.brigadier.Commands.literal("list")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("catalog", "list"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("open")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .executes { ctx ->
                            val name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")
                            onCommand(ctx.source.sender, "traders", arrayOf("catalog", "open", name))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("create")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes { ctx ->
                            val name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")
                            onCommand(ctx.source.sender, "traders", arrayOf("catalog", "create", name))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("delete")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .executes { ctx ->
                            val name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")
                            onCommand(ctx.source.sender, "traders", arrayOf("catalog", "delete", name))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("edit")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .executes { ctx ->
                            val name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")
                            onCommand(ctx.source.sender, "traders", arrayOf("catalog", "edit", name))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("listing")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("listing"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                .then(io.papermc.paper.command.brigadier.Commands.literal("add")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("catalog", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .then(io.papermc.paper.command.brigadier.Commands.argument("buy", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(io.papermc.paper.command.brigadier.Commands.argument("sell", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes { ctx ->
                                    val catalog = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "catalog")
                                    val buy = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "buy")
                                    val sell = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "sell")
                                    onCommand(ctx.source.sender, "traders", arrayOf("listing", "add", catalog, buy, sell))
                                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                                }
                            )
                        )
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("remove")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("catalog", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .then(io.papermc.paper.command.brigadier.Commands.argument("slot", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .executes { ctx ->
                                val catalog = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "catalog")
                                val slot = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "slot")
                                onCommand(ctx.source.sender, "traders", arrayOf("listing", "remove", catalog, slot))
                                com.mojang.brigadier.Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("vendor")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("vendor"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                .then(io.papermc.paper.command.brigadier.Commands.literal("spawn")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("vendor", "spawn"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                    .then(io.papermc.paper.command.brigadier.Commands.argument("preset_or_skin", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes { ctx ->
                            val presetOrSkin = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "preset_or_skin")
                            onCommand(ctx.source.sender, "traders", arrayOf("vendor", "spawn", presetOrSkin))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                        .then(io.papermc.paper.command.brigadier.Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .executes { ctx ->
                                val presetOrSkin = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "preset_or_skin")
                                val type = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type")
                                onCommand(ctx.source.sender, "traders", arrayOf("vendor", "spawn", presetOrSkin, type))
                                com.mojang.brigadier.Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("remove")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("vendor", "remove"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("undo")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("vendor", "undo"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("link")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("catalog", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .executes { ctx ->
                            val catalog = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "catalog")
                            onCommand(ctx.source.sender, "traders", arrayOf("vendor", "link", catalog))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("equip")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("slot", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests { _, builder ->
                            listOf("mainhand", "offhand", "helmet", "chestplate", "leggings", "boots").forEach { builder.suggest(it) }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            val slot = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "slot")
                            onCommand(ctx.source.sender, "traders", arrayOf("vendor", "equip", slot))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("hologram")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("lines", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes { ctx ->
                            val lines = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "lines")
                            onCommand(ctx.source.sender, "traders", arrayOf("vendor", "hologram") + lines.split(" ").toTypedArray())
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("market")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("market"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("migrate")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("migrate"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                .then(io.papermc.paper.command.brigadier.Commands.literal("dtl")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("migrate", "dtl"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
            )
            .then(io.papermc.paper.command.brigadier.Commands.literal("citizen")
                .executes { ctx ->
                    onCommand(ctx.source.sender, "traders", arrayOf("citizen"))
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                .then(io.papermc.paper.command.brigadier.Commands.literal("link")
                    .then(io.papermc.paper.command.brigadier.Commands.argument("catalog", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(catalogSuggestions)
                        .executes { ctx ->
                            val catalog = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "catalog")
                            onCommand(ctx.source.sender, "traders", arrayOf("citizen", "link", catalog))
                            com.mojang.brigadier.Command.SINGLE_SUCCESS
                        }
                    )
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("unlink")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("citizen", "unlink"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
                .then(io.papermc.paper.command.brigadier.Commands.literal("list")
                    .executes { ctx ->
                        onCommand(ctx.source.sender, "traders", arrayOf("citizen", "list"))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                )
            )

        if (devCommands != null) {
            root.then(devCommands.buildNode { sender, args -> onCommand(sender, "traders", args) })
        }

        return root.build()
    }

    fun onCommand(sender: CommandSender, label: String, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender, emptyArray())
            return
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "catalog", "cat" -> handleCatalog(sender, args)
            "listing", "l" -> handleListing(sender, args)
            "vendor", "v" -> handleVendor(sender, args)
            "market", "m" -> handleMarket(sender)
            "migrate" -> handleMigrate(sender, args)
            "citizen", "cit" -> handleCitizen(sender, args)
            "dev" -> devCommands?.handle(sender, args)
            "help" -> sendUsage(sender, args)
            else -> {
                if (sender is Player) {
                    plugin.localeManager.dispatch(sender, "errors.no-directives")
                }
            }
        }

    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("sovereign.admin.reload")) {
            if (sender is Player) plugin.localeManager.dispatch(sender, "general.restricted-access")
            return
        }
        plugin.reload()
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <green>ᴄᴏɴꜰɪɢᴜʀᴀᴛɪᴏɴ ʀᴇʟᴏᴀᴅᴇᴅ."))
    }

    private fun handleCatalog(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders catalog [list|open|edit|create|delete]"))
            return
        }

        when (args[1].lowercase()) {
            "list" -> {
                if (!sender.hasPermission("sovereign.catalog.list")) {
                    if (sender is Player) plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                val catalogs = plugin.catalogRepository.listIdentifiers()
                if (catalogs.isEmpty()) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <gray>ɴᴏ ᴄᴀᴛᴀʟᴏɢs ᴄᴏɴꜰɪɢᴜʀᴇᴅ."))
                } else {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <green>ᴄᴀᴛᴀʟᴏɢs (${catalogs.size}):"))
                    catalogs.forEach { id ->
                        sender.sendMessage(mm.deserialize("  <gray>- <yellow>$id"))
                    }
                }
            }

            "open" -> {
                if (sender !is Player) return
                if (!sender.hasPermission("sovereign.catalog.open")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders catalog open [ɴᴀᴍᴇ]"))
                    return
                }
                val catalogId = args[2].lowercase()
                val catalog = plugin.catalogRepository.resolve(catalogId)
                if (catalog == null) {
                    plugin.localeManager.dispatch(
                        sender, "catalog.not-found",
                        "%input%" to catalogId
                    )
                    return
                }
                plugin.catalogSessionManager.openSession(sender, catalog, 0, TransactionMode.ACQUIRE)
            }

            "create" -> {
                if (!sender.hasPermission("sovereign.catalog.create")) {
                    if (sender is Player) plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders catalog create [ɴᴀᴍᴇ]"))
                    return
                }
                val catalogId = args[2].lowercase()
                if (plugin.catalogRepository.resolve(catalogId) != null) {
                    plugin.localeManager.dispatch(
                        sender as? Player ?: return,
                        "catalog.duplicate",
                        "%input%" to catalogId
                    )
                    return
                }
                plugin.catalogRepository.createDefault(catalogId)
                if (sender is Player) plugin.localeManager.dispatch(sender, "catalog.created")
            }

            "delete" -> {
                if (!sender.hasPermission("sovereign.catalog.delete")) {
                    if (sender is Player) plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) return
                val catalogId = args[2].lowercase()
                plugin.catalogRepository.remove(catalogId)
                if (sender is Player) {
                    plugin.localeManager.dispatch(
                        sender, "catalog.removed",
                        "%catalog%" to catalogId
                    )
                }
            }

            "edit" -> {
                if (sender !is Player) return
                if (!sender.hasPermission("sovereign.catalog.edit")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders catalog edit [ɴᴀᴍᴇ]"))
                    return
                }
                val catalogId = args[2].lowercase()
                val catalog = plugin.catalogRepository.resolve(catalogId)
                if (catalog == null) {
                    plugin.localeManager.dispatch(
                        sender, "catalog.not-found",
                        "%input%" to catalogId
                    )
                    return
                }
                plugin.catalogEditor.openEditor(sender, catalog)
            }
        }
    }

    private fun handleListing(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders listing [add|remove]"))
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (!sender.hasPermission("sovereign.listing.add")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 5) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders listing add [ᴄᴀᴛᴀʟᴏɢ] [ʙᴜʏ-ᴘʀɪᴄᴇ] [sᴇʟʟ-ᴘʀɪᴄᴇ]"))
                    sender.sendMessage(mm.deserialize("<gray>  ʜᴏʟᴅ ᴛʜᴇ ɪᴛᴇᴍ ʏᴏᴜ ᴡᴀɴᴛ ᴛᴏ ᴀᴅᴅ ɪɴ ʏᴏᴜʀ ʜᴀɴᴅ."))
                    return
                }
                val catalogId = args[2].lowercase()
                val buyPrice = args[3].toDoubleOrNull()
                val sellPrice = args[4].toDoubleOrNull()
                if (buyPrice == null || sellPrice == null) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ʙᴜʏ ᴀɴᴅ sᴇʟʟ ᴘʀɪᴄᴇs ᴍᴜsᴛ ʙᴇ ɴᴜᴍʙᴇʀs."))
                    return
                }
                plugin.catalogEditor.addListingFromCommand(sender, catalogId, buyPrice, sellPrice)
            }

            "remove" -> {
                if (!sender.hasPermission("sovereign.listing.remove")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 4) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders listing remove [ᴄᴀᴛᴀʟᴏɢ] [sʟᴏᴛ]"))
                    return
                }
                val catalogId = args[2].lowercase()
                val slot = args[3].toIntOrNull()
                if (slot == null || slot < 0) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>sʟᴏᴛ ᴍᴜsᴛ ʙᴇ ᴀ ɴᴏɴ-ɴᴇɢᴀᴛɪᴠᴇ ɴᴜᴍʙᴇʀ."))
                    return
                }
                val catalog = plugin.catalogRepository.resolve(catalogId)
                if (catalog == null) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ᴄᴀᴛᴀʟᴏɢ '<yellow>$catalogId<red>' ɴᴏᴛ ꜰᴏᴜɴᴅ."))
                    return
                }
                if (catalog.sections.isEmpty()) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ᴄᴀᴛᴀʟᴏɢ ʜᴀs ɴᴏ sᴇᴄᴛɪᴏɴs."))
                    return
                }
                val section = catalog.sections[0]
                var removed = false
                for (mode in TransactionMode.entries) {
                    val listings = section.listingsForMode(mode)
                    if (slot < listings.size && listings[slot] != null) {
                        listings[slot] = null
                        removed = true
                    }
                }
                if (removed) {
                    plugin.catalogRepository.persistAll()
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ʀᴇᴍᴏᴠᴇᴅ ʟɪsᴛɪɴɢ ᴀᴛ sʟᴏᴛ <yellow>$slot <red>ꜰʀᴏᴍ ᴀʟʟ ᴍᴏᴅᴇs."))
                } else {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ɴᴏ ʟɪsᴛɪɴɢ ᴀᴛ sʟᴏᴛ $slot."))
                }
            }
        }
    }

    private fun handleVendor(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders vendor [spawn|remove|undo|link|equip|hologram]"))
            return
        }

        when (args[1].lowercase()) {
            "spawn" -> {
                if (!sender.hasPermission("sovereign.vendor.spawn")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }

                val npcManager = plugin.npcManager
                val firstArg = args.getOrNull(2)

                val preset = if (firstArg != null) npcManager.resolvePreset(firstArg) else null

                if (preset != null) {

                    val entityType = resolveEntityType(preset.entityType)
                    plugin.vendorRegistry.spawnVendor(
                        sender.location, sender,
                        skinName = if (entityType == EntityTypes.PLAYER) preset.skin else null,
                        entityType = entityType,
                        hologramLines = preset.hologram,
                        lookCloseRadius = preset.lookCloseRadius,
                        interactionWidth = preset.interactionWidth,
                        interactionHeight = preset.interactionHeight,
                        autoLinkCatalog = preset.catalog
                    )
                    plugin.localeManager.dispatch(sender, "vendor.spawned")
                } else {

                    val skinName = firstArg
                    val entityTypeName = args.getOrNull(3)?.uppercase()
                    val entityType = if (entityTypeName != null) {
                        resolveEntityType(entityTypeName)
                    } else {
                        resolveEntityType(npcManager.defaultEntityType)
                    }

                    plugin.vendorRegistry.spawnVendor(
                        sender.location, sender,
                        skinName = if (entityType == EntityTypes.PLAYER) skinName else null,
                        entityType = entityType,
                        hologramLines = npcManager.defaultHologram,
                        lookCloseRadius = npcManager.defaultLookCloseRadius,
                        interactionWidth = npcManager.defaultInteractionWidth,
                        interactionHeight = npcManager.defaultInteractionHeight
                    )
                    plugin.localeManager.dispatch(sender, "vendor.spawned")
                }
            }

            "remove" -> {
                if (!sender.hasPermission("sovereign.vendor.remove")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                val removed = plugin.vendorRegistry.removeNearestVendor(sender.location, 5.0)
                if (removed != null) {
                    lastRemoved[sender.uniqueId] = removed
                    val skinLabel = removed.skinName ?: "default"
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ʀᴇᴍᴏᴠᴇᴅ ᴠᴇɴᴅᴏʀ <gray>(<yellow>$skinLabel<gray>)<red>. ᴜsᴇ <yellow>/traders vendor undo <red>ᴛᴏ ʀᴇsᴛᴏʀᴇ."))
                } else {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ɴᴏ ᴠᴇɴᴅᴏʀ ꜰᴏᴜɴᴅ ᴡɪᴛʜɪɴ 5 ʙʟᴏᴄᴋs."))
                }
            }

            "undo" -> {
                if (!sender.hasPermission("sovereign.vendor.remove")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                val data = lastRemoved.remove(sender.uniqueId)
                if (data != null) {
                    plugin.vendorRegistry.undoRemove(data)
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <green>ᴠᴇɴᴅᴏʀ ʀᴇsᴛᴏʀᴇᴅ."))
                } else {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ɴᴏᴛʜɪɴɢ ᴛᴏ ᴜɴᴅᴏ."))
                }
            }

            "link" -> {
                if (!sender.hasPermission("sovereign.vendor.link")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders vendor link [ᴄᴀᴛᴀʟᴏɢ]"))
                    return
                }
                val catalogId = args[2].lowercase()
                val linked = plugin.vendorRegistry.linkNearestVendor(sender.location, 3.0, catalogId)
                if (linked) {
                    plugin.localeManager.dispatch(
                        sender, "vendor.linked",
                        "%catalog%" to catalogId
                    )
                } else {
                    plugin.localeManager.dispatch(sender, "vendor.unlinked")
                }
            }

            "equip" -> {
                if (!sender.hasPermission("sovereign.vendor.equip")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders vendor equip [mainhand|offhand|helmet|chestplate|leggings|boots]"))
                    return
                }
                val slotName = args[2].uppercase()
                val slot = when (slotName) {
                    "MAINHAND", "MAIN_HAND" -> EquipmentSlot.MAIN_HAND
                    "OFFHAND", "OFF_HAND" -> EquipmentSlot.OFF_HAND
                    "HELMET", "HEAD" -> EquipmentSlot.HELMET
                    "CHESTPLATE", "CHEST_PLATE", "CHEST" -> EquipmentSlot.CHEST_PLATE
                    "LEGGINGS", "LEGS" -> EquipmentSlot.LEGGINGS
                    "BOOTS", "FEET" -> EquipmentSlot.BOOTS
                    else -> {
                        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ᴜɴᴋɴᴏᴡɴ sʟᴏᴛ: <yellow>$slotName"))
                        return
                    }
                }

                val heldItem = sender.inventory.itemInMainHand
                if (heldItem.type.isAir) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ʜᴏʟᴅ ᴀɴ ɪᴛᴇᴍ ɪɴ ʏᴏᴜʀ ᴍᴀɪɴ ʜᴀɴᴅ ᴛᴏ ᴇǫᴜɪᴘ ɪᴛ ᴏɴ ᴛʜᴇ ᴠᴇɴᴅᴏʀ."))
                    return
                }

                val assembly = findNearestAssembly(sender)
                if (assembly == null) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ɴᴏ ᴠᴇɴᴅᴏʀ ꜰᴏᴜɴᴅ ᴡɪᴛʜɪɴ 5 ʙʟᴏᴄᴋs."))
                    return
                }

                assembly.equipment[slot] = heldItem.clone()
                EquipmentRenderer.broadcastAll(assembly)

                plugin.vendorRegistry.persistVendorToConfig(assembly)
                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <green>ᴇǫᴜɪᴘᴘᴇᴅ <yellow>${heldItem.type.name} <green>ᴛᴏ <yellow>$slotName<green>."))
            }

            "hologram" -> {
                if (!sender.hasPermission("sovereign.vendor.hologram")) {
                    plugin.localeManager.dispatch(sender, "general.restricted-access")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders vendor hologram [ʟɪɴᴇ1||ʟɪɴᴇ2||...]"))
                    sender.sendMessage(mm.deserialize("<gray>  sᴇᴘᴀʀᴀᴛᴇ ʟɪɴᴇs ᴡɪᴛʜ <yellow>|| <gray>— sᴜᴘᴘᴏʀᴛs ᴍɪɴɪᴍᴇssᴀɢᴇ."))
                    return
                }

                val assembly = findNearestAssembly(sender)
                if (assembly == null) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ɴᴏ ᴠᴇɴᴅᴏʀ ꜰᴏᴜɴᴅ ᴡɪᴛʜɪɴ 5 ʙʟᴏᴄᴋs."))
                    return
                }

                HologramRenderer.despawnAll(assembly)

                val rawText = args.drop(2).joinToString(" ")
                val newLines = rawText.split("||").map { it.trim() }.filter { it.isNotEmpty() }
                assembly.hologramLines.clear()
                assembly.hologramLines.addAll(newLines)

                val world = sender.location.world
                val interactionEntity = world?.getEntity(assembly.interactionId)
                val loc = interactionEntity?.location ?: sender.location
                HologramRenderer.allocate(assembly)
                for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                    HologramRenderer.showTo(assembly, loc, player)
                }

                plugin.vendorRegistry.persistVendorToConfig(assembly)

                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <green>ᴜᴘᴅᴀᴛᴇᴅ ʜᴏʟᴏɢʀᴀᴍ (${newLines.size} ʟɪɴᴇs)."))
            }
        }
    }

    private fun handleMigrate(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("sovereign.admin.migrate")) {
            if (sender is Player) plugin.localeManager.dispatch(sender, "general.restricted-access")
            return
        }
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders migrate dtl"))
            return
        }
        when (args[1].lowercase()) {
            "dtl", "dtltraders", "dtltradersplus" -> {
                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <aqua>sᴛᴀʀᴛɪɴɢ ᴅᴛʟᴛʀᴀᴅᴇʀs ᴍɪɢʀᴀᴛɪᴏɴ..."))
                val module = DtlMigrationModule(plugin)
                module.migrate(sender)
            }
            else -> {
                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>» <red>ᴜɴᴋɴᴏᴡɴ ᴍɪɢʀᴀᴛɪᴏɴ sᴏᴜʀᴄᴇ: <yellow>${args[1]}"))
                sender.sendMessage(mm.deserialize("<gray>  ᴀᴠᴀɪʟᴀʙʟᴇ: <yellow>ᴅᴛʟ"))
            }
        }
    }

    private fun handleMarket(sender: CommandSender) {
        if (sender !is Player) return
        if (!sender.hasPermission("sovereign.admin.market")) {
            plugin.localeManager.dispatch(sender, "general.restricted-access")
            return
        }
        plugin.pricingModule.openDashboard(sender)
    }

    private fun handleCitizen(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return
        if (!sender.hasPermission("sovereign.admin.citizen")) {
            plugin.localeManager.dispatch(sender, "general.restricted-access")
            return
        }

        val bridge = plugin.citizensBridgeManager
        if (bridge == null) {
            sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <red>ᴄɪᴛɪᴢᴇᴎs ᴘʟᴜɢɪᴎ ɪs ᴎᴏᴛ ʟᴏᴀᴅᴇᴅ."))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders citizen [link|unlink|list]"))
            return
        }

        when (args[1].lowercase()) {
            "link" -> {
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders citizen link [ᴄᴀᴛᴀʟᴏɢ]"))
                    sender.sendMessage(mm.deserialize("<gray>  ʟᴏᴏᴋ ᴀᴛ ᴀ ᴄɪᴛɪᴢᴇᴎs ᴎᴘᴄ ᴡɪᴛʜɪᴎ 5 ʙʟᴏᴄᴋs."))
                    return
                }

                val catalogId = args[2].lowercase()
                if (plugin.catalogRepository.resolve(catalogId) == null) {
                    plugin.localeManager.dispatch(sender, "catalog.not-found", "%input%" to catalogId)
                    return
                }

                val npcId = findLookedAtCitizensNpcId(sender)
                if (npcId == null) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <red>ᴎᴏ ᴄɪᴛɪᴢᴇᴎs ᴎᴘᴄ ғᴏᴜᴎᴅ ᴎᴇᴀʀʙʏ. ʟᴏᴏᴋ ᴀᴛ ᴏᴎᴇ ᴀᴎᴅ ᴛʀʏ ᴀɢᴀɪᴎ."))
                    return
                }

                bridge.link(npcId, catalogId)
                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <green>ᴄɪᴛɪᴢᴇᴎs ᴎᴘᴄ <yellow>#$npcId <green>ʟɪᴎᴋᴇᴅ ᴛᴏ ᴄᴀᴛᴀʟᴏɢ <yellow>$catalogId<green>."))
            }

            "unlink" -> {
                val npcId = findLookedAtCitizensNpcId(sender)
                if (npcId == null) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <red>ᴎᴏ ᴄɪᴛɪᴢᴇᴎs ᴎᴘᴄ ғᴏᴜᴎᴅ ᴎᴇᴀʀʙʏ."))
                    return
                }

                val removed = bridge.unlink(npcId)
                if (removed) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <red>ᴄɪᴛɪᴢᴇᴎs ᴎᴘᴄ <yellow>#$npcId <red>ᴜᴎʟɪᴎᴋᴇᴅ."))
                } else {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <yellow>ᴛʜᴀᴛ ᴎᴘᴄ ɪs ᴎᴏᴛ ʟɪᴎᴋᴇᴅ."))
                }
            }

            "list" -> {
                val all = bridge.allMappings()
                if (all.isEmpty()) {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <gray>ᴎᴏ ᴄɪᴛɪᴢᴇᴎs ᴎᴘᴄs ᴀʀᴇ ʟɪᴎᴋᴇᴅ."))
                } else {
                    sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <green>ᴄɪᴛɪᴢᴇᴎs ʟɪᴎᴋs (${all.size}):"))
                    for ((npcId, catalogId) in all) {
                        sender.sendMessage(mm.deserialize("  <gray>- <yellow>NPC #$npcId <gray>→ <green>$catalogId"))
                    }
                }
            }

            else -> {
                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇᴋᴇᴎ</bold> <gray>» <yellow>ᴜsᴀɢᴇ: /traders citizen [link|unlink|list]"))
            }
        }
    }

    private fun findLookedAtCitizensNpcId(player: Player): Int? {
        val target = player.getTargetEntity(5) ?: return null
        if (!target.hasMetadata("NPC")) return null
        return try {
            val citizensRegistry = Class.forName("net.citizensnpcs.api.CitizensAPI")
                .getMethod("getNPCRegistry")
                .invoke(null)
            val npc = citizensRegistry.javaClass
                .getMethod("getNPC", org.bukkit.entity.Entity::class.java)
                .invoke(citizensRegistry, target) ?: return null
            npc.javaClass.getMethod("getId").invoke(npc) as? Int
        } catch (_: Exception) {
            null
        }
    }

    private fun sendUsage(sender: CommandSender, args: Array<out String>) {
        when (args.getOrNull(1)?.lowercase()) {
            null -> sendHelpOverview(sender)
            "catalog", "cat" -> sendCatalogHelp(sender)
            "listing", "l" -> sendListingHelp(sender)
            "vendor", "v" -> sendVendorHelp(sender)
            "admin" -> sendAdminHelp(sender)
            "citizen", "cit" -> {
                if (plugin.citizensBridgeManager != null) sendCitizenHelp(sender)
                else sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u00bb <red>ᴄɪᴛɪᴢᴇɴs ɪɴᴛᴇɢʀᴀᴛɪᴏɴ ɪs ɴᴏᴛ ʟᴏᴀᴅᴇᴅ."))
            }
            "market" -> {
                if (plugin.pricingModule.isEnabled) sendMarketHelp(sender)
                else sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u00bb <red>ᴅʏɴᴀᴍɪᴄ ᴘʀɪᴄɪɴɢ ɪs ɴᴏᴛ ᴇɴᴀʙʟᴇᴅ."))
            }
            else -> {
                val bad = args.getOrNull(1) ?: ""
                sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u00bb <red>ᴜɴᴋɴᴏᴡɴ ᴛᴏᴘɪᴄ: <yellow>$bad"))
                sender.sendMessage(mm.deserialize("<gray>  ᴀᴠᴀɪʟᴀʙʟᴇ: <yellow>catalog<gray>, <yellow>listing<gray>, <yellow>vendor<gray>, <yellow>admin"))
            }
        }
    }

    private fun sendHelpOverview(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴᴛʀᴀᴅᴇʀs</bold> <gray>\u2014 ᴄᴏᴍᴍᴀɴᴅ ʀᴇꜰᴇʀᴇɴᴄᴇ"))
        if (hasAnyPerm(sender, "sovereign.catalog.list", "sovereign.catalog.open",
                "sovereign.catalog.edit", "sovereign.catalog.create", "sovereign.catalog.delete")) {
            sender.sendMessage(mm.deserialize(
                "  <click:run_command:'/traders help catalog'><hover:show_text:'<gray>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs'><green>\u25b8 ᴄᴀᴛᴀʟᴏɢ <dark_gray>\u2014 <gray>list \u00b7 open \u00b7 edit \u00b7 create \u00b7 delete</hover></click>"
            ))
        }
        if (hasAnyPerm(sender, "sovereign.listing.add", "sovereign.listing.remove")) {
            sender.sendMessage(mm.deserialize(
                "  <click:run_command:'/traders help listing'><hover:show_text:'<gray>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs'><green>\u25b8 ʟɪsᴛɪɴɢ <dark_gray>\u2014 <gray>add \u00b7 remove</hover></click>"
            ))
        }
        if (hasAnyPerm(sender, "sovereign.vendor.spawn", "sovereign.vendor.remove",
                "sovereign.vendor.link", "sovereign.vendor.equip", "sovereign.vendor.hologram")) {
            sender.sendMessage(mm.deserialize(
                "  <click:run_command:'/traders help vendor'><hover:show_text:'<gray>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs'><green>\u25b8 ᴠᴇɴᴅᴏʀ <dark_gray>\u2014 <gray>spawn \u00b7 remove \u00b7 undo \u00b7 link \u00b7 equip \u00b7 hologram</hover></click>"
            ))
        }
        if (hasAnyPerm(sender, "sovereign.admin.reload", "sovereign.admin.migrate")) {
            sender.sendMessage(mm.deserialize(
                "  <click:run_command:'/traders help admin'><hover:show_text:'<gray>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs'><green>\u25b8 ᴀᴅᴍɪɴ <dark_gray>\u2014 <gray>reload \u00b7 migrate</hover></click>"
            ))
        }
        if (plugin.pricingModule.isEnabled && sender.hasPermission("sovereign.admin.market")) {
            sender.sendMessage(mm.deserialize(
                "  <click:run_command:'/traders help market'><hover:show_text:'<gray>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs'><green>\u25b8 ᴍᴀʀᴋᴇᴛ <dark_gray>\u2014 <gray>dynamic pricing dashboard</hover></click>"
            ))
        }
        if (plugin.citizensBridgeManager != null && sender.hasPermission("sovereign.admin.citizen")) {
            sender.sendMessage(mm.deserialize(
                "  <click:run_command:'/traders help citizen'><hover:show_text:'<gray>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs'><green>\u25b8 ᴄɪᴛɪᴢᴇɴ <dark_gray>\u2014 <gray>link \u00b7 unlink \u00b7 list</hover></click>"
            ))
        }
        sender.sendMessage(mm.deserialize("<dark_gray>ᴛʏᴘᴇ <yellow>/traders help [ᴛᴏᴘɪᴄ] <dark_gray>ꜰᴏʀ ᴅᴇᴛᴀɪʟs \u2014 ᴏʀ ᴄʟɪᴄᴋ ᴀ ᴄᴀᴛᴇɢᴏʀʏ"))
    }

    private fun sendCatalogHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u2014 <green>ᴄᴀᴛᴀʟᴏɢ ᴄᴏᴍᴍᴀɴᴅs"))
        if (sender.hasPermission("sovereign.catalog.list"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders catalog list", "ʟɪsᴛ ᴀʟʟ ᴄᴀᴛᴀʟᴏɢs")))
        if (sender.hasPermission("sovereign.catalog.open"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders catalog open [ɴᴀᴍᴇ]", "ᴏᴘᴇɴ ᴀ ᴄᴀᴛᴀʟᴏɢ")))
        if (sender.hasPermission("sovereign.catalog.edit"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders catalog edit [ɴᴀᴍᴇ]", "ᴇᴅɪᴛ ᴀ ᴄᴀᴛᴀʟᴏɢ (ɢᴜɪ)")))
        if (sender.hasPermission("sovereign.catalog.create"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders catalog create [ɴᴀᴍᴇ]", "ᴄʀᴇᴀᴛᴇ ᴀ ᴄᴀᴛᴀʟᴏɢ")))
        if (sender.hasPermission("sovereign.catalog.delete"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders catalog delete [ɴᴀᴍᴇ]", "ᴅᴇʟᴇᴛᴇ ᴀ ᴄᴀᴛᴀʟᴏɢ")))
        sender.sendMessage(mm.deserialize(helpBack()))
    }

    private fun sendListingHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u2014 <green>ʟɪsᴛɪɴɢ ᴄᴏᴍᴍᴀɴᴅs"))
        if (sender.hasPermission("sovereign.listing.add"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders listing add [ᴄᴀᴛᴀʟᴏɢ] [ʙᴜʏ] [sᴇʟʟ]", "ᴀᴅᴅ ʜᴇʟᴅ ɪᴛᴇᴍ")))
        if (sender.hasPermission("sovereign.listing.remove"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders listing remove [ᴄᴀᴛᴀʟᴏɢ] [sʟᴏᴛ]", "ʀᴇᴍᴏᴠᴇ ʟɪsᴛɪɴɢ")))
        sender.sendMessage(mm.deserialize(helpBack()))
    }

    private fun sendVendorHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u2014 <green>ᴠᴇɴᴅᴏʀ ᴄᴏᴍᴍᴀɴᴅs"))
        if (sender.hasPermission("sovereign.vendor.spawn"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders vendor spawn [ᴘʀᴇsᴇᴛ|sᴋɪɴ] [ᴛʏᴘᴇ]", "sᴘᴀᴡɴ ᴀ ᴠᴇɴᴅᴏʀ ɴᴘᴄ")))
        if (sender.hasPermission("sovereign.vendor.remove")) {
            sender.sendMessage(mm.deserialize(helpEntry("/traders vendor remove", "ʀᴇᴍᴏᴠᴇ ɴᴇᴀʀᴇsᴛ ᴠᴇɴᴅᴏʀ")))
            sender.sendMessage(mm.deserialize(helpEntry("/traders vendor undo", "ʀᴇsᴛᴏʀᴇ ʟᴀsᴛ ʀᴇᴍᴏᴠᴇᴅ ᴠᴇɴᴅᴏʀ")))
        }
        if (sender.hasPermission("sovereign.vendor.link"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders vendor link [ᴄᴀᴛᴀʟᴏɢ]", "ʟɪɴᴋ ᴠᴇɴᴅᴏʀ ᴛᴏ ᴄᴀᴛᴀʟᴏɢ")))
        if (sender.hasPermission("sovereign.vendor.equip"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders vendor equip [sʟᴏᴛ]", "ɢɪᴠᴇ ʜᴇʟᴅ ɪᴛᴇᴍ ᴛᴏ ᴠᴇɴᴅᴏʀ sʟᴏᴛ")))
        if (sender.hasPermission("sovereign.vendor.hologram"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders vendor hologram [ʟɪɴᴇs]", "sᴇᴛ ʜᴏʟᴏɢʀᴀᴍ ᴛᴇxᴛ (|| = ɴᴇᴡʟɪɴᴇ)")))
        sender.sendMessage(mm.deserialize(helpBack()))
    }

    private fun sendAdminHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u2014 <green>ᴀᴅᴍɪɴ ᴄᴏᴍᴍᴀɴᴅs"))
        if (sender.hasPermission("sovereign.admin.reload"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders reload", "ʀᴇʟᴏᴀᴅ ᴄᴏɴꜰɪɢᴜʀᴀᴛɪᴏɴ")))
        if (sender.hasPermission("sovereign.admin.migrate"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders migrate dtl", "ᴍɪɢʀᴀᴛᴇ ꜰʀᴏᴍ ᴅᴛʟᴛʀᴀᴅᴇʀs/ᴘʟᴜs")))
        sender.sendMessage(mm.deserialize(helpBack()))
    }

    private fun sendMarketHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u2014 <green>ᴍᴀʀᴋᴇᴛ ᴄᴏᴍᴍᴀɴᴅs"))
        if (sender.hasPermission("sovereign.admin.market"))
            sender.sendMessage(mm.deserialize(helpEntry("/traders market", "ᴏᴘᴇɴ ᴅʏɴᴀᴍɪᴄ ᴘʀɪᴄɪɴɢ ᴅᴀsʜʙᴏᴀʀᴅ")))
        sender.sendMessage(mm.deserialize(helpBack()))
    }

    private fun sendCitizenHelp(sender: CommandSender) {
        sender.sendMessage(mm.deserialize("<white><bold>sᴏᴠᴇʀᴇɪɢɴ</bold> <gray>\u2014 <green>ᴄɪᴛɪᴢᴇɴ ᴄᴏᴍᴍᴀɴᴅs"))
        if (sender.hasPermission("sovereign.admin.citizen")) {
            sender.sendMessage(mm.deserialize(helpEntry("/traders citizen link [ᴄᴀᴛᴀʟᴏɢ]", "ʟɪɴᴋ ᴄɪᴛɪᴢᴇɴs ɴᴘᴄ ᴛᴏ ᴄᴀᴛᴀʟᴏɢ")))
            sender.sendMessage(mm.deserialize(helpEntry("/traders citizen unlink", "ᴜɴʟɪɴᴋ ᴄɪᴛɪᴢᴇɴs ɴᴘᴄ")))
            sender.sendMessage(mm.deserialize(helpEntry("/traders citizen list", "ʟɪsᴛ ᴀʟʟ ᴄɪᴛɪᴢᴇɴs ʟɪɴᴋs")))
        }
        sender.sendMessage(mm.deserialize(helpBack()))
    }

    private fun helpEntry(command: String, description: String): String {
        val base = command.replace(Regex("\\s*\\[.*"), "")
        val suggest = if (base != command) "$base " else base
        return "  <click:suggest_command:'$suggest'><hover:show_text:'<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴜsᴇ'><yellow>$command <gray>\u2014 $description</hover></click>"
    }

    private fun helpBack(): String {
        return "<click:run_command:'/traders help'><hover:show_text:'<gray>ʙᴀᴄᴋ ᴛᴏ ᴏᴠᴇʀᴠɪᴇᴡ'><dark_gray>\u25c2 <yellow>/traders help <dark_gray>ꜰᴏʀ ᴏᴠᴇʀᴠɪᴇᴡ</hover></click>"
    }

    private fun hasAnyPerm(sender: CommandSender, vararg perms: String): Boolean {
        return perms.any { sender.hasPermission(it) }
    }

    private fun findNearestAssembly(player: Player): net.sovereign.display.VendorAssembly? {
        val origin = player.location
        return plugin.vendorRegistry.allAssemblies().minByOrNull { assembly ->
            val world = origin.world ?: return@minByOrNull Double.MAX_VALUE
            val entity = world.getEntity(assembly.interactionId) ?: return@minByOrNull Double.MAX_VALUE
            val dist = entity.location.distanceSquared(origin)
            if (dist > 25.0) Double.MAX_VALUE else dist
        }?.let { assembly ->
            val world = origin.world ?: return null
            val entity = world.getEntity(assembly.interactionId) ?: return null
            if (entity.location.distanceSquared(origin) <= 25.0) assembly else null
        }
    }

    private fun resolveEntityType(name: String): com.github.retrooper.packetevents.protocol.entity.type.EntityType {
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
}
