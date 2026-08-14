package ru.florestdev.florestTelegramPRO;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;

import java.net.http.HttpClient;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class PlayerTracker implements Listener {

    private final FlorestTelegramPRO main;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    public final Methods methods;

    public PlayerTracker(FlorestTelegramPRO main, Methods methods) {
        this.main = main;
        this.methods = methods;
    }

    public void register() {
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String message = main.getConfig().getString("human_joined").replace("{user}", player.getName());
        String token = main.getConfig().getString("telegram_bot_token");
        String chat_id = main.getConfig().getString("telegram_chat_id");

        if (main.getConfig().getBoolean("desc_editing_bool")) {
            Date currentDate = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = formatter.format(currentDate);
            methods.setChatDescription(token, chat_id, main.getConfig().getString("on_online_desc")
                    .replace("{players_online}", String.valueOf(main.getServer().getOnlinePlayers().size()))
                    .replace("{players_max}", String.valueOf(main.getServer().getMaxPlayers()))
                    .replace("{time}", formattedDate));
        }

        if (main.getConfig().getBoolean("support_prefix")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                LuckPerms api = provider.getProvider();
                User user = api.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    List<Group> list = new ArrayList<>(user.getInheritedGroups(user.getQueryOptions()));
                    if (list.isEmpty()) {
                        message = message.replace("{prefix}", "");
                    } else {
                        Collection<PrefixNode> groupPrefixes = list.getFirst().getNodes(NodeType.PREFIX);
                        if (!groupPrefixes.isEmpty()) {
                            message = message.replace("{prefix}", removeMinecraftFormatting(groupPrefixes.stream().toList().getFirst().getMetaValue()));
                        } else {
                            message = message.replace("{prefix}", "");
                        }
                    }
                }
            }
        }

        // 🔥 ПАРСИМ ПЛЕЙСХОЛДЕРЫ PLACEHOLDERAPI
        if (main.placeholderUtil != null) {
            message = main.placeholderUtil.parsePlaceholders(player, message);
        }

        methods.sendTelegramMessage(token, chat_id, message);

        TwoFactorDatabase.TwoFactorData data = main.getTwoFactorDatabase().getByUsername(player.getName());

        if (data != null && data.enabled) {
            if (data.isBlocked(3, 600000)) {
                player.kickPlayer("§cСлишком много попыток. Подождите 10 минут.");
                return;
            }

            String code = String.format("%06d", new Random().nextInt(1000000));
            long expires = System.currentTimeMillis() + 120000;
            main.getTwoFactorDatabase().updateCode(player.getName(), code, expires);

            methods.sendTelegramMessageToUser(token, data.telegramId, main.getConfig().getString("2fa_message").replace("{username}", player.getName()).replace("{ip}", player.getAddress().getAddress().getHostAddress()).replace("{code}", code));
            main.getTwoFactorHandler().freezePlayer(player);
        } else {
            return;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String message = main.getConfig().getString("human_quited").replace("{user}", player.getName());
        String token = main.getConfig().getString("telegram_bot_token");
        String chat_id = main.getConfig().getString("telegram_chat_id");

        main.getTwoFactorHandler().unfreezePlayer(event.getPlayer());

        if (main.getConfig().getBoolean("desc_editing_bool")) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
                Date currentDate = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String formattedDate = formatter.format(currentDate);
                methods.setChatDescription(token, chat_id, main.getConfig().getString("on_online_desc")
                        .replace("{players_online}", String.valueOf(main.getServer().getOnlinePlayers().size() - 1))
                        .replace("{players_max}", String.valueOf(main.getServer().getMaxPlayers()))
                        .replace("{time}", formattedDate));
            }, 60L);
        }

        if (main.getConfig().getBoolean("support_prefix")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                LuckPerms api = provider.getProvider();
                User user = api.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    List<Group> list = new ArrayList<>(user.getInheritedGroups(user.getQueryOptions()));
                    if (list.isEmpty()) {
                        message = message.replace("{prefix}", "");
                    } else {
                        Collection<PrefixNode> groupPrefixes = list.getFirst().getNodes(NodeType.PREFIX);
                        if (!groupPrefixes.isEmpty()) {
                            message = message.replace("{prefix}", removeMinecraftFormatting(groupPrefixes.stream().toList().getFirst().getMetaValue()));
                        } else {
                            message = message.replace("{prefix}", "");
                        }
                    }
                }
            }
        }

        // 🔥 ПАРСИМ ПЛЕЙСХОЛДЕРЫ PLACEHOLDERAPI
        if (main.placeholderUtil != null) {
            message = main.placeholderUtil.parsePlaceholders(player, message);
        }

        methods.sendTelegramMessage(token, chat_id, message);
    }

    public static String removeMinecraftFormatting(String text) {
        if (text == null) return "";
        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");
        return text;
    }
}