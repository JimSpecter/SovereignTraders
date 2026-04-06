# Migrating from dtlTraders

SovereignTraders can import all your shops from **dtlTraders** or **dtlTradersPlus** in one command.

## How to migrate

1. Make sure the dtlTraders/dtlTradersPlus data folder is still in your `plugins/` directory (you don't need the plugin itself to be running)
2. Run:

```
/sovereign migrate dtl
```

3. That's it. The plugin finds the data folder, reads every shop, and creates matching catalogs.

## What gets migrated

| dtlTraders | SovereignTraders |
|---|---|
| Shops | Catalogs |
| Pages | Sections |
| Buy items | Acquire listings |
| Sell items | Liquidate listings |
| Trade items | Barter listings |
| Commands (run_only, buy_and_run, buy_and_keep) | Directives |
| Permissions | Authorization fields |
| Item stacks (including NBT) | Preserved exactly |
| Buy/sell/trade limits | Quotas |
| Broadcast messages | Broadcast templates |

## What happens to the original data

The plugin creates a backup copy of your dtlTraders folder (named `dtlTradersPlus_pre-sovereign-backup` or similar) before doing anything. Your original data is not modified.

## After migration

Run `/sovereign catalog list` to see all your imported catalogs. Open each one with `/sovereign catalog open <name>` to verify everything looks right.

## Supported formats

The migrator handles both:
- **Modern format** — per-shop folders under `shops/`
- **Legacy format** — single `guis.yml` with all shops

## Legacy color codes

All `&`-color codes and `§`-codes from dtlTraders are automatically converted to MiniMessage format during migration.
