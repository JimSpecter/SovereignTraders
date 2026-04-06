# Directives

Directives let you run commands when a player buys a listing. Use them to give ranks, crate keys, kits, permission nodes, or anything else a command can do.

## Setting up a directive listing

In your catalog YAML, set the listing type to `directive` and add your commands:

```yaml
0:
  uid: 10
  type: directive
  material: NETHER_STAR
  quantity: 1
  label: "<gold>VIP Rank"
  acquire-cost: 5000
  show-cost: true
  directive-run-mode: EXECUTE_ONLY
  directives:
    0:
      command: "lp user %player% parent set vip"
      executor: CONSOLE
    1:
      command: "give %player% diamond 64"
      executor: CONSOLE
```

## Placeholders

| Placeholder | Replaced with |
|---|---|
| `%player%` | The buying player's name |

## Executor

| Value | What it does |
|---|---|
| `CONSOLE` | Runs the command as the server console |
| `PLAYER` | Runs the command as the player |

## Run modes

| Mode | What happens |
|---|---|
| `EXECUTE_ONLY` | Just runs the commands. No item is given, no cost is charged unless `acquire-cost` is set. If cost is set, it's charged before running. |
| `ACQUIRE_AND_EXECUTE` | Charges the player, runs the commands. No item is given. |
| `ACQUIRE_AND_RETAIN` | Charges the player, gives them the item, **and** runs the commands. |

## Example: crate key

```yaml
0:
  type: directive
  material: TRIPWIRE_HOOK
  label: "<light_purple>Crate Key"
  acquire-cost: 1000
  directive-run-mode: EXECUTE_ONLY
  directives:
    0:
      command: "crates give %player% vote 1"
      executor: CONSOLE
```

Player pays $1000, receives a crate key via the crates plugin command. The tripwire hook in the GUI is just for display.

## Quotas on directives

Directives support quotas just like regular listings. Combine them with a quota to create once-per-day purchases, limited-time offers, etc.
