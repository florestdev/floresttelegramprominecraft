package ru.florestdev.florestTelegramPRO;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.net.http.HttpClient;
import java.util.List;

public class CommandTracker implements Listener {

    private final FlorestTelegramPRO main;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    public final Methods methods;

    public CommandTracker(FlorestTelegramPRO main, Methods methods) {
        this.main = main;
        this.methods = methods;
    }

    public void register() {
        main.getServer().getPluginManager().registerEvents(this, main);
        main.getLogger().info("CommandTracker registered!");
    }

    public List<String> banned() {
        return main.getConfig().getStringList("blacklist_commands");
    }

    public List<String> whitelisted() {
        return main.getConfig().getStringList("whitelist_commands");
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled()) {
            String command = event.getMessage();
            if (command.startsWith("/")) {
                Player player = event.getPlayer();
                String commandName = command.split(" ")[0];

                boolean shouldSend = false;

                if (whitelisted().contains("all")) {
                    if (!banned().contains(command) && !banned().contains(commandName)) {
                        shouldSend = true;
                    }
                } else {
                    if (whitelisted().contains(commandName)) {
                        shouldSend = true;
                    }
                }

                if (shouldSend) {
                    String token = main.getConfig().getString("telegram_bot_token");
                    String chatId = main.getConfig().getString("telegram_chat_id");
                    String message = main.getConfig().getString("human_process_command")
                            .replace("{user}", player.getName())
                            .replace("{command}", commandName);

                    // 🔥 ПАРСИМ ПЛЕЙСХОЛДЕРЫ PLACEHOLDERAPI
                    if (main.placeholderUtil != null) {
                        message = main.placeholderUtil.parsePlaceholders(player, message);
                    }

                    methods.sendTelegramMessage(token, chatId, message);
                }
            }
        }
    }
}