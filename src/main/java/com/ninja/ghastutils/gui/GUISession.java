
package com.ninja.ghastutils.gui;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class GUISession {
    private final UUID playerId;
    private final Inventory inventory;
    private final String title;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;
    private final Consumer<InventoryCloseEvent> closeHandler;
    private final Consumer<PlayerQuitEvent> quitHandler;
    private final Consumer<InventoryDragEvent> dragHandler;
    private final boolean preventClick;
    private final boolean removeOnClose;
    private long lastActivity;

    public GUISession(Player player, Inventory inventory, String title, Map<Integer, Consumer<InventoryClickEvent>> clickHandlers, Consumer<InventoryCloseEvent> closeHandler, Consumer<PlayerQuitEvent> quitHandler, Consumer<InventoryDragEvent> dragHandler, boolean preventClick, boolean removeOnClose) {
        this.playerId = player.getUniqueId();
        this.inventory = inventory;
        this.title = title;
        this.clickHandlers = clickHandlers;
        this.closeHandler = closeHandler;
        this.quitHandler = quitHandler;
        this.dragHandler = dragHandler;
        this.preventClick = preventClick;
        this.removeOnClose = removeOnClose;
        this.lastActivity = System.currentTimeMillis();
    }

    public void handleClick(InventoryClickEvent event) {
        this.updateActivity();
        if (event.getView().getTopInventory().equals(this.inventory)) {
            int slot = event.getRawSlot();
            if (this.preventClick && slot < this.inventory.getSize()) {
                event.setCancelled(true);
            }

            if (this.clickHandlers.containsKey(slot)) {
                ((Consumer)this.clickHandlers.get(slot)).accept(event);
            }

        }
    }

    public void handleDrag(InventoryDragEvent event) {
        this.updateActivity();
        if (event.getView().getTopInventory().equals(this.inventory)) {
            if (this.preventClick) {
                for(int slot : event.getRawSlots()) {
                    if (slot < this.inventory.getSize()) {
                        event.setCancelled(true);
                        break;
                    }
                }
            }

            if (this.dragHandler != null) {
                this.dragHandler.accept(event);
            }

        }
    }

    public void handleClose(InventoryCloseEvent event) {
        this.updateActivity();
        if (this.closeHandler != null) {
            this.closeHandler.accept(event);
        }

    }

    public void handleQuit(PlayerQuitEvent event) {
        if (this.quitHandler != null) {
            this.quitHandler.accept(event);
        }

    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public String getTitle() {
        return this.title;
    }

    public long getLastActivity() {
        return this.lastActivity;
    }

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    public boolean isRemoveOnClose() {
        return this.removeOnClose;
    }
}
