
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PetCommand extends SubCommand {
    private final GhastUtils plugin;

    public PetCommand(GhastUtils plugin) {
        super("pet", "ghastutils.pet", false);
        this.plugin = plugin;
        this.addAlias("pets");
    }

    public String getDescription() {
        return "Manage pet multipliers";
    }

    public String getUsage() {
        return "/gutil pet <player> <start/stop> [multiplier]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return true;
        } else if (args.length < 2) {
            sender.sendMessage("§cUsage: " + this.getUsage());
            return true;
        } else {
            String playerName = args[0];
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return true;
            } else {
                switch (args[1].toLowerCase()) {
                    case "start":
                        if (args.length < 3) {
                            sender.sendMessage("§cUsage: /gutil pet <player> start <multiplier>");
                            return true;
                        }

                        try {
                            double multiplier = Double.parseDouble(args[2]);
                            if (multiplier <= (double)0.0F) {
                                sender.sendMessage("§cMultiplier must be greater than 0");
                                return true;
                            }

                            this.plugin.getMultiplierManager().setPetMultiplier(target.getUniqueId(), multiplier, true);
                            String var10001 = target.getName();
                            sender.sendMessage("§aPet multiplier activated for " + var10001 + " with multiplier " + multiplier + "x");
                            target.sendMessage("§aYour pet boost has been activated! Multiplier: " + multiplier + "x");
                        } catch (NumberFormatException var10) {
                            sender.sendMessage("§cInvalid number format");
                        }
                        break;
                    case "stop":
                        this.plugin.getMultiplierManager().setPetMultiplier(target.getUniqueId(), (double)0.0F, false);
                        sender.sendMessage("§aPet multiplier deactivated for " + target.getName());
                        target.sendMessage("§cYour pet boost has been deactivated");
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
            return null;
        } else if (args.length == 2) {
            return this.filterStartsWith(Arrays.asList("start", "stop"), args[1]);
        } else {
            return (List<String>)(args.length == 3 && args[1].equalsIgnoreCase("start") ? this.filterStartsWith(Arrays.asList("0.3", "0.5", "1.0", "1.5"), args[2]) : new ArrayList());
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
