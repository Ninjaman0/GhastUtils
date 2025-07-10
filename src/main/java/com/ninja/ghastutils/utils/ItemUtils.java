
package com.ninja.ghastutils.utils;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.sell.SellableItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {
    private static final GhastUtils plugin = GhastUtils.getInstance();

    public static boolean isSimilar(ItemStack item, SellableItem sellableItem, boolean fuzzyMatch) {
        if (item != null && item.getType() == sellableItem.getMaterial()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            } else if (sellableItem.getCustomModelData() == -1 || meta.hasCustomModelData() && meta.getCustomModelData() == sellableItem.getCustomModelData()) {
                if (!sellableItem.getName().isEmpty()) {
                    if (!meta.hasDisplayName()) {
                        return false;
                    }

                    String itemName = meta.getDisplayName();
                    String requiredName = sellableItem.getName();
                    if (fuzzyMatch) {
                        String strippedItemName = MessageUtils.stripColors(itemName).toLowerCase();
                        String strippedRequiredName = MessageUtils.stripColors(requiredName).toLowerCase();
                        if (!strippedItemName.contains(strippedRequiredName)) {
                            return false;
                        }
                    } else {
                        String translatedRequired = MessageUtils.translateColors(requiredName);
                        if (!itemName.equals(translatedRequired)) {
                            return false;
                        }
                    }
                }

                if (!sellableItem.getLore().isEmpty()) {
                    if (!meta.hasLore() || meta.getLore() == null) {
                        return false;
                    }

                    List<String> itemLore = meta.getLore();
                    List<String> sellableLore = sellableItem.getLore();
                    List<String> translatedSellableLore = MessageUtils.translateColors(sellableLore);
                    if (fuzzyMatch) {
                        for(String requiredLine : translatedSellableLore) {
                            String stripped = MessageUtils.stripColors(requiredLine).toLowerCase();
                            boolean found = false;

                            for(String itemLine : itemLore) {
                                String strippedItemLine = MessageUtils.stripColors(itemLine).toLowerCase();
                                if (strippedItemLine.contains(stripped)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                return false;
                            }
                        }
                    } else {
                        for(String line : translatedSellableLore) {
                            if (!itemLore.contains(line)) {
                                return false;
                            }
                        }
                    }
                }

                if (sellableItem.hasNbtData()) {
                    Map<String, String> requiredNbt = sellableItem.getNbtData();

                    for(Map.Entry<String, String> entry : requiredNbt.entrySet()) {
                        String key = (String)entry.getKey();
                        String value = (String)entry.getValue();
                        if (!NBTUtils.hasString(meta, key) || !NBTUtils.getString(meta, key).equals(value)) {
                            return false;
                        }
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isSimilar(ItemStack item, SellableItem sellableItem) {
        return isSimilar(item, sellableItem, false);
    }

    public static boolean isCustomItem(ItemStack item, CustomItem customItem) {
        if (item != null && item.getType() == customItem.getMaterial()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            } else if (NBTUtils.hasString(meta, "ghastutils.custom_item_id")) {
                String id = NBTUtils.getString(meta, "ghastutils.custom_item_id");
                return id.equals(customItem.getId());
            } else if (!meta.hasDisplayName()) {
                return false;
            } else {
                String itemName = meta.getDisplayName();
                String customName = MessageUtils.translateColors(customItem.getName());
                if (!itemName.equals(customName)) {
                    return false;
                } else if (customItem.getCustomModelData() == -1 || meta.hasCustomModelData() && meta.getCustomModelData() == customItem.getCustomModelData()) {
                    if (meta.hasLore()) {
                        List<String> lore = meta.getLore();
                        String idLine = "§8ID: " + customItem.getId();
                        return lore.stream().anyMatch((line) -> line.equals(idLine));
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static ItemStack setNBTString(ItemStack item, String key, String value) {
        if (item == null) {
            return null;
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return item;
            } else {
                NBTUtils.setString(meta, key, value);
                item.setItemMeta(meta);
                return item;
            }
        }
    }

    public static String getNBTString(ItemStack item, String key) {
        if (item == null) {
            return null;
        } else {
            ItemMeta meta = item.getItemMeta();
            return meta == null ? null : NBTUtils.getString(meta, key);
        }
    }

    public static void addGlow(ItemStack item) {
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                item.addUnsafeEnchantment(Enchantment.LUCK_OF_THE_SEA, 1);
                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                item.setItemMeta(meta);
            }
        }
    }

    public static void setCustomModelData(ItemMeta meta, int value) {
        if (meta != null) {
            meta.setCustomModelData(value);
        }
    }

    public static Integer getCustomModelData(ItemMeta meta) {
        return meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : null;
    }

    public static boolean hasCustomModelData(ItemMeta meta) {
        return meta != null && meta.hasCustomModelData();
    }

    public static boolean isAir(Material material) {
        return material == null || material.isAir();
    }

    public static void setCustomModelData(ItemStack item, int value) {
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(value);
                item.setItemMeta(meta);
            }
        }
    }

    public static Integer getCustomModelData(ItemStack item) {
        if (item == null) {
            return null;
        } else {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        }
    }

    public static boolean hasCustomModelData(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.hasCustomModelData();
        }
    }

    public static ItemBuilder builder(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder builder(ItemStack item) {
        return new ItemBuilder(item);
    }

    public static Map<Integer, ItemStack> addItems(Inventory inventory, ItemStack... items) {
        Map<Integer, ItemStack> leftover = new HashMap();
        if (items == null) {
            return leftover;
        } else {
            for(int i = 0; i < items.length; ++i) {
                ItemStack item = items[i];
                if (item != null && item.getType() != Material.AIR) {
                    int amountLeft = item.getAmount();
                    boolean isCustomItem = isCustomNonStackableItem(item);
                    if (!isCustomItem) {
                        for(int j = 0; j < inventory.getSize(); ++j) {
                            ItemStack existingItem = inventory.getItem(j);
                            if (existingItem != null && canStack(existingItem, item)) {
                                int space = existingItem.getMaxStackSize() - existingItem.getAmount();
                                if (space > 0) {
                                    int toAdd = Math.min(amountLeft, space);
                                    existingItem.setAmount(existingItem.getAmount() + toAdd);
                                    amountLeft -= toAdd;
                                    if (amountLeft <= 0) {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (amountLeft > 0) {
                        ItemStack clone = item.clone();

                        while(amountLeft > 0) {
                            int maxStackSize = clone.getMaxStackSize();
                            int toAdd = Math.min(amountLeft, maxStackSize);
                            clone.setAmount(toAdd);
                            int firstEmpty = inventory.firstEmpty();
                            if (firstEmpty == -1) {
                                clone.setAmount(amountLeft);
                                leftover.put(i, clone);
                                break;
                            }

                            inventory.setItem(firstEmpty, clone.clone());
                            amountLeft -= toAdd;
                            if (isCustomItem) {
                                clone = item.clone();
                            }
                        }
                    }
                }
            }

            return leftover;
        }
    }

    public static boolean canStack(ItemStack item1, ItemStack item2) {
        if (item1 != null && item2 != null) {
            if (item1.getType() != item2.getType()) {
                return false;
            } else {
                ItemMeta meta1 = item1.getItemMeta();
                ItemMeta meta2 = item2.getItemMeta();
                if (meta1 == null && meta2 == null) {
                    return true;
                } else if (meta1 != null && meta2 != null) {
                    if (isCustomItemMeta(meta1) || isCustomItemMeta(meta2)) {
                        String id1 = NBTUtils.getString(meta1, "ghastutils.custom_item_id");
                        String id2 = NBTUtils.getString(meta2, "ghastutils.custom_item_id");
                        if (id1 == null || id2 == null || !id1.equals(id2)) {
                            return false;
                        }

                        CustomItem customItem = plugin.getCraftingManager().getCustomItem(id1);
                        if (customItem != null && !customItem.isStackable()) {
                            return false;
                        }
                    }

                    if (meta1.hasDisplayName() != meta2.hasDisplayName()) {
                        return false;
                    } else if (meta1.hasDisplayName() && !meta1.getDisplayName().equals(meta2.getDisplayName())) {
                        return false;
                    } else if (meta1.hasLore() != meta2.hasLore()) {
                        return false;
                    } else if (meta1.hasLore() && !meta1.getLore().equals(meta2.getLore())) {
                        return false;
                    } else if (!meta1.getEnchants().equals(meta2.getEnchants())) {
                        return false;
                    } else if (meta1.hasCustomModelData() != meta2.hasCustomModelData()) {
                        return false;
                    } else if (meta1.hasCustomModelData() && meta1.getCustomModelData() != meta2.getCustomModelData()) {
                        return false;
                    } else if (!meta1.getItemFlags().equals(meta2.getItemFlags())) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean isSameItem(ItemStack a, ItemStack b) {
        if (a != null && b != null && a.getType() == b.getType()) {
            ItemMeta metaA = a.getItemMeta();
            ItemMeta metaB = b.getItemMeta();
            if (metaA != null && metaB != null) {
                if (!Objects.equals(metaA.getDisplayName(), metaB.getDisplayName())) {
                    return false;
                } else if (!Objects.equals(metaA.getLore(), metaB.getLore())) {
                    return false;
                } else if (metaA.hasCustomModelData() != metaB.hasCustomModelData()) {
                    return false;
                } else {
                    return !metaA.hasCustomModelData() || metaA.getCustomModelData() == metaB.getCustomModelData();
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean isCustomItemMeta(ItemMeta meta) {
        return NBTUtils.hasString(meta, "ghastutils.custom_item_id");
    }

    private static boolean isCustomNonStackableItem(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            } else if (!NBTUtils.hasString(meta, "ghastutils.custom_item_id")) {
                return false;
            } else {
                String id = NBTUtils.getString(meta, "ghastutils.custom_item_id");
                CustomItem customItem = plugin.getCraftingManager().getCustomItem(id);
                return customItem != null && !customItem.isStackable();
            }
        }
    }

    public static class ItemBuilder {
        private ItemStack item;
        private ItemMeta meta;

        public ItemBuilder(Material material) {
            this.item = new ItemStack(material);
            this.meta = this.item.getItemMeta();
        }

        public ItemBuilder(ItemStack item) {
            this.item = item.clone();
            this.meta = this.item.getItemMeta();
        }

        public ItemBuilder name(String name) {
            if (this.meta != null) {
                this.meta.setDisplayName(MessageUtils.translateItemColors(name));
            }

            return this;
        }

        public ItemBuilder lore(String... lore) {
            if (this.meta != null) {
                this.meta.setLore((List)Arrays.stream(lore).map(MessageUtils::translateItemColors).collect(Collectors.toList()));
            }

            return this;
        }

        public ItemBuilder lore(List<String> lore) {
            if (this.meta != null) {
                this.meta.setLore((List)lore.stream().map(MessageUtils::translateColors).collect(Collectors.toList()));
            }

            return this;
        }

        public ItemBuilder addLore(String... lore) {
            if (this.meta != null) {
                List<String> existingLore = this.meta.getLore();
                if (existingLore == null) {
                    existingLore = new ArrayList();
                }

                existingLore.addAll((Collection)Arrays.stream(lore).map(MessageUtils::translateColors).collect(Collectors.toList()));
                this.meta.setLore(existingLore);
            }

            return this;
        }

        public ItemBuilder amount(int amount) {
            this.item.setAmount(amount);
            return this;
        }

        public ItemBuilder modelData(int modelData) {
            if (this.meta != null) {
                this.meta.setCustomModelData(modelData);
            }

            return this;
        }

        public ItemBuilder enchant(Enchantment enchantment, int level) {
            if (this.meta != null) {
                this.meta.addEnchant(enchantment, level, true);
            }

            return this;
        }

        public ItemBuilder flag(ItemFlag... flags) {
            if (this.meta != null) {
                this.meta.addItemFlags(flags);
            }

            return this;
        }

        public ItemBuilder nbt(String key, String value) {
            if (this.meta != null) {
                NBTUtils.setString(this.meta, key, value);
            }

            return this;
        }

        public ItemBuilder glow() {
            if (this.meta != null) {
                this.item.addUnsafeEnchantment(Enchantment.LUCK_OF_THE_SEA, 1);
                this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }

            return this;
        }

        public ItemBuilder customItem(String id) {
            if (this.meta != null) {
                NBTUtils.setString(this.meta, "ghastutils.custom_item_id", id);
                List<String> lore = this.meta.getLore();
                if (lore == null) {
                    lore = new ArrayList();
                }

                String idLine = "§8ID: " + id;
                if (lore.stream().noneMatch((line) -> line.equals(idLine))) {
                    lore.add(idLine);
                    this.meta.setLore(lore);
                }
            }

            return this;
        }

        public ItemStack build() {
            if (this.meta != null) {
                this.item.setItemMeta(this.meta);
            }

            return this.item;
        }
    }
}
