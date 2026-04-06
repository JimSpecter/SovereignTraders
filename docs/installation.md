# Installation

## Steps

1. Download `SovereignTraders.jar`
2. Drop it into your server's `plugins/` folder
3. Install [PacketEvents](https://github.com/retrooper/packetevents/releases) if you don't already have it — drop the Spigot version into `plugins/` too
4. Make sure you have an economy plugin installed (see [Economy Providers](economy-providers.md))
5. Start (or restart) your server
6. Done. The plugin creates its config files on first boot.

## Generated files

After first startup, you'll find these in `plugins/SovereignTraders/`:

```
plugins/SovereignTraders/
├── config.yml          # Main settings (economy provider, etc.)
├── messages.yml        # All player-facing messages
├── guis.yml            # GUI layout and appearance
├── npc.yml             # Vendor presets and spawned vendor data
├── catalogs/           # One .yml file per shop you create
├── quota-ledger.yml    # Player quota tracking (auto-managed)
└── citizens-bridge.yml # Citizens NPC links (if Citizens is installed)
```

## Optional plugins

These are **not required** but SovereignTraders will hook into them if present:

| Plugin | What it does |
|---|---|
| Vault + economy (EssentialsX, CMI, etc.) | Default economy provider |
| GemsEconomy | Alternative economy |
| CoinsEngine | Alternative economy |
| PlayerPoints | Alternative economy |
| Citizens | Link your existing Citizens NPCs to catalogs |

## Folia

SovereignTraders works on Folia servers with zero extra configuration. It detects Folia automatically and switches to the region-threaded scheduler.
