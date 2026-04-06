# Vendors

Vendors are the NPC entities that players right-click to open a shop. They are **packet-based** — they don't use real entities on the server, so they have zero performance impact and don't interfere with mob caps.

> Vendors do **not** require Citizens. They work entirely on their own using PacketEvents.

## Spawning a vendor

Stand where you want the NPC and run:

```
/sovereign vendor spawn
```

This spawns a default player-type vendor with the default hologram.

### With a skin

```
/sovereign vendor spawn Notch
```

Uses the Minecraft skin of the player "Notch". Any valid Minecraft username works.

### With a different entity type

```
/sovereign vendor spawn Notch VILLAGER
```

Supported entity types: `PLAYER`, `VILLAGER`, `ZOMBIE`, `SKELETON`, `WANDERING_TRADER`, `PIGLIN`, `WITCH`, `IRON_GOLEM`, `SNOW_GOLEM`, `ENDERMAN`, `BLAZE`, `ALLAY`, `FOX`, `CAT`, `WOLF`

> Skins only apply to `PLAYER` type vendors. Other types use their default Minecraft appearance.

### Using a preset

```
/sovereign vendor spawn trader
```

Presets are defined in `npc.yml` and bundle together an entity type, skin, hologram, and other settings. Three come included: `trader`, `wanderer`, and `villager`.

## Linking a vendor to a catalog

After spawning a vendor, look at it and run:

```
/sovereign vendor link weapons
```

Now when players right-click this vendor, the `weapons` catalog opens.

## Removing a vendor

Look at the vendor (within 5 blocks) and run:

```
/sovereign vendor remove
```

### Undo a removal

Changed your mind? Run this right after:

```
/sovereign vendor undo
```

The vendor is restored exactly as it was, including its catalog link.

## Equipping a vendor

Hold an item in your hand, look at the vendor, and run:

```
/sovereign vendor equip <slot>
```

Slots: `mainhand`, `offhand`, `helmet`, `chestplate`, `leggings`, `boots`

Example — give the vendor a diamond sword:

```
/sovereign vendor equip mainhand
```

## Changing the hologram

Look at the vendor and run:

```
/sovereign vendor hologram <lines>
```

Use `||` to separate lines. Lines support MiniMessage formatting.

```
/sovereign vendor hologram <gold><bold>Shop</bold> || <gray>Right click to browse
```

## Head tracking

Vendors automatically turn their head to look at nearby players. The tracking radius is configurable per-vendor in `npc.yml` (default: 5 blocks). Set to `0` to disable.

## Presets

Define reusable vendor templates in `npc.yml`:

```yaml
presets:
  trader:
    skin: "Notch"
    entity-type: PLAYER
    hologram:
      - "<gold><bold>Trader</bold>"
      - "<gray>Right click to trade"
    look-close-radius: 5.0
    interaction-width: 0.8
    interaction-height: 1.8
    catalog: weapons     # Auto-links this catalog on spawn
```

Then spawn it with `/sovereign vendor spawn trader`.

## Editing vendors after spawning

All spawned vendors are saved in the `vendors:` section of `npc.yml`. You can edit any property there (skin, hologram, entity type, catalog link, look radius, hitbox size) and run `/sovereign reload` to apply changes live.

## Persistence

Vendors survive server restarts. They're stored both as invisible Interaction entities in the world and in `npc.yml`. On startup, the plugin scans loaded chunks and recovers all vendors automatically.
