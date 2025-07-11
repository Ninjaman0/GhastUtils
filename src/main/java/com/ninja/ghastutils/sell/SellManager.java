package com.ninja.ghastutils.sell;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.config.ConfigType;
import com.ninja.ghastutils.utils.EconomyUtil;
import com.ninja.ghastutils.utils.ItemUtils;
import com.ninja.ghastutils.utils.LogManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellManager {
    private final GhastUtils plugin;
    private final Map<String, SellableItem> sellableItems;
    private final Map<Player, SellSession> activeSellSessions;

    public SellManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.sellableItems = new ConcurrentHashMap<String, SellableItem>();
        this.activeSellSessions = new ConcurrentHashMap<Player, SellSession>();
        this.loadSellableItems();
    }

    private void loadSellableItems() {
        FileConfiguration sellConfig = this.plugin.getConfigManager().getConfig(ConfigType.SELL);
        ConfigurationSection itemsSection = sellConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            this.plugin.getLogger().warning("No sellable items found in sell.yml");
        } else {
            for(String itemId : itemsSection.getKeys(false)) {
                try {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection != null) {
                        String itemName = itemSection.getString("itemname", "");
                        Material material = Material.valueOf(itemSection.getString("material", "STONE").toUpperCase());
                        double basePrice = itemSection.getDouble("base_price", 0.0);
                        String permission = itemSection.getString("permission", null);
                        List<String> lore = new ArrayList<String>();
                        if (itemSection.contains("lore")) {
                            lore = itemSection.getStringList("lore");
                        }

                        int customModelData = -1;
                        if (itemSection.contains("custom_model_data")) {
                            customModelData = itemSection.getInt("custom_model_data");
                        }

                        SellableItem sellableItem = new SellableItem(itemId, itemName, material, basePrice, lore, customModelData);
                        if (permission != null && !permission.isEmpty() && !permission.equalsIgnoreCase("NONE")) {
                            sellableItem.setPermission(permission);
                        }

                        if (itemSection.contains("nbt_data")) {
                            ConfigurationSection nbtSection = itemSection.getConfigurationSection("nbt_data");
                            if (nbtSection != null) {
                                Map<String, String> nbtData = new HashMap<String, String>();

                                for(String key : nbtSection.getKeys(false)) {
                                    nbtData.put(key, nbtSection.getString(key));
                                }

                                sellableItem.setNbtData(nbtData);
                            }
                        }

                        this.sellableItems.put(itemId, sellableItem);
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().log(Level.WARNING, "Error loading sellable item " + itemId, e);
                }
            }

            this.plugin.getLogger().info("Loaded " + this.sellableItems.size() + " sellable items");
        }
    }

    public void saveItems() {
        FileConfiguration sellConfig = this.plugin.getConfigManager().getConfig(ConfigType.SELL);
        sellConfig.set("items", null);

        for(Map.Entry<String, SellableItem> entry : this.sellableItems.entrySet()) {
            String itemId = entry.getKey();
            SellableItem item = entry.getValue();
            String path = "items." + itemId;
            sellConfig.set(path + ".itemname", item.getName());
            sellConfig.set(path + ".material", item.getMaterial().toString());
            sellConfig.set(path + ".base_price", item.getBasePrice());
            if (!item.getLore().isEmpty()) {
                sellConfig.set(path + ".lore", item.getLore());
            }

            if (item.getCustomModelData() != -1) {
                sellConfig.set(path + ".custom_model_data", item.getCustomModelData());
            }

            if (item.getPermission() != null && !item.getPermission().isEmpty()) {
                sellConfig.set(path + ".permission", item.getPermission());
            }

            if (item.hasNbtData()) {
                for(Map.Entry<String, String> nbtEntry : item.getNbtData().entrySet()) {
                    sellConfig.set(path + ".nbt_data." + nbtEntry.getKey(), nbtEntry.getValue());
                }
            }
        }

        this.plugin.getConfigManager().saveConfig(ConfigType.SELL);
    }

    public void registerItem(String itemId, ItemStack item, double basePrice) {
        if (item != null && item.hasItemMeta()) {
            String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().toString();
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : new ArrayList<String>();
            int customModelData = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : -1;
            SellableItem sellableItem = new SellableItem(itemId, itemName, item.getType(), basePrice, lore, customModelData);
            this.sellableItems.put(itemId, sellableItem);
            this.saveItems();
        } else {
            this.plugin.getLogger().warning("Attempted to register null or invalid item as " + itemId);
        }
    }

    public SellResult sellItems(Player player, List<ItemStack> items) {
        double totalPrice = 0.0;
        int itemsSold = 0;
        Map<String, Integer> soldItems = new HashMap<String, Integer>();
        double multiplier = this.plugin.getMultiplierManager().getTotalMultiplier(player.getUniqueId());

        for(ItemStack item : new ArrayList<ItemStack>(items)) {
            if (item != null && !item.getType().isAir()) {
                SellableItem sellableItem = this.findMatchingSellableItem(item);
                if (sellableItem != null && (sellableItem.getPermission() == null || sellableItem.getPermission().isEmpty() || player.hasPermission(sellableItem.getPermission()))) {
                    double price = sellableItem.getBasePrice() * multiplier * (double)item.getAmount();
                    totalPrice += price;
                    String itemId = sellableItem.getId();
                    soldItems.put(itemId, soldItems.getOrDefault(itemId, 0) + item.getAmount());
                    item.setAmount(0);
                    ++itemsSold;
                }
            }
        }

        if (totalPrice > 0.0) {
            if (EconomyUtil.isInitialized()) {
                EconomyUtil.deposit(player, totalPrice, "sell");
                LogManager.transaction("SELL", player.getName(), totalPrice, "SellGUI", "SUCCESS");
            } else {
                LogManager.error("Failed to deposit money for player " + player.getName() + ": EconomyUtil not initialized");
                player.sendMessage("§cError processing transaction. Please contact an administrator.");
            }
        }

        return new SellResult(totalPrice, itemsSold, soldItems, multiplier);
    }

    private SellableItem findMatchingSellableItem(ItemStack item) {
        if (item == null) {
            return null;
        } else {
            for(SellableItem sellableItem : this.sellableItems.values()) {
                if (ItemUtils.isSimilar(item, sellableItem, false)) {
                    return sellableItem;
                }
            }

            for(SellableItem sellableItem : this.sellableItems.values()) {
                if (ItemUtils.isSimilar(item, sellableItem, true)) {
                    return sellableItem;
                }
            }

            return null;
        }
    }

    public double getItemSellPrice(ItemStack item) {
        if (item == null) {
            return 0.0;
        } else {
            SellableItem sellableItem = this.findMatchingSellableItem(item);
            return sellableItem != null ? sellableItem.getBasePrice() : 0.0;
        }
    }

    public Map<String, SellableItem> getSellableItems() {
        return new HashMap<String, SellableItem>(this.sellableItems);
    }

    public SellableItem getSellableItem(String id) {
        return this.sellableItems.get(id);
    }

    public void removeSellableItem(String id) {
        this.sellableItems.remove(id);
        this.saveItems();
    }

    public void startSellSession(Player player) {
        SellSession session = new SellSession();
        this.activeSellSessions.put(player, session);
    }

    public SellSession getSellSession(Player player) {
        return this.activeSellSessions.get(player);
    }

    public void endSellSession(Player player) {
        this.activeSellSessions.remove(player);
    }

    public double getSellPrice(String itemId, Player player) {
        SellableItem item = this.sellableItems.get(itemId);
        if (item != null) {
            double multiplier = this.plugin.getMultiplierManager().getTotalMultiplier(player.getUniqueId());
            return item.getBasePrice() * multiplier;
        } else {
            return 0.0;
        }
    }

    public void reload() {
        this.sellableItems.clear();
        this.loadSellableItems();
    }
}