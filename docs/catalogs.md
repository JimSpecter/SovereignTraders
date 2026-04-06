# Catalogs

A **catalog** is a shop. It has a name, one or more pages (called **sections**), and items inside those pages.

## Creating a catalog

```
/sovereign catalog create weapons
```

This creates a catalog called `weapons` with one empty section and 4 rows of item slots. A file appears at `plugins/SovereignTraders/catalogs/weapons.yml`.

## Opening a catalog

```
/sovereign catalog open weapons
```

Opens the shop GUI for that catalog. This is also what happens when a player right-clicks a vendor NPC linked to this catalog.

## Editing a catalog

```
/sovereign catalog edit weapons
```

Opens the in-game editor. From here you can:

- **Add items** — pick up an item from your inventory, then click an empty (green) slot
- **Set prices** — left-click an item to type buy/sell prices in chat
- **Set quotas** — shift-click an item to set per-player limits
- **Remove items** — right-click an item to delete it
- **Switch modes** — click the mode button to toggle between Acquire/Liquidate/Barter editing
- **Bulk quota** — click the clock button to set quotas for all items in the current mode at once
- **Save** — click the emerald to save and close

## Listing catalogs

```
/sovereign catalog list
```

## Deleting a catalog

```
/sovereign catalog delete weapons
```

This removes the catalog and its file permanently.

## Catalog modes

Each catalog can have up to three modes enabled:

| Mode | What it does |
|---|---|
| **Acquire** | Players buy items from the shop |
| **Liquidate** | Players sell items to the shop |
| **Barter** | Players trade items for other items |

Players toggle between enabled modes using the mode button in the bottom-right of the shop GUI.

Modes are set in the catalog YAML file:

```yaml
modes:
  acquire: true
  liquidate: true
  barter: false
```

## Sections (pages)

A catalog can have multiple sections (pages). Players navigate between them using the Previous/Next buttons at the bottom of the GUI.

Each section has:
- An **identifier** (name)
- A **row count** (1–5 rows of items)
- An optional **permission** to restrict access

## Catalog permissions

You can restrict who can open a catalog by setting an `authorization` field:

```yaml
authorization: "shop.vip"
```

Only players with the `shop.vip` permission can open this catalog. Leave it blank or remove it for no restriction.

Sections can also have their own permission, independent of the catalog permission.
