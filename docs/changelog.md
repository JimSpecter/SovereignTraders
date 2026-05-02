# Changelog

## v1.0.7

### New Feature
- **bStats Integration** — Added bStats support for SovereignTraders using plugin ID `30709`, with the metrics classes shaded and relocated into the final jar for Paper/Folia-safe runtime reporting.

### Bug Fix
- **PlayerPoints Economic Drift** — Fractional prices (from dynamic multipliers, catalog discounts, or user-set decimal values) were silently floor-truncated when transacting with PlayerPoints. A cost of `10.75` would charge `10`, and a reward of `10.75` would pay `10`, causing systematic balance drift over time. All three operations — withdraw, deposit, and affordability check — now round to the nearest integer consistently.

---

## v1.0.5

### Bug Fixes
- **Reload Kills Timed Services** — Fixed quota reset timers and reduction window schedulers permanently stopping after a `/traders reload`. Both services now correctly restart their tick loops after reload instead of only re-syncing data.
- **Acquisition Rollback Atomicity** — When a purchase failed due to a full inventory, the rollback incorrectly called `removeItem` on the player's existing inventory instead of restoring from a pre-transaction snapshot. Items the player already held could be erroneously consumed. Rollback now always restores from a cloned snapshot taken before any inventory mutation.
- **Liquidation Rollback** — When a sell transaction's deposit was rejected by the economy provider, consumed items were not returned to the player. The player would lose their items with no payment. The inventory is now restored from snapshot on any deposit failure.
- **Refund Failure Alerting** — If a refund deposit fails during an acquisition rollback, the failure is now logged at `SEVERE` so operators can identify and manually correct cases where a player lost both their money and their items.

---

## v1.0.4

### Bug Fix
- **Inventory Full Rollback** — Fixed items being incorrectly removed from a player's existing inventory when a purchase could not fit.

---

## v1.0.3

### Breaking Change
- **Command Root Renamed** — The primary command has changed from `/sovereign` to `/traders`. Aliases `/st` and `/svt` remain unchanged. Any macros, scripts, or command blocks using `/sovereign` must be updated to `/traders`.

---

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
