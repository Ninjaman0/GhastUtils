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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
    private final Map<String, CustomIngredient> customIngredients;
    private final Map<Player, CraftingSession> craftingSessions;
    private boolean recipesRegistered = false;

    public CraftingManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.customItems = new ConcurrentHashMap<>();
        this.customIngredients = new ConcurrentHashMap<>();
        this.craftingSessions = new HashMap<>();
        this.loadCustomIngredients();
        this.loadCustomItems();
        Bukkit.getScheduler().runTask(plugin, this::registerAllRecipes);
    }

    private void registerAllRecipes() {
        if (!this.recipesRegistered) {
            LogManager.info("Registering custom crafting recipes...");
            int count = 0;

            for(Map.Entry<String, CustomItem> entry : this.customItems.entrySet()) {
                CustomItem item = entry.getValue();
                if (!item.getRecipe().isEmpty()) {
                    Map<Integer, ItemStack> recipeItems = this.convertRecipeToItemStacks(item.getRecipe());
                    if (this.registerRecipe(entry.getKey(), recipeItems)) {
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
        this.customIngredients.clear();
        this.recipesRegistered = false;
        this.loadCustomIngredients();
        this.loadCustomItems();
        this.registerAllRecipes();
        this.logCraftingChanges("Reloaded all custom items and recipes");
    }

    public boolean registerRecipe(String itemId, Map<Integer, ItemStack> recipeItems) {
        if (itemId == null || recipeItems == null) {
            return false;
        }
        
        CustomItem customItem = this.customItems.get(itemId);
        if (customItem == null) {
            return false;
        }

        ItemStack result = this.createItemStack(itemId, 1);
        if (result == null) {
            return false;
        }

        NamespacedKey key = new NamespacedKey(this.plugin, itemId.toLowerCase());
        Recipe existingRecipe = Bukkit.getRecipe(key);
        if (existingRecipe != null) {
            Bukkit.removeRecipe(key);
        }

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        String[] shape = new String[3];
        Map<Character, ItemStack> ingredients = new HashMap<>();
        char currentChar = 'A';

        for(int i = 0; i < 3; ++i) {
            StringBuilder row = new StringBuilder();

            for(int j = 0; j < 3; ++j) {
                int slot = i * 3 + j + 1;
                ItemStack item = recipeItems.get(slot);
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
        }

        recipe.shape(shape);

        for(Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            ItemStack ingredient = entry.getValue();
            try {
                recipe.setIngredient(entry.getKey(), ingredient.getType());
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

        Map<Integer, RecipeIngredient> recipeData = new HashMap<>();
        for(Map.Entry<Integer, ItemStack> entry : recipeItems.entrySet()) {
            ItemStack item = entry.getValue();
            if (item != null) {
                String customItemId = this.getCustomItemIdFromItemStack(item);
                String ingredientId = this.getCustomIngredientIdFromItemStack(item);
                
                if (customItemId != null) {
                    recipeData.put(entry.getKey(), new RecipeIngredient(customItemId, item.getAmount(), false));
                } else if (ingredientId != null) {
                    recipeData.put(entry.getKey(), new RecipeIngredient(ingredientId, item.getAmount(), true));
                } else {
                    recipeData.put(entry.getKey(), new RecipeIngredient(item.getType(), item.getAmount()));
                }
            }
        }

        customItem.setRecipe(recipeData);
        this.saveItems();
        return true;
    }

    private Map<Integer, ItemStack> convertRecipeToItemStacks(Map<Integer, RecipeIngredient> recipe) {
        Map<Integer, ItemStack> items = new HashMap<>();
        if (recipe == null) {
            return items;
        }

        for(Map.Entry<Integer, RecipeIngredient> entry : recipe.entrySet()) {
            RecipeIngredient ingredient = entry.getValue();
            if (ingredient != null) {
                ItemStack item;
                if (ingredient.isMaterial()) {
                    item = new ItemStack(ingredient.getMaterial(), ingredient.getAmount());
                } else if (ingredient.isCustomIngredient()) {
                    item = this.createIngredientItemStack(ingredient.getCustomItemId(), ingredient.getAmount());
                } else {
                    item = this.createItemStack(ingredient.getCustomItemId(), ingredient.getAmount());
                }

                if (item != null) {
                    items.put(entry.getKey(), item);
                }
            }
        }

        return items;
    }

    private void loadCustomIngredients() {
        FileConfiguration craftingConfig = this.plugin.getConfigManager().getConfig(ConfigType.CRAFTING);
        ConfigurationSection ingredientsSection = craftingConfig.getConfigurationSection("ingredients");

        if (ingredientsSection == null) {
            LogManager.debug("No custom ingredients found in crafting.yml");
            return;
        }

        for (String ingredientId : ingredientsSection.getKeys(false)) {
            try {
                ConfigurationSection ingredientSection = ingredientsSection.getConfigurationSection(ingredientId);
                if (ingredientSection != null) {
                    String itemName = ingredientSection.getString("item-name", ingredientId);

                    Material material;
                    try {
                        material = Material.valueOf(ingredientSection.getString("material", "WHEAT").toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LogManager.warning("Invalid material for ingredient " + ingredientId);
                        continue;
                    }

                    List<String> lore = ingredientSection.getStringList("lore");
                    int customModelData = ingredientSection.getInt("custom-model-data", -1);

                    CustomIngredient ingredient = new CustomIngredient(ingredientId, itemName, material, lore, customModelData);
                    this.customIngredients.put(ingredientId, ingredient);
                    LogManager.debug("Loaded custom ingredient: " + ingredientId);
                }
            } catch (Exception e) {
                LogManager.warning("Error loading custom ingredient " + ingredientId + ": " + e.getMessage());
            }
        }

        LogManager.info("Loaded " + this.customIngredients.size() + " custom ingredients");
    }

    private void loadCustomItems() {
        FileConfiguration craftingConfig = this.plugin.getConfigManager().getConfig(ConfigType.CRAFTING);
        if (craftingConfig == null) {
            this.plugin.getLogger().warning("Crafting config is null - cannot load custom items");
            return;
        }

        for (String itemId : craftingConfig.getKeys(false)) {
            try {
                if (itemId.equals("ingredients")) {
                    continue;
                }

                ConfigurationSection itemSection = craftingConfig.getConfigurationSection(itemId);
                if (itemSection == null) {
                    this.plugin.getLogger().warning("Invalid configuration section for item " + itemId);
                    continue;
                }

                if (!itemSection.contains("material")) {
                    this.plugin.getLogger().warning("Missing required 'material' field for item " + itemId);
                    continue;
                }

                String materialString = itemSection.getString("material");
                if (materialString == null || materialString.isEmpty()) {
                    this.plugin.getLogger().warning("Empty material for item " + itemId);
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(materialString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid material '" + materialString + "' for item " + itemId);
                    continue;
                }

                String itemName = itemSection.getString("itemname", itemId);
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
                        for (String flag : flagsSection.getKeys(false)) {
                            if (flagsSection.getBoolean(flag)) {
                                try {
                                    ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                                    customItem.addFlag(itemFlag);
                                } catch (IllegalArgumentException e) {
                                    this.plugin.getLogger().warning("Invalid item flag: " + flag + " for item " + itemId);
                                }
                            }
                        }
                    }
                }

                if (itemSection.contains("recipe")) {
                    ConfigurationSection recipeSection = itemSection.getConfigurationSection("recipe");
                    if (recipeSection != null) {
                        Map<Integer, RecipeIngredient> recipe = new HashMap<>();

                        for (String slot : recipeSection.getKeys(false)) {
                            try {
                                int slotNum = Integer.parseInt(slot);
                                String ingredientString = recipeSection.getString(slot);
                                if (ingredientString == null) {
                                    continue;
                                }

                                String[] parts = ingredientString.split(":");
                                String ingredientId = parts[0];
                                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                                RecipeIngredient ingredient;
                                
                                // Check if it's a custom ingredient first
                                if (this.customIngredients.containsKey(ingredientId)) {
                                    ingredient = new RecipeIngredient(ingredientId, amount, true);
                                } else {
                                    try {
                                        // Try to parse as material
                                        Material ingredientMaterial = Material.valueOf(ingredientId.toUpperCase());
                                        ingredient = new RecipeIngredient(ingredientMaterial, amount);
                                    } catch (IllegalArgumentException e) {
                                        // Must be a custom item
                                        ingredient = new RecipeIngredient(ingredientId, amount, false);
                                    }
                                }

                                recipe.put(slotNum, ingredient);
                            } catch (NumberFormatException e) {
                                this.plugin.getLogger().warning("Invalid recipe slot number or amount: " + slot + " for item " + itemId);
                            }
                        }

                        customItem.setRecipe(recipe);
                    }
                }

                if (itemSection.contains("effects")) {
                    ConfigurationSection effectsSection = itemSection.getConfigurationSection("effects");
                    if (effectsSection != null) {
                        for (String action : effectsSection.getKeys(false)) {
                            List<String> commands = effectsSection.getStringList(action);
                            customItem.addEffects(action, commands);
                        }
                    }
                }

                this.customItems.put(itemId, customItem);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Error loading custom item: " + itemId, e);
            }
        }

        this.plugin.getLogger().info("Loaded " + this.customItems.size() + " custom items");
    }

    public void saveItems() {
        FileConfiguration craftingConfig = this.plugin.getConfigManager().getConfig(ConfigType.CRAFTING);

        // Save ingredients first
        craftingConfig.set("ingredients", null);
        if (!this.customIngredients.isEmpty()) {
            for (Map.Entry<String, CustomIngredient> entry : this.customIngredients.entrySet()) {
                String ingredientId = entry.getKey();
                CustomIngredient ingredient = entry.getValue();

                craftingConfig.set("ingredients." + ingredientId + ".item-name", ingredient.getName());
                craftingConfig.set("ingredients." + ingredientId + ".material", ingredient.getMaterial().toString());
                craftingConfig.set("ingredients." + ingredientId + ".lore", ingredient.getLore());
                if (ingredient.getCustomModelData() != -1) {
                    craftingConfig.set("ingredients." + ingredientId + ".custom-model-data", ingredient.getCustomModelData());
                }
            }
        }

        // Clear existing items section
        for(String key : craftingConfig.getKeys(false)) {
            if (!key.equals("ingredients")) {
                craftingConfig.set(key, null);
            }
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
                    RecipeIngredient ingredient = entry.getValue();
                    String ingredientString;
                    if (ingredient.isMaterial()) {
                        ingredientString = ingredient.getMaterial().toString() + ":" + ingredient.getAmount();
                    } else {
                        ingredientString = ingredient.getCustomItemId() + ":" + ingredient.getAmount();
                    }
                    craftingConfig.set(itemId + ".recipe." + String.valueOf(entry.getKey()), ingredientString);
                }
            }

            if (!item.getEffects().isEmpty()) {
                for(Map.Entry<String, List<String>> entry : item.getEffects().entrySet()) {
                    craftingConfig.set(itemId + ".effects." + entry.getKey(), entry.getValue());
                }
            }
        }

        this.plugin.getConfigManager().saveConfig(ConfigType.CRAFTING);
        this.logCraftingChanges("Saved all custom items");
    }

    public boolean registerIngredient(String ingredientId, ItemStack item) {
        if (ingredientId == null || item == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        String itemName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : ingredientId;
        List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<>();
        int customModelData = meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : -1;

        CustomIngredient ingredient = new CustomIngredient(ingredientId, itemName, item.getType(), lore, customModelData);
        this.customIngredients.put(ingredientId, ingredient);
        this.saveItems();
        this.logCraftingChanges("Registered new ingredient: " + ingredientId);

        return true;
    }

    private void logCraftingChanges(String message) {
        File logFile = new File(this.plugin.getDataFolder(), "crafting.log");

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.println("[" + dateFormat.format(new Date()) + "] " + message);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to write to crafting log", e);
        }
    }

    public void registerItem(String itemId, ItemStack item) {
        if (itemId != null && item != null) {
            ItemMeta meta = item.getItemMeta();
            String itemName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : itemId;
            List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<>();
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
        if (itemId == null || recipeItems == null) {
            return false;
        }
        
        CustomItem customItem = this.customItems.get(itemId);
        if (customItem == null) {
            return false;
        }

        Map<Integer, RecipeIngredient> recipe = new HashMap<>();

        for(Map.Entry<Integer, ItemStack> entry : recipeItems.entrySet()) {
            ItemStack itemStack = entry.getValue();
            if (itemStack != null) {
                String customItemId = this.getCustomItemIdFromItemStack(itemStack);
                String ingredientId = this.getCustomIngredientIdFromItemStack(itemStack);
                
                RecipeIngredient ingredient;
                if (customItemId != null) {
                    ingredient = new RecipeIngredient(customItemId, itemStack.getAmount(), false);
                } else if (ingredientId != null) {
                    ingredient = new RecipeIngredient(ingredientId, itemStack.getAmount(), true);
                } else {
                    ingredient = new RecipeIngredient(itemStack.getType(), itemStack.getAmount());
                }
                recipe.put(entry.getKey(), ingredient);
            }
        }

        if (this.hasCircularDependency(itemId, recipe)) {
            return false;
        }

        customItem.setRecipe(recipe);
        this.saveItems();
        this.logCraftingChanges("Updated recipe for item: " + itemId);
        return this.registerRecipe(itemId, recipeItems);
    }

    private boolean hasCircularDependency(String itemId, Map<Integer, RecipeIngredient> recipe) {
        Set<String> visited = new HashSet<>();
        return this.checkCircularDependencyRecursive(itemId, recipe, visited);
    }

    private boolean checkCircularDependencyRecursive(String itemId, Map<Integer, RecipeIngredient> recipe, Set<String> visited) {
        if (visited.contains(itemId)) {
            return true;
        }
        
        visited.add(itemId);

        for(RecipeIngredient ingredient : recipe.values()) {
            if (!ingredient.isMaterial()) {
                String ingredientId = ingredient.getCustomItemId();
                if (ingredient.isCustomIngredient()) {
                    // Custom ingredients don't have recipes, so no circular dependency
                    continue;
                } else {
                    CustomItem ingredientItem = this.customItems.get(ingredientId);
                    if (ingredientItem != null && this.checkCircularDependencyRecursive(ingredientId, ingredientItem.getRecipe(), new HashSet<>(visited))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isMatchingCustomItem(ItemStack itemStack, CustomItem customItem) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || itemStack.getType() != customItem.getMaterial()) {
            return false;
        }

        String displayName = meta.getDisplayName();
        String expectedName = MessageUtils.translateItemColors(customItem.getName());
        if (!expectedName.equals(displayName)) {
            return false;
        }

        List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
        List<String> expectedLore = MessageUtils.translateItemColors(customItem.getLore());
        if (lore.size() != expectedLore.size()) {
            return false;
        }

        for(int i = 0; i < lore.size(); ++i) {
            if (!lore.get(i).equals(expectedLore.get(i))) {
                return false;
            }
        }

        if (customItem.getCustomModelData() != -1) {
            if (!ItemUtils.hasCustomModelData(meta) || ItemUtils.getCustomModelData(meta) != customItem.getCustomModelData()) {
                return false;
            }
        }

        String nbtId = NBTUtils.getString(meta, "ghastutils.custom_item_id");
        return nbtId == null || nbtId.equals(customItem.getId());
    }

    public ItemStack createItemStack(String itemId, int amount) {
        if (itemId == null) {
            return null;
        }
        
        CustomItem customItem = this.customItems.get(itemId);
        if (customItem == null) {
            return null;
        }

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
                    meta.addItemFlags(flag);
                }

                NBTUtils.setString(meta, "ghastutils.custom_item_id", itemId);
                if (customItem.isVanillaFeaturesDisabled()) {
                    NBTUtils.setBoolean(meta, "ghastutils.no_vanilla", true);
                }

                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                if (lore.stream().noneMatch(line -> line.equals("§8ID: " + itemId))) {
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

    public ItemStack createIngredientItemStack(String ingredientId, int amount) {
        CustomIngredient ingredient = this.customIngredients.get(ingredientId);
        if (ingredient == null) {
            return null;
        }

        try {
            ItemStack itemStack = new ItemStack(ingredient.getMaterial(), amount);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtils.translateItemColors(ingredient.getName()));
                meta.setLore(MessageUtils.translateItemColors(ingredient.getLore()));
                
                if (ingredient.getCustomModelData() != -1) {
                    ItemUtils.setCustomModelData(meta, ingredient.getCustomModelData());
                }

                NBTUtils.setString(meta, "ghastutils.custom_ingredient_id", ingredientId);
                itemStack.setItemMeta(meta);
            }

            return itemStack;
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Failed to create ingredient item stack for " + ingredientId + ": " + e.getMessage());
            return null;
        }
    }

    public boolean hasRequiredMaterials(Player player, String itemId) {
        CustomItem customItem = this.customItems.get(itemId);
        if (customItem == null || customItem.getRecipe().isEmpty()) {
            return false;
        }

        Map<Material, Integer> requiredMaterials = new HashMap<>();
        Map<String, Integer> requiredCustomItems = new HashMap<>();
        Map<String, Integer> requiredIngredients = new HashMap<>();

        for(RecipeIngredient ingredient : customItem.getRecipe().values()) {
            if (ingredient.isMaterial()) {
                Material material = ingredient.getMaterial();
                requiredMaterials.put(material, requiredMaterials.getOrDefault(material, 0) + ingredient.getAmount());
            } else if (ingredient.isCustomIngredient()) {
                String ingredientId = ingredient.getCustomItemId();
                requiredIngredients.put(ingredientId, requiredIngredients.getOrDefault(ingredientId, 0) + ingredient.getAmount());
            } else {
                String customItemId = ingredient.getCustomItemId();
                requiredCustomItems.put(customItemId, requiredCustomItems.getOrDefault(customItemId, 0) + ingredient.getAmount());
            }
        }

        // Check materials
        for(Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
            if (!this.hasEnoughMaterial(player, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        // Check custom items
        for(Map.Entry<String, Integer> entry : requiredCustomItems.entrySet()) {
            if (!this.hasEnoughCustomItem(player, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        // Check custom ingredients
        for(Map.Entry<String, Integer> entry : requiredIngredients.entrySet()) {
            if (!this.hasEnoughCustomIngredient(player, entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    private boolean hasEnoughMaterial(Player player, Material material, int amount) {
        int count = 0;

        for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && 
                this.getCustomItemIdFromItemStack(item) == null && 
                this.getCustomIngredientIdFromItemStack(item) == null) {
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

    private boolean hasEnoughCustomIngredient(Player player, String ingredientId, int amount) {
        int count = 0;

        for(ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String customIngredientId = this.getCustomIngredientIdFromItemStack(item);
                if (customIngredientId != null && customIngredientId.equals(ingredientId)) {
                    count += item.getAmount();
                }
            }
        }

        return count >= amount;
    }

    private String getCustomIngredientIdFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        // Check NBT first
        if (NBTUtils.hasString(meta, "ghastutils.custom_ingredient_id")) {
            String id = NBTUtils.getString(meta, "ghastutils.custom_ingredient_id");
            if (this.customIngredients.containsKey(id)) {
                return id;
            }
        }

        // Check by matching properties
        for (Map.Entry<String, CustomIngredient> entry : this.customIngredients.entrySet()) {
            if (this.isMatchingCustomIngredient(itemStack, entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean isMatchingCustomIngredient(ItemStack itemStack, CustomIngredient ingredient) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || itemStack.getType() != ingredient.getMaterial()) {
            return false;
        }

        // Check display name
        String displayName = meta.getDisplayName();
        String expectedName = MessageUtils.translateItemColors(ingredient.getName());
        if (!expectedName.equals(displayName)) {
            return false;
        }

        // Check lore
        List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
        List<String> expectedLore = MessageUtils.translateItemColors(ingredient.getLore());
        if (lore.size() != expectedLore.size()) {
            return false;
        }

        for (int i = 0; i < lore.size(); i++) {
            if (!lore.get(i).equals(expectedLore.get(i))) {
                return false;
            }
        }

        // Check custom model data
        if (ingredient.getCustomModelData() != -1) {
            if (!ItemUtils.hasCustomModelData(meta) ||
                    ItemUtils.getCustomModelData(meta) != ingredient.getCustomModelData()) {
                return false;
            }
        }

        return true;
    }

    public boolean craftItem(Player player, String itemId) {
        CustomItem customItem = this.customItems.get(itemId);
        if (customItem == null || customItem.getRecipe().isEmpty()) {
            return false;
        }

        if (customItem.getPermission() != null && !player.hasPermission(customItem.getPermission())) {
            return false;
        }

        if (!this.hasRequiredMaterials(player, itemId)) {
            return false;
        }

        if (player.getInventory().firstEmpty() == -1) {
            return false;
        }

        this.removeRequiredMaterials(player, customItem.getRecipe());
        ItemStack craftedItem = this.createItemStack(itemId, 1);
        player.getInventory().addItem(craftedItem);
        return true;
    }

    private void removeRequiredMaterials(Player player, Map<Integer, RecipeIngredient> recipe) {
        Map<Material, Integer> materialsToRemove = new HashMap<>();
        Map<String, Integer> customItemsToRemove = new HashMap<>();
        Map<String, Integer> ingredientsToRemove = new HashMap<>();

        for(RecipeIngredient ingredient : recipe.values()) {
            if (ingredient.isMaterial()) {
                Material material = ingredient.getMaterial();
                materialsToRemove.put(material, materialsToRemove.getOrDefault(material, 0) + ingredient.getAmount());
            } else if (ingredient.isCustomIngredient()) {
                String ingredientId = ingredient.getCustomItemId();
                ingredientsToRemove.put(ingredientId, ingredientsToRemove.getOrDefault(ingredientId, 0) + ingredient.getAmount());
            } else {
                String customItemId = ingredient.getCustomItemId();
                customItemsToRemove.put(customItemId, customItemsToRemove.getOrDefault(customItemId, 0) + ingredient.getAmount());
            }
        }

        // Remove materials
        for(Map.Entry<Material, Integer> entry : materialsToRemove.entrySet()) {
            Material material = entry.getKey();
            int amountToRemove = entry.getValue();

            for(ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material && 
                    this.getCustomItemIdFromItemStack(item) == null &&
                    this.getCustomIngredientIdFromItemStack(item) == null) {
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

        // Remove custom items
        for(Map.Entry<String, Integer> entry : customItemsToRemove.entrySet()) {
            String customItemId = entry.getKey();
            int amountToRemove = entry.getValue();

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

        // Remove custom ingredients
        for(Map.Entry<String, Integer> entry : ingredientsToRemove.entrySet()) {
            String ingredientId = entry.getKey();
            int amountToRemove = entry.getValue();

            for(ItemStack item : player.getInventory().getContents()) {
                if (item != null) {
                    String itemIngredientId = this.getCustomIngredientIdFromItemStack(item);
                    if (itemIngredientId != null && itemIngredientId.equals(ingredientId)) {
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
        return this.customItems.get(id);
    }

    public Map<String, CustomItem> getCustomItems() {
        return new HashMap<>(this.customItems);
    }

    public Map<String, CustomIngredient> getCustomIngredients() {
        return new HashMap<>(this.customIngredients);
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
        return this.craftingSessions.get(player);
    }

    public void endCraftingSession(Player player) {
        this.craftingSessions.remove(player);
    }

    public String getRecipeCostFormatted(String itemId) {
        CustomItem item = this.customItems.get(itemId);
        if (item == null || item.getRecipe().isEmpty()) {
            return "No recipe";
        }

        Map<String, Integer> ingredients = new HashMap<>();

        for(RecipeIngredient ingredient : item.getRecipe().values()) {
            String name;
            if (ingredient.isMaterial()) {
                name = ingredient.getMaterial().toString();
            } else if (ingredient.isCustomIngredient()) {
                CustomIngredient customIngredient = this.customIngredients.get(ingredient.getCustomItemId());
                name = customIngredient != null ? customIngredient.getName() : ingredient.getCustomItemId();
            } else {
                name = ingredient.getCustomItemId();
            }

            ingredients.put(name, ingredients.getOrDefault(name, 0) + ingredient.getAmount());
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for(Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            if (!first) {
                sb.append(", ");
            }

            sb.append(entry.getValue()).append("x ").append(entry.getKey());
            first = false;
        }

        return sb.toString();
    }

    public void executeItemEffects(Player player, ItemStack item, String action) {
        String itemId = this.getCustomItemIdFromItemStack(item);
        if (itemId == null) {
            return;
        }
        
        CustomItem customItem = this.customItems.get(itemId);
        if (customItem == null) {
            return;
        }

        if (customItem.isVanillaFeaturesDisabled() && (action.equals("RIGHT_CLICK") || action.equals("RIGHT_CLICK_AIR") || action.equals("RIGHT_CLICK_BLOCK"))) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && NBTUtils.getBoolean(meta, "ghastutils.no_vanilla") && player.hasMetadata("lastInteractEvent")) {
                Object obj = player.getMetadata("lastInteractEvent").get(0).value();
                if (obj instanceof PlayerInteractEvent event) {
                    event.setUseItemInHand(Result.DENY);
                }
            }
        }

        List<String> commands = customItem.getEffects().get(action);
        if (commands != null && !commands.isEmpty()) {
            for(String cmd : commands) {
                this.executeCommand(player, cmd);
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
                    break;
            }
        }
    }

    public String getCustomItemIdFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        if (NBTUtils.hasString(meta, "ghastutils.custom_item_id")) {
            String id = NBTUtils.getString(meta, "ghastutils.custom_item_id");
            if (this.customItems.containsKey(id)) {
                return id;
            }
        }

        for (CustomItem customItem : this.customItems.values()) {
            if (this.isMatchingCustomItem(itemStack, customItem)) {
                return customItem.getId();
            }
        }

        return null;
    }

    public String getCustomIngredientIdFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        // Use the existing private method
        return this.getCustomIngredientIdFromItemStack(itemStack);
    }

    public static class CustomIngredient {
        private final String id;
        private final String name;
        private final Material material;
        private final List<String> lore;
        private final int customModelData;

        public CustomIngredient(String id, String name, Material material, List<String> lore, int customModelData) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.customModelData = customModelData;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Material getMaterial() { return material; }
        public List<String> getLore() { return new ArrayList<>(lore); }
        public int getCustomModelData() { return customModelData; }
    }
}