package io.shantek.helpers;

import io.shantek.SlotShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class Commands implements CommandExecutor {
    private final SlotShop plugin;

    public Commands(SlotShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("slotshop")) {
            if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
                sendAvailableSubcommands(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "purgegamble":
                    handlePurgeGamble(sender);
                    return true;
                case "create":
                    handleCreate(sender, false);
                    return true;
                case "creategamble":
                    handleCreate(sender, true);
                    return true;
                case "history":
                    PurchaseHistory.displayPurchaseHistory(sender, args, plugin);
                    return true;
                case "clear":
                    PurchaseHistory.clearPurchaseHistory(sender, plugin);
                    return true;
                case "purgesales":
                    handlePurgeSales(sender, args);
                    return true;
                case "addcoowner":
                    handleAddCoOwner(sender, args);
                    return true;
                case "removecoowner":
                    handleRemoveCoOwner(sender);
                    return true;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Type /slotshop for a list of commands.");
                    return true;
            }
        }
        return false;
    }

    private void sendAvailableSubcommands(CommandSender sender) {
        List<String> matchingSubcommands = new ArrayList<>();
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
        for (String subcommand : matchingSubcommands) {
            sender.sendMessage(subcommand);
        }
    }

    private void handlePurgeGamble(CommandSender sender) {
        if (!sender.hasPermission("slotshop.command.purgegamble") && !sender.isOp()) {
            sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You don't have permission to use this command.");
        } else {
            Functions.purgeGambleTimes();
            sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.YELLOW + "Gamble times purged for all players.");
        }
    }

    private void handleCreate(CommandSender sender, boolean isGamble) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return;
        }

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null && targetBlock.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) targetBlock.getState();

            // Function to check if the target block has an attached sign
            if (hasAttachedSign(targetBlock)) {
                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Please remove the signs off the barrel first.");
            } else {
                String customName = isGamble ? Functions.gambleBarrelName : Functions.customBarrelName;
                barrel.setCustomName(customName);
                barrel.update();
                player.sendMessage(ChatColor.GREEN + "Slot Shop " + (isGamble ? "Gamble " : "") + "created successfully.");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Barrel not found.");
        }
    }

    public boolean hasAttachedSign(Block block) {
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP };
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getState() instanceof Sign) {
                Sign sign = (Sign) relative.getState();
                // Check if the sign is attached to the current block
                if (isSignAttached(sign, block)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSignAttached(Sign sign, Block block) {
        Block attachedBlock = sign.getBlock().getRelative(((org.bukkit.material.Sign) sign.getData()).getAttachedFace());
        return attachedBlock.equals(block);
    }

    private void handlePurgeSales(CommandSender sender, String[] args) {
        if (!sender.hasPermission("slotshop.purgesales") && !sender.isOp()) {
            sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.RED + " You don't have permission to run this command.");
        } else {
            String days = args.length > 1 ? args[1] : "30";
            Functions.clearOldRecords(days, plugin);
            sender.sendMessage(ChatColor.GREEN + "[SlotShop]" + ChatColor.YELLOW + " Old records cleared successfully.");
        }
    }

    private void handleAddCoOwner(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "This command can only be executed by a player.");
            return;
        }

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlock((Set) null, 5);
        if (targetBlock.getState() instanceof Sign) {
            Sign sign = (Sign) targetBlock.getState();
            String ownerName = sign.getLine(0);
            Block attachedBlock = targetBlock.getRelative(Functions.getAttachedFace(targetBlock));
            if (!(attachedBlock.getState() instanceof Barrel)) {
                player.sendMessage(ChatColor.RED + "This sign is not associated with a valid shop.");
                return;
            }

            Barrel barrel = (Barrel) attachedBlock.getState();
            String barrelCustomName = barrel.getCustomName();
            if (barrelCustomName == null || (!barrelCustomName.equals(Functions.customBarrelName) && !barrelCustomName.equals(Functions.gambleBarrelName))) {
                player.sendMessage(ChatColor.RED + "This sign is not associated with a valid shop.");
                return;
            }

            if (ownerName.equals(player.getName())) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Usage: /slotshop addcoowner <coowner>");
                    return;
                }

                String coOwnerName = args[1];
                if (coOwnerName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You cannot add yourself as a co-owner.");
                    return;
                }

                OfflinePlayer coOwner = Bukkit.getOfflinePlayer(coOwnerName);
                if (coOwner.hasPlayedBefore()) {
                    Location signLocation = targetBlock.getLocation();
                    Functions.addCoOwner(signLocation, coOwnerName, plugin);
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.GREEN + "Co-owner added to the shop.");
                    ConfigData.saveShopData(plugin);
                    return;
                }

                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "Invalid player name.");
                return;
            }

            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be the owner of the shop to add a co-owner.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be looking at a sign to add a co-owner.");
    }

    private void handleRemoveCoOwner(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "This command can only be executed by a player.");
            return;
        }

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlock((Set) null, 5);
        if (targetBlock.getState() instanceof Sign) {
            Sign sign = (Sign) targetBlock.getState();
            String ownerName = sign.getLine(0);
            if (ownerName.equals(player.getName())) {
                Location signLocation = targetBlock.getLocation();
                if (Functions.removeCoOwner(signLocation, plugin)) {
                    player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.GREEN + "Co-owner removed from the shop.");
                    ConfigData.saveShopData(plugin);
                    return;
                }

                player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "This sign is not associated with a shop.");
                return;
            }

            player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be the owner of the shop to remove a co-owner.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "[SlotShop] " + ChatColor.RED + "You must be looking at a sign to remove a co-owner.");
    }
}
