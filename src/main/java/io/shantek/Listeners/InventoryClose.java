package io.shantek.Listeners;

import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClose {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel)inventory.getHolder();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(this.customBarrelName) || barrelCustomName.equals(this.gambleBarrelName))) {
                Block block = barrel.getBlock();
                Block signBlock = this.getAttachedSignBlock(block);
                if (signBlock != null && signBlock.getState() instanceof Sign) {
                    Sign sign = (Sign)signBlock.getState();
                    this.updateStockLine(barrel, signBlock);
                }
            }
        }

    }


}
