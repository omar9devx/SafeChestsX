package com.safechestsx;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.monster.Creeper;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SafeChestsXPlugin extends JavaPlugin {
    private ClaimsManager claimsManager;
    private Messages messages;
    private final Map<UUID, Set<org.bukkit.Location>> selections = new HashMap<>();
    private NamespacedKey wandKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        wandKey = new NamespacedKey(this, "claim_wand");
        messages = new Messages(getConfig());
        claimsManager = new ClaimsManager(this);
        claimsManager.load();
        ClaimCommand command = new ClaimCommand(this);
        getCommand("claimchest").setExecutor(command);
        getCommand("claimchest").setTabCompleter(command);
        registerListener(new ClaimListener(this));
    }

    @Override
    public void onDisable() {
        if (claimsManager != null) {
            claimsManager.saveSync();
        }
    }

    public void reloadMessages() {
        messages = new Messages(getConfig());
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public ClaimsManager getClaimsManager() {
        return claimsManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public boolean isChestBlock(Block block) {
        Material type = block.getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SHOVEL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void giveWandAndGuide(Player player) {
        player.getInventory().addItem(createGuideBook());
        player.getInventory().addItem(createWand());
        messages.send(player, "wand-received");
    }

    private ItemStack createWand() {
        Material material = Material.matchMaterial(getConfig().getString("settings.wand-material", "WOODEN_SHOVEL"));
        if (material == null) {
            material = Material.WOODEN_SHOVEL;
        }
        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(messages.color(getConfig().getString("settings.wand-name", "&aClaim Wand")));
        List<String> lore = getConfig().getStringList("settings.wand-lore");
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(messages.color(line));
        }
        meta.setLore(colored);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        return wand;
    }

    private ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(getConfig().getString("settings.book-title", "SafeChestsX Guide"));
        meta.setAuthor(getConfig().getString("settings.book-author", "SafeChestsX"));
        meta.setPages(List.of(
                "SafeChestsX Quick Guide\n\n1) Run /claimchest to get the wand.\n2) Right click chests to select.\n3) /claimchest claim <name>\n4) /claimchest trust <player> while looking at a chest.\n\nUse /claimchest manage for advanced actions.",
                "Manage Actions:\n- add/remove (with selection)\n- info\n- delete\n- rename -a <newName>\n- trust/untrust -a <player>\n\nClaims prevent breaking, opening, and explosions."
        ));
        book.setItemMeta(meta);
        return book;
    }

    public void addSelection(Player player, org.bukkit.Location location) {
        if (!isChestBlock(location.getBlock())) {
            messages.send(player, "selection-invalid");
            return;
        }
        selections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(location);
        messages.send(player, "selection-add");
    }

    public void removeSelection(Player player, org.bukkit.Location location) {
        if (!isChestBlock(location.getBlock())) {
            messages.send(player, "selection-invalid");
            return;
        }
        selections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).remove(location);
        messages.send(player, "selection-remove");
    }

    public Set<org.bukkit.Location> getSelection(Player player) {
        return new HashSet<>(selections.getOrDefault(player.getUniqueId(), Set.of()));
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    public boolean anyClaimed(Set<org.bukkit.Location> locations) {
        for (org.bukkit.Location location : locations) {
            if (claimsManager.isClaimed(location)) {
                return true;
            }
        }
        return false;
    }

    public Claim getClaimPlayerIsLookingAt(Player player) {
        int range = getConfig().getInt("settings.selection-range", 5);
        Block target = player.getTargetBlockExact(range);
        if (target == null || !isChestBlock(target)) {
            return null;
        }
        return claimsManager.getClaimByChestKey(claimsManager.getChestKey(target.getLocation()));
    }

    public boolean canAccessClaim(Player player, Claim claim) {
        if (player.hasPermission("safechestsx.admin")) {
            return true;
        }
        return claimsManager.isTrusted(claim, player.getUniqueId());
    }

    public boolean isOwnerOrAdmin(Player player, Claim claim) {
        return player.hasPermission("safechestsx.admin") || claim.getOwner().equals(player.getUniqueId());
    }

    public boolean isProtectedExplosion(Entity entity) {
        return entity instanceof Creeper || entity instanceof TNTPrimed || entity instanceof ExplosiveMinecart;
    }
}
