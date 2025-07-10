
package com.ninja.ghastutils.armor;

import com.ninja.ghastutils.GhastUtils;
import com.ninja.ghastutils.utils.MessageUtils;
import com.ninja.ghastutils.utils.NBTUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public class ArmorListener implements Listener {
    private final GhastUtils plugin;
    private final ArmorManager armorManager;
    private final Map<UUID, Map<String, String>> playerArmorPieces;
    private final Map<UUID, Double> playerMultipliers;
    private final Map<UUID, BukkitTask> particleTasks;
    private final Set<UUID> processingPlayers;

    public ArmorListener(GhastUtils plugin, ArmorManager armorManager) {
        this.plugin = plugin;
        this.armorManager = armorManager;
        this.playerArmorPieces = new ConcurrentHashMap();
        this.playerMultipliers = new ConcurrentHashMap();
        this.particleTasks = new ConcurrentHashMap();
        this.processingPlayers = Collections.synchronizedSet(new HashSet());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
                this.checkPlayerArmor(player);
            }

        }, 20L);
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        this.cleanup(uuid);
    }

    private void cleanup(UUID uuid) {
        this.playerArmorPieces.remove(uuid);
        this.playerMultipliers.remove(uuid);
        BukkitTask task = (BukkitTask)this.particleTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        this.processingPlayers.remove(uuid);
    }

    private boolean isArmorDamaged(ItemStack item) {
        if (item != null && item.getItemMeta() instanceof Damageable) {
            Damageable meta = (Damageable)item.getItemMeta();
            return meta.getDamage() > 0;
        } else {
            return false;
        }
    }

    private void checkArmorDurability(Player player, ItemStack item, String slot) {
        if (this.isArmorDamaged(item)) {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("piece", slot);
            MessageUtils.sendMessage(player, "armor.damaged", placeholders);
        }

    }

    private void startParticleEffect(Player player) {
        if (this.armorManager.areParticlesEnabled()) {
            UUID uuid = player.getUniqueId();
            BukkitTask existingTask = (BukkitTask)this.particleTasks.get(uuid);
            if (existingTask != null) {
                existingTask.cancel();
            }

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                if (!player.isOnline()) {
                    this.cleanup(uuid);
                } else {
                    try {
                        Location location = player.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
                        player.getWorld().spawnParticle(Particle.valueOf(this.armorManager.getParticleType()), location, this.armorManager.getParticleCount(), this.armorManager.getParticleRadius(), this.armorManager.getParticleRadius(), this.armorManager.getParticleRadius(), 0.05);
                    } catch (Exception e) {
                        Logger var10000 = this.plugin.getLogger();
                        String var10001 = player.getName();
                        var10000.warning("Error spawning particles for " + var10001 + ": " + e.getMessage());
                    }

                }
            }, 20L, 20L);
            this.particleTasks.put(uuid, task);
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.checkPlayerArmor(event.getPlayer()), 1L);
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onEnchantItem(EnchantItemEvent event) {
        if (!event.getEnchanter().hasPermission("ghastutils.bypass.enchant")) {
            if (this.armorManager.isArmorPiece(event.getItem())) {
                event.setCancelled(true);
                event.getEnchanter().sendMessage(MessageUtils.translateColors("&cThis armor cannot be enchanted!"));
            }

        }
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getView().getPlayer() instanceof Player) {
            Player player = (Player)event.getView().getPlayer();
            if (player.hasPermission("ghastutils.bypass.enchant")) {
                return;
            }
        }

        ItemStack firstItem = event.getInventory().getFirstItem();
        ItemStack secondItem = event.getInventory().getSecondItem();
        if (firstItem != null && this.armorManager.isArmorPiece(firstItem) || secondItem != null && this.armorManager.isArmorPiece(secondItem)) {
            event.setResult((ItemStack)null);
            if (event.getView().getPlayer() instanceof Player) {
                ((Player)event.getView().getPlayer()).sendMessage(MessageUtils.translateColors("&cCustom armor cannot be modified in anvils!"));
            }
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            if (this.armorManager.isEnabled()) {
                Player player = (Player)event.getWhoClicked();
                boolean isArmorRelated = false;
                if (event.getSlotType() == SlotType.ARMOR) {
                    isArmorRelated = true;
                }

                if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory()) && event.getSlot() >= 36 && event.getSlot() <= 39) {
                    isArmorRelated = true;
                }

                if (event.isShiftClick() && event.getCurrentItem() != null && this.isArmorMaterial(event.getCurrentItem().getType())) {
                    isArmorRelated = true;
                }

                if (event.getCursor() != null && this.isArmorMaterial(event.getCursor().getType()) && event.getSlotType() == SlotType.ARMOR) {
                    isArmorRelated = true;
                }

                if (isArmorRelated) {
                    this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.checkPlayerArmor(player), 1L);
                }

            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (this.armorManager.isEnabled()) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && this.isArmorMaterial(item.getType()) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.useItemInHand() != Result.DENY)) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                    if (!item.equals(player.getInventory().getItemInMainHand()) && !item.equals(player.getInventory().getItemInOffHand())) {
                        this.checkPlayerArmor(player);
                    }

                }, 3L);
            }

        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (this.armorManager.isEnabled()) {
            Player player = event.getPlayer();
            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (newItem != null && this.isArmorMaterial(newItem.getType())) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.checkPlayerArmor(player), 2L);
            }

        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (this.armorManager.isEnabled()) {
            Player player = event.getPlayer();
            if (this.isArmorMaterial(event.getOffHandItem().getType()) || this.isArmorMaterial(event.getMainHandItem().getType())) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.checkPlayerArmor(player), 1L);
            }

        }
    }

    private boolean isArmorMaterial(Material material) {
        if (material == null) {
            return false;
        } else {
            try {
                EquipmentSlot slot = material.getEquipmentSlot();
                return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
            } catch (Throwable var4) {
                String name = material.name();
                return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("PLAYER_HEAD") || name.equals("SKULL_ITEM");
            }
        }
    }

    public void checkPlayerArmor(Player player) {
        if (!this.armorManager.isEnabled()) {
            this.playerArmorPieces.remove(player.getUniqueId());
            this.playerMultipliers.remove(player.getUniqueId());
        } else {
            UUID uuid = player.getUniqueId();
            if (!this.processingPlayers.contains(uuid)) {
                this.processingPlayers.add(uuid);

                try {
                    Map<String, String> previousArmorPieces = (Map)this.playerArmorPieces.getOrDefault(uuid, new HashMap());
                    Map<String, String> currentArmorPieces = new HashMap();
                    double previousMultiplier = (Double)this.playerMultipliers.getOrDefault(uuid, (double)1.0F);
                    double currentMultiplier = (double)1.0F;
                    ItemStack helmet = player.getInventory().getHelmet();
                    ItemStack chestplate = player.getInventory().getChestplate();
                    ItemStack leggings = player.getInventory().getLeggings();
                    ItemStack boots = player.getInventory().getBoots();
                    this.checkArmorPiece(player, "helmet", helmet, previousArmorPieces, currentArmorPieces);
                    this.checkArmorPiece(player, "chestplate", chestplate, previousArmorPieces, currentArmorPieces);
                    this.checkArmorPiece(player, "leggings", leggings, previousArmorPieces, currentArmorPieces);
                    this.checkArmorPiece(player, "boots", boots, previousArmorPieces, currentArmorPieces);

                    for(String armorId : currentArmorPieces.values()) {
                        if (armorId != null) {
                            ArmorManager.ArmorPieceData piece = (ArmorManager.ArmorPieceData)this.armorManager.getArmorPieces().get(armorId);
                            if (piece != null) {
                                currentMultiplier += piece.getMultiplier();
                            }
                        }
                    }

                    this.playerArmorPieces.put(uuid, currentArmorPieces);
                    this.playerMultipliers.put(uuid, currentMultiplier);
                    if (Math.abs(currentMultiplier - previousMultiplier) > 0.001) {
                        if (this.armorManager.areParticlesEnabled()) {
                            try {
                                Location location = player.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
                                Particle particle = Particle.valueOf(this.armorManager.getParticleType());
                                player.getWorld().spawnParticle(particle, location, this.armorManager.getParticleCount(), this.armorManager.getParticleRadius(), this.armorManager.getParticleRadius(), this.armorManager.getParticleRadius(), 0.05);
                                if (currentMultiplier > (double)1.0F) {
                                    this.startParticleEffect(player);
                                } else {
                                    BukkitTask task = (BukkitTask)this.particleTasks.remove(uuid);
                                    if (task != null) {
                                        task.cancel();
                                    }
                                }
                            } catch (IllegalArgumentException var19) {
                                this.plugin.getLogger().warning("Invalid particle type: " + this.armorManager.getParticleType());
                            }
                        }

                        Map<String, String> placeholders = MessageUtils.placeholders();
                        placeholders.put("multiplier", String.format("%.2f", currentMultiplier));
                    }
                } finally {
                    this.processingPlayers.remove(uuid);
                }

            }
        }
    }

    private void checkArmorPiece(Player player, String slot, ItemStack item, Map<String, String> previousArmor, Map<String, String> currentArmor) {
        String previousArmorId = (String)previousArmor.get(slot);
        String currentArmorId = null;
        if (item != null && !item.getType().equals(Material.AIR)) {
            if (item.hasItemMeta() && item.getItemMeta() != null) {
                ItemMeta meta = item.getItemMeta();
                String nbtArmorId = NBTUtils.getString(meta, "ghastutils.armor_id");
                if (nbtArmorId != null) {
                    currentArmorId = nbtArmorId;
                }
            }

            if (currentArmorId == null) {
                currentArmorId = this.armorManager.getArmorId(item);
            }

            if (currentArmorId != null) {
                ArmorManager.ArmorPieceData piece = (ArmorManager.ArmorPieceData)this.armorManager.getArmorPieces().get(currentArmorId);
                if (piece != null) {
                    if (!this.armorManager.hasPermission(player, piece)) {
                        MessageUtils.sendMessage(player, "armor.permission-required");
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(new ItemStack[]{item});
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }

                        switch (slot) {
                            case "helmet" -> player.getInventory().setHelmet((ItemStack)null);
                            case "chestplate" -> player.getInventory().setChestplate((ItemStack)null);
                            case "leggings" -> player.getInventory().setLeggings((ItemStack)null);
                            case "boots" -> player.getInventory().setBoots((ItemStack)null);
                        }

                        currentArmorId = null;
                    }
                } else {
                    currentArmorId = null;
                }
            }
        }

        currentArmor.put(slot, currentArmorId);
        if (previousArmorId == null && currentArmorId != null || previousArmorId != null && !previousArmorId.equals(currentArmorId)) {
            if (previousArmorId != null) {
                ArmorManager.ArmorPieceData oldPiece = (ArmorManager.ArmorPieceData)this.armorManager.getArmorPieces().get(previousArmorId);
                if (oldPiece != null) {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("name", MessageUtils.stripColors(oldPiece.getName()));
                    placeholders.put("multiplier", String.format("%.0f", oldPiece.getMultiplier() * (double)100.0F));
                    MessageUtils.sendMessage(player, "armor.unequipped", placeholders);
                }
            }

            if (currentArmorId != null) {
                ArmorManager.ArmorPieceData newPiece = (ArmorManager.ArmorPieceData)this.armorManager.getArmorPieces().get(currentArmorId);
                if (newPiece != null) {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("name", MessageUtils.stripColors(newPiece.getName()));
                    placeholders.put("multiplier", String.format("%.0f", newPiece.getMultiplier() * (double)100.0F));
                    MessageUtils.sendMessage(player, "armor.equipped", placeholders);
                    this.checkArmorDurability(player, item, slot);
                }
            }
        }

    }

    public double getArmorMultiplier(Player player) {
        if (!this.armorManager.isEnabled()) {
            return (double)1.0F;
        } else {
            UUID uuid = player.getUniqueId();
            if (!this.playerMultipliers.containsKey(uuid)) {
                this.checkPlayerArmor(player);
            }

            return (Double)this.playerMultipliers.getOrDefault(uuid, (double)1.0F);
        }
    }
}
