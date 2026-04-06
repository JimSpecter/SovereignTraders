# Quotas

Quotas limit how many times a player can use a listing within a time period.

## Example

You want players to only buy a "Daily Reward" item once every 24 hours.

In the listing YAML:

```yaml
quota:
  limit: 1
  reset-interval: 86400    # seconds (86400 = 24 hours)
  show-progress: true
  show-timer: true
```

## Properties

| Property | What it does |
|---|---|
| `limit` | Max transactions per player before they're blocked. `0` = no limit. |
| `reset-interval` | Seconds until the counter resets. `0` = never resets (permanent limit). |
| `show-progress` | Show "Quota: 0/1" on the item lore |
| `show-timer` | Show "Resets in: 23h 59m" on the item lore |

## Setting quotas in-game

### Per item

Open the editor (`/sovereign catalog edit <name>`), shift-click a listing, and type the limit and reset interval in chat.

### Bulk (all items in a mode)

In the editor, click the **clock button** in the control bar. This sets the same quota on every listing in the current mode (Acquire, Liquidate, or Barter).

## How the timer works

The countdown runs server-side and ticks every second. When it hits zero, all players' usage counts for that listing are wiped. The timer is per-listing, not per-player — everyone's quota resets at the same time.

## Quota data

Player usage is tracked in `plugins/SovereignTraders/quota-ledger.yml`. This file is auto-saved periodically. You normally don't need to touch it.
