
package com.ninja.ghastutils.sell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public class SellableItem {
    private final String id;
    private final String name;
    private final Material material;
    private final double basePrice;
    private final List<String> lore;
    private final int customModelData;
    private String permission;
    private Map<String, String> nbtData;

    public SellableItem(String id, String name, Material material, double basePrice, List<String> lore, int customModelData) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.basePrice = basePrice;
        this.lore = lore;
        this.customModelData = customModelData;
        this.permission = null;
        this.nbtData = new HashMap();
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

    public double getBasePrice() {
        return this.basePrice;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public int getCustomModelData() {
        return this.customModelData;
    }

    public String getPermission() {
        return this.permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean hasNbtData() {
        return this.nbtData != null && !this.nbtData.isEmpty();
    }

    public Map<String, String> getNbtData() {
        return this.nbtData;
    }

    public void setNbtData(Map<String, String> nbtData) {
        this.nbtData = (Map<String, String>)(nbtData != null ? nbtData : new HashMap());
    }

    public void addNbtData(String key, String value) {
        if (this.nbtData == null) {
            this.nbtData = new HashMap();
        }

        this.nbtData.put(key, value);
    }
}
