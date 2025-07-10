//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.ninja.ghastutils.config;

public enum ConfigType {
    MAIN("config.yml"),
    PLAYERS("players.yml"),
    SELL("sell.yml"),
    SELL_GUI("sell_gui.yml"),
    CRAFTING("crafting.yml"),
    MESSAGES("messages.yml"),
    BLOCK_COMMANDS("block_commands.yml"),
    ECONOMY_FALLBACK("fallback_economy.yml");

    private final String fileName;

    private ConfigType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }
}
