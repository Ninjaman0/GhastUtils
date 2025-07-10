
package com.ninja.ghastutils.crafting;

import org.bukkit.Material;

public class RecipeIngredient {
    private final Material material;
    private final String customItemId;
    private final int amount;
    private final boolean isMaterial;
    private final boolean isCustomIngredient;

    public RecipeIngredient(Material material, int amount) {
        this.material = material;
        this.customItemId = null;
        this.amount = amount;
        this.isMaterial = true;
        this.isCustomIngredient = false;
    }

    public RecipeIngredient(String customItemId, int amount, boolean isCustomIngredient) {
        this.material = null;
        this.customItemId = customItemId;
        this.amount = amount;
        this.isMaterial = false;
        this.isCustomIngredient = isCustomIngredient;
    }

    public RecipeIngredient(String customItemId, int amount) {
        this(customItemId, amount, false);
    }

    public Material getMaterial() {
        return this.material;
    }

    public String getCustomItemId() {
        return this.customItemId;
    }

    public int getAmount() {
        return this.amount;
    }

    public boolean isMaterial() {
        return this.isMaterial;
    }

    public boolean isCustomIngredient() {
        return this.isCustomIngredient;
    }
}
