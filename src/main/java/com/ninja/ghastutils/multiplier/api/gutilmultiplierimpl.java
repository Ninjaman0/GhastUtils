
package com.ninja.ghastutils.multiplier.api;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.multiplier.MultiplierManager;
import org.bukkit.entity.Player;

public class gutilmultiplierimpl implements MultiplierManagerinter {
    private final GhastUtils plugin;

    public gutilmultiplierimpl(GhastUtils plugin) {
        this.plugin = plugin;
    }

    public double getTotalMultiplier(Player player) {
        if (player == null) {
            return (double)1.0F;
        } else {
            MultiplierManager multiplierManager = this.plugin.getMultiplierManager();
            return multiplierManager == null ? (double)1.0F : multiplierManager.getTotalMultiplier(player.getUniqueId());
        }
    }
}
