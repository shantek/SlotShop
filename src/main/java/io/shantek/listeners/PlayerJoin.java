package io.shantek.listeners;

import io.shantek.SlotShop;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PlayerJoin {

    public SlotShop slotShop;

    public PlayerJoin(SlotShop slotShop) {
        this.slotShop = slotShop;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        List<SlotShop.Purchase> playerPurchases = (List)this.purchaseHistory.get(playerUUID);
        if (playerPurchases != null && !playerPurchases.isEmpty()) {
            int totalPurchases = playerPurchases.size();
            player.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " You made " + ChatColor.WHITE + totalPurchases + " sales" + ChatColor.YELLOW + " since you last checked.");
            player.sendMessage(ChatColor.YELLOW + "To see them, type " + ChatColor.GREEN + "/slotshop history");
        }

    }
}
