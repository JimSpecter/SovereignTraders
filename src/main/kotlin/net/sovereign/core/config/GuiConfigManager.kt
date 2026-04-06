package net.sovereign.core.config

import net.sovereign.core.SovereignCore
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class GuiConfigManager(private val plugin: SovereignCore) {

    private val configFile: File = File(plugin.dataFolder, "guis.yml")
    var configuration: FileConfiguration private set

    init {
        plugin.saveResource("guis.yml", false)
        configuration = YamlConfiguration.loadConfiguration(configFile)
    }

    fun reload() {
        configuration = YamlConfiguration.loadConfiguration(configFile)
    }

    val catalogFillerMaterial: String
        get() = configuration.getString("catalog.filler-material", "BLACK_STAINED_GLASS_PANE") ?: "BLACK_STAINED_GLASS_PANE"

    val clickAcquire: String
        get() = configuration.getString("catalog.click-acquire", "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ᴛᴏ ᴀᴄǫᴜɪʀᴇ") ?: "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ᴛᴏ ᴀᴄǫᴜɪʀᴇ"

    val clickLiquidate: String
        get() = configuration.getString("catalog.click-liquidate", "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ᴛᴏ ʟɪǫᴜɪᴅᴀᴛᴇ") ?: "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ᴛᴏ ʟɪǫᴜɪᴅᴀᴛᴇ"

    val clickBarter: String
        get() = configuration.getString("catalog.click-barter", "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ᴛᴏ ʙᴀʀᴛᴇʀ") ?: "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ᴛᴏ ʙᴀʀᴛᴇʀ"

    val shiftClickHint: String
        get() = configuration.getString("catalog.shift-click-hint", "<dark_gray>◆ <aqua>sʜɪꜰᴛ-ᴄʟɪᴄᴋ ꜰᴏʀ ʙᴜʟᴋ") ?: "<dark_gray>◆ <aqua>sʜɪꜰᴛ-ᴄʟɪᴄᴋ ꜰᴏʀ ʙᴜʟᴋ"

    val barterRequiredLabel: String
        get() = configuration.getString("listing.barter-required-label", "<red>ʀᴇǫᴜɪʀᴇᴅ:") ?: "<red>ʀᴇǫᴜɪʀᴇᴅ:"

    val barterReceiveLabel: String
        get() = configuration.getString("listing.barter-receive-label", "<green>ʀᴇᴄᴇɪᴠᴇ:") ?: "<green>ʀᴇᴄᴇɪᴠᴇ:"

    val reductionBadge: String
        get() = configuration.getString("listing.reduction-badge", "<light_purple>⧖ ʟɪᴍɪᴛᴇᴅ ᴏꜰꜰᴇʀ") ?: "<light_purple>⧖ ʟɪᴍɪᴛᴇᴅ ᴏꜰꜰᴇʀ"

    val catalogReductionBadge: String
        get() = configuration.getString("listing.catalog-reduction-badge", "<light_purple>⧖ sᴛᴏʀᴇᴡɪᴅᴇ sᴀʟᴇ") ?: "<light_purple>⧖ sᴛᴏʀᴇᴡɪᴅᴇ sᴀʟᴇ"

    val confirmationEnabled: Boolean
        get() = configuration.getBoolean("confirmation.enabled", true)

    val confirmationTitle: String
        get() = configuration.getString("confirmation.title", "%item% - ᴄᴏɴꜰɪʀᴍ - %mode%") ?: "%item% - ᴄᴏɴꜰɪʀᴍ - %mode%"

    val confirmationConfirmMaterial: String
        get() = configuration.getString("confirmation.confirm-material", "LIME_STAINED_GLASS_PANE") ?: "LIME_STAINED_GLASS_PANE"

    val confirmationConfirmLabel: String
        get() = configuration.getString("confirmation.confirm-label", "<green><bold>✔ ᴄᴏɴꜰɪʀᴍ") ?: "<green><bold>✔ ᴄᴏɴꜰɪʀᴍ"

    val confirmationCancelMaterial: String
        get() = configuration.getString("confirmation.cancel-material", "RED_STAINED_GLASS_PANE") ?: "RED_STAINED_GLASS_PANE"

    val confirmationCancelLabel: String
        get() = configuration.getString("confirmation.cancel-label", "<red><bold>✘ ᴄᴀɴᴄᴇʟ") ?: "<red><bold>✘ ᴄᴀɴᴄᴇʟ"

    val confirmationFillerMaterial: String
        get() = configuration.getString("confirmation.filler-material", "BLACK_STAINED_GLASS_PANE") ?: "BLACK_STAINED_GLASS_PANE"

    val quantitySelectorEnabled: Boolean
        get() = configuration.getBoolean("quantity-selector.enabled", true)

    val quantitySelectorTitle: String
        get() = configuration.getString("quantity-selector.title", "%item% - sᴇʟᴇᴄᴛ ǫᴜᴀɴᴛɪᴛʏ") ?: "%item% - sᴇʟᴇᴄᴛ ǫᴜᴀɴᴛɪᴛʏ"

    val quantitySelectorMaterial: String
        get() = configuration.getString("quantity-selector.preset-material", "LIGHT_BLUE_STAINED_GLASS_PANE") ?: "LIGHT_BLUE_STAINED_GLASS_PANE"

    val quantitySelectorSelectedMaterial: String
        get() = configuration.getString("quantity-selector.selected-material", "LIME_STAINED_GLASS_PANE") ?: "LIME_STAINED_GLASS_PANE"

    val quantitySelectorCancelMaterial: String
        get() = configuration.getString("quantity-selector.cancel-material", "RED_STAINED_GLASS_PANE") ?: "RED_STAINED_GLASS_PANE"

    val quantitySelectorCancelLabel: String
        get() = configuration.getString("quantity-selector.cancel-label", "<red><bold>✘ ᴄᴀɴᴄᴇʟ") ?: "<red><bold>✘ ᴄᴀɴᴄᴇʟ"

    val quantitySelectorFillerMaterial: String
        get() = configuration.getString("quantity-selector.filler-material", "BLACK_STAINED_GLASS_PANE") ?: "BLACK_STAINED_GLASS_PANE"

    val editorTitle: String
        get() = configuration.getString("editor.title",
            "<white><bold>ᴇᴅɪᴛᴏʀ</bold> <gray>» <yellow>%catalog% <gray>[<white>%mode%<gray>]"
        ) ?: "<white><bold>ᴇᴅɪᴛᴏʀ</bold> <gray>» <yellow>%catalog% <gray>[<white>%mode%<gray>]"

    val editorPlaceholderMaterial: String
        get() = configuration.getString("editor.placeholder-material", "LIME_STAINED_GLASS_PANE") ?: "LIME_STAINED_GLASS_PANE"

    val editorPlaceholderName: String
        get() = configuration.getString("editor.placeholder-name", "<green><bold>+ ᴀᴅᴅ ɪᴛᴇᴍ") ?: "<green><bold>+ ᴀᴅᴅ ɪᴛᴇᴍ"

    val editorPlaceholderLore: List<String>
        get() = configuration.getStringList("editor.placeholder-lore").ifEmpty {
            listOf("<gray>ᴄʟɪᴄᴋ ᴡɪᴛʜ ɪᴛᴇᴍ ɪɴ ʜᴀɴᴅ", "<gray>ᴛᴏ ᴀᴅᴅ ɪᴛ ᴛᴏ ᴛʜɪs sʟᴏᴛ")
        }

    val editorFillerMaterial: String
        get() = configuration.getString("editor.filler-material", "GRAY_STAINED_GLASS_PANE") ?: "GRAY_STAINED_GLASS_PANE"

    val editorModeName: String
        get() = configuration.getString("editor.mode-name", "<yellow>ᴍᴏᴅᴇ: <white>%mode%") ?: "<yellow>ᴍᴏᴅᴇ: <white>%mode%"

    val editorModeLore: String
        get() = configuration.getString("editor.mode-lore", "<gray>ᴄʟɪᴄᴋ ᴛᴏ ᴄʏᴄʟᴇ ᴍᴏᴅᴇs") ?: "<gray>ᴄʟɪᴄᴋ ᴛᴏ ᴄʏᴄʟᴇ ᴍᴏᴅᴇs"

    val editorSaveMaterial: String
        get() = configuration.getString("editor.save-material", "EMERALD") ?: "EMERALD"

    val editorSaveName: String
        get() = configuration.getString("editor.save-name", "<green><bold>sᴀᴠᴇ & ᴄʟᴏsᴇ") ?: "<green><bold>sᴀᴠᴇ & ᴄʟᴏsᴇ"

    val editorSaveLore: String
        get() = configuration.getString("editor.save-lore", "<gray>ᴘᴇʀsɪsᴛs ᴀʟʟ ᴄʜᴀɴɢᴇs") ?: "<gray>ᴘᴇʀsɪsᴛs ᴀʟʟ ᴄʜᴀɴɢᴇs"

    val editorQuotaMaterial: String
        get() = configuration.getString("editor.quota-material", "CLOCK") ?: "CLOCK"

    val editorQuotaName: String
        get() = configuration.getString("editor.quota-name", "<aqua><bold>ǫᴜᴏᴛᴀ") ?: "<aqua><bold>ǫᴜᴏᴛᴀ"

    val editorQuotaLore: String
        get() = configuration.getString("editor.quota-lore", "<gray>sᴇᴛ ǫᴜᴏᴛᴀ ꜰᴏʀ ᴀʟʟ ʟɪsᴛɪɴɢs ɪɴ ᴛʜɪs ᴍᴏᴅᴇ") ?: "<gray>sᴇᴛ ǫᴜᴏᴛᴀ ꜰᴏʀ ᴀʟʟ ʟɪsᴛɪɴɢs ɪɴ ᴛʜɪs ᴍᴏᴅᴇ"

    val editorAddMaterial: String
        get() = configuration.getString("editor.add-material", "HOPPER") ?: "HOPPER"

    val editorAddName: String
        get() = configuration.getString("editor.add-name", "<aqua><bold>ᴀᴅᴅ ꜰʀᴏᴍ ʜᴀɴᴅ") ?: "<aqua><bold>ᴀᴅᴅ ꜰʀᴏᴍ ʜᴀɴᴅ"

    val editorAddLore: List<String>
        get() = configuration.getStringList("editor.add-lore").ifEmpty {
            listOf("<gray>ᴜɴᴜsᴇᴅ")
        }

    val editorListingLore: List<String>
        get() = configuration.getStringList("editor.listing-lore").ifEmpty {
            listOf(
                "<dark_gray>ᴜɪᴅ: %uid%",
                "<green>ʙᴜʏ: %buy%",
                "<gold>sᴇʟʟ: %sell%",
                "<gray>⛃ ǫᴜᴏᴛᴀ: <white>%quota% <dark_gray>| <gray>ʀᴇsᴇᴛ: <white>%reset%",
                "",
                "<yellow>ʟᴇꜰᴛ-ᴄʟɪᴄᴋ → ᴇᴅɪᴛ ᴘʀɪᴄᴇs",
                "<aqua>sʜɪꜰᴛ-ᴄʟɪᴄᴋ → ᴇᴅɪᴛ ǫᴜᴏᴛᴀ",
                "<red>ʀɪɢʜᴛ-ᴄʟɪᴄᴋ → ʀᴇᴍᴏᴠᴇ"
            )
        }

    val retreatSlots: List<Int>
        get() = configuration.getIntegerList("navigation.retreat.slots")

    val retreatMaterial: String
        get() = configuration.getString("navigation.retreat.material", "EMERALD_BLOCK") ?: "EMERALD_BLOCK"

    val retreatLabel: String
        get() = configuration.getString("navigation.retreat.label", "<green>ᴘʀᴇᴠɪᴏᴜs") ?: "<green>ᴘʀᴇᴠɪᴏᴜs"

    val advanceSlots: List<Int>
        get() = configuration.getIntegerList("navigation.advance.slots")

    val advanceMaterial: String
        get() = configuration.getString("navigation.advance.material", "EMERALD_BLOCK") ?: "EMERALD_BLOCK"

    val advanceLabel: String
        get() = configuration.getString("navigation.advance.label", "<green>ɴᴇxᴛ") ?: "<green>ɴᴇxᴛ"

    val dismissSlots: List<Int>
        get() = configuration.getIntegerList("navigation.dismiss.slots")

    val dismissMaterial: String
        get() = configuration.getString("navigation.dismiss.material", "REDSTONE_BLOCK") ?: "REDSTONE_BLOCK"

    val dismissLabel: String
        get() = configuration.getString("navigation.dismiss.label", "<red>ᴄʟᴏsᴇ") ?: "<red>ᴄʟᴏsᴇ"

    val modeToggleSlot: Int
        get() = configuration.getInt("mode-toggle.slot", 8)

    val acquireMaterial: String
        get() = configuration.getString("mode-toggle.acquire.material", "GOLDEN_APPLE") ?: "GOLDEN_APPLE"

    val acquireLabel: String
        get() = configuration.getString("mode-toggle.acquire.label", "<green>ᴀᴄǫᴜɪʀᴇ") ?: "<green>ᴀᴄǫᴜɪʀᴇ"

    val liquidateMaterial: String
        get() = configuration.getString("mode-toggle.liquidate.material", "STONE") ?: "STONE"

    val liquidateLabel: String
        get() = configuration.getString("mode-toggle.liquidate.label", "<gold>ʟɪǫᴜɪᴅᴀᴛᴇ") ?: "<gold>ʟɪǫᴜɪᴅᴀᴛᴇ"

    val barterMaterial: String
        get() = configuration.getString("mode-toggle.barter.material", "REDSTONE_BLOCK") ?: "REDSTONE_BLOCK"

    val barterLabel: String
        get() = configuration.getString("mode-toggle.barter.label", "<aqua>ʙᴀʀᴛᴇʀ") ?: "<aqua>ʙᴀʀᴛᴇʀ"

    val dashboardOverviewTitle: String
        get() = configuration.getString("dashboard.overview.title",
            "<white><bold>sᴠᴛ ᴍᴀʀᴋᴇᴛ"
        ) ?: "<white><bold>sᴠᴛ ᴍᴀʀᴋᴇᴛ"

    val dashboardOverviewFillerMaterial: String
        get() = configuration.getString("dashboard.overview.filler-material", "BLACK_STAINED_GLASS_PANE") ?: "BLACK_STAINED_GLASS_PANE"

    val dashboardPauseActiveMaterial: String
        get() = configuration.getString("dashboard.overview.pause-active-material", "RED_DYE") ?: "RED_DYE"

    val dashboardPauseActiveLabel: String
        get() = configuration.getString("dashboard.overview.pause-active-label", "<red><bold>⏸ ᴘᴀᴜsᴇ") ?: "<red><bold>⏸ ᴘᴀᴜsᴇ"

    val dashboardPauseActiveLore: List<String>
        get() = configuration.getStringList("dashboard.overview.pause-active-lore").ifEmpty {
            listOf("<gray>ᴛᴏɢɢʟᴇ ᴅʏɴᴀᴍɪᴄ ᴘʀɪᴄɪɴɢ", "<yellow>ᴄᴜʀʀᴇɴᴛʟʏ ᴀᴄᴛɪᴠᴇ")
        }

    val dashboardPausePausedMaterial: String
        get() = configuration.getString("dashboard.overview.pause-paused-material", "LIME_DYE") ?: "LIME_DYE"

    val dashboardPausePausedLabel: String
        get() = configuration.getString("dashboard.overview.pause-paused-label", "<green><bold>▶ ʀᴇsᴜᴍᴇ") ?: "<green><bold>▶ ʀᴇsᴜᴍᴇ"

    val dashboardPausePausedLore: List<String>
        get() = configuration.getStringList("dashboard.overview.pause-paused-lore").ifEmpty {
            listOf("<gray>ᴛᴏɢɢʟᴇ ᴅʏɴᴀᴍɪᴄ ᴘʀɪᴄɪɴɢ", "<green>ᴄᴜʀʀᴇɴᴛʟʏ ᴘᴀᴜsᴇᴅ")
        }

    val dashboardPanicMaterial: String
        get() = configuration.getString("dashboard.overview.panic-material", "TNT") ?: "TNT"

    val dashboardPanicLabel: String
        get() = configuration.getString("dashboard.overview.panic-label", "<red><bold>☠ ᴘᴀɴɪᴄ ʀᴇsᴇᴛ") ?: "<red><bold>☠ ᴘᴀɴɪᴄ ʀᴇsᴇᴛ"

    val dashboardPanicLore: List<String>
        get() = configuration.getStringList("dashboard.overview.panic-lore").ifEmpty {
            listOf("<gray>ʀᴇsᴇᴛ ᴀʟʟ ᴘʀɪᴄᴇs ᴛᴏ ʙᴀsᴇ", "<red>⚠ ᴛʜɪs ᴄᴀɴɴᴏᴛ ʙᴇ ᴜɴᴅᴏɴᴇ")
        }

    val dashboardOverviewCloseMaterial: String
        get() = configuration.getString("dashboard.overview.close-material", "BARRIER") ?: "BARRIER"

    val dashboardOverviewCloseLabel: String
        get() = configuration.getString("dashboard.overview.close-label", "<red><bold>✘ ᴄʟᴏsᴇ") ?: "<red><bold>✘ ᴄʟᴏsᴇ"

    val dashboardOverviewInfoMaterial: String
        get() = configuration.getString("dashboard.overview.info-material", "BOOK") ?: "BOOK"

    val dashboardOverviewInfoLabel: String
        get() = configuration.getString("dashboard.overview.info-label", "<aqua><bold>ɪɴꜰᴏ") ?: "<aqua><bold>ɪɴꜰᴏ"

    val dashboardOverviewInfoLore: List<String>
        get() = configuration.getStringList("dashboard.overview.info-lore").ifEmpty {
            listOf(
                "<gray>ᴛʀᴀᴄᴋᴇᴅ: <white>%tracked% <dark_gray>| <gray>ᴠɪsɪʙʟᴇ: <white>%visible%",
                "<gray>ᴘᴀɢᴇ: <white>%page%<gray>/<white>%pages%",
                "<gray>ᴀᴠɢ ᴍᴜʟᴛɪᴘʟɪᴇʀ: <white>%avg_multiplier%x",
                "<gray>ꜰʀᴏᴢᴇɴ: <aqua>%frozen% <dark_gray>| <gray>ᴏᴠᴇʀʀɪᴅᴅᴇɴ: <light_purple>%overridden%",
                "<gray>sᴛᴀᴛᴇ: <white>%state%",
                "",
                "<gray>ᴅʏɴᴀᴍɪᴄ ᴘʀɪᴄɪɴɢ ʀᴀɪsᴇs ᴘʀɪᴄᴇs",
                "<gray>ᴡʜᴇɴ ᴅᴇᴍᴀɴᴅ ɪɴᴄʀᴇᴀsᴇs, ᴀɴᴅ",
                "<gray>ʟᴏᴡᴇʀs ᴛʜᴇᴍ ᴡʜᴇɴ sᴜᴘᴘʟʏ ʙᴜɪʟᴅs"
            )
        }

    val dashboardOverviewSearchMaterial: String
        get() = configuration.getString("dashboard.overview.search-material", "NAME_TAG") ?: "NAME_TAG"

    val dashboardOverviewSearchLabel: String
        get() = configuration.getString("dashboard.overview.search-label", "<white><bold>sᴇᴀʀᴄʜ") ?: "<white><bold>sᴇᴀʀᴄʜ"

    val dashboardOverviewSearchLore: List<String>
        get() = configuration.getStringList("dashboard.overview.search-lore").ifEmpty {
            listOf(
                "<gray>ǫᴜᴇʀʏ: <white>%query%",
                "<yellow>ᴄʟɪᴄᴋ → sᴇᴛ sᴇᴀʀᴄʜ",
                "<red>sʜɪꜰᴛ-ᴄʟɪᴄᴋ → ᴄʟᴇᴀʀ"
            )
        }

    val dashboardOverviewFilterMaterial: String
        get() = configuration.getString("dashboard.overview.filter-material", "HOPPER") ?: "HOPPER"

    val dashboardOverviewFilterLabel: String
        get() = configuration.getString("dashboard.overview.filter-label", "<gold><bold>ꜰɪʟᴛᴇʀ") ?: "<gold><bold>ꜰɪʟᴛᴇʀ"

    val dashboardOverviewFilterLore: List<String>
        get() = configuration.getStringList("dashboard.overview.filter-lore").ifEmpty {
            listOf(
                "<gray>ᴄᴜʀʀᴇɴᴛ: <white>%filter%",
                "<yellow>ᴄʟɪᴄᴋ → ᴄʏᴄʟᴇ"
            )
        }

    val dashboardOverviewSortMaterial: String
        get() = configuration.getString("dashboard.overview.sort-material", "COMPASS") ?: "COMPASS"

    val dashboardOverviewSortLabel: String
        get() = configuration.getString("dashboard.overview.sort-label", "<green><bold>sᴏʀᴛ") ?: "<green><bold>sᴏʀᴛ"

    val dashboardOverviewSortLore: List<String>
        get() = configuration.getStringList("dashboard.overview.sort-lore").ifEmpty {
            listOf(
                "<gray>ᴄᴜʀʀᴇɴᴛ: <white>%sort%",
                "<yellow>ᴄʟɪᴄᴋ → ᴄʏᴄʟᴇ"
            )
        }

    val dashboardDetailTitle: String
        get() = configuration.getString("dashboard.detail.title",
            "<white><bold>sᴠᴛ ᴍᴀʀᴋᴇᴛ</bold> <gray>» <yellow>#%uid%"
        ) ?: "<white><bold>sᴠᴛ ᴍᴀʀᴋᴇᴛ</bold> <gray>» <yellow>#%uid%"

    val dashboardDetailFillerMaterial: String
        get() = configuration.getString("dashboard.detail.filler-material", "BLACK_STAINED_GLASS_PANE") ?: "BLACK_STAINED_GLASS_PANE"

    val dashboardStatsMaterial: String
        get() = configuration.getString("dashboard.detail.stats-material", "COMPASS") ?: "COMPASS"

    val dashboardStatsLabel: String
        get() = configuration.getString("dashboard.detail.stats-label", "<white><bold>ᴀᴄᴛɪᴠɪᴛʏ") ?: "<white><bold>ᴀᴄᴛɪᴠɪᴛʏ"

    val dashboardSparklineUpMaterial: String
        get() = configuration.getString("dashboard.detail.sparkline-up-material", "LIME_STAINED_GLASS_PANE") ?: "LIME_STAINED_GLASS_PANE"

    val dashboardSparklineUpLabel: String
        get() = configuration.getString("dashboard.detail.sparkline-up-label", "<green>▲ ᴘʀɪᴄᴇ ᴜᴘ") ?: "<green>▲ ᴘʀɪᴄᴇ ᴜᴘ"

    val dashboardSparklineDownMaterial: String
        get() = configuration.getString("dashboard.detail.sparkline-down-material", "RED_STAINED_GLASS_PANE") ?: "RED_STAINED_GLASS_PANE"

    val dashboardSparklineDownLabel: String
        get() = configuration.getString("dashboard.detail.sparkline-down-label", "<red>▼ ᴘʀɪᴄᴇ ᴅᴏᴡɴ") ?: "<red>▼ ᴘʀɪᴄᴇ ᴅᴏᴡɴ"

    val dashboardSparklineStableMaterial: String
        get() = configuration.getString("dashboard.detail.sparkline-stable-material", "LIGHT_GRAY_STAINED_GLASS_PANE") ?: "LIGHT_GRAY_STAINED_GLASS_PANE"

    val dashboardSparklineStableLabel: String
        get() = configuration.getString("dashboard.detail.sparkline-stable-label", "<gray>— sᴛᴀʙʟᴇ") ?: "<gray>— sᴛᴀʙʟᴇ"

    val dashboardPeakMaterial: String
        get() = configuration.getString("dashboard.detail.peak-material", "GOLD_INGOT") ?: "GOLD_INGOT"

    val dashboardPeakLabel: String
        get() = configuration.getString("dashboard.detail.peak-label", "<gold><bold>ᴘᴇᴀᴋ ᴘʀɪᴄᴇ") ?: "<gold><bold>ᴘᴇᴀᴋ ᴘʀɪᴄᴇ"

    val dashboardLowestMaterial: String
        get() = configuration.getString("dashboard.detail.lowest-material", "IRON_INGOT") ?: "IRON_INGOT"

    val dashboardLowestLabel: String
        get() = configuration.getString("dashboard.detail.lowest-label", "<aqua><bold>ʟᴏᴡᴇsᴛ ᴘʀɪᴄᴇ") ?: "<aqua><bold>ʟᴏᴡᴇsᴛ ᴘʀɪᴄᴇ"

    val dashboardFreezeFrozenMaterial: String
        get() = configuration.getString("dashboard.detail.freeze-frozen-material", "PACKED_ICE") ?: "PACKED_ICE"

    val dashboardFreezeFrozenLabel: String
        get() = configuration.getString("dashboard.detail.freeze-frozen-label", "<green><bold>❄ ᴜɴꜰʀᴇᴇᴢᴇ") ?: "<green><bold>❄ ᴜɴꜰʀᴇᴇᴢᴇ"

    val dashboardFreezeFrozenLore: String
        get() = configuration.getString("dashboard.detail.freeze-frozen-lore", "<green>ᴜɴꜰʀᴇᴇᴢᴇ ᴛʜɪs ʟɪsᴛɪɴɢ") ?: "<green>ᴜɴꜰʀᴇᴇᴢᴇ ᴛʜɪs ʟɪsᴛɪɴɢ"

    val dashboardFreezeActiveMaterial: String
        get() = configuration.getString("dashboard.detail.freeze-active-material", "BLUE_ICE") ?: "BLUE_ICE"

    val dashboardFreezeActiveLabel: String
        get() = configuration.getString("dashboard.detail.freeze-active-label", "<aqua><bold>❄ ꜰʀᴇᴇᴢᴇ") ?: "<aqua><bold>❄ ꜰʀᴇᴇᴢᴇ"

    val dashboardFreezeActiveLore: String
        get() = configuration.getString("dashboard.detail.freeze-active-lore", "<gray>ꜰʀᴇᴇᴢᴇ ᴀᴛ ᴄᴜʀʀᴇɴᴛ ᴘʀɪᴄᴇ") ?: "<gray>ꜰʀᴇᴇᴢᴇ ᴀᴛ ᴄᴜʀʀᴇɴᴛ ᴘʀɪᴄᴇ"

    val dashboardResetMaterial: String
        get() = configuration.getString("dashboard.detail.reset-material", "SUNFLOWER") ?: "SUNFLOWER"

    val dashboardResetLabel: String
        get() = configuration.getString("dashboard.detail.reset-label", "<yellow><bold>↺ ʀᴇsᴇᴛ") ?: "<yellow><bold>↺ ʀᴇsᴇᴛ"

    val dashboardResetLore: List<String>
        get() = configuration.getStringList("dashboard.detail.reset-lore").ifEmpty {
            listOf("<gray>sɴᴀᴘ ʙᴀᴄᴋ ᴛᴏ ʙᴀsᴇ ᴘʀɪᴄᴇ", "<gray>ᴄʟᴇᴀʀs ᴀᴄᴄᴜᴍᴜʟᴀᴛᴇᴅ ᴘʀᴇssᴜʀᴇ")
        }

    val dashboardOverrideMaterial: String
        get() = configuration.getString("dashboard.detail.override-material", "ANVIL") ?: "ANVIL"

    val dashboardOverrideLabel: String
        get() = configuration.getString("dashboard.detail.override-label", "<light_purple><bold>⚙ ᴏᴠᴇʀʀɪᴅᴇ") ?: "<light_purple><bold>⚙ ᴏᴠᴇʀʀɪᴅᴇ"

    val dashboardOverrideLore: List<String>
        get() = configuration.getStringList("dashboard.detail.override-lore").ifEmpty {
            listOf("<gray>sᴇᴛ ᴀ ᴍᴀɴᴜᴀʟ ᴘʀɪᴄᴇ ᴍᴜʟᴛɪᴘʟɪᴇʀ", "<gray>ᴛʏᴘᴇ ᴀ ᴍᴜʟᴛɪᴘʟɪᴇʀ ɪɴ ᴄʜᴀᴛ (ᴇ.ɢ. 1.5)")
        }

    val dashboardClearOverrideMaterial: String
        get() = configuration.getString("dashboard.detail.clear-override-material", "STRUCTURE_VOID") ?: "STRUCTURE_VOID"

    val dashboardClearOverrideLabel: String
        get() = configuration.getString("dashboard.detail.clear-override-label", "<red><bold>✘ ᴄʟᴇᴀʀ ᴏᴠᴇʀʀɪᴅᴇ") ?: "<red><bold>✘ ᴄʟᴇᴀʀ ᴏᴠᴇʀʀɪᴅᴇ"

    val dashboardClearOverrideLore: String
        get() = configuration.getString("dashboard.detail.clear-override-lore", "<gray>ʀᴇᴍᴏᴠᴇ ᴍᴀɴᴜᴀʟ ᴏᴠᴇʀʀɪᴅᴇ") ?: "<gray>ʀᴇᴍᴏᴠᴇ ᴍᴀɴᴜᴀʟ ᴏᴠᴇʀʀɪᴅᴇ"

    val dashboardDetailBackMaterial: String
        get() = configuration.getString("dashboard.detail.back-material", "ARROW") ?: "ARROW"

    val dashboardDetailBackLabel: String
        get() = configuration.getString("dashboard.detail.back-label", "<white><bold>← ʙᴀᴄᴋ") ?: "<white><bold>← ʙᴀᴄᴋ"

    val dashboardDetailCloseMaterial: String
        get() = configuration.getString("dashboard.detail.close-material", "BARRIER") ?: "BARRIER"

    val dashboardDetailCloseLabel: String
        get() = configuration.getString("dashboard.detail.close-label", "<red><bold>✘ ᴄʟᴏsᴇ") ?: "<red><bold>✘ ᴄʟᴏsᴇ"

    val dashboardDetailPreviewLabel: String
        get() = configuration.getString("dashboard.detail.preview-label", "<white><bold>ʟɪsᴛɪɴɢ #%uid%") ?: "<white><bold>ʟɪsᴛɪɴɢ #%uid%"

    val dashboardDetailCurrentMaterial: String
        get() = configuration.getString("dashboard.detail.current-material", "GOLD_INGOT") ?: "GOLD_INGOT"

    val dashboardDetailCurrentLabel: String
        get() = configuration.getString("dashboard.detail.current-label", "<gold><bold>ᴄᴜʀʀᴇɴᴛ ᴘʀɪᴄᴇ") ?: "<gold><bold>ᴄᴜʀʀᴇɴᴛ ᴘʀɪᴄᴇ"

    val dashboardDetailCurrentLore: List<String>
        get() = configuration.getStringList("dashboard.detail.current-lore").ifEmpty {
            listOf(
                "<gray>ʙᴀsᴇ: <yellow>$%base%",
                "<gray>ᴍᴜʟᴛɪᴘʟɪᴇʀ: <white>%multiplier%x",
                "<gray>sʜɪꜰᴛ: <white>%shift%%",
                "<gray>ᴇꜰꜰᴇᴄᴛɪᴠᴇ: <gold>$%effective%",
                "",
                "<gray>ᴘᴇᴀᴋ: <yellow>$%peak%",
                "<gray>ʟᴏᴡᴇsᴛ: <aqua>$%lowest%"
            )
        }

    val dashboardDetailStatusMaterial: String
        get() = configuration.getString("dashboard.detail.status-material", "COMPARATOR") ?: "COMPARATOR"

    val dashboardDetailStatusLabel: String
        get() = configuration.getString("dashboard.detail.status-label", "<white><bold>sᴛᴀᴛᴜs") ?: "<white><bold>sᴛᴀᴛᴜs"

    val dashboardDetailStatusLore: List<String>
        get() = configuration.getStringList("dashboard.detail.status-lore").ifEmpty {
            listOf(
                "<gray>ɢʟᴏʙᴀʟ: <white>%global_state%",
                "<gray>ʟɪsᴛɪɴɢ: <white>%listing_state%",
                "<gray>ᴏᴠᴇʀʀɪᴅᴇ: <white>%override%",
                "<gray>ʟᴀsᴛ ᴄʜᴀɴɢᴇ: <white>%last_change%"
            )
        }

    val dashboardDetailHelpMaterial: String
        get() = configuration.getString("dashboard.detail.help-material", "BOOK") ?: "BOOK"

    val dashboardDetailHelpLabel: String
        get() = configuration.getString("dashboard.detail.help-label", "<aqua><bold>ʜᴇʟᴘ") ?: "<aqua><bold>ʜᴇʟᴘ"

    val dashboardDetailHelpLore: List<String>
        get() = configuration.getStringList("dashboard.detail.help-lore").ifEmpty {
            listOf(
                "<gray>❄ ꜰʀᴇᴇᴢᴇ ʟᴏᴄᴋs ᴛʜᴇ ᴄᴜʀʀᴇɴᴛ",
                "<gray>  ᴍᴜʟᴛɪᴘʟɪᴇʀ ɪɴ ᴘʟᴀᴄᴇ",
                "<gray>↺ ʀᴇsᴇᴛ ᴄʟᴇᴀʀs ᴀᴄᴄᴜᴍᴜʟᴀᴛᴇᴅ",
                "<gray>  ᴠᴏʟᴜᴍᴇ ᴘʀᴇssᴜʀᴇ",
                "<gray>⚙ ᴏᴠᴇʀʀɪᴅᴇ sᴇᴛs ᴀ ᴍᴀɴᴜᴀʟ",
                "<gray>  ᴍᴜʟᴛɪᴘʟɪᴇʀ ᴠɪᴀ ᴄʜᴀᴛ ɪɴᴘᴜᴛ"
            )
        }

    val loreFrozenBadge: String
        get() = configuration.getString("dashboard.lore.frozen-badge", "<aqua>❄ ꜰʀᴏᴢᴇɴ") ?: "<aqua>❄ ꜰʀᴏᴢᴇɴ"

    val loreOverriddenBadge: String
        get() = configuration.getString("dashboard.lore.overridden-badge", "<light_purple>⚙ ᴏᴠᴇʀʀɪᴅᴅᴇɴ") ?: "<light_purple>⚙ ᴏᴠᴇʀʀɪᴅᴅᴇɴ"

    val lorePausedBadge: String
        get() = configuration.getString("dashboard.lore.paused-badge", "<red>⏸ ɢʟᴏʙᴀʟʟʏ ᴘᴀᴜsᴇᴅ") ?: "<red>⏸ ɢʟᴏʙᴀʟʟʏ ᴘᴀᴜsᴇᴅ"

    val loreClickDetail: String
        get() = configuration.getString("dashboard.lore.click-detail", "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs") ?: "<dark_gray>◆ <white>ᴄʟɪᴄᴋ ꜰᴏʀ ᴅᴇᴛᴀɪʟs"

    val loreMultiplierLine: String
        get() = configuration.getString("dashboard.lore.multiplier-line",
            "<gray>ᴍᴜʟᴛɪᴘʟɪᴇʀ: <white>%multiplier%x %trend_color%%trend% %sign%%shift%%"
        ) ?: "<gray>ᴍᴜʟᴛɪᴘʟɪᴇʀ: <white>%multiplier%x %trend_color%%trend% %sign%%shift%%"

    val loreBasePriceLine: String
        get() = configuration.getString("dashboard.lore.base-price-line", "<gray>ʙᴀsᴇ ᴘʀɪᴄᴇ: <yellow>$%base%") ?: "<gray>ʙᴀsᴇ ᴘʀɪᴄᴇ: <yellow>$%base%"

    val loreEffectiveLine: String
        get() = configuration.getString("dashboard.lore.effective-line", "<gray>ᴇꜰꜰᴇᴄᴛɪᴠᴇ: <gold>$%effective%") ?: "<gray>ᴇꜰꜰᴇᴄᴛɪᴠᴇ: <gold>$%effective%"

    val loreVolumeLine: String
        get() = configuration.getString("dashboard.lore.volume-line",
            "<gray>ᴠᴏʟᴜᴍᴇ: <green>%buys% ʙᴏᴜɢʜᴛ <dark_gray>/ <red>%sells% sᴏʟᴅ"
        ) ?: "<gray>ᴠᴏʟᴜᴍᴇ: <green>%buys% ʙᴏᴜɢʜᴛ <dark_gray>/ <red>%sells% sᴏʟᴅ"

    val loreDemandLine: String
        get() = configuration.getString("dashboard.lore.demand-line", "<gray>ᴅᴇᴍᴀɴᴅ sᴄᴏʀᴇ: <yellow>%score%x") ?: "<gray>ᴅᴇᴍᴀɴᴅ sᴄᴏʀᴇ: <yellow>%score%x"

    val loreSupplyLine: String
        get() = configuration.getString("dashboard.lore.supply-line", "<gray>sᴜᴘᴘʟʏ ᴘʀᴇssᴜʀᴇ: <aqua>%pressure%x") ?: "<gray>sᴜᴘᴘʟʏ ᴘʀᴇssᴜʀᴇ: <aqua>%pressure%x"

    val loreStatsNameLine: String
        get() = configuration.getString("dashboard.lore.stats-name-line", "<gray>ɴᴀᴍᴇ: <white>%name%") ?: "<gray>ɴᴀᴍᴇ: <white>%name%"

    val loreStatsBuysLine: String
        get() = configuration.getString("dashboard.lore.stats-buys-line", "<gray>ᴛᴏᴛᴀʟ ʙᴜʏs: <green>%buys%") ?: "<gray>ᴛᴏᴛᴀʟ ʙᴜʏs: <green>%buys%"

    val loreStatsSellsLine: String
        get() = configuration.getString("dashboard.lore.stats-sells-line", "<gray>ᴛᴏᴛᴀʟ sᴇʟʟs: <red>%sells%") ?: "<gray>ᴛᴏᴛᴀʟ sᴇʟʟs: <red>%sells%"

    val loreStatsDemandLine: String
        get() = configuration.getString("dashboard.lore.stats-demand-line", "<gray>ᴅᴇᴍᴀɴᴅ sᴄᴏʀᴇ: <yellow>%score%x") ?: "<gray>ᴅᴇᴍᴀɴᴅ sᴄᴏʀᴇ: <yellow>%score%x"

    val loreStatsSupplyLine: String
        get() = configuration.getString("dashboard.lore.stats-supply-line", "<gray>sᴜᴘᴘʟʏ ᴘʀᴇssᴜʀᴇ: <aqua>%pressure%x") ?: "<gray>sᴜᴘᴘʟʏ ᴘʀᴇssᴜʀᴇ: <aqua>%pressure%x"

    val loreHistoryMultiplierLine: String
        get() = configuration.getString("dashboard.lore.history-multiplier-line", "<gray>ᴍᴜʟᴛɪᴘʟɪᴇʀ: <white>%multiplier%x") ?: "<gray>ᴍᴜʟᴛɪᴘʟɪᴇʀ: <white>%multiplier%x"

    val loreHistoryVolumeLine: String
        get() = configuration.getString("dashboard.lore.history-volume-line",
            "<gray>ʙᴜʏs: <green>%buys% <dark_gray>/ <gray>sᴇʟʟs: <red>%sells%"
        ) ?: "<gray>ʙᴜʏs: <green>%buys% <dark_gray>/ <gray>sᴇʟʟs: <red>%sells%"
}
