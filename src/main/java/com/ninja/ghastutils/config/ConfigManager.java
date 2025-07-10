
package com.ninja.ghastutils.config;

import com.ninja.ghastutils.GhastUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final GhastUtils plugin;
    private final Map<ConfigType, FileConfiguration> configs;
    private final Map<ConfigType, File> configFiles;
    private final Map<ConfigType, ReentrantLock> fileLocks = new ConcurrentHashMap();

    public ConfigManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.configs = new HashMap();
        this.configFiles = new HashMap();
    }

    private ReentrantLock getLock(ConfigType type) {
        return (ReentrantLock)this.fileLocks.computeIfAbsent(type, (k) -> new ReentrantLock());
    }

    public void loadConfigs() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        File backupFolder = new File(this.plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        for(ConfigType type : ConfigType.values()) {
            this.loadConfig(type);
        }

    }

    public void loadConfig(ConfigType type) {
        File file = new File(this.plugin.getDataFolder(), type.getFileName());
        if (!file.exists()) {
            if (type != ConfigType.MESSAGES && type != ConfigType.MAIN) {
                try {
                    this.plugin.saveResource(type.getFileName(), false);
                } catch (IllegalArgumentException var6) {
                    try {
                        file.createNewFile();
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        if (type == ConfigType.SELL) {
                            config.createSection("items");
                        } else if (type == ConfigType.PLAYERS) {
                            config.createSection("players");
                        } else if (type == ConfigType.CRAFTING) {
                        }

                        config.save(file);
                    } catch (IOException ioEx) {
                        this.plugin.getLogger().log(Level.SEVERE, "Could not create " + type.getFileName(), ioEx);
                    }
                }
            } else {
                this.plugin.saveResource(type.getFileName(), false);
            }
        }

        this.checkForUpdates(type, file);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.configs.put(type, config);
        this.configFiles.put(type, file);
        this.plugin.getLogger().info("Loaded " + type.getFileName());
    }

    private void checkForUpdates(ConfigType type, File file) {
        InputStream defaultConfigStream = this.plugin.getResource(type.getFileName());
        if (defaultConfigStream != null) {
            try {
                FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(file);
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
                boolean updated = false;

                for(String key : defaultConfig.getKeys(true)) {
                    if (!existingConfig.contains(key)) {
                        existingConfig.set(key, defaultConfig.get(key));
                        updated = true;
                    }
                }

                if (updated) {
                    this.createBackup(type);
                    existingConfig.save(file);
                    this.plugin.getLogger().info("Updated " + type.getFileName() + " with missing values");
                }
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Error checking for config updates: " + e.getMessage(), e);
            } finally {
                try {
                    defaultConfigStream.close();
                } catch (IOException var16) {
                }

            }

        }
    }

    public FileConfiguration getConfig(ConfigType type) {
        if (!this.configs.containsKey(type)) {
            this.loadConfig(type);
        }

        return (FileConfiguration)this.configs.get(type);
    }

    public void saveConfig(ConfigType type) {
        ReentrantLock lock = this.getLock(type);
        lock.lock();

        try {
            File file = (File)this.configFiles.get(type);
            File tempFile = new File(file.getParent(), file.getName() + ".tmp");
            ((FileConfiguration)this.configs.get(type)).save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.createBackup(type));
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save " + type.getFileName(), e);
        } finally {
            lock.unlock();
        }

    }

    public void reloadConfig(ConfigType type) {
        File file = (File)this.configFiles.get(type);
        if (file.exists()) {
            this.configs.put(type, YamlConfiguration.loadConfiguration(file));
        } else {
            this.plugin.getLogger().warning("Could not reload " + type.getFileName() + ": File does not exist");
            this.loadConfig(type);
        }

    }

    public void reloadAllConfigs() {
        for(ConfigType type : ConfigType.values()) {
            this.reloadConfig(type);
        }

    }

    private void rotateOldBackups(File backupFolder, String baseName) {
        File[] backups = backupFolder.listFiles((dir, name) -> name.startsWith(baseName) && name.length() > baseName.length());
        if (backups != null && backups.length > 10) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

            for(int i = 0; i < backups.length - 10; ++i) {
                try {
                    Files.delete(backups[i].toPath());
                } catch (IOException var6) {
                    this.plugin.getLogger().warning("Failed to delete old backup: " + backups[i].getName());
                }
            }

        }
    }

    private void createBackup(ConfigType type) {
        File file = (File)this.configFiles.get(type);
        File backupFolder = new File(this.plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        String timestamp = (new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")).format(new Date());
        String var10003 = type.getFileName();
        File backupFile = new File(backupFolder, var10003 + "." + timestamp);
        int maxAttempts = 3;
        int attempt = 0;
        boolean success = false;

        while(attempt < maxAttempts && !success) {
            try {
                if (this.isFileAvailable(file)) {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    success = true;
                    this.plugin.getLogger().fine("Created backup of " + type.getFileName());
                    this.rotateOldBackups(backupFolder, type.getFileName());
                } else {
                    ++attempt;
                    if (attempt < maxAttempts) {
                        Thread.sleep((long)(200 * attempt));
                    }
                }
            } catch (InterruptedException | IOException e) {
                ++attempt;
                if (attempt >= maxAttempts) {
                    this.plugin.getLogger().log(Level.WARNING, "Failed to create backup after " + maxAttempts + " attempts: " + ((Exception)e).getMessage());
                }
            }
        }

    }

    public boolean isFileAvailable(String path) {
        if (path == null) {
            return false;
        } else {
            File file = new File(this.plugin.getDataFolder(), path);
            return file != null && file.exists() && !file.isDirectory();
        }
    }

    private boolean isFileAvailable(File file) {
        if (file == null) {
            return false;
        } else {
            try {
                return file.exists() && !file.isDirectory() && file.canRead();
            } catch (SecurityException var3) {
                return false;
            }
        }
    }
}
