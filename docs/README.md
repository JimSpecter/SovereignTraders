# Welcome

**SovereignTraders** is a shop plugin for Paper 1.21+ servers. You create shops (called **catalogs**), fill them with items, and attach them to NPC vendors that players can right-click to buy, sell, or barter.

## What it does

- Spawn NPC vendors with custom skins, holograms, and equipment — no Citizens required
- Create shops with a buy mode, sell mode, and item-for-item barter mode
- Support for Vault, GemsEconomy, CoinsEngine, and PlayerPoints
- Per-item transaction quotas with auto-reset timers
- Sales and limited-time discounts on individual items or entire shops, with optional time-window scheduling
- Transaction logging for all buy, sell, and barter activity
- Run commands when players buy something (give ranks, kits, crate keys, etc.)
- Full in-game GUI editor — no YAML editing needed for day-to-day management
- Shift-click bulk buy/sell with smart quantity presets
- Folia compatible out of the box
- One-click migration from dtlTraders / dtlTradersPlus

## Requirements

- Paper 1.21+ (or Folia 1.21+)
- [PacketEvents](https://github.com/retrooper/packetevents) (required — used for vendor NPCs)
- An economy plugin (Vault + EssentialsX, GemsEconomy, CoinsEngine, or PlayerPoints)
- Java 21
