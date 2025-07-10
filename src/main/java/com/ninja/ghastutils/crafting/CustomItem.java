
package com.ninja.ghastutils.crafting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

public class CustomItem {
    private final String id;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final Set<ItemFlag> flags;
    private final Map<String, List<String>> effects;
    private Map<Integer, RecipeIngredient> recipe;
    private String permission;
    private boolean glow;
    private int customModelData = -1;
    private boolean stackable = true;
    private boolean vanillaFeaturesDisabled = false;

    public CustomItem(String id, String name, Material material, List<String> lore) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.flags = new HashSet();
        this.recipe = new HashMap();
        this.effects = new HashMap();
        this.glow = false;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Material getMaterial() {
        return this.material;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public Set<ItemFlag> getFlags() {
        return this.flags;
    }

    public void addFlag(ItemFlag flag) {
        this.flags.add(flag);
    }

    public Map<Integer, RecipeIngredient> getRecipe() {
        return this.recipe;
    }

    public void setRecipe(Map<Integer, RecipeIngredient> recipe) {
        this.recipe = recipe;
    }

    public String getPermission() {
        return this.permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isGlow() {
        return this.glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public int getCustomModelData() {
        return this.customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public Map<String, List<String>> getEffects() {
        return this.effects;
    }

    public void addEffects(String action, List<String> commands) {
        this.effects.put(action, commands);
    }

    public boolean isStackable() {
        return this.stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    public boolean isVanillaFeaturesDisabled() {
        return this.vanillaFeaturesDisabled;
    }

    public void setVanillaFeaturesDisabled(boolean vanillaFeaturesDisabled) {
        this.vanillaFeaturesDisabled = vanillaFeaturesDisabled;
    }
}
