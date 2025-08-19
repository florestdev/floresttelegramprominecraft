package ru.florestdev.florestTelegramPRO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class TelegramReciever {

    private final JavaPlugin plugin;
    private final String botToken;
    private int lastUpdateId = 0;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TelegramReciever(JavaPlugin plugin, String botToken) {
        this.plugin = plugin;
        this.botToken = botToken;
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

    public boolean isUserAdmin(String botToken, String chatId, String userId) {
        try {
            String url = String.format("https://api.telegram.org/bot%s/getChatMember", botToken);
            String requestBody = String.format("chat_id=%s&user_id=%s", chatId, userId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "FlorestPlugin")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() == 200) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                boolean ok = jsonObject.get("ok").getAsBoolean();

                if (ok) {
                    JsonObject result = jsonObject.getAsJsonObject("result");
                    if (result != null) {
                        String status = result.get("status").getAsString();
                        if (status != null) {
                            boolean isAdmin = status.equals("administrator") || status.equals("creator");
                            if (isAdmin && status.equals("administrator")) { // Проверяем is_anonymous только для администраторов (не для creator)
                                if (result.has("is_anonymous")) { // Проверяем, существует ли поле is_anonymous
                                    boolean isAnonymous = result.get("is_anonymous").getAsBoolean();
                                    if (isAnonymous) {
                                        plugin.getLogger().info("User " + userId + " is an anonymous admin.");
                                        return true; // Считаем анонимного администратора не-админом в этом контексте.
                                    }
                                } else {
                                    plugin.getLogger().warning("is_anonymous field is missing for admin " + userId);
                                }
                            }
                            return isAdmin; // Возвращаем true, если пользователь администратор (и не анонимный, если применимо), или если пользователь - создатель
                        } else {
                            plugin.getLogger().warning("Status field is null in getChatMember response.");
                        }
                    } else {
                        plugin.getLogger().warning("Result field is null in getChatMember response.");
                    }
                } else {
                    plugin.getLogger().warning("getChatMember request failed (ok=false): " + responseBody);
                }
            } else {
                plugin.getLogger().warning("getChatMember request failed: " + response.statusCode() + ", " + responseBody);
            }
        } catch (IOException | InterruptedException e) {
            plugin.getLogger().severe("Error while checking if user is admin: " + e.getMessage());
        }
        return false; // Произошла ошибка или пользователь не является администратором
    }

    public List<String> getBannedCommands() {
        return plugin.getConfig().getStringList("banned_commands");
    }

    public String getChatID() {
        return plugin.getConfig().getString("telegram_chat_id");
    }

    public ConsoleCommandSender getCommandsSender() {
        return plugin.getServer().getConsoleSender();
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
                                            String chat = "0";
                                            String id = "0";
                                            if (message.has("from")) {
                                                JsonObject fromObj = message.getAsJsonObject("from");
                                                if (fromObj != null && fromObj.has("first_name")) {
                                                    from = fromObj.get("first_name").getAsString();
                                                }
                                                if (fromObj != null && fromObj.has("id")) {
                                                    id = fromObj.get("id").getAsString();
                                                }
                                            }
                                            if (message.has("chat")) {
                                                JsonObject ChatObj = message.getAsJsonObject("chat");
                                                if (ChatObj != null && ChatObj.has("id")) {
                                                    chat = ChatObj.get("id").getAsString();
                                                }
                                            }
                                            // Store the telegram name and message
                                            String telegramName = from;
                                            // Format the telegram message using the configuration
                                            String minecraftTelegramFormat = plugin.getConfig().getString("minecraft_telegram_format", "[TG] {telegram_name}: {telegram_message}");
                                            String formattedMessage = minecraftTelegramFormat.replace("{telegram_name}", telegramName).replace("{telegram_message}", text);

                                            if (chat.equalsIgnoreCase(getChatID())) {
                                                if (!text.startsWith("/")) {
                                                    messages.add(formattedMessage);
                                                } else {
                                                    String token = plugin.getConfig().getString("telegram_bot_token");
                                                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                                                    if (isUserAdmin(token, chat_id, id)) {
                                                        if (!getBannedCommands().contains("all")) {
                                                            if (!getBannedCommands().contains(text) && !getBannedCommands().contains(text.split(" ")[0])) {
                                                                messages.add(text);
                                                                String message__ = plugin.getConfig().getString("command_sent_message").replace("{user}", from);
                                                                SendTelegramFUNCTION(token, chat_id, message__);
                                                            } else {
                                                                String banned_command_text = plugin.getConfig().getString("command_was_banned").replace("{user}", from);
                                                                SendTelegramFUNCTION(token, chat_id, banned_command_text);
                                                            }
                                                        } else {
                                                            String commands_was_banned = plugin.getConfig().getString("commands_was_disabled").replace("{user}", from);
                                                            SendTelegramFUNCTION(token, chat_id, commands_was_banned);
                                                        }

                                                    } else {
                                                        String message___ = plugin.getConfig().getString("you_havent_got_permission").replace("{user}", from);
                                                        SendTelegramFUNCTION(token, chat_id, message___);
                                                    }
                                                }
                                            }

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
        ConsoleCommandSender sender = getCommandsSender();

        if (!newMessages.isEmpty()) {
            for (String message : newMessages) {
                if (!message.startsWith("/")) {
                    // Важно: используем экземпляр плагина для доступа к планировщику
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(message); // Send the formatted message directly
                        }
                    });
                    plugin.getLogger().info("Telegram: " + message);
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        String command = message.substring(1);
                        plugin.getServer().dispatchCommand(sender, command);
                        plugin.getLogger().info("Sent a command from the Telegram!");
                    });
                }
            }
        }
    }
}