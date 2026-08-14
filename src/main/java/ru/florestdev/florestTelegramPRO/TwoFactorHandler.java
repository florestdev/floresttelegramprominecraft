package ru.florestdev.florestTelegramPRO;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwoFactorHandler implements Listener {

    private final FlorestTelegramPRO plugin;
    private final Map<UUID, Location> frozenLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();

    public TwoFactorHandler(FlorestTelegramPRO plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // 🧊 ЗАМОРОЗИТЬ ИГРОКА
    public void freezePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        frozenLocations.put(uuid, player.getLocation().clone());
        frozenPlayers.put(uuid, true);

        // Эффект слепоты и замедления
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        // Отключаем полёт
        player.setAllowFlight(false);
        player.setFlying(false);

        // Отправляем сообщение
        player.sendMessage(ChatColor.RED + plugin.getConfig().getString("2fa_message_enter").replace("{username}", player.getName()));

        // Запускаем задачу, которая будет телепортировать игрока обратно, если он попытается уйти
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!frozenPlayers.containsKey(uuid)) {
                    this.cancel();
                    return;
                }

                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    this.cancel();
                    return;
                }

                Location freezeLoc = frozenLocations.get(uuid);
                if (freezeLoc == null) return;

                // Проверяем, не отошёл ли игрок от точки заморозки
                if (p.getLocation().distanceSquared(freezeLoc) > 0.25) {
                    // Универсальная телепортация (работает везде)
                    player.teleport(freezeLoc);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // 🔓 РАЗМОРОЗИТЬ ИГРОКА
    public void unfreezePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        frozenLocations.remove(uuid);
        frozenPlayers.remove(uuid);

        // Убираем эффекты
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        player.sendMessage("§a✅ 2FA подтверждена! Ты разморожен. Добро пожаловать!");
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.containsKey(player.getUniqueId());
    }

    // 🚫 БЛОКИРОВКА ДВИЖЕНИЯ (если игрок попытается двигаться — возвращаем назад)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isFrozen(event.getPlayer())) return;
        Location freezeLoc = frozenLocations.get(event.getPlayer().getUniqueId());
        if (freezeLoc != null) {
            event.setTo(freezeLoc);
        } else {
            event.setCancelled(true);
        }
    }

    // 🚫 БЛОКИРОВКА ПОВОРОТОВ
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // 🚫 БЛОКИРОВКА КЛИКОВ ПО ИНВЕНТАРЮ
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 🚫 БЛОКИРОВКА РАЗРУШЕНИЯ / СТАВКИ БЛОКОВ
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // 🚫 БЛОКИРОВКА УРОНА (чтобы не убили во время 2FA)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 🚫 БЛОКИРОВКА ГОЛОДА
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 🚫 БЛОКИРОВКА ЧАТА (кроме команды /ftp code)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + plugin.getConfig().getString("2fa_not_passed"));
        }
    }

    // 🚫 БЛОКИРОВКА КОМАНД (кроме /ftp code и /login)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!isFrozen(event.getPlayer())) return;

        String command = event.getMessage().toLowerCase();
        // Разрешённые команды
        if (command.startsWith("/2fa code") ||
                command.startsWith("/l") ||
                command.startsWith("/login")) {
            return; // Пропускаем
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + plugin.getConfig().getString("2fa_not_passed"));
    }
}