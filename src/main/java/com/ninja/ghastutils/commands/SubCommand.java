
package com.ninja.ghastutils.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class SubCommand {
    private final String name;
    private final String permission;
    private final boolean playerOnly;
    private final List<String> aliases;

    public SubCommand(String name, String permission, boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = new ArrayList();
    }

    public String getName() {
        return this.name;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean isPlayerOnly() {
        return this.playerOnly;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    protected void addAlias(String alias) {
        this.aliases.add(alias);
    }

    public boolean hasPermission(CommandSender sender) {
        return this.permission == null || sender.hasPermission(this.permission);
    }

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract boolean execute(CommandSender var1, String[] var2);

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList();
    }

    protected boolean checkPlayer(CommandSender sender) {
        if (this.playerOnly && !(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players");
            return false;
        } else {
            return true;
        }
    }

    protected boolean checkPermission(CommandSender sender) {
        if (this.permission != null && !sender.hasPermission(this.permission)) {
            sender.sendMessage("§cYou don't have permission to use this command");
            return false;
        } else {
            return true;
        }
    }
}
