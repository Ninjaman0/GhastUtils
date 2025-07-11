package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.gui.CompactorGui;
import com.ninja.ghastutils.gui.CraftingEditorGui;
import com.ninja.ghastutils.gui.CraftingViewGui;
import com.ninja.ghastutils.gui.SellGui;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

public class GuiListener implements Listener {
    private final GhastUtils plugin;
    private final Map<UUID, SellGui> activeSellGuis = new HashMap<>();
    private final Map<UUID, CraftingEditorGui> activeCraftingEditorGuis = new HashMap<>();
    private final Map<UUID, CraftingViewGui> activeCraftingViewGuis = new HashMap<>();
    private final Map<UUID, CompactorGui> activeCompactorGuis = new HashMap<>();

    public GuiListener(GhastUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        String title = event.getView().getTitle();
        Inventory topInventory = event.getView().getTopInventory();
        int slot = event.getRawSlot();

        // Handle Sell GUI
        if (title.startsWith("Sell Items") || title.equals("§6Sell Items")) {
            SellGui sellGui = activeSellGuis.get(playerId);
            if (sellGui != null) {
                handleSellGuiClick(event, player, sellGui);
            }
        }
        // Handle Crafting Editor GUI
        else if (title.startsWith("Recipe Editor:")) {
            CraftingEditorGui editorGui = activeCraftingEditorGuis.get(playerId);
            if (editorGui != null) {
                handleCraftingEditorGuiClick(event, player, editorGui);
            }
        }
        // Handle Crafting View GUI
        else if (title.startsWith("Recipe:")) {
            CraftingViewGui viewGui = activeCraftingViewGuis.get(playerId);
            if (viewGui != null) {
                handleCraftingViewGuiClick(event, player, viewGui);
            }
        }
        // Handle Compactor GUI
        else if (title.equals("§6Compactor")) {
            CompactorGui compactorGui = activeCompactorGuis.get(playerId);
            if (compactorGui != null) {
                handleCompactorGuiClick(event, player, compactorGui);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        String title = event.getView().getTitle();
        int topInventorySize = event.getView().getTopInventory().getSize();
        boolean affectsTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < topInventorySize);

        if (!affectsTopInventory) {
            return;
        }

        // Handle Sell GUI drag
        if (title.startsWith("Sell Items") || title.equals("§6Sell Items")) {
            SellGui gui = activeSellGuis.get(playerId);
            if (gui != null) {
                Stream<Integer> filteredSlots = event.getRawSlots().stream().filter(slot -> slot < topInventorySize);
                boolean allowDrag = filteredSlots.allMatch(gui::isInSellSlot);
                if (!allowDrag) {
                    event.setCancelled(true);
                } else {
                    BukkitScheduler scheduler = plugin.getServer().getScheduler();
                    scheduler.runTask(plugin, gui::calculateTotalValue);
                }
            } else {
                event.setCancelled(true);
            }
        }
        // Handle Crafting Editor GUI drag
        else if (title.startsWith("Recipe Editor:")) {
            CraftingEditorGui gui = activeCraftingEditorGuis.get(playerId);
            if (gui != null) {
                Stream<Integer> filteredSlots = event.getRawSlots().stream().filter(slot -> slot < topInventorySize);
                boolean allowDrag = filteredSlots.allMatch(gui::isRecipeSlot);
                if (!allowDrag) {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
        // Handle other GUIs - cancel drag by default
        else if (title.startsWith("Recipe:") || title.equals("§6Compactor")) {
            event.setCancelled(true);
        }
    }

    private void handleSellGuiClick(InventoryClickEvent event, Player player, SellGui gui) {
        int slot = event.getRawSlot();
        Inventory topInventory = event.getView().getTopInventory();

        if (slot < topInventory.getSize()) {
            if (!gui.isInSellSlot(slot)) {
                event.setCancelled(true);
                if (slot == 49) {
                    gui.handleSellClick();
                } else if (slot == 45) {
                    gui.handleCancelClick();
                }
            } else {
                BukkitScheduler scheduler = plugin.getServer().getScheduler();
                scheduler.runTask(plugin, gui::calculateTotalValue);
            }
        } else if (event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                int firstAvailableSellSlot = -1;

                for (int sellSlot : gui.getSellSlots()) {
                    ItemStack item = topInventory.getItem(sellSlot);
                    if (item == null || item.getType().isAir()) {
                        firstAvailableSellSlot = sellSlot;
                        break;
                    }
                }

                if (firstAvailableSellSlot == -1) {
                    event.setCancelled(true);
                } else {
                    BukkitScheduler scheduler = plugin.getServer().getScheduler();
                    scheduler.runTask(plugin, gui::calculateTotalValue);
                }
            }
        }
    }

    private void handleCraftingEditorGuiClick(InventoryClickEvent event, Player player, CraftingEditorGui gui) {
        int slot = event.getRawSlot();
        Inventory topInventory = event.getView().getTopInventory();

        if (slot < topInventory.getSize()) {
            if (!gui.isRecipeSlot(slot)) {
                event.setCancelled(true);
                if (slot == 49) {
                    gui.handleSaveClick();
                } else if (slot == 48) {
                    gui.handleClearClick();
                }
            }
        } else if (event.isShiftClick() && event.getCurrentItem() != null) {
            boolean foundEmptySlot = false;
            int[] recipeSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};

            for (int recipeSlot : recipeSlots) {
                ItemStack currentItem = topInventory.getItem(recipeSlot);
                if (currentItem == null || currentItem.getType() == Material.AIR) {
                    foundEmptySlot = true;
                    break;
                }
            }

            if (!foundEmptySlot) {
                event.setCancelled(true);
            }
        }
    }

    private void handleCraftingViewGuiClick(InventoryClickEvent event, Player player, CraftingViewGui gui) {
        event.setCancelled(true);
        if (event.getRawSlot() == 49 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ANVIL) {
            gui.handleCraftClick();
        }
    }

    private void handleCompactorGuiClick(InventoryClickEvent event, Player player, CompactorGui gui) {
        int slot = event.getRawSlot();
        
        // Cancel all clicks in the top inventory except for specific handling
        if (slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            
            // Handle the click with the cursor item for selection slots
            ItemStack cursorItem = event.getCursor();
            gui.handleClick(slot, cursorItem);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        String title = event.getView().getTitle();

        if (title.startsWith("Sell Items") || title.equals("§6Sell Items")) {
            SellGui gui = activeSellGuis.get(playerId);
            if (gui != null) {
                gui.onClose();
                activeSellGuis.remove(playerId);
            }
        } else if (title.startsWith("Recipe Editor:")) {
            CraftingEditorGui gui = activeCraftingEditorGuis.get(playerId);
            if (gui != null) {
                gui.onClose();
                activeCraftingEditorGuis.remove(playerId);
            }
        } else if (title.startsWith("Recipe:")) {
            activeCraftingViewGuis.remove(playerId);
        } else if (title.equals("§6Compactor")) {
            CompactorGui gui = activeCompactorGuis.get(playerId);
            if (gui != null) {
                gui.onClose();
                activeCompactorGuis.remove(playerId);
            }
        }
    }

    public void registerSellGui(UUID uuid, SellGui gui) {
        activeSellGuis.put(uuid, gui);
    }

    public void registerCraftingEditorGui(UUID uuid, CraftingEditorGui gui) {
        activeCraftingEditorGuis.put(uuid, gui);
    }

    public void registerCraftingViewGui(UUID uuid, CraftingViewGui gui) {
        activeCraftingViewGuis.put(uuid, gui);
    }

    public void registerCompactorGui(UUID uuid, CompactorGui gui) {
        activeCompactorGuis.put(uuid, gui);
    }

    public SellGui getSellGui(UUID uuid) {
        return activeSellGuis.get(uuid);
    }

    public CraftingEditorGui getCraftingEditorGui(UUID uuid) {
        return activeCraftingEditorGuis.get(uuid);
    }

    public CraftingViewGui getCraftingViewGui(UUID uuid) {
        return activeCraftingViewGuis.get(uuid);
    }

    public CompactorGui getCompactorGui(UUID uuid) {
        return activeCompactorGuis.get(uuid);
    }
}