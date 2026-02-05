package com.safechestsx;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class VirtualChestListener implements Listener {
    private final VirtualChestManager manager;

    public VirtualChestListener(VirtualChestManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        manager.saveInventory(event.getInventory());
    }
}
