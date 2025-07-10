
package com.ninja.ghastutils.data;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.config.ConfigManager;
import com.ninja.ghastutils.config.ConfigType;
import com.ninja.ghastutils.multiplier.PlayerMultiplier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataManager implements Listener {
    private final GhastUtils plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final ConfigManager configManager;
    private final ReentrantLock saveLock = new ReentrantLock();
    private boolean autoSaveEnabled = true;
    private int autoSaveInterval = 300;
    private int autoSaveTaskId = -1;
    private long lastSaveTime = 0L;
    private static final long MIN_SAVE_INTERVAL = 5000L;

    public PlayerDataManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.playerDataMap = new ConcurrentHashMap();
        this.configManager = plugin.getConfigManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        if (this.autoSaveTaskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.autoSaveTaskId);
        }

        if (this.autoSaveEnabled) {
            this.autoSaveTaskId = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, this::safeSaveAllPlayerData, (long)this.autoSaveInterval * 20L, (long)this.autoSaveInterval * 20L).getTaskId();
            this.plugin.getLogger().info("Auto-save task started (Interval: " + this.autoSaveInterval + "s)");
        }

    }

    private void safeSaveAllPlayerData() {
        long now = System.currentTimeMillis();
        if (now - this.lastSaveTime >= 5000L) {
            if (this.saveLock.tryLock()) {
                try {
                    this.saveAllPlayerData();
                    this.lastSaveTime = System.currentTimeMillis();
                } finally {
                    this.saveLock.unlock();
                }
            }

        }
    }

    public void loadAllPlayerData() {
        this.playerDataMap.clear();
        FileConfiguration playersConfig = this.configManager.getConfig(ConfigType.PLAYERS);
        if (playersConfig.isConfigurationSection("players")) {
            ConfigurationSection playersSection = playersConfig.getConfigurationSection("players");
            if (playersSection == null) {
                return;
            }

            for(String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                    if (playerSection != null) {
                        PlayerData playerData = new PlayerData(uuid);
                        this.loadPlayerData(playerData, playerSection);
                        this.playerDataMap.put(uuid, playerData);
                    }
                } catch (IllegalArgumentException var8) {
                    this.plugin.getLogger().warning("Invalid UUID in players.yml: " + uuidStr);
                }
            }
        }

        this.plugin.getLogger().info("Loaded data for " + this.playerDataMap.size() + " players");
    }

    private void loadPlayerData(PlayerData playerData, ConfigurationSection section) {
        PlayerMultiplier multiplier = playerData.getMultiplier();
        if (section.isConfigurationSection("booster")) {
            ConfigurationSection boosterSection = section.getConfigurationSection("booster");
            if (boosterSection != null) {
                multiplier.setBoosterMultiplier(boosterSection.getDouble("multiplier", (double)1.0F));
                multiplier.setBoosterExpiration(boosterSection.getLong("expires", 0L));
            }
        }

        if (section.isConfigurationSection("pet")) {
            ConfigurationSection petSection = section.getConfigurationSection("pet");
            if (petSection != null) {
                multiplier.setPetActive(petSection.getBoolean("active", false));
                multiplier.setPetMultiplier(petSection.getDouble("multiplier", (double)0.0F));
            }
        }

    }

    public void saveAllPlayerData() {
        this.saveLock.lock();

        try {
            FileConfiguration playersConfig = this.configManager.getConfig(ConfigType.PLAYERS);
            ConfigurationSection playersSection = playersConfig.createSection("players");

            for(Map.Entry<UUID, PlayerData> entry : this.playerDataMap.entrySet()) {
                this.savePlayerData(playersSection, (UUID)entry.getKey(), (PlayerData)entry.getValue());
            }

            this.configManager.saveConfig(ConfigType.PLAYERS);
        } finally {
            this.saveLock.unlock();
        }

    }

    private void savePlayerData(ConfigurationSection config, UUID uuid, PlayerData playerData) {
        String path = uuid.toString();
        ConfigurationSection playerSection = config.createSection(path);
        PlayerMultiplier multiplier = playerData.getMultiplier();
        if (multiplier.getBoosterMultiplier() > (double)1.0F && multiplier.getBoosterExpiration() > System.currentTimeMillis() / 1000L) {
            ConfigurationSection boosterSection = playerSection.createSection("booster");
            boosterSection.set("multiplier", multiplier.getBoosterMultiplier());
            boosterSection.set("expires", multiplier.getBoosterExpiration());
        }

        if (multiplier.isPetActive() && multiplier.getPetMultiplier() > (double)0.0F) {
            ConfigurationSection petSection = playerSection.createSection("pet");
            petSection.set("active", true);
            petSection.set("multiplier", multiplier.getPetMultiplier());
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        this.getPlayerData(uuid);
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (this.playerDataMap.containsKey(uuid)) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                this.saveLock.lock();

                try {
                    FileConfiguration playersConfig = this.configManager.getConfig(ConfigType.PLAYERS);
                    ConfigurationSection playersSection = playersConfig.getConfigurationSection("players");
                    if (playersSection == null) {
                        playersSection = playersConfig.createSection("players");
                    }

                    this.savePlayerData(playersSection, uuid, (PlayerData)this.playerDataMap.get(uuid));
                    this.configManager.saveConfig(ConfigType.PLAYERS);
                } finally {
                    this.saveLock.unlock();
                }

            });
        }

    }

    public PlayerData getPlayerData(UUID uuid) {
        return (PlayerData)this.playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public void setPlayerData(UUID uuid, PlayerData playerData) {
        this.playerDataMap.put(uuid, playerData);
    }

    public void shutdown() {
        if (this.autoSaveTaskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.autoSaveTaskId);
        }

        this.saveAllPlayerData();
    }

    public void setAutoSaveSettings(boolean enabled, int intervalSeconds) {
        this.autoSaveEnabled = enabled;
        this.autoSaveInterval = Math.max(30, intervalSeconds);
        this.startAutoSaveTask();
    }
}
