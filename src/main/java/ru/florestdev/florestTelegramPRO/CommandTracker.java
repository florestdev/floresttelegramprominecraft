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

    public CommandTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void SendTelegramFUNCTION(String botToken, String chatId, String message) throws IOException, InterruptedException {
        // Функция для отправки сообщения в тг
        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
        String requestBody = String.format("chat_id=%s&text=%s", chatId, message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            plugin.getLogger().info("Successful sending.");
        }
        else {
            plugin.getLogger().info("Own bad! We can't send message to Telegram APIs.");
        }
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
                        try {
                            SendTelegramFUNCTION(token, chatId, message);
                        } catch (IOException | InterruptedException e) {
                            // ...
                        }
                    }
                } else {
                    if (whitelisted().contains(event.getMessage().split(" ")[0])) {
                        String token = plugin.getConfig().getString("telegram_bot_token");
                        String chatId = plugin.getConfig().getString("telegram_chat_id");
                        String message = plugin.getConfig().getString("human_process_command").replace("{user}", event.getPlayer().getName()).replace("{command}", event.getMessage().split(" ")[0]);
                        try {
                            SendTelegramFUNCTION(token, chatId, message);
                        } catch (IOException | InterruptedException e) {
                            // ...
                        }
                    }
                }
            }
        }
    }

}
