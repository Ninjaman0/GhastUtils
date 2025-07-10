
package com.ninja.ghastutils.commands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.subcommands.ArmorCommand;
import com.ninja.ghastutils.commands.subcommands.AutoSellCommand;
import com.ninja.ghastutils.commands.subcommands.AutoCraftCommand;
import com.ninja.ghastutils.commands.subcommands.BlockCommand;
import com.ninja.ghastutils.commands.subcommands.BoosterCommand;
import com.ninja.ghastutils.commands.subcommands.CompactorCommand;
import com.ninja.ghastutils.commands.subcommands.CraftingCommand;
import com.ninja.ghastutils.commands.subcommands.EventMultiplierCommand;
import com.ninja.ghastutils.commands.subcommands.HelpCommand;
import com.ninja.ghastutils.commands.subcommands.IngredientCommand;
import com.ninja.ghastutils.commands.subcommands.MultiplierCommand;
import com.ninja.ghastutils.commands.subcommands.PetCommand;
import com.ninja.ghastutils.commands.subcommands.ReloadCommand;
import com.ninja.ghastutils.commands.subcommands.SellCommand;
import com.ninja.ghastutils.commands.subcommands.SellItemCommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class MainCommand implements CommandExecutor, TabCompleter {
    private final GhastUtils plugin;
    private final Map<String, SubCommand> subCommands;

    public MainCommand(GhastUtils plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap();
        this.registerSubCommands();
    }

    private void registerSubCommands() {
        this.registerSubCommand(new ReloadCommand(this.plugin));
        this.registerSubCommand(new BoosterCommand(this.plugin));
        this.registerSubCommand(new PetCommand(this.plugin));
        this.registerSubCommand(new MultiplierCommand(this.plugin));
        this.registerSubCommand(new EventMultiplierCommand(this.plugin));
        this.registerSubCommand(new SellCommand(this.plugin));
        this.registerSubCommand(new SellItemCommand(this.plugin));
        this.registerSubCommand(new CraftingCommand(this.plugin));
        this.registerSubCommand(new IngredientCommand(this.plugin));
        this.registerSubCommand(new AutoCraftCommand(this.plugin));
        this.registerSubCommand(new CompactorCommand(this.plugin));
        this.registerSubCommand(new BlockCommand(this.plugin));
        this.registerSubCommand(new AutoSellCommand(this.plugin));
        this.registerSubCommand(new ArmorCommand(this.plugin));
        this.registerSubCommand(new HelpCommand(this.plugin, this.subCommands));
    }

    public void registerSubCommand(SubCommand subCommand) {
        this.subCommands.put(subCommand.getName().toLowerCase(), subCommand);

        for(String alias : subCommand.getAliases()) {
            this.subCommands.put(alias.toLowerCase(), subCommand);
        }

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6§l=== GhastUtils ===");
            sender.sendMessage("§eVersion: §f" + this.plugin.getDescription().getVersion());
            sender.sendMessage("§eUse §f/gutil help §efor a list of commands");
            return true;
        } else {
            String subCommand = args[0].toLowerCase();
            if (this.subCommands.containsKey(subCommand)) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);

                try {
                    return ((SubCommand)this.subCommands.get(subCommand)).execute(sender, subArgs);
                } catch (Exception e) {
                    sender.sendMessage("§cAn error occurred while executing the command");
                    this.plugin.getLogger().severe("Error executing command: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            } else {
                sender.sendMessage("§cUnknown command. Use §f/gutil help §cfor a list of commands");
                return true;
            }
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList();
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();

            for(Map.Entry<String, SubCommand> entry : this.subCommands.entrySet()) {
                SubCommand subCommand = (SubCommand)entry.getValue();
                if (((String)entry.getKey()).equals(subCommand.getName()) && ((String)entry.getKey()).startsWith(partialName) && subCommand.hasPermission(sender)) {
                    completions.add((String)entry.getKey());
                }
            }
        } else if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            if (this.subCommands.containsKey(subCommandName)) {
                SubCommand subCommand = (SubCommand)this.subCommands.get(subCommandName);
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                return subCommand.tabComplete(sender, subArgs);
            }
        }

        return completions;
    }
}
