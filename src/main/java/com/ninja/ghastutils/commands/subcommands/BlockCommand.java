package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.blocks.BlockManager;
import com.ninja.ghastutils.blocks.CustomBlock;
import com.ninja.ghastutils.commands.SubCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BlockCommand extends SubCommand {
    private final GhastUtils plugin;

    public BlockCommand(GhastUtils plugin) {
        super("block", "ghastutils.block.admin", true);
        this.plugin = plugin;
        this.addAlias("blocks");
    }

    public String getDescription() {
        return "Manage interactive blocks";
    }

    public String getUsage() {
        return "/gutil block <get/set/remove/info/list> [id]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (this.checkPlayer(sender) && this.checkPermission(sender)) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: " + this.getUsage());
                return true;
            } else {
                Player player = (Player)sender;
                String action = args[0].toLowerCase();
                BlockManager blockManager = this.plugin.getBlockManager();
                switch (action) {
                    case "get":
                        if (args.length < 2) {
                            player.sendMessage("§cUsage: /gutil block get <id>");
                            return true;
                        }

                        String blockId = args[1];
                        CustomBlock customBlock = blockManager.getCustomBlock(blockId);
                        if (customBlock == null) {
                            player.sendMessage("§cCustom block not found: " + blockId);
                            return true;
                        }

                        ItemStack blockItem = blockManager.createBlockItem(blockId);
                        if (blockItem == null) {
                            player.sendMessage("§cFailed to create block item: " + blockId);
                            return true;
                        }

                        Map<Integer, ItemStack> leftover = player.getInventory().addItem(blockItem);
                        if (leftover.isEmpty()) {
                            player.sendMessage("§aGave " + blockId + " to you");
                        } else {
                            for(ItemStack item : leftover.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }

                            player.sendMessage("§aGave " + blockId + " to you (items were dropped)");
                        }
                        break;
                    case "set":
                        if (args.length < 2) {
                            player.sendMessage("§cUsage: /gutil block set <id>");
                            return true;
                        }

                        String setBlockId = args[1];
                        if (!blockManager.hasCustomBlock(setBlockId)) {
                            player.sendMessage("§cCustom block not found: " + setBlockId);
                            return true;
                        }

                        Block targetBlock = player.getTargetBlock((Set<Material>)null, 5);
                        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                            player.sendMessage("§cYou need to look at a block to set it as a custom block");
                            return true;
                        }

                        boolean success = blockManager.placeCustomBlock(targetBlock.getLocation(), setBlockId);
                        if (success) {
                            String locationStr = this.formatLocation(targetBlock.getLocation());
                            player.sendMessage("§aSet block at " + locationStr + " to " + setBlockId);
                        } else {
                            player.sendMessage("§cFailed to set custom block");
                        }
                        break;
                    case "remove":
                        Block targetRemoveBlock = player.getTargetBlock((Set<Material>)null, 5);
                        if (targetRemoveBlock == null || targetRemoveBlock.getType() == Material.AIR) {
                            player.sendMessage("§cYou need to look at a block to remove it");
                            return true;
                        }

                        if (!blockManager.isCustomBlock(targetRemoveBlock.getLocation())) {
                            player.sendMessage("§cThe block you're looking at is not a custom block");
                            return true;
                        }

                        String removedBlockId = blockManager.removeCustomBlock(targetRemoveBlock.getLocation());
                        if (removedBlockId != null) {
                            player.sendMessage("§aRemoved custom block " + removedBlockId + " from " + this.formatLocation(targetRemoveBlock.getLocation()));
                        } else {
                            player.sendMessage("§cFailed to remove custom block");
                        }
                        break;
                    case "info":
                        Block targetInfoBlock = player.getTargetBlock((Set<Material>)null, 5);
                        if (targetInfoBlock == null || targetInfoBlock.getType() == Material.AIR) {
                            player.sendMessage("§cYou need to look at a block to get information about it");
                            return true;
                        }

                        if (!blockManager.isCustomBlock(targetInfoBlock.getLocation())) {
                            player.sendMessage("§cThe block you're looking at is not a custom block");
                            return true;
                        }

                        String infoBlockId = blockManager.getCustomBlockId(targetInfoBlock.getLocation());
                        CustomBlock infoBlock = blockManager.getCustomBlock(infoBlockId);
                        if (infoBlock != null) {
                            player.sendMessage("§6§l=== Block Info ===");
                            player.sendMessage("§eID: §f" + infoBlockId);
                            player.sendMessage("§eType: §f" + infoBlock.getMaterial().toString());
                            player.sendMessage("§eName: §f" + infoBlock.getName());
                            String locationStr = this.formatLocation(targetInfoBlock.getLocation());
                            player.sendMessage("§eLocation: §f" + locationStr);
                            String permissionStr = infoBlock.getPermission() != null ? infoBlock.getPermission() : "None";
                            player.sendMessage("§ePermission: §f" + permissionStr);
                            if (!infoBlock.getCommands().isEmpty()) {
                                player.sendMessage("§eCommands:");

                                for(Map.Entry<String, List<String>> entry : infoBlock.getCommands().entrySet()) {
                                    player.sendMessage("§f  " + entry.getKey() + ":");

                                    for(String cmd : entry.getValue()) {
                                        player.sendMessage("§f    - " + cmd);
                                    }
                                }
                            }
                        } else {
                            player.sendMessage("§cFailed to get block info for ID: " + infoBlockId);
                        }
                        break;
                    case "list":
                        Map<String, CustomBlock> customBlocks = blockManager.getCustomBlocks();
                        if (customBlocks.isEmpty()) {
                            player.sendMessage("§cNo custom blocks found");
                            return true;
                        }

                        player.sendMessage("§6§l=== Custom Blocks ===");

                        for(Map.Entry<String, CustomBlock> entry : customBlocks.entrySet()) {
                            CustomBlock block = entry.getValue();
                            String permissionInfo = block.getPermission() != null ? " §7(Permission: " + block.getPermission() + ")" : "";
                            String blockIdStr = entry.getKey();
                            player.sendMessage("§e" + blockIdStr + " §7- §f" + block.getName() + " §7- §f" + block.getMaterial().toString() + permissionInfo);
                        }
                        break;
                    default:
                        player.sendMessage("§cUnknown action: " + action);
                        player.sendMessage("§cUsage: " + this.getUsage());
                }

                return true;
            }
        } else {
            return true;
        }
    }

    private String formatLocation(Location location) {
        String worldName = location.getWorld().getName();
        return worldName + " [" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "]";
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return new ArrayList<>();
        } else if (args.length == 1) {
            return this.filterStartsWith(Arrays.asList("get", "set", "remove", "info", "list"), args[0]);
        } else {
            return args.length != 2 || (!args[0].equalsIgnoreCase("get") && !args[0].equalsIgnoreCase("set")) ?
                    new ArrayList<>() :
                    this.filterStartsWith(new ArrayList<>(this.plugin.getBlockManager().getCustomBlocks().keySet()), args[1]);
        }
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            String lowercasePrefix = prefix.toLowerCase();
            return list.stream().filter(s -> s.toLowerCase().startsWith(lowercasePrefix)).collect(Collectors.toList());
        } else {
            return list;
        }
    }
}