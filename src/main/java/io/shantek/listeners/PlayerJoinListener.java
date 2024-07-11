package io.shantek.listeners;

import io.shantek.SlotShop;
import io.shantek.helpers.PurchaseHistory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final SlotShop plugin;

    public PlayerJoinListener(SlotShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        List<PurchaseHistory.Purchase> playerPurchases = PurchaseHistory.getPurchaseHistory().get(playerUUID);
        if (playerPurchases != null && !playerPurchases.isEmpty()) {
            int totalPurchases = playerPurchases.size();
            player.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " You made " + ChatColor.WHITE + totalPurchases + " sales" + ChatColor.YELLOW + " since you last checked.");
            player.sendMessage(ChatColor.YELLOW + "To see them, type " + ChatColor.GREEN + "/slotshop history");
        }
    }
}
