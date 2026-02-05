package com.safechestsx;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class ClaimListener implements Listener {
    private final SafeChestsXPlugin plugin;

    public ClaimListener(SafeChestsXPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.isChestBlock(block)) {
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
        InventoryHolder holder = event.getInventory().getHolder();
        BlockState state = null;
        if (holder instanceof Chest chest) {
            state = chest;
        } else if (event.getInventory() instanceof DoubleChestInventory doubleChestInventory) {
            InventoryHolder left = doubleChestInventory.getLeftSide().getHolder();
            if (left instanceof Chest chest) {
                state = chest;
            }
        }
        if (state == null) {
            return;
        }
        Claim claim = plugin.getClaimsManager().getClaimByChestKey(plugin.getClaimsManager().getChestKey(state.getLocation()));
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
        if (!plugin.isChestBlock(block)) {
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
            if (plugin.isChestBlock(block)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.isChestBlock(block)) {
                iterator.remove();
            }
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
}
