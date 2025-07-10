
package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.utils.NBTUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

public class VanillaFeatureListener implements Listener {
    private final GhastUtils plugin;

    public VanillaFeatureListener(GhastUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (this.isVanillaDisabled(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage("§cThis item cannot be consumed.");
        }

    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            player.setMetadata("lastInteractEvent", new FixedMetadataValue(this.plugin, event));
            ItemStack item = event.getItem();
            if (this.isVanillaDisabled(item)) {
                event.setUseItemInHand(Result.DENY);
                if (this.isThrowableItem(item.getType())) {
                    event.setCancelled(true);
                }
            }
        }

    }

    private boolean isVanillaDisabled(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            } else if (NBTUtils.getBoolean(meta, "ghastutils.no_vanilla")) {
                return true;
            } else {
                String customItemId = NBTUtils.getString(meta, "ghastutils.custom_item_id");
                if (customItemId == null) {
                    return false;
                } else {
                    CustomItem customItem = this.plugin.getCraftingManager().getCustomItem(customItemId);
                    return customItem != null && customItem.isVanillaFeaturesDisabled();
                }
            }
        } else {
            return false;
        }
    }

    private boolean isThrowableItem(Material type) {
        String name = type.name();
        return name.contains("POTION") || name.equals("ENDER_PEARL") || name.equals("EGG") || name.equals("SNOWBALL") || name.equals("SPLASH_POTION") || name.equals("LINGERING_POTION") || name.equals("EXPERIENCE_BOTTLE") || name.equals("TRIDENT");
    }
}
