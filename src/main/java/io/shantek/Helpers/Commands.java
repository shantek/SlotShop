package io.shantek.Helpers;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Commands {

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
                            SlotShop.ShopData shopData = (SlotShop.ShopData)this.shopDataMap.get(signLocation);
                            if (shopData == null) {
                                shopData = new SlotShop.ShopData(coOwnerName);
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
                                SlotShop.ShopData shopData = (SlotShop.ShopData)this.shopDataMap.get(signLocation);
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

}
