# Commands

The base command is `/traders`. Aliases: `/st`, `/svt`.

## General

| Command | What it does |
|---|---|
| `/traders` | Shows the help overview |
| `/traders help [topic]` | Detailed help. Topics: `catalog`, `listing`, `vendor`, `admin`, `market`, `citizen` |
| `/traders reload` | Reloads all config files and catalogs |

## Catalog

| Command | What it does |
|---|---|
| `/traders catalog list` | List all catalogs |
| `/traders catalog open <name>` | Open a catalog GUI |
| `/traders catalog create <name>` | Create a new catalog |
| `/traders catalog delete <name>` | Delete a catalog |
| `/traders catalog edit <name>` | Open the in-game editor |

## Listing

| Command | What it does |
|---|---|
| `/traders listing add <catalog> <buy> <sell>` | Add the held item to a catalog with buy/sell prices |
| `/traders listing remove <catalog> <slot>` | Remove a listing by slot number |

## Vendor

| Command | What it does |
|---|---|
| `/traders vendor spawn [preset\|skin] [type]` | Spawn a vendor NPC |
| `/traders vendor remove` | Remove the nearest vendor (within 5 blocks) |
| `/traders vendor undo` | Restore the last removed vendor |
| `/traders vendor link <catalog>` | Link nearest vendor to a catalog |
| `/traders vendor equip <slot>` | Give the held item to the vendor's equipment slot |
| `/traders vendor hologram <lines>` | Set hologram text (use `\|\|` for line breaks) |

## Citizens

| Command | What it does |
|---|---|
| `/traders citizen link <catalog>` | Link a Citizens NPC to a catalog (look at the NPC) |
| `/traders citizen unlink` | Unlink a Citizens NPC |
| `/traders citizen list` | List all Citizens NPC links |

## Admin

| Command | What it does |
|---|---|
| `/traders migrate dtl` | Migrate from dtlTraders/dtlTradersPlus |

## Tab completion

All commands have full tab completion. Catalog names are suggested automatically where applicable.
