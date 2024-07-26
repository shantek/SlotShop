package io.shantek.helpers;

import io.shantek.SlotShop;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class Functions {
    public static final String customBarrelName = "SlotShop";
    public static final String gambleBarrelName = "GambleShop";
    private static final Map<UUID, Long> purchaseCooldowns = ConfigData.getPlayerLastPurchaseTimes(); // Ensure it uses the persistent map
    private static final Map<UUID, Long> purchaseCooldownSlotShop = new HashMap<>();
    private static final Map<Location, ShopData> shopDataMap = new HashMap<>();

    public static boolean canPurchaseGambleShop(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L; // Use seconds for consistency
        long lastPurchaseTime = purchaseCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldownDuration = ConfigData.getCooldownDuration();
        return currentTime >= lastPurchaseTime + cooldownDuration;
    }

    public static long getRemainingGambleCooldown(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L; // Use seconds for consistency
        long lastPurchaseTime = purchaseCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldownDuration = ConfigData.getCooldownDuration();
        return (lastPurchaseTime + cooldownDuration) - currentTime;
    }

    public static void updateGamblePurchaseTime(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L; // Use seconds for consistency
        purchaseCooldowns.put(player.getUniqueId(), currentTime);
        ConfigData.savePlayerLastPurchaseTimes(SlotShop.getInstance());
    }

    public static String formatRemainingCooldown(long remainingTimeSeconds) {
        long days = remainingTimeSeconds / (24 * 3600);
        remainingTimeSeconds %= 24 * 3600;
        long hours = remainingTimeSeconds / 3600;
        remainingTimeSeconds %= 3600;
        long minutes = remainingTimeSeconds / 60;
        long seconds = remainingTimeSeconds % 60;

        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    public static void purgeGambleTimes() {
        purchaseCooldowns.clear();
        ConfigData.savePlayerLastPurchaseTimes(SlotShop.getInstance());
    }

    public static BlockFace getAttachedFace(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            return ((Directional) blockData).getFacing().getOppositeFace();
        }
        return null;
    }

    public static boolean hasAttachedSign(Block barrelBlock) {
        for (BlockFace face : BlockFace.values()) {
            Block attachedBlock = barrelBlock.getRelative(face);
            if (attachedBlock.getState() instanceof Sign) {
                return true;
            }
        }
        return false;
    }

    public static Block getAttachedBlock(Block block) {
        for (BlockFace face : BlockFace.values()) {
            Block attachedBlock = block.getRelative(face);
            if (attachedBlock.getState() instanceof Sign && getAttachedFace(attachedBlock) == face.getOppositeFace()) {
                return attachedBlock;
            }
        }
        return null;
    }

    public static Block getAttachedSignBlock(Block barrelBlock) {
        for (BlockFace face : BlockFace.values()) {
            Block attachedBlock = barrelBlock.getRelative(face);
            if (attachedBlock.getState() instanceof Sign) {
                return attachedBlock;
            }
        }
        return null;
    }

    public static void updateStockLine(Barrel barrel, Block signBlock) {
        int stockCount = countNonNullSlots(barrel.getInventory().getContents());
        Sign sign = (Sign) signBlock.getState();
        sign.setLine(3, ChatColor.WHITE + "Stock: " + stockCount);
        sign.update();
    }

    public static int countNonNullSlots(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }





    public static boolean canPurchase(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L;
        long lastPurchaseTime = purchaseCooldowns.getOrDefault(player.getUniqueId(), 0L);
        return currentTime >= lastPurchaseTime + SlotShop.getCooldownTimeSeconds();
    }

    public static long getRemainingCooldown(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L;
        long lastPurchaseTime = purchaseCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remainingTime = lastPurchaseTime + SlotShop.getCooldownTimeSeconds() - currentTime;
        return Math.max(0L, remainingTime);
    }



    public static long setPurchaseCooldown(Player player) {
        long currentTime = System.currentTimeMillis() / 1000L;
        long cooldownTime = currentTime + SlotShop.getCooldownTimeSeconds();
        purchaseCooldowns.put(player.getUniqueId(), cooldownTime);
        return cooldownTime;
    }

    public static void setSlotshopCooldown(Player player) {
        purchaseCooldownSlotShop.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public static boolean canPurchaseSlotShop(Player player) {
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldownSlotShop.get(player.getUniqueId());
        return lastPurchaseTime == null || currentTime - lastPurchaseTime >= 250L;
    }

    public static ChosenItem getRandomItem(Inventory inventory) {
        List<ChosenItem> slots = new ArrayList<>();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                slots.add(new ChosenItem(i, item));
            }
        }

        if (slots.isEmpty()) {
            return null;
        } else {
            int chosenIndex = new Random().nextInt(slots.size());
            return slots.get(chosenIndex);
        }
    }

    public static boolean chargePlayer(Player buyer, String seller, String coOwner, double cost) {
        EconomyResponse withdrawResponse = SlotShop.getInstance().econ.withdrawPlayer(buyer, cost);
        if (!withdrawResponse.transactionSuccess()) {
            buyer.sendMessage(ChatColor.RED + "Transaction failed: " + withdrawResponse.errorMessage);
            return false;
        }

        if (coOwner != null && !coOwner.isEmpty()) {
            double paymentAmount = cost * 0.5;
            EconomyResponse sellerPayment = SlotShop.getInstance().econ.depositPlayer(Bukkit.getOfflinePlayer(seller), paymentAmount);
            if (!sellerPayment.transactionSuccess()) {
                buyer.sendMessage(ChatColor.RED + "Transaction failed: " + sellerPayment.errorMessage);
                SlotShop.getInstance().econ.withdrawPlayer(buyer, cost);
                return false;
            }

            EconomyResponse coOwnerPayment = SlotShop.getInstance().econ.depositPlayer(Bukkit.getOfflinePlayer(coOwner), paymentAmount);
            if (!coOwnerPayment.transactionSuccess()) {
                buyer.sendMessage(ChatColor.RED + "Transaction failed: " + coOwnerPayment.errorMessage);
                SlotShop.getInstance().econ.withdrawPlayer(buyer, cost);
                SlotShop.getInstance().econ.withdrawPlayer(Bukkit.getOfflinePlayer(seller), paymentAmount);
                return false;
            }
        } else {
            EconomyResponse payment = SlotShop.getInstance().econ.depositPlayer(Bukkit.getOfflinePlayer(seller), cost);
            if (!payment.transactionSuccess()) {
                buyer.sendMessage(ChatColor.RED + "Transaction failed: " + payment.errorMessage);
                SlotShop.getInstance().econ.withdrawPlayer(buyer, cost);
                return false;
            }
        }

        return true;
    }

    public static boolean canAfford(Player player, double cost) {
        return SlotShop.getInstance().econ.has(player, cost);
    }

    public static boolean hasInventorySpace(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    public static int getQuantityInInventory(Inventory inventory, ItemStack itemStack) {
        int quantity = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(itemStack)) {
                quantity += item.getAmount();
            }
        }
        return quantity;
    }

    public static boolean isStickEquipped(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.STICK;
    }

    public static boolean isAxeEquipped(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        Material itemType = item.getType();
        return itemType.toString().toLowerCase().contains("_axe");
    }

    public static Map<Location, ShopData> getShopDataMap() {
        return shopDataMap;
    }

    public static void addPurchaseToHistory(String buyerName, String sellerName, String coOwnerName, double cost, ItemStack item, SlotShop plugin) {
        OfflinePlayer offlineSeller = Bukkit.getOfflinePlayer(sellerName);
        UUID sellerUUID = offlineSeller.getUniqueId();
        List<PurchaseHistory.Purchase> sellerPurchases = PurchaseHistory.getPurchaseHistory().computeIfAbsent(sellerUUID, k -> new ArrayList<>());
        long currentTime = System.currentTimeMillis();
        PurchaseHistory.Purchase sellerPurchase = new PurchaseHistory.Purchase(buyerName, sellerName, cost, item, currentTime);
        sellerPurchases.add(sellerPurchase);

        if (coOwnerName != null && !coOwnerName.isEmpty()) {
            OfflinePlayer offlineCoOwner = Bukkit.getOfflinePlayer(coOwnerName);
            UUID coOwnerUUID = offlineCoOwner.getUniqueId();
            List<PurchaseHistory.Purchase> coOwnerPurchases = PurchaseHistory.getPurchaseHistory().computeIfAbsent(coOwnerUUID, k -> new ArrayList<>());
            PurchaseHistory.Purchase coOwnerPurchase = new PurchaseHistory.Purchase(buyerName, coOwnerName, cost, item, currentTime);
            coOwnerPurchases.add(coOwnerPurchase);
        }

        PurchaseHistory.savePurchaseHistory(plugin);
        plugin.getLogger().info("Purchase saved - Buyer: " + buyerName + ", Seller: " + sellerName + ", Co-Owner: " + coOwnerName + ", Cost: " + cost + ", Item: " + item.getType().toString());
    }

    public static void addCoOwner(Location signLocation, String coOwnerName, SlotShop plugin) {
        ShopData shopData = shopDataMap.get(signLocation);
        if (shopData == null) {
            shopData = new ShopData(coOwnerName);
            shopDataMap.put(signLocation, shopData);
        } else {
            shopData.setCoOwner(coOwnerName);
        }
        plugin.getLogger().info("Co-owner " + coOwnerName + " added to shop at " + signLocation);
    }

    public static boolean removeCoOwner(Location signLocation, SlotShop plugin) {
        if (shopDataMap.containsKey(signLocation)) {
            shopDataMap.get(signLocation).setCoOwner(null);
            plugin.getLogger().info("Co-owner removed from shop at " + signLocation);
            return true;
        }
        return false;
    }

    public static void clearOldRecords(String days, SlotShop plugin) {
        int daysToKeep;
        try {
            daysToKeep = Integer.parseInt(days);
        } catch (NumberFormatException e) {
            daysToKeep = 30; // Default to 30 days if input is invalid
        }

        long currentTime = System.currentTimeMillis();
        long daysInMillis = daysToKeep * 24 * 60 * 60 * 1000L;

        PurchaseHistory.getPurchaseHistory().entrySet().removeIf(entry -> {
            List<PurchaseHistory.Purchase> purchases = entry.getValue();
            purchases.removeIf(purchase -> currentTime - purchase.getTime() > daysInMillis);
            return purchases.isEmpty();
        });

        PurchaseHistory.savePurchaseHistory(plugin);
    }

    public static class ShopData {
        private String coOwner;

        public ShopData(String coOwner) {
            this.coOwner = coOwner;
        }

        public String getCoOwner() {
            return coOwner;
        }

        public void setCoOwner(String coOwner) {
            this.coOwner = coOwner;
        }
    }
}
