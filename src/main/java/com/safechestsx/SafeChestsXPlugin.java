package com.safechestsx;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.monster.Creeper;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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
import java.util.LinkedHashSet;
import java.util.UUID;

public class SafeChestsXPlugin extends JavaPlugin {
    private ClaimsManager claimsManager;
    private VirtualChestManager virtualChestManager;
    private Messages messages;
    private final Map<UUID, Set<org.bukkit.Location>> selections = new HashMap<>();
    private NamespacedKey wandKey;
    private net.milkbowl.vault.economy.Economy economy;

    @Override
    public void onEnable() {
        if (!com.safechestsx.VersionSupport.INSTANCE.verifyOrDisable(this)) {
            return;
        }
        saveDefaultConfig();
        wandKey = new NamespacedKey(this, "claim_wand");
        messages = new Messages(getConfig());
        claimsManager = new ClaimsManager(this);
        claimsManager.load();
        virtualChestManager = new VirtualChestManager(this);
        virtualChestManager.load();
        setupEconomy();
        ClaimCommand command = new ClaimCommand(this);
        if (getCommand("claimchest") != null) {
            getCommand("claimchest").setExecutor(command);
            getCommand("claimchest").setTabCompleter(command);
        }
        ChestCommand chestCommand = new ChestCommand(this, virtualChestManager);
        if (getCommand("chest") != null) {
            getCommand("chest").setExecutor(chestCommand);
            getCommand("chest").setTabCompleter(chestCommand);
        }
        if (getCommand("chestpay") != null) {
            getCommand("chestpay").setExecutor(chestCommand);
            getCommand("chestpay").setTabCompleter(chestCommand);
        }
        registerListener(new ClaimListener(this));
        registerListener(new VirtualChestListener(virtualChestManager));
        getServer().getServicesManager().register(com.safechestsx.api.SafeChestsXAPI.class,
                new com.safechestsx.api.SafeChestsXAPIImpl(claimsManager, virtualChestManager),
                this,
                org.bukkit.plugin.ServicePriority.Normal);
        getServer().getServicesManager().register(com.safechestsx.api.v3.SafeChestsXApiV3.class,
                new com.safechestsx.api.v3.SafeChestsXApiV3Impl(claimsManager, virtualChestManager),
                this,
                org.bukkit.plugin.ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (claimsManager != null) {
            claimsManager.saveSync();
        }
        if (virtualChestManager != null) {
            virtualChestManager.saveSync();
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

    public VirtualChestManager getVirtualChestManager() {
        return virtualChestManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        return economy;
    }

    public String formatCurrency(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format("$%,.2f", amount);
    }

    public boolean isContainerBlock(Block block) {
        BlockState state = block.getState();
        return state instanceof InventoryHolder;
    }

    public org.bukkit.Location getInventoryLocation(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
            InventoryHolder left = doubleChest.getLeftSide();
            if (left instanceof BlockState leftState) {
                return leftState.getLocation();
            }
        }
        if (holder instanceof BlockState state) {
            return state.getLocation();
        }
        return null;
    }

    public Set<org.bukkit.Location> getContainerLocations(Block block) {
        Set<org.bukkit.Location> locations = new LinkedHashSet<>();
        if (!isContainerBlock(block)) {
            return locations;
        }
        BlockState state = block.getState();
        if (state instanceof org.bukkit.block.Chest chest) {
            Inventory inventory = chest.getInventory();
            InventoryHolder holder = inventory.getHolder();
            if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
                InventoryHolder left = doubleChest.getLeftSide();
                InventoryHolder right = doubleChest.getRightSide();
                if (left instanceof BlockState leftState) {
                    locations.add(leftState.getLocation());
                }
                if (right instanceof BlockState rightState) {
                    locations.add(rightState.getLocation());
                }
                return locations;
            }
        }
        locations.add(block.getLocation());
        return locations;
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
                "SafeChestsX Quick Guide\n\n1) Run /claimchest to get the wand.\n2) Right click containers to select.\n3) /claimchest claim <name>\n4) /claimchest trust <player> while looking at a container.\n\nUse /claimchest manage for advanced actions.",
                "Manage Actions:\n- add/remove (with selection)\n- info\n- delete\n- rename -a <newName>\n- trust/untrust -a <player>\n\nClaims prevent breaking, opening, explosions, and hopper access."
        ));
        book.setItemMeta(meta);
        return book;
    }

    public void addSelection(Player player, org.bukkit.Location location) {
        if (!isContainerBlock(location.getBlock())) {
            messages.send(player, "selection-invalid");
            return;
        }
        Set<org.bukkit.Location> targets = getContainerLocations(location.getBlock());
        selections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).addAll(targets);
        messages.send(player, "selection-add");
    }

    public void removeSelection(Player player, org.bukkit.Location location) {
        if (!isContainerBlock(location.getBlock())) {
            messages.send(player, "selection-invalid");
            return;
        }
        Set<org.bukkit.Location> targets = getContainerLocations(location.getBlock());
        selections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).removeAll(targets);
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
        if (target == null || !isContainerBlock(target)) {
            return null;
        }
        return claimsManager.getClaimByChestKey(claimsManager.getChestKey(target.getLocation()));
    }

    public boolean isValidClaimName(String name) {
        if (name == null) {
            return false;
        }
        int minLength = getConfig().getInt("settings.claim-name-min-length", 3);
        int maxLength = getConfig().getInt("settings.claim-name-max-length", 24);
        String pattern = getConfig().getString("settings.claim-name-regex", "^[A-Za-z0-9_-]+$");
        if (name.length() < minLength || name.length() > maxLength) {
            return false;
        }
        return name.matches(pattern);
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

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        var registration = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
        }
    }
}
