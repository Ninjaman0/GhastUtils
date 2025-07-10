
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;

public class HelpCommand extends SubCommand {
    private final GhastUtils plugin;
    private final Map<String, SubCommand> subCommands;

    public HelpCommand(GhastUtils plugin, Map<String, SubCommand> subCommands) {
        super("help", (String)null, false);
        this.plugin = plugin;
        this.subCommands = subCommands;
    }

    public String getDescription() {
        return "Show help for GhastUtils commands";
    }

    public String getUsage() {
        return "/gutil help [page]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException var11) {
                sender.sendMessage("§cInvalid page number");
                return true;
            }
        }

        List<SubCommand> availableCommands = new ArrayList();

        for(SubCommand cmd : this.subCommands.values()) {
            if (!availableCommands.contains(cmd) && cmd.hasPermission(sender)) {
                availableCommands.add(cmd);
            }
        }

        int commandsPerPage = 6;
        int totalPages = (int)Math.ceil((double)availableCommands.size() / (double)commandsPerPage);
        if (page < 1) {
            page = 1;
        } else if (page > totalPages) {
            page = totalPages;
        }

        sender.sendMessage("§6§l=== GhastUtils Help (Page " + page + "/" + totalPages + ") ===");
        int startIndex = (page - 1) * commandsPerPage;
        int endIndex = Math.min(startIndex + commandsPerPage, availableCommands.size());

        for(int i = startIndex; i < endIndex; ++i) {
            SubCommand cmd = (SubCommand)availableCommands.get(i);
            String var10001 = cmd.getUsage();
            sender.sendMessage("§e" + var10001 + " §7- §f" + cmd.getDescription());
        }

        if (page < totalPages) {
            sender.sendMessage("§7Use §f/gutil help " + (page + 1) + " §7to see the next page");
        }

        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return new ArrayList();
        } else {
            List<String> completions = new ArrayList();
            List<SubCommand> availableCommands = new ArrayList();

            for(SubCommand cmd : this.subCommands.values()) {
                if (!availableCommands.contains(cmd) && cmd.hasPermission(sender)) {
                    availableCommands.add(cmd);
                }
            }

            int commandsPerPage = 6;
            int totalPages = (int)Math.ceil((double)availableCommands.size() / (double)commandsPerPage);

            for(int i = 1; i <= totalPages; ++i) {
                completions.add(String.valueOf(i));
            }

            return this.filterStartsWith(completions, args[0]);
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
