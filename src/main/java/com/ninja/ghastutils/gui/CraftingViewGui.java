
package com.ninja.ghastutils.gui;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.crafting.RecipeIngredient;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CraftingViewGui {
    private final GhastUtils plugin;
    private final Player player;
    private final String itemId;
    private final Inventory inventory;
    private final int[] RECIPE_SLOTS = new int[]{11, 12, 13, 20, 21, 22, 29, 30, 31};
    private final int RESULT_SLOT = 25;

    public CraftingViewGui(GhastUtils plugin, Player player, String itemId) {
        this.plugin = plugin;
        this.player = player;
        this.itemId = itemId;
        this.inventory = Bukkit.createInventory((InventoryHolder)null, 54, "Recipe: " + itemId);
        plugin.getGuiListener().registerCraftingViewGui(player.getUniqueId(), this);
        this.initializeGui();
    }

    private void initializeGui() {
        for(int i = 0; i < this.inventory.getSize(); ++i) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8 || i == 10 || i == 19 || i == 28 || i == 37 || i == 15 || i == 16 || i == 33 || i == 34 || i == 43 || i == 42 || i == 38 || i == 39 || i == 40 || i == 41 || i == 32 || i == 23 || i == 14) {
                this.inventory.setItem(i, this.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
        }

        CustomItem customItem = this.plugin.getCraftingManager().getCustomItem(this.itemId);
        if (customItem != null) {
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

            this.inventory.setItem(25, this.plugin.getCraftingManager().createItemStack(this.itemId, 1));
            this.inventory.setItem(24, this.createGuiItem(Material.ARROW, "§e§l→"));
            boolean canCraft = this.plugin.getCraftingManager().hasRequiredMaterials(this.player, this.itemId) && (customItem.getPermission() == null || this.player.hasPermission(customItem.getPermission()));
            this.inventory.setItem(49, this.createGuiItem(canCraft ? Material.ANVIL : Material.BARRIER, canCraft ? "§a§lCraft Item" : "§c§lCannot Craft", canCraft ? "§7Click to craft this item" : "§7Missing materials or permission"));
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

    public void handleCraftClick() {
        if (this.plugin.getCraftingManager().craftItem(this.player, this.itemId)) {
            Player var10000 = this.player;
            String var10001 = this.plugin.getCraftingManager().getCustomItem(this.itemId).getName();
            var10000.sendMessage("§aSuccessfully crafted " + var10001);
            this.initializeGui();
        } else {
            this.player.sendMessage("§cUnable to craft item. Check if you have the required materials and permissions.");
        }

    }

    public void open() {
        this.player.openInventory(this.inventory);
    }
}
