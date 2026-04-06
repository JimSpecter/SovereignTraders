# SovereignTraders

A shop plugin for Paper 1.21+ servers. Create item shops, attach them to NPC vendors, and let players buy, sell, or barter through a clean GUI.

## What it does

- NPC vendors with custom skins, holograms, and equipment — no Citizens required, but it supports Citizens too
- Buy mode, sell mode, and item-for-item barter mode
- Vault, GemsEconomy, CoinsEngine, and PlayerPoints support
- Per-item transaction quotas with auto-reset timers
- Sales and limited-time discounts with time-window scheduling
- Transaction logging for auditing
- Directive system — run commands when players purchase (ranks, kits, crate keys, whatever you want)
- Full in-game GUI editor, no YAML editing needed for day-to-day management
- Shift-click bulk buy/sell with smart quantity presets
- Folia compatible out of the box
- One-click migration from dtlTraders / dtlTradersPlus

## Requirements

- Paper 1.21+ (or Folia 1.21+)
- [PacketEvents](https://github.com/retrooper/packetevents)
- An economy plugin (Vault + EssentialsX, GemsEconomy, CoinsEngine, or PlayerPoints)
- Java 21

## Building

```sh
./gradlew buildFree
```

The obfuscated JAR lands in `build/libs/`.

For development (unobfuscated):

```sh
./gradlew shadowFreeJar
```

To spin up a local test server:

```sh
./gradlew runServerFree
```

This downloads a Paper server, drops in the plugin plus VaultUnlocked, EssentialsX, and PacketEvents, and starts it. If you have local plugin JARs (Citizens.jar, GemsEconomy.jar, etc.) in the project root, they get picked up automatically.

## Project structure

```
src/main/     Shared source — core, display, economy, catalog, config
src/free/     Free edition entry point and resources
src/test/     Unit tests
docs/         User-facing documentation
```

`main` has everything: vendor rendering, catalog management, economy providers, the GUI editor, quota enforcement, transaction logging, and the dtlTraders migration tool.

`free` is a thin entry point (`SovereignFreePlugin`) and a no-op pricing module.

## Documentation

Full docs are in the [docs/](docs/) folder:

- [Installation](docs/installation.md)
- [Commands and Permissions](docs/commands.md)
- [Configuration](docs/configuration.md)
- [Catalogs and Listings](docs/catalogs.md)
- [GUI Customization](docs/gui-customization.md)
- [Vendors](docs/vendors.md)
- [Economy Providers](docs/economy-providers.md)
- [Quotas](docs/quotas.md)
- [Sales](docs/sales.md)
- [Directives](docs/directives.md)
- [Barter Mode](docs/barter.md)
- [Citizens Integration](docs/citizens.md)
- [Migration from dtlTraders](docs/migration.md)
- [FAQ](docs/faq.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Source-available under the [PolyForm Noncommercial License 1.0.0](LICENSE). You can read, learn from, and modify the code for noncommercial purposes. The author retains all commercial distribution rights.

