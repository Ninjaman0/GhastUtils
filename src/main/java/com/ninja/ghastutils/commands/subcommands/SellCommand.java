
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.gui.SellGui;
import com.ninja.ghastutils.sell.SellResult;
import com.ninja.ghastutils.utils.MessageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand extends SubCommand {
    private final GhastUtils plugin;

    public SellCommand(GhastUtils plugin) {
        super("sell", "ghastutils.sell", true);
        this.plugin = plugin;
        this.addAlias("s");
        this.addAlias("sellitems");
    }

    public String getDescription() {
        return "Sell items in your inventory";
    }

    public String getUsage() {
        return "/gutil sell [gui]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (this.checkPlayer(sender) && this.checkPermission(sender)) {
            Player player = (Player)sender;
            if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
                if (!player.hasPermission("ghastutils.sell.gui")) {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("permission", "ghastutils.sell.gui");
                    MessageUtils.sendMessage(player, "permission.denied", placeholders);
                    return true;
                } else {
                    SellGui gui = new SellGui(this.plugin, player);
                    gui.open();
                    return true;
                }
            } else {
                List<ItemStack> itemsToSell = new ArrayList();

                for(ItemStack item : player.getInventory().getContents()) {
                    if (item != null) {
                        itemsToSell.add(item);
                    }
                }

                SellResult result = this.plugin.getSellManager().sellItems(player, itemsToSell);
                if (result.getItemsSold() > 0) {
                    double totalPrice = result.getTotalPrice();
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("items", String.valueOf(result.getItemsSold()));
                    placeholders.put("price", String.format("%.2f", totalPrice));
                    placeholders.put("multiplier", String.format("%.2f", result.getMultiplier()));
                    MessageUtils.sendMessage(player, "sell.sold-header", placeholders);

                    for(Map.Entry<String, Integer> entry : result.getSoldItems().entrySet()) {
                        placeholders.put("amount", String.valueOf(entry.getValue()));
                        placeholders.put("item", (String)entry.getKey());
                    }
                } else {
                    MessageUtils.sendMessage(player, "sell.no-sellable-items");
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
        } else {
            return (List<String>)(args.length == 1 ? this.filterStartsWith(Arrays.asList("gui"), args[0]) : new ArrayList());
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
