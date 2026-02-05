package com.safechestsx.api;

import com.safechestsx.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.UUID;

@Deprecated
public interface SafeChestsXAPI {
    Claim getClaimByLocation(Location location);

    boolean isClaimed(Location location);

    Collection<Claim> getClaimsOwnedBy(UUID owner);

    int getVirtualChestLimit(UUID owner);

    int getVirtualChestCount(UUID owner);

    boolean addVirtualChestSlots(UUID owner, int amount);

    Inventory getVirtualChestInventory(UUID owner, int index);

    boolean openVirtualChest(Player player, int index);
}
