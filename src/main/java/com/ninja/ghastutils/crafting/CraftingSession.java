
package com.ninja.ghastutils.crafting;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class CraftingSession {
    private final String itemId;
    private final Map<Integer, ItemStack> recipe;

    public CraftingSession(String itemId) {
        this.itemId = itemId;
        this.recipe = new HashMap();
    }

    public String getItemId() {
        return this.itemId;
    }

    public Map<Integer, ItemStack> getRecipe() {
        return new HashMap(this.recipe);
    }

    public void setSlot(int slot, ItemStack item) {
        if (item == null) {
            this.recipe.remove(slot);
        } else {
            this.recipe.put(slot, item.clone());
        }

    }

    public void clear() {
        this.recipe.clear();
    }
}
