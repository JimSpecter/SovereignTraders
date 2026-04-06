# GUI Customization

All GUI visuals are controlled from `plugins/SovereignTraders/guis.yml`. Every material, label, and lore line can be changed. All text supports **MiniMessage** format.

> Legacy `&`-color codes are auto-converted to MiniMessage on first load. You don't need to convert them yourself.

## Catalog browser

```yaml
catalog:
  filler-material: BLACK_STAINED_GLASS_PANE
  click-acquire: "<dark_gray>◆ <white>Click to buy"
  click-liquidate: "<dark_gray>◆ <white>Click to sell"
  click-barter: "<dark_gray>◆ <white>Click to barter"
```

## Navigation bar

The bottom row of the catalog GUI. Controls Previous, Next, Close buttons and the mode toggle.

```yaml
navigation:
  retreat:
    slots: [2]          # Which slot(s) in the bottom row
    material: EMERALD_BLOCK
    label: "<green>Previous"
  advance:
    slots: [6]
    material: EMERALD_BLOCK
    label: "<green>Next"
  dismiss:
    slots: [4]
    material: REDSTONE_BLOCK
    label: "<red>Close"

mode-toggle:
  slot: 8
  acquire:
    material: GOLDEN_APPLE
    label: "<green>Acquire"
  liquidate:
    material: STONE
    label: "<gold>Liquidate"
  barter:
    material: REDSTONE_BLOCK
    label: "<aqua>Barter"
```

## Listing display

```yaml
listing:
  barter-required-label: "<red>Required:"
  barter-receive-label: "<green>Receive:"
  reduction-badge: "<light_purple>⧖ Limited Offer"
  catalog-reduction-badge: "<light_purple>⧖ Storewide Sale"
```

## Editor GUI

The in-game catalog editor appearance:

```yaml
editor:
  title: "<white><bold>Editor</bold> <gray>» <yellow>%catalog% <gray>[<white>%mode%<gray>]"
  placeholder-material: LIME_STAINED_GLASS_PANE
  placeholder-name: "<green><bold>+ Add Item"
  placeholder-lore:
    - "<gray>Pick up an item from your"
    - "<gray>inventory, then click here"
  filler-material: GRAY_STAINED_GLASS_PANE
  save-material: EMERALD
  save-name: "<green><bold>Save & Close"
  listing-lore:
    - "<dark_gray>UID: %uid%"
    - "<green>Buy: %buy%"
    - "<gold>Sell: %sell%"
    - "<gray>Quota: <white>%quota% <dark_gray>| <gray>Reset: <white>%reset%"
    - ""
    - "<yellow>Left-click → Edit prices"
    - "<aqua>Shift-click → Edit quota"
    - "<red>Right-click → Remove"
```
