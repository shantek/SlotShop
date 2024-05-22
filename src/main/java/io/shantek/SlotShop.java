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
