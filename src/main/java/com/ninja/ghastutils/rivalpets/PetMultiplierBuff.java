
package com.ninja.ghastutils.rivalpets;

import com.ninja.ghastutils.GhastUtils;
import me.rivaldev.rivalpets.api.RivalPetsAPI;
import me.rivaldev.rivalpets.buffs.PetBuffRegister;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class PetMultiplierBuff extends PetBuffRegister {
    private final GhastUtils plugin;
    private static final String BUFF_NAME = "ghastutils_multiplier";

    public PetMultiplierBuff(GhastUtils plugin) {
        this.plugin = plugin;
        RivalPetsAPI.getApi().registerBuff(this, "GhastUtils");
    }

    public String getBuffName() {
        return "ghastutils_multiplier";
    }

    public void onActivatePet(Player player) {
        this.updateMultiplier(player);
    }

    public void onDeactivatePet(Player player) {
        this.plugin.getMultiplierManager().setPetMultiplier(player.getUniqueId(), (double)0.0F, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (RivalPetsAPI.getApi().hasBuff(player, this.getBuffName())) {
            this.updateMultiplier(player);
        }

    }

    private void updateMultiplier(Player player) {
        double buffBoost = RivalPetsAPI.getApi().getBuffBoost(player, this.getBuffName());
        this.plugin.getMultiplierManager().setPetMultiplier(player.getUniqueId(), buffBoost, true);
    }
}
