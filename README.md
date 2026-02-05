# SafeChestsX

SafeChestsX is a lightweight Bukkit/Spigot plugin for claiming and protecting container blocks (chests, barrels, shulker boxes, etc.).

## Highlights
- Protect containers from unauthorized opening, breaking, explosions, pistons, and hoppers.
- Claim multiple containers at once using a selection wand.
- Trust or untrust other players per claim.
- Manage claims with add/remove, rename, and info actions.
- List your claims (or all claims if you are an admin).
- Optional limits for max claims per player and max containers per claim.
- Virtual chests accessible anywhere with optional Vault-powered purchases.
- Public API (service-registered) for other plugins to query claims or virtual chests.

## Core Commands
- `/claimchest` - receive the claim wand and guide book.
- `/claimchest claim <name>` - create a new claim from your current selection.
- `/claimchest trust <player>` / `/claimchest untrust <player>` - manage access for the claim you are looking at.
- `/claimchest manage <claim> <add|remove|info|delete|rename|trust|untrust>` - advanced management.
- `/claimchest selection` - show selection size and whether any selected containers are already claimed.
- `/claimchest info` - show info for the claimed container you are looking at.
- `/claimchest list` - list your claims.
- `/claimchest list all` - list all claims (admin only).
- `/claimchest unclaim` - remove the claim you are looking at (owner/admin).
- `/claimchest reload` - reload configuration (admin only).
- `/chest <number>` - open a virtual chest anywhere.
- `/chestpay <amount>` - buy more virtual chests (uses Vault economy).

## API
SafeChestsX registers a service with Bukkit's `ServicesManager`, similar to Vault. Other plugins can access it like this:

```java
SafeChestsXAPI api = SafeChestsXAPIProvider.get().orElse(null);
if (api != null) {
    boolean claimed = api.isClaimed(location);
}
```

```kotlin
val api = SafeChestsXApi.get()
if (api != null) {
    val claimed = api.isClaimed(location)
}
```
