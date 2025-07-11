package com.ninja.ghastutils;

import com.ninja.ghastutils.armor.ArmorListener;
import com.ninja.ghastutils.armor.ArmorManager;
import com.ninja.ghastutils.backup.BackupManager;
import com.ninja.ghastutils.blocks.BlockManager;
import com.ninja.ghastutils.commands.MainCommand;
import com.ninja.ghastutils.commands.subcommands.AutoSellCommand;
import com.ninja.ghastutils.commands.subcommands.AutoCraftCommand;
import com.ninja.ghastutils.config.ConfigManager;
import com.ninja.ghastutils.crafting.CraftingManager;
import com.ninja.ghastutils.data.PlayerDataManager;
import com.ninja.ghastutils.events.EventBossBar;
import com.ninja.ghastutils.gui.GUIManager;
import com.ninja.ghastutils.listeners.ArmorPlaceListener;
import com.ninja.ghastutils.listeners.BlockInteractionListener;
import com.ninja.ghastutils.listeners.GuiListener;
import com.ninja.ghastutils.listeners.MultiplierListener;
import com.ninja.ghastutils.listeners.VanillaFeatureListener;
import com.ninja.ghastutils.multiplier.MultiplierManager;
import com.ninja.ghastutils.multiplier.api.multiplierProvider;
import com.ninja.ghastutils.papi.PlaceholderManager;
import com.ninja.ghastutils.rivalpets.PetMultiplierBuff;
import com.ninja.ghastutils.sell.SellManager;
import com.ninja.ghastutils.utils.EconomyUtil;
import com.ninja.ghastutils.utils.LogManager;
import com.ninja.ghastutils.utils.MessageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class GhastUtils extends JavaPlugin {
    private static GhastUtils instance;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private MultiplierManager multiplierManager;
    private SellManager sellManager;
    private CraftingManager craftingManager;
    private PlaceholderManager placeholderManager;
    private PetMultiplierBuff petMultiplierBuff;
    private ArmorManager armorManager;
    private ArmorListener armorListener;
    private GUIManager guiManager;
    private GuiListener guiListener;
    private AutoSellCommand autoSellCommand;
    private AutoCraftCommand autoCraftCommand;
    private BlockManager blockManager;
    private BackupManager backupManager;
    private EventBossBar eventBossBar;
    private VanillaFeatureListener vanillaFeatureListener;

    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();
        multiplierProvider.initialize(this);
        LogManager.initialize(this);
        MessageUtils.initialize(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.playerDataManager.loadAllPlayerData();
        EconomyUtil.initialize();
        this.multiplierManager = new MultiplierManager(this);
        this.sellManager = new SellManager(this);
        this.craftingManager = new CraftingManager(this);
        this.armorManager = new ArmorManager(this);
        this.guiManager = new GUIManager(this);
        this.blockManager = new BlockManager(this);
        this.backupManager = new BackupManager(this);
        this.guiListener = new GuiListener(this);
        this.vanillaFeatureListener = new VanillaFeatureListener(this);
        
        this.getServer().getPluginManager().registerEvents(this.guiListener, this);
        this.getServer().getPluginManager().registerEvents(new ArmorPlaceListener(), this);
        this.registerCommands();
        this.armorListener = new ArmorListener(this, this.armorManager);
        this.getServer().getPluginManager().registerEvents(new MultiplierListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockInteractionListener(this), this);
        this.getServer().getPluginManager().registerEvents(this.armorListener, this);
        this.eventBossBar = this.multiplierManager.getEventBossBar();
        this.setupPlaceholderAPI();
        this.setupRivalPetsIntegration();
        this.scheduleRecurringTasks();
        LogManager.info("GhastUtils has been enabled!");
    }

    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        this.autoSellCommand = new AutoSellCommand(this);
        this.autoCraftCommand = new AutoCraftCommand(this);
        this.registerCommand("gutil", mainCommand, mainCommand);
        
        CommandExecutor sellExecutor = (sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
                String[] newArgs = new String[0];
                return mainCommand.onCommand(sender, command, label, new String[]{"sell", "gui"});
            } else {
                return mainCommand.onCommand(sender, command, label, new String[]{"sell"});
            }
        };
        TabCompleter sellTabCompleter = (sender, command, alias, args) -> 
            args.length == 1 ? Arrays.asList("gui") : new ArrayList<>();
        this.registerCommand("sell", sellExecutor, sellTabCompleter);
        this.registerCommand("sellgui", (sender, command, label, args) -> 
            mainCommand.onCommand(sender, command, label, new String[]{"sell", "gui"}), 
            (sender, command, alias, args) -> new ArrayList<>());
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = this.getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(tabCompleter);
            LogManager.debug("Registered command: " + name);
        } else {
            LogManager.error("Failed to register command: " + name);
        }
    }

    public void onDisable() {
        if (this.playerDataManager != null) {
            this.playerDataManager.shutdown();
        }

        if (this.blockManager != null) {
            this.blockManager.savePlacedBlocks();
        }

        if (this.autoSellCommand != null) {
            this.autoSellCommand.onDisable();
        }

        if (this.autoCraftCommand != null) {
            this.autoCraftCommand.onDisable();
        }

        if (this.backupManager != null) {
            this.backupManager.shutdown();
        }

        if (this.placeholderManager != null) {
            this.placeholderManager.unregister();
        }

        if (this.eventBossBar != null) {
            this.eventBossBar.disable();
        }

        LogManager.info("GhastUtils has been disabled!");
    }

    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                this.placeholderManager = new PlaceholderManager(this);
                this.placeholderManager.register();
                LogManager.info("Hooked into PlaceholderAPI!");
            }, 20L);
        }
    }

    private void setupRivalPetsIntegration() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("RivalPets") != null) {
                try {
                    this.petMultiplierBuff = new PetMultiplierBuff(this);
                    LogManager.info("Hooked into RivalPets!");
                } catch (Exception e) {
                    LogManager.warning("Failed to hook into RivalPets: " + e.getMessage());
                }
            }
        }, 60L);
    }

    private void scheduleRecurringTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                this.multiplierManager.checkBoosterExpirations();
            } catch (Exception e) {
                LogManager.error("Error checking booster expirations", e);
            }
        }, 1200L, 1200L);
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                this.playerDataManager.saveAllPlayerData();
            } catch (Exception e) {
                LogManager.error("Error auto-saving player data", e);
            }
        }, 6000L, 6000L);
    }

    public void reload() {
        this.configManager.reloadAllConfigs();
        MessageUtils.reload();
        if (this.playerDataManager != null) {
            this.playerDataManager.loadAllPlayerData();
        }

        if (this.multiplierManager != null) {
            this.multiplierManager.reload();
        }

        if (this.sellManager != null) {
            this.sellManager.reload();
        }

        if (this.craftingManager != null) {
            this.craftingManager.reload();
        }

        if (this.armorManager != null) {
            this.armorManager.reload();
        }

        if (this.blockManager != null) {
            this.blockManager.reload();
        }

        EconomyUtil.initialize();
        LogManager.info("GhastUtils has been reloaded!");
    }

    public static GhastUtils getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    public MultiplierManager getMultiplierManager() {
        return this.multiplierManager;
    }

    public SellManager getSellManager() {
        return this.sellManager;
    }

    public CraftingManager getCraftingManager() {
        return this.craftingManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return this.placeholderManager;
    }

    public PetMultiplierBuff getPetMultiplierBuff() {
        return this.petMultiplierBuff;
    }

    public ArmorManager getArmorManager() {
        return this.armorManager;
    }

    public ArmorListener getArmorListener() {
        return this.armorListener;
    }

    public GUIManager getGuiManager() {
        return this.guiManager;
    }

    public GuiListener getGuiListener() {
        return this.guiListener;
    }

    public BlockManager getBlockManager() {
        return this.blockManager;
    }

    public BackupManager getBackupManager() {
        return this.backupManager;
    }

    public EventBossBar getEventBossBar() {
        return this.eventBossBar;
    }

    public AutoCraftCommand getAutoCraftCommand() {
        return this.autoCraftCommand;
    }
}