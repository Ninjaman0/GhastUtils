
package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.GhastUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MultiplierListener implements Listener {
    private final GhastUtils plugin;

    public MultiplierListener(GhastUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getMultiplierManager().calculateArmorMultiplier(player), 5L);
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            this.plugin.getMultiplierManager().calculateArmorMultiplier(player);
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if (this.isArmorItem(event.getItem().getItemStack().getType().name())) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getMultiplierManager().calculateArmorMultiplier(player), 1L);
            }
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        if (this.isArmorItem(event.getBrokenItem().getType().name())) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getMultiplierManager().calculateArmorMultiplier(player), 1L);
        }

    }

    private boolean isArmorItem(String itemName) {
        return itemName.endsWith("_HELMET") || itemName.endsWith("_CHESTPLATE") || itemName.endsWith("_LEGGINGS") || itemName.endsWith("_BOOTS") || itemName.equals("PLAYER_HEAD") || itemName.equals("SKULL_ITEM");
    }
}
