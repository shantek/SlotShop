package io.shantek;

import io.shantek.helpers.Commands;
import io.shantek.helpers.ConfigData;
import io.shantek.helpers.PurchaseHistory;
import io.shantek.listeners.InventoryCloseListener;
import io.shantek.listeners.PlayerInteractListener;
import io.shantek.listeners.PlayerJoinListener;
import io.shantek.listeners.SignChangeListener;
import io.shantek.helpers.TabComplete;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SlotShop extends JavaPlugin {
    private static SlotShop instance;
    private static final int PAGE_SIZE = 10;
    private static long COOLDOWN_TIME_SECONDS = 86400L;
    public Economy econ = null;

    public static SlotShop getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Check if Vault is available
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return; // Ensure the method exits here
        }

        // Load configurations and initialize the plugin
        ConfigData.loadConfiguration(this);
        COOLDOWN_TIME_SECONDS = ConfigData.getCooldownDuration();
        PurchaseHistory.loadPurchaseHistory(this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new SignChangeListener(this), this);
        getCommand("slotshop").setExecutor(new Commands(this));
        getCommand("slotshop").setTabCompleter(new TabComplete(this));

        // Save data on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PurchaseHistory.savePurchaseHistory(this);
            ConfigData.saveShopData(this);
        }));

        // Load shop data
        ConfigData.loadShopData(this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static int getPageSize() {
        return PAGE_SIZE;
    }

    public static long getCooldownTimeSeconds() {
        return COOLDOWN_TIME_SECONDS;
    }
}
