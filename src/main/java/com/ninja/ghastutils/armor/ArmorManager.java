package com.ninja.ghastutils.armor;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.utils.ItemUtils;
import com.ninja.ghastutils.utils.LogManager;
import com.ninja.ghastutils.utils.MessageUtils;
import com.ninja.ghastutils.utils.NBTUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class ArmorManager {
    private final GhastUtils plugin;
    private final Map<String, ArmorPieceData> armorPieces = new HashMap();
    private boolean enabled;
    private boolean particlesEnabled;
    private String particleType;
    private int particleCount;
    private double particleRadius;

    public ArmorManager(GhastUtils plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        this.armorPieces.clear();
        FileConfiguration config = this.plugin.getConfig();
        this.enabled = config.getBoolean("armor-multipliers.enabled", true);
        ConfigurationSection particlesSection = config.getConfigurationSection("armor-multipliers.particles");
        if (particlesSection != null) {
            this.particlesEnabled = particlesSection.getBoolean("enabled", true);
            this.particleType = particlesSection.getString("type", "WITCH");
            this.particleCount = particlesSection.getInt("count", 15);
            this.particleRadius = particlesSection.getDouble("radius", (double)0.5F);
        } else {
            this.particlesEnabled = true;
            this.particleType = "WITCH";
            this.particleCount = 15;
            this.particleRadius = (double)0.5F;
        }

        if (this.enabled) {
            ConfigurationSection armorSection = config.getConfigurationSection("armor-multipliers.pieces");
            if (armorSection == null) {
                LogManager.warning("No armor pieces found in config!");
            } else {
                for(String armorId : armorSection.getKeys(false)) {
                    ConfigurationSection pieceSection = armorSection.getConfigurationSection(armorId);
                    if (pieceSection != null) {
                        String materialName = pieceSection.getString("material", "DIAMOND_HELMET");

                        Material material;
                        try {
                            material = Material.valueOf(materialName.toUpperCase());
                        } catch (IllegalArgumentException var21) {
                            LogManager.warning("Invalid material for armor piece " + armorId + ": " + materialName);
                            continue;
                        }

                        String name = pieceSection.getString("name", materialName);
                        List<String> lore = pieceSection.getStringList("lore");
                        double multiplier = pieceSection.getDouble("multiplier", 0.1);
                        String permission = pieceSection.getString("permission", (String)null);
                        Integer customModelData = pieceSection.contains("custom-model-data") ? pieceSection.getInt("custom-model-data") : null;
                        String headTexture = pieceSection.getString("head-texture", "");
                        String headOwner = pieceSection.getString("head-owner", "");
                        Color color = null;
                        if (pieceSection.contains("color")) {
                            try {
                                String colorStr = pieceSection.getString("color");
                                if (colorStr != null) {
                                    if (colorStr.startsWith("#")) {
                                        color = Color.fromRGB(Integer.parseInt(colorStr.substring(1), 16));
                                    } else {
                                        String[] rgb = colorStr.split(",");
                                        if (rgb.length == 3) {
                                            color = Color.fromRGB(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LogManager.warning("Invalid color format for armor piece " + armorId + ": " + e.getMessage());
                            }
                        }

                        this.armorPieces.put(armorId, new ArmorPieceData(material, name, lore, multiplier, permission, customModelData, color, headTexture, headOwner));
                        LogManager.debug("Loaded armor piece: " + armorId + " with multiplier: " + multiplier);
                    }
                }

                LogManager.info("Loaded " + this.armorPieces.size() + " armor pieces with multipliers");
            }
        }
    }

    public ItemStack createArmorItem(String armorId) {
        if (!this.armorPieces.containsKey(armorId)) {
            return null;
        } else {
            ArmorPieceData piece = (ArmorPieceData)this.armorPieces.get(armorId);
            ItemStack item = new ItemStack(piece.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            } else {
                if (piece.getName() != null) {
                    meta.setDisplayName(MessageUtils.translateColors(piece.getName()));
                }

                if (!piece.getLore().isEmpty()) {
                    meta.setLore(MessageUtils.translateColors(piece.getLore()));
                }

                if (piece.getCustomModelData() != null) {
                    ItemUtils.setCustomModelData(meta, piece.getCustomModelData());
                }

                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS});
                meta.setUnbreakable(true);
                NBTUtils.setString(meta, "ghastutils.armor_id", armorId);
                NBTUtils.setBoolean(meta, "ghastutils.unenchantable", true);
                NBTUtils.setBoolean(meta, "ghastutils.unplaceable", true);
                if (piece.getMaterial() == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta)meta;
                    NBTUtils.setBoolean(meta, "ghastutils.unplaceable", true);
                    if (!piece.getHeadTexture().isEmpty()) {
                        try {
                            PlayerProfile profile = this.plugin.getServer().createPlayerProfile(UUID.randomUUID(), "CustomHead");
                            PlayerTextures textures = profile.getTextures();
                            URL url = new URL(piece.getHeadTexture());
                            textures.setSkin(url);
                            profile.setTextures(textures);
                            skullMeta.setOwnerProfile(profile);
                        } catch (Exception e) {
                            LogManager.warning("Failed to set custom head texture for " + armorId + ": " + e.getMessage());
                        }
                    } else if (!piece.getHeadOwner().isEmpty()) {
                        String owner = piece.getHeadOwner();
                        if (!owner.equals("{player}")) {
                            String trimmedName = owner.length() > 16 ? owner.substring(0, 16) : owner;
                            skullMeta.setOwner(trimmedName);
                        }
                    }
                }

                if (piece.getColor() != null && meta instanceof LeatherArmorMeta) {
                    ((LeatherArmorMeta)meta).setColor(piece.getColor());
                }

                item.setItemMeta(meta);
                return item;
            }
        }
    }

    public List<ItemStack> createAllArmorItems() {
        List<ItemStack> items = new ArrayList();

        for(String armorId : this.armorPieces.keySet()) {
            ItemStack item = this.createArmorItem(armorId);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    public boolean isArmorPiece(ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && NBTUtils.hasString(meta, "ghastutils.armor_id")) {
                String armorId = NBTUtils.getString(meta, "ghastutils.armor_id");
                return this.armorPieces.containsKey(armorId);
            } else {
                return this.getArmorId(item) != null;
            }
        } else {
            return false;
        }
    }

    public String getArmorId(ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            } else {
                if (NBTUtils.hasString(meta, "ghastutils.armor_id")) {
                    String armorId = NBTUtils.getString(meta, "ghastutils.armor_id");
                    if (this.armorPieces.containsKey(armorId)) {
                        return armorId;
                    }
                }

                for(Map.Entry<String, ArmorPieceData> entry : this.armorPieces.entrySet()) {
                    if (this.isMatchingItem(item, (ArmorPieceData)entry.getValue())) {
                        return (String)entry.getKey();
                    }
                }

                return null;
            }
        } else {
            return null;
        }
    }

    public ArmorPieceData getArmorPieceData(ItemStack item) {
        String armorId = this.getArmorId(item);
        return armorId != null ? (ArmorPieceData)this.armorPieces.get(armorId) : null;
    }

    public double getArmorMultiplier(ItemStack item) {
        ArmorPieceData piece = this.getArmorPieceData(item);
        return piece != null ? piece.multiplier : (double)0.0F;
    }

    public Map<String, ArmorPieceData> getArmorPieces() {
        return this.armorPieces;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    private boolean isMatchingItem(ItemStack item, ArmorPieceData piece) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        } else if (item.getType() != piece.material) {
            return false;
        } else {
            if (piece.name != null && !piece.name.isEmpty()) {
                if (!meta.hasDisplayName()) {
                    return false;
                }

                String itemName = MessageUtils.stripColors(meta.getDisplayName()).toLowerCase();
                String pieceName = MessageUtils.stripColors(piece.name).toLowerCase();
                if (!itemName.equals(pieceName)) {
                    return false;
                }
            }

            if (!piece.lore.isEmpty()) {
                if (!meta.hasLore() || meta.getLore() == null) {
                    return false;
                }

                List<String> itemLore = (List)MessageUtils.stripColors(meta.getLore()).stream().map(String::toLowerCase).collect(Collectors.toList());

                for(String requiredLine : (List)MessageUtils.stripColors(piece.lore).stream().map(String::toLowerCase).collect(Collectors.toList())) {
                    boolean found = itemLore.stream().anyMatch((itemLine) -> itemLine.contains(requiredLine));
                    if (!found) {
                        return false;
                    }
                }
            }

            if (piece.customModelData != null) {
                if (!ItemUtils.hasCustomModelData(meta)) {
                    return false;
                }

                if (ItemUtils.getCustomModelData(meta) != piece.customModelData) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean hasPermission(Player player, ArmorPieceData piece) {
        return piece.permission != null && !piece.permission.isEmpty() ? player.hasPermission(piece.permission) : true;
    }

    public String getParticleType() {
        return this.particleType;
    }

    public int getParticleCount() {
        return this.particleCount;
    }

    public double getParticleRadius() {
        return this.particleRadius;
    }

    public boolean areParticlesEnabled() {
        return this.particlesEnabled;
    }

    public static class ArmorPieceData {
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final double multiplier;
        private final String permission;
        private final Integer customModelData;
        private final Color color;
        private final String headTexture;
        private final String headOwner;

        public ArmorPieceData(Material material, String name, List<String> lore, double multiplier, String permission, Integer customModelData, Color color, String headTexture, String headOwner) {
            this.material = material;
            this.name = name;
            this.lore = (List<String>)(lore != null ? lore : new ArrayList());
            this.multiplier = multiplier;
            this.permission = permission;
            this.customModelData = customModelData;
            this.color = color;
            this.headTexture = headTexture;
            this.headOwner = headOwner;
        }

        public Material getMaterial() {
            return this.material;
        }

        public String getName() {
            return this.name;
        }

        public List<String> getLore() {
            return this.lore;
        }

        public double getMultiplier() {
            return this.multiplier;
        }

        public String getPermission() {
            return this.permission;
        }

        public Integer getCustomModelData() {
            return this.customModelData;
        }

        public Color getColor() {
            return this.color;
        }

        public String getHeadTexture() {
            return this.headTexture;
        }

        public String getHeadOwner() {
            return this.headOwner;
        }
    }
}
