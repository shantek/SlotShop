package io.shantek.helpers;

import io.shantek.SlotShop;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigData {
    private static long cooldownDuration = 86400L;
    private static Map<Location, ShopData> shopDataMap = new HashMap<>();

    public static void loadConfiguration(SlotShop plugin) {
        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveDefaultConfig();
        cooldownDuration = config.getLong("cooldown-duration-seconds", 86400L);
    }

    public static long getCooldownDuration() {
        return cooldownDuration;
    }

    public static Map<Location, ShopData> getShopDataMap() {
        return shopDataMap;
    }

    public static void saveShopData(SlotShop plugin) {
        File file = new File(plugin.getDataFolder(), "shopdata.yml");
        YamlConfiguration config = new YamlConfiguration();
        if (shopDataMap.isEmpty()) {
            plugin.getLogger().info("No shop data found. Skipping save operation.");
        } else {
            for (Map.Entry<Location, ShopData> entry : shopDataMap.entrySet()) {
                Location signLocation = entry.getKey();
                ShopData shopData = entry.getValue();
                String path = "shopdata." + signLocation.getWorld().getName() + "_" + signLocation.getBlockX() + "_" + signLocation.getBlockY() + "_" + signLocation.getBlockZ();
                config.set(path + ".coOwner", shopData.getCoOwner());
            }
            try {
                config.save(file);
                plugin.getLogger().info("Shop data saved successfully.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save shop data:");
                e.printStackTrace();
            }
        }
    }

    public static void loadShopData(SlotShop plugin) {
        plugin.getLogger().info("Loading shop data");
        File file = new File(plugin.getDataFolder(), "shopdata.yml");
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            if (config.contains("shopdata")) {
                for (String key : config.getConfigurationSection("shopdata").getKeys(false)) {
                    String[] parts = key.split("_");
                    Location signLocation = new Location(plugin.getServer().getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                    String coOwner = config.getString("shopdata." + key + ".coOwner");
                    shopDataMap.put(signLocation, new ShopData(coOwner));
                }
                plugin.getLogger().info("Successfully loaded shop data");
            }
        } catch (InvalidConfigurationException | IOException e) {
            plugin.getLogger().severe("Failed to load shop data:");
            e.printStackTrace();
        }
    }
}
