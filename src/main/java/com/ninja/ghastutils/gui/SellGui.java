
package com.ninja.ghastutils.gui;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.sell.SellResult;
import com.ninja.ghastutils.sell.SellSession;
import com.ninja.ghastutils.utils.EconomyUtil;
import com.ninja.ghastutils.utils.MessageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SellGui {
    private final GhastUtils plugin;
    private final Player player;
    public final Inventory inventory;
    private final SellSession session;
    private double currentValue = (double)0.0F;
    private int itemCount = 0;
    private static final int[] SELL_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    public SellGui(GhastUtils plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        Map<String, String> placeholders = MessageUtils.placeholders();
        String title = MessageUtils.getMessage("sell.gui-title", placeholders);
        if (title == null || title.isEmpty()) {
            title = "§6Sell Items";
        }

        this.inventory = Bukkit.createInventory((InventoryHolder)null, 54, title);
        plugin.getSellManager().startSellSession(player);
        this.session = plugin.getSellManager().getSellSession(player);
        plugin.getGuiManager().registerSellGui(player.getUniqueId(), this);
        this.initializeGui();
    }

    private void initializeGui() {
        for(int i = 0; i < 9; ++i) {
            this.inventory.setItem(i, this.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        for(int i = 45; i < 54; ++i) {
            this.inventory.setItem(i, this.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        for(int i = 0; i < 6; ++i) {
            this.inventory.setItem(i * 9, this.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            this.inventory.setItem(i * 9 + 8, this.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        Map<String, String> placeholders = MessageUtils.placeholders();
        String sellButtonText = MessageUtils.getMessage("sell.gui-sell-button", placeholders);
        if (sellButtonText == null || sellButtonText.isEmpty()) {
            sellButtonText = "§a§lSell Items";
        }

        this.inventory.setItem(49, this.createGuiItem(Material.EMERALD, sellButtonText, "§7Click to sell all items in the GUI"));
        String cancelButtonText = MessageUtils.getMessage("sell.gui-cancel-button", placeholders);
        if (cancelButtonText == null || cancelButtonText.isEmpty()) {
            cancelButtonText = "§c§lCancel";
        }

        this.inventory.setItem(45, this.createGuiItem(Material.BARRIER, cancelButtonText, "§7Click to cancel and return items"));
        this.inventory.setItem(4, this.createGuiItem(Material.PAPER, "§6§lSell Items", "§7Place items in the empty slots to sell them"));
        double multiplier = this.plugin.getMultiplierManager().getTotalMultiplier(this.player.getUniqueId());
        placeholders.put("multiplier", String.format("%.2f", multiplier));
        this.inventory.setItem(47, this.createGuiItem(Material.EXPERIENCE_BOTTLE, "§6§lMultiplier: §f" + String.format("%.2f", multiplier) + "x"));
        this.updateTotalValue();
    }

    private void updateTotalValue() {
        double multiplier = this.plugin.getMultiplierManager().getTotalMultiplier(this.player.getUniqueId());
        Inventory var10000 = this.inventory;
        Material var10003 = Material.GOLD_INGOT;
        String var10004 = "§6§lTotal Value: §f" + EconomyUtil.formatCurrency(this.currentValue);
        String[] var10005 = new String[]{"§7Items: §f" + this.itemCount, null};
        Object[] var10009 = new Object[]{multiplier};
        var10005[1] = "§7Multiplier: §f" + String.format("%.2f", var10009) + "x";
        var10000.setItem(51, this.createGuiItem(var10003, var10004, var10005));
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public void open() {
        this.player.openInventory(this.inventory);
    }

    public boolean isInSellSlot(int slot) {
        for(int sellSlot : SELL_SLOTS) {
            if (sellSlot == slot) {
                return true;
            }
        }

        return false;
    }

    public int[] getSellSlots() {
        return SELL_SLOTS;
    }

    public void calculateTotalValue() {
        this.currentValue = (double)0.0F;
        this.itemCount = 0;
        List<ItemStack> items = new ArrayList();

        for(int slot : SELL_SLOTS) {
            ItemStack item = this.inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
                this.itemCount += item.getAmount();
            }
        }

        double multiplier = this.plugin.getMultiplierManager().getTotalMultiplier(this.player.getUniqueId());

        for(ItemStack item : items) {
            double basePrice = this.plugin.getSellManager().getItemSellPrice(item);
            this.currentValue += basePrice * multiplier * (double)item.getAmount();
        }

        this.updateTotalValue();
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 54 && (slot < 9 || slot > 44 || slot % 9 == 0 || slot % 9 == 8)) {
            event.setCancelled(true);
            if (slot == 49) {
                this.handleSellClick();
                return;
            }

            if (slot == 45) {
                this.handleCancelClick();
                return;
            }
        }

        if (this.isInSellSlot(slot)) {
            Bukkit.getScheduler().runTaskLater(this.plugin, this::calculateTotalValue, 1L);
        } else {
            if (slot < this.inventory.getSize()) {
                event.setCancelled(true);
            }

            if (event.isShiftClick() && slot >= this.inventory.getSize()) {
                Bukkit.getScheduler().runTaskLater(this.plugin, this::calculateTotalValue, 1L);
            }

        }
    }

    public void handleSellClick() {
        List<ItemStack> itemsToSell = new ArrayList();

        for(int slot : SELL_SLOTS) {
            ItemStack item = this.inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                itemsToSell.add(item.clone());
            }
        }

        if (itemsToSell.isEmpty()) {
            MessageUtils.sendMessage(this.player, "sell.no-items");
        } else {
            SellResult result = this.plugin.getSellManager().sellItems(this.player, itemsToSell);
            if (result.getItemsSold() > 0) {
                double totalPrice = result.getTotalPrice();
                Map<String, String> placeholders = MessageUtils.placeholders();
                placeholders.put("amount", String.valueOf(result.getItemsSold()));
                placeholders.put("price", EconomyUtil.formatCurrency(totalPrice));
                MessageUtils.sendMessage(this.player, "sell.sold-header", placeholders);
                placeholders.put("multiplier", String.format("%.2f", result.getMultiplier()));
                MessageUtils.sendMessage(this.player, "sell.sold-multiplier", placeholders);

                for(Map.Entry<String, Integer> entry : result.getSoldItems().entrySet()) {
                    placeholders.put("amount", String.valueOf(entry.getValue()));
                    placeholders.put("item", (String)entry.getKey());
                    MessageUtils.sendMessage(this.player, "sell.sold-item", placeholders);
                }

                for(int slot : SELL_SLOTS) {
                    this.inventory.setItem(slot, (ItemStack)null);
                }

                this.currentValue = (double)0.0F;
                this.itemCount = 0;
                this.updateTotalValue();

                try {
                    this.player.playSound(this.player.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 1.0F, 1.0F);
                } catch (Exception var11) {
                    try {
                        this.player.playSound(this.player.getLocation(), Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP"), 1.0F, 1.0F);
                    } catch (Exception var10) {
                    }
                }
            } else {
                MessageUtils.sendMessage(this.player, "sell.no-sellable-items");
            }

        }
    }

    public void handleCancelClick() {
        for(int slot : SELL_SLOTS) {
            ItemStack item = this.inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                if (this.player.getInventory().firstEmpty() != -1) {
                    this.player.getInventory().addItem(new ItemStack[]{item});
                } else {
                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), item);
                    MessageUtils.sendMessage(this.player, "sell.inventory-full");
                }

                this.inventory.setItem(slot, (ItemStack)null);
            }
        }

        this.player.closeInventory();
    }

    public void onClose() {
        for(int slot : SELL_SLOTS) {
            ItemStack item = this.inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                if (this.player.getInventory().firstEmpty() != -1) {
                    this.player.getInventory().addItem(new ItemStack[]{item});
                } else {
                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), item);
                    MessageUtils.sendMessage(this.player, "sell.inventory-full");
                }
            }
        }

        this.plugin.getSellManager().endSellSession(this.player);
    }
}
