package com.safechestsx;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class VirtualChestHolder implements InventoryHolder {
    private final UUID owner;
    private final int index;

    public VirtualChestHolder(UUID owner, int index) {
        this.owner = owner;
        this.index = index;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
