# SafeChestsX

SafeChestsX is a lightweight Bukkit/Spigot plugin for claiming and protecting container blocks (chests, barrels, shulker boxes, etc.).

## Highlights
- Protect containers from unauthorized opening, breaking, explosions, pistons, and hoppers.
- Claim multiple containers at once using a selection wand.
- Trust or untrust other players per claim.
- Manage claims with add/remove, rename, and info actions.
- List your claims (or all claims if you are an admin).
- Optional limits for max claims per player and max containers per claim.

## Core Commands
- `/claimchest` - receive the claim wand and guide book.
- `/claimchest claim <name>` - create a new claim from your current selection.
- `/claimchest trust <player>` / `/claimchest untrust <player>` - manage access for the claim you are looking at.
- `/claimchest manage <claim> <add|remove|info|delete|rename|trust|untrust>` - advanced management.
- `/claimchest selection` - show selection size and whether any selected containers are already claimed.
- `/claimchest list` - list your claims.
- `/claimchest list all` - list all claims (admin only).
- `/claimchest reload` - reload configuration (admin only).
