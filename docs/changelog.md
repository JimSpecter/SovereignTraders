# Changelog

## v1.0.2

### New Feature
- **Scheduled Sales Activation** — Reduction time windows (`window-start`, `window-end`) on catalogs and listings are now automatically enforced. The plugin checks every 5 seconds and toggles `reductionActive` on/off based on the current time. Set a start and end time in ISO-8601 format and the sale activates and deactivates itself — no manual toggling required.
- **Transaction Logging** — All buy, sell, and barter transactions are now logged to `plugins/SovereignTraders/transactions.log` as pipe-delimited records. Includes timestamp, player, action, item, quantity, cost, and economy provider. Enable/disable via `config.yml` → `general.transaction-log`.

---

## v1.0.1

### New Feature
- **Quantity Selector** — Shift-click any item in a catalog to open a bulk buy/sell menu. Choose from smart presets (×1, ×5, ×16, ×32, ×64, stack, half-inventory, max) or pick a custom amount. Works with both Acquire and Liquidate modes.

### Bug Fix
- Fixed a critical `VerifyError` on startup caused by invalid bytecode in the obfuscation pipeline. The plugin would fail to enable with `Expecting a stackmap frame at branch target`. Resolved by correcting the ProGuard preverification step for Java 21.

---

## v1.0.0

- Initial release
