# Sales & Discounts

SovereignTraders supports two types of discounts: **per-listing** and **catalog-wide**.

## Per-listing discount

Reduce the price of a single item. Configured in the listing YAML:

```yaml
reduction:
  active: true
  percent: 25.0
  override-cost: 0.0
  window-start: null
  window-duration: 0
  window-end: null
  show-cost: true
  show-start: false
  show-duration: false
  show-end: false
```

| Property | What it does |
|---|---|
| `active` | Enable/disable the discount |
| `percent` | Discount percentage (25 = 25% off) |
| `override-cost` | Set a flat discounted price instead of using a percentage. `0` = use percent. |
| `show-cost` | Show the original price crossed out next to the discounted price |

Items with an active discount get an enchantment glint in the GUI and a "Limited Offer" badge.

## Catalog-wide discount

Apply a discount to every item in a catalog at once. Configured in the catalog YAML:

```yaml
reduction:
  active: true
  percent: 10.0
```

This stacks with per-listing discounts. A 10% catalog discount on an item with a 25% listing discount means the player gets the listing discount (whichever applies).

Catalog-wide discounts show a "Storewide Sale" badge instead of "Limited Offer".

## Timed windows

Both listing and catalog discounts support **automatic scheduling**. Set a start time, an end time, or both, and the plugin will toggle `active` on and off by itself. The scheduler checks every 5 seconds.

### Configuration

```yaml
reduction:
  active: false          # The scheduler will flip this automatically
  percent: 25.0
  window-start: "2026-03-15T10:00:00Z"
  window-end: "2026-03-15T22:00:00Z"
```

### Supported time formats

| Format | Example |
|---|---|
| ISO-8601 instant (UTC) | `2026-03-15T10:00:00Z` |
| ISO-8601 with offset | `2026-03-15T12:00:00+02:00` |
| ISO-8601 local datetime (treated as UTC) | `2026-03-15T10:00:00` |
| Epoch milliseconds | `1773756000000` |

### Behavior

| `window-start` | `window-end` | What happens |
|---|---|---|
| Set | Set | Sale is active only between the two times |
| Set | Not set | Sale activates once the start time is reached and stays on |
| Not set | Set | Sale is active immediately and deactivates at the end time |
| Not set | Not set | No scheduling — `active` is only changed manually |

When the scheduler toggles a **catalog-level** window, it broadcasts a console message for admin visibility. Changes are persisted automatically.

## Visual indicators

When a discount is active:
- The item gets an **enchantment glint** (shimmer effect)
- A **badge** appears in the lore ("Limited Offer" or "Storewide Sale")
- If `show-cost` is true, the **original price is shown crossed out**
