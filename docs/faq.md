# FAQ & Troubleshooting

## The plugin won't start / disables itself on startup

**"Economy provider failed to initialise"** — Your economy plugin isn't set up correctly.

- If using Vault: make sure both Vault (or VaultUnlocked) **and** an economy plugin (EssentialsX, CMI, etc.) are installed and loaded before SovereignTraders
- If using GemsEconomy/CoinsEngine/PlayerPoints: make sure the plugin is installed and the name in `config.yml` is spelled exactly right (`vault`, `gemseconomy`, `coinsengine`, `playerpoints`)
- For CoinsEngine: also check that `coins-engine-currency` matches a currency ID in your CoinsEngine config

## Vendor NPCs are invisible

- Make sure [PacketEvents](https://github.com/retrooper/packetevents) is installed. Vendors won't render without it.
- If using player-type vendors with skins, the skin download takes a moment. Wait a few seconds after spawning.
- Try relogging. Vendors are packet-based — if you were out of range when they spawned, you may need to rejoin for the client to receive the packets.

## Vendor skins show as Steve/Alex

The plugin fetches skins from Mojang's servers. This can fail if:
- The username doesn't exist
- Mojang's API is temporarily down
- Your server can't make outbound HTTPS connections

Check console for "Failed to fetch skin" messages.

## Items in the shop don't match what I added

Make sure you're holding the **exact item** you want when using `/sovereign listing add` or the editor. The plugin stores the item including all metadata (enchantments, custom names, lore, NBT).

## Players can't open a catalog

Check for:
1. **Catalog authorization** — does the catalog have an `authorization` field? The player needs that permission.
2. **Section authorization** — same thing but per-section.
3. **Command permission** — if opened via command, the player needs `sovereign.catalog.open`.
4. **Vendor link** — if opened via NPC, make sure the vendor is linked to a catalog (`/sovereign vendor link <name>`).

## The mode toggle button doesn't appear / only one mode works

The catalog only shows modes that are enabled. Check the catalog YAML:

```yaml
modes:
  acquire: true
  liquidate: true
  barter: false
```

If only `acquire` is `true`, there's no toggle button because there's nothing to toggle to.

## Quota timer shows wrong time / doesn't reset

The quota timer is server-side and ticks every second. If you restart the server, the timers restart from their configured interval. Quota usage data persists across restarts, but the countdown timers don't.

## How do I edit a catalog without the in-game editor?

Each catalog is a YAML file in `plugins/SovereignTraders/catalogs/<name>.yml`. Edit it with any text editor and run `/sovereign reload`.

## How do I move a vendor?

You can't move vendors in-place. Remove it (`/sovereign vendor remove`), walk to the new location, and spawn a new one. Or edit the location values in `npc.yml` — but note the docs say location changes there are not applied on reload (the in-world entity stays put).

## Does this work on Folia?

Yes. SovereignTraders detects Folia automatically and uses the correct scheduler. No configuration needed.

## I migrated from dtlTraders but some items look wrong

The migrator handles most cases, but some edge cases with very old dtlTraders data formats may not convert perfectly. Check the specific items in `/sovereign catalog edit <name>` and fix them manually if needed. The original dtlTraders data is backed up — it's not modified.

## Can I use both SovereignTraders vendors and Citizens NPCs?

Yes. SovereignTraders vendors and Citizens NPCs are completely independent. You can have both on the same server and link Citizens NPCs to catalogs using `/sovereign citizen link`.
