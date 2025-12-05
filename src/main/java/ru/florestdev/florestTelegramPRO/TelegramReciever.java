package ru.florestdev.florestTelegramPRO;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
        String requestBody = String.format("chat_id=%s&text=%s", chatId, message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "FlorestPlugin")
                .POST(BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            this.plugin.getLogger().info("Successful sending.");
        } else {
            this.plugin.getLogger().info("Own bad! We can't send message to Telegram APIs.");
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
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() == 200) {
                Gson gson = new Gson();
                JsonObject jsonObject = (JsonObject) gson.fromJson(responseBody, JsonObject.class);
                boolean ok = jsonObject.get("ok").getAsBoolean();
                if (ok) {
                    JsonObject result = jsonObject.getAsJsonObject("result");
                    if (result != null) {
                        String status = result.get("status").getAsString();
                        if (status != null) {
                            boolean isAdmin = status.equals("administrator") || status.equals("creator");
                            if (isAdmin && status.equals("administrator") && result.has("is_anonymous") && result.get("is_anonymous").getAsBoolean()) {
                                this.plugin.getLogger().info("User " + userId + " is an anonymous admin.");
                                return true;
                            }
                            return isAdmin;
                        }
                        this.plugin.getLogger().warning("Status field is null in getChatMember response.");
                    } else {
                        this.plugin.getLogger().warning("Result field is null in getChatMember response.");
                    }
                } else {
                    this.plugin.getLogger().warning("getChatMember request failed (ok=false): " + responseBody);
                }
            } else {
                this.plugin.getLogger().warning("getChatMember request failed: " + response.statusCode() + ", " + responseBody);
            }
        } catch (InterruptedException | IOException e) {
            this.plugin.getLogger().severe("Error while checking if user is admin: " + e.getMessage());
        }
        return false;
    }

    public List<String> getBannedCommands() {
        return this.plugin.getConfig().getStringList("banned_commands");
    }

    public String getChatID() {
        return this.plugin.getConfig().getString("telegram_chat_id");
    }

    public ConsoleCommandSender getCommandsSender() {
        return this.plugin.getServer().getConsoleSender();
    }

    public List<String> getNewMessages() {
        List<String> messages = new ArrayList<>();
        String url = String.format("https://api.telegram.org/bot%s/getUpdates?offset=%d", this.botToken, this.lastUpdateId + 1);
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
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
                                    if (updateId > this.lastUpdateId) {
                                        this.lastUpdateId = updateId;
                                    }

                                    // ---- обработка edited_message ----
                                    if (update.has("edited_message")) {
                                        JsonObject edited = update.getAsJsonObject("edited_message");
                                        if (edited != null && edited.has("text")) {
                                            String text = edited.get("text").getAsString();
                                            String from = edited.has("from") && edited.getAsJsonObject("from").has("first_name")
                                                    ? edited.getAsJsonObject("from").get("first_name").getAsString()
                                                    : "Telegram";
                                            String chat = edited.has("chat") && edited.getAsJsonObject("chat").has("id")
                                                    ? edited.getAsJsonObject("chat").get("id").getAsString()
                                                    : "0";
                                            if (chat.equalsIgnoreCase(this.getChatID())) {
                                                String format = plugin.getConfig().getString("minecraft_telegram_edited_message",
                                                        "[TG] {telegram_name} edited message: {telegram_message}");
                                                String formatted = format
                                                        .replace("{telegram_name}", from)
                                                        .replace("{telegram_message}", text);
                                                messages.add(formatted);
                                            }
                                        }
                                    }

                                    // ---- обработка новых сообщений ----
                                    if (update.has("message") && update.getAsJsonObject("message").has("text")) {
                                        JsonObject message = update.getAsJsonObject("message");
                                        if (message != null) {
                                            String text = message.get("text").getAsString();
                                            String from = "Telegram";
                                            String chat = "0";
                                            String id = "0";

                                            if (message.has("from")) {
                                                JsonObject fromObj = message.getAsJsonObject("from");
                                                if (fromObj.has("first_name")) from = fromObj.get("first_name").getAsString();
                                                if (fromObj.has("id")) id = fromObj.get("id").getAsString();
                                            }

                                            if (message.has("chat")) {
                                                JsonObject chatObj = message.getAsJsonObject("chat");
                                                if (chatObj.has("id")) chat = chatObj.get("id").getAsString();
                                            }

                                            // ---- обработка reply_to_message ----
                                            boolean hasReply = message.has("reply_to_message") && plugin.getConfig().getBoolean("support_replies_in_tg");
                                            String formattedMessage;
                                            if (hasReply) {
                                                JsonObject reply = message.getAsJsonObject("reply_to_message");
                                                String authorReply = "unknown";
                                                String authorReplyMsgId = "0";
                                                if (reply.has("from") && reply.getAsJsonObject("from").has("first_name"))
                                                    authorReply = reply.getAsJsonObject("from").get("first_name").getAsString();
                                                if (reply.has("message_id"))
                                                    authorReplyMsgId = reply.get("message_id").getAsString();
                                                String format = plugin.getConfig().getString("minecraft_telegram_format_with_reply",
                                                        "[TG] {telegram_name}: {telegram_message}\n({author_reply} - {author_reply_message_id})");
                                                formattedMessage = format.replace("{telegram_name}", from)
                                                        .replace("{telegram_message}", text)
                                                        .replace("{author_reply}", authorReply)
                                                        .replace("{author_reply_message_id}", authorReplyMsgId);
                                            } else {
                                                String format = plugin.getConfig().getString("minecraft_telegram_format",
                                                        "[TG] {telegram_name}: {telegram_message}");
                                                formattedMessage = format.replace("{telegram_name}", from)
                                                        .replace("{telegram_message}", text);
                                            }

                                            // ---- фильтр команд и добавление в сообщения ----
                                            if (chat.equalsIgnoreCase(this.getChatID())) {
                                                if (!text.startsWith("/")) {
                                                    if (!plugin.getConfig().getBoolean("enable_restrictions_for_messages") ||
                                                            !text.startsWith(plugin.getConfig().getString("prefix_for_telegram")))
                                                        messages.add(formattedMessage);
                                                } else if (text.equalsIgnoreCase("/players")) {
                                                    Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
                                                    if (players.isEmpty()) {
                                                        this.SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("no_players_on_server"));
                                                    } else {
                                                        List<String> playerNames = players.stream().map(Player::getName).toList().stream().limit(200L).toList();
                                                        String message___ = plugin.getConfig().getString("players_msg_format")
                                                                .replace("{online}", String.valueOf(players.size()))
                                                                .replace("{max_players}", String.valueOf(plugin.getServer().getMaxPlayers()))
                                                                .replace("{players_nicknames}", String.join(", ", playerNames));
                                                        this.SendTelegramFUNCTION(botToken, chat, message___);
                                                    }
                                                } else { // команды
                                                    handleTelegramCommand(from, id, text, chat, messages);
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
        } catch (InterruptedException | IOException e) {
            plugin.getLogger().severe("Error fetching Telegram messages: " + e.getMessage());
        }
        return messages;
    }

    private void handleTelegramCommand(String from, String id, String text, String chat, List<String> messages) {
        try {
            if (isUserAdmin(botToken, chat, id) || plugin.getConfig().getStringList("additional_admins").contains(id)) {
                if (!getBannedCommands().contains("all") && !getBannedCommands().contains(text) && !getBannedCommands().contains(text.split(" ")[0])) {
                    messages.add(text);
                    SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("command_sent_message").replace("{user}", from));
                } else {
                    SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("command_was_banned").replace("{user}", from));
                }
            } else {
                SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("you_havent_got_permission").replace("{user}", from));
            }
        } catch (IOException | InterruptedException e) {
            plugin.getLogger().severe("Failed to send Telegram command response: " + e.getMessage());
        }
    }

    public void processMessages() {
        List<String> newMessages = getNewMessages();
        ConsoleCommandSender sender = getCommandsSender();
        if (!newMessages.isEmpty()) {
            for (String message : newMessages) {
                if (!message.startsWith("/")) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(message);
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