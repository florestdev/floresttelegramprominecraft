package ru.florestdev.florestTelegramPRO;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.net.http.HttpClient;
import java.util.regex.Pattern;


public class AchievementManager implements Listener {

    private final Plugin plugin;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");
    public final Methods methods;


    public AchievementManager(Plugin plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        String advancementTitle = Optional.ofNullable(event.getAdvancement().getDisplay())
                .map(AdvancementDisplay::getTitle)
                .orElse("Unknown Advancement");
        String bot_token = plugin.getConfig().getString("telegram_bot_token");
        String chatId = plugin.getConfig().getString("telegram_chat_id");
        String message = plugin.getConfig().getString("format_advancements").replace("{user}", player.getName()).replace("{advancement}", advancementTitle);

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

        try {
            methods.SendTelegramFUNCTION(bot_token, chatId, message);
        } catch (IOException | InterruptedException e) {
            // ...
        }
    }

    public static String removeMinecraftFormatting(String text) {
        if (text == null) return "";

        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");

        return text;
    }

}