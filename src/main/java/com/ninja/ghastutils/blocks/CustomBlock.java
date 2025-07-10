
package com.ninja.ghastutils.blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public class CustomBlock {
    private final String id;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final Map<String, List<String>> commands;
    private String permission;
    private Integer customModelData;
    private long cooldown;

    public CustomBlock(String id, String name, Material material, List<String> lore) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.lore = new ArrayList(lore);
        this.commands = new HashMap();
        this.cooldown = 0L;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Material getMaterial() {
        return this.material;
    }

    public List<String> getLore() {
        return new ArrayList(this.lore);
    }

    public String getPermission() {
        return this.permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getCustomModelData() {
        return this.customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public long getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public Map<String, List<String>> getCommands() {
        Map<String, List<String>> result = new HashMap();

        for(Map.Entry<String, List<String>> entry : this.commands.entrySet()) {
            result.put((String)entry.getKey(), new ArrayList((Collection)entry.getValue()));
        }

        return result;
    }

    public void addCommands(String action, List<String> commandList) {
        this.commands.put(action, new ArrayList(commandList));
    }
}
