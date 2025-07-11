package com.ninja.ghastutils.listeners;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.crafting.RecipeIngredient;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Map;

public class VanillaCraftingListener implements Listener {
    private final GhastUtils plugin;

    public VanillaCraftingListener(GhastUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            // Check if this is one of our custom recipes
            String key = shapedRecipe.getKey().getKey();
            CustomItem customItem = plugin.getCraftingManager().getCustomItem(key);
            
            if (customItem != null) {
                CraftingInventory craftingInventory = event.getInventory();
                ItemStack[] matrix = craftingInventory.getMatrix();
                
                // Validate that the ingredients match our custom recipe requirements
                if (!validateCustomRecipe(customItem, matrix)) {
                    event.getInventory().setResult(null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            String key = shapedRecipe.getKey().getKey();
            CustomItem customItem = plugin.getCraftingManager().getCustomItem(key);
            
            if (customItem != null) {
                Player player = (Player) event.getWhoClicked();
                
                // Check permission
                if (customItem.getPermission() != null && !player.hasPermission(customItem.getPermission())) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou don't have permission to craft this item!");
                    return;
                }
                
                CraftingInventory craftingInventory = event.getInventory();
                ItemStack[] matrix = craftingInventory.getMatrix();
                
                // Validate ingredients one more time
                if (!validateCustomRecipe(customItem, matrix)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Execute any craft effects
                if (!customItem.getEffects().isEmpty()) {
                    for (Map.Entry<String, java.util.List<String>> entry : customItem.getEffects().entrySet()) {
                        if (entry.getKey().equals("ON_CRAFT")) {
                            for (String command : entry.getValue()) {
                                executeCommand(player, command);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean validateCustomRecipe(CustomItem customItem, ItemStack[] matrix) {
        Map<Integer, RecipeIngredient> recipe = customItem.getRecipe();
        
        // Check each slot in the 3x3 crafting grid
        for (int i = 0; i < 9; i++) {
            int slot = i + 1; // Recipe slots are 1-indexed
            ItemStack matrixItem = i < matrix.length ? matrix[i] : null;
            RecipeIngredient requiredIngredient = recipe.get(slot);
            
            if (requiredIngredient == null) {
                // No ingredient required in this slot
                if (matrixItem != null && matrixItem.getType() != Material.AIR) {
                    return false; // But there's an item there
                }
            } else {
                // Ingredient required in this slot
                if (matrixItem == null || matrixItem.getType() == Material.AIR) {
                    return false; // But slot is empty
                }
                
                // Check if the item matches the required ingredient
                if (!matchesIngredient(matrixItem, requiredIngredient)) {
                    return false;
                }
                
                // Check amount
                if (matrixItem.getAmount() < requiredIngredient.getAmount()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private boolean matchesIngredient(ItemStack item, RecipeIngredient ingredient) {
        if (ingredient.isMaterial()) {
            // Check for vanilla material, but exclude custom items/ingredients
            if (item.getType() != ingredient.getMaterial()) {
                return false;
            }
            
            // Make sure it's not a custom item or ingredient
            String customItemId = plugin.getCraftingManager().getCustomItemIdFromItemStack(item);
            if (customItemId != null) {
                return false; // This is a custom item, not a vanilla material
            }
            
            return true;
        } else if (ingredient.isCustomIngredient()) {
            // Check for custom ingredient
            String ingredientId = getCustomIngredientIdFromItemStack(item);
            return ingredientId != null && ingredientId.equals(ingredient.getCustomItemId());
        } else {
            // Check for custom item
            String customItemId = plugin.getCraftingManager().getCustomItemIdFromItemStack(item);
            return customItemId != null && customItemId.equals(ingredient.getCustomItemId());
        }
    }

    private String getCustomIngredientIdFromItemStack(ItemStack item) {
        // This should match the logic in CraftingManager
        return plugin.getCraftingManager().getCustomIngredientIdFromItemStack(item);
    }

    private void executeCommand(Player player, String command) {
        String[] parts = command.split(": ", 2);
        if (parts.length == 2) {
            String executor = parts[0].toLowerCase();
            String cmd = parts[1].replace("%player%", player.getName());
            
            switch (executor) {
                case "console":
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                    break;
                case "player":
                    player.performCommand(cmd);
                    break;
                case "op":
                    boolean wasOp = player.isOp();
                    try {
                        player.setOp(true);
                        player.performCommand(cmd);
                    } finally {
                        if (!wasOp) {
                            player.setOp(false);
                        }
                    }
                    break;
            }
        }
    }
}