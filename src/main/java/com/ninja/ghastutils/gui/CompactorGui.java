package com.ninja.ghastutils.gui;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CompactorGui {
    private final GhastUtils plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, String> selectedItems; // slot -> itemId
    private static final Map<UUID, CompactorSession> activeSessions = new ConcurrentHashMap<>();
    private static final int[] SELECTION_SLOTS = {10, 12, 14, 16, 22}; // 5 slots for selection

    public CompactorGui(GhastUtils plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.selectedItems = new HashMap<>();
        
        Map<String, String> placeholders = MessageUtils.placeholders();
        String title = MessageUtils.getMessage("compactor.gui-title", placeholders);
        if (title == null || title.isEmpty()) {
            title = "§6Compactor";
        }
        
        this.inventory = Bukkit.createInventory((InventoryHolder) null, 45, title);
        plugin.getGuiManager().registerCompactorGui(player.getUniqueId(), this);
        
        initializeGui();
    }

    private void initializeGui() {
        // Fill border with glass panes
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i > 35 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
        }

        // Set selection slots
        for (int slot : SELECTION_SLOTS) {
            inventory.setItem(slot, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, 
                "§7Click to select an item", "§7Hold the item you want to select", "§7and click this slot"));
        }

        // Add control buttons
        inventory.setItem(40, createGuiItem(Material.EMERALD, "§a§lStart Compacting", 
            "§7Click to start auto-crafting", "§7selected items"));
        inventory.setItem(38, createGuiItem(Material.BARRIER, "§c§lStop Compacting", 
            "§7Click to stop auto-crafting"));
        inventory.setItem(39, createGuiItem(Material.PAPER, "§e§lInfo", 
            "§7Select up to 5 items to auto-craft", "§7The compactor will automatically craft", 
            "§7these items when you have materials", "§7Hold an item and click a selection slot"));

        updateDisplay();
    }

    private void updateDisplay() {
        CompactorSession session = activeSessions.get(player.getUniqueId());
        
        for (int i = 0; i < SELECTION_SLOTS.length; i++) {
            int slot = SELECTION_SLOTS[i];
            String selectedItemId = selectedItems.get(slot);
            
            if (selectedItemId != null) {
                CustomItem customItem = plugin.getCraftingManager().getCustomItem(selectedItemId);
                if (customItem != null) {
                    ItemStack displayItem = plugin.getCraftingManager().createItemStack(selectedItemId, 1);
                    if (displayItem != null) {
                        ItemMeta meta = displayItem.getItemMeta();
                        if (meta != null) {
                            List<String> lore = meta.getLore();
                            if (lore == null) lore = new ArrayList<>();
                            lore.add("");
                            lore.add("§7Status: " + (session != null && session.isItemActive(selectedItemId) ? 
                                "§aActive" : "§cInactive"));
                            lore.add("§7Click to remove from compactor");
                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);
                        }
                        inventory.setItem(slot, displayItem);
                    }
                }
            } else {
                inventory.setItem(slot, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, 
                    "§7Click to select an item", "§7Hold the item you want to select", "§7and click this slot"));
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(int slot, ItemStack clickedItem) {
        if (Arrays.stream(SELECTION_SLOTS).anyMatch(s -> s == slot)) {
            handleSelectionSlotClick(slot, clickedItem);
        }
    }

    public void handleStartClick() {
        if (selectedItems.isEmpty()) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "compactor.no-items-selected", placeholders);
            return;
        }

        CompactorSession session = activeSessions.computeIfAbsent(player.getUniqueId(), 
            k -> new CompactorSession(plugin, player));
        
        for (String itemId : selectedItems.values()) {
            session.addItem(itemId);
        }
        
        session.start();
        updateDisplay();
        
        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("count", String.valueOf(selectedItems.size()));
        MessageUtils.sendMessage(player, "compactor.started", placeholders);
    }

    public void handleStopClick() {
        CompactorSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
            updateDisplay();
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "compactor.stopped", placeholders);
        } else {
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "compactor.not-running", placeholders);
        }
    }

    private void handleSelectionSlotClick(int slot, ItemStack clickedItem) {
        if (selectedItems.containsKey(slot)) {
            // Remove selected item
            selectedItems.remove(slot);
            updateDisplay();
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "compactor.item-removed", placeholders);
        } else if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            // Try to select item from player's hand
            String itemId = plugin.getCraftingManager().getCustomItemIdFromItemStack(clickedItem);
            if (itemId != null) {
                CustomItem customItem = plugin.getCraftingManager().getCustomItem(itemId);
                if (customItem != null && !customItem.getRecipe().isEmpty()) {
                    selectedItems.put(slot, itemId);
                    updateDisplay();
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("item", customItem.getName());
                    MessageUtils.sendMessage(player, "compactor.item-selected", placeholders);
                } else {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    MessageUtils.sendMessage(player, "compactor.no-recipe", placeholders);
                }
            } else {
                Map<String, String> placeholders = MessageUtils.placeholders();
                MessageUtils.sendMessage(player, "compactor.not-custom-item", placeholders);
            }
        } else {
            // No item in hand, show instruction
            player.sendMessage("§cHold an item in your hand and click to select it!");
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void onClose() {
        // Keep session running even when GUI is closed
    }

    public static void cleanupSession(UUID playerId) {
        CompactorSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.stop();
        }
    }

    public static class CompactorSession {
        private final GhastUtils plugin;
        private final Player player;
        private final Set<String> activeItems;
        private BukkitTask task;

        public CompactorSession(GhastUtils plugin, Player player) {
            this.plugin = plugin;
            this.player = player;
            this.activeItems = new HashSet<>();
        }

        public void addItem(String itemId) {
            activeItems.add(itemId);
        }

        public void removeItem(String itemId) {
            activeItems.remove(itemId);
        }

        public boolean isItemActive(String itemId) {
            return activeItems.contains(itemId);
        }

        public void start() {
            if (task != null) {
                task.cancel();
            }

            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    stop();
                    return;
                }

                for (String itemId : new HashSet<>(activeItems)) {
                    CustomItem customItem = plugin.getCraftingManager().getCustomItem(itemId);
                    if (customItem != null && 
                        plugin.getCraftingManager().hasRequiredMaterials(player, itemId) &&
                        (customItem.getPermission() == null || player.hasPermission(customItem.getPermission()))) {
                        
                        plugin.getCraftingManager().craftItem(player, itemId);
                        // Only craft one item per cycle to prevent lag
                        break;
                    }
                }
            }, 20L, 20L); // Every second
        }

        public void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            activeItems.clear();
        }
    }
}