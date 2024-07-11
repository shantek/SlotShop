package io.shantek.helpers;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigData {
    private static Map<Location, ShopData> shopDataMap = new HashMap<>();
    private static long cooldownDuration;

    public static void loadConfiguration(SlotShop plugin) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
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

        for (Map.Entry<Location, ShopData> entry : shopDataMap.entrySet()) {
            Location loc = entry.getKey();
            ShopData shopData = entry.getValue();
            String path = "shopdata." + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
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

    public static void loadShopData(SlotShop plugin) {
        File file = new File(plugin.getDataFolder(), "shopdata.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
                plugin.getLogger().info("Created new shopdata.yml file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create shopdata.yml file:");
                e.printStackTrace();
                return;
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("shopdata")) {
            for (String key : config.getConfigurationSection("shopdata").getKeys(false)) {
                String[] parts = key.split("_");
                World world = Bukkit.getWorld(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                Location loc = new Location(world, x, y, z);
                String coOwner = config.getString("shopdata." + key + ".coOwner");
                shopDataMap.put(loc, new ShopData(coOwner));
            }
            plugin.getLogger().info("Shop data loaded successfully.");
        }
    }
}
