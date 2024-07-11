package io.shantek.listeners;

import io.shantek.SlotShop;
import io.shantek.helpers.Functions;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class InventoryCloseListener implements Listener {
    private final SlotShop plugin;

    public InventoryCloseListener(SlotShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel) inventory.getHolder();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(Functions.customBarrelName) || barrelCustomName.equals(Functions.gambleBarrelName))) {
                Block block = barrel.getBlock();
                Block signBlock = Functions.getAttachedSignBlock(block);
                if (signBlock != null && signBlock.getState() instanceof Sign) {
                    Functions.updateStockLine(barrel, signBlock);
                }
            }
        }
    }
}
