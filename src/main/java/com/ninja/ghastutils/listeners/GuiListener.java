package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.GhastUtils;
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
    private final Map<UUID, SellGui> activeSellGuis = new HashMap<UUID, SellGui>();
    private final Map<UUID, CraftingEditorGui> activeCraftingEditorGuis = new HashMap<UUID, CraftingEditorGui>();
    private final Map<UUID, CraftingViewGui> activeCraftingViewGuis = new HashMap<UUID, CraftingViewGui>();

    public GuiListener(GhastUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            String title = event.getView().getTitle();
            Inventory topInventory = event.getView().getTopInventory();
            int slot = event.getRawSlot();
            if (!title.startsWith("Sell Items") && !title.equals("§6Sell Items")) {
                if (title.startsWith("Recipe Editor:")) {
                    CraftingEditorGui editorGui = this.activeCraftingEditorGuis.get(playerId);
                    if (editorGui != null) {
                        this.handleCraftingEditorGuiClick(event, player, editorGui);
                    }

                } else {
                    if (title.startsWith("Recipe:")) {
                        CraftingViewGui viewGui = this.activeCraftingViewGuis.get(playerId);
                        if (viewGui != null) {
                            this.handleCraftingViewGuiClick(event, player, viewGui);
                        }
                    }

                }
            } else {
                SellGui sellGui = this.activeSellGuis.get(playerId);
                if (sellGui != null) {
                    this.handleSellGuiClick(event, player, sellGui);
                }

            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            String title = event.getView().getTitle();
            int topInventorySize = event.getView().getTopInventory().getSize();
            boolean affectsTopInventory = event.getRawSlots().stream().anyMatch((slot) -> slot < topInventorySize);
            if (affectsTopInventory) {
                if (!title.startsWith("Sell Items") && !title.equals("§6Sell Items")) {
                    if (title.startsWith("Recipe Editor:")) {
                        CraftingEditorGui gui = this.activeCraftingEditorGuis.get(playerId);
                        if (gui != null) {
                            Stream<Integer> filteredSlots = event.getRawSlots().stream().filter((slot) -> slot < topInventorySize);
                            Objects.requireNonNull(gui);
                            boolean allowDrag = filteredSlots.allMatch(gui::isRecipeSlot);
                            if (!allowDrag) {
                                event.setCancelled(true);
                            }
                        } else {
                            event.setCancelled(true);
                        }
                    } else if (title.startsWith("Recipe:")) {
                        event.setCancelled(true);
                    }
                } else {
                    SellGui gui = this.activeSellGuis.get(playerId);
                    if (gui != null) {
                        Stream<Integer> filteredSlots = event.getRawSlots().stream().filter((slot) -> slot < topInventorySize);
                        Objects.requireNonNull(gui);
                        boolean allowDrag = filteredSlots.allMatch(gui::isInSellSlot);
                        if (!allowDrag) {
                            event.setCancelled(true);
                        } else {
                            BukkitScheduler scheduler = this.plugin.getServer().getScheduler();
                            GhastUtils pluginRef = this.plugin;
                            Objects.requireNonNull(gui);
                            scheduler.runTask(pluginRef, gui::calculateTotalValue);
                        }
                    } else {
                        event.setCancelled(true);
                    }
                }

            }
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
                BukkitScheduler scheduler = this.plugin.getServer().getScheduler();
                GhastUtils pluginRef = this.plugin;
                Objects.requireNonNull(gui);
                scheduler.runTask(pluginRef, gui::calculateTotalValue);
            }
        } else if (event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                int firstAvailableSellSlot = -1;

                for(int sellSlot : gui.getSellSlots()) {
                    ItemStack item = topInventory.getItem(sellSlot);
                    if (item == null || item.getType().isAir()) {
                        firstAvailableSellSlot = sellSlot;
                        break;
                    }
                }

                if (firstAvailableSellSlot == -1) {
                    event.setCancelled(true);
                } else {
                    BukkitScheduler scheduler = this.plugin.getServer().getScheduler();
                    GhastUtils pluginRef = this.plugin;
                    Objects.requireNonNull(gui);
                    scheduler.runTask(pluginRef, gui::calculateTotalValue);
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
            int[] recipeSlots = new int[]{11, 12, 13, 20, 21, 22, 29, 30, 31};

            for(int recipeSlot : recipeSlots) {
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

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            UUID playerId = player.getUniqueId();
            String title = event.getView().getTitle();
            if (!title.startsWith("Sell Items") && !title.equals("§6Sell Items")) {
                if (title.startsWith("Recipe Editor:")) {
                    CraftingEditorGui gui = this.activeCraftingEditorGuis.get(playerId);
                    if (gui != null) {
                        gui.onClose();
                        this.activeCraftingEditorGuis.remove(playerId);
                    }
                } else if (title.startsWith("Recipe:")) {
                    this.activeCraftingViewGuis.remove(playerId);
                }
            } else {
                SellGui gui = this.activeSellGuis.get(playerId);
                if (gui != null) {
                    gui.onClose();
                    this.activeSellGuis.remove(playerId);
                }
            }

        }
    }

    public void registerSellGui(UUID uuid, SellGui gui) {
        this.activeSellGuis.put(uuid, gui);
    }

    public void registerCraftingEditorGui(UUID uuid, CraftingEditorGui gui) {
        this.activeCraftingEditorGuis.put(uuid, gui);
    }

    public void registerCraftingViewGui(UUID uuid, CraftingViewGui gui) {
        this.activeCraftingViewGuis.put(uuid, gui);
    }

    public SellGui getSellGui(UUID uuid) {
        return this.activeSellGuis.get(uuid);
    }

    public CraftingEditorGui getCraftingEditorGui(UUID uuid) {
        return this.activeCraftingEditorGuis.get(uuid);
    }

    public CraftingViewGui getCraftingViewGui(UUID uuid) {
        return this.activeCraftingViewGuis.get(uuid);
    }
}