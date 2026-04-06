# Economy Providers

SovereignTraders supports four economy systems. Pick one and set it in `config.yml`.

## Vault (default)

Works with any Vault-compatible economy — EssentialsX, CMI, GemsEconomy (via Vault bridge), etc.

```yaml
general:
  economy-provider: vault
```

You need both Vault (or VaultUnlocked) **and** an economy plugin that registers with Vault.

## GemsEconomy

Direct integration with GemsEconomy's multi-currency system.

```yaml
general:
  economy-provider: gemseconomy
```

## CoinsEngine

Direct integration with NightExpress CoinsEngine. You must also specify which currency to use.

```yaml
general:
  economy-provider: coinsengine
  coins-engine-currency: coins
```

Replace `coins` with whatever currency ID you've defined in CoinsEngine.

## PlayerPoints

Uses Rosewood's PlayerPoints as the currency.

```yaml
general:
  economy-provider: playerpoints
```

> **Note:** PlayerPoints uses whole numbers (integers). Decimal prices like `10.50` will be rounded to `10`.

## What happens if the economy fails?

If SovereignTraders can't connect to your chosen economy on startup, the plugin disables itself and tells you why in console. Fix the economy setup and restart.
