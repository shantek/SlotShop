package io.shantek.Helpers;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ConfigData {

    private void loadConfiguration() {
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
        COOLDOWN_TIME_SECONDS = this.getConfig().getLong("cooldown-duration-seconds", 86400L);
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
                SlotShop.ShopData shopData = (SlotShop.ShopData)this.shopDataMap.get(signLocation);
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
                    SlotShop.ShopData shopData = new SlotShop.ShopData(coOwner);
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

}
