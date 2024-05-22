package io.shantek.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

public class SignChange {

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block signBlock = event.getBlock();
        Block attachedBlock = signBlock.getRelative(this.getAttachedFace(signBlock));
        if (attachedBlock.getState() instanceof Barrel) {
            Barrel barrel = (Barrel)attachedBlock.getState();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(this.customBarrelName) || barrelCustomName.equals(this.gambleBarrelName))) {
                if (barrelCustomName.equals(this.gambleBarrelName) && !player.hasPermission("slotshop.create.gamblebarrel")) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to set up a Gamble Shop!");
                    signBlock.breakNaturally();
                } else if (barrelCustomName.equals(this.customBarrelName) && !player.hasPermission("slotshop.create")) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to set up a Slot Shop!");
                    signBlock.breakNaturally();
                } else {
                    String oldOwner = event.getLine(0);
                    String description = event.getLine(1);
                    String priceString = event.getLine(2);
                    if (!description.isEmpty() && !priceString.isEmpty()) {
                        String linePrefix = ChatColor.GREEN + "Cost: ";
                        if (priceString.startsWith(linePrefix)) {
                            priceString = priceString.replace(linePrefix, "");
                        }

                        double price;
                        try {
                            price = Double.parseDouble(priceString);
                        } catch (NumberFormatException var16) {
                            player.sendMessage(ChatColor.RED + "Invalid price!");
                            signBlock.breakNaturally();
                            return;
                        }

                        String ownerName = player.getName();
                        ItemStack[] inventoryContents = barrel.getInventory().getContents();
                        if (inventoryContents.length > 0 && inventoryContents[0] != null && inventoryContents[0].getType() == Material.PAPER) {
                            ItemMeta itemMeta = inventoryContents[0].getItemMeta();
                            if (itemMeta != null && itemMeta.hasDisplayName()) {
                                ownerName = itemMeta.getDisplayName();
                            }
                        }

                        if (player.isOp() && oldOwner != null && !oldOwner.isEmpty()) {
                            OfflinePlayer owner = Bukkit.getOfflinePlayer(oldOwner);
                            if (!owner.hasPlayedBefore()) {
                                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Player name is invalid. Please fix.");
                                return;
                            }

                            ownerName = oldOwner;
                        }

                        event.setLine(0, ownerName);
                        event.setLine(2, ChatColor.GREEN + "Cost: " + price);
                        event.setLine(3, ChatColor.BLACK + "Stock: " + this.countNonNullSlots(inventoryContents));
                        signBlock.getState().update();
                        player.sendMessage(ChatColor.GREEN + "Shop successfully created!");
                        String shopType = barrelCustomName.equals(this.gambleBarrelName) ? "Gamble Shop" : "Slot Shop";
                        this.getLogger().info("[SlotShop] " + player.getName() + " has created a " + shopType + " selling " + description + " for " + price);
                        if (barrelCustomName.equals(this.gambleBarrelName)) {
                            signBlock.setMetadata("barrelColor", new FixedMetadataValue(this, "red"));
                        } else if (barrelCustomName.equals(this.customBarrelName)) {
                            signBlock.setMetadata("barrelColor", new FixedMetadataValue(this, "yellow"));
                        }

                        this.updateStockLine(barrel, signBlock);
                    } else {
                        player.sendMessage(ChatColor.RED + "Description and price cannot be empty!");
                        signBlock.breakNaturally();
                    }
                }
            }
        }
    }

}
