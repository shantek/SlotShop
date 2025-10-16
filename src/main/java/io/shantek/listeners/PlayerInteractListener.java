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
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
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
                } else if (player.hasPermission("shantek.slotshop.mod") && player.getInventory().getItemInMainHand().getType() == Material.PAPER) {
                    // Allow them to open the barrel
                    return;
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

                if (barrelCustomName.equals(Functions.gambleBarrelName)) {
                    if (!Functions.canPurchaseGambleShop(player)) {
                        long remainingTime = Functions.getRemainingGambleCooldown(player);
                        String formattedTime = Functions.formatRemainingCooldown(remainingTime);
                        player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You can't purchase for " + formattedTime);
                        event.setCancelled(true);
                        return;
                    }
                }

                if (isOwnerOrCoOwner) {
                    if (isOwner(player, ownerName)) {
                        // Allow owners to edit or destroy the sign
                        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                            // Allow sign destruction
                            return;
                        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            ItemStack itemInHand = player.getInventory().getItemInMainHand();
                            Material itemType = itemInHand.getType();
                            String itemName = itemType.toString();

                            if (itemName.equals("GLOW_INK_SAC") || itemName.endsWith("_DYE")) {
                                // Allow using dye or glow ink on the sign
                                return;
                            } else {
                                player.sendMessage(ChatColor.RED + "Break the sign to recreate the shop");
                                event.setCancelled(true);
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Only the shop owner can edit the sign.");
                        event.setCancelled(true);
                    }
                } else if (player.isOp()  && player.isSneaking()) {
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
            if (barrel.getCustomName().equals(Functions.gambleBarrelName)) {
                if (!Functions.canPurchaseGambleShop(player)) {
                    long remainingTime = Functions.getRemainingGambleCooldown(player);
                    String formattedTime = Functions.formatRemainingCooldown(remainingTime);
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You can't purchase for " + formattedTime);
                    event.setCancelled(true);
                    return;
                } else {
                    Functions.updateGamblePurchaseTime(player); // Set the proper cooldown
                }
            }
            processPurchase(player, barrel, sign, ownerName, shopCoowner, cost);
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

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "shoptransaction " + player.getName() + " purchase " + cost);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordsrv broadcast #1426867724130324611 **[SlotShop]** " + player.getName() + " purchased " + formattedName + " from " + ownerName + " for $" + formatPrice(cost));

    }

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##"); // removes trailing .0 but keeps .50

    private String formatPrice(double value) {
        // e.g., 45.0 → "45", 1.5 → "1.50"
        String formatted = MONEY_FORMAT.format(value);
        if (!formatted.contains(".")) return formatted; // whole number
        if (formatted.endsWith(".0")) return formatted.substring(0, formatted.length() - 2);
        if (formatted.indexOf('.') == formatted.length() - 2) return formatted + "0";
        return formatted;
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
