package ru.florestdev.florestTelegramPRO;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TelegramReciever {

    private final FlorestTelegramPRO plugin;
    private final String botToken;
    private final HttpClient httpClient;
    private int lastUpdateId;

    public Runtime runtime = Runtime.getRuntime();

    public TelegramReciever(FlorestTelegramPRO plugin, String botToken) {
        this.plugin = plugin;
        this.botToken = botToken;
        this.httpClient = HttpClient.newHttpClient();
        this.lastUpdateId = 0;
    }

    /* ========================================================= */

    public void SendTelegramFUNCTION(String botToken, String chatId, String message) {
        try {
            StringBuilder body = new StringBuilder(128);
            body.append("chat_id=").append(chatId);
            body.append("&text=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));

            if (plugin.getConfig().getBoolean("support_themes")) {
                int theme = plugin.getConfig().getInt("follow_theme");
                if (theme > 0) {
                    body.append("&message_thread_id=").append(theme);
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception ignored) {}
    }

    /* ========================================================= */

    public boolean isUserAdmin(String botToken, String chatId, String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/getChatMember"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "chat_id=" + chatId + "&user_id=" + userId))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return false;

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.get("ok").getAsBoolean()) return false;

            String status = json.getAsJsonObject("result").get("status").getAsString();
            return status.equals("administrator") || status.equals("creator");

        } catch (Exception e) {
            return false;
        }
    }

    /* ========================================================= */

    public List<String> getNewMessages() {
        List<String> messages = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.telegram.org/bot" + botToken +
                                    "/getUpdates?offset=" + (lastUpdateId + 1)))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return messages;

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.get("ok").getAsBoolean()) return messages;

            JsonArray updates = root.getAsJsonArray("result");
            if (updates.isEmpty()) return messages;

            for (JsonElement el : updates) {
                JsonObject upd = el.getAsJsonObject();
                lastUpdateId = upd.get("update_id").getAsInt();

                if (upd.has("edited_message")) {
                    handleEdited(upd.getAsJsonObject("edited_message"), messages);
                }

                if (upd.has("message")) {
                    handleMessage(upd.getAsJsonObject("message"), messages);
                }
            }

        } catch (Exception ignored) {}

        return messages;
    }

    /* ========================================================= */

    private void handleEdited(JsonObject msg, List<String> out) {
        if (!msg.has("text")) return;
        if (!msg.getAsJsonObject("chat").get("id").getAsString().equals(getChatID()))
            return;

        String from = msg.getAsJsonObject("from").get("first_name").getAsString();
        String text = msg.get("text").getAsString();

        String format = plugin.getConfig().getString(
                "minecraft_telegram_edited_message",
                "[TG] {telegram_name} edited: {telegram_message}"
        );

        out.add(format.replace("{telegram_name}", from)
                .replace("{telegram_message}", text));
    }

    /* ========================================================= */

    private void handleMessage(JsonObject msg, List<String> out) {
        if (!msg.has("text")) return;

        String chatId = msg.getAsJsonObject("chat").get("id").getAsString();
        if (!chatId.equals(getChatID())) return;

        if (plugin.getConfig().getBoolean("support_themes")) {
            int theme = plugin.getConfig().getInt("follow_theme");
            if (theme > 0 && (!msg.has("message_thread_id")
                    || msg.get("message_thread_id").getAsInt() != theme))
                return;
        }

        String text = msg.get("text").getAsString();
        String from = msg.getAsJsonObject("from").get("first_name").getAsString();
        String userId = msg.getAsJsonObject("from").get("id").getAsString();

        if (!text.startsWith("/")) {
            String format = plugin.getConfig().getString(
                    "minecraft_telegram_format",
                    "[TG] {telegram_name}: {telegram_message}"
            );

            out.add(format.replace("{telegram_name}", from)
                    .replace("{telegram_message}", text));
            return;
        }

        if (text.equalsIgnoreCase("/players")) {
            Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
            StringBuilder sb = new StringBuilder(256);

            if (players.isEmpty()) {
                sb.append(plugin.getConfig().getString("no_players_on_server"));
            } else {
                sb.append(plugin.getConfig().getString("players_msg_format")
                        .replace("{online}", String.valueOf(players.size()))
                        .replace("{max_players}", String.valueOf(plugin.getServer().getMaxPlayers()))
                        .replace("{players_nicknames}",
                                String.join(", ", players.stream().limit(200).map(Player::getName).toList())));
            }

            SendTelegramFUNCTION(botToken, chatId, sb.toString());
            return;
        }

        if (text.equalsIgnoreCase("/tps")) {
            if (plugin.getEssentials() == null) {
                SendTelegramFUNCTION(botToken, chatId, "Server haven't got the EssentialsX plugin for this feature. Please install!");
                return;
            } else {
                double currentTps = plugin.essentials.getTimer().getAverageTPS();
                long maxMemory = runtime.maxMemory() / 1024 / 1024;
                long freeMemory = runtime.freeMemory() / 1024 / 1024;

                // Вычисляем реально используемую память
                long usedMemory = maxMemory - freeMemory;
                SendTelegramFUNCTION(botToken, chatId, plugin.getConfig().getString("tps_message").replace("{tps}", String.valueOf(currentTps)).replace("{ram_usage}", String.valueOf(usedMemory)).replace("{ram_maximum}", String.valueOf(maxMemory)));
                return;
            }
        }

        handleTelegramCommand(from, userId, text, chatId, out);
    }

    /* ========================================================= */

    private void handleTelegramCommand(String from, String id, String text, String chat, List<String> out) {
        if (!isUserAdmin(botToken, chat, id)
                && !plugin.getConfig().getStringList("additional_admins").contains(id)) {
            SendTelegramFUNCTION(botToken, chat,
                    plugin.getConfig().getString("you_havent_got_permission").replace("{user}", from));
            return;
        }

        String base = text.split(" ")[0];
        if (getBannedCommands().contains("all")
                || getBannedCommands().contains(text)
                || getBannedCommands().contains(base)) {
            SendTelegramFUNCTION(botToken, chat,
                    plugin.getConfig().getString("command_was_banned").replace("{user}", from));
            return;
        }

        out.add(text);
        SendTelegramFUNCTION(botToken, chat,
                plugin.getConfig().getString("command_sent_message").replace("{user}", from));
    }

    /* ========================================================= */

    public void processMessages() {
        List<String> messages = getNewMessages();
        if (messages.isEmpty()) return;

        ConsoleCommandSender sender = getCommandsSender();
        for (String msg : messages) {
            if (msg.startsWith("/")) {
                // dispatch commands синхронно
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getServer().dispatchCommand(sender, msg.substring(1)));
            } else {
                // отправка игрокам синхронно
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg)));
            }
        }
    }

    /* ========================================================= */

    public List<String> getBannedCommands() {
        return plugin.getConfig().getStringList("banned_commands");
    }

    public String getChatID() {
        return plugin.getConfig().getString("telegram_chat_id");
    }

    public ConsoleCommandSender getCommandsSender() {
        return plugin.getServer().getConsoleSender();
    }
}