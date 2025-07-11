
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.crafting.CustomItem;
import com.ninja.ghastutils.gui.CraftingEditorGui;
import com.ninja.ghastutils.gui.CraftingViewGui;
import com.ninja.ghastutils.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CraftingCommand extends SubCommand {
    private final GhastUtils plugin;

    public CraftingCommand(GhastUtils plugin) {
        super("crafting", "ghastutils.crafting", true);
        this.plugin = plugin;
    }

    public String getDescription() {
        return "Manage custom recipes and items";
    }

    public String getUsage() {
        return "/gutil crafting <register/editor/view/give/take/list> [id] [amount]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (this.checkPlayer(sender) && this.checkPermission(sender)) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: " + this.getUsage());
                return true;
            } else {
                Player player = (Player)sender;
                switch (args[0].toLowerCase()) {
                    case "register":
                        if (!player.hasPermission("ghastutils.crafting.admin")) {
                            player.sendMessage("§cYou don't have permission to register custom items");
                            return true;
                        }

                        if (args.length < 2) {
                            player.sendMessage("§cUsage: /gutil crafting register <id>");
                            return true;
                        }

                        ItemStack heldItem = player.getInventory().getItemInMainHand();
                        if (heldItem == null || ItemUtils.isAir(heldItem.getType())) {
                            player.sendMessage("§cYou must be holding an item to register");
                            return true;
                        }

                        String itemId = args[1];
                        this.plugin.getCraftingManager().registerItem(itemId, heldItem);
                        player.sendMessage("§aItem registered with ID: " + itemId);
                        CraftingEditorGui editorGui = new CraftingEditorGui(this.plugin, player, itemId);
                        editorGui.open();
                        break;
                    case "editor":
                        if (!player.hasPermission("ghastutils.crafting.admin")) {
                            player.sendMessage("§cYou don't have permission to edit custom recipes");
                            return true;
                        }

                        if (args.length < 2) {
                            player.sendMessage("§cUsage: /gutil crafting editor <id>");
                            return true;
                        }

                        String editorItemId = args[1];
                        CustomItem editorItem = this.plugin.getCraftingManager().getCustomItem(editorItemId);
                        if (editorItem == null) {
                            player.sendMessage("§cCustom item not found: " + editorItemId);
                            return true;
                        }

                        CraftingEditorGui gui = new CraftingEditorGui(this.plugin, player, editorItemId);
                        gui.open();
                        break;
                    case "view":
                        if (args.length < 2) {
                            player.sendMessage("§cUsage: /gutil crafting view <id>");
                            return true;
                        }

                        String viewItemId = args[1];
                        CustomItem viewItem = this.plugin.getCraftingManager().getCustomItem(viewItemId);
                        if (viewItem == null) {
                            player.sendMessage("§cCustom item not found: " + viewItemId);
                            return true;
                        }

                        CraftingViewGui viewGui = new CraftingViewGui(this.plugin, player, viewItemId);
                        viewGui.open();
                        break;
                    case "give":
                        if (!player.hasPermission("ghastutils.crafting.admin")) {
                            player.sendMessage("§cYou don't have permission to give custom items");
                            return true;
                        }

                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /gutil crafting give <player> <id> [amount]");
                            return true;
                        }

                        String targetName = args[1];
                        Player target = Bukkit.getPlayer(targetName);
                        if (target == null) {
                            player.sendMessage("§cPlayer not found: " + targetName);
                            return true;
                        }

                        String giveItemId = args[2];
                        int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;
                        CustomItem giveItem = this.plugin.getCraftingManager().getCustomItem(giveItemId);
                        if (giveItem == null) {
                            player.sendMessage("§cCustom item not found: " + giveItemId);
                            return true;
                        }

                        ItemStack customItemStack = this.plugin.getCraftingManager().createItemStack(giveItemId, amount);
                        target.getInventory().addItem(new ItemStack[]{customItemStack});
                        player.sendMessage("§aGave " + amount + "x " + giveItem.getName() + " to " + target.getName());
                        target.sendMessage("§aYou received " + amount + "x " + giveItem.getName());
                        break;
                    case "take":
                        if (!player.hasPermission("ghastutils.crafting.admin")) {
                            player.sendMessage("§cYou don't have permission to take custom items");
                            return true;
                        }

                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /gutil crafting take <player> <id> [amount]");
                            return true;
                        }

                        String takeTargetName = args[1];
                        Player takeTarget = Bukkit.getPlayer(takeTargetName);
                        if (takeTarget == null) {
                            player.sendMessage("§cPlayer not found: " + takeTargetName);
                            return true;
                        }

                        String takeItemId = args[2];
                        if (args.length > 3) {
                            Integer.parseInt(args[3]);
                        } else {
                            boolean var10000 = true;
                        }

                        player.sendMessage("§cTaking items is not implemented yet");
                        break;
                    case "list":
                        Map<String, CustomItem> items = this.plugin.getCraftingManager().getCustomItems();
                        if (items.isEmpty()) {
                            player.sendMessage("§cNo custom items registered");
                            return true;
                        }

                        player.sendMessage("§6§l=== Custom Items ===");

                        for(Map.Entry<String, CustomItem> entry : items.entrySet()) {
                            CustomItem item = (CustomItem)entry.getValue();
                            String recipeInfo = item.getRecipe().isEmpty() ? "§c(No Recipe)" : "§a(Has Recipe)";
                            String var10001 = (String)entry.getKey();
                            player.sendMessage("§e" + var10001 + " §7- §f" + item.getName() + " §7- " + recipeInfo);
                        }
                        break;
                    default:
                        player.sendMessage("§cUnknown action: ");
                        player.sendMessage("§cUsage: " + this.getUsage());
                }

                return true;
            }
        } else {
            return true;
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return new ArrayList();
        } else if (args.length == 1) {
            List<String> completions = new ArrayList();
            completions.add("view");
            completions.add("list");
            if (sender.hasPermission("ghastutils.crafting.admin")) {
                completions.add("register");
                completions.add("editor");
                completions.add("give");
                completions.add("take");
            }

            return this.filterStartsWith(completions, args[0]);
        } else {
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("editor") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")) {
                    if (!args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take")) {
                        List<String> itemIds = new ArrayList(this.plugin.getCraftingManager().getCustomItems().keySet());
                        return this.filterStartsWith(itemIds, args[1]);
                    }

                    return null;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")) {
                    List<String> itemIds = new ArrayList(this.plugin.getCraftingManager().getCustomItems().keySet());
                    return this.filterStartsWith(itemIds, args[2]);
                }
            } else if (args.length == 4 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
                return this.filterStartsWith(Arrays.asList("1", "5", "10", "64"), args[3]);
            }

            return new ArrayList();
        }
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        List<String> result = new ArrayList();

        for(String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }

        return result;
    }
}
