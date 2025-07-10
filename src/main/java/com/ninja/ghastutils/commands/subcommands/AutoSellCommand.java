
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class AutoSellCommand extends SubCommand {
    private final GhastUtils plugin;
    private final Map<UUID, BukkitTask> autoSellTasks = new HashMap();

    public AutoSellCommand(GhastUtils plugin) {
        super("autosell", "ghastutils.sell.autosell", true);
        this.plugin = plugin;
        this.addAlias("asell");
    }

    public String getDescription() {
        return "Toggle automatic selling of items every 3 seconds";
    }

    public String getUsage() {
        return "/gutil autosell";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (this.checkPlayer(sender) && this.checkPermission(sender)) {
            Player player = (Player)sender;
            UUID playerId = player.getUniqueId();
            if (this.autoSellTasks.containsKey(playerId)) {
                BukkitTask task = (BukkitTask)this.autoSellTasks.remove(playerId);
                task.cancel();
                player.sendMessage("§cAuto-selling disabled");
            } else {
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                    if (!player.isOnline()) {
                        this.cancelAutoSell(playerId);
                    } else {
                        Bukkit.dispatchCommand(player, "gutil sell");
                    }
                }, 0L, 60L);
                this.autoSellTasks.put(playerId, task);
                player.sendMessage("§aAuto-selling enabled (every 3 seconds)");
            }

            return true;
        } else {
            return true;
        }
    }

    private void cancelAutoSell(UUID playerId) {
        BukkitTask task = (BukkitTask)this.autoSellTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

    }

    public void onDisable() {
        this.autoSellTasks.values().forEach(BukkitTask::cancel);
        this.autoSellTasks.clear();
    }
}

