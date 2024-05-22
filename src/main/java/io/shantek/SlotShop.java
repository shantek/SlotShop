//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.shantek;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SlotShop extends JavaPlugin implements Listener, TabCompleter {
    private static final int PAGE_SIZE = 10;
    private Economy econ = null;
    private String customBarrelName = "SlotShop";
    private String gambleBarrelName = "GambleShop";
    private static long COOLDOWN_TIME_SECONDS = 86400L;
    private final Map<UUID, Long> purchaseCooldowns = new HashMap();
    private Map<UUID, Long> purchaseCooldownSlotShop = new HashMap();
    private static final long COOLDOWN_DURATION = 250L;
    private final Map<UUID, List<Purchase>> purchaseHistory = new ConcurrentHashMap();
    private Map<Location, ShopData> shopDataMap = new HashMap();
    private FileConfiguration dataConfig;
    private File dataFile;

    public SlotShop() {
    }

    public void onEnable() {
        if (!this.setupEconomy()) {
            this.getLogger().severe("Disabled due to no Vault dependency found!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            this.loadConfiguration();
            this.loadPurchaseHistory();
            this.getServer().getPluginManager().registerEvents(this, this);
            this.getCommand("slotshop").setExecutor(this);
            this.getCommand("slotshop").setTabCompleter(this);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                this.savePurchaseHistory();
                this.saveShopData();
            }));
            this.loadShopData();
        }
    }

    private void loadConfiguration() {
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
        COOLDOWN_TIME_SECONDS = this.getConfig().getLong("cooldown-duration-seconds", 86400L);
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("slotshop") && args.length == 1) {
            List<String> subcommands = new ArrayList();
            if (sender.hasPermission("slotshop.create")) {
                subcommands.add("create");
            }

            if (sender.hasPermission("slotshop.create.gamblebarrel")) {
                subcommands.add("creategamble");
            }

            if (sender.hasPermission("slotshop.command.purgegamble")) {
                subcommands.add("purgegamble");
            }

            subcommands.add("history");
            subcommands.add("clear");
            if (sender.hasPermission("slotshop.purgesales")) {
                subcommands.add("purgesales");
            }

            if (sender.hasPermission("slotshop.coowner.add")) {
                subcommands.add("addcoowner");
            }

            if (sender.hasPermission("slotshop.coowner.remove")) {
                subcommands.add("removecoowner");
            }

            List<String> matchingSubcommands = new ArrayList();
            String input = args[0].toLowerCase();
            Iterator var8 = subcommands.iterator();

            while(var8.hasNext()) {
                String subcommand = (String)var8.next();
                if (subcommand.startsWith(input)) {
                    matchingSubcommands.add(subcommand);
                }
            }

            return matchingSubcommands;
        } else {
            return Collections.emptyList();
        }
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("slotshop")) {
            if (args.length == 0 || args.length == 1 && args[0].isEmpty()) {
                List<String> matchingSubcommands = new ArrayList();
                if (sender.hasPermission("slotshop.create")) {
                    matchingSubcommands.add(ChatColor.GREEN + "create");
                }

                if (sender.hasPermission("slotshop.create.gamblebarrel")) {
                    matchingSubcommands.add(ChatColor.GREEN + "creategamble");
                }

                if (sender.hasPermission("slotshop.command.purgegamble") || sender.isOp()) {
                    matchingSubcommands.add(ChatColor.GREEN + "purgegamble");
                }

                matchingSubcommands.add(ChatColor.GREEN + "history");
                matchingSubcommands.add(ChatColor.GREEN + "clear");
                matchingSubcommands.add(ChatColor.GREEN + "addcoowner");
                matchingSubcommands.add(ChatColor.GREEN + "removecoowner");
                if (sender.hasPermission("slotshop.purgesales") || sender.isOp()) {
                    matchingSubcommands.add(ChatColor.GREEN + "purgesales");
                }

                sender.sendMessage(ChatColor.YELLOW + "Available subcommands:");
                Iterator var18 = matchingSubcommands.iterator();

                while(var18.hasNext()) {
                    String subcommand = (String)var18.next();
                    sender.sendMessage(subcommand);
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("purgegamble")) {
                if (!sender.hasPermission("slotshop.command.purgegamble") && !sender.isOp()) {
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to use this command.");
                } else {
                    this.purgeGambleTimes();
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.YELLOW + "Gamble times purged for all players.");
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (sender.hasPermission("slotshop.create")) {
                    this.createSlotShop(sender, false);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to create a Slot Shop.");
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("creategamble")) {
                if (sender.hasPermission("slotshop.create.gamblebarrel")) {
                    this.createSlotShop(sender, true);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to create a Gamble Slot Shop.");
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("history")) {
                this.displayPurchaseHistory(sender, args);
                return true;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                this.clearPurchaseHistory(sender);
                return true;
            }

            if (args[0].equalsIgnoreCase("purgesales")) {
                if (!sender.hasPermission("slotshop.purgesales") && !sender.isOp()) {
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " You don't have permission to run this command.");
                } else {
                    String days = "30";
                    if (args.length > 1) {
                        days = args[1];
                    }

                    this.clearOldRecords(days);
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " Old records cleared successfully.");
                }

                return true;
            }

            Player player;
            Block targetBlock;
            Sign sign;
            String ownerName;
            if (args[0].equalsIgnoreCase("addcoowner")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "This command can only be executed by a player.");
                    return true;
                }

                player = (Player)sender;
                targetBlock = player.getTargetBlock((Set)null, 5);
                if (targetBlock.getState() instanceof Sign) {
                    sign = (Sign)targetBlock.getState();
                    ownerName = sign.getLine(0);
                    Block attachedBlock = targetBlock.getRelative(this.getAttachedFace(targetBlock));
                    if (!(attachedBlock.getState() instanceof Barrel)) {
                        player.sendMessage(ChatColor.RED + "This sign is not associated with a valid shop.");
                        return true;
                    }

                    Barrel barrel = (Barrel)attachedBlock.getState();
                    String barrelCustomName = barrel.getCustomName();
                    if (barrelCustomName == null || !barrelCustomName.equals(this.customBarrelName) && !barrelCustomName.equals(this.gambleBarrelName)) {
                        player.sendMessage(ChatColor.RED + "This sign is not associated with a valid shop.");
                        return true;
                    }

                    if (ownerName.equals(player.getName())) {
                        if (args.length < 2) {
                            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Usage: /slotshop addcoowner <coowner>");
                            return true;
                        }

                        String coOwnerName = args[1];
                        if (coOwnerName.equalsIgnoreCase(player.getName())) {
                            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You cannot add yourself as a co-owner.");
                            return true;
                        }

                        OfflinePlayer coOwner = Bukkit.getOfflinePlayer(coOwnerName);
                        if (coOwner.hasPlayedBefore()) {
                            Location signLocation = targetBlock.getLocation();
                            ShopData shopData = (ShopData)this.shopDataMap.get(signLocation);
                            if (shopData == null) {
                                shopData = new ShopData(coOwnerName);
                                this.shopDataMap.put(signLocation, shopData);
                            } else {
                                shopData.setCoOwner(coOwnerName);
                            }

                            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.GREEN + "Co-owner added to the shop.");
                            this.saveShopData();
                            return true;
                        }

                        player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Invalid player name.");
                        return true;
                    }

                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be the owner of the shop to add a co-owner.");
                    return true;
                }

                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be looking at a sign to add a co-owner.");
                return true;
            }

            if (args[0].equalsIgnoreCase("removecoowner")) {
                if (sender instanceof Player) {
                    player = (Player)sender;
                    targetBlock = player.getTargetBlock((Set)null, 5);
                    if (targetBlock.getState() instanceof Sign) {
                        sign = (Sign)targetBlock.getState();
                        ownerName = sign.getLine(0);
                        if (ownerName.equals(player.getName())) {
                            Location signLocation = targetBlock.getLocation();
                            if (this.shopDataMap.containsKey(signLocation)) {
                                ShopData shopData = (ShopData)this.shopDataMap.get(signLocation);
                                shopData.setCoOwner((String)null);
                                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.GREEN + "Co-owner removed from the shop.");
                                this.saveShopData();
                                return true;
                            }

                            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "This sign is not associated with a shop.");
                            return true;
                        }

                        player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be the owner of the shop to remove a co-owner.");
                        return true;
                    }

                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be looking at a sign to remove a co-owner.");
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "This command can only be executed by a player.");
                return true;
            }
        }

        return false;
    }

    private void clearOldRecords(String days) {
        int daysToKeep;
        try {
            daysToKeep = Integer.parseInt(days);
        } catch (NumberFormatException var12) {
            daysToKeep = 30;
        }

        long currentTime = System.currentTimeMillis();
        long daysInMillis = (long)(daysToKeep * 24 * 60 * 60 * 1000);
        Iterator<Map.Entry<UUID, List<Purchase>>> iterator = this.purchaseHistory.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<UUID, List<Purchase>> entry = (Map.Entry)iterator.next();
            List<Purchase> purchases = (List)entry.getValue();
            Iterator<Purchase> purchaseIterator = purchases.iterator();

            while(purchaseIterator.hasNext()) {
                Purchase purchase = (Purchase)purchaseIterator.next();
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
                    List<Purchase> playerPurchases = new ArrayList();
                    Iterator var10 = serializedPurchases.iterator();

                    while(var10.hasNext()) {
                        Map<?, ?> serializedPurchase = (Map)var10.next();
                        String buyer = (String)serializedPurchase.get("buyer");
                        String seller = (String)serializedPurchase.get("seller");
                        String item = (String)serializedPurchase.get("item");
                        double cost = (Double)serializedPurchase.get("cost");
                        long timestamp = (Long)serializedPurchase.get("timestamp");
                        Purchase purchase = new Purchase(buyer, seller, cost, new ItemStack(Material.valueOf(item)), timestamp);
                        playerPurchases.add(purchase);
                    }

                    this.purchaseHistory.put(uuid, playerPurchases);
                }

                this.getLogger().info("Purchase history loaded successfully.");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        List<Purchase> playerPurchases = (List)this.purchaseHistory.get(playerUUID);
        if (playerPurchases != null && !playerPurchases.isEmpty()) {
            int totalPurchases = playerPurchases.size();
            player.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " You made " + ChatColor.WHITE + totalPurchases + " sales" + ChatColor.YELLOW + " since you last checked.");
            player.sendMessage(ChatColor.YELLOW + "To see them, type " + ChatColor.GREEN + "/slotshop history");
        }

    }

    private void addPurchaseToHistory(String buyerName, String sellerName, String coOwnerName, double cost, ItemStack item) {
        OfflinePlayer offlineSeller = Bukkit.getOfflinePlayer(sellerName);
        UUID sellerUUID = offlineSeller.getUniqueId();
        List<Purchase> sellerPurchases = (List)this.purchaseHistory.computeIfAbsent(sellerUUID, (k) -> {
            return new ArrayList();
        });
        long currentTime = System.currentTimeMillis();
        Purchase sellerPurchase = new Purchase(buyerName, sellerName, cost, item, currentTime);
        sellerPurchases.add(sellerPurchase);
        if (coOwnerName != null && !coOwnerName.isEmpty()) {
            OfflinePlayer offlineCoOwner = Bukkit.getOfflinePlayer(coOwnerName);
            UUID coOwnerUUID = offlineCoOwner.getUniqueId();
            List<Purchase> coOwnerPurchases = (List)this.purchaseHistory.computeIfAbsent(coOwnerUUID, (k) -> {
                return new ArrayList();
            });
            Purchase coOwnerPurchase = new Purchase(buyerName, coOwnerName, cost, item, currentTime);
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
            Map.Entry<UUID, List<Purchase>> entry = (Map.Entry)var4.next();
            String playerUUID = ((UUID)entry.getKey()).toString();
            List<Purchase> playerPurchases = (List)entry.getValue();
            List<Map<String, Object>> serializedPurchases = new ArrayList();
            Iterator var9 = playerPurchases.iterator();

            while(var9.hasNext()) {
                Purchase purchase = (Purchase)var9.next();
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
            Map.Entry<UUID, List<Purchase>> entry = (Map.Entry)var4.next();
            String playerUUID = ((UUID)entry.getKey()).toString();
            List<Purchase> playerPurchases = (List)entry.getValue();
            List<Map<String, Object>> serializedPurchases = new ArrayList();
            Iterator var9 = playerPurchases.iterator();

            while(var9.hasNext()) {
                Purchase purchase = (Purchase)var9.next();
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
            List<Purchase> playerPurchases = (List)this.purchaseHistory.get(playerUUID);
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
                        Purchase purchase = (Purchase)playerPurchases.get(nextPage);
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof Barrel) {
            Barrel barrel = (Barrel)inventory.getHolder();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName != null && (barrelCustomName.equals(this.customBarrelName) || barrelCustomName.equals(this.gambleBarrelName))) {
                Block block = barrel.getBlock();
                Block signBlock = this.getAttachedSignBlock(block);
                if (signBlock != null && signBlock.getState() instanceof Sign) {
                    Sign sign = (Sign)signBlock.getState();
                    this.updateStockLine(barrel, signBlock);
                }
            }
        }

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

    public void saveShopData() {
        File file = new File(this.getDataFolder(), "shopdata.yml");
        YamlConfiguration config = new YamlConfiguration();
        if (this.shopDataMap.isEmpty()) {
            this.getLogger().info("No shop data found. Skipping save operation.");
        } else {
            Iterator var3 = this.shopDataMap.keySet().iterator();

            while(var3.hasNext()) {
                Location signLocation = (Location)var3.next();
                ShopData shopData = (ShopData)this.shopDataMap.get(signLocation);
                String path = "shopdata." + signLocation.getWorld().getName() + "_" + signLocation.getBlockX() + "_" + signLocation.getBlockY() + "_" + signLocation.getBlockZ();
                config.set(path + ".coOwner", shopData.getCoOwner());
            }

            try {
                config.save(file);
                this.getLogger().info("Shop data saved successfully.");
            } catch (IOException var7) {
                IOException e = var7;
                this.getLogger().severe("Failed to save shop data:");
                e.printStackTrace();
            }

        }
    }

    public void loadShopData() {
        this.getLogger().info("Loading shop data");
        File file = new File(this.getDataFolder(), "shopdata.yml");
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(file);
            if (config.contains("shopdata")) {
                ConfigurationSection shopDataSection = config.getConfigurationSection("shopdata");
                Iterator var4 = shopDataSection.getKeys(false).iterator();

                while(var4.hasNext()) {
                    String key = (String)var4.next();
                    String[] parts = key.split("_");
                    World world = Bukkit.getWorld(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    Location signLocation = new Location(world, (double)x, (double)y, (double)z);
                    String coOwner = shopDataSection.getString(key + ".coOwner");
                    ShopData shopData = new ShopData(coOwner);
                    this.shopDataMap.put(signLocation, shopData);
                }

                this.getLogger().info("Successfully loaded shop data");
            }
        } catch (InvalidConfigurationException | IOException var14) {
            Exception e = var14;
            this.getLogger().severe("Failed to load shop data:");
            e.printStackTrace();
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
                            ShopData shopData = (ShopData)this.shopDataMap.get(signLocation);
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
                    ShopData shopData = (ShopData)this.shopDataMap.get(signLocation);
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
                ChosenItem chosenItem = this.getRandomItem(inventory);
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

    private ChosenItem getRandomItem(Inventory inventory) {
        List<ChosenItem> slots = new ArrayList();
        ItemStack[] contents = inventory.getContents();

        int chosenIndex;
        for(chosenIndex = 0; chosenIndex < contents.length; ++chosenIndex) {
            ItemStack item = contents[chosenIndex];
            if (item != null && item.getType() != Material.AIR) {
                slots.add(new ChosenItem(chosenIndex, item));
            }
        }

        if (slots.isEmpty()) {
            return null;
        } else {
            chosenIndex = (new Random()).nextInt(slots.size());
            return (ChosenItem)slots.get(chosenIndex);
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

    public class ShopData {
        private String coOwner;

        public ShopData(String coOwner) {
            this.coOwner = coOwner;
        }

        public String getCoOwner() {
            return this.coOwner;
        }

        public void setCoOwner(String coOwner) {
            this.coOwner = coOwner;
        }
    }

    public class Purchase {
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
            return this.buyer;
        }

        public String getSeller() {
            return this.seller;
        }

        public double getCost() {
            return this.cost;
        }

        public String getItemName() {
            return this.itemName;
        }

        public long getTime() {
            return this.time;
        }
    }

    private class ChosenItem {
        private final int slot;
        private final ItemStack item;

        public ChosenItem(int slot, ItemStack item) {
            this.slot = slot;
            this.item = item;
        }

        public int getSlot() {
            return this.slot;
        }

        public ItemStack getItem() {
            return this.item;
        }
    }
}
