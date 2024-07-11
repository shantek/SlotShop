package io.shantek.helpers;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PurchaseHistory {

    public SlotShop slotShop;

    public PurchaseHistory(SlotShop slotShop) {
        this.slotShop = slotShop;
    }

    private void addPurchaseToHistory(String buyerName, String sellerName, String coOwnerName, double cost, ItemStack item) {
        OfflinePlayer offlineSeller = Bukkit.getOfflinePlayer(sellerName);
        UUID sellerUUID = offlineSeller.getUniqueId();
        List<SlotShop.Purchase> sellerPurchases = (List)this.purchaseHistory.computeIfAbsent(sellerUUID, (k) -> {
            return new ArrayList();
        });
        long currentTime = System.currentTimeMillis();
        SlotShop.Purchase sellerPurchase = new SlotShop.Purchase(buyerName, sellerName, cost, item, currentTime);
        sellerPurchases.add(sellerPurchase);
        if (coOwnerName != null && !coOwnerName.isEmpty()) {
            OfflinePlayer offlineCoOwner = Bukkit.getOfflinePlayer(coOwnerName);
            UUID coOwnerUUID = offlineCoOwner.getUniqueId();
            List<SlotShop.Purchase> coOwnerPurchases = (List)this.purchaseHistory.computeIfAbsent(coOwnerUUID, (k) -> {
                return new ArrayList();
            });
            SlotShop.Purchase coOwnerPurchase = new SlotShop.Purchase(buyerName, coOwnerName, cost, item, currentTime);
            coOwnerPurchases.add(coOwnerPurchase);
        }

        this.savePurchaseHistory();
        this.getLogger().info("Purchase saved - Buyer: " + buyerName + ", Seller: " + sellerName + ", Co-Owner: " + coOwnerName + ", Cost: " + cost + ", Item: " + item.getType().toString());
    }

    private void savePurchaseHistory() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File purchaseFile = new File(dataFolder, "purchase_history.yml");
        YamlConfiguration dataConfig = new YamlConfiguration();
        Iterator var4 = this.purchaseHistory.entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<UUID, List<SlotShop.Purchase>> entry = (Map.Entry)var4.next();
            String playerUUID = ((UUID)entry.getKey()).toString();
            List<SlotShop.Purchase> playerPurchases = (List)entry.getValue();
            List<Map<String, Object>> serializedPurchases = new ArrayList();
            Iterator var9 = playerPurchases.iterator();

            while(var9.hasNext()) {
                SlotShop.Purchase purchase = (SlotShop.Purchase)var9.next();
                Map<String, Object> serializedPurchase = new HashMap();
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
            this.getLogger().info("Purchase history saved successfully.");
        } catch (IOException var12) {
            IOException e = var12;
            this.getLogger().severe("Failed to save purchase history: " + e.getMessage());
        }

    }

    private void savePurchaseHistoryToFile() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File purchaseFile = new File(dataFolder, "purchase_history.yml");
        YamlConfiguration dataConfig = new YamlConfiguration();
        Iterator var4 = this.purchaseHistory.entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<UUID, List<SlotShop.Purchase>> entry = (Map.Entry)var4.next();
            String playerUUID = ((UUID)entry.getKey()).toString();
            List<SlotShop.Purchase> playerPurchases = (List)entry.getValue();
            List<Map<String, Object>> serializedPurchases = new ArrayList();
            Iterator var9 = playerPurchases.iterator();

            while(var9.hasNext()) {
                SlotShop.Purchase purchase = (SlotShop.Purchase)var9.next();
                Map<String, Object> serializedPurchase = new HashMap();
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
            this.getLogger().info("Purchase history saved successfully.");
        } catch (IOException var12) {
            IOException e = var12;
            this.getLogger().severe("Failed to save purchase history: " + e.getMessage());
        }

    }

    private void displayPurchaseHistory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
        } else {
            Player player = (Player)sender;
            UUID playerUUID = player.getUniqueId();
            List<SlotShop.Purchase> playerPurchases = (List)this.purchaseHistory.get(playerUUID);
            if (playerPurchases != null && !playerPurchases.isEmpty()) {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException var38) {
                        sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " Please enter a valid page number");
                        return;
                    }
                }

                int totalPurchases = playerPurchases.size();
                int totalPages = (int)Math.ceil((double)totalPurchases / 10.0);
                if (page >= 1 && page <= totalPages) {
                    if (totalPages == 1) {
                        sender.sendMessage("\n" + ChatColor.GREEN + "[SlotShop] " + ChatColor.YELLOW + String.format("%d Slot Shop transactions found (1 page):", totalPurchases) + "\n");
                    } else {
                        sender.sendMessage("\n" + ChatColor.GREEN + "[SlotShop] " + ChatColor.YELLOW + String.format("%d Slot Shop transactions found (%d pages):", totalPurchases, totalPages) + "\n");
                    }

                    int startIndex = (page - 1) * 10;
                    int endIndex = Math.min(startIndex + 10, totalPurchases);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
                    dateFormat.setTimeZone(TimeZone.getDefault());

                    int nextPage;
                    for(nextPage = endIndex - 1; nextPage >= startIndex; --nextPage) {
                        SlotShop.Purchase purchase = (SlotShop.Purchase)playerPurchases.get(nextPage);
                        long currentTimeMillis = System.currentTimeMillis();
                        long purchaseTimeMillis = purchase.getTime();
                        long timeDifferenceMillis = currentTimeMillis - purchaseTimeMillis;
                        long seconds = timeDifferenceMillis / 1000L;
                        long minutes = seconds / 60L;
                        long hours = minutes / 60L;
                        long days = hours / 24L;
                        String timestamp;
                        if (days > 0L) {
                            timestamp = days + "d ago";
                        } else if (hours > 0L) {
                            timestamp = hours + "h ago";
                        } else if (minutes > 0L) {
                            timestamp = minutes + "m ago";
                        } else {
                            timestamp = seconds + "s ago";
                        }

                        String itemName = purchase.getItemName();
                        String[] words = itemName.split("_");
                        StringBuilder formattedItemName = new StringBuilder();
                        String[] var32 = words;
                        int var33 = words.length;

                        for(int var34 = 0; var34 < var33; ++var34) {
                            String word = var32[var34];
                            if (word.length() > 0) {
                                String firstLetter = word.substring(0, 1).toUpperCase();
                                String restOfWord = word.substring(1).toLowerCase();
                                formattedItemName.append(firstLetter).append(restOfWord).append(" ");
                            }
                        }

                        String formattedName = formattedItemName.toString().trim();
                        String message = timestamp + ": " + ChatColor.GREEN + purchase.getBuyer() + ChatColor.WHITE + " purchased " + ChatColor.YELLOW + formattedName + ChatColor.WHITE + " for " + ChatColor.YELLOW + purchase.getCost();
                        sender.sendMessage(message);
                    }

                    nextPage = page + 1;
                    if (nextPage <= totalPages) {
                        String nextPageCommand = String.format("/slotshop history %d", nextPage);
                        String nextPageMessage = ChatColor.GREEN + "Use " + ChatColor.YELLOW + nextPageCommand + ChatColor.GREEN + " to see the next page.";
                        sender.sendMessage(nextPageMessage);
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

}
