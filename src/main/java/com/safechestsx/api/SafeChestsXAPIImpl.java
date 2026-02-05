package com.safechestsx.api;

import com.safechestsx.Claim;
import com.safechestsx.ClaimsManager;
import com.safechestsx.VirtualChestManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.UUID;

public class SafeChestsXAPIImpl implements SafeChestsXAPI {
    private final ClaimsManager claimsManager;
    private final VirtualChestManager virtualChestManager;

    public SafeChestsXAPIImpl(ClaimsManager claimsManager, VirtualChestManager virtualChestManager) {
        this.claimsManager = claimsManager;
        this.virtualChestManager = virtualChestManager;
    }

    @Override
    public Claim getClaimByLocation(Location location) {
        if (location == null) {
            return null;
        }
        return claimsManager.getClaimByChestKey(claimsManager.getChestKey(location));
    }

    @Override
    public boolean isClaimed(Location location) {
        return location != null && claimsManager.isClaimed(location);
    }

    @Override
    public Collection<Claim> getClaimsOwnedBy(UUID owner) {
        return claimsManager.getClaimsOwnedBy(owner);
    }

    @Override
    public int getVirtualChestLimit(UUID owner) {
        return virtualChestManager.getChestLimit(owner);
    }

    @Override
    public int getVirtualChestCount(UUID owner) {
        return virtualChestManager.getChestCount(owner);
    }

    @Override
    public boolean addVirtualChestSlots(UUID owner, int amount) {
        return virtualChestManager.addChestSlots(owner, amount);
    }

    @Override
    public Inventory getVirtualChestInventory(UUID owner, int index) {
        return virtualChestManager.createChestInventory(owner, index);
    }

    @Override
    public boolean openVirtualChest(Player player, int index) {
        return virtualChestManager.openChest(player, index);
    }
}
