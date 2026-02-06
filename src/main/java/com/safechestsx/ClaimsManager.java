package com.safechestsx;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClaimsManager {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final com.safechestsx.storage.ClaimsDatabase database;
    private final Map<String, Claim> claimsByName = new HashMap<>();
    private final Map<String, String> claimByChestKey = new HashMap<>();
    private final Object ioLock = new Object();

    public ClaimsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "claims.yml");
        this.database = new com.safechestsx.storage.ClaimsDatabase(plugin);
    }

    public void load() {
        claimsByName.clear();
        claimByChestKey.clear();
        if (database.isEmpty() && dataFile.exists()) {
            loadFromYaml();
            saveSync();
            return;
        }
        for (com.safechestsx.storage.ClaimRecord record : database.loadAll()) {
            Claim claim = new Claim(record.getName(), record.getOwner());
            claim.getTrusted().addAll(record.getTrusted());
            claim.getBypassed().addAll(record.getBypassed());
            claim.getChestKeys().addAll(record.getChestKeys());
            for (String key : record.getChestKeys()) {
                claimByChestKey.put(key, record.getName());
            }
            claimsByName.put(record.getName().toLowerCase(), claim);
        }
    }

    private void loadFromYaml() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("claims");
        if (section == null) {
            return;
        }
        for (String claimName : section.getKeys(false)) {
            ConfigurationSection claimSection = section.getConfigurationSection(claimName);
            if (claimSection == null) {
                continue;
            }
            String ownerRaw = claimSection.getString("owner");
            if (ownerRaw == null) {
                continue;
            }
            UUID owner = UUID.fromString(ownerRaw);
            Claim claim = new Claim(claimName, owner);
            for (String uuid : claimSection.getStringList("trusted")) {
                claim.getTrusted().add(UUID.fromString(uuid));
            }
            for (String key : claimSection.getStringList("chests")) {
                claim.getChestKeys().add(key);
                claimByChestKey.put(key, claimName);
            }
            claimsByName.put(claimName.toLowerCase(), claim);
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    public void saveSync() {
        synchronized (ioLock) {
            database.saveAll(claimsByName.values());
        }
    }

    public boolean claimExists(String name) {
        return claimsByName.containsKey(name.toLowerCase());
    }

    public Claim getClaim(String name) {
        return claimsByName.get(name.toLowerCase());
    }

    public Collection<Claim> getClaims() {
        return claimsByName.values();
    }

    public Collection<Claim> getClaimsOwnedBy(UUID owner) {
        Set<Claim> owned = new HashSet<>();
        for (Claim claim : claimsByName.values()) {
            if (claim.getOwner().equals(owner)) {
                owned.add(claim);
            }
        }
        return owned;
    }

    public Claim getClaimByChestKey(String key) {
        String claimName = claimByChestKey.get(key);
        return claimName == null ? null : claimsByName.get(claimName.toLowerCase());
    }

    public String getChestKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public boolean isClaimed(Location location) {
        return claimByChestKey.containsKey(getChestKey(location));
    }

    public Claim createClaim(String name, UUID owner, Set<Location> chests) {
        Claim claim = new Claim(name, owner);
        for (Location location : chests) {
            String key = getChestKey(location);
            claim.getChestKeys().add(key);
            claimByChestKey.put(key, name);
        }
        claimsByName.put(name.toLowerCase(), claim);
        return claim;
    }

    public int addChests(Claim claim, Set<Location> chests) {
        int added = 0;
        for (Location location : chests) {
            String key = getChestKey(location);
            if (claimByChestKey.containsKey(key)) {
                continue;
            }
            claim.getChestKeys().add(key);
            claimByChestKey.put(key, claim.getName());
            added++;
        }
        return added;
    }

    public int removeChests(Claim claim, Set<Location> chests) {
        int removed = 0;
        for (Location location : chests) {
            String key = getChestKey(location);
            if (!claim.getChestKeys().remove(key)) {
                continue;
            }
            claimByChestKey.remove(key);
            removed++;
        }
        return removed;
    }

    public void deleteClaim(Claim claim) {
        for (String key : claim.getChestKeys()) {
            claimByChestKey.remove(key);
        }
        claimsByName.remove(claim.getName().toLowerCase());
    }

    public void renameClaim(Claim claim, String newName) {
        claimsByName.remove(claim.getName().toLowerCase());
        Claim renamed = new Claim(newName, claim.getOwner());
        renamed.getChestKeys().addAll(claim.getChestKeys());
        renamed.getTrusted().addAll(claim.getTrusted());
        claimsByName.put(newName.toLowerCase(), renamed);
        for (String key : renamed.getChestKeys()) {
            claimByChestKey.put(key, newName);
        }
    }

    public boolean isTrusted(Claim claim, UUID playerId) {
        return claim.getOwner().equals(playerId)
                || claim.getTrusted().contains(playerId)
                || claim.getBypassed().contains(playerId);
    }

    public String getTrustedNames(Claim claim) {
        if (claim.getTrusted().isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (UUID uuid : claim.getTrusted()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(player.getName() == null ? uuid.toString() : player.getName());
        }
        return builder.toString();
    }

    public String getBypassNames(Claim claim) {
        if (claim.getBypassed().isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (UUID uuid : claim.getBypassed()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(player.getName() == null ? uuid.toString() : player.getName());
        }
        return builder.toString();
    }
}
