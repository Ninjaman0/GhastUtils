
package com.ninja.ghastutils.papi;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.multiplier.MultiplierManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholderManager extends PlaceholderExpansion {
    private final GhastUtils plugin;

    public PlaceholderManager(GhastUtils plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "ghastutils";
    }

    public String getAuthor() {
        return "Ninja0_0";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return "";
        } else if (identifier.equals("multiplier_total")) {
            double multiplier = this.plugin.getMultiplierManager().getTotalMultiplier(player.getUniqueId());
            return String.format("%.2f", multiplier);
        } else if (identifier.equals("booster_timeleft")) {
            long timeLeft = this.plugin.getMultiplierManager().getBoosterTimeLeft(player.getUniqueId());
            return timeLeft <= 0L ? "None" : this.formatTime(timeLeft);
        } else if (identifier.equals("booster_active")) {
            return this.plugin.getMultiplierManager().getBoosterTimeLeft(player.getUniqueId()) > 0L ? "true" : "false";
        } else if (identifier.equals("pet_active")) {
            return this.plugin.getMultiplierManager().isPetActive(player.getUniqueId()) ? "true" : "false";
        } else {
            if (identifier.startsWith("crafting_")) {
                String[] parts = identifier.split("_", 3);
                if (parts.length < 3) {
                    return null;
                }

                String itemId = parts[1];
                String property = parts[2];
                if (property.equals("cost")) {
                    return this.plugin.getCraftingManager().getRecipeCostFormatted(itemId);
                }

                if (property.equals("permission")) {
                    return this.plugin.getCraftingManager().getCustomItem(itemId) != null ? this.plugin.getCraftingManager().getCustomItem(itemId).getPermission() : "None";
                }

                if (property.equals("no_vanilla")) {
                    return this.plugin.getCraftingManager().getCustomItem(itemId) != null ? String.valueOf(this.plugin.getCraftingManager().getCustomItem(itemId).isVanillaFeaturesDisabled()) : "false";
                }
            }

            if (identifier.startsWith("sellprice_")) {
                String itemId = identifier.substring("sellprice_".length());
                if (player.isOnline()) {
                    double price = this.plugin.getSellManager().getSellPrice(itemId, player.getPlayer());
                    return String.format("%.2f", price);
                }
            }

            if (identifier.equals("sell_gui")) {
                return "0.00";
            } else if (identifier.equals("total_sold")) {
                return "0.00";
            } else {
                if (identifier.startsWith("block_")) {
                    String[] parts = identifier.split("_", 2);
                    if (parts.length < 2) {
                        return null;
                    }

                    String property = parts[1];
                    if (property.equals("cooldown") && player.isOnline()) {
                        return String.valueOf(this.plugin.getBlockManager().getDefaultCooldown());
                    }
                }

                if (identifier.startsWith("event_")) {
                    String[] parts = identifier.split("_", 2);
                    if (parts.length < 2) {
                        return null;
                    }

                    String property = parts[1];
                    if (property.equals("active")) {
                        return this.plugin.getMultiplierManager().getEventMultipliers().isEmpty() ? "false" : "true";
                    }

                    if (property.equals("count")) {
                        return String.valueOf(this.plugin.getMultiplierManager().getEventMultipliers().size());
                    }

                    if (property.equals("multiplier")) {
                        double eventMultiplier = (double)0.0F;

                        for(MultiplierManager.EventMultiplier event : this.plugin.getMultiplierManager().getEventMultipliers().values()) {
                            eventMultiplier += event.getValue();
                        }

                        return String.format("%.2f", eventMultiplier);
                    }
                }

                return null;
            }
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60L) {
            return seconds + "s";
        } else {
            return seconds < 3600L ? seconds / 60L + "m " + seconds % 60L + "s" : seconds / 3600L + "h " + seconds % 3600L / 60L + "m";
        }
    }
}
