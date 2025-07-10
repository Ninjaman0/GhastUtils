
package com.ninja.ghastutils.utils;

import com.ninja.ghastutils.GhastUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NBTUtils {
    private static final GhastUtils plugin = GhastUtils.getInstance();

    public static void setString(ItemMeta meta, String key, String value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.STRING, value);
        }
    }

    public static String getString(ItemMeta meta, String key) {
        if (meta == null) {
            return null;
        } else {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(namespacedKey, PersistentDataType.STRING) ? (String)container.get(namespacedKey, PersistentDataType.STRING) : null;
        }
    }

    public static boolean hasString(ItemMeta meta, String key) {
        if (meta == null) {
            return false;
        } else {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(namespacedKey, PersistentDataType.STRING);
        }
    }

    public static void setInt(ItemMeta meta, String key, int value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.INTEGER, value);
        }
    }

    public static int getInt(ItemMeta meta, String key) {
        if (meta == null) {
            return 0;
        } else {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(namespacedKey, PersistentDataType.INTEGER) ? (Integer)container.get(namespacedKey, PersistentDataType.INTEGER) : 0;
        }
    }

    public static void setDouble(ItemMeta meta, String key, double value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.DOUBLE, value);
        }
    }

    public static double getDouble(ItemMeta meta, String key) {
        if (meta == null) {
            return (double)0.0F;
        } else {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(namespacedKey, PersistentDataType.DOUBLE) ? (Double)container.get(namespacedKey, PersistentDataType.DOUBLE) : (double)0.0F;
        }
    }

    public static void setBoolean(ItemMeta meta, String key, boolean value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.BYTE, Byte.valueOf((byte)(value ? 1 : 0)));
        }
    }

    public static boolean getBoolean(ItemMeta meta, String key) {
        if (meta == null) {
            return false;
        } else {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(namespacedKey, PersistentDataType.BYTE)) {
                return (Byte)container.get(namespacedKey, PersistentDataType.BYTE) == 1;
            } else {
                return false;
            }
        }
    }

    public static void remove(ItemMeta meta, String key) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.remove(namespacedKey);
        }
    }

    public static Map<String, String> getAllStrings(ItemMeta meta) {
        Map<String, String> result = new HashMap();
        if (meta == null) {
            return result;
        } else {
            PersistentDataContainer container = meta.getPersistentDataContainer();

            for(NamespacedKey key : container.getKeys()) {
                if (key.getNamespace().equals(plugin.getName().toLowerCase()) && container.has(key, PersistentDataType.STRING)) {
                    String value = (String)container.get(key, PersistentDataType.STRING);
                    result.put(key.getKey(), value);
                }
            }

            return result;
        }
    }

    public static void setCompound(ItemMeta meta, String key, Map<String, String> values) {
        if (meta != null && values != null) {
            for(Map.Entry<String, String> entry : values.entrySet()) {
                setString(meta, key + "." + (String)entry.getKey(), (String)entry.getValue());
            }

        }
    }

    public static Map<String, String> getCompound(ItemMeta meta, String key) {
        Map<String, String> result = new HashMap();
        if (meta == null) {
            return result;
        } else {
            Map<String, String> allValues = getAllStrings(meta);
            String prefix = key + ".";

            for(Map.Entry<String, String> entry : allValues.entrySet()) {
                if (((String)entry.getKey()).startsWith(prefix)) {
                    String subKey = ((String)entry.getKey()).substring(prefix.length());
                    result.put(subKey, (String)entry.getValue());
                }
            }

            return result;
        }
    }
}
