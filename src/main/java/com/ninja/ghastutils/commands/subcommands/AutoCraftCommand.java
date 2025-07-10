package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.crafting.CraftingManager;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoCraftCommand extends SubCommand {
    private final GhastUtils plugin;
    private final Map<UUID, BukkitTask> autoCraftTasks = new HashMap<>();

    public AutoCraftCommand(GhastUtils plugin) {
        super("autocraft", "ghastutils.crafting.autocraft", true);
        this.plugin = plugin;
        this.addAlias("acraft");
    }

    @Override
    public String getDescription() {
        return "Toggle automatic crafting of items every 3 seconds";
    }

    @Override
    public String getUsage() {
        return "/gutil autocraft";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!checkPlayer(sender) || !checkPermission(sender)) {
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (autoCraftTasks.containsKey(playerId)) {
            // Disable auto-crafting
            BukkitTask task = autoCraftTasks.remove(playerId);
            task.cancel();
            
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "crafting.autocraft-disabled", placeholders);
        } else {
            // Enable auto-crafting
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    cancelAutoCraft(playerId);
                    return;
                }
                
                performAutoCraft(player);
            }, 0L, 60L); // Every 3 seconds (60 ticks)
            
            autoCraftTasks.put(playerId, task);
            
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "crafting.autocraft-enabled", placeholders);
        }

        return true;
    }

    private void performAutoCraft(Player player) {
        CraftingManager craftingManager = plugin.getCraftingManager();
        Map<String, CustomItem> customItems = craftingManager.getCustomItems();
        
        boolean craftedSomething = false;
        
        for (Map.Entry<String, CustomItem> entry : customItems.entrySet()) {
            String itemId = entry.getKey();
            CustomItem customItem = entry.getValue();
            
            // Check if player has permission to craft this item
            if (customItem.getPermission() != null && !player.hasPermission(customItem.getPermission())) {
                continue;
            }
            
            // Check if recipe exists and player has materials
            if (!customItem.getRecipe().isEmpty() && craftingManager.hasRequiredMaterials(player, itemId)) {
                // Try to craft the item
                if (craftingManager.craftItem(player, itemId)) {
                    craftedSomething = true;
                    // Only craft one item per cycle to prevent spam
                    break;
                }
            }
        }
    }

    private void cancelAutoCraft(UUID playerId) {
        BukkitTask task = autoCraftTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void onDisable() {
        autoCraftTasks.values().forEach(BukkitTask::cancel);
        autoCraftTasks.clear();
    }
}