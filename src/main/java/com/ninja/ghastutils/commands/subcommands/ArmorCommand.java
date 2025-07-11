
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.armor.ArmorManager;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.utils.MessageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArmorCommand extends SubCommand {
    private final GhastUtils plugin;

    public ArmorCommand(GhastUtils plugin) {
        super("armor", "ghastutils.armor.admin", false);
        this.plugin = plugin;
        this.addAlias("armors");
    }

    public String getDescription() {
        return "Manage custom armor pieces";
    }

    public String getUsage() {
        return "/gutil armor <give/take/list> [player] [id] [amount]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("permission", this.getPermission());
            MessageUtils.sendMessage(sender, "permission.denied", placeholders);
            return true;
        } else if (args.length == 0) {
            sender.sendMessage("§cUsage: " + this.getUsage());
            return true;
        } else {
            switch (args[0].toLowerCase()) {
                case "give":
                    return this.handleGiveCommand(sender, args);
                case "take":
                    return this.handleTakeCommand(sender, args);
                case "list":
                    return this.handleListCommand(sender, args);
                default:
                    sender.sendMessage("§cUnknown action: ");
                    sender.sendMessage("§cUsage: " + this.getUsage());
                    return true;
            }
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gutil armor give <player> <id> [amount]");
            return true;
        } else {
            String playerName = args[1];
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return true;
            } else {
                String armorId = args[2];
                ArmorManager armorManager = this.plugin.getArmorManager();
                if (!armorManager.getArmorPieces().containsKey(armorId)) {
                    sender.sendMessage("§cArmor piece not found: " + armorId);
                    return true;
                } else {
                    int amount = 1;
                    if (args.length > 3) {
                        try {
                            amount = Integer.parseInt(args[3]);
                            if (amount <= 0) {
                                sender.sendMessage("§cAmount must be greater than 0");
                                return true;
                            }
                        } catch (NumberFormatException var12) {
                            sender.sendMessage("§cInvalid amount: " + args[3]);
                            return true;
                        }
                    }

                    ItemStack armorItem = armorManager.createArmorItem(armorId);
                    if (armorItem == null) {
                        sender.sendMessage("§cFailed to create armor item: " + armorId);
                        return true;
                    } else {
                        armorItem.setAmount(amount);
                        Map<Integer, ItemStack> leftover = target.getInventory().addItem(new ItemStack[]{armorItem});
                        if (leftover.isEmpty()) {
                            sender.sendMessage("§aGave " + amount + "x " + armorId + " to " + target.getName());
                            Map<String, String> placeholders = MessageUtils.placeholders();
                            placeholders.put("amount", String.valueOf(amount));
                            placeholders.put("armor", armorId);
                            MessageUtils.sendMessage(target, "armor.received", placeholders);
                        } else {
                            for(ItemStack item : leftover.values()) {
                                target.getWorld().dropItemNaturally(target.getLocation(), item);
                            }

                            sender.sendMessage("§aGave " + amount + "x " + armorId + " to " + target.getName() + " (some items were dropped)");
                            Map<String, String> placeholders = MessageUtils.placeholders();
                            placeholders.put("amount", String.valueOf(amount));
                            placeholders.put("armor", armorId);
                            MessageUtils.sendMessage(target, "armor.received-dropped", placeholders);
                        }

                        return true;
                    }
                }
            }
        }
    }

    private boolean handleTakeCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gutil armor take <player> <id> [amount]");
            return true;
        } else {
            String playerName = args[1];
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return true;
            } else {
                String armorId = args[2];
                ArmorManager armorManager = this.plugin.getArmorManager();
                if (!armorManager.getArmorPieces().containsKey(armorId)) {
                    sender.sendMessage("§cArmor piece not found: " + armorId);
                    return true;
                } else {
                    int amountToTake = 1;
                    if (args.length > 3) {
                        try {
                            amountToTake = Integer.parseInt(args[3]);
                            if (amountToTake <= 0) {
                                sender.sendMessage("§cAmount must be greater than 0");
                                return true;
                            }
                        } catch (NumberFormatException var16) {
                            sender.sendMessage("§cInvalid amount: " + args[3]);
                            return true;
                        }
                    }

                    int found = 0;

                    for(ItemStack item : target.getInventory().getContents()) {
                        if (item != null && armorManager.isArmorPiece(item)) {
                            String id = armorManager.getArmorId(item);
                            if (id != null && id.equals(armorId)) {
                                int itemAmount = item.getAmount();
                                if (found + itemAmount <= amountToTake) {
                                    target.getInventory().removeItem(new ItemStack[]{item});
                                    found += itemAmount;
                                } else {
                                    int toRemove = amountToTake - found;
                                    item.setAmount(itemAmount - toRemove);
                                    found = amountToTake;
                                }

                                if (found >= amountToTake) {
                                    break;
                                }
                            }
                        }
                    }

                    if (found < amountToTake) {
                        ItemStack[] armorContents = target.getInventory().getArmorContents();

                        for(int i = 0; i < armorContents.length; ++i) {
                            ItemStack item = armorContents[i];
                            if (item != null && armorManager.isArmorPiece(item)) {
                                String id = armorManager.getArmorId(item);
                                if (id != null && id.equals(armorId)) {
                                    armorContents[i] = null;
                                    ++found;
                                    if (found >= amountToTake) {
                                        break;
                                    }
                                }
                            }
                        }

                        target.getInventory().setArmorContents(armorContents);
                    }

                    if (found > 0) {
                        sender.sendMessage("§aTook " + found + "x " + armorId + " from " + target.getName());
                        Map<String, String> placeholders = MessageUtils.placeholders();
                        placeholders.put("amount", String.valueOf(found));
                        placeholders.put("armor", armorId);
                        MessageUtils.sendMessage(target, "armor.taken", placeholders);
                        this.plugin.getArmorListener().checkPlayerArmor(target);
                    } else {
                        sender.sendMessage("§cNo " + armorId + " found in " + target.getName() + "'s inventory");
                    }

                    return true;
                }
            }
        }
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        ArmorManager armorManager = this.plugin.getArmorManager();
        Map<String, ArmorManager.ArmorPieceData> armorPieces = armorManager.getArmorPieces();
        if (armorPieces.isEmpty()) {
            sender.sendMessage("§cNo custom armor pieces found");
            return true;
        } else {
            sender.sendMessage("§6§l=== Custom Armor Pieces ===");

            for(Map.Entry<String, ArmorManager.ArmorPieceData> entry : armorPieces.entrySet()) {
                ArmorManager.ArmorPieceData piece = (ArmorManager.ArmorPieceData)entry.getValue();
                String multiplier = String.format("%.2f", piece.getMultiplier());
                String materialName = piece.getMaterial().toString();
                String permissionInfo = piece.getPermission() != null ? " §7(Permission: " + piece.getPermission() + ")" : "";
                sender.sendMessage("§e" + (String)entry.getKey() + " §7- §f" + materialName + " §7- §f+" + multiplier + "x" + permissionInfo);
            }

            return true;
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return new ArrayList();
        } else if (args.length == 1) {
            return this.filterStartsWith(Arrays.asList("give", "take", "list"), args[0]);
        } else if (args.length != 2 || !args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take")) {
            if (args.length != 3 || !args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take")) {
                return (List<String>)(args.length != 4 || !args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take") ? new ArrayList() : this.filterStartsWith(Arrays.asList("1", "5", "10", "64"), args[3]));
            } else {
                return this.filterStartsWith(new ArrayList(this.plugin.getArmorManager().getArmorPieces().keySet()), args[2]);
            }
        } else {
            return null;
        }
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            String lowercasePrefix = prefix.toLowerCase();
            return (List)list.stream().filter((s) -> s.toLowerCase().startsWith(lowercasePrefix)).collect(Collectors.toList());
        } else {
            return list;
        }
    }
}
