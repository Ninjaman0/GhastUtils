package com.ninja.ghastutils.blocks;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.config.ConfigType;
import com.ninja.ghastutils.utils.ItemUtils;
import com.ninja.ghastutils.utils.LogManager;
import com.ninja.ghastutils.utils.MessageUtils;
import com.ninja.ghastutils.utils.NBTUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockManager {
    private final GhastUtils plugin;
    private final Map<String, CustomBlock> customBlocks;
    private final Map<String, String> placedBlocks;
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private long defaultCooldown = 0L;

    public BlockManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.customBlocks = new ConcurrentHashMap();
        this.placedBlocks = new ConcurrentHashMap();
        this.playerCooldowns = new ConcurrentHashMap();
        this.loadCustomBlocks();
        this.loadPlacedBlocks();
    }

    private void loadCustomBlocks() {
        FileConfiguration blockConfig = this.plugin.getConfigManager().getConfig(ConfigType.BLOCK_COMMANDS);
        ConfigurationSection blocksSection = blockConfig.getConfigurationSection("blocks");
        this.defaultCooldown = blockConfig.getLong("settings.default-cooldown", 0L);
        if (this.defaultCooldown > 0L) {
            LogManager.info("Block command cooldown set to " + this.defaultCooldown + "ms");
        }

        if (blocksSection == null) {
            LogManager.warning("No custom blocks found in block_commands.yml");
        } else {
            Iterator var3 = blocksSection.getKeys(false).iterator();

            while(true) {
                String blockId;
                ConfigurationSection blockSection;
                String name;
                Material material;
                while(true) {
                    if (!var3.hasNext()) {
                        LogManager.info("Loaded " + this.customBlocks.size() + " custom blocks");
                        return;
                    }

                    blockId = (String)var3.next();
                    blockSection = blocksSection.getConfigurationSection(blockId);
                    if (blockSection != null) {
                        name = blockSection.getString("name", blockId);

                        try {
                            material = Material.valueOf(blockSection.getString("material", "STONE").toUpperCase());
                            break;
                        } catch (IllegalArgumentException var16) {
                            LogManager.warning("Invalid material for custom block " + blockId + ": " + blockSection.getString("material"));
                        }
                    }
                }

                List<String> lore = blockSection.getStringList("lore");
                String permission = blockSection.getString("permission", (String)null);
                Integer customModelData = blockSection.contains("custom_model_data") ? blockSection.getInt("custom_model_data") : null;
                CustomBlock customBlock = new CustomBlock(blockId, name, material, lore);
                customBlock.setPermission(permission);
                if (customModelData != null) {
                    customBlock.setCustomModelData(customModelData);
                }

                if (blockSection.contains("cooldown")) {
                    customBlock.setCooldown(blockSection.getLong("cooldown"));
                }

                ConfigurationSection commandsSection = blockSection.getConfigurationSection("commands");
                if (commandsSection != null) {
                    for(String action : commandsSection.getKeys(false)) {
                        List<String> commands = commandsSection.getStringList(action);
                        if (!commands.isEmpty()) {
                            customBlock.addCommands(action, commands);
                        }
                    }
                }

                this.customBlocks.put(blockId, customBlock);
            }
        }
    }

    private void loadPlacedBlocks() {
        FileConfiguration blockConfig = this.plugin.getConfigManager().getConfig(ConfigType.BLOCK_COMMANDS);
        ConfigurationSection placedSection = blockConfig.getConfigurationSection("placed_blocks");
        if (placedSection == null) {
            LogManager.debug("No placed blocks found in config");
        } else {
            for(String locationString : placedSection.getKeys(false)) {
                String blockId = placedSection.getString(locationString);
                if (blockId != null && this.customBlocks.containsKey(blockId)) {
                    this.placedBlocks.put(locationString, blockId);
                }
            }

            LogManager.info("Loaded " + this.placedBlocks.size() + " placed custom blocks");
        }
    }

    public void savePlacedBlocks() {
        FileConfiguration blockConfig = this.plugin.getConfigManager().getConfig(ConfigType.BLOCK_COMMANDS);
        blockConfig.set("placed_blocks", (Object)null);

        for(Map.Entry<String, String> entry : this.placedBlocks.entrySet()) {
            blockConfig.set("placed_blocks." + (String)entry.getKey(), entry.getValue());
        }

        this.plugin.getConfigManager().saveConfig(ConfigType.BLOCK_COMMANDS);
        LogManager.info("Saved " + this.placedBlocks.size() + " placed custom blocks");
    }

    public ItemStack createBlockItem(String blockId) {
        CustomBlock customBlock = (CustomBlock)this.customBlocks.get(blockId);
        if (customBlock == null) {
            return null;
        } else {
            ItemStack item = new ItemStack(customBlock.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            } else {
                meta.setDisplayName(MessageUtils.translateColors(customBlock.getName()));
                List<String> lore = new ArrayList(customBlock.getLore());
                lore.add("§8Custom Block");
                lore.add("§8ID: " + blockId);
                meta.setLore(MessageUtils.translateColors(lore));
                if (customBlock.getCustomModelData() != null) {
                    meta.setCustomModelData(customBlock.getCustomModelData());
                }

                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                NBTUtils.setString(meta, "ghastutils.custom_block_id", blockId);
                item.setItemMeta(meta);
                ItemUtils.addGlow(item);
                return item;
            }
        }
    }

    public boolean placeCustomBlock(Location location, String blockId) {
        if (location != null && blockId != null) {
            CustomBlock customBlock = (CustomBlock)this.customBlocks.get(blockId);
            if (customBlock == null) {
                return false;
            } else {
                Block block = location.getBlock();
                block.setType(customBlock.getMaterial());
                String locationString = this.serializeLocation(location);
                this.placedBlocks.put(locationString, blockId);
                this.savePlacedBlocks();
                return true;
            }
        } else {
            return false;
        }
    }

    public String removeCustomBlock(Location location) {
        if (location == null) {
            return null;
        } else {
            String locationString = this.serializeLocation(location);
            String blockId = (String)this.placedBlocks.remove(locationString);
            if (blockId != null) {
                this.savePlacedBlocks();
            }

            return blockId;
        }
    }

    public boolean isCustomBlock(Location location) {
        if (location == null) {
            return false;
        } else {
            String locationString = this.serializeLocation(location);
            return this.placedBlocks.containsKey(locationString);
        }
    }

    public String getCustomBlockId(Location location) {
        if (location == null) {
            return null;
        } else {
            String locationString = this.serializeLocation(location);
            return (String)this.placedBlocks.get(locationString);
        }
    }

    public boolean hasPermission(Player player, String blockId) {
        CustomBlock customBlock = (CustomBlock)this.customBlocks.get(blockId);
        if (customBlock == null) {
            return false;
        } else {
            String permission = customBlock.getPermission();
            return permission == null || permission.isEmpty() || player.hasPermission(permission);
        }
    }

    public boolean isOnCooldown(Player player, String blockId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)this.playerCooldowns.get(uuid);
        if (cooldowns == null) {
            return false;
        } else {
            Long cooldownEnd = (Long)cooldowns.get(blockId);
            return cooldownEnd != null && cooldownEnd > System.currentTimeMillis();
        }
    }

    public long getRemainingCooldown(Player player, String blockId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)this.playerCooldowns.get(uuid);
        if (cooldowns == null) {
            return 0L;
        } else {
            Long cooldownEnd = (Long)cooldowns.get(blockId);
            return cooldownEnd != null && cooldownEnd > System.currentTimeMillis() ? cooldownEnd - System.currentTimeMillis() : 0L;
        }
    }

    private void setCooldown(Player player, String blockId, CustomBlock customBlock) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)this.playerCooldowns.computeIfAbsent(uuid, (k) -> new ConcurrentHashMap());
        long cooldownMs = customBlock.getCooldown();
        if (cooldownMs <= 0L) {
            cooldownMs = this.defaultCooldown;
        }

        if (cooldownMs > 0L) {
            long cooldownEnd = System.currentTimeMillis() + cooldownMs;
            cooldowns.put(blockId, cooldownEnd);
        }

    }

    private String formatCooldown(long milliseconds) {
        if (milliseconds < 1000L) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000L) {
            return String.format("%.1fs", (double)milliseconds / (double)1000.0F);
        } else {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(minutes);
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public void executeBlockCommands(Player player, Location location, String action) {
        if (player != null && location != null && action != null) {
            String blockId = this.getCustomBlockId(location);
            if (blockId != null) {
                CustomBlock customBlock = (CustomBlock)this.customBlocks.get(blockId);
                if (customBlock != null) {
                    if (customBlock.getPermission() != null && !customBlock.getPermission().isEmpty() && !player.hasPermission(customBlock.getPermission())) {
                        MessageUtils.sendMessage(player, "block.permission-required");
                    } else if (this.isOnCooldown(player, blockId)) {
                        long remaining = this.getRemainingCooldown(player, blockId);
                        Map<String, String> placeholders = MessageUtils.placeholders();
                        placeholders.put("time", this.formatCooldown(remaining));
                        placeholders.put("block", customBlock.getName());
                        MessageUtils.sendMessage(player, "block.cooldown", placeholders);
                    } else {
                        List<String> commands = (List)customBlock.getCommands().get(action);
                        if (commands == null || commands.isEmpty()) {
                            commands = (List)customBlock.getCommands().get("ANY");
                            if (commands == null || commands.isEmpty()) {
                                return;
                            }
                        }

                        this.setCooldown(player, blockId, customBlock);

                        for(String command : commands) {
                            this.executeCommand(player, command, location);
                        }

                    }
                }
            }
        }
    }

    private void executeCommand(Player player, String command, Location location) {
        String[] parts = command.split(": ", 2);
        if (parts.length != 2) {
            LogManager.warning("Invalid command format: " + command);
        } else {
            String executor = parts[0].toLowerCase();
            String cmd = parts[1];
            cmd = cmd.replace("%player%", player.getName()).replace("%world%", location.getWorld().getName()).replace("%x%", String.valueOf(location.getBlockX())).replace("%y%", String.valueOf(location.getBlockY())).replace("%z%", String.valueOf(location.getBlockZ())).replace("%uuid%", player.getUniqueId().toString());
            switch (executor) {
                case "console":
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    break;
                case "player":
                    player.performCommand(cmd);
                    break;
                case "op":
                    boolean wasOp = player.isOp();

                    try {
                        player.setOp(true);
                        player.performCommand(cmd);
                        break;
                    } finally {
                        if (!wasOp) {
                            player.setOp(false);
                        }

                    }
                case "message":
                    player.sendMessage(MessageUtils.translateColors(cmd));
                    break;
                default:
                    LogManager.warning("Unknown command executor: " + executor);
            }

        }
    }

    private String serializeLocation(Location location) {
        String var10000 = location.getWorld().getName();
        return var10000 + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public Location deserializeLocation(String locationString) {
        String[] parts = locationString.split(",");
        if (parts.length != 4) {
            return null;
        } else {
            try {
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                return new Location(Bukkit.getWorld(worldName), (double)x, (double)y, (double)z);
            } catch (NullPointerException | NumberFormatException var7) {
                LogManager.warning("Failed to deserialize location: " + locationString);
                return null;
            }
        }
    }

    public CustomBlock getCustomBlock(String blockId) {
        return (CustomBlock)this.customBlocks.get(blockId);
    }

    public boolean hasCustomBlock(String blockId) {
        return this.customBlocks.containsKey(blockId);
    }

    public Map<String, CustomBlock> getCustomBlocks() {
        return Collections.unmodifiableMap(this.customBlocks);
    }

    public Map<String, String> getPlacedBlocks() {
        return Collections.unmodifiableMap(this.placedBlocks);
    }

    public long getDefaultCooldown() {
        return this.defaultCooldown;
    }

    public void setDefaultCooldown(long cooldownMs) {
        this.defaultCooldown = cooldownMs;
        FileConfiguration config = this.plugin.getConfigManager().getConfig(ConfigType.BLOCK_COMMANDS);
        config.set("settings.default-cooldown", cooldownMs);
        this.plugin.getConfigManager().saveConfig(ConfigType.BLOCK_COMMANDS);
    }

    public void reload() {
        this.customBlocks.clear();
        this.placedBlocks.clear();
        this.playerCooldowns.clear();
        this.loadCustomBlocks();
        this.loadPlacedBlocks();
    }
}
