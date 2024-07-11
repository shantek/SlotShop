package io.shantek.listeners;

import io.shantek.SlotShop;
import io.shantek.helpers.ChosenItem;
import io.shantek.helpers.Functions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
                    // Allow owners and co-owners to open the barrel without purchasing
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] You are the owner/co-owner of this shop. You can manage the shop items.");
                    return;
                } else {
                    // Proceed with the purchase process for other players
                    handlePurchaseProcess(player, barrel, sign, ownerName, signLocation, event);
                }
            }
        }
    }

    private void handleSignClick(Player player, Block clickedBlock, PlayerInteractEvent event) {
        Block attachedBlock = clickedBlock.getRelative(Functions.getAttachedFace(clickedBlock));
        if (attachedBlock.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) attachedBlock.getState();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(Functions.customBarrelName) || barrelCustomName.equals(Functions.gambleBarrelName))) {
                Sign sign = (Sign) clickedBlock.getState();
                String ownerName = sign.getLine(0);
                Location signLocation = clickedBlock.getLocation();

                boolean isOwnerOrCoOwner = isOwnerOrCoOwner(player, ownerName, signLocation);

                if (isOwnerOrCoOwner) {
                    // Allow owners and co-owners to open the barrel without purchasing
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] You are the owner/co-owner of this shop. You can manage the shop items.");
                    return;
                } else {
                    // Proceed with the purchase process for other players
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

        if (playerHasPermission(player, ownerName, hasCoowner, shopCoowner)) {
            event.setCancelled(true);
            sendShopInfo(player, ownerName, barrel.getCustomName(), shopCoowner, itemDescription, cost);
        } else if (!Functions.canAfford(player, cost)) {
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

    private boolean playerHasPermission(Player player, String ownerName, boolean hasCoowner, String shopCoowner) {
        boolean printInfo = false;
        if (ownerName.equals(player.getName())) {
            if (!Functions.isStickEquipped(player)) {
                return false;
            }
            printInfo = true;
        } else {
            if (player.isOp() && Functions.isStickEquipped(player)) {
                printInfo = true;
                return false;
            }
            if (hasCoowner && player.getName().equals(shopCoowner)) {
                if (!Functions.isStickEquipped(player)) {
                    return false;
                }
                printInfo = true;
            } else {
                printInfo = true;
            }
        }
        return printInfo;
    }

    private void sendShopInfo(Player player, String ownerName, String barrelCustomName, String shopCoowner, String itemDescription, double cost) {
        player.sendMessage(ChatColor.GREEN + "[SlotShop] Info about this Slot Shop:");
        player.sendMessage(ChatColor.YELLOW + "Shop Owner: " + ChatColor.WHITE + ownerName);
        player.sendMessage(ChatColor.YELLOW + "Shop Type: " + ChatColor.WHITE + barrelCustomName);
        player.sendMessage(ChatColor.YELLOW + "Shop Co-owner: " + ChatColor.WHITE + (shopCoowner != null && !shopCoowner.isEmpty() ? shopCoowner : "No co-owner set"));
        player.sendMessage(ChatColor.YELLOW + "Item Description: " + ChatColor.WHITE + itemDescription);
        player.sendMessage(ChatColor.YELLOW + "Slot Price: " + ChatColor.WHITE + cost);
    }

    private void processPurchase(Player player, Barrel barrel, Sign sign, String ownerName, String shopCoowner, double cost) {
        Inventory inventory = barrel.getInventory();
        ChosenItem chosenItem = Functions.getRandomItem(inventory);
        if (chosenItem == null) {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Shop is out of stock!");
            return;
        }

        ItemStack purchasedItem = chosenItem.getItem().clone();
        int availableQuantity = Functions.getQuantityInInventory(inventory, chosenItem.getItem());
        if (availableQuantity < purchasedItem.getAmount()) {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Shop is out of stock!");
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
