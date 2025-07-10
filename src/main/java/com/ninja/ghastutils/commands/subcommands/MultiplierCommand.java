
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MultiplierCommand extends SubCommand {
    private final GhastUtils plugin;

    public MultiplierCommand(GhastUtils plugin) {
        super("multiplier", "ghastutils.multiplier", true);
        this.plugin = plugin;
    }

    public String getDescription() {
        return "View your current multiplier breakdown";
    }

    public String getUsage() {
        return "/gutil multiplier";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (this.checkPlayer(sender) && this.checkPermission(sender)) {
            Player player = (Player)sender;
            Map<String, Double> breakdown = this.plugin.getMultiplierManager().getMultiplierBreakdown(player.getUniqueId());
            sender.sendMessage("§6§l=== Multiplier Breakdown ===");

            for(Map.Entry<String, Double> entry : breakdown.entrySet()) {
                if (!((String)entry.getKey()).equals("Total")) {
                    String multiplierText = (Double)entry.getValue() > (double)0.0F ? "+" + this.formatMultiplier((Double)entry.getValue()) : this.formatMultiplier((Double)entry.getValue());
                    String var10001 = (String)entry.getKey();
                    sender.sendMessage("§e" + var10001 + ": §f" + multiplierText);
                }
            }

            double total = (Double)breakdown.getOrDefault("Total", (double)1.0F);
            String var11 = this.formatMultiplier(total);
            sender.sendMessage("§e§lTotal: §f" + var11 + "x");
            long boosterTimeLeft = this.plugin.getMultiplierManager().getBoosterTimeLeft(player.getUniqueId());
            if (boosterTimeLeft > 0L) {
                var11 = this.formatTime(boosterTimeLeft);
                sender.sendMessage("§eBooster Time Left: §f" + var11);
            }

            return true;
        } else {
            return true;
        }
    }

    private String formatMultiplier(double multiplier) {
        return String.format("%.2f", multiplier);
    }

    private String formatTime(long seconds) {
        if (seconds < 60L) {
            return seconds + "s";
        } else {
            return seconds < 3600L ? seconds / 60L + "m " + seconds % 60L + "s" : seconds / 3600L + "h " + seconds % 3600L / 60L + "m";
        }
    }
}
