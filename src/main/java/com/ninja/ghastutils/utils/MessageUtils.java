
package com.ninja.ghastutils.utils;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.config.ConfigType;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MessageUtils {
    private static GhastUtils plugin;
    private static FileConfiguration messagesConfig;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    public static void initialize(GhastUtils pluginInstance) {
        plugin = pluginInstance;
        reload();
    }

    public static void reload() {
        messagesConfig = plugin.getConfigManager().getConfig(ConfigType.MESSAGES);
    }

    public static String getMessage(String key, Map<String, String> placeholders) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            return "§cMissing message: " + key;
        } else {
            if (placeholders != null) {
                for(Map.Entry<String, String> entry : placeholders.entrySet()) {
                    message = message.replace("%" + (String)entry.getKey() + "%", (CharSequence)entry.getValue());
                }
            }

            return translateColors(message);
        }
    }

    public static String getMessage(String key) {
        return getMessage(key, (Map)null);
    }

    public static void sendMessage(Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        player.sendMessage(translateColors(message));
    }

    public static void sendMessage(Player player, String key) {
        sendMessage((Player)player, key, (Map)null);
    }

    public static void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        sender.sendMessage(translateColors(message));
    }

    public static void sendMessage(CommandSender sender, String key) {
        sendMessage((CommandSender)sender, key, (Map)null);
    }

    public static Map<String, String> placeholders() {
        return new HashMap();
    }

    public static String translateItemColors(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> translateItemColors(List<String> list) {
        List<String> result = new ArrayList();
        if (list == null) {
            return result;
        } else {
            for(String line : list) {
                result.add(translateItemColors(line));
            }

            return result;
        }
    }

    public static String translateColors(String text) {
        if (text == null) {
            return "";
        } else {
            String result = ChatColor.translateAlternateColorCodes('&', text);
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer();

            while(matcher.find()) {
                String hexColor = matcher.group(1);

                try {
                    String replacement = getClosestChatColor(hexColor).toString();
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                } catch (Exception var6) {
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                }
            }

            matcher.appendTail(buffer);
            return buffer.toString();
        }
    }

    public static List<String> translateColors(List<String> list) {
        List<String> result = new ArrayList();
        if (list == null) {
            return result;
        } else {
            for(String line : list) {
                result.add(translateColors(line));
            }

            return result;
        }
    }

    public static String stripColors(String text) {
        if (text == null) {
            return "";
        } else {
            Matcher matcher = HEX_PATTERN.matcher(text);
            String noHex = matcher.replaceAll("");
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', noHex));
        }
    }

    public static List<String> stripColors(List<String> list) {
        List<String> result = new ArrayList();
        if (list == null) {
            return result;
        } else {
            for(String line : list) {
                result.add(stripColors(line));
            }

            return result;
        }
    }

    private static ChatColor getClosestChatColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            ChatColor closestColor = ChatColor.WHITE;
            double minDistance = Double.MAX_VALUE;

            for(ChatColor color : ChatColor.values()) {
                if (color != ChatColor.BOLD && color != ChatColor.ITALIC && color != ChatColor.UNDERLINE && color != ChatColor.STRIKETHROUGH && color != ChatColor.MAGIC && color != ChatColor.RESET) {
                    Color javaColor = getColorFromChatColor(color);
                    double distance = colorDistance(r, g, b, javaColor.getRed(), javaColor.getGreen(), javaColor.getBlue());
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestColor = color;
                    }
                }
            }

            return closestColor;
        } catch (Exception var14) {
            return ChatColor.WHITE;
        }
    }

    private static Color getColorFromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK:
                return new Color(0, 0, 0);
            case DARK_BLUE:
                return new Color(0, 0, 170);
            case DARK_GREEN:
                return new Color(0, 170, 0);
            case DARK_AQUA:
                return new Color(0, 170, 170);
            case DARK_RED:
                return new Color(170, 0, 0);
            case DARK_PURPLE:
                return new Color(170, 0, 170);
            case GOLD:
                return new Color(255, 170, 0);
            case GRAY:
                return new Color(170, 170, 170);
            case DARK_GRAY:
                return new Color(85, 85, 85);
            case BLUE:
                return new Color(85, 85, 255);
            case GREEN:
                return new Color(85, 255, 85);
            case AQUA:
                return new Color(85, 255, 255);
            case RED:
                return new Color(255, 85, 85);
            case LIGHT_PURPLE:
                return new Color(255, 85, 255);
            case YELLOW:
                return new Color(255, 255, 85);
            case WHITE:
            default:
                return new Color(255, 255, 255);
        }
    }

    private static double colorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        return Math.sqrt(Math.pow((double)(r2 - r1), (double)2.0F) + Math.pow((double)(g2 - g1), (double)2.0F) + Math.pow((double)(b2 - b1), (double)2.0F));
    }
}
