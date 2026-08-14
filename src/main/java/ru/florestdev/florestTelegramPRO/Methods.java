package ru.florestdev.florestTelegramPRO;

import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.json.JSONObject;

public class Methods {

    public final Plugin plugin;
    public final HttpClient client = HttpClient.newHttpClient();

    public Methods(Plugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> banTelegramUser(String botToken, String chatId, String userId) {
        String url = String.format("https://api.telegram.org/bot%s/banChatMember", botToken);
        String requestBody = String.format("chat_id=%s&user_id=%s", chatId, userId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("User " + userId + " banned successfully in chat " + chatId);
                    } else {
                        plugin.getLogger().warning("Failed to ban user " + userId + " in chat " + chatId +
                                ". Status code: " + response.statusCode() + ", Body: " + response.body());
                    }
                });
    }

    public CompletableFuture<Void> unbanTelegramUser(String botToken, String chatId, String userId) {
        String url = String.format("https://api.telegram.org/bot%s/unbanChatMember", botToken);
        String requestBody = String.format("chat_id=%s&user_id=%s", chatId, userId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("User " + userId + " unbanned successfully in chat " + chatId);
                    } else {
                        plugin.getLogger().warning("Failed to unban user " + userId + " in chat " + chatId +
                                ". Status code: " + response.statusCode() + ", Body: " + response.body());
                    }
                });
    }

    // Set chat description
    public CompletableFuture<Void> setChatDescription(String botToken, String chatId, String text) {
        String url = String.format("https://api.telegram.org/bot%s/setChatDescription", botToken);
        String requestBody = String.format("chat_id=%s&description=%s", chatId, URLEncoder.encode(text, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("Successful editing.");
                    } else {
                        plugin.getLogger().info("Own bad! Not successful editing.");
                    }
                });
    }

    public CompletableFuture<Void> sendTelegramMessageToUser(String botToken, String userId, String message) {
        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

        // Кодируем параметры
        String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

        String requestBody = String.format("chat_id=%s&text=%s", encodedUserId, encodedMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("Message sent successfully to user " + userId);
                    } else {
                        // Парсим ошибку от Telegram
                        try {
                            JSONObject json = new JSONObject(response.body());
                            String description = json.optString("description", "Unknown error");
                            plugin.getLogger().severe("Failed to send message to user " + userId +
                                    ": " + description);

                            // Проверяем конкретные ошибки
                            if (description.contains("bot was blocked by the user")) {
                                plugin.getLogger().warning("User " + userId + " blocked the bot!");
                            } else if (description.contains("user not found")) {
                                plugin.getLogger().warning("User " + userId + " not found!");
                            } else if (description.contains("bot can't initiate conversation")) {
                                plugin.getLogger().warning("User " + userId + " hasn't started the bot!");
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to send message. Status: " + response.statusCode() +
                                    ", Response: " + response.body());
                        }
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error sending message to user " + userId + ": " +
                            throwable.getMessage());
                    return null;
                });
    }

    // Отправка сообщения
    public CompletableFuture<Void> sendTelegramMessage(String botToken, String chatId, String message) {
        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("chat_id=").append(chatId);
        requestBody.append("&text=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));

        if (plugin.getConfig().getBoolean("support_themes")) {
            int themeId = plugin.getConfig().getInt("follow_theme", 0);
            if (themeId > 0) {
                requestBody.append("&message_thread_id=").append(themeId);
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("Successful sending.");
                    } else {
                        plugin.getLogger().info("Own bad! We can't send message to Telegram APIs.");
                    }
                });
    }

    // Универсальный helper для асинхронного API запроса
    public CompletableFuture<String> makeApiRequest(String botToken, String method, String requestBody) {
        String url = String.format("https://api.telegram.org/bot%s/%s", botToken, method);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        plugin.getLogger().warning("API Request failed: " + method + " Status code: " + response.statusCode() + ", Body: " + response.body());
                        throw new RuntimeException("API request failed with status code: " + response.statusCode());
                    }
                });
    }

    // Revoke user rights
    public CompletableFuture<Void> revokeUserRights(String botToken, String chatId, String userId) {
        String requestBody = String.format(
                "chat_id=%s&user_id=%s&can_send_messages=false&can_send_media_messages=false&" +
                        "can_send_polls=false&can_send_other_messages=false&can_add_web_page_previews=false&",
                chatId, userId);

        return makeApiRequest(botToken, "restrictChatMember", requestBody)
                .thenAccept(result -> plugin.getLogger().info("Rights revoked for user " + userId + " in chat " + chatId + ". Result: " + result))
                .exceptionally(e -> { plugin.getLogger().severe("Exception while revoking rights: " + e.getMessage()); return null; });
    }

    public CompletableFuture<Void> restoreUserRights(String botToken, String chatId, String userId) {
        String requestBody = String.format(
                "chat_id=%s&user_id=%s&can_send_messages=true&can_send_media_messages=true&" +
                        "can_send_polls=true&can_send_other_messages=true&can_add_web_page_previews=true&",
                chatId, userId);

        return makeApiRequest(botToken, "restrictChatMember", requestBody)
                .thenAccept(result -> plugin.getLogger().info("Rights restored for user " + userId + " in chat " + chatId + ". Result: " + result))
                .exceptionally(e -> { plugin.getLogger().severe("Exception while restoring rights: " + e.getMessage()); return null; });
    }
    public CompletableFuture<Void> restrictChat(String botToken, String chatId) {
        String url = String.format("https://api.telegram.org/bot%s/setChatPermissions", botToken);
        String requestBody = String.format("chat_id=%s&can_send_messages=false", chatId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("Chat " + chatId + " restricted successfully.");
                    } else {
                        plugin.getLogger().warning("Failed to restrict chat " + chatId + ". Status code: "
                                + response.statusCode() + ", Body: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    plugin.getLogger().severe("Exception while restricting chat: " + e.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> unrestrictChat(String botToken, String chatId) {
        String url = String.format("https://api.telegram.org/bot%s/setChatPermissions", botToken);
        String requestBody = String.format("chat_id=%s&can_send_messages=true", chatId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        plugin.getLogger().info("Chat " + chatId + " unrestricted successfully.");
                    } else {
                        plugin.getLogger().warning("Failed to unrestrict chat " + chatId + ". Status code: "
                                + response.statusCode() + ", Body: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    plugin.getLogger().severe("Exception while unrestricting chat: " + e.getMessage());
                    return null;
                });
    }

}