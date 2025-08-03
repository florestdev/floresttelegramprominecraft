package ru.florestdev.florestTelegramPRO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class TelegramReciever {

    private final JavaPlugin plugin;
    private final String botToken;
    private int lastUpdateId = 0;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TelegramReciever(JavaPlugin plugin, String botToken) {
        this.plugin = plugin;
        this.botToken = botToken;
    }

    public List<String> getNewMessages() {
        List<String> messages = new ArrayList<>();
        String url = String.format("https://api.telegram.org/bot%s/getUpdates?offset=%d", botToken, lastUpdateId + 1);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                try {
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                    if (jsonResponse != null && jsonResponse.get("ok").getAsBoolean()) {
                        JsonArray updates = jsonResponse.getAsJsonArray("result");

                        if (updates != null) {
                            for (JsonElement updateElement : updates) {
                                JsonObject update = updateElement.getAsJsonObject();
                                if (update != null) {
                                    int updateId = update.get("update_id").getAsInt();
                                    if (updateId > lastUpdateId) {
                                        lastUpdateId = updateId;
                                    }
                                    if (update.has("message") && update.getAsJsonObject("message").has("text")) {
                                        JsonObject message = update.getAsJsonObject("message");
                                        if (message != null) {
                                            String text = message.get("text").getAsString();
                                            String from = "Telegram";
                                            if (message.has("from")) {
                                                JsonObject fromObj = message.getAsJsonObject("from");
                                                if (fromObj != null && fromObj.has("first_name")) {
                                                    from = fromObj.get("first_name").getAsString();
                                                }
                                            }
                                            // Store the telegram name and message
                                            String telegramName = from;
                                            // Format the telegram message using the configuration
                                            String minecraftTelegramFormat = plugin.getConfig().getString("minecraft_telegram_format", "[TG] {telegram_name}: {telegram_message}");
                                            String formattedMessage = minecraftTelegramFormat.replace("{telegram_name}", telegramName).replace("{telegram_message}", text);

                                            messages.add(formattedMessage);

                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (JsonParseException e) {
                    plugin.getLogger().warning("Error parsing Telegram response: " + e.getMessage());
                    plugin.getLogger().warning("Response body: " + responseBody);
                }
            } else {
                plugin.getLogger().warning("Failed to get updates from Telegram: " + response.statusCode());
                plugin.getLogger().warning("Response body: " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            plugin.getLogger().severe("Error fetching Telegram messages: " + e.getMessage());
        }
        return messages;
    }

    public void processMessages() {
        List<String> newMessages = getNewMessages();

        if (!newMessages.isEmpty()) {
            for (String message : newMessages) {
                // Важно: используем экземпляр плагина для доступа к планировщику
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message); // Send the formatted message directly
                    }
                });
                plugin.getLogger().info("Telegram: " + message);
            }
        }
    }
}