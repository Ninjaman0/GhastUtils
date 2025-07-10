package com.ninja.ghastutils.commands.subcommands;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.commands.SubCommand;
import com.ninja.ghastutils.gui.CompactorGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CompactorCommand extends SubCommand {
    private final GhastUtils plugin;

    public CompactorCommand(GhastUtils plugin) {
        super("compactor", "ghastutils.crafting.compactor", true);
        this.plugin = plugin;
        this.addAlias("compact");
    }

    @Override
    public String getDescription() {
        return "Open the compactor GUI for automated crafting";
    }

    @Override
    public String getUsage() {
        return "/gutil compactor";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!checkPlayer(sender) || !checkPermission(sender)) {
            return true;
        }

        Player player = (Player) sender;
        CompactorGui gui = new CompactorGui(plugin, player);
        gui.open();

        return true;
    }
}