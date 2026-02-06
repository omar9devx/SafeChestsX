package com.safechestsx;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ClaimCommand implements CommandExecutor, TabCompleter {
    private final SafeChestsXPlugin plugin;

    public ClaimCommand(SafeChestsXPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (!player.hasPermission("safechestsx.use")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.giveWandAndGuide(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "claim" -> handleClaim(player, args);
            case "trust" -> handleTrust(player, args, true);
            case "untrust" -> handleTrust(player, args, false);
            case "manage" -> handleManage(player, args);
            case "list" -> handleList(player, args);
            case "selection" -> handleSelection(player, args);
            case "info" -> handleInfo(player);
            case "unclaim" -> handleUnclaim(player);
            case "help" -> handleHelp(player);
            case "reload" -> handleReload(player);
            default -> plugin.giveWandAndGuide(player);
        }
        return true;
    }

    private void handleClaim(Player player, String[] args) {
        if (!player.hasPermission("safechestsx.claim")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("/claimchest claim <name>");
            return;
        }
        String name = args[1];
        if (!plugin.isValidClaimName(name)) {
            plugin.getMessages().send(player, "claim-invalid");
            return;
        }
        if (plugin.getClaimsManager().claimExists(name)) {
            plugin.getMessages().send(player, "claim-exists");
            return;
        }
        int maxClaims = plugin.getConfig().getInt("settings.max-claims-per-player", 0);
        if (maxClaims > 0 && plugin.getClaimsManager().getClaimsOwnedBy(player.getUniqueId()).size() >= maxClaims) {
            plugin.getMessages().send(player, "claim-limit", Map.of("limit", String.valueOf(maxClaims)));
            return;
        }
        Set<org.bukkit.Location> selection = plugin.getSelection(player);
        if (selection.isEmpty()) {
            plugin.getMessages().send(player, "selection-empty");
            return;
        }
        int maxChests = plugin.getConfig().getInt("settings.max-chests-per-claim", 0);
        if (maxChests > 0 && selection.size() > maxChests) {
            plugin.getMessages().send(player, "chest-limit", Map.of(
                    "count", String.valueOf(selection.size()),
                    "limit", String.valueOf(maxChests)
            ));
            return;
        }
        if (plugin.anyClaimed(selection)) {
            plugin.getMessages().send(player, "selection-claimed");
            return;
        }
        Claim claim = plugin.getClaimsManager().createClaim(name, player.getUniqueId(), selection);
        plugin.clearSelection(player);
        plugin.getClaimsManager().saveAsync();
        plugin.getMessages().send(player, "claim-created", Map.of(
                "claim", claim.getName(),
                "count", String.valueOf(claim.getChestKeys().size())
        ));
    }

    private void handleTrust(Player player, String[] args, boolean add) {
        if (!player.hasPermission("safechestsx.trust")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("/claimchest " + (add ? "trust" : "untrust") + " <player>");
            return;
        }
        Claim claim = plugin.getClaimPlayerIsLookingAt(player);
        if (claim == null) {
            plugin.getMessages().send(player, "trust-target");
            return;
        }
        if (!plugin.isOwnerOrAdmin(player, claim)) {
            plugin.getMessages().send(player, "claim-not-owner");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            plugin.getMessages().send(player, "player-not-found");
            return;
        }
        if (add) {
            claim.getTrusted().add(target.getUniqueId());
            plugin.getMessages().send(player, "trust-added", Map.of(
                    "player", getDisplayName(target, args[1]),
                    "claim", claim.getName()
            ));
        } else {
            claim.getTrusted().remove(target.getUniqueId());
            plugin.getMessages().send(player, "trust-removed", Map.of(
                    "player", getDisplayName(target, args[1]),
                    "claim", claim.getName()
            ));
        }
        plugin.getClaimsManager().saveAsync();
    }

    private void handleManage(Player player, String[] args) {
        if (!player.hasPermission("safechestsx.manage")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("/claimchest manage <claim> <action> -a <args>");
            return;
        }
        String claimName = args[1];
        Claim claim = plugin.getClaimsManager().getClaim(claimName);
        if (claim == null) {
            plugin.getMessages().send(player, "claim-missing");
            return;
        }
        if (!plugin.isOwnerOrAdmin(player, claim)) {
            plugin.getMessages().send(player, "claim-not-owner");
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        String extra = parseArgs(args);
        switch (action) {
            case "add" -> manageAdd(player, claim);
            case "remove" -> manageRemove(player, claim);
            case "info" -> manageInfo(player, claim);
            case "delete" -> manageDelete(player, claim);
            case "rename" -> manageRename(player, claim, extra);
            case "trust" -> manageTrust(player, claim, extra, true);
            case "untrust" -> manageTrust(player, claim, extra, false);
            default -> player.sendMessage("Unknown action.");
        }
    }

    private String parseArgs(String[] args) {
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--args")) {
                if (i + 1 < args.length) {
                    return args[i + 1];
                }
            }
        }
        return "";
    }

    private void manageAdd(Player player, Claim claim) {
        Set<org.bukkit.Location> selection = plugin.getSelection(player);
        if (selection.isEmpty()) {
            plugin.getMessages().send(player, "selection-empty");
            return;
        }
        int maxChests = plugin.getConfig().getInt("settings.max-chests-per-claim", 0);
        if (maxChests > 0 && claim.getChestKeys().size() + selection.size() > maxChests) {
            plugin.getMessages().send(player, "chest-limit", Map.of(
                    "count", String.valueOf(claim.getChestKeys().size() + selection.size()),
                    "limit", String.valueOf(maxChests)
            ));
            return;
        }
        int added = plugin.getClaimsManager().addChests(claim, selection);
        plugin.clearSelection(player);
        plugin.getClaimsManager().saveAsync();
        plugin.getMessages().send(player, "claim-added", Map.of(
                "claim", claim.getName(),
                "count", String.valueOf(added)
        ));
    }

    private void manageRemove(Player player, Claim claim) {
        Set<org.bukkit.Location> selection = plugin.getSelection(player);
        if (selection.isEmpty()) {
            plugin.getMessages().send(player, "selection-empty");
            return;
        }
        int removed = plugin.getClaimsManager().removeChests(claim, selection);
        plugin.clearSelection(player);
        plugin.getClaimsManager().saveAsync();
        plugin.getMessages().send(player, "claim-removed", Map.of(
                "claim", claim.getName(),
                "count", String.valueOf(removed)
        ));
    }

    private void manageInfo(Player player, Claim claim) {
        plugin.getMessages().send(player, "manage-info", Map.of(
                "claim", claim.getName(),
                "owner", Bukkit.getOfflinePlayer(claim.getOwner()).getName() == null ? claim.getOwner().toString() : Bukkit.getOfflinePlayer(claim.getOwner()).getName(),
                "count", String.valueOf(claim.getChestKeys().size()),
                "trusted", plugin.getClaimsManager().getTrustedNames(claim)
        ));
    }

    private void manageDelete(Player player, Claim claim) {
        plugin.getClaimsManager().deleteClaim(claim);
        plugin.getClaimsManager().saveAsync();
        plugin.getMessages().send(player, "manage-deleted", Map.of("claim", claim.getName()));
    }

    private void manageRename(Player player, Claim claim, String newName) {
        if (newName.isBlank()) {
            player.sendMessage("/claimchest manage " + claim.getName() + " rename -a <newName>");
            return;
        }
        if (!plugin.isValidClaimName(newName)) {
            plugin.getMessages().send(player, "claim-invalid");
            return;
        }
        if (plugin.getClaimsManager().claimExists(newName)) {
            plugin.getMessages().send(player, "claim-exists");
            return;
        }
        plugin.getClaimsManager().renameClaim(claim, newName);
        plugin.getClaimsManager().saveAsync();
        plugin.getMessages().send(player, "manage-renamed", Map.of("claim", newName));
    }

    private void manageTrust(Player player, Claim claim, String playerName, boolean add) {
        if (playerName.isBlank()) {
            player.sendMessage("/claimchest manage " + claim.getName() + " " + (add ? "trust" : "untrust") + " -a <player>");
            return;
        }
        OfflinePlayer target = resolvePlayer(playerName);
        if (target == null) {
            plugin.getMessages().send(player, "player-not-found");
            return;
        }
        if (add) {
            claim.getTrusted().add(target.getUniqueId());
            plugin.getMessages().send(player, "trust-added", Map.of(
                    "player", getDisplayName(target, playerName),
                    "claim", claim.getName()
            ));
        } else {
            claim.getTrusted().remove(target.getUniqueId());
            plugin.getMessages().send(player, "trust-removed", Map.of(
                    "player", getDisplayName(target, playerName),
                    "claim", claim.getName()
            ));
        }
        plugin.getClaimsManager().saveAsync();
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("safechestsx.admin")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        plugin.reloadConfig();
        plugin.reloadMessages();
        plugin.getMessages().send(player, "reload");
    }

    private void handleSelection(Player player, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
            plugin.clearSelection(player);
            plugin.getMessages().send(player, "selection-cleared");
            return;
        }
        Set<org.bukkit.Location> selection = plugin.getSelection(player);
        SafeChestsXPlugin.SelectionSummary summary = plugin.getSelectionSummary(player);
        boolean isClaimed = plugin.anyClaimed(selection);
        String claimedKey = isClaimed ? "selection-status-yes" : "selection-status-no";
        String claimed = plugin.getMessages().getRaw(claimedKey);
        if (claimed.isBlank()) {
            claimed = isClaimed ? "yes" : "no";
        }
        plugin.getMessages().send(player, "selection-status", Map.of(
                "count", String.valueOf(selection.size()),
                "claimed", claimed,
                "pos1", plugin.formatLocation(summary.getPositionOne()),
                "pos2", plugin.formatLocation(summary.getPositionTwo())
        ));
    }

    private void handleList(Player player, String[] args) {
        boolean listAll = args.length > 1 && args[1].equalsIgnoreCase("all");
        if (listAll && !player.hasPermission("safechestsx.admin")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        List<Claim> claims = listAll
                ? new ArrayList<>(plugin.getClaimsManager().getClaims())
                : new ArrayList<>(plugin.getClaimsManager().getClaimsOwnedBy(player.getUniqueId()));
        if (claims.isEmpty()) {
            plugin.getMessages().send(player, "list-empty");
            return;
        }
        String scopeKey = listAll ? "list-scope-all" : "list-scope-yours";
        String scope = plugin.getMessages().getRaw(scopeKey);
        if (scope.isBlank()) {
            scope = listAll ? "all" : "yours";
        }
        plugin.getMessages().send(player, "list-header", Map.of(
                "count", String.valueOf(claims.size()),
                "scope", scope
        ));
        claims.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (Claim claim : claims) {
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(claim.getOwner()).getName();
            plugin.getMessages().send(player, "list-entry", Map.of(
                    "claim", claim.getName(),
                    "owner", ownerName == null ? claim.getOwner().toString() : ownerName,
                    "count", String.valueOf(claim.getChestKeys().size())
            ));
        }
    }

    private void handleInfo(Player player) {
        Claim claim = plugin.getClaimPlayerIsLookingAt(player);
        if (claim == null) {
            plugin.getMessages().send(player, "info-target");
            return;
        }
        plugin.getMessages().send(player, "manage-info", Map.of(
                "claim", claim.getName(),
                "owner", Bukkit.getOfflinePlayer(claim.getOwner()).getName() == null ? claim.getOwner().toString() : Bukkit.getOfflinePlayer(claim.getOwner()).getName(),
                "count", String.valueOf(claim.getChestKeys().size()),
                "trusted", plugin.getClaimsManager().getTrustedNames(claim)
        ));
    }

    private void handleUnclaim(Player player) {
        Claim claim = plugin.getClaimPlayerIsLookingAt(player);
        if (claim == null) {
            plugin.getMessages().send(player, "unclaim-target");
            return;
        }
        if (!plugin.isOwnerOrAdmin(player, claim)) {
            plugin.getMessages().send(player, "claim-not-owner");
            return;
        }
        plugin.getClaimsManager().deleteClaim(claim);
        plugin.getClaimsManager().saveAsync();
        plugin.getMessages().send(player, "unclaim-success", Map.of("claim", claim.getName()));
    }

    private void handleHelp(Player player) {
        plugin.getMessages().sendList(player, "claim-help", Map.of());
    }

    private OfflinePlayer resolvePlayer(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target != null && target.getName() != null) {
            return target;
        }
        target = Bukkit.getOfflinePlayer(name);
        return target.getName() == null ? null : target;
    }

    private String getDisplayName(OfflinePlayer player, String fallback) {
        return player.getName() == null ? fallback : player.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("claim", "trust", "untrust", "manage", "list", "selection", "info", "unclaim", "help", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("manage")) {
            return filter(plugin.getClaimsManager().getClaims().stream().map(Claim::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("manage")) {
            return filter(List.of("add", "remove", "info", "delete", "rename", "trust", "untrust"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return filter(List.of("all"), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        if (input == null || input.isBlank()) {
            return options;
        }
        List<String> matches = new ArrayList<>();
        String lower = input.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
