package com.ninja.ghastutils.events;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.multiplier.MultiplierManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class EventBossBar implements Listener {
    private final GhastUtils plugin;
    private final Map<String, BossBar> eventBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Boolean>> playerVisibility = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public EventBossBar(GhastUtils plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.startUpdateTask();
    }

    private void startUpdateTask() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }

        this.updateTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::updateBossBars, 20L, 20L);
    }

    private void updateBossBars() {
        MultiplierManager multiplierManager = this.plugin.getMultiplierManager();
        Map<String, MultiplierManager.EventMultiplier> events = multiplierManager.getEventMultipliers();

        for(String eventId : new HashMap<>(this.eventBossBars).keySet()) {
            if (!events.containsKey(eventId)) {
                BossBar bossBar = this.eventBossBars.remove(eventId);
                if (bossBar != null) {
                    bossBar.removeAll();
                }
            }
        }

        for(Map.Entry<String, MultiplierManager.EventMultiplier> entry : events.entrySet()) {
            String eventId = entry.getKey();
            MultiplierManager.EventMultiplier event = entry.getValue();
            BossBar bossBar = this.eventBossBars.computeIfAbsent(eventId, (id) -> this.createBossBar(event));
            String title = this.getEventTitle(event);
            bossBar.setTitle(title);
            if (event.getEndTime() > 0L) {
                long remaining = event.getEndTime() - System.currentTimeMillis() / 1000L;
                long duration = this.getEventDuration(event);
                if (duration > 0L && remaining > 0L) {
                    double progress = (double)remaining / (double)duration;
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                } else {
                    bossBar.setProgress(0.0);
                }
            } else {
                bossBar.setProgress(1.0);
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
                if (this.shouldSeeEventBossBar(player, eventId)) {
                    bossBar.addPlayer(player);
                }
            }
        }
    }

    private BossBar createBossBar(MultiplierManager.EventMultiplier event) {
        BarColor color = this.getBarColor(event);
        BarStyle style = this.getBarStyle(event);
        return Bukkit.createBossBar(this.getEventTitle(event), color, style, new BarFlag[0]);
    }

    private String getEventTitle(MultiplierManager.EventMultiplier event) {
        String timeRemaining = event.getEndTime() > 0L ? " §8(§7" + event.getTimeRemaining() + "§8)" : " §8(§7Permanent§8)";
        String var10000 = event.getName();
        return "§e§l" + var10000 + " §f+" + String.format("%.2f", event.getValue()) + "x" + timeRemaining;
    }

    private BarColor getBarColor(MultiplierManager.EventMultiplier event) {
        double value = event.getValue();
        if (value >= 2.0) {
            return BarColor.PURPLE;
        } else if (value >= 1.5) {
            return BarColor.RED;
        } else if (value >= 1.0) {
            return BarColor.YELLOW;
        } else {
            return value >= 0.5 ? BarColor.GREEN : BarColor.BLUE;
        }
    }

    private BarStyle getBarStyle(MultiplierManager.EventMultiplier event) {
        return event.getEndTime() > 0L ? BarStyle.SEGMENTED_10 : BarStyle.SOLID;
    }

    private long getEventDuration(MultiplierManager.EventMultiplier event) {
        if (event.getEndTime() <= 0L) {
            return 0L;
        } else {
            long endTime = event.getEndTime();
            long now = System.currentTimeMillis() / 1000L;
            if (endTime <= now) {
                return 0L;
            } else {
                long remaining = endTime - now;
                return Math.min(86400L, remaining * 2L);
            }
        }
    }

    private boolean shouldSeeEventBossBar(Player player, String eventId) {
        Map<String, Boolean> visibility = this.playerVisibility.computeIfAbsent(player.getUniqueId(), (k) -> new ConcurrentHashMap<>());
        return visibility.getOrDefault(eventId, true);
    }

    public void setEventBossBarVisible(Player player, String eventId, boolean visible) {
        UUID uuid = player.getUniqueId();
        Map<String, Boolean> visibility = this.playerVisibility.computeIfAbsent(uuid, (k) -> new ConcurrentHashMap<>());
        visibility.put(eventId, visible);
        BossBar bossBar = this.eventBossBars.get(eventId);
        if (bossBar != null) {
            if (visible) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    public void setAllEventBossBarsVisible(Player player, boolean visible) {
        UUID uuid = player.getUniqueId();
        Map<String, Boolean> visibility = this.playerVisibility.computeIfAbsent(uuid, (k) -> new ConcurrentHashMap<>());

        for(String eventId : this.eventBossBars.keySet()) {
            visibility.put(eventId, visible);
            BossBar bossBar = this.eventBossBars.get(eventId);
            if (bossBar != null) {
                if (visible) {
                    bossBar.addPlayer(player);
                } else {
                    bossBar.removePlayer(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for(Map.Entry<String, BossBar> entry : this.eventBossBars.entrySet()) {
            String eventId = entry.getKey();
            BossBar bossBar = entry.getValue();
            if (this.shouldSeeEventBossBar(player, eventId)) {
                bossBar.addPlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for(BossBar bossBar : this.eventBossBars.values()) {
            bossBar.removePlayer(player);
        }
    }

    public void disable() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
            this.updateTask = null;
        }

        for(BossBar bossBar : this.eventBossBars.values()) {
            bossBar.removeAll();
        }

        this.eventBossBars.clear();
        this.playerVisibility.clear();
    }
}