# SafeChestsX

SafeChestsX is a lightweight Bukkit/Spigot plugin for claiming and protecting container blocks (chests, barrels, shulker boxes, etc.).

## Highlights (v4)
- Protect containers from unauthorized opening, breaking, explosions, pistons, and hoppers.
- Claim multiple containers at once using a selection wand.
- Trust or untrust other players per claim.
- Manage claims with add/remove, rename, and info actions.
- List your claims (or all claims if you are an admin).
- Optional limits for max claims per player and max containers per claim.
- Virtual chests accessible anywhere with optional Vault-powered purchases.
- Public API (service-registered) for other plugins to query claims, manage trust, and virtual chests.
- Supports Paper **1.21.4 - 1.21.11** (best tested on **1.21.4 - 1.21.7**).

## Major Changes in v4
- New V4 API surface with stronger claim and virtual chest helpers.
- Selection wand now uses Position 1/Position 2 clicks for consistency.
- Simplified selection management with a clear subcommand.
- Extension system with a registry for add-on modules.

## Core Commands
- `/claimchest` - receive the claim wand and guide book.
- `/claimchest claim <name>` - create a new claim from your current selection.
- `/claimchest trust <player>` / `/claimchest untrust <player>` - manage access for the claim you are looking at.
- `/claimchest manage <claim> <add|remove|info|delete|rename|trust|untrust>` - advanced management.
- `/claimchest selection` - show selection size, claim status, and position markers.
- `/claimchest selection clear` - clear your current selection.
- `/claimchest wand` - receive only the claim wand.
- `/claimchest guide` - receive only the guide book.
- `/claimchest info` - show info for the claimed container you are looking at.
- `/claimchest list` - list your claims.
- `/claimchest list all` - list all claims (admin only).
- `/claimchest unclaim` - remove the claim you are looking at (owner/admin).
- `/claimchest reload` - reload configuration (admin only).
- `/chest <number>` - open a virtual chest anywhere.
- `/chestpay <amount>` - buy more virtual chests (uses Vault economy).
- `/scxadmin` - admin management tools (reload, claim cleanup, virtual chest limits).
- `/bypasscc <player> <claim>` - admin bypass access (acts like trust).
- `/unbypasscc <player> <claim>` - remove bypass access.

## Customization
- Toggle protection for explosions, pistons, and hopper transfers in `settings` (`protect-explosions`, `protect-pistons`, `protect-hoppers`).
- Adjust wand material, lore, and claim limits in `config.yml`.
- Choose selection behavior via `settings.selection-mode` (`add` or `replace`).
- Toggle wand crafting with `settings.allow-wand-craft`.
- Claims are stored in a fast local SQLite database (`plugins/SafeChestsX/claims.db`) to reduce IO pressure.

## API
SafeChestsX registers a service with Bukkit's `ServicesManager`, similar to Vault. For new integrations use the V4 API.

```java
SafeChestsXApiV4 api = SafeChestsXApiV4Provider.get();
if (api != null) {
    boolean claimed = api.isClaimed(location);
}
```

```kotlin
val api = SafeChestsXApiV4Provider.get()
if (api != null) {
    val claimed = api.isClaimed(location)
}
```

V3 remains available, and the legacy Java API is still available but deprecated:
```java
SafeChestsXAPI legacy = SafeChestsXAPIProvider.get().orElse(null);
```

### Extensions
You can register extensions via the V4 API to add custom logic or integrations.
```kotlin
val api = SafeChestsXApiV4Provider.get() ?: return
api.registerExtension(object : SafeChestsXExtension {
    override val id = "example"
    override val displayName = "Example Extension"
})
```

## Download & Install
See [download.md](download.md) for the full step-by-step guide.
