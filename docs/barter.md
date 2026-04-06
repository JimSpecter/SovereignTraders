# Barter System

Barter lets players trade items for other items — no money involved.

## How it works

A barter listing has two sides:

- **Requirements** — what the player must give up
- **Receivables** — what the player gets in return

Example: trade 10 iron ingots for 1 diamond.

## Enabling barter on a catalog

In your catalog YAML file, enable the barter mode:

```yaml
modes:
  acquire: true
  liquidate: true
  barter: true
```

Or toggle it through the editor mode button.

## Setting up barter listings

Barter listings are configured in the catalog YAML under `barter-listings`:

```yaml
barter-listings:
  0:
    uid: 5
    type: barter
    material: DIAMOND
    quantity: 1
    label: "<aqua>Diamond Trade"
    barter-requirements:
      0:
        material: IRON_INGOT
        quantity: 10
        label: "Iron Ingot"
    barter-receivables:
      0:
        material: DIAMOND
        quantity: 1
        label: "Diamond"
```

You can have multiple requirements and multiple receivables per listing.

## In the GUI

When a player is in Barter mode, each listing shows:
- **Required:** — the items they need to have
- **Receive:** — the items they'll get

If the player doesn't have the required items, the transaction fails with a message.

## Barter + other features

Barter listings support:
- Quotas (limit trades per player)
- Permissions
- Broadcasting
- Drop-on-overflow (drops received items if inventory is full)
- Directives (run commands on trade)
