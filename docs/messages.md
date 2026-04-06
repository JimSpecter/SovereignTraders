# Messages

All player-facing messages are in `plugins/SovereignTraders/messages.yml`. Every message supports **MiniMessage** format.

> Legacy `&`-color codes are auto-converted to MiniMessage on first load.

## Structure

```yaml
general:
  tag: "<white><bold>Sovereign</bold> <gray>» "     # Prefix for all messages
  acquire-cost-format: "<gray>Cost: <green>$%amount%"
  liquidate-reward-format: "<gray>Reward: <green>$%amount%"
  currency-pattern: "#0.00"                          # Java DecimalFormat pattern

operations:
  acquisition-confirmed: "<green>Purchased %listing% for %amount%."
  liquidation-confirmed: "<green>Sold %listing% for %amount%."
  barter-confirmed: "<green>Barter completed."
  insufficient-balance: "<red>Not enough money. You need %deficit% more."
  insufficient-materials: "<red>You don't have the required items."
  inventory-full: "<red>Your inventory is full."
  quota-exhausted: "<red>Quota reached. Available again in %remaining%."
  restricted: "<red>You don't have permission to %action% this item."

catalog:
  not-found: "<red>No catalog found: %input%"
  created: "<green>Catalog created."
  removed: "<yellow>%catalog% <red>removed."
  header: "%catalog% - %section% - %mode%"    # GUI title format

vendor:
  linked: "<green>Vendor linked to <yellow>%catalog%<green>."
  unlinked: "<red>This vendor has no catalog."
  spawned: "<green>Vendor spawned."
```

## Placeholders

Placeholders use `%name%` syntax. The available placeholders depend on the message:

| Placeholder | Used in | Value |
|---|---|---|
| `%amount%` | Transaction messages | Formatted currency amount |
| `%listing%` | Transaction messages | Item name |
| `%deficit%` | Insufficient funds | How much more money is needed |
| `%remaining%` | Quota exhausted | Time until reset (e.g. "2h 30m") |
| `%action%` | Restricted | The mode name (Acquire/Liquidate/Barter) |
| `%catalog%` | Catalog messages | Catalog display name |
| `%input%` | Error messages | What the player typed |

## Currency format

The `currency-pattern` field uses Java's `DecimalFormat` syntax:

- `#0.00` → `10.50`
- `#,##0.00` → `1,000.50`
- `#0` → `10` (no decimals)
