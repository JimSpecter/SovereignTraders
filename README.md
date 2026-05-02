<div align="center">

# SovereignTraders

**NPC shop plugin for Paper 1.21+**

[![Version](https://img.shields.io/badge/version-1.0.7-5865F2?style=for-the-badge)](https://github.com/JimSpecter/SovereignTraders/releases)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B-00AA00?style=for-the-badge)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-supported-00AA00?style=for-the-badge)](https://papermc.io/software/folia)
[![License](https://img.shields.io/badge/License-PolyForm%20NC-orange?style=for-the-badge)](LICENSE)

Create shops, attach them to NPC vendors, and let players buy, sell, or barter — all through a clean in-game GUI with no YAML editing required.

</div>

---

## Features

| Feature | Description |
|---|---|
| **Custom NPC vendors** | Spawn vendors with custom skins, holograms, and equipment. No Citizens dependency, but it's supported. |
| **Three trade modes** | Buy, sell, and item-for-item barter — all configurable per listing. |
| **Multi-economy** | Works with Vault, GemsEconomy, CoinsEngine, and PlayerPoints out of the box. |
| **In-game editor** | Create and manage catalogs, listings, and vendors entirely in-game. |
| **Quotas** | Limit how many times a player can buy an item, with optional auto-reset timers. |
| **Sales & discounts** | Apply discounts to individual items or entire catalogs, with time-window scheduling. |
| **Directives** | Run console or player commands when a player completes a purchase (ranks, kits, crate keys, etc.). |
| **Barter mode** | Accept items instead of currency — define exactly what to take and what to give. |
| **Bulk buy/sell** | Shift-click for a quantity selector with smart presets. |
| **Transaction log** | Every buy, sell, and barter is written to a plain-text log for auditing. |
| **Folia support** | Thread-safe and Folia-compatible out of the box. |
| **Citizens integration** | Link vendors to Citizens NPCs if you prefer. |
| **dtlTraders migration** | One-click import from dtlTraders and dtlTradersPlus. |

---

## Requirements

- **Paper 1.21+** or Folia 1.21+
- **Java 21**
- **[PacketEvents](https://github.com/retrooper/packetevents)** — required for vendor NPCs
- One of: Vault + EssentialsX, GemsEconomy, CoinsEngine, or PlayerPoints

---

## Installation

1. Drop the JAR into your `plugins/` folder
2. Install [PacketEvents](https://github.com/retrooper/packetevents/releases) if you haven't already
3. Start the server — a default config will generate
4. Run `/traders` in-game to get started

See [docs/installation.md](docs/installation.md) for a full setup walkthrough.

---

## Building

```sh
# Clone the repo
git clone https://github.com/JimSpecter/SovereignTraders.git
cd SovereignTraders

# Build (obfuscated with ProGuard)
./gradlew buildFree

# Development build (unobfuscated)
./gradlew shadowFreeJar

# Spin up a local test server
./gradlew runServerFree
```

The test server task downloads Paper automatically and pre-installs VaultUnlocked, EssentialsX, and PacketEvents. Drop any additional plugin JARs (Citizens.jar, GemsEconomy.jar, etc.) in the project root and they'll be picked up automatically.

---

## Documentation

| Topic | |
|---|---|
| [Installation](docs/installation.md) | Getting the plugin running |
| [Commands & Permissions](docs/commands.md) | Full command and permission reference |
| [Configuration](docs/configuration.md) | config.yml walkthrough |
| [Catalogs & Listings](docs/catalogs.md) | How shops and items work |
| [GUI Customization](docs/gui-customization.md) | Customizing every GUI |
| [Vendors](docs/vendors.md) | Setting up NPC vendors |
| [Economy Providers](docs/economy-providers.md) | Supported economy plugins |
| [Quotas](docs/quotas.md) | Purchase limits and resets |
| [Sales](docs/sales.md) | Discounts and time-window scheduling |
| [Directives](docs/directives.md) | Command execution on purchase |
| [Barter Mode](docs/barter.md) | Item-for-item trading |
| [Citizens Integration](docs/citizens.md) | Using Citizens NPCs as vendors |
| [Migration](docs/migration.md) | Importing from dtlTraders |
| [FAQ](docs/faq.md) | Common questions |

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). TL;DR: fork, make a focused change, ensure `./gradlew check` passes, then open a PR.

---

## License

Source-available under the [PolyForm Noncommercial License 1.0.0](LICENSE). You can read, learn from, and modify the code for noncommercial purposes. The author retains all commercial distribution rights.

