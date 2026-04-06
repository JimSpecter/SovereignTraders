# Commands

The base command is `/sovereign`. Aliases: `/st`, `/svt`.

## General

| Command | What it does |
|---|---|
| `/sovereign` | Shows the help overview |
| `/sovereign help [topic]` | Detailed help. Topics: `catalog`, `listing`, `vendor`, `admin`, `market`, `citizen` |
| `/sovereign reload` | Reloads all config files and catalogs |

## Catalog

| Command | What it does |
|---|---|
| `/sovereign catalog list` | List all catalogs |
| `/sovereign catalog open <name>` | Open a catalog GUI |
| `/sovereign catalog create <name>` | Create a new catalog |
| `/sovereign catalog delete <name>` | Delete a catalog |
| `/sovereign catalog edit <name>` | Open the in-game editor |

## Listing

| Command | What it does |
|---|---|
| `/sovereign listing add <catalog> <buy> <sell>` | Add the held item to a catalog with buy/sell prices |
| `/sovereign listing remove <catalog> <slot>` | Remove a listing by slot number |

## Vendor

| Command | What it does |
|---|---|
| `/sovereign vendor spawn [preset\|skin] [type]` | Spawn a vendor NPC |
| `/sovereign vendor remove` | Remove the nearest vendor (within 5 blocks) |
| `/sovereign vendor undo` | Restore the last removed vendor |
| `/sovereign vendor link <catalog>` | Link nearest vendor to a catalog |
| `/sovereign vendor equip <slot>` | Give the held item to the vendor's equipment slot |
| `/sovereign vendor hologram <lines>` | Set hologram text (use `\|\|` for line breaks) |

## Citizens

| Command | What it does |
|---|---|
| `/sovereign citizen link <catalog>` | Link a Citizens NPC to a catalog (look at the NPC) |
| `/sovereign citizen unlink` | Unlink a Citizens NPC |
| `/sovereign citizen list` | List all Citizens NPC links |

## Admin

| Command | What it does |
|---|---|
| `/sovereign migrate dtl` | Migrate from dtlTraders/dtlTradersPlus |

## Tab completion

All commands have full tab completion. Catalog names are suggested automatically where applicable.
