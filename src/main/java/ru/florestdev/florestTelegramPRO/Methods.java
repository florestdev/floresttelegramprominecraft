package ru.florestdev.florestTelegramPRO;

import com.google.gson.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Methods {

    public final Plugin plugin;

    public Methods(Plugin plugin) {
        this.plugin = plugin;
    }

    public void banTelegramUser(String botToken, String chatId, String userId) throws IOException, InterruptedException {
        String url = String.format("https://api.telegram.org/bot%s/banChatMember", botToken); // Проверьте метод API
        String requestBody = String.format("chat_id=%s&user_id=%s", chatId, userId); // Проверьте параметры
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            plugin.getLogger().info("User " + userId + " banned successfully in chat " + chatId);
        } else {
            plugin.getLogger().warning("Failed to ban user " + userId + " in chat " + chatId + ". Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    public void unbanTelegramUser(String botToken, String chatId, String userId) throws IOException, InterruptedException {
        // Функция для разбана пользователя (ТРЕБУЕТ ПРОВЕРКИ API Telegram)
        String url = String.format("https://api.telegram.org/bot%s/unbanChatMember", botToken); // Проверьте метод API
        String requestBody = String.format("chat_id=%s&user_id=%s", chatId, userId); // Проверьте параметры
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            plugin.getLogger().info("User " + userId + " unbanned successfully in chat " + chatId);
        } else {
            plugin.getLogger().warning("Failed to unban user " + userId + " in chat " + chatId + ". Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    //Реализация мьюта/размьюта требует более детального анализа API, чтобы найти, как это сделать
    //Возможно, потребуется использовать права администратора в канале

    public void restrictChat(String botToken, String chatId) throws IOException, InterruptedException {
        // Функция для закрытия чата от сообщений (ОЧЕНЬ ВАЖНО ПРОВЕРИТЬ API Telegram)
        //  Это может потребовать изменения прав группы через API
        //  Возможно, это вообще не получится сделать через Bot API

        //*** ПРИМЕР (НЕ РАБОЧИЙ, ТОЛЬКО ДЛЯ ИЛЛЮСТРАЦИИ): ***
        //  Предположим, есть метод API для изменения прав группы
        String url = String.format("https://api.telegram.org/bot%s/setChatPermissions", botToken); //ВЫДУМАННЫЙ МЕТОД
        String requestBody = String.format("chat_id=%s&can_send_messages=false", chatId); //ВЫДУМАННЫЕ ПАРАМЕТРЫ
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            plugin.getLogger().info("Chat " + chatId + " restricted successfully.");
        } else {
            plugin.getLogger().warning("Failed to restrict chat " + chatId + ". Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    public void unrestrictChat(String botToken, String chatId) throws IOException, InterruptedException {
        String url = String.format("https://api.telegram.org/bot%s/setChatPermissions", botToken); //ВЫДУМАННЫЙ МЕТОД
        String requestBody = String.format("chat_id=%s&can_send_messages=true", chatId); //ВЫДУМАННЫЕ ПАРАМЕТРЫ
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            plugin.getLogger().info("Chat " + chatId + " unrestricted successfully." + "\n" + response.body());
        } else {
            plugin.getLogger().warning("Failed to unrestrict chat " + chatId + ". Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    // Helper function to make API requests
    private String makeApiRequest(String botToken, String method, String requestBody) throws IOException, InterruptedException {
        String url = String.format("https://api.telegram.org/bot%s/%s", botToken, method);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            plugin.getLogger().warning("API Request failed: " + method + " Status code: " + response.statusCode() + ", Body: " + response.body());
            throw new IOException("API request failed with status code: " + response.statusCode());
        }
    }

    public void revokeUserRights(String botToken, String chatId, String userId) {
        try {
            // Revoke all rights (can_send_messages=false, can_send_media_messages=false, etc.)
            String requestBody = String.format(
                    "chat_id=%s&user_id=%s&can_send_messages=false&can_send_media_messages=false&" +
                            "can_send_polls=false&can_send_other_messages=false&can_add_web_page_previews=false&",
                    chatId, userId);

            String result = makeApiRequest(botToken, "restrictChatMember", requestBody);

            plugin.getLogger().info("Rights revoked for user " + userId + " in chat " + chatId + ". Result: " + result);

        } catch (IOException | InterruptedException e) {
            plugin.getLogger().severe("Exception while revoking rights for user " + userId + ": " + e.getMessage());
        }
    }

    public void restoreUserRights(String botToken, String chatId, String userId) {
        try {
            // Restore default rights (can_send_messages=true, can_send_media_messages=true, etc.)
            String requestBody = String.format(
                    "chat_id=%s&user_id=%s&can_send_messages=true&can_send_media_messages=true&" +
                            "can_send_polls=true&can_send_other_messages=true&can_add_web_page_previews=true&",
                    chatId, userId);

            String result = makeApiRequest(botToken, "restrictChatMember", requestBody);

            plugin.getLogger().info("Rights restored for user " + userId + " in chat " + chatId + ". Result: " + result);

        } catch (IOException | InterruptedException e) {
            plugin.getLogger().severe("Exception while restoring rights for user " + userId + ": " + e.getMessage());
        }
    }

    public void setChatDescription(String botToken, String chatID, String text) throws IOException, InterruptedException {
        String url = String.format("https://api.telegram.org/bot%s/setChatDescription", botToken);
        String requestBody = String.format("chat_id=%s&description=%s", chatID, text);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            plugin.getLogger().info("Successful editing.");
        }
        else {
            plugin.getLogger().info("Own bad! Not successful editing.");
        }
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
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

    public List<String> getNewMessages(String botToken, int lastUpdateId) {
        List<String> messages = new ArrayList<>();
        String url = String.format("https://api.telegram.org/bot%s/getUpdates?offset=%d", botToken, lastUpdateId + 1);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

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
                                            String reply_author = "----------------------------";
                                            String reply_id = "---------------------------------";
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
                                            if (message.has("reply_to_message")) {
                                                JsonObject replyMessageObj = message.getAsJsonObject("reply_to_message");
                                                if (replyMessageObj != null && replyMessageObj.has("id")) {
                                                    reply_id = replyMessageObj.get("id").getAsString();
                                                }
                                                if (replyMessageObj != null && replyMessageObj.has("from")) {
                                                    JsonObject repliedFromObj = replyMessageObj.getAsJsonObject("from");
                                                    if (repliedFromObj != null && repliedFromObj.has("first_name")) {
                                                        String repliedFromName = repliedFromObj.get("first_name").getAsString();
                                                        reply_author = repliedFromName;
                                                    }
                                                }
                                            }
                                            // Store the telegram name and message
                                            String telegramName = from;
                                            // Format the telegram message using the configuration
                                            String minecraftTelegramFormat = plugin.getConfig().getString("minecraft_telegram_format", "[TG] {telegram_name}: {telegram_message}");

                                            String formattedMessage = minecraftTelegramFormat.replace("{telegram_name}", telegramName).replace("{telegram_message}", text);
                                            String formattedMessageWithReply = minecraftTelegramFormat.replace("{telegram_name}", telegramName).replace("{telegram_message}", text);

                                            if (plugin.getConfig().getBoolean("support_replies_in_tg")) {
                                                if (reply_id.equalsIgnoreCase("---------------------------------")) {
                                                    formattedMessageWithReply = null;
                                                } else {
                                                    formattedMessageWithReply = formattedMessageWithReply.replace("{author_reply}", reply_author).replace("{author_reply_message_id}", reply_id);
                                                }
                                            } else {
                                                formattedMessageWithReply = null;
                                            }

                                            if (chat.equalsIgnoreCase(getChatID())) {
                                                if (!text.startsWith("/")) {
                                                    if (!plugin.getConfig().getBoolean("enable_restrictions_for_messages")) {
                                                        if (formattedMessageWithReply == null) {
                                                            messages.add(formattedMessage);
                                                        } else {
                                                            messages.add(formattedMessageWithReply);
                                                        }
                                                    } else {
                                                        if (!text.startsWith(Objects.requireNonNull(plugin.getConfig().getString("prefix_for_telegram")))) {
                                                            if (formattedMessageWithReply == null) {
                                                                messages.add(formattedMessage);
                                                            } else {
                                                                messages.add(formattedMessageWithReply);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (text.equalsIgnoreCase("/players")) {
                                                        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();

                                                        if (players.isEmpty()) {
                                                            String token = plugin.getConfig().getString("telegram_bot_token");
                                                            String chat_id = plugin.getConfig().getString("telegram_chat_id");
                                                            SendTelegramFUNCTION(token, chat_id, plugin.getConfig().getString("no_players_on_server"));
                                                        } else {

                                                            List<String> playerNames = players.stream()
                                                                    .map(Player::getName)
                                                                    .toList()
                                                                    .stream().limit(200).toList();
                                                            String token = plugin.getConfig().getString("telegram_bot_token");
                                                            String chat_id = plugin.getConfig().getString("telegram_chat_id");
                                                            String message___ = plugin.getConfig().getString("players_msg_format").replace("{online}", String.valueOf(players.size())).replace("{max_players}", String.valueOf(plugin.getServer().getMaxPlayers())).replace("{players_nicknames}", String.join(", ", playerNames));
                                                            SendTelegramFUNCTION(token, chat_id, message___);
                                                        }

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
                                                            if (!plugin.getConfig().getStringList("additional_admins").contains(id)) {
                                                                String message___ = plugin.getConfig().getString("you_havent_got_permission").replace("{user}", from);
                                                                SendTelegramFUNCTION(token, chat_id, message___);
                                                            } else {
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
                                                            }
                                                        }
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

}
