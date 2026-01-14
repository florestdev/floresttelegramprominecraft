package ru.florestdev.florestTelegramPRO;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.checkerframework.checker.fenum.qual.SwingTitleJustification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class PlayerTracker implements Listener {

    private final Plugin plugin;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    public final Methods methods;

    public PlayerTracker(Plugin plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    // Метод для регистрации этого Listener'а в плагине
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Обработчик события входа игрока
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = plugin.getConfig().getString("human_joined").replace("{user}", event.getPlayer().getName());
        String token = plugin.getConfig().getString("telegram_bot_token");
        String chat_id = plugin.getConfig().getString("telegram_chat_id");

        if (plugin.getConfig().getBoolean("desc_editing_bool")) {
            Date currentDate = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = formatter.format(currentDate);
            methods.setChatDescription(token, chat_id, plugin.getConfig().getString("on_online_desc").replace("{players_online}", String.valueOf(plugin.getServer().getOnlinePlayers().size())).replace("{players_max}", String.valueOf(plugin.getServer().getMaxPlayers())).replace("{time}", formattedDate));
        }

        if (plugin.getConfig().getBoolean("support_prefix")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            LuckPerms api = provider.getProvider();
            User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());
            assert user != null;
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

        methods.sendTelegramMessage(token, chat_id, message);
    }

    // Обработчик события выхода игрока
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = plugin.getConfig().getString("human_quited").replace("{user}", event.getPlayer().getName());
        String token = plugin.getConfig().getString("telegram_bot_token");
        String chat_id = plugin.getConfig().getString("telegram_chat_id");

        if (plugin.getConfig().getBoolean("desc_editing_bool")) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Date currentDate = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = formatter.format(currentDate);
            methods.setChatDescription(token, chat_id, plugin.getConfig().getString("on_online_desc").replace("{players_online}", String.valueOf(plugin.getServer().getOnlinePlayers().size() - 1)).replace("{players_max}", String.valueOf(plugin.getServer().getMaxPlayers())).replace("{time}", formattedDate));
        }

        if (plugin.getConfig().getBoolean("support_prefix")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            LuckPerms api = provider.getProvider();
            User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());
            assert user != null;
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

        methods.sendTelegramMessage(token, chat_id, message);
    }

    public static String removeMinecraftFormatting(String text) {
        if (text == null) return "";

        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");

        return text;
    }


}