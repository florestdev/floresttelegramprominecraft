package ru.florestdev.florestTelegramPRO;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class CommandTracker implements Listener {

    private final Plugin plugin;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    public final Methods methods;

    public CommandTracker(Plugin plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("CommandTracker registered!");
    }

    public List<String> banned() {
        return plugin.getConfig().getStringList("blacklist_commands");
    }

    public List<String> whitelisted() {
        return plugin.getConfig().getStringList("whitelist_commands");
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled()) {  // Проверка на отмену события (чтобы не обрабатывать отмененные команды)
            String command = event.getMessage();
            if (command.startsWith("/")) {  // Проверка, является ли введенный текст командой (начинается с "/")
                if (whitelisted().contains("all")) {
                    if (!banned().contains(command) && !banned().contains(command.split(" ")[0])) {
                        String token = plugin.getConfig().getString("telegram_bot_token");
                        String chatId = plugin.getConfig().getString("telegram_chat_id");
                        String message = plugin.getConfig().getString("human_process_command").replace("{user}", event.getPlayer().getName()).replace("{command}", event.getMessage().split(" ")[0]);
                        methods.sendTelegramMessage(token, chatId, message);
                    }
                } else {
                    if (whitelisted().contains(event.getMessage().split(" ")[0])) {
                        String token = plugin.getConfig().getString("telegram_bot_token");
                        String chatId = plugin.getConfig().getString("telegram_chat_id");
                        String message = plugin.getConfig().getString("human_process_command").replace("{user}", event.getPlayer().getName()).replace("{command}", event.getMessage().split(" ")[0]);
                        methods.sendTelegramMessage(token, chatId, message);
                    }
                }
            }
        }
    }

}
