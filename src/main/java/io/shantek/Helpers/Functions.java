package io.shantek.Helpers;

import io.shantek.SlotShop;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.util.*;

public class Functions {

    private void clearOldRecords(String days) {
        int daysToKeep;
        try {
            daysToKeep = Integer.parseInt(days);
        } catch (NumberFormatException var12) {
            daysToKeep = 30;
        }

        long currentTime = System.currentTimeMillis();
        long daysInMillis = (long)(daysToKeep * 24 * 60 * 60 * 1000);
        Iterator<Map.Entry<UUID, List<SlotShop.Purchase>>> iterator = this.purchaseHistory.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<UUID, List<SlotShop.Purchase>> entry = (Map.Entry)iterator.next();
            List<SlotShop.Purchase> purchases = (List)entry.getValue();
            Iterator<SlotShop.Purchase> purchaseIterator = purchases.iterator();

            while(purchaseIterator.hasNext()) {
                SlotShop.Purchase purchase = (SlotShop.Purchase)purchaseIterator.next();
                if (currentTime - purchase.getTime() > daysInMillis) {
                    purchaseIterator.remove();
                }
            }

            if (purchases.isEmpty()) {
                iterator.remove();
            }
        }

        this.savePurchaseHistory();
    }

    private void clearPurchaseHistory(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " This command can only be executed by a player.");
        } else {
            Player player = (Player)sender;
            UUID playerUUID = player.getUniqueId();
            if (!this.purchaseHistory.containsKey(playerUUID)) {
                sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " No purchase history found.");
            } else {
                this.purchaseHistory.remove(playerUUID);
                this.savePurchaseHistory();
                sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " Sales history cleared.");
            }
        }
    }

    private void loadPurchaseHistory() {
        File dataFolder = this.getDataFolder();
        File purchaseFile = new File(dataFolder, "purchase_history.yml");
        if (!purchaseFile.exists()) {
            this.getLogger().info("[SlotShop] No purchase history found.");
        } else {
            YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(purchaseFile);
            ConfigurationSection purchasesSection = dataConfig.getConfigurationSection("purchases");
            if (purchasesSection == null) {
                this.getLogger().info("[SlotShop] No purchase history found.");
            } else {
                Iterator var5 = purchasesSection.getKeys(false).iterator();

                while(var5.hasNext()) {
                    String playerUUID = (String)var5.next();

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(playerUUID);
                    } catch (IllegalArgumentException var20) {
                        this.getLogger().warning("Invalid UUID string: " + playerUUID);
                        continue;
                    }

                    List<Map<?, ?>> serializedPurchases = dataConfig.getMapList("purchases." + playerUUID);
                    List<SlotShop.Purchase> playerPurchases = new ArrayList();
                    Iterator var10 = serializedPurchases.iterator();

                    while(var10.hasNext()) {
                        Map<?, ?> serializedPurchase = (Map)var10.next();
                        String buyer = (String)serializedPurchase.get("buyer");
                        String seller = (String)serializedPurchase.get("seller");
                        String item = (String)serializedPurchase.get("item");
                        double cost = (Double)serializedPurchase.get("cost");
                        long timestamp = (Long)serializedPurchase.get("timestamp");
                        SlotShop.Purchase purchase = new SlotShop.Purchase(buyer, seller, cost, new ItemStack(Material.valueOf(item)), timestamp);
                        playerPurchases.add(purchase);
                    }

                    this.purchaseHistory.put(uuid, playerPurchases);
                }

                this.getLogger().info("Purchase history loaded successfully.");
            }
        }
    }

    private void purgeGambleTimes() {
        this.purchaseCooldowns.clear();
    }

    private String formatRemainingCooldown(long remainingTime) {
        long seconds = remainingTime % 60L;
        long minutes = remainingTime / 60L % 60L;
        long hours = remainingTime / 3600L % 24L;
        long days = remainingTime / 86400L;
        StringBuilder formattedTime = new StringBuilder();
        if (days > 0L) {
            formattedTime.append(days).append("d ");
        }

        if (hours > 0L) {
            formattedTime.append(hours).append("h ");
        }

        if (minutes > 0L) {
            formattedTime.append(minutes).append("m ");
        }

        formattedTime.append(seconds).append("s ");
        return formattedTime.toString();
    }

    private Block getAttachedSignBlock(Block barrelBlock) {
        BlockFace[] var2 = BlockFace.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            BlockFace face = var2[var4];
            Block attachedBlock = barrelBlock.getRelative(face);
            if (attachedBlock.getState() instanceof Sign) {
                return attachedBlock;
            }
        }

        return null;
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        } else {
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            } else {
                this.econ = (Economy)rsp.getProvider();
                return this.econ != null;
            }
        }
    }

    private Block getAttachedBlock(Block block) {
        Block attachedBlock = null;
        BlockFace[] signFaces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        BlockFace[] var4 = signFaces;
        int var5 = signFaces.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            BlockFace face = var4[var6];
            Block relativeBlock = block.getRelative(face);
            if (relativeBlock.getState() instanceof Sign && relativeBlock.getRelative(face.getOppositeFace()).equals(block)) {
                attachedBlock = relativeBlock;
                System.out.println("Attached Block found: " + relativeBlock.getType() + " at " + relativeBlock.getLocation());
                break;
            }
        }

        if (attachedBlock == null) {
            System.out.println("No attached block found.");
        }

        return attachedBlock;
    }

    private void setSlotshopCooldown(Player player) {
        this.purchaseCooldownSlotShop.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean canPurchaseSlotShop(Player player) {
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = (Long)this.purchaseCooldownSlotShop.get(player.getUniqueId());
        return lastPurchaseTime == null || currentTime - lastPurchaseTime >= 250L;
    }

    private boolean isAxeEquipped(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        Material itemType = item.getType();
        return itemType.toString().toLowerCase().contains("_axe");
    }

    private boolean isStickEquipped(Player player) {
        Material heldItem = player.getInventory().getItemInMainHand().getType();
        return heldItem == Material.STICK;
    }

    private int getQuantityInInventory(Inventory inventory, ItemStack itemStack) {
        int quantity = 0;
        ItemStack[] contents = inventory.getContents();
        ItemStack[] var5 = contents;
        int var6 = contents.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            ItemStack item = var5[var7];
            if (item != null && item.isSimilar(itemStack)) {
                quantity += item.getAmount();
            }
        }

        return quantity;
    }

    private void updateStockLine(Barrel barrel, Block signBlock) {
        int stockCount = this.countNonNullSlots(barrel.getInventory().getContents());
        Sign sign = (Sign)signBlock.getState();
        sign.setLine(3, ChatColor.DARK_GRAY + "Stock: " + stockCount);
        sign.update();
    }

    private int countNonNullSlots(ItemStack[] items) {
        int count = 0;
        ItemStack[] var3 = items;
        int var4 = items.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            ItemStack item = var3[var5];
            if (item != null && item.getType() != Material.AIR) {
                ++count;
            }
        }

        return count;
    }

    private SlotShop.ChosenItem getRandomItem(Inventory inventory) {
        List<SlotShop.ChosenItem> slots = new ArrayList();
        ItemStack[] contents = inventory.getContents();

        int chosenIndex;
        for(chosenIndex = 0; chosenIndex < contents.length; ++chosenIndex) {
            ItemStack item = contents[chosenIndex];
            if (item != null && item.getType() != Material.AIR) {
                slots.add(new SlotShop.ChosenItem(chosenIndex, item));
            }
        }

        if (slots.isEmpty()) {
            return null;
        } else {
            chosenIndex = (new Random()).nextInt(slots.size());
            return (SlotShop.ChosenItem)slots.get(chosenIndex);
        }
    }

    private boolean chargePlayer(Player buyer, String seller, String coOwner, double cost) {
        EconomyResponse withdrawResponse = this.econ.withdrawPlayer(buyer, cost);
        if (!withdrawResponse.transactionSuccess()) {
            buyer.sendMessage(ChatColor.RED + "Transaction failed: " + withdrawResponse.errorMessage);
            return false;
        } else {
            if (coOwner != null && !coOwner.isEmpty()) {
                double paymentAmount = cost * 0.5;
                EconomyResponse sellerPayment = this.econ.depositPlayer(Bukkit.getOfflinePlayer(seller), paymentAmount);
                if (!sellerPayment.transactionSuccess()) {
                    buyer.sendMessage(ChatColor.RED + "Transaction failed: " + sellerPayment.errorMessage);
                    this.econ.withdrawPlayer(buyer, cost);
                    return false;
                }

                EconomyResponse coOwnerPayment = this.econ.depositPlayer(Bukkit.getOfflinePlayer(coOwner), paymentAmount);
                if (!coOwnerPayment.transactionSuccess()) {
                    buyer.sendMessage(ChatColor.RED + "Transaction failed: " + coOwnerPayment.errorMessage);
                    this.econ.withdrawPlayer(buyer, cost);
                    this.econ.withdrawPlayer(Bukkit.getOfflinePlayer(seller), paymentAmount);
                    return false;
                }
            } else {
                EconomyResponse payment = this.econ.depositPlayer(Bukkit.getOfflinePlayer(seller), cost);
                if (!payment.transactionSuccess()) {
                    buyer.sendMessage(ChatColor.RED + "Transaction failed: " + payment.errorMessage);
                    this.econ.withdrawPlayer(buyer, cost);
                    return false;
                }
            }

            return true;
        }
    }

    private boolean canAfford(Player player, double cost) {
        return this.econ.has(player, cost);
    }

    private boolean hasInventorySpace(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    private BlockFace getAttachedFace(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            Directional directional = (Directional)blockData;
            return directional.getFacing().getOppositeFace();
        } else {
            return null;
        }
    }

    private boolean canPurchase(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L;
        long lastPurchaseTime = (Long)this.purchaseCooldowns.getOrDefault(player.getUniqueId(), 0L);
        return currentTime >= lastPurchaseTime + COOLDOWN_TIME_SECONDS;
    }

    private long getRemainingCooldown(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L;
        long lastPurchaseTime = (Long)this.purchaseCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remainingTime = lastPurchaseTime + COOLDOWN_TIME_SECONDS - currentTime;
        return Math.max(0L, remainingTime);
    }

    private long setPurchaseCooldown(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L;
        long cooldownTime = currentTime + COOLDOWN_TIME_SECONDS;
        this.purchaseCooldowns.put(player.getUniqueId(), cooldownTime);
        return cooldownTime;
    }

    private void createSlotShop(CommandSender sender, boolean isGamble) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
        } else {
            Player player = (Player)sender;
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock != null && targetBlock.getState() instanceof Barrel) {
                Barrel barrel = (Barrel)targetBlock.getState();
                if (this.hasAttachedSign(targetBlock)) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Please remove the signs off the barrel first.");
                } else {
                    String customName = isGamble ? this.gambleBarrelName : this.customBarrelName;
                    barrel.setCustomName(customName);
                    barrel.update();
                    player.sendMessage(ChatColor.GREEN + "Slot Shop " + (isGamble ? "Gamble " : "") + "created successfully.");
                }
            } else {
                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Barrel not found.");
            }
        }
    }

    private boolean hasAttachedSign(Block barrelBlock) {
        BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        BlockFace[] var3 = faces;
        int var4 = faces.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            BlockFace face = var3[var5];
            Block attachedBlock = barrelBlock.getRelative(face);
            if (attachedBlock.getState() instanceof Sign) {
                return true;
            }
        }

        return false;
    }

}
