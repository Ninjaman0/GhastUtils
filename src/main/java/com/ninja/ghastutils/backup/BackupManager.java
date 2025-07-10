package com.ninja.ghastutils.backup;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.utils.LogManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BackupManager {
    private final GhastUtils plugin;
    private final File backupFolder;
    private final SimpleDateFormat dateFormat;
    private final long backupIntervalMillis;
    private final int maxBackups;
    private int backupTaskId = -1;

    public BackupManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        this.backupIntervalMillis = TimeUnit.DAYS.toMillis(1L);
        this.maxBackups = plugin.getConfig().getInt("backup.max-backups", 7);
        if (!this.backupFolder.exists()) {
            this.backupFolder.mkdirs();
        }

        this.startBackupTask();
    }

    private void startBackupTask() {
        if (this.backupTaskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.backupTaskId);
            this.backupTaskId = -1;
        }

        long intervalTicks = this.backupIntervalMillis / 50L;
        this.backupTaskId = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, this::createBackup, intervalTicks, intervalTicks).getTaskId();
        LogManager.info("Scheduled automatic backups every " + this.backupIntervalMillis / 86400000L + " days");
    }

    public void createBackup() {
        try {
            LogManager.info("Creating backup...");
            String timestamp = this.dateFormat.format(new Date());
            File currentBackupFolder = new File(this.backupFolder, timestamp);
            if (!currentBackupFolder.exists()) {
                currentBackupFolder.mkdirs();
            }

            File[] files = this.plugin.getDataFolder().listFiles();
            if (files != null) {
                for(File file : files) {
                    if (!file.getName().equals("backups")) {
                        if (file.isFile()) {
                            this.copyFile(file, new File(currentBackupFolder, file.getName()));
                        } else if (file.isDirectory()) {
                            this.copyDirectory(file, new File(currentBackupFolder, file.getName()));
                        }
                    }
                }
            }

            this.cleanupOldBackups();
            LogManager.info("Backup created successfully: " + timestamp);
        } catch (Exception e) {
            LogManager.error("Failed to create backup", e);
        }

    }

    private void copyFile(File source, File destination) throws IOException {
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }

        File[] files = source.listFiles();
        if (files != null) {
            for(File file : files) {
                if (file.isFile()) {
                    this.copyFile(file, new File(destination, file.getName()));
                } else if (file.isDirectory()) {
                    this.copyDirectory(file, new File(destination, file.getName()));
                }
            }
        }

    }

    private void cleanupOldBackups() {
        File[] backupDirs = this.backupFolder.listFiles(File::isDirectory);
        if (backupDirs != null && backupDirs.length > this.maxBackups) {
            Arrays.sort(backupDirs, Comparator.comparing(File::lastModified));
            int toDelete = backupDirs.length - this.maxBackups;

            for(int i = 0; i < toDelete; ++i) {
                this.deleteDirectory(backupDirs[i]);
                LogManager.debug("Deleted old backup: " + backupDirs[i].getName());
            }
        }

    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for(File file : files) {
                if (file.isDirectory()) {
                    this.deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        directory.delete();
    }

    public File getBackupFolder() {
        return this.backupFolder;
    }

    public long getBackupIntervalMillis() {
        return this.backupIntervalMillis;
    }

    public int getMaxBackups() {
        return this.maxBackups;
    }

    public void shutdown() {
        if (this.backupTaskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.backupTaskId);
            this.backupTaskId = -1;
        }

    }
}