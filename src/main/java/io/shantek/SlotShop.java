//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.shantek;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.shantek.helpers.PurchaseHistory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SlotShop extends JavaPlugin implements Listener, TabCompleter {

    public PurchaseHistory purchaseHistory;

    private static final int PAGE_SIZE = 10;
    private Economy econ = null;
    private String customBarrelName = "SlotShop";
    private String gambleBarrelName = "GambleShop";
    private static long COOLDOWN_TIME_SECONDS = 86400L;
    private final Map<UUID, Long> purchaseCooldowns = new HashMap();
    private Map<UUID, Long> purchaseCooldownSlotShop = new HashMap();
    private static final long COOLDOWN_DURATION = 250L;
    private final Map<UUID, List<Purchase>> transactionHistory = new ConcurrentHashMap();
    public Map<Location, ShopData> shopDataMap = new HashMap();
    private FileConfiguration dataConfig;
    private File dataFile;



    public SlotShop() {
    }

    public void onEnable() {
        if (!this.setupEconomy()) {
            this.getLogger().severe("Disabled due to no Vault dependency found!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            config.loadConfiguration();
            this.loadPurchaseHistory();
            this.getServer().getPluginManager().registerEvents(this, this);
            this.getCommand("slotshop").setExecutor(this);
            this.getCommand("slotshop").setTabCompleter(this);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                purchaseHistory.savePurchaseHistory();
                purchaseHistory.saveShopData();
            }));
            this.loadShopData();
        }
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
