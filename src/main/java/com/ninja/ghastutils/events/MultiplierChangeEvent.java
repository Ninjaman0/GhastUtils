
package com.ninja.ghastutils.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MultiplierChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final double newMultiplier;

    public MultiplierChangeEvent(Player player, double newMultiplier) {
        this.player = player;
        this.newMultiplier = newMultiplier;
    }

    public Player getPlayer() {
        return this.player;
    }

    public double getNewMultiplier() {
        return this.newMultiplier;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
