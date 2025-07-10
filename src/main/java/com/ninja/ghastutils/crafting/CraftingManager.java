
package com.ninja.ghastutils.crafting;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.config.ConfigType;
import com.ninja.ghastutils.utils.ItemUtils;
import com.ninja.ghastutils.utils.LogManager;
import com.ninja.ghastutils.utils.MessageUtils;
import com.ninja.ghastutils.utils.NBTUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;

public class CraftingManager {
    private final GhastUtils plugin;
    private final Map<String, CustomItem> customItems;
    private final Map<Player, CraftingSession> craftingSessions;
    private boolean recipesRegistered = false;

    public CraftingManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.customItems = new ConcurrentHashMap();
        this.craftingSessions = new HashMap();
        this.loadCustomItems();
        Bukkit.getScheduler().runTask(plugin, this::registerAllRecipes);
    }

    private void registerAllRecipes() {
        if (!this.recipesRegistered) {
            LogManager.info("Registering custom crafting recipes...");
            int count = 0;

            for(Map.Entry<String, CustomItem> entry : this.customItems.entrySet()) {
                CustomItem item = (CustomItem)entry.getValue();
                if (!item.getRecipe().isEmpty()) {
                    Map<Integer, ItemStack> recipeItems = this.convertRecipeToItemStacks(item.getRecipe());
                    if (this.registerRecipe((String)entry.getKey(), recipeItems)) {
                        ++count;
                    }
                }
            }

            this.recipesRegistered = true;
            LogManager.info("Registered " + count + " custom crafting recipes");
        }
    }

    public void reload() {
        Bukkit.recipeIterator().forEachRemaining((recipe) -> {
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                if (shapedRecipe.getKey().getNamespace().equals(this.plugin.getName().toLowerCase())) {
                    Bukkit.removeRecipe(shapedRecipe.getKey());
                }
            }

        });
        this.customItems.clear();
        this.recipesRegistered = false;
        this.loadCustomItems();
        this.registerAllRecipes();
        this.logCraftingChanges("Reloaded all custom items and recipes");
    }

    public boolean registerRecipe(String itemId, Map<Integer, ItemStack> recipeItems) {
        if (itemId != null && recipeItems != null) {
            CustomItem customItem = (CustomItem)this.customItems.get(itemId);
            if (customItem == null) {
                return false;
            } else {
                ItemStack result = this.createItemStack(itemId, 1);
                if (result == null) {
                    return false;
                } else {
                    NamespacedKey key = new NamespacedKey(this.plugin, itemId.toLowerCase());
                    Recipe existingRecipe = Bukkit.getRecipe(key);
                    if (existingRecipe != null) {
                        Bukkit.removeRecipe(key);
                    }

                    ShapedRecipe recipe = new ShapedRecipe(key, result);
                    String[] shape = new String[3];
                    Map<Character, ItemStack> ingredients = new HashMap();
                    char currentChar = 'A';

                    for(int i = 0; i < 3; ++i) {
                        StringBuilder row = new StringBuilder();

                        for(int j = 0; j < 3; ++j) {
                            int slot = i * 3 + j + 1;
                            ItemStack item = (ItemStack)recipeItems.get(slot);
                            if (item != null && item.getType() != Material.AIR) {
                                row.append(currentChar);
                                ingredients.put(currentChar, item);
                                ++currentChar;
                            } else {
                                row.append(' ');
                            }
                        }

                        shape[i] = row.toString();
                    }

                    boolean isValidShape = false;

                    for(String row : shape) {
                        if (!row.trim().isEmpty()) {
                            isValidShape = true;
                            break;
                        }
                    }

                    if (!isValidShape) {
                        this.plugin.getLogger().warning("Invalid recipe shape for item " + itemId + " - at least one slot must be filled");
                        return false;
                    } else {
                        recipe.shape(shape);

                        for(Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
                            ItemStack ingredient = (ItemStack)entry.getValue();
                            String customItemId = this.getCustomItemIdFromItemStack(ingredient);

                            try {
                                if (customItemId != null) {
                                    ItemStack customIngredient = this.createItemStack(customItemId, 1);
                                    if (customIngredient != null) {
                                        recipe.setIngredient((Character)entry.getKey(), customIngredient.getType());
                                    }
                                } else {
                                    recipe.setIngredient((Character)entry.getKey(), ingredient.getType());
                                }
                            } catch (IllegalArgumentException e) {
                                this.plugin.getLogger().warning("Failed to set ingredient for recipe " + itemId + ": " + e.getMessage());
                            }
                        }

                        try {
                            Bukkit.addRecipe(recipe);
                        } catch (IllegalStateException e) {
                            this.plugin.getLogger().warning("Failed to register recipe for " + itemId + ": " + e.getMessage());
                            return false;
                        }

                        Map<Integer, RecipeIngredient> recipeData = new HashMap();

                        for(Map.Entry<Integer, ItemStack> entry : recipeItems.entrySet()) {
                            ItemStack item = (ItemStack)entry.getValue();
                            if (item != null) {
                                String customItemId = this.getCustomItemIdFromItemStack(item);
                                if (customItemId != null) {
                                    recipeData.put((Integer)entry.getKey(), new RecipeIngredient(customItemId, item.getAmount()));
                                } else {
                                    recipeData.put((Integer)entry.getKey(), new RecipeIngredient(item.getType(), item.getAmount()));
                                }
                            }
                        }

                        customItem.setRecipe(recipeData);
                        this.saveItems();
                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean verifyRecipeItems(Player player, Map<Integer, RecipeIngredient> recipe) {
        ItemStack[] matrix = player.getOpenInventory().getTopInventory().getContents();

        for(int slot = 0; slot < 9; ++slot) {
            int recipeSlot = slot + 1;
            RecipeIngredient required = (RecipeIngredient)recipe.get(recipeSlot);
            ItemStack provided = matrix[slot];
            if (required == null) {
                if (provided != null && provided.getType() != Material.AIR) {
                    return false;
                }
            } else {
                if (provided == null || provided.getType() == Material.AIR) {
                    return false;
                }

                if (required.isMaterial()) {
                    if (provided.getType() != required.getMaterial() || provided.getAmount() < required.getAmount() || this.getCustomItemIdFromItemStack(provided) != null) {
                        return false;
                    }
                } else {
                    CustomItem requiredItem = (CustomItem)this.customItems.get(required.getCustomItemId());
                    if (requiredItem == null || !this.isMatchingCustomItem(provided, requiredItem) || provided.getAmount() < required.getAmount()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Map<Integer, ItemStack> convertRecipeToItemStacks(Map<Integer, RecipeIngredient> recipe) {
        Map<Integer, ItemStack> items = new HashMap();
        if (recipe == null) {
            return items;
        } else {
            for(Map.Entry<Integer, RecipeIngredient> entry : recipe.entrySet()) {
                RecipeIngredient ingredient = (RecipeIngredient)entry.getValue();
                if (ingredient != null) {
                    ItemStack item;
                    if (ingredient.isMaterial()) {
                        item = new ItemStack(ingredient.getMaterial(), ingredient.getAmount());
                    } else {
                        item = this.createItemStack(ingredient.getCustomItemId(), ingredient.getAmount());
                    }

                    if (item != null) {
                        items.put((Integer)entry.getKey(), item);
                    }
                }
            }

            return items;
        }
    }

    private void loadCustomItems() {
        FileConfiguration craftingConfig = this.plugin.getConfigManager().getConfig(ConfigType.CRAFTING);
        if (craftingConfig == null) {
            this.plugin.getLogger().warning("Crafting config is null - cannot load custom items");
        } else {
            for(String itemId : craftingConfig.getKeys(false)) {
                try {
                    ConfigurationSection itemSection = craftingConfig.getConfigurationSection(itemId);
                    if (itemSection != null) {
                        String itemName = itemSection.getString("itemname", itemId);

                        Material material;
                        try {
                            material = Material.valueOf(itemSection.getString("material").toUpperCase());
                        } catch (IllegalArgumentException var26) {
                            this.plugin.getLogger().warning("Invalid material for item " + itemId);
                            continue;
                        }

                        boolean glow = itemSection.getBoolean("glow", false);
                        List<String> lore = itemSection.getStringList("lore");
                        String permission = itemSection.getString("permission", "craft." + itemId);
                        boolean noVanilla = itemSection.getBoolean("no-vanilla", false);
                        CustomItem customItem = new CustomItem(itemId, itemName, material, lore);
                        customItem.setPermission(permission);
                        customItem.setGlow(glow);
                        customItem.setVanillaFeaturesDisabled(noVanilla);
                        if (itemSection.contains("custom_model_data")) {
                            customItem.setCustomModelData(itemSection.getInt("custom_model_data"));
                        }

                        if (itemSection.contains("flags")) {
                            ConfigurationSection flagsSection = itemSection.getConfigurationSection("flags");
                            if (flagsSection != null) {
                                for(String flag : flagsSection.getKeys(false)) {
                                    if (flagsSection.getBoolean(flag)) {
                                        try {
                                            ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                                            customItem.addFlag(itemFlag);
                                        } catch (IllegalArgumentException var25) {
                                            this.plugin.getLogger().warning("Invalid item flag: " + flag);
                                        }
                                    }
                                }
                            }
                        }

                        if (itemSection.contains("recipe")) {
                            ConfigurationSection recipeSection = itemSection.getConfigurationSection("recipe");
                            if (recipeSection != null) {
                                Map<Integer, RecipeIngredient> recipe = new HashMap();

                                for(String slot : recipeSection.getKeys(false)) {
                                    try {
                                        int slotNum = Integer.parseInt(slot);
                                        String ingredientString = recipeSection.getString(slot);
                                        if (ingredientString != null) {
                                            String[] parts = ingredientString.split(":");
                                            String ingredientId = parts[0];
                                            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                                            RecipeIngredient ingredient;
                                            try {
                                                Material ingredientMaterial = Material.valueOf(ingredientId.toUpperCase());
                                                ingredient = new RecipeIngredient(ingredientMaterial, amount);
                                            } catch (IllegalArgumentException var23) {
                                                ingredient = new RecipeIngredient(ingredientId, amount);
                                            }

                                            recipe.put(slotNum, ingredient);
                                        }
                                    } catch (NumberFormatException var24) {
                                        this.plugin.getLogger().warning("Invalid recipe slot number: " + slot);
                                    }
                                }

                                customItem.setRecipe(recipe);
                            }
                        }

                        if (itemSection.contains("effects")) {
                            ConfigurationSection effectsSection = itemSection.getConfigurationSection("effects");
                            if (effectsSection != null) {
                                for(String action : effectsSection.getKeys(false)) {
                                    List<String> commands = effectsSection.getStringList(action);
                                    customItem.addEffects(action, commands);
                                }
                            }
                        }

                        this.customItems.put(itemId, customItem);
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().log(Level.WARNING, "Error loading custom item: " + itemId, e);
                }
            }

            this.plugin.getLogger().info("Loaded " + this.customItems.size() + " custom items");
        }
    }

    public void saveItems() {
        FileConfiguration craftingConfig = this.plugin.getConfigManager().getConfig(ConfigType.CRAFTING);
        if (craftingConfig != null) {
            for(String key : craftingConfig.getKeys(false)) {
                craftingConfig.set(key, (Object)null);
            }

            for(CustomItem item : this.customItems.values()) {
                String itemId = item.getId();
                craftingConfig.set(itemId + ".itemname", item.getName());
                craftingConfig.set(itemId + ".material", item.getMaterial().toString());
                craftingConfig.set(itemId + ".lore", item.getLore());
                craftingConfig.set(itemId + ".permission", item.getPermission());
                craftingConfig.set(itemId + ".glow", item.isGlow());
                craftingConfig.set(itemId + ".no-vanilla", item.isVanillaFeaturesDisabled());
                if (item.getCustomModelData() != -1) {
                    craftingConfig.set(itemId + ".custom_model_data", item.getCustomModelData());
                }

                for(ItemFlag flag : item.getFlags()) {
                    craftingConfig.set(itemId + ".flags." + flag.name().toLowerCase(), true);
                }

                if (!item.getRecipe().isEmpty()) {
                    for(Map.Entry<Integer, RecipeIngredient> entry : item.getRecipe().entrySet()) {
                        RecipeIngredient ingredient = (RecipeIngredient)entry.getValue();
                        String ingredientString = ingredient.isMaterial() ? ingredient.getMaterial().toString() + ":" + ingredient.getAmount() : ingredient.getCustomItemId() + ":" + ingredient.getAmount();
                        craftingConfig.set(itemId + ".recipe." + String.valueOf(entry.getKey()), ingredientString);
                    }
                }

                if (!item.getEffects().isEmpty()) {
                    for(Map.Entry<String, List<String>> entry : item.getEffects().entrySet()) {
                        craftingConfig.set(itemId + ".effects." + (String)entry.getKey(), entry.getValue());
                    }
                }
            }

            this.plugin.getConfigManager().saveConfig(ConfigType.CRAFTING);
            this.logCraftingChanges("Saved all custom items");
        }
    }

    private void logCraftingChanges(String message) {
        File logFile = new File(this.plugin.getDataFolder(), "crafting.log");

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String var10001 = dateFormat.format(new Date());
            writer.println("[" + var10001 + "] " + message);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to write to crafting log", e);
        }

    }

    public void registerItem(String itemId, ItemStack item) {
        if (itemId != null && item != null) {
            ItemMeta meta = item.getItemMeta();
            String itemName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : itemId;
            List<String> lore = (List<String>)(meta != null && meta.hasLore() ? meta.getLore() : new ArrayList());
            CustomItem customItem = new CustomItem(itemId, itemName, item.getType(), lore);
            if (meta != null && ItemUtils.hasCustomModelData(meta)) {
                customItem.setCustomModelData(ItemUtils.getCustomModelData(meta));
            }

            if (meta != null) {
                for(ItemFlag flag : meta.getItemFlags()) {
                    customItem.addFlag(flag);
                }
            }

            customItem.setPermission("craft." + itemId);
            customItem.setGlow(!item.getEnchantments().isEmpty());
            this.customItems.put(itemId, customItem);
            this.saveItems();
            this.logCraftingChanges("Registered new item: " + itemId);
        }
    }

    public boolean setRecipe(String itemId, Map<Integer, ItemStack> recipeItems) {
        if (itemId != null && recipeItems != null) {
            CustomItem customItem = (CustomItem)this.customItems.get(itemId);
            if (customItem == null) {
                return false;
            } else {
                Map<Integer, RecipeIngredient> recipe = new HashMap();

                for(Map.Entry<Integer, ItemStack> entry : recipeItems.entrySet()) {
                    ItemStack itemStack = (ItemStack)entry.getValue();
                    if (itemStack != null) {
                        String customItemId = this.getCustomItemIdFromItemStack(itemStack);
                        RecipeIngredient ingredient = customItemId != null ? new RecipeIngredient(customItemId, itemStack.getAmount()) : new RecipeIngredient(itemStack.getType(), itemStack.getAmount());
                        recipe.put((Integer)entry.getKey(), ingredient);
                    }
                }

                if (this.hasCircularDependency(itemId, recipe)) {
                    return false;
                } else {
                    customItem.setRecipe(recipe);
                    this.saveItems();
                    this.logCraftingChanges("Updated recipe for item: " + itemId);
                    return this.registerRecipe(itemId, recipeItems);
                }
            }
        } else {
            return false;
        }
    }

    private boolean hasCircularDependency(String itemId, Map<Integer, RecipeIngredient> recipe) {
        Set<String> visited = new HashSet();
        return this.checkCircularDependencyRecursive(itemId, recipe, visited);
    }

    private boolean checkCircularDependencyRecursive(String itemId, Map<Integer, RecipeIngredient> recipe, Set<String> visited) {
        if (visited.contains(itemId)) {
            return true;
        } else {
            visited.add(itemId);

            for(RecipeIngredient ingredient : recipe.values()) {
                if (!ingredient.isMaterial()) {
                    String ingredientId = ingredient.getCustomItemId();
                    CustomItem ingredientItem = (CustomItem)this.customItems.get(ingredientId);
                    if (ingredientItem != null && this.checkCircularDependencyRecursive(ingredientId, ingredientItem.getRecipe(), new HashSet(visited))) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private String getCustomItemIdFromItemStack(ItemStack itemStack) {
        if (itemStack != null && itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) {
                return null;
            } else {
                if (NBTUtils.hasString(meta, "ghastutils.custom_item_id")) {
                    String id = NBTUtils.getString(meta, "ghastutils.custom_item_id");
                    if (this.customItems.containsKey(id)) {
                        return id;
                    }
                }

                for(CustomItem customItem : this.customItems.values()) {
                    if (this.isMatchingCustomItem(itemStack, customItem)) {
                        return customItem.getId();
                    }
                }

                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isMatchingCustomItem(ItemStack itemStack, CustomItem customItem) {
        if (itemStack != null && itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) {
                return false;
            } else if (itemStack.getType() != customItem.getMaterial()) {
                return false;
            } else {
                String displayName = meta.getDisplayName();
                String expectedName = MessageUtils.translateItemColors(customItem.getName());
                if (!expectedName.equals(displayName)) {
                    return false;
                } else {
                    List<String> lore = (List<String>)(meta.getLore() != null ? meta.getLore() : new ArrayList());
                    List<String> expectedLore = MessageUtils.translateItemColors(customItem.getLore());
                    if (lore.size() != expectedLore.size()) {
                        return false;
                    } else {
                        for(int i = 0; i < lore.size(); ++i) {
                            if (!((String)lore.get(i)).equals(expectedLore.get(i))) {
                                return false;
                            }
                        }

                        if (customItem.getCustomModelData() == -1 || ItemUtils.hasCustomModelData(meta) && ItemUtils.getCustomModelData(meta) == customItem.getCustomModelData()) {
                            String nbtId = NBTUtils.getString(meta, "ghastutils.custom_item_id");
                            return nbtId == null || nbtId.equals(customItem.getId());
                        } else {
                            return false;
                        }
                    }
                }
            }
        } else {
            return false;
        }
    }

    public ItemStack createItemStack(String itemId, int amount) {
        if (itemId == null) {
            return null;
        } else {
            CustomItem customItem = (CustomItem)this.customItems.get(itemId);
            if (customItem == null) {
                return null;
            } else {
                try {
                    ItemStack itemStack = new ItemStack(customItem.getMaterial(), amount);
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(MessageUtils.translateItemColors(customItem.getName()));
                        meta.setLore(MessageUtils.translateItemColors(customItem.getLore()));
                        if (customItem.getCustomModelData() != -1) {
                            ItemUtils.setCustomModelData(meta, customItem.getCustomModelData());
                        }

                        for(ItemFlag flag : customItem.getFlags()) {
                            meta.addItemFlags(new ItemFlag[]{flag});
                        }

                        NBTUtils.setString(meta, "ghastutils.custom_item_id", itemId);
                        if (customItem.isVanillaFeaturesDisabled()) {
                            NBTUtils.setBoolean(meta, "ghastutils.no_vanilla", true);
                        }

                        List<String> lore = (List<String>)(meta.getLore() != null ? meta.getLore() : new ArrayList());
                        if (lore.stream().noneMatch((line) -> line.equals("§8ID: " + itemId))) {
                            lore.add("§8ID: " + itemId);
                            meta.setLore(lore);
                        }

                        itemStack.setItemMeta(meta);
                    }

                    if (customItem.isGlow()) {
                        ItemUtils.addGlow(itemStack);
                    }

                    return itemStack;
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Failed to create item stack for " + itemId + ": " + e.getMessage());
                    return null;
                }
            }
        }
    }

    public boolean hasRequiredMaterials(Player player, String itemId) {
        CustomItem customItem = (CustomItem)this.customItems.get(itemId);
        if (customItem != null && !customItem.getRecipe().isEmpty()) {
            Map<Material, Integer> requiredMaterials = new HashMap();
            Map<String, Integer> requiredCustomItems = new HashMap();

            for(RecipeIngredient ingredient : customItem.getRecipe().values()) {
                if (ingredient.isMaterial()) {
                    Material material = ingredient.getMaterial();
                    requiredMaterials.put(material, (Integer)requiredMaterials.getOrDefault(material, 0) + ingredient.getAmount());
                } else {
                    String customItemId = ingredient.getCustomItemId();
                    requiredCustomItems.put(customItemId, (Integer)requiredCustomItems.getOrDefault(customItemId, 0) + ingredient.getAmount());
                }
            }

            for(Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
                if (!this.hasEnoughMaterial(player, (Material)entry.getKey(), (Integer)entry.getValue())) {
                    return false;
                }
            }

            for(Map.Entry<String, Integer> entry : requiredCustomItems.entrySet()) {
                if (!this.hasEnoughCustomItem(player, (String)entry.getKey(), (Integer)entry.getValue())) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean hasEnoughMaterial(Player player, Material material, int amount) {
        int count = 0;

        for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && this.getCustomItemIdFromItemStack(item) == null) {
                count += item.getAmount();
            }
        }

        return count >= amount;
    }

    private boolean hasEnoughCustomItem(Player player, String itemId, int amount) {
        int count = 0;

        for(ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String customItemId = this.getCustomItemIdFromItemStack(item);
                if (customItemId != null && customItemId.equals(itemId)) {
                    count += item.getAmount();
                }
            }
        }

        return count >= amount;
    }

    public boolean craftItem(Player player, String itemId) {
        CustomItem customItem = (CustomItem)this.customItems.get(itemId);
        if (customItem != null && !customItem.getRecipe().isEmpty()) {
            if (customItem.getPermission() != null && !player.hasPermission(customItem.getPermission())) {
                return false;
            } else if (!this.verifyRecipeItems(player, customItem.getRecipe())) {
                return false;
            } else if (player.getInventory().firstEmpty() == -1) {
                return false;
            } else {
                this.removeCraftingItems(player, customItem.getRecipe());
                ItemStack craftedItem = this.createItemStack(itemId, 1);
                player.getInventory().addItem(new ItemStack[]{craftedItem});
                return true;
            }
        } else {
            return false;
        }
    }

    private void removeCraftingItems(Player player, Map<Integer, RecipeIngredient> recipe) {
        ItemStack[] matrix = player.getOpenInventory().getTopInventory().getContents();

        for(int slot = 0; slot < 9; ++slot) {
            int recipeSlot = slot + 1;
            RecipeIngredient required = (RecipeIngredient)recipe.get(recipeSlot);
            if (required != null && required.getAmount() > 0) {
                ItemStack item = matrix[slot];
                if (item != null) {
                    int newAmount = item.getAmount() - required.getAmount();
                    if (newAmount <= 0) {
                        matrix[slot] = null;
                    } else {
                        item.setAmount(newAmount);
                    }
                }
            }
        }

        player.getOpenInventory().getTopInventory().setContents(matrix);
    }

    private void removeRequiredMaterials(Player player, Map<Integer, RecipeIngredient> recipe) {
        Map<Material, Integer> materialsToRemove = new HashMap();
        Map<String, Integer> customItemsToRemove = new HashMap();

        for(RecipeIngredient ingredient : recipe.values()) {
            if (ingredient.isMaterial()) {
                Material material = ingredient.getMaterial();
                materialsToRemove.put(material, (Integer)materialsToRemove.getOrDefault(material, 0) + ingredient.getAmount());
            } else {
                String customItemId = ingredient.getCustomItemId();
                customItemsToRemove.put(customItemId, (Integer)customItemsToRemove.getOrDefault(customItemId, 0) + ingredient.getAmount());
            }
        }

        for(Map.Entry<Material, Integer> entry : materialsToRemove.entrySet()) {
            Material material = (Material)entry.getKey();
            int amountToRemove = (Integer)entry.getValue();

            for(ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material && this.getCustomItemIdFromItemStack(item) == null) {
                    int amountInStack = item.getAmount();
                    if (amountInStack <= amountToRemove) {
                        amountToRemove -= amountInStack;
                        item.setAmount(0);
                    } else {
                        item.setAmount(amountInStack - amountToRemove);
                        amountToRemove = 0;
                    }

                    if (amountToRemove == 0) {
                        break;
                    }
                }
            }
        }

        for(Map.Entry<String, Integer> entry : customItemsToRemove.entrySet()) {
            String customItemId = (String)entry.getKey();
            int amountToRemove = (Integer)entry.getValue();

            for(ItemStack item : player.getInventory().getContents()) {
                if (item != null) {
                    String itemId = this.getCustomItemIdFromItemStack(item);
                    if (itemId != null && itemId.equals(customItemId)) {
                        int amountInStack = item.getAmount();
                        if (amountInStack <= amountToRemove) {
                            amountToRemove -= amountInStack;
                            item.setAmount(0);
                        } else {
                            item.setAmount(amountInStack - amountToRemove);
                            amountToRemove = 0;
                        }

                        if (amountToRemove == 0) {
                            break;
                        }
                    }
                }
            }
        }

    }

    public CustomItem getCustomItem(String id) {
        return (CustomItem)this.customItems.get(id);
    }

    public Map<String, CustomItem> getCustomItems() {
        return new HashMap(this.customItems);
    }

    public void removeCustomItem(String id) {
        this.customItems.remove(id);
        this.saveItems();
    }

    public void startCraftingSession(Player player, String itemId) {
        CraftingSession session = new CraftingSession(itemId);
        this.craftingSessions.put(player, session);
    }

    public CraftingSession getCraftingSession(Player player) {
        return (CraftingSession)this.craftingSessions.get(player);
    }

    public void endCraftingSession(Player player) {
        this.craftingSessions.remove(player);
    }

    public String getRecipeCostFormatted(String itemId) {
        CustomItem item = (CustomItem)this.customItems.get(itemId);
        if (item != null && !item.getRecipe().isEmpty()) {
            Map<String, Integer> ingredients = new HashMap();

            for(RecipeIngredient ingredient : item.getRecipe().values()) {
                String name;
                if (ingredient.isMaterial()) {
                    name = ingredient.getMaterial().toString();
                } else {
                    name = ingredient.getCustomItemId();
                }

                ingredients.put(name, (Integer)ingredients.getOrDefault(name, 0) + ingredient.getAmount());
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;

            for(Map.Entry<String, Integer> entry : ingredients.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }

                sb.append(entry.getValue()).append("x ").append((String)entry.getKey());
                first = false;
            }

            return sb.toString();
        } else {
            return "No recipe";
        }
    }

    public void executeItemEffects(Player player, ItemStack item, String action) {
        String itemId = this.getCustomItemIdFromItemStack(item);
        if (itemId != null) {
            CustomItem customItem = (CustomItem)this.customItems.get(itemId);
            if (customItem != null) {
                if (customItem.isVanillaFeaturesDisabled() && (action.equals("RIGHT_CLICK") || action.equals("RIGHT_CLICK_AIR") || action.equals("RIGHT_CLICK_BLOCK"))) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && NBTUtils.getBoolean(meta, "ghastutils.no_vanilla") && player.hasMetadata("lastInteractEvent")) {
                        Object obj = ((MetadataValue)player.getMetadata("lastInteractEvent").get(0)).value();
                        if (obj instanceof PlayerInteractEvent) {
                            PlayerInteractEvent event = (PlayerInteractEvent)obj;
                            event.setUseItemInHand(Result.DENY);
                        }
                    }
                }

                List<String> commands = (List)customItem.getEffects().get(action);
                if (commands != null && !commands.isEmpty()) {
                    for(String cmd : commands) {
                        this.executeCommand(player, cmd);
                    }

                }
            }
        }
    }

    private void executeCommand(Player player, String command) {
        String[] parts = command.split(": ", 2);
        if (parts.length == 2) {
            String executor = parts[0].toLowerCase();
            String cmd = parts[1].replace("%player%", player.getName());
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
                    } finally {
                        if (!wasOp) {
                            player.setOp(false);
                        }

                    }
            }

        }
    }
}
