# Configuration

The main config file is `plugins/SovereignTraders/config.yml`. It controls which economy to use and general plugin settings.

## config.yml

```yaml
general:
  # Which economy plugin to use.
  # Options: vault, gemseconomy, coinsengine, playerpoints
  economy-provider: vault

  # Only matters if you use CoinsEngine.
  # Set this to the currency ID from your CoinsEngine config.
  coins-engine-currency: coins

  # Log all buy/sell/barter transactions to transactions.log
  transaction-log: true
```

That's the entire free config. Pick your economy provider, optionally enable transaction logging, and you're done.

## Reloading

After editing any config file, run:

```
/sovereign reload
```

This reloads `config.yml`, `messages.yml`, `guis.yml`, `npc.yml`, and all catalogs without restarting your server.
