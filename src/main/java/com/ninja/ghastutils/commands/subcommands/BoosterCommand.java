
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BoosterCommand extends SubCommand {
    private final GhastUtils plugin;

    public BoosterCommand(GhastUtils plugin) {
        super("booster", "ghastutils.booster", false);
        this.plugin = plugin;
        this.addAlias("boosters");
    }

    public String getDescription() {
        return "Manage player boosters";
    }

    public String getUsage() {
        return "/gutil booster <give/clear> <player> [multiplier] [duration]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return true;
        } else if (args.length < 2) {
            sender.sendMessage("§cUsage: " + this.getUsage());
            return true;
        } else {
            String action = args[0].toLowerCase();
            String playerName = args[1];
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return true;
            } else {
                switch (action) {
                    case "give":
                        if (args.length < 4) {
                            sender.sendMessage("§cUsage: /gutil booster give <player> <multiplier> <duration>");
                            return true;
                        }

                        try {
                            double multiplier = Double.parseDouble(args[2]);
                            long duration = Long.parseLong(args[3]);
                            if (multiplier <= (double)0.0F) {
                                sender.sendMessage("§cMultiplier must be greater than 0");
                                return true;
                            }

                            if (duration <= 0L) {
                                sender.sendMessage("§cDuration must be greater than 0");
                                return true;
                            }

                            this.plugin.getMultiplierManager().setBooster(target.getUniqueId(), multiplier, duration);
                            target.sendMessage("§aYou received a booster with multiplier " + multiplier + "x for " + duration + " seconds");
                        } catch (NumberFormatException var12) {
                            sender.sendMessage("§cInvalid number format");
                        }
                        break;
                    case "clear":
                        this.plugin.getMultiplierManager().clearBooster(target.getUniqueId());
                        sender.sendMessage("§aBooster cleared for " + target.getName());
                        target.sendMessage("§cYour booster has been cleared");
                        break;
                    default:
                        sender.sendMessage("§cUnknown action: " + action);
                        sender.sendMessage("§cUsage: " + this.getUsage());
                }

                return true;
            }
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return new ArrayList();
        } else if (args.length == 1) {
            return this.filterStartsWith(Arrays.asList("give", "clear"), args[0]);
        } else if (args.length == 2) {
            return null;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return this.filterStartsWith(Arrays.asList("1.5", "2.0", "2.5", "3.0"), args[2]);
        } else {
            return (List<String>)(args.length == 4 && args[0].equalsIgnoreCase("give") ? this.filterStartsWith(Arrays.asList("300", "600", "1800", "3600"), args[3]) : new ArrayList());
        }
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        List<String> result = new ArrayList();

        for(String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }

        return result;
    }
}
