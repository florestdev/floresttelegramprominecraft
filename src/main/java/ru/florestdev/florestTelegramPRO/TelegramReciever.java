package ru.florestdev.florestTelegramPRO;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TelegramReciever {

    private final FlorestTelegramPRO plugin;
    private final String botToken;
    private final HttpClient httpClient;
    private int lastUpdateId;
    public Runtime runtime = Runtime.getRuntime();

    // Вспомогательный класс для хранения данных о сообщении (для реакций)
    private static class CachedMessage {
        final String author;
        final String text;

        CachedMessage(String author, String text) {
            this.author = author;
            // Обрезаем до 100 символов для экономии ОЗУ и чистоты чата
            this.text = text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
    }

    // Кэш: ID сообщения -> {Автор, Текст}. Лимит 1000 записей.
    private final Map<Integer, CachedMessage> messageCache = Collections.synchronizedMap(
            new LinkedHashMap<Integer, CachedMessage>(1001, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, CachedMessage> eldest) {
                    return size() > 1000;
                }
            }
    );

    public TelegramReciever(FlorestTelegramPRO plugin, String botToken) {
        this.plugin = plugin;
        this.botToken = botToken;
        this.httpClient = HttpClient.newHttpClient();
        this.lastUpdateId = 0;
    }

    public void SendTelegramFUNCTION(String botToken, String chatId, String message) {
        try {
            StringBuilder body = new StringBuilder(128);
            body.append("chat_id=").append(chatId);
            body.append("&text=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));

            if (plugin.getConfig().getBoolean("support_themes")) {
                int theme = plugin.getConfig().getInt("follow_theme");
                if (theme > 0) body.append("&message_thread_id=").append(theme);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            // Отправляем и парсим ответ, чтобы сохранить ID сообщения бота в кэш
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        try {
                            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                            if (json.get("ok").getAsBoolean()) {
                                JsonObject result = json.getAsJsonObject("result");
                                int msgId = result.get("message_id").getAsInt();
                                messageCache.put(msgId, new CachedMessage("Бот", message));
                            }
                        } catch (Exception ignored) {}
                    });

        } catch (Exception ignored) {}
    }

    public List<String> getNewMessages() {
        List<String> messages = new ArrayList<>();
        try {
            // Добавляем allowed_updates, чтобы Telegram присылал реакции
            String allowed = URLEncoder.encode(
                    "[\"message\",\"edited_message\",\"message_reaction\"]",
                    StandardCharsets.UTF_8
            );

            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates" +
                    "?offset=" + (lastUpdateId + 1) +
                    "&allowed_updates=" + allowed;

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return messages;

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.get("ok").getAsBoolean()) return messages;

            JsonArray updates = root.getAsJsonArray("result");
            for (JsonElement el : updates) {
                JsonObject upd = el.getAsJsonObject();
                lastUpdateId = upd.get("update_id").getAsInt();

                if (upd.has("message")) {
                    handleMessage(upd.getAsJsonObject("message"), messages);
                } else if (upd.has("edited_message")) {
                    handleEdited(upd.getAsJsonObject("edited_message"), messages);
                } else if (upd.has("message_reaction")) {
                    handleReaction(upd.getAsJsonObject("message_reaction"), messages);
                }
            }
        } catch (Exception ignored) {}
        return messages;
    }

    private void handleMessage(JsonObject msg, List<String> out) {
        if (!msg.has("text") || !msg.getAsJsonObject("chat").get("id").getAsString().equals(getChatID())) return;

        int msgId = msg.get("message_id").getAsInt();
        String from = msg.getAsJsonObject("from").get("first_name").getAsString();
        String text = msg.get("text").getAsString();
        String userId = msg.getAsJsonObject("from").get("id").getAsString();

        // Сохраняем в кэш для будущих реакций
        messageCache.put(msgId, new CachedMessage(from, text));

        if (plugin.getConfig().getBoolean("support_themes")) {
            int theme = plugin.getConfig().getInt("follow_theme");
            if (theme > 0 && (!msg.has("message_thread_id") || msg.get("message_thread_id").getAsInt() != theme)) return;
        }

        if (!text.startsWith("/")) {
            String format = plugin.getConfig().getString("minecraft_telegram_format", "[TG] {telegram_name}: {telegram_message}");
            out.add(format.replace("{telegram_name}", from).replace("{telegram_message}", text));
        } else {
            processInternalCommand(text, from, userId, msg.getAsJsonObject("chat").get("id").getAsString(), out);
        }
    }

    private void handleEdited(JsonObject msg, List<String> out) {
        if (!msg.has("text") || !msg.getAsJsonObject("chat").get("id").getAsString().equals(getChatID())) return;

        String from = msg.getAsJsonObject("from").get("first_name").getAsString();
        String text = msg.get("text").getAsString();

        // Обновляем текст в кэше, если сообщение отредактировали
        messageCache.put(msg.get("message_id").getAsInt(), new CachedMessage(from, text));

        String format = plugin.getConfig().getString("minecraft_telegram_edited_message");
        out.add(format.replace("{telegram_name}", from).replace("{telegram_message}", text));
    }

    private void handleReaction(JsonObject reactionObj, List<String> out) {
        try {
            if (!reactionObj.getAsJsonObject("chat").get("id").getAsString().equals(getChatID())) return;

            int msgId = reactionObj.get("message_id").getAsInt();
            String reactorName = reactionObj.getAsJsonObject("user").get("first_name").getAsString();

            JsonArray newReactions = reactionObj.getAsJsonArray("new_reaction");
            if (newReactions.isEmpty()) return; // Реакцию убрали

            String emoji = newReactions.get(newReactions.size() - 1).getAsJsonObject().get("emoji").getAsString();

            // Достаем данные из кэша
            CachedMessage cached = messageCache.get(msgId);
            String author = (cached != null) ? cached.author : "Unknown";
            String originalText = (cached != null) ? cached.text : "...";

            String format = plugin.getConfig().getString("minecraft_telegram_reaction_received");
            out.add(format.replace("{telegram_name}", reactorName)
                    .replace("{reaction}", emoji)
                    .replace("{author}", author)
                    .replace("{message}", originalText));
        } catch (Exception ignored) {}
    }

    private void processInternalCommand(String text, String from, String userId, String chatId, List<String> out) {
        if (text.equalsIgnoreCase("/players")) {
            sendPlayersList(chatId);
        } else if (text.equalsIgnoreCase("/tps")) {
            sendTpsInfo(chatId);
        } else {
            handleTelegramCommand(from, userId, text, chatId, out);
        }
    }

    // --- Вспомогательные методы для чистоты кода ---

    private void sendPlayersList(String chatId) {
        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
        String msg = players.isEmpty() ? plugin.getConfig().getString("no_players_on_server") :
                plugin.getConfig().getString("players_msg_format")
                        .replace("{online}", String.valueOf(players.size()))
                        .replace("{max_players}", String.valueOf(plugin.getServer().getMaxPlayers()))
                        .replace("{players_nicknames}", String.join(", ", players.stream().map(Player::getName).toList()));
        SendTelegramFUNCTION(botToken, chatId, msg);
    }

    private void sendTpsInfo(String chatId) {
        if (plugin.getEssentials() == null) {
            SendTelegramFUNCTION(botToken, chatId, "EssentialsX not found!");
            return;
        }
        double tps = plugin.essentials.getTimer().getAverageTPS();
        long max = runtime.maxMemory() / 1024 / 1024;
        long used = max - (runtime.freeMemory() / 1024 / 1024);
        String msg = plugin.getConfig().getString("tps_message")
                .replace("{tps}", String.format("%.2f", tps))
                .replace("{ram_usage}", String.valueOf(used))
                .replace("{ram_maximum}", String.valueOf(max));
        SendTelegramFUNCTION(botToken, chatId, msg);
    }

    private void handleTelegramCommand(String from, String id, String text, String chat, List<String> out) {
        if (!isUserAdmin(botToken, chat, id) && !plugin.getConfig().getStringList("additional_admins").contains(id)) {
            SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("you_havent_got_permission").replace("{user}", from));
            return;
        }
        String base = text.split(" ")[0];
        if (getBannedCommands().contains("all") || getBannedCommands().contains(text) || getBannedCommands().contains(base)) {
            SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("command_was_banned").replace("{user}", from));
            return;
        }
        out.add(text);
        SendTelegramFUNCTION(botToken, chat, plugin.getConfig().getString("command_sent_message").replace("{user}", from));
    }

    public boolean isUserAdmin(String botToken, String chatId, String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/getChatMember"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("chat_id=" + chatId + "&user_id=" + userId))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = json.getAsJsonObject("result").get("status").getAsString();
            return status.equals("administrator") || status.equals("creator");
        } catch (Exception e) { return false; }
    }

    public void processMessages() {
        List<String> messages = getNewMessages();
        if (messages.isEmpty()) return;
        ConsoleCommandSender sender = plugin.getServer().getConsoleSender();
        for (String msg : messages) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.startsWith("/")) plugin.getServer().dispatchCommand(sender, msg.substring(1));
                else Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
            });
        }
    }

    public List<String> getBannedCommands() { return plugin.getConfig().getStringList("banned_commands"); }
    public String getChatID() { return plugin.getConfig().getString("telegram_chat_id"); }
}