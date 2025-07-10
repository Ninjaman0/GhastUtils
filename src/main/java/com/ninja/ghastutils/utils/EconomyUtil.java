
package com.ninja.ghastutils.utils;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import java.math.BigDecimal;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EconomyUtil {
    private static Essentials essentials;
    private static boolean initialized = false;

    public static void initialize() {
        if (Bukkit.getPluginManager().getPlugin("Essentials") == null) {
            LogManager.error("EssentialsX not found! Economy features will be disabled.");
            initialized = false;
        } else {
            try {
                essentials = (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
                initialized = true;
                LogManager.info("Successfully hooked into EssentialsX economy!");
            } catch (Exception e) {
                LogManager.error("Failed to hook into EssentialsX: " + e.getMessage(), e);
                initialized = false;
            }

        }
    }

    public static boolean isInitialized() {
        return initialized && essentials != null;
    }

    public static boolean deposit(Player player, double amount, String source) {
        if (!isInitialized()) {
            LogManager.error("Tried to deposit money but EssentialsX is not initialized");
            return false;
        } else {
            try {
                User user = essentials.getUser(player);
                user.giveMoney(BigDecimal.valueOf(amount));
                Map<String, String> placeholders = MessageUtils.placeholders();
                placeholders.put("amount", formatCurrency(amount));
                placeholders.put("balance", formatCurrency(getBalance(player)));
                LogManager.transaction("DEPOSIT", player.getName(), amount, source, "SUCCESS");
                return true;
            } catch (Exception e) {
                LogManager.error("Error depositing money for " + player.getName(), e);
                return false;
            }
        }
    }

    public static boolean withdraw(Player player, double amount, String source) {
        if (!isInitialized()) {
            LogManager.error("Tried to withdraw money but EssentialsX is not initialized");
            return false;
        } else {
            try {
                User user = essentials.getUser(player);
                if (user.getMoney().doubleValue() < amount) {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("amount", formatCurrency(amount));
                    MessageUtils.sendMessage(player, "economy.insufficient-funds", placeholders);
                    LogManager.transaction("WITHDRAW", player.getName(), amount, source, "INSUFFICIENT_FUNDS");
                    return false;
                } else {
                    user.takeMoney(BigDecimal.valueOf(amount));
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("amount", formatCurrency(amount));
                    placeholders.put("balance", formatCurrency(getBalance(player)));
                    LogManager.transaction("WITHDRAW", player.getName(), amount, source, "SUCCESS");
                    return true;
                }
            } catch (Exception e) {
                LogManager.error("Error withdrawing money from " + player.getName(), e);
                return false;
            }
        }
    }

    public static double getBalance(Player player) {
        if (!isInitialized()) {
            LogManager.error("Tried to get balance but EssentialsX is not initialized");
            return (double)0.0F;
        } else {
            try {
                User user = essentials.getUser(player);
                return user.getMoney().doubleValue();
            } catch (Exception e) {
                LogManager.error("Error getting balance for " + player.getName(), e);
                return (double)0.0F;
            }
        }
    }

    public static boolean has(Player player, double amount) {
        if (!isInitialized()) {
            LogManager.error("Tried to check balance but EssentialsX is not initialized");
            return false;
        } else {
            try {
                User user = essentials.getUser(player);
                return user.getMoney().doubleValue() >= amount;
            } catch (Exception e) {
                LogManager.error("Error checking balance for " + player.getName(), e);
                return false;
            }
        }
    }

    public static String formatCurrency(double amount) {
        if (!isInitialized()) {
            return String.format("$%.2f", amount);
        } else {
            try {
                return essentials.getSettings().getCurrencyFormat().format(BigDecimal.valueOf(amount));
            } catch (Exception var3) {
                return String.format("$%.2f", amount);
            }
        }
    }
}
