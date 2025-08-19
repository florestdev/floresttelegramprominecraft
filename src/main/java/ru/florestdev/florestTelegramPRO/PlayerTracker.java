package ru.florestdev.florestTelegramPRO;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlayerTracker implements Listener {

    private final Plugin plugin;
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public PlayerTracker(Plugin plugin) {
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
        try {
            SendTelegramFUNCTION(token, chat_id, message);
        } catch (IOException | InterruptedException e) {
            // ...
        }
    }

    // Обработчик события выхода игрока
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = plugin.getConfig().getString("human_quited").replace("{user}", event.getPlayer().getName());
        String token = plugin.getConfig().getString("telegram_bot_token");
        String chat_id = plugin.getConfig().getString("telegram_chat_id");
        try {
            SendTelegramFUNCTION(token, chat_id, message);
        } catch (IOException | InterruptedException e) {
            // ...
        }
    }

}