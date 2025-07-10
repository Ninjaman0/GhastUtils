
package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.config.ConfigType;
import com.ninja.ghastutils.utils.MessageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends SubCommand {
    private final GhastUtils plugin;

    public ReloadCommand(GhastUtils plugin) {
        super("reload", "ghastutils.admin.reload", false);
        this.plugin = plugin;
        this.addAlias("r");
    }

    public String getDescription() {
        return "Reload the plugin configuration";
    }

    public String getUsage() {
        return "/gutil reload [config/messages/all]";
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("permission", this.getPermission());
            MessageUtils.sendMessage(sender, "permission.denied", placeholders);
            return true;
        } else {
            MessageUtils.sendMessage(sender, "reload.start");

            try {
                if (args.length > 0) {
                    switch (args[0].toLowerCase()) {
                        case "config":
                            this.plugin.getConfigManager().reloadConfig(ConfigType.MAIN);
                            MessageUtils.sendMessage(sender, "reload.config");
                            break;
                        case "messages":
                            this.plugin.getConfigManager().reloadConfig(ConfigType.MESSAGES);
                            MessageUtils.reload();
                            MessageUtils.sendMessage(sender, "reload.messages");
                            break;
                        case "all":
                        default:
                            this.plugin.reload();
                            MessageUtils.sendMessage(sender, "reload.success");
                    }
                } else {
                    this.plugin.reload();
                    MessageUtils.sendMessage(sender, "reload.success");
                }
            } catch (Exception e) {
                this.plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
                e.printStackTrace();
                Map<String, String> placeholders = MessageUtils.placeholders();
                placeholders.put("error", e.getMessage());
                MessageUtils.sendMessage(sender, "reload.error", placeholders);
            }

            return true;
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermission(sender)) {
            return new ArrayList();
        } else {
            return (List<String>)(args.length == 1 ? this.filterStartsWith(Arrays.asList("config", "messages", "all"), args[0]) : new ArrayList());
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
