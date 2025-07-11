
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.sell.SellableItem;
import com.ninja.ghastutils.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SellItemCommand extends SubCommand {
    private final GhastUtils plugin;

    public SellItemCommand(GhastUtils plugin) {
        super("sellitem", "ghastutils.sell.admin", true);
        this.plugin = plugin;
    }

    public String getDescription() {
        return "Manage sellable items";
    }

    public String getUsage() {
        return "/gutil sellitem <register/list> [id] [price]";
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
                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /gutil sellitem register <id> <price>");
                            return true;
                        }

                        ItemStack heldItem = player.getInventory().getItemInMainHand();
                        if (heldItem == null || ItemUtils.isAir(heldItem.getType())) {
                            player.sendMessage("§cYou must be holding an item to register");
                            return true;
                        }

                        String itemId = args[1];

                        double price;
                        try {
                            price = Double.parseDouble(args[2]);
                            if (price < (double)0.0F) {
                                player.sendMessage("§cPrice must be greater than or equal to 0");
                                return true;
                            }
                        } catch (NumberFormatException var23) {
                            player.sendMessage("§cInvalid price format");
                            return true;
                        }

                        ItemMeta meta = heldItem.getItemMeta();
                        String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "";
                        List<String> lore = (List<String>)(meta != null && meta.hasLore() ? meta.getLore() : new ArrayList());
                        int customModelData = meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : -1;
                        new SellableItem(itemId, name, heldItem.getType(), price, lore, customModelData);
                        SellableItem existingItem = this.plugin.getSellManager().getSellableItem(itemId);
                        if (existingItem != null) {
                            player.sendMessage("§eOverwriting existing item with ID: " + itemId);
                        }

                        this.plugin.getSellManager().registerItem(itemId, heldItem, price);
                        player.sendMessage("§aItem registered with ID: §e" + itemId + "§a and price: §e$" + price);
                        player.sendMessage("§aItem metadata (name, lore, custom model data) has been preserved.");
                        break;
                    case "list":
                        Map<String, SellableItem> items = this.plugin.getSellManager().getSellableItems();
                        if (items.isEmpty()) {
                            player.sendMessage("§cNo sellable items registered");
                            return true;
                        }

                        player.sendMessage("§6§l=== Sellable Items ===");

                        for(Map.Entry<String, SellableItem> entry : items.entrySet()) {
                            SellableItem item = (SellableItem)entry.getValue();
                            String itemName = item.getName().isEmpty() ? item.getMaterial().toString() : item.getName();
                            String modelInfo = item.getCustomModelData() != -1 ? " §7(Model: " + item.getCustomModelData() + ")" : "";
                            player.sendMessage("§e" + (String)entry.getKey() + " §7- §f" + itemName + modelInfo + " §7- §f$" + item.getBasePrice());
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
            return this.filterStartsWith(Arrays.asList("register", "list"), args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("register")) {
            return this.filterStartsWith(Arrays.asList("item_id", "custom_item", "rare_item"), args[1]);
        } else {
            return (List<String>)(args.length == 3 && args[0].equalsIgnoreCase("register") ? this.filterStartsWith(Arrays.asList("10", "50", "100", "500", "1000"), args[2]) : new ArrayList());
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
