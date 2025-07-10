
package com.ninja.ghastutils.sell;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class SellSession {
    private final List<ItemStack> items = new ArrayList();
    private double totalValue = (double)0.0F;

    public void addItem(ItemStack item, double value) {
        this.items.add(item.clone());
        this.totalValue += value;
    }

    public List<ItemStack> getItems() {
        return new ArrayList(this.items);
    }

    public double getTotalValue() {
        return this.totalValue;
    }

    public void clear() {
        this.items.clear();
        this.totalValue = (double)0.0F;
    }
}
