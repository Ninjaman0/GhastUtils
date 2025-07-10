
package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.blocks.BlockManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BlockInteractionListener implements Listener {
    private final GhastUtils plugin;
    private final BlockManager blockManager;

    public BlockInteractionListener(GhastUtils plugin) {
        this.plugin = plugin;
        this.blockManager = plugin.getBlockManager();
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block != null) {
            if (this.blockManager.isCustomBlock(block.getLocation())) {
                event.setCancelled(true);
                String action = this.getActionString(event.getAction(), player.isSneaking());
                this.blockManager.executeBlockCommands(player, block.getLocation(), action);
            } else {
                ItemStack item = event.getItem();
                if (item != null) {
                    String action = this.getActionString(event.getAction(), player.isSneaking());
                    this.plugin.getCraftingManager().executeItemEffects(player, item, action);
                }

            }
        }
    }

    private String getActionString(Action action, boolean sneaking) {
        String baseAction = action.name().split("_")[0];
        String actionType = baseAction + "_CLICK";
        return sneaking ? "SHIFT_" + actionType : actionType;
    }
}
