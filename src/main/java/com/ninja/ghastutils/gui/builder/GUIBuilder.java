
package com.ninja.ghastutils.gui.builder;

import com.ninja.ghastutils.gui.GUISession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUIBuilder {
    private final String title;
    private final int rows;
    private final Map<Integer, ItemStack> items = new HashMap();
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap();
    private Consumer<InventoryCloseEvent> closeHandler;
    private Consumer<PlayerQuitEvent> quitHandler;
    private Consumer<InventoryDragEvent> dragHandler;
    private boolean preventClick = true;
    private boolean removeOnClose = true;

    public GUIBuilder(String title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
    }

    public GUIBuilder item(int slot, ItemStack item) {
        this.items.put(slot, item);
        return this;
    }

    public GUIBuilder item(int slot, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        this.items.put(slot, item);
        return this;
    }

    public GUIBuilder item(int slot, Material material, String name, String... lore) {
        return this.item(slot, material, name, Arrays.asList(lore));
    }

    public GUIBuilder onClick(int slot, Consumer<InventoryClickEvent> handler) {
        this.clickHandlers.put(slot, handler);
        return this;
    }

    public GUIBuilder onClose(Consumer<InventoryCloseEvent> handler) {
        this.closeHandler = handler;
        return this;
    }

    public GUIBuilder onQuit(Consumer<PlayerQuitEvent> handler) {
        this.quitHandler = handler;
        return this;
    }

    public GUIBuilder onDrag(Consumer<InventoryDragEvent> handler) {
        this.dragHandler = handler;
        return this;
    }

    public GUIBuilder preventClick(boolean prevent) {
        this.preventClick = prevent;
        return this;
    }

    public GUIBuilder removeOnClose(boolean remove) {
        this.removeOnClose = remove;
        return this;
    }

    public GUIBuilder fillBorder(Material material) {
        ItemStack item = new ItemStack(material);

        for(int i = 0; i < 9; ++i) {
            this.items.put(i, item);
            this.items.put(9 * (this.rows - 1) + i, item);
        }

        for(int i = 1; i < this.rows - 1; ++i) {
            this.items.put(9 * i, item);
            this.items.put(9 * i + 8, item);
        }

        return this;
    }

    public GUIBuilder fill(Material material) {
        ItemStack item = new ItemStack(material);

        for(int i = 0; i < 9 * this.rows; ++i) {
            this.items.put(i, item);
        }

        return this;
    }

    public GUISession build(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 9 * this.rows, this.title);

        for(Map.Entry<Integer, ItemStack> entry : this.items.entrySet()) {
            inventory.setItem((Integer)entry.getKey(), (ItemStack)entry.getValue());
        }

        return new GUISession(player, inventory, this.title, this.clickHandlers, this.closeHandler, this.quitHandler, this.dragHandler, this.preventClick, this.removeOnClose);
    }

    public Inventory buildInventory() {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 9 * this.rows, this.title);

        for(Map.Entry<Integer, ItemStack> entry : this.items.entrySet()) {
            inventory.setItem((Integer)entry.getKey(), (ItemStack)entry.getValue());
        }

        return inventory;
    }
}
