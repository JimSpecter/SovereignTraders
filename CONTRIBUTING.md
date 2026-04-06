# Contributing to SovereignTraders

Contributions are welcome. Whether it's a bug fix, a new economy provider, or a QoL improvement, here's how to get involved.

## Getting started

1. Fork the repo and clone it
2. Make sure you have Java 21 installed
3. Run `./gradlew build` to verify everything compiles
4. Run `./gradlew test` to make sure tests pass

## Development

The project uses Kotlin 2.1, targets Paper 1.21+ via paperweight, and is Folia-safe.

**Build the plugin:**
```sh
./gradlew shadowFreeJar       # unobfuscated, for development
./gradlew buildFree            # obfuscated with ProGuard, for release
```

**Run a test server:**
```sh
./gradlew runServerFree
```

This spins up a Paper server with VaultUnlocked, EssentialsX, and PacketEvents pre-installed. Drop any extra plugin JARs (Citizens.jar, GemsEconomy.jar, etc.) in the project root and they'll be picked up automatically.

## Project layout

- `src/main/` — All shared logic: vendors, catalogs, economy, config, display
- `src/free/` — Free edition bootstrap and pricing stub
- `src/test/` — Unit tests (JUnit 5 + Mockito)
- `docs/` — User-facing documentation

## Code style

- Kotlin, not Java
- No wildcard imports
- Keep functions short and focused
- No comments unless absolutely necessary to explain non-obvious behavior — the code should speak for itself
- Follow existing patterns in the codebase

## Pull requests

1. Create a branch from `main`
2. Keep changes focused — one fix or feature per PR
3. Make sure `./gradlew check` passes before submitting
4. Write a clear description of what changed and why

If you're unsure whether a change would be accepted, open an issue first to discuss it.

## Reporting bugs

Open an issue with:
- Server version (Paper/Folia, Minecraft version)
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Any relevant console output

## License

By contributing, you agree that your contributions will be licensed under the same [PolyForm Noncommercial License 1.0.0](LICENSE) as the rest of the project.
