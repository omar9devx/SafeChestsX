package com.safechestsx;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChestCommand implements CommandExecutor, TabCompleter {
    private final SafeChestsXPlugin plugin;
    private final VirtualChestManager chestManager;
    private final Map<UUID, PendingPurchase> pendingPurchases = new ConcurrentHashMap<>();

    public ChestCommand(SafeChestsXPlugin plugin, VirtualChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("chest")) {
            return handleChest(player, args);
        }
        if (command.getName().equalsIgnoreCase("chestpay")) {
            return handleChestPay(player, args);
        }
        return true;
    }

    private boolean handleChest(Player player, String[] args) {
        if (!player.hasPermission("safechestsx.chest")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessages().sendList(player, "chest-help", Map.of());
            return true;
        }
        int index;
        try {
            index = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            plugin.getMessages().send(player, "chest-invalid");
            return true;
        }
        int limit = chestManager.getChestLimit(player.getUniqueId());
        if (index < 1 || index > limit) {
            plugin.getMessages().send(player, "chest-locked", Map.of(
                    "index", String.valueOf(index),
                    "limit", String.valueOf(limit)
            ));
            return true;
        }
        if (!chestManager.openChest(player, index)) {
            plugin.getMessages().send(player, "chest-invalid");
        }
        return true;
    }

    private boolean handleChestPay(Player player, String[] args) {
        if (!player.hasPermission("safechestsx.chestpay")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessages().sendList(player, "chestpay-help", Map.of());
            return true;
        }
        if (plugin.getEconomy() == null) {
            plugin.getMessages().send(player, "vault-missing");
            return true;
        }
        if (args[0].equalsIgnoreCase("confirm")) {
            return confirmPurchase(player);
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            pendingPurchases.remove(player.getUniqueId());
            plugin.getMessages().send(player, "chestpay-cancelled");
            return true;
        }
        int count;
        try {
            count = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            plugin.getMessages().send(player, "chestpay-invalid");
            return true;
        }
        if (count <= 0) {
            plugin.getMessages().send(player, "chestpay-invalid");
            return true;
        }
        int max = plugin.getConfig().getInt("settings.max-virtual-chests", 0);
        int limit = chestManager.getChestLimit(player.getUniqueId());
        if (max > 0 && limit + count > max) {
            plugin.getMessages().send(player, "chestpay-max", Map.of(
                    "limit", String.valueOf(max)
            ));
            return true;
        }
        double price = plugin.getConfig().getDouble("settings.virtual-chest-price", 1_000_000D);
        double total = price * count;
        pendingPurchases.put(player.getUniqueId(), new PendingPurchase(count, total, System.currentTimeMillis()));
        plugin.getMessages().send(player, "chestpay-pending", Map.of(
                "count", String.valueOf(count),
                "total", plugin.formatCurrency(total),
                "price", plugin.formatCurrency(price)
        ));
        return true;
    }

    private boolean confirmPurchase(Player player) {
        PendingPurchase pending = pendingPurchases.get(player.getUniqueId());
        if (pending == null) {
            plugin.getMessages().send(player, "chestpay-none");
            return true;
        }
        long timeoutMs = plugin.getConfig().getLong("settings.chestpay-confirm-timeout-ms", 60000L);
        if (System.currentTimeMillis() - pending.createdAt() > timeoutMs) {
            pendingPurchases.remove(player.getUniqueId());
            plugin.getMessages().send(player, "chestpay-expired");
            return true;
        }
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            plugin.getMessages().send(player, "vault-missing");
            return true;
        }
        if (!economy.has(player, pending.total())) {
            plugin.getMessages().send(player, "chestpay-insufficient", Map.of(
                    "total", plugin.formatCurrency(pending.total())
            ));
            return true;
        }
        economy.withdrawPlayer(player, pending.total());
        boolean added = chestManager.addChestSlots(player.getUniqueId(), pending.count());
        pendingPurchases.remove(player.getUniqueId());
        if (!added) {
            plugin.getMessages().send(player, "chestpay-max", Map.of(
                    "limit", String.valueOf(plugin.getConfig().getInt("settings.max-virtual-chests", 0))
            ));
            return true;
        }
        plugin.getMessages().send(player, "chestpay-success", Map.of(
                "count", String.valueOf(pending.count()),
                "total", plugin.formatCurrency(pending.total())
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("chest")) {
            if (args.length == 1) {
                List<String> options = new ArrayList<>();
                if (sender instanceof Player player) {
                    int limit = chestManager.getChestLimit(player.getUniqueId());
                    for (int i = 1; i <= Math.min(limit, 10); i++) {
                        options.add(String.valueOf(i));
                    }
                }
                options.add("help");
                return filter(options, args[0]);
            }
            return Collections.emptyList();
        }
        if (command.getName().equalsIgnoreCase("chestpay")) {
            if (args.length == 1) {
                return filter(List.of("confirm", "cancel", "help"), args[0]);
            }
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

    private record PendingPurchase(int count, double total, long createdAt) {
    }
}
