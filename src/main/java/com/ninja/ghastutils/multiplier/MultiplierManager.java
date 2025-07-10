
package com.ninja.ghastutils.multiplier;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.data.PlayerData;
import com.ninja.ghastutils.events.EventBossBar;
import com.ninja.ghastutils.events.MultiplierChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MultiplierManager {
    private final GhastUtils plugin;
    private final Map<Material, Double> armorMultipliers;
    private final Map<UUID, MultiplierSource> cachedSources;
    private final Map<UUID, ReentrantLock> playerLocks;
    private final Map<String, EventMultiplier> globalEventMultipliers;
    private final Map<UUID, Long> playerBoosterCooldowns = new ConcurrentHashMap();
    private EventBossBar eventBossBar;
    private double globalMultiplier = (double)1.0F;
    private double minMultiplier = (double)1.0F;
    private boolean multiplyEffects = true;
    private boolean addEffects = true;
    private int cleanupTaskId = -1;

    public MultiplierManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.armorMultipliers = new ConcurrentHashMap();
        this.cachedSources = new ConcurrentHashMap();
        this.playerLocks = new ConcurrentHashMap();
        this.globalEventMultipliers = new ConcurrentHashMap();
        this.loadSettings();
        this.loadArmorMultipliers();
        this.eventBossBar = new EventBossBar(plugin);
        this.startCleanupTask();
    }

    private void startCleanupTask() {
        if (this.cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.cleanupTaskId);
        }

        this.cleanupTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            this.cleanupExpiredBoosters();
            this.cleanupInactivePlayers();
            this.cleanupExpiredEventMultipliers();
        }, 1200L, 1200L).getTaskId();
    }

    private void cleanupExpiredBoosters() {
        long currentTime = System.currentTimeMillis() / 1000L;
        int expired = 0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            ReentrantLock lock = (ReentrantLock)this.playerLocks.computeIfAbsent(playerUuid, (k) -> new ReentrantLock());
            if (lock.tryLock()) {
                try {
                    PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
                    PlayerMultiplier multiplier = playerData.getMultiplier();
                    if (multiplier.getBoosterExpiration() > 0L && multiplier.getBoosterExpiration() <= currentTime) {
                        this.clearBooster(playerUuid);
                        ++expired;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        if (expired > 0) {
            this.plugin.getLogger().info("Cleaned up " + expired + " expired boosters");
        }

    }

    private void cleanupInactivePlayers() {
        Iterator<Map.Entry<UUID, ReentrantLock>> it = this.playerLocks.entrySet().iterator();

        while(it.hasNext()) {
            UUID playerId = (UUID)((Map.Entry)it.next()).getKey();
            if (Bukkit.getPlayer(playerId) == null) {
                it.remove();
                this.cachedSources.remove(playerId);
                this.playerBoosterCooldowns.remove(playerId);
            }
        }

    }

    private void cleanupExpiredEventMultipliers() {
        long currentTime = System.currentTimeMillis() / 1000L;
        List<String> expiredEvents = new ArrayList();

        for(Map.Entry<String, EventMultiplier> entry : this.globalEventMultipliers.entrySet()) {
            EventMultiplier eventMultiplier = (EventMultiplier)entry.getValue();
            if (eventMultiplier.getEndTime() > 0L && eventMultiplier.getEndTime() <= currentTime) {
                expiredEvents.add((String)entry.getKey());
            }
        }

        for(String eventId : expiredEvents) {
            EventMultiplier eventMultiplier = (EventMultiplier)this.globalEventMultipliers.remove(eventId);
            String message = "§c[Event Multiplier] The " + eventMultiplier.getName() + " event has ended!";
            Bukkit.broadcastMessage(message);
            this.plugin.getLogger().info("Event multiplier '" + eventId + "' has expired");
        }

        if (!expiredEvents.isEmpty()) {
            this.cachedSources.clear();
        }

    }

    private void loadSettings() {
        FileConfiguration config = this.plugin.getConfig();
        this.globalMultiplier = config.getDouble("multiplier.global", (double)1.0F);
        this.minMultiplier = config.getDouble("multiplier.min", (double)1.0F);
        this.multiplyEffects = config.getBoolean("multiplier.multiply_effects", true);
        this.addEffects = config.getBoolean("multiplier.add_effects", true);
        ConfigurationSection eventsSection = config.getConfigurationSection("multiplier.events");
        if (eventsSection != null) {
            for(String eventId : eventsSection.getKeys(false)) {
                ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
                if (eventSection != null) {
                    String name = eventSection.getString("name", eventId);
                    double value = eventSection.getDouble("value", (double)0.0F);
                    long endTime = eventSection.getLong("end_time", 0L);
                    boolean active = eventSection.getBoolean("active", true);
                    if (active && (endTime == 0L || endTime > System.currentTimeMillis() / 1000L)) {
                        this.globalEventMultipliers.put(eventId, new EventMultiplier(eventId, name, value, endTime));
                    }
                }
            }
        }

        this.plugin.getLogger().info("Loaded multiplier settings: global=" + this.globalMultiplier + ", min=" + this.minMultiplier);
    }

    private void loadArmorMultipliers() {
        FileConfiguration config = this.plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("armor_multipliers");
        if (section != null) {
            for(String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    double multiplier = section.getDouble(key);
                    this.armorMultipliers.put(material, multiplier);
                } catch (IllegalArgumentException var8) {
                    this.plugin.getLogger().warning("Invalid material name: " + key);
                }
            }
        }

    }

    public void saveAll() {
        this.plugin.getPlayerDataManager().saveAllPlayerData();
        this.saveEventMultipliers();
    }

    private void saveEventMultipliers() {
        FileConfiguration config = this.plugin.getConfig();
        config.set("multiplier.events", (Object)null);
        ConfigurationSection eventsSection = config.createSection("multiplier.events");

        for(Map.Entry<String, EventMultiplier> entry : this.globalEventMultipliers.entrySet()) {
            String eventId = (String)entry.getKey();
            EventMultiplier event = (EventMultiplier)entry.getValue();
            ConfigurationSection eventSection = eventsSection.createSection(eventId);
            eventSection.set("name", event.getName());
            eventSection.set("value", event.getValue());
            eventSection.set("end_time", event.getEndTime());
            eventSection.set("active", true);
        }

        this.plugin.saveConfig();
    }

    public void setBooster(UUID playerUuid, double multiplier, long durationSeconds) {
        if (multiplier < (double)0.0F) {
            this.plugin.getLogger().warning("Attempted to set negative multiplier: " + multiplier + " for " + String.valueOf(playerUuid));
            multiplier = (double)0.0F;
        }

        if (this.playerBoosterCooldowns.containsKey(playerUuid) && System.currentTimeMillis() < (Long)this.playerBoosterCooldowns.get(playerUuid)) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.sendMessage("§cYou need to wait before activating another booster.");
            }

        } else {
            PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
            PlayerMultiplier playerMultiplier = playerData.getMultiplier();
            long expiryTime = System.currentTimeMillis() / 1000L + durationSeconds;
            playerMultiplier.setBoosterExpiration(expiryTime);
            playerMultiplier.setBoosterMultiplier(multiplier);
            this.playerBoosterCooldowns.put(playerUuid, expiryTime * 1000L);
            this.cachedSources.remove(playerUuid);
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                double total = this.getTotalMultiplier(playerUuid);
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    MultiplierChangeEvent event = new MultiplierChangeEvent(player, total);
                    Bukkit.getPluginManager().callEvent(event);
                });
                Map<String, String> placeholders = new HashMap();
                placeholders.put("multiplier", String.format("%.2f", multiplier));
                placeholders.put("duration", this.formatTime(durationSeconds));
                player.sendMessage("§aBooster activated! Multiplier: " + multiplier + "x, Duration: " + this.formatTime(durationSeconds));
            }

        }
    }

    public void clearBooster(UUID playerUuid) {
        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
        PlayerMultiplier playerMultiplier = playerData.getMultiplier();
        playerMultiplier.setBoosterMultiplier((double)1.0F);
        playerMultiplier.setBoosterExpiration(0L);
        this.cachedSources.remove(playerUuid);
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            double total = this.getTotalMultiplier(playerUuid);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                MultiplierChangeEvent event = new MultiplierChangeEvent(player, total);
                Bukkit.getPluginManager().callEvent(event);
            });
            player.sendMessage("§cYour booster has expired!");
        }

    }

    public void setPetMultiplier(UUID playerUuid, double multiplier, boolean active) {
        if (multiplier < (double)0.0F) {
            this.plugin.getLogger().warning("Attempted to set negative pet multiplier: " + multiplier + " for " + String.valueOf(playerUuid));
            multiplier = (double)0.0F;
        }

        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
        PlayerMultiplier playerMultiplier = playerData.getMultiplier();
        playerMultiplier.setPetMultiplier(multiplier);
        playerMultiplier.setPetActive(active);
        this.cachedSources.remove(playerUuid);
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            double total = this.getTotalMultiplier(playerUuid);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                MultiplierChangeEvent event = new MultiplierChangeEvent(player, total);
                Bukkit.getPluginManager().callEvent(event);
            });
            if (active && multiplier > (double)0.0F) {
                Object[] var10002 = new Object[]{multiplier};
                player.sendMessage("§aPet boost activated! Multiplier: " + String.format("%.2f", var10002) + "x");
            } else if (!active) {
                player.sendMessage("§cYour pet boost has been deactivated.");
            }
        }

    }

    public double calculateArmorMultiplier(Player player) {
        double customArmorMultiplier = this.plugin.getArmorListener().getArmorMultiplier(player);
        if (customArmorMultiplier > (double)1.0F) {
            return customArmorMultiplier - (double)1.0F;
        } else {
            double armorMultiplier = (double)0.0F;

            for(ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && !item.getType().equals(Material.AIR) && this.armorMultipliers.containsKey(item.getType())) {
                    armorMultiplier += (Double)this.armorMultipliers.get(item.getType());
                }
            }

            return armorMultiplier;
        }
    }

    public double getTotalMultiplier(UUID playerUuid) {
        if (this.cachedSources.containsKey(playerUuid)) {
            MultiplierSource source = (MultiplierSource)this.cachedSources.get(playerUuid);
            if (!source.isExpired()) {
                return source.getTotal();
            }
        }

        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
        PlayerMultiplier playerMultiplier = playerData.getMultiplier();
        MultiplierSource source = new MultiplierSource();
        source.addBase(this.globalMultiplier);

        for(EventMultiplier event : this.globalEventMultipliers.values()) {
            if (this.multiplyEffects) {
                source.addMultiplicative(event.getValue());
            } else if (this.addEffects) {
                source.addAdditive(event.getValue());
            }
        }

        if (playerMultiplier.getBoosterExpiration() > System.currentTimeMillis() / 1000L) {
            if (this.multiplyEffects) {
                source.addMultiplicative(playerMultiplier.getBoosterMultiplier() - (double)1.0F);
            } else if (this.addEffects) {
                source.addAdditive(playerMultiplier.getBoosterMultiplier() - (double)1.0F);
            }
        }

        if (playerMultiplier.isPetActive() && playerMultiplier.getPetMultiplier() > (double)0.0F) {
            if (this.multiplyEffects) {
                source.addMultiplicative(playerMultiplier.getPetMultiplier());
            } else if (this.addEffects) {
                source.addAdditive(playerMultiplier.getPetMultiplier());
            }
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            double armorMultiplier = this.calculateArmorMultiplier(player);
            if (armorMultiplier > (double)0.0F) {
                if (this.multiplyEffects) {
                    source.addMultiplicative(armorMultiplier);
                } else if (this.addEffects) {
                    source.addAdditive(armorMultiplier);
                }
            }
        }

        double total = source.calculate();
        if (total < this.minMultiplier) {
            total = this.minMultiplier;
        }

        source.setTotal(total);
        this.cachedSources.put(playerUuid, source);
        return total;
    }

    public Map<String, Double> getMultiplierBreakdown(UUID playerUuid) {
        Map<String, Double> breakdown = new LinkedHashMap();
        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
        PlayerMultiplier playerMultiplier = playerData.getMultiplier();
        breakdown.put("Global", this.globalMultiplier);

        for(EventMultiplier event : this.globalEventMultipliers.values()) {
            breakdown.put("Event: " + event.getName(), event.getValue());
        }

        if (playerMultiplier.getBoosterExpiration() > System.currentTimeMillis() / 1000L) {
            breakdown.put("Booster", playerMultiplier.getBoosterMultiplier() - (double)1.0F);
        }

        if (playerMultiplier.isPetActive()) {
            breakdown.put("Pet", playerMultiplier.getPetMultiplier());
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            breakdown.put("Armor", this.calculateArmorMultiplier(player));
        }

        breakdown.put("Total", this.getTotalMultiplier(playerUuid));
        return breakdown;
    }

    public void checkBoosterExpirations() {
        long currentTime = System.currentTimeMillis() / 1000L;
        int expired = 0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
            PlayerMultiplier multiplier = playerData.getMultiplier();
            if (multiplier.getBoosterExpiration() > 0L && multiplier.getBoosterExpiration() <= currentTime) {
                multiplier.setBoosterMultiplier((double)1.0F);
                multiplier.setBoosterExpiration(0L);
                this.cachedSources.remove(playerUuid);
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    player.sendMessage("§cYour booster has expired!");
                    MultiplierChangeEvent event = new MultiplierChangeEvent(player, this.getTotalMultiplier(playerUuid));
                    Bukkit.getPluginManager().callEvent(event);
                });
                ++expired;
            }
        }

        if (expired > 0) {
        }

    }

    public long getBoosterTimeLeft(UUID playerUuid) {
        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
        PlayerMultiplier multiplier = playerData.getMultiplier();
        return multiplier.getBoosterExpiration() > System.currentTimeMillis() / 1000L ? multiplier.getBoosterExpiration() - System.currentTimeMillis() / 1000L : 0L;
    }

    public boolean isPetActive(UUID playerUuid) {
        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(playerUuid);
        PlayerMultiplier multiplier = playerData.getMultiplier();
        return multiplier.isPetActive();
    }

    public void setGlobalMultiplier(double multiplier) {
        if (multiplier < (double)0.0F) {
            this.plugin.getLogger().warning("Attempted to set negative global multiplier: " + multiplier);
            multiplier = (double)0.0F;
        }

        this.globalMultiplier = multiplier;
        this.cachedSources.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            Object[] var10002 = new Object[]{multiplier};
            player.sendMessage("§6Global multiplier updated: §f" + String.format("%.2f", var10002) + "x");
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                MultiplierChangeEvent event = new MultiplierChangeEvent(player, this.getTotalMultiplier(player.getUniqueId()));
                Bukkit.getPluginManager().callEvent(event);
            });
        }

    }

    public boolean createEventMultiplier(String eventId, String eventName, double value, long durationSeconds) {
        if (value <= (double)0.0F) {
            return false;
        } else {
            long endTime = durationSeconds > 0L ? System.currentTimeMillis() / 1000L + durationSeconds : 0L;
            this.globalEventMultipliers.put(eventId, new EventMultiplier(eventId, eventName, value, endTime));
            this.cachedSources.clear();
            this.saveEventMultipliers();
            String message = "§a[Event Multiplier] " + eventName + " has started! +" + String.format("%.2f", value) + "x multiplier";
            if (endTime > 0L) {
                message = message + " for " + this.formatTime(durationSeconds);
            }

            Bukkit.broadcastMessage(message);

            for(Player player : Bukkit.getOnlinePlayers()) {
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    double total = this.getTotalMultiplier(player.getUniqueId());
                    Object[] var10002 = new Object[]{total};
                    player.sendMessage("§aYour new total multiplier: §f" + String.format("%.2f", var10002) + "x");
                    MultiplierChangeEvent event = new MultiplierChangeEvent(player, total);
                    Bukkit.getPluginManager().callEvent(event);
                }, 5L);
            }

            return true;
        }
    }

    public boolean removeEventMultiplier(String eventId) {
        EventMultiplier event = (EventMultiplier)this.globalEventMultipliers.remove(eventId);
        if (event == null) {
            return false;
        } else {
            this.cachedSources.clear();
            this.saveEventMultipliers();
            String message = "§c[Event Multiplier] The " + event.getName() + " event has ended!";
            Bukkit.broadcastMessage(message);

            for(Player player : Bukkit.getOnlinePlayers()) {
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    double total = this.getTotalMultiplier(player.getUniqueId());
                    Object[] var10002 = new Object[]{total};
                    player.sendMessage("§aYour new total multiplier: §f" + String.format("%.2f", var10002) + "x");
                    MultiplierChangeEvent event2 = new MultiplierChangeEvent(player, total);
                    Bukkit.getPluginManager().callEvent(event2);
                }, 5L);
            }

            return true;
        }
    }

    public Map<String, EventMultiplier> getEventMultipliers() {
        return Collections.unmodifiableMap(this.globalEventMultipliers);
    }

    public double getGlobalMultiplier() {
        return this.globalMultiplier;
    }

    public double getMinMultiplier() {
        return this.minMultiplier;
    }

    public void setMinMultiplier(double multiplier) {
        if (!(multiplier < (double)0.0F)) {
            this.minMultiplier = multiplier;
            this.cachedSources.clear();
        }
    }

    public String formatTime(long seconds) {
        if (seconds < 60L) {
            return seconds + "s";
        } else {
            return seconds < 3600L ? seconds / 60L + "m " + seconds % 60L + "s" : seconds / 3600L + "h " + seconds % 3600L / 60L + "m";
        }
    }

    public void reload() {
        this.armorMultipliers.clear();
        this.cachedSources.clear();
        this.playerBoosterCooldowns.clear();
        this.globalEventMultipliers.clear();
        this.loadSettings();
        this.loadArmorMultipliers();
        this.checkBoosterExpirations();
        this.plugin.getLogger().info("Multiplier manager reloaded");
    }

    public boolean startEvent(String id, CommandSender sender) {
        FileConfiguration config = this.plugin.getConfig();
        ConfigurationSection eventsConfig = config.getConfigurationSection("multiplier.event-templates");
        if (eventsConfig != null && eventsConfig.contains(id)) {
            ConfigurationSection eventSection = eventsConfig.getConfigurationSection(id);
            if (eventSection == null) {
                sender.sendMessage("§cInvalid event template configuration for '" + id + "'!");
                return false;
            } else {
                String name = eventSection.getString("name", id);
                double value = eventSection.getDouble("value", (double)0.5F);
                long duration = eventSection.getLong("duration", 3600L);
                if (this.globalEventMultipliers.containsKey(id)) {
                    sender.sendMessage("§cEvent '" + id + "' is already running!");
                    return false;
                } else if (this.createEventMultiplier(id, name, value, duration)) {
                    sender.sendMessage("§aEvent '" + id + "' started successfully!");
                    return true;
                } else {
                    sender.sendMessage("§cFailed to start event '" + id + "'!");
                    return false;
                }
            }
        } else {
            sender.sendMessage("§cEvent template '" + id + "' not found!");
            return false;
        }
    }

    public boolean stopEvent(String id, CommandSender sender) {
        if (!this.globalEventMultipliers.containsKey(id)) {
            sender.sendMessage("§cEvent '" + id + "' is not running!");
            return false;
        } else if (this.removeEventMultiplier(id)) {
            sender.sendMessage("§aEvent '" + id + "' stopped successfully!");
            return true;
        } else {
            sender.sendMessage("§cFailed to stop event '" + id + "'!");
            return false;
        }
    }

    public EventBossBar getEventBossBar() {
        return this.eventBossBar;
    }

    private static class MultiplierSource {
        private double base = (double)1.0F;
        private List<Double> additives = new ArrayList();
        private List<Double> multiplicatives = new ArrayList();
        private long expiry = 0L;
        private double total = (double)0.0F;

        public MultiplierSource() {
            this.expiry = System.currentTimeMillis() + 10000L;
        }

        public void addBase(double value) {
            this.base = value;
        }

        public void addAdditive(double value) {
            this.additives.add(value);
        }

        public void addMultiplicative(double value) {
            this.multiplicatives.add(value);
        }

        public double calculate() {
            double result = this.base;

            for(double value : this.additives) {
                result += value;
            }

            for(double value : this.multiplicatives) {
                result *= (double)1.0F + value;
            }

            return result;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > this.expiry;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public double getTotal() {
            return this.total;
        }
    }

    public static class EventMultiplier {
        private final String id;
        private final String name;
        private final double value;
        private final long endTime;

        public EventMultiplier(String id, String name, double value, long endTime) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.endTime = endTime;
        }

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public double getValue() {
            return this.value;
        }

        public long getEndTime() {
            return this.endTime;
        }

        public boolean isExpired() {
            return this.endTime > 0L && this.endTime <= System.currentTimeMillis() / 1000L;
        }

        public String getTimeRemaining() {
            if (this.endTime == 0L) {
                return "Permanent";
            } else {
                long remaining = this.endTime - System.currentTimeMillis() / 1000L;
                if (remaining <= 0L) {
                    return "Expired";
                } else {
                    long hours = remaining / 3600L;
                    long minutes = remaining % 3600L / 60L;
                    long seconds = remaining % 60L;
                    if (hours > 0L) {
                        return String.format("%dh %dm %ds", hours, minutes, seconds);
                    } else {
                        return minutes > 0L ? String.format("%dm %ds", minutes, seconds) : String.format("%ds", seconds);
                    }
                }
            }
        }
    }
}
