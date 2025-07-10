package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.crafting.CraftingManager;
import com.ninja.ghastutils.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IngredientCommand extends SubCommand {
    private final GhastUtils plugin;

    public IngredientCommand(GhastUtils plugin) {
        super("ingredient", "ghastutils.crafting.admin", true);
        this.plugin = plugin;
        this.addAlias("ingredients");
    }

    @Override
    public String getDescription() {
        return "Manage custom crafting ingredients";
    }

    @Override
    public String getUsage() {
        return "/gutil ingredient <register/list> [id]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!checkPlayer(sender) || !checkPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: " + getUsage());
            return true;
        }

        Player player = (Player) sender;
        String action = args[0].toLowerCase();

        switch (action) {
            case "register":
                return handleRegisterCommand(player, args);
            case "list":
                return handleListCommand(player);
            default:
                sender.sendMessage("§cUnknown action: " + action);
                sender.sendMessage("§cUsage: " + getUsage());
                return true;
        }
    }

    private boolean handleRegisterCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /gutil ingredient register <id>");
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "crafting.no-item-held", placeholders);
            return true;
        }

        String ingredientId = args[1];
        CraftingManager craftingManager = plugin.getCraftingManager();
        
        if (craftingManager.registerIngredient(ingredientId, heldItem)) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("id", ingredientId);
            MessageUtils.sendMessage(player, "crafting.ingredient-registered", placeholders);
        } else {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("id", ingredientId);
            MessageUtils.sendMessage(player, "crafting.ingredient-register-failed", placeholders);
        }

        return true;
    }

    private boolean handleListCommand(Player player) {
        CraftingManager craftingManager = plugin.getCraftingManager();
        Map<String, CraftingManager.CustomIngredient> ingredients = craftingManager.getCustomIngredients();

        if (ingredients.isEmpty()) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            MessageUtils.sendMessage(player, "crafting.no-ingredients", placeholders);
            return true;
        }

        player.sendMessage("§6§l=== Custom Ingredients ===");
        for (Map.Entry<String, CraftingManager.CustomIngredient> entry : ingredients.entrySet()) {
            CraftingManager.CustomIngredient ingredient = entry.getValue();
            String materialName = ingredient.getMaterial().toString();
            String modelInfo = ingredient.getCustomModelData() != -1 ? 
                " §7(Model: " + ingredient.getCustomModelData() + ")" : "";
            player.sendMessage("§e" + entry.getKey() + " §7- §f" + 
                ingredient.getName() + " §7- §f" + materialName + modelInfo);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("register", "list"), args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("register")) {
            return filterStartsWith(Arrays.asList("ingredient_id", "custom_ingredient"), args[1]);
        }

        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return list;
        }
        
        String lowercasePrefix = prefix.toLowerCase();
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(lowercasePrefix))
            .collect(java.util.stream.Collectors.toList());
    }
}