package io.shantek.listeners;

import io.shantek.SlotShop;
import io.shantek.helpers.ChosenItem;
import io.shantek.helpers.Functions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class PlayerInteractListener implements Listener {
    private final SlotShop plugin;

    public PlayerInteractListener(SlotShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            if (clickedBlock.getState() instanceof Barrel) {
                handleBarrelClick(player, clickedBlock, event);
            } else if (clickedBlock.getState() instanceof Sign) {
                handleSignClick(player, clickedBlock, event);
            }
        }
    }

    private void handleBarrelClick(Player player, Block clickedBlock, PlayerInteractEvent event) {
        Barrel barrel = (Barrel) clickedBlock.getState();
        String barrelCustomName = barrel.getCustomName();
        if (barrelCustomName != null && (barrelCustomName.equals(Functions.customBarrelName) || barrelCustomName.equals(Functions.gambleBarrelName))) {
            Block attachedBlock = Functions.getAttachedBlock(clickedBlock);
            if (attachedBlock != null && attachedBlock.getState() instanceof Sign) {
                Sign sign = (Sign) attachedBlock.getState();
                String ownerName = sign.getLine(0);
                Location signLocation = attachedBlock.getLocation();

                boolean isOwnerOrCoOwner = isOwnerOrCoOwner(player, ownerName, signLocation);

                if (isOwnerOrCoOwner) {
                    if (Functions.isStickEquipped(player)) {
                        // Show shop info if holding a stick
                        sendShopInfo(player, ownerName, barrelCustomName, signLocation);
                        event.setCancelled(true);
                    } else {
                        // Allow opening the barrel without prompts if not holding a stick
                        return;
                    }
                } else {
                    // Provide shop info for non-owners
                    sendShopInfo(player, ownerName, barrelCustomName, signLocation);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleSignClick(Player player, Block clickedBlock, PlayerInteractEvent event) {
        BlockFace face = Functions.getAttachedFace(clickedBlock);
        if (face == null) {
            return;
        }
        Block attachedBlock = clickedBlock.getRelative(face);
        if (attachedBlock.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) attachedBlock.getState();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(Functions.customBarrelName) || barrelCustomName.equals(Functions.gambleBarrelName))) {
                Sign sign = (Sign) clickedBlock.getState();
                String ownerName = sign.getLine(0);
                Location signLocation = clickedBlock.getLocation();

                boolean isOwnerOrCoOwner = isOwnerOrCoOwner(player, ownerName, signLocation);

                if (isOwnerOrCoOwner) {
                    if (isOwner(player, ownerName)) {
                        // Allow owners to edit or destroy the sign
                        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                            // Allow sign destruction
                            return;
                        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            player.sendMessage(ChatColor.RED + "Break the sign to recreate the shop");
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Only the shop owner can edit the sign.");
                        event.setCancelled(true);
                    }
                } else if (player.isOp()) {
                    // Allow ops to destroy the sign
                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        return;
                    } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        player.sendMessage(ChatColor.RED + "Break the sign to recreate the shop");
                        event.setCancelled(true);
                    }
                } else {
                    // Prevent non-owners from editing the sign, proceed with the purchase process
                    event.setCancelled(true);
                    handlePurchaseProcess(player, barrel, sign, ownerName, signLocation, event);
                }
            }
        }
    }

    private boolean isOwnerOrCoOwner(Player player, String ownerName, Location signLocation) {
        String playerName = player.getName();
        boolean hasCoowner = false;
        String shopCoowner = null;
        if (Functions.getShopDataMap().containsKey(signLocation)) {
            Functions.ShopData shopData = Functions.getShopDataMap().get(signLocation);
            if (shopData != null) {
                shopCoowner = shopData.getCoOwner();
                hasCoowner = true;
            }
        }
        return playerName.equals(ownerName) || (hasCoowner && playerName.equals(shopCoowner));
    }

    private boolean isOwner(Player player, String ownerName) {
        return player.getName().equals(ownerName);
    }

    private void handlePurchaseProcess(Player player, Barrel barrel, Sign sign, String ownerName, Location signLocation, PlayerInteractEvent event) {
        String itemDescription = sign.getLine(1);
        String costLine = sign.getLine(2);

        if (itemDescription.isEmpty() || costLine.isEmpty() || !costLine.startsWith(ChatColor.GREEN + "Cost: ")) {
            return;
        }

        double cost;
        try {
            cost = Double.parseDouble(costLine.replace(ChatColor.GREEN + "Cost: ", ""));
        } catch (NumberFormatException e) {
            return;
        }

        boolean hasCoowner = false;
        String shopCoowner = null;
        if (Functions.getShopDataMap().containsKey(signLocation)) {
            Functions.ShopData shopData = Functions.getShopDataMap().get(signLocation);
            if (shopData != null) {
                shopCoowner = shopData.getCoOwner();
                hasCoowner = true;
            }
        }

        if (!Functions.canAfford(player, cost)) {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have enough money!");
        } else if (!Functions.hasInventorySpace(player)) {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have enough inventory space!");
        } else if (!Functions.canPurchaseSlotShop(player)) {
            event.setCancelled(true);
        } else {
            Functions.setSlotshopCooldown(player);
            if (barrel.getCustomName().equals(Functions.gambleBarrelName) && !Functions.canPurchase(player)) {
                long remainingTime = Functions.getRemainingCooldown(player);
                String formattedTime = Functions.formatRemainingCooldown(remainingTime);
                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Wait " + formattedTime + " to use a Gamble Shop again.");
                event.setCancelled(true);
            } else {
                processPurchase(player, barrel, sign, ownerName, shopCoowner, cost);
            }
        }
    }

    private void sendShopInfo(Player player, String ownerName, String barrelCustomName, Location signLocation) {
        boolean hasCoowner = false;
        String shopCoowner = null;
        if (Functions.getShopDataMap().containsKey(signLocation)) {
            Functions.ShopData shopData = Functions.getShopDataMap().get(signLocation);
            if (shopData != null) {
                shopCoowner = shopData.getCoOwner();
                hasCoowner = true;
            }
        }

        player.sendMessage(ChatColor.GREEN + "[SlotShop] Info about this Slot Shop:");
        player.sendMessage(ChatColor.YELLOW + "Shop Owner: " + ChatColor.WHITE + ownerName);
        player.sendMessage(ChatColor.YELLOW + "Shop Type: " + ChatColor.WHITE + barrelCustomName);
        player.sendMessage(ChatColor.YELLOW + "Shop Co-owner: " + ChatColor.WHITE + (hasCoowner && shopCoowner != null ? shopCoowner : "No co-owner set"));
        // Additional shop info can be sent here
    }

    private void revertSignToEditableState(Sign sign) {
        // Clear line 1 and line 4
        sign.setLine(0, "");
        sign.setLine(3, "");
        // Remove "Cost: " from line 3
        String costLine = sign.getLine(2).replace(ChatColor.GREEN + "Cost: ", "");
        sign.setLine(2, costLine);
        sign.update();
    }

    private void processPurchase(Player player, Barrel barrel, Sign sign, String ownerName, String shopCoowner, double cost) {
        Inventory inventory = barrel.getInventory();
        ChosenItem chosenItem = Functions.getRandomItem(inventory);
        if (chosenItem == null) {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Shop is out of stock!");
            notifyOwnerOutOfStock(player, ownerName, sign.getLine(1));
            return;
        }

        ItemStack purchasedItem = chosenItem.getItem().clone();
        int availableQuantity = Functions.getQuantityInInventory(inventory, chosenItem.getItem());
        if (availableQuantity < purchasedItem.getAmount()) {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Shop is out of stock!");
            notifyOwnerOutOfStock(player, ownerName, sign.getLine(1));
            return;
        }

        if (shopCoowner != null && !shopCoowner.isEmpty()) {
            Functions.setPurchaseCooldown(player);
        }

        double ownerPayment = cost;
        double coOwnerPayment = 0.0;
        if (shopCoowner != null && !shopCoowner.isEmpty()) {
            ownerPayment /= 2.0;
            coOwnerPayment = ownerPayment;
        }

        Functions.chargePlayer(player, ownerName, shopCoowner, cost);
        player.getInventory().addItem(purchasedItem);
        inventory.setItem(chosenItem.getSlot(), null);

        String formattedName = Arrays.stream(purchasedItem.getType().toString().split("_")).map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()).reduce("", (a, b) -> a + " " + b).trim();
        player.sendMessage(ChatColor.GREEN + "[SlotShop] You purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + cost + ChatColor.GREEN + " from " + ChatColor.YELLOW + ownerName + ".");

        Functions.addPurchaseToHistory(player.getName(), ownerName, shopCoowner, shopCoowner != null && !shopCoowner.isEmpty() ? ownerPayment : cost, purchasedItem, plugin);
        notifyOwnerAndCoOwner(player, ownerName, purchasedItem, formattedName, ownerPayment, coOwnerPayment, shopCoowner);

        Functions.updateStockLine(barrel, sign.getBlock());
    }

    private void notifyOwnerOutOfStock(Player buyer, String ownerName, String itemDescription) {
        Player owner = Bukkit.getPlayer(ownerName);
        if (owner != null) {
            owner.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + buyer.getName() + " is trying to buy from the " + itemDescription + " shop, but it's out of stock.");
        }
    }

    private void notifyOwnerAndCoOwner(Player player, String ownerName, ItemStack purchasedItem, String formattedName, double ownerPayment, double coOwnerPayment, String shopCoowner) {
        Player owner = Bukkit.getPlayer(ownerName);
        if (owner != null) {
            owner.sendMessage(ChatColor.GREEN + "[SlotShop] " + player.getName() + " purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + ownerPayment + ChatColor.GREEN + ".");
            if (shopCoowner != null && !shopCoowner.isEmpty()) {
                Player coOwner = Bukkit.getPlayer(shopCoowner);
                if (coOwner != null) {
                    coOwner.sendMessage(ChatColor.GREEN + "[SlotShop] " + player.getName() + " purchased " + ChatColor.YELLOW + purchasedItem.getAmount() + "x " + formattedName + ChatColor.GREEN + " for " + ChatColor.YELLOW + coOwnerPayment + ChatColor.GREEN + ".");
                }
            }
        }
    }
}
