
package com.ninja.ghastutils.sell;

import java.util.Map;

public class SellResult {
    private final double totalPrice;
    private final int itemsSold;
    private final Map<String, Integer> soldItems;
    private final double multiplier;

    public SellResult(double totalPrice, int itemsSold, Map<String, Integer> soldItems, double multiplier) {
        this.totalPrice = totalPrice;
        this.itemsSold = itemsSold;
        this.soldItems = soldItems;
        this.multiplier = multiplier;
    }

    public double getTotalPrice() {
        return this.totalPrice;
    }

    public int getItemsSold() {
        return this.itemsSold;
    }

    public Map<String, Integer> getSoldItems() {
        return this.soldItems;
    }

    public double getMultiplier() {
        return this.multiplier;
    }
}
