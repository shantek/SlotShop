package io.shantek.helpers;

import io.shantek.SlotShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabComplete implements TabCompleter {
    private final SlotShop plugin;

    public TabComplete(SlotShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("slotshop") && args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("slotshop.create")) {
                subcommands.add("create");
            }
            if (sender.hasPermission("slotshop.create.gamblebarrel")) {
                subcommands.add("creategamble");
            }
            if (sender.hasPermission("slotshop.command.purgegamble")) {
                subcommands.add("purgegamble");
            }
            subcommands.add("history");
            subcommands.add("clear");
            if (sender.hasPermission("slotshop.purgesales")) {
                subcommands.add("purgesales");
            }
            if (sender.hasPermission("slotshop.coowner.add")) {
                subcommands.add("addcoowner");
            }
            if (sender.hasPermission("slotshop.coowner.remove")) {
                subcommands.add("removecoowner");
            }

            List<String> matchingSubcommands = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(input)) {
                    matchingSubcommands.add(subcommand);
                }
            }
            return matchingSubcommands;
        } else {
            return Collections.emptyList();
        }
    }
}
