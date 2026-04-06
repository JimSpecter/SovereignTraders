# Permissions

## Catalog

| Permission | What it allows |
|---|---|
| `sovereign.catalog.list` | List all catalogs |
| `sovereign.catalog.open` | Open a catalog GUI |
| `sovereign.catalog.create` | Create new catalogs |
| `sovereign.catalog.delete` | Delete catalogs |
| `sovereign.catalog.edit` | Use the in-game catalog editor |

## Listing

| Permission | What it allows |
|---|---|
| `sovereign.listing.add` | Add listings via command |
| `sovereign.listing.remove` | Remove listings via command |

## Vendor

| Permission | What it allows |
|---|---|
| `sovereign.vendor.spawn` | Spawn vendor NPCs |
| `sovereign.vendor.remove` | Remove vendor NPCs |
| `sovereign.vendor.link` | Link vendors to catalogs |
| `sovereign.vendor.equip` | Equip items on vendors |
| `sovereign.vendor.hologram` | Change vendor holograms |

## Admin

| Permission | What it allows |
|---|---|
| `sovereign.admin.reload` | Reload configuration |
| `sovereign.admin.migrate` | Run migrations |
| `sovereign.admin.citizen` | Manage Citizens NPC links |

## Per-catalog and per-listing permissions

On top of the command permissions above, individual catalogs, sections, and listings can have their own `authorization` field. This is a custom permission string that you define — players need it to access that specific catalog/section/listing.

Example: a catalog with `authorization: "shop.vip"` requires the player to have `shop.vip` to open it.

These are set in the catalog YAML files, not in a permissions plugin config.
