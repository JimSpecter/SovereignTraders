# Confirmation GUI

By default, when a player clicks an item in a shop, a confirmation screen opens before the transaction goes through. This prevents accidental purchases.

## How it looks

The confirmation GUI shows:
- The item being purchased in the center
- Green panes on the left — click to confirm
- Red panes on the right — click to cancel
- Item lore shows the mode and cost

## Disabling it

If you want instant transactions (no confirmation step), set this in `guis.yml`:

```yaml
confirmation:
  enabled: false
```

## Customization

All confirmation GUI visuals are in `guis.yml`:

```yaml
confirmation:
  enabled: true
  title: "%item% - ᴄᴏɴꜰɪʀᴍ - %mode%"
  confirm-material: LIME_STAINED_GLASS_PANE
  confirm-label: "<green><bold>✔ Confirm"
  cancel-material: RED_STAINED_GLASS_PANE
  cancel-label: "<red><bold>✘ Cancel"
  filler-material: BLACK_STAINED_GLASS_PANE
```

The preview item lore is in `messages.yml` under `confirmation.preview-lore`.
