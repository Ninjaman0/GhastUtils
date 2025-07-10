
package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.utils.NBTUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArmorPlaceListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (NBTUtils.getBoolean(meta, "ghastutils.unplaceable")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cThis head cannot be placed!");
                }

            }
        }
    }
}
