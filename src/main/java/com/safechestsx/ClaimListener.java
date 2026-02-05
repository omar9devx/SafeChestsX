package com.safechestsx;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;

public class ClaimListener implements Listener {
    private final SafeChestsXPlugin plugin;

    public ClaimListener(SafeChestsXPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.isContainerBlock(block)) {
            return;
        }
        Claim claim = plugin.getClaimsManager().getClaimByChestKey(plugin.getClaimsManager().getChestKey(block.getLocation()));
        if (claim == null) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessages().send(event.getPlayer(), "claim-protected");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        org.bukkit.Location location = plugin.getInventoryLocation(event.getInventory());
        if (location == null) {
            return;
        }
        Claim claim = plugin.getClaimsManager().getClaimByChestKey(plugin.getClaimsManager().getChestKey(location));
        if (claim == null) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (plugin.canAccessClaim(player, claim)) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessages().send(player, "claim-protected");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!plugin.isContainerBlock(block)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (plugin.isWand(item)) {
            if (event.getAction().isLeftClick()) {
                plugin.removeSelection(player, block.getLocation());
            } else if (event.getAction().isRightClick()) {
                plugin.addSelection(player, block.getLocation());
            }
            event.setCancelled(true);
            return;
        }
        Claim claim = plugin.getClaimsManager().getClaimByChestKey(plugin.getClaimsManager().getChestKey(block.getLocation()));
        if (claim == null) {
            return;
        }
        if (plugin.canAccessClaim(player, claim)) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessages().send(player, "claim-protected");
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!plugin.isProtectedExplosion(entity)) {
            return;
        }
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.isContainerBlock(block)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.isContainerBlock(block)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (isClaimedPistonMove(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (isClaimedPistonMove(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isClaimedInventory(event.getSource()) || isClaimedInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() != Material.WOODEN_SHOVEL) {
            return;
        }
        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    private boolean isClaimedInventory(Inventory inventory) {
        org.bukkit.Location location = plugin.getInventoryLocation(inventory);
        if (location == null) {
            return false;
        }
        return plugin.getClaimsManager().isClaimed(location);
    }

    private boolean isClaimedPistonMove(List<Block> blocks) {
        for (Block block : blocks) {
            if (!plugin.isContainerBlock(block)) {
                continue;
            }
            for (org.bukkit.Location location : plugin.getContainerLocations(block)) {
                if (plugin.getClaimsManager().isClaimed(location)) {
                    return true;
                }
            }
        }
        return false;
    }
}
