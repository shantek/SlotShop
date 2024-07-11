package io.shantek.listeners;

import io.shantek.SlotShop;
import io.shantek.helpers.Functions;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

public class SignChangeListener implements Listener {
    private final SlotShop plugin;

    public SignChangeListener(SlotShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block signBlock = event.getBlock();
        Block attachedBlock = signBlock.getRelative(Functions.getAttachedFace(signBlock));
        if (attachedBlock.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) attachedBlock.getState();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(Functions.customBarrelName) || barrelCustomName.equals(Functions.gambleBarrelName))) {
                if (barrelCustomName.equals(Functions.gambleBarrelName) && !player.hasPermission("slotshop.create.gamblebarrel")) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to set up a Gamble Shop!");
                    signBlock.breakNaturally();
                } else if (barrelCustomName.equals(Functions.customBarrelName) && !player.hasPermission("slotshop.create")) {
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
                        } catch (NumberFormatException e) {
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
                            OfflinePlayer owner = player.getServer().getOfflinePlayer(oldOwner);
                            if (!owner.hasPlayedBefore()) {
                                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Player name is invalid. Please fix.");
                                return;
                            }

                            ownerName = oldOwner;
                        }

                        event.setLine(0, ownerName);
                        event.setLine(2, ChatColor.GREEN + "Cost: " + price);
                        event.setLine(3, ChatColor.BLACK + "Stock: " + Functions.countNonNullSlots(inventoryContents));
                        signBlock.getState().update();
                        player.sendMessage(ChatColor.GREEN + "Shop successfully created!");
                        String shopType = barrelCustomName.equals(Functions.gambleBarrelName) ? "Gamble Shop" : "Slot Shop";
                        plugin.getLogger().info("[SlotShop] " + player.getName() + " has created a " + shopType + " selling " + description + " for " + price);
                        signBlock.setMetadata("barrelColor", new FixedMetadataValue(plugin, barrelCustomName.equals(Functions.gambleBarrelName) ? "red" : "yellow"));
                        Functions.updateStockLine(barrel, signBlock);
                    } else {
                        player.sendMessage(ChatColor.RED + "Description and price cannot be empty!");
                        signBlock.breakNaturally();
                    }
                }
            }
        }
    }
}
