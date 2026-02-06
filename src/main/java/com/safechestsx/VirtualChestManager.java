package com.safechestsx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualChestManager {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Object ioLock = new Object();
    private final Map<UUID, VirtualChestProfile> cache = new ConcurrentHashMap<>();

    public VirtualChestManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "virtual-chests.yml");
    }

    @SuppressWarnings("unchecked")
    public void load() {
        cache.clear();
        if (!dataFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            int limit = section.getInt("limit", getDefaultLimit());
            List<List<ItemStack>> chests = new ArrayList<>();
            ConfigurationSection chestSection = section.getConfigurationSection("chests");
            if (chestSection != null) {
                for (String chestKey : chestSection.getKeys(false)) {
                    int index = parseIndex(chestKey);
                    if (index <= 0) {
                        continue;
                    }
                    ensureSize(chests, index);
                    List<ItemStack> items = (List<ItemStack>) chestSection.getList(chestKey, List.of());
                    chests.set(index - 1, items);
                }
            }
            cache.put(uuid, new VirtualChestProfile(uuid, limit, chests));
        }
    }

    public void saveSync() {
        synchronized (ioLock) {
            FileConfiguration config = new YamlConfiguration();
            ConfigurationSection players = config.createSection("players");
            for (VirtualChestProfile profile : cache.values()) {
                ConfigurationSection section = players.createSection(profile.owner().toString());
                section.set("limit", profile.limit());
                ConfigurationSection chests = section.createSection("chests");
                for (int i = 0; i < profile.chests().size(); i++) {
                    List<ItemStack> items = profile.chests().get(i);
                    if (items == null || items.isEmpty()) {
                        continue;
                    }
                    chests.set(String.valueOf(i + 1), items);
                }
            }
            try {
                config.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save virtual-chests.yml: " + e.getMessage());
            }
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    public int getChestLimit(UUID owner) {
        return getProfile(owner).limit();
    }

    public int getChestCount(UUID owner) {
        return getProfile(owner).limit();
    }

    public int getProfileCount() {
        return cache.size();
    }

    public boolean addChestSlots(UUID owner, int amount) {
        if (amount <= 0) {
            return false;
        }
        int max = plugin.getConfig().getInt("settings.max-virtual-chests", 0);
        VirtualChestProfile profile = getProfile(owner);
        int target = profile.limit() + amount;
        if (max > 0 && target > max) {
            return false;
        }
        profile.setLimit(target);
        saveAsync();
        return true;
    }

    public boolean setChestLimit(UUID owner, int newLimit) {
        if (newLimit <= 0) {
            return false;
        }
        int max = plugin.getConfig().getInt("settings.max-virtual-chests", 0);
        if (max > 0 && newLimit > max) {
            return false;
        }
        VirtualChestProfile profile = getProfile(owner);
        profile.setLimit(newLimit);
        saveAsync();
        return true;
    }

    public boolean openChest(Player player, int index) {
        Inventory inventory = createChestInventory(player.getUniqueId(), index);
        if (inventory == null) {
            return false;
        }
        player.openInventory(inventory);
        return true;
    }

    public Inventory createChestInventory(UUID owner, int index) {
        VirtualChestProfile profile = getProfile(owner);
        if (index < 1 || index > profile.limit()) {
            return null;
        }
        ensureSize(profile.chests(), index);
        List<ItemStack> items = profile.chests().get(index - 1);
        int size = getChestSize();
        String title = plugin.getConfig().getString("settings.virtual-chest-title", "Virtual Chest {index}");
        title = title.replace("{index}", String.valueOf(index));
        VirtualChestHolder holder = new VirtualChestHolder(owner, index);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), size); i++) {
                inventory.setItem(i, items.get(i));
            }
        }
        return inventory;
    }

    public void saveInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof VirtualChestHolder chestHolder)) {
            return;
        }
        VirtualChestProfile profile = getProfile(chestHolder.getOwner());
        int index = chestHolder.getIndex();
        if (index < 1 || index > profile.limit()) {
            return;
        }
        ensureSize(profile.chests(), index);
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            items.add(item);
        }
        profile.chests().set(index - 1, items);
        saveAsync();
    }

    private VirtualChestProfile getProfile(UUID owner) {
        return cache.computeIfAbsent(owner, uuid -> new VirtualChestProfile(uuid, getDefaultLimit(), new ArrayList<>()));
    }

    private int getDefaultLimit() {
        int limit = plugin.getConfig().getInt("settings.default-virtual-chests", 10);
        return Math.max(1, limit);
    }

    private int getChestSize() {
        int size = plugin.getConfig().getInt("settings.virtual-chest-size", 27);
        if (size % 9 != 0) {
            size = 27;
        }
        return Math.max(9, Math.min(size, 54));
    }

    private void ensureSize(List<List<ItemStack>> list, int index) {
        while (list.size() < index) {
            list.add(new ArrayList<>());
        }
    }

    private int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static final class VirtualChestProfile {
        private final UUID owner;
        private int limit;
        private final List<List<ItemStack>> chests;

        private VirtualChestProfile(UUID owner, int limit, List<List<ItemStack>> chests) {
            this.owner = owner;
            this.limit = limit;
            this.chests = chests;
        }

        public UUID owner() {
            return owner;
        }

        public int limit() {
            return limit;
        }

        public List<List<ItemStack>> chests() {
            return chests;
        }

        public void setLimit(int newLimit) {
            if (newLimit < chests.size()) {
                chests.subList(newLimit, chests.size()).clear();
            }
            limit = newLimit;
        }
    }
}
