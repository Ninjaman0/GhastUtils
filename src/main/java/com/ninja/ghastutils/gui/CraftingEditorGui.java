
package com.ninja.ghastutils.gui;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.crafting.RecipeIngredient;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CraftingEditorGui {
    private final GhastUtils plugin;
    private final Player player;
    private final String itemId;
    private final Inventory inventory;
    private final int[] RECIPE_SLOTS = new int[]{11, 12, 13, 20, 21, 22, 29, 30, 31};
    private final int RESULT_SLOT = 25;
    private boolean isSaving = false;

    public CraftingEditorGui(GhastUtils plugin, Player player, String itemId) {
        this.plugin = plugin;
        this.player = player;
        this.itemId = itemId;
        this.inventory = Bukkit.createInventory((InventoryHolder)null, 54, "Recipe Editor: " + itemId);
        plugin.getGuiListener().registerCraftingEditorGui(player.getUniqueId(), this);
        this.initializeGui();
    }

    private void initializeGui() {
        for(int i = 0; i < this.inventory.getSize(); ++i) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8 || i == 10 || i == 19 || i == 28 || i == 37 || i == 15 || i == 16 || i == 33 || i == 34 || i == 43 || i == 42 || i == 38 || i == 39 || i == 40 || i == 41 || i == 32 || i == 23 || i == 14) {
                this.inventory.setItem(i, this.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
        }

        this.inventory.setItem(25, this.plugin.getCraftingManager().createItemStack(this.itemId, 1));
        this.inventory.setItem(24, this.createGuiItem(Material.ARROW, "§e§l→"));
        this.inventory.setItem(49, this.createGuiItem(Material.EMERALD, "§a§lSave Recipe", "§7Click to save the recipe"));
        this.inventory.setItem(48, this.createGuiItem(Material.BARRIER, "§c§lClear Recipe", "§7Click to clear the recipe"));
        CustomItem customItem = this.plugin.getCraftingManager().getCustomItem(this.itemId);
        if (customItem != null && !customItem.getRecipe().isEmpty()) {
            Map<Integer, RecipeIngredient> recipe = customItem.getRecipe();

            for(int i = 0; i < this.RECIPE_SLOTS.length; ++i) {
                RecipeIngredient ingredient = (RecipeIngredient)recipe.get(i + 1);
                if (ingredient != null) {
                    ItemStack item;
                    if (ingredient.isMaterial()) {
                        item = new ItemStack(ingredient.getMaterial(), ingredient.getAmount());
                    } else {
                        item = this.plugin.getCraftingManager().createItemStack(ingredient.getCustomItemId(), ingredient.getAmount());
                    }

                    this.inventory.setItem(this.RECIPE_SLOTS[i], item);
                }
            }
        }

    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
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

    public boolean isRecipeSlot(int slot) {
        for(int recipeSlot : this.RECIPE_SLOTS) {
            if (slot == recipeSlot) {
                return true;
            }
        }

        return false;
    }

    public void handleSaveClick() {
        if (!this.isSaving) {
            this.isSaving = true;
            Map<Integer, ItemStack> recipe = new HashMap();

            for(int i = 0; i < this.RECIPE_SLOTS.length; ++i) {
                ItemStack item = this.inventory.getItem(this.RECIPE_SLOTS[i]);
                if (item != null && item.getType() != Material.AIR) {
                    recipe.put(i + 1, item.clone());
                }
            }

            if (recipe.isEmpty()) {
                this.player.sendMessage("§cYou need to add at least one ingredient to the recipe");
                this.isSaving = false;
            } else {
                if (this.plugin.getCraftingManager().setRecipe(this.itemId, recipe)) {
                    this.player.sendMessage("§aRecipe saved successfully!");
                    this.player.closeInventory();
                } else {
                    this.player.sendMessage("§cFailed to save recipe. Check for circular dependencies.");
                    this.isSaving = false;
                }

            }
        }
    }

    public void handleClearClick() {
        for(int slot : this.RECIPE_SLOTS) {
            ItemStack item = this.inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                if (this.player.getInventory().firstEmpty() != -1) {
                    this.player.getInventory().addItem(new ItemStack[]{item});
                } else {
                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), item);
                }

                this.inventory.setItem(slot, (ItemStack)null);
            }
        }

        this.player.sendMessage("§aRecipe cleared!");
    }

    public void open() {
        this.player.openInventory(this.inventory);
    }

    public void onClose() {
        if (!this.isSaving) {
            for(int slot : this.RECIPE_SLOTS) {
                ItemStack item = this.inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    if (this.player.getInventory().firstEmpty() != -1) {
                        this.player.getInventory().addItem(new ItemStack[]{item});
                    } else {
                        this.player.getWorld().dropItemNaturally(this.player.getLocation(), item);
                    }
                }
            }

        }
    }
}
