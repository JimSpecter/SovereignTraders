# Listings

A **listing** is an item inside a catalog. Each listing has a type, a price, and various optional features.

## Adding a listing via command

Hold the item you want to add, then run:

```
/traders listing add <catalog> <buy-price> <sell-price>
```

Example — add a diamond sword that costs $500 to buy and pays $200 to sell:

```
/traders listing add weapons 500 200
```

- If the buy price is > 0, the item is added to the Acquire mode
- If the sell price is > 0, the item is added to the Liquidate mode
- The item in your hand is used, including its name, lore, enchantments, and NBT data

## Adding via the editor

Open the editor with `/traders catalog edit <name>`, pick up an item from your inventory, and click an empty slot. You'll be prompted to type prices in chat.

## Removing a listing

```
/traders listing remove <catalog> <slot>
```

Or right-click it in the editor GUI.

## Listing types

| Type | What it does |
|---|---|
| **Tradable** | Normal buy/sell item. The default. |
| **Static** | Display-only. Shows up in the GUI but can't be clicked. |
| **Directive** | Runs commands when purchased (see [Directives](directives.md)). |
| **Barter** | Item-for-item trade (see [Barter System](barter.md)). |

## Quantity selector (bulk buy/sell)

**Shift-click** any listing in the catalog GUI to open the quantity selector. This lets you buy or sell in bulk without clicking one item at a time.

The selector shows smart presets based on the item and your inventory:

| Preset | What it does |
|---|---|
| ×1 | Single item (same as a normal click) |
| ×5 | Buy/sell 5 |
| ×16 | Buy/sell 16 |
| ×32 | Buy/sell 32 |
| ×64 | Full stack |
| Half inventory | Buy/sell enough to fill half your empty slots |
| Max | Buy/sell the maximum you can afford / carry |

- In **Acquire** mode, the total cost is shown for each preset. Presets you can't afford are grayed out.
- In **Liquidate** mode, presets you don't have enough items for are grayed out.
- The selector respects quotas — if you've hit your limit, the preset won't exceed it.

## Listing properties

These are all configurable in the catalog YAML file under each listing:

| Property | What it does |
|---|---|
| `acquire-cost` | Price to buy this item |
| `liquidate-reward` | Price the shop pays when a player sells this item |
| `quantity` | Stack size given/required per transaction |
| `label` | Custom display name (MiniMessage format) |
| `annotations` | Extra lore lines shown on the item |
| `show-annotations` | Show/hide the annotations |
| `show-cost` | Show/hide the price on the item lore |
| `authorization` | Permission required to use this listing |
| `drop-on-overflow` | Drop items on the ground if the player's inventory is full |
| `partial-liquidation` | Allow selling less than the full stack if the player doesn't have enough |
| `broadcast` | Announce transactions server-wide |
| `broadcast-template` | The message template (supports `%player%`, `%listing%`, `%amount%`) |

## Custom item stacks

When you add an item through the editor or the `/traders listing add` command, the plugin stores the **exact item** — including custom names, lore, enchantments, custom model data, and any NBT. The shop gives out that exact item.

## Unique IDs

Every listing gets a globally unique ID (UID). This ID is used internally for quotas, dynamic pricing, and tracking. You'll see it in the editor as a gray number. You don't need to manage it — it's automatic.
