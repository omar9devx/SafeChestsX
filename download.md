# SafeChestsX Download & Install Guide (Paper 1.21.4 - 1.21.11)

## 1) Download the plugin
1. Download the latest **SafeChestsX** jar from your release source.
2. Make sure the file name ends with `.jar`.

## 2) Add to your server
1. Stop your server if it is running.
2. Place the jar inside your server's `plugins/` folder.

## 3) (Optional) Install Vault + economy
If you want to enable `/chestpay`, you must install:
- **Vault**
- **An economy plugin** (e.g., EssentialsX Economy)

## 4) Start the server
1. Start your Paper server (versions **1.21.4 to 1.21.11**).
2. The plugin will generate its config at:
   - `plugins/SafeChestsX/config.yml`
   - `plugins/SafeChestsX/claims.yml`
   - `plugins/SafeChestsX/virtual-chests.yml`

## 5) Configure (optional)
Open `config.yml` and update:
- `settings.default-virtual-chests`
- `settings.virtual-chest-price`
- `settings.max-claims-per-player`
- `settings.max-chests-per-claim`

Then run:
```
/claimchest reload
```

## 6) Verify commands
Try:
- `/claimchest`
- `/chest 1`
- `/chestpay 1`

You’re ready to go.
