package com.ninja.ghastutils.gui;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.gui.builder.GUIBuilder;
import com.ninja.ghastutils.utils.LogManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class GUIManager implements Listener {
    private final GhastUtils plugin;
    private final Map<UUID, GUISession> activeSessions;
    private final Map<UUID, Long> lastInteraction;
    private final Set<UUID> processingInventory;
    private final Map<UUID, SellGui> sellGuis;
    private final Map<UUID, CraftingEditorGui> craftingEditorGuis;
    private final Map<UUID, CraftingViewGui> craftingViewGuis;
    private final Map<UUID, CompactorGui> compactorGuis;
    private long sessionTimeout = 600000L;
    private int cleanupTaskId = -1;

    public GUIManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<UUID, GUISession>();
        this.lastInteraction = new ConcurrentHashMap<UUID, Long>();
        this.processingInventory = Collections.synchronizedSet(new HashSet<UUID>());
        this.sellGuis = new ConcurrentHashMap<UUID, SellGui>();
        this.craftingEditorGuis = new ConcurrentHashMap<UUID, CraftingEditorGui>();
        this.craftingViewGuis = new ConcurrentHashMap<UUID, CraftingViewGui>();
        this.compactorGuis = new ConcurrentHashMap<UUID, CompactorGui>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.startCleanupTask();
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            if (!this.processingInventory.add(playerId)) {
                event.setCancelled(true);
            } else {
                try {
                    if (this.sellGuis.containsKey(playerId)) {
                        SellGui sellGui = this.sellGuis.get(playerId);
                        if (event.getInventory().equals(sellGui.inventory)) {
                            sellGui.handleClick(event);
                            return;
                        }
                    }

                    if (this.craftingEditorGuis.containsKey(playerId)) {
                        CraftingEditorGui editorGui = this.craftingEditorGuis.get(playerId);
                    } else if (this.craftingViewGuis.containsKey(playerId)) {
                        CraftingViewGui viewGui = this.craftingViewGuis.get(playerId);
                    } else {
                        GUISession session = this.getSession(player);
                        if (session != null) {
                            this.lastInteraction.put(playerId, System.currentTimeMillis());
                            if (!this.isValidSlot(event.getRawSlot(), session.getInventory())) {
                                event.setCancelled(true);
                            } else {
                                session.handleClick(event);
                            }
                        }
                    }
                } finally {
                    this.processingInventory.remove(playerId);
                }
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            if (!this.processingInventory.add(playerId)) {
                event.setCancelled(true);
            } else {
                try {
                    if (this.sellGuis.containsKey(playerId)) {
                        SellGui sellGui = this.sellGuis.get(playerId);
                        if (event.getInventory().equals(sellGui.inventory)) {
                            boolean affectsTopInventory = event.getRawSlots().stream().anyMatch((slot) -> slot < event.getView().getTopInventory().getSize());
                            if (affectsTopInventory) {
                                Stream<Integer> var10000 = event.getRawSlots().stream().filter((slot) -> slot < event.getView().getTopInventory().getSize());
                                Objects.requireNonNull(sellGui);
                                boolean allowDrag = var10000.allMatch(sellGui::isInSellSlot);
                                if (!allowDrag) {
                                    event.setCancelled(true);
                                    return;
                                }
                            }

                            return;
                        }
                    }

                    GUISession session = this.getSession(player);
                    if (session != null) {
                        this.lastInteraction.put(playerId, System.currentTimeMillis());

                        for(int slot : event.getRawSlots()) {
                            if (!this.isValidSlot(slot, session.getInventory())) {
                                event.setCancelled(true);
                                return;
                            }
                        }

                        session.handleDrag(event);
                    }
                } finally {
                    this.processingInventory.remove(playerId);
                }
            }
        }
    }

    private boolean isValidSlot(int slot, Inventory inventory) {
        return slot >= 0 && slot < inventory.getSize();
    }

    private void startCleanupTask() {
        if (this.cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.cleanupTaskId);
        }

        this.cleanupTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            long now = System.currentTimeMillis();
            int cleaned = 0;
            Iterator<Map.Entry<UUID, GUISession>> it = this.activeSessions.entrySet().iterator();

            while(it.hasNext()) {
                Map.Entry<UUID, GUISession> entry = it.next();
                UUID playerId = entry.getKey();
                if (now - this.lastInteraction.getOrDefault(playerId, 0L) > this.sessionTimeout) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.closeInventory();
                    }

                    it.remove();
                    this.lastInteraction.remove(playerId);
                    this.processingInventory.remove(playerId);
                    ++cleaned;
                }
            }

            if (cleaned > 0) {
                LogManager.debug("Cleaned up " + cleaned + " stale GUI sessions");
            }

        }, 1200L, 1200L).getTaskId();
    }

    public GUIBuilder builder(String title, int rows) {
        return new GUIBuilder(title, rows);
    }

    public void registerSession(Player player, GUISession session) {
        UUID playerId = player.getUniqueId();
        GUISession existingSession = this.activeSessions.get(playerId);
        if (existingSession != null) {
            LogManager.debug("Player " + player.getName() + " already has a GUI session, closing it");
            player.closeInventory();
        }

        this.activeSessions.put(playerId, session);
        this.lastInteraction.put(playerId, System.currentTimeMillis());
        String var10000 = player.getName();
        LogManager.debug("Registered GUI session for " + var10000 + ": " + session.getTitle());
    }

    public void registerSellGui(UUID playerId, SellGui gui) {
        this.sellGuis.put(playerId, gui);
    }

    public void registerCraftingEditorGui(UUID playerId, CraftingEditorGui gui) {
        this.craftingEditorGuis.put(playerId, gui);
    }

    public void registerCraftingViewGui(UUID playerId, CraftingViewGui gui) {
        this.craftingViewGuis.put(playerId, gui);
    }

    public void registerCompactorGui(UUID playerId, CompactorGui gui) {
        this.compactorGuis.put(playerId, gui);
    }

    public SellGui getSellGui(UUID playerId) {
        return this.sellGuis.get(playerId);
    }

    public CraftingEditorGui getCraftingEditorGui(UUID playerId) {
        return this.craftingEditorGuis.get(playerId);
    }

    public CraftingViewGui getCraftingViewGui(UUID playerId) {
        return this.craftingViewGuis.get(playerId);
    }

    public CompactorGui getCompactorGui(UUID playerId) {
        return this.compactorGuis.get(playerId);
    }

    public GUISession getSession(Player player) {
        return this.activeSessions.get(player.getUniqueId());
    }

    public void removeSession(Player player) {
        UUID playerId = player.getUniqueId();
        this.activeSessions.remove(playerId);
        this.lastInteraction.remove(playerId);
        this.processingInventory.remove(playerId);
    }

    public int getSessionCount() {
        return this.activeSessions.size();
    }

    public void setSessionTimeout(long timeoutMillis) {
        this.sessionTimeout = timeoutMillis;
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            UUID playerId = player.getUniqueId();
            if (this.sellGuis.containsKey(playerId)) {
                SellGui sellGui = this.sellGuis.get(playerId);
                if (event.getInventory().equals(sellGui.inventory)) {
                    sellGui.onClose();
                    this.sellGuis.remove(playerId);
                }
            }

            if (this.craftingEditorGuis.containsKey(playerId)) {
                CraftingEditorGui editorGui = this.craftingEditorGuis.get(playerId);
                this.craftingEditorGuis.remove(playerId);
            }

            if (this.craftingViewGuis.containsKey(playerId)) {
                this.craftingViewGuis.remove(playerId);
            }

            if (this.compactorGuis.containsKey(playerId)) {
                CompactorGui compactorGui = this.compactorGuis.remove(playerId);
                if (compactorGui != null) {
                    compactorGui.onClose();
                }
            }

            GUISession session = this.getSession(player);
            if (session != null) {
                session.handleClose(event);
                if (session.isRemoveOnClose()) {
                    this.removeSession(player);
                    LogManager.debug("Removed GUI session for " + player.getName() + " on inventory close");
                }
            }

        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        this.sellGuis.remove(playerId);
        this.craftingEditorGuis.remove(playerId);
        this.craftingViewGuis.remove(playerId);
        this.compactorGuis.remove(playerId);
        CompactorGui.cleanupSession(playerId);
        GUISession session = this.getSession(player);
        if (session != null) {
            session.handleQuit(event);
            this.removeSession(player);
            LogManager.debug("Removed GUI session for " + player.getName() + " on player quit");
        }

    }
}