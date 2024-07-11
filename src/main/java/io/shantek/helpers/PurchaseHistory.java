package io.shantek.helpers;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PurchaseHistory {
    private static final Map<UUID, List<Purchase>> purchaseHistory = new ConcurrentHashMap<>();

    public static Map<UUID, List<Purchase>> getPurchaseHistory() {
        return purchaseHistory;
    }

    public static void loadPurchaseHistory(SlotShop plugin) {
        File dataFolder = plugin.getDataFolder();
        File purchaseFile = new File(dataFolder, "purchase_history.yml");
        if (!purchaseFile.exists()) {
            plugin.getLogger().info("[SlotShop] No purchase history found.");
        } else {
            YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(purchaseFile);
            ConfigurationSection purchasesSection = dataConfig.getConfigurationSection("purchases");
            if (purchasesSection == null) {
                plugin.getLogger().info("[SlotShop] No purchase history found.");
            } else {
                for (String playerUUID : purchasesSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(playerUUID);
                        List<Map<?, ?>> serializedPurchases = dataConfig.getMapList("purchases." + playerUUID);
                        List<Purchase> playerPurchases = new ArrayList<>();
                        for (Map<?, ?> serializedPurchase : serializedPurchases) {
                            String buyer = (String) serializedPurchase.get("buyer");
                            String seller = (String) serializedPurchase.get("seller");
                            String item = (String) serializedPurchase.get("item");
                            double cost = (Double) serializedPurchase.get("cost");
                            long timestamp = (Long) serializedPurchase.get("timestamp");
                            playerPurchases.add(new Purchase(buyer, seller, cost, new ItemStack(Material.valueOf(item)), timestamp));
                        }
                        purchaseHistory.put(uuid, playerPurchases);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID string: " + playerUUID);
                    }
                }
                plugin.getLogger().info("Purchase history loaded successfully.");
            }
        }
    }

    public static void savePurchaseHistory(SlotShop plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File purchaseFile = new File(dataFolder, "purchase_history.yml");
        YamlConfiguration dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, List<Purchase>> entry : purchaseHistory.entrySet()) {
            String playerUUID = entry.getKey().toString();
            List<Map<String, Object>> serializedPurchases = new ArrayList<>();
            for (Purchase purchase : entry.getValue()) {
                Map<String, Object> serializedPurchase = new HashMap<>();
                serializedPurchase.put("timestamp", purchase.getTime());
                serializedPurchase.put("buyer", purchase.getBuyer());
                serializedPurchase.put("seller", purchase.getSeller());
                serializedPurchase.put("item", purchase.getItemName());
                serializedPurchase.put("cost", purchase.getCost());
                serializedPurchases.add(serializedPurchase);
            }
            dataConfig.set("purchases." + playerUUID, serializedPurchases);
        }

        try {
            dataConfig.save(purchaseFile);
            plugin.getLogger().info("Purchase history saved successfully.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save purchase history: " + e.getMessage());
        }
    }

    public static void displayPurchaseHistory(CommandSender sender, String[] args, SlotShop plugin) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
        } else {
            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();
            List<Purchase> playerPurchases = purchaseHistory.get(playerUUID);
            if (playerPurchases != null && !playerPurchases.isEmpty()) {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " Please enter a valid page number");
                        return;
                    }
                }

                int totalPurchases = playerPurchases.size();
                int totalPages = (int) Math.ceil((double) totalPurchases / 10.0);
                if (page >= 1 && page <= totalPages) {
                    sender.sendMessage("\n" + ChatColor.GREEN + "[SlotShop] " + ChatColor.YELLOW + String.format("%d Slot Shop transactions found (%d pages):", totalPurchases, totalPages) + "\n");

                    int startIndex = (page - 1) * 10;
                    int endIndex = Math.min(startIndex + 10, totalPurchases);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
                    dateFormat.setTimeZone(TimeZone.getDefault());

                    for (int i = endIndex - 1; i >= startIndex; --i) {
                        Purchase purchase = playerPurchases.get(i);
                        long currentTimeMillis = System.currentTimeMillis();
                        long purchaseTimeMillis = purchase.getTime();
                        long timeDifferenceMillis = currentTimeMillis - purchaseTimeMillis;
                        long seconds = timeDifferenceMillis / 1000L;
                        long minutes = seconds / 60L;
                        long hours = minutes / 60L;
                        long days = hours / 24L;
                        String timestamp = days > 0 ? days + "d ago" : (hours > 0 ? hours + "h ago" : (minutes > 0 ? minutes + "m ago" : seconds + "s ago"));
                        String itemName = purchase.getItemName();
                        String formattedName = Arrays.stream(itemName.split("_")).map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()).reduce("", (a, b) -> a + " " + b).trim();
                        String message = timestamp + ": " + ChatColor.GREEN + purchase.getBuyer() + ChatColor.WHITE + " purchased " + ChatColor.YELLOW + formattedName + ChatColor.WHITE + " for " + ChatColor.YELLOW + purchase.getCost();
                        sender.sendMessage(message);
                    }

                    if (page < totalPages) {
                        sender.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + "/slotshop history " + (page + 1) + ChatColor.GREEN + " to see the next page.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "End of records.");
                    }
                    sender.sendMessage(ChatColor.GREEN + "To clear history, use " + ChatColor.YELLOW + "/slotshop clear");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " Please enter a valid page number between 1-" + totalPages);
                }
            } else {
                sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " No purchase history found.");
            }
        }
    }

    public static void clearPurchaseHistory(CommandSender sender, SlotShop plugin) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " This command can only be executed by a player.");
        } else {
            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();
            if (!purchaseHistory.containsKey(playerUUID)) {
                sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " No purchase history found.");
            } else {
                purchaseHistory.remove(playerUUID);
                savePurchaseHistory(plugin);
                sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " Sales history cleared.");
            }
        }
    }

    public static class Purchase {
        private final String buyer;
        private final String seller;
        private final double cost;
        private final String itemName;
        private final long time;

        public Purchase(String buyer, String seller, double cost, ItemStack item, long time) {
            this.buyer = buyer;
            this.seller = seller;
            this.cost = cost;
            this.itemName = item.getType().toString();
            this.time = time;
        }

        public String getBuyer() {
            return buyer;
        }

        public String getSeller() {
            return seller;
        }

        public double getCost() {
            return cost;
        }

        public String getItemName() {
            return itemName;
        }

        public long getTime() {
            return time;
        }
    }
}
