package io.shantek.Listeners;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerInteract {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        String shopCoowner = null;
        if (clickedBlock != null) {
            Sign sign;
            String ownerName;
            String itemDescription;
            String costLine;
            if (clickedBlock.getState() instanceof Barrel) {
                Barrel barrel = (Barrel)clickedBlock.getState();
                String barrelCustomName = barrel.getCustomName();
                if (barrelCustomName != null && (barrelCustomName.equals(this.customBarrelName) || barrelCustomName.equals(this.gambleBarrelName))) {
                    Block attachedBlock = this.getAttachedBlock(clickedBlock);
                    if (attachedBlock != null && attachedBlock.getState() instanceof Sign) {
                        sign = (Sign)attachedBlock.getState();
                        ownerName = sign.getLine(0);
                        itemDescription = sign.getLine(1);
                        costLine = sign.getLine(2);
                        Location signLocation = attachedBlock.getLocation();
                        System.out.println("Clicked Barrel Location: " + clickedBlock.getLocation());
                        System.out.println("Attached Sign Location: " + signLocation);
                        System.out.println("Owner: " + ownerName);
                        System.out.println("Description: " + itemDescription);
                        System.out.println("Cost: " + costLine);
                        System.out.println("Sign Location: " + signLocation);
                        if (ownerName == null || ownerName.isEmpty() || itemDescription == null || itemDescription.isEmpty() || costLine == null || costLine.isEmpty()) {
                            return;
                        }

                        if (sign == null || signLocation == null) {
                            return;
                        }

                        if (!costLine.startsWith(ChatColor.GREEN + "Cost: ")) {
                            return;
                        }

                        double cost = 0.0;

                        try {
                            cost = Double.parseDouble(costLine.replace(ChatColor.GREEN + "Cost: ", ""));
                        } catch (NumberFormatException var34) {
                        }

                        boolean hasCoowner = false;
                        if (this.shopDataMap.containsKey(signLocation)) {
                            SlotShop.ShopData shopData = (SlotShop.ShopData)this.shopDataMap.get(signLocation);
                            if (shopData != null) {
                                shopCoowner = shopData.getCoOwner();
                                hasCoowner = true;
                            }
                        }

                        boolean printInfo = false;
                        if (ownerName.equals(player.getName())) {
                            if (!this.isStickEquipped(player)) {
                                return;
                            }

                            printInfo = true;
                        } else {
                            if (player.isOp() && this.isStickEquipped(player)) {
                                printInfo = true;
                                return;
                            }

                            if (hasCoowner && player.getName().equals(shopCoowner)) {
                                if (!this.isStickEquipped(player)) {
                                    return;
                                }

                                printInfo = true;
                            } else {
                                printInfo = true;
                            }
                        }

                        if (printInfo) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.GREEN + "[SlotShop] Info about this Slot Shop:");
                            player.sendMessage(ChatColor.YELLOW + "Shop Owner: " + ChatColor.WHITE + ownerName);
                            player.sendMessage(ChatColor.YELLOW + "Shop Type: " + ChatColor.WHITE + barrelCustomName);
                            if (shopCoowner != null && !shopCoowner.isEmpty()) {
                                player.sendMessage(ChatColor.YELLOW + "Shop Co-owner: " + ChatColor.WHITE + shopCoowner);
                            } else {
                                player.sendMessage(ChatColor.YELLOW + "Shop Co-owner: " + ChatColor.WHITE + "No co-owner set");
                            }

                            player.sendMessage(ChatColor.YELLOW + "Item Description: " + ChatColor.WHITE + itemDescription);
                            player.sendMessage(ChatColor.YELLOW + "Slot Price: " + ChatColor.WHITE + cost);
                            return;
                        }

                        return;
                    }

                    return;
                }

                return;
            } else if (clickedBlock.getState() instanceof Sign) {
                Block attachedBlock = clickedBlock.getRelative(this.getAttachedFace(clickedBlock));
                if (!(attachedBlock.getState() instanceof Barrel)) {
                    return;
                }

                Barrel barrel = (Barrel)attachedBlock.getState();
                String barrelCustomName = barrel.getCustomName();
                if (barrelCustomName == null || !barrelCustomName.equals(this.customBarrelName) && !barrelCustomName.equals(this.gambleBarrelName)) {
                    return;
                }

                sign = (Sign)clickedBlock.getState();
                ownerName = sign.getLine(0);
                itemDescription = sign.getLine(1);
                costLine = sign.getLine(2);
                if (!costLine.startsWith(ChatColor.GREEN + "Cost: ")) {
                    return;
                }

                boolean hasCoowner = false;
                Location signLocation = clickedBlock.getLocation();
                if (this.shopDataMap.containsKey(signLocation)) {
                    SlotShop.ShopData shopData = (SlotShop.ShopData)this.shopDataMap.get(signLocation);
                    if (shopData != null) {
                        shopCoowner = shopData.getCoOwner();
                        hasCoowner = true;
                    }
                }

                if (hasCoowner && shopCoowner.equals(player.getName())) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You can't purchase from a show you co-own!");
                    event.setCancelled(true);
                    return;
                }

                double cost = Double.parseDouble(costLine.replace(ChatColor.GREEN + "Cost: ", ""));
                ownerName.equals(player.getName());
                if (ownerName.equals(player.getName())) {
                    return;
                }

                if (player.isOp() && this.isStickEquipped(player)) {
                    return;
                }

                if (player.isOp() && this.isAxeEquipped(player)) {
                    return;
                }

                event.setCancelled(true);
                BlockState signState = clickedBlock.getState();
                signState.update(true, false);
                if (!this.canAfford(player, cost)) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have enough money!");
                    return;
                }

                if (!this.hasInventorySpace(player)) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have enough inventory space!");
                    return;
                }

                if (!this.canPurchaseSlotShop(player)) {
                    event.setCancelled(true);
                    return;
                }

                this.setSlotshopCooldown(player);
                if (barrelCustomName.equals(this.gambleBarrelName) && !this.canPurchase(player)) {
                    long remainingTime = this.getRemainingCooldown(player);
                    String formattedTime = this.formatRemainingCooldown(remainingTime);
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Wait " + formattedTime + " to use a Gamble Shop again.");
                    event.setCancelled(true);
                    return;
                }

                Inventory inventory = barrel.getInventory();
                SlotShop.ChosenItem chosenItem = this.getRandomItem(inventory);
                if (chosenItem == null) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Shop is out of stock!");
                    event.setCancelled(true);
                    return;
                }

                ItemStack purchasedItem = chosenItem.getItem().clone();
                int availableQuantity = this.getQuantityInInventory(inventory, chosenItem.getItem());
                if (availableQuantity < purchasedItem.getAmount()) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Shop is out of stock!");
                    event.setCancelled(true);
                    return;
                }

                if (barrelCustomName.equals(this.gambleBarrelName)) {
                    this.setPurchaseCooldown(player);
                }

                double ownerPayment = cost;
                double coOwnerPayment = 0.0;
                if (shopCoowner != null && !shopCoowner.isEmpty()) {
                    ownerPayment /= 2.0;
                    coOwnerPayment = ownerPayment;
                }

                this.chargePlayer(player, ownerName, shopCoowner, cost);
                player.getInventory().addItem(new ItemStack[]{purchasedItem});
                inventory.setItem(chosenItem.getSlot(), (ItemStack)null);
                String itemName = purchasedItem.getType().toString();
                String[] words = itemName.split("_");
                StringBuilder formattedItemName = new StringBuilder();
                String[] var28 = words;
                int var29 = words.length;

                for(int var30 = 0; var30 < var29; ++var30) {
                    String word = var28[var30];
                    if (word.length() > 0) {
                        String firstLetter = word.substring(0, 1).toUpperCase();
                        String restOfWord = word.substring(1).toLowerCase();
                        formattedItemName.append(firstLetter).append(restOfWord).append(" ");
                    }
                }

                String formattedName = formattedItemName.toString().trim();
                player.sendMessage(ChatColor.GREEN + "[SlotShop] You purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + cost + ChatColor.GREEN + " from " + ChatColor.YELLOW + ownerName + ".");
                if (ownerName != null && !ownerName.isEmpty()) {
                    this.addPurchaseToHistory(player.getName(), ownerName, shopCoowner, shopCoowner != null && !shopCoowner.isEmpty() ? ownerPayment : cost, purchasedItem);
                }

                Player owner = Bukkit.getPlayer(ownerName);
                if (owner != null) {
                    owner.sendMessage(ChatColor.GREEN + "[SlotShop] " + player.getName() + " purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + (shopCoowner != null && !shopCoowner.isEmpty() ? ownerPayment : cost) + ChatColor.GREEN + ".");
                    if (shopCoowner != null && !shopCoowner.isEmpty()) {
                        Player coOwner = Bukkit.getPlayer(shopCoowner);
                        if (coOwner != null) {
                            coOwner.sendMessage(ChatColor.GREEN + "[SlotShop] " + player.getName() + " purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + coOwnerPayment + ChatColor.GREEN + ".");
                        }
                    }

                    this.getLogger().info(ChatColor.GREEN + "[SlotShop] " + player.getName() + " purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + (shopCoowner != null && !shopCoowner.isEmpty() ? ownerPayment : cost) + ChatColor.GREEN + " from " + ChatColor.YELLOW + ownerName + ".");
                }

                this.updateStockLine(barrel, clickedBlock);
            }

        }
    }


}
