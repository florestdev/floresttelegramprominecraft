package ru.florestdev.florestTelegramPRO;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.net.http.HttpClient;
import java.util.*;
import java.util.regex.Pattern;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.RegisteredServiceProvider;


public class ChatListener implements Listener {

    private final FlorestTelegramPRO main;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");
    public final Methods methods;

    public ChatListener(FlorestTelegramPRO main, Methods methods) {
        this.main = main;
        this.methods = methods;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        String playerName = player.getName();

        String token = main.getConfig().getString("telegram_bot_token");
        String chat_id = main.getConfig().getString("telegram_chat_id");
        String messageFormat = main.getConfig().getString("telegram_message_format", "[MC] {player}: {message}");
        String formattedMessage = messageFormat.replace("{player}", playerName).replace("{message}", message);

        if (main.getConfig().getBoolean("support_prefix")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                LuckPerms api = provider.getProvider();
                User user = api.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    List<Group> list = new ArrayList<>(user.getInheritedGroups(user.getQueryOptions()));
                    if (list.isEmpty()) {
                        formattedMessage = formattedMessage.replace("{prefix}", "");
                    } else {
                        Collection<PrefixNode> groupPrefixes = list.getFirst().getNodes(NodeType.PREFIX);
                        if (!groupPrefixes.isEmpty()) {
                            formattedMessage = formattedMessage.replace("{prefix}", removeMinecraftFormatting(groupPrefixes.stream().toList().getFirst().getMetaValue()));
                        } else {
                            formattedMessage = formattedMessage.replace("{prefix}", "");
                        }
                    }
                }
            }
        }

        // 🔥 ПАРСИМ ПЛЕЙСХОЛДЕРЫ PLACEHOLDERAPI
        if (main.placeholderUtil != null) {
            formattedMessage = main.placeholderUtil.parsePlaceholders(player, formattedMessage);
        }

        if (!main.getConfig().getBoolean("enable_restrictions_for_messages")) {
            methods.sendTelegramMessage(token, chat_id, formattedMessage);
        } else {
            String prefix = main.getConfig().getString("prefix_for_minecraft");
            if (prefix != null && !message.startsWith(prefix)) {
                methods.sendTelegramMessage(token, chat_id, formattedMessage);
            }
        }
    }

    public static String removeMinecraftFormatting(String text) {
        if (text == null) return "";
        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");
        return text;
    }
}