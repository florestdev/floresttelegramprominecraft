package ru.florestdev.florestTelegramPRO;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TelegramReciever {

    private final FlorestTelegramPRO plugin;
    private final String botToken;
    private final HttpClient httpClient;
    private int lastUpdateId;
    public Runtime runtime = Runtime.getRuntime();

    // Очередь команд для обработки в основном потоке с захватом вывода
    private static class PendingCommand {
        final String command;
        final String chatId;
        final String senderName;

        PendingCommand(String command, String chatId, String senderName) {
            this.command = command;
            this.chatId = chatId;
            this.senderName = senderName;
        }
    }

    private final List<PendingCommand> commandQueue = Collections.synchronizedList(new ArrayList<>());

    private static class CachedMessage {
        final String author;
        final String text;

        CachedMessage(String author, String text) {
            this.author = author;
            this.text = text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
    }

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
            String allowed = URLEncoder.encode("[\"message\",\"edited_message\",\"message_reaction\"]", StandardCharsets.UTF_8);
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1) + "&allowed_updates=" + allowed;

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
                    JsonObject msg = upd.getAsJsonObject("message");
                    if (msg.has("photo")) handlePhoto(msg);
                    else handleMessage(msg, messages);
                } else if (upd.has("edited_message")) {
                    handleEdited(upd.getAsJsonObject("edited_message"), messages);
                } else if (upd.has("message_reaction")) {
                    handleReaction(upd.getAsJsonObject("message_reaction"), messages);
                }
            }
        } catch (Exception ignored) {}
        return messages;
    }

    public void handleMessage(JsonObject msg, List<String> out) {
        // 1. Базовые проверки
        if (!msg.has("text") || !msg.getAsJsonObject("chat").get("id").getAsString().equals(getChatID())) return;

        int msgId = msg.get("message_id").getAsInt();
        String from = msg.getAsJsonObject("from").get("first_name").getAsString();
        String text = msg.get("text").getAsString();
        String userId = msg.getAsJsonObject("from").get("id").getAsString();
        String chatId = msg.getAsJsonObject("chat").get("id").getAsString();

        // Кэшируем (это безопасно делать асинхронно)
        messageCache.put(msgId, new CachedMessage(from, text));

        // Проверка тем (Threads)
        if (plugin.getConfig().getBoolean("support_themes")) {
            int theme = plugin.getConfig().getInt("follow_theme");
            if (theme > 0 && (!msg.has("message_thread_id") || msg.get("message_thread_id").getAsInt() != theme)) return;
        }

        if (!text.startsWith("/")) {
            String format = plugin.getConfig().getString("minecraft_telegram_format", "[TG] {telegram_name}: {telegram_message}");
            String finalMessage = format.replace("{telegram_name}", from).replace("{telegram_message}", text);

            // ВАЖНО: Возвращаемся в основной поток сервера для работы с чатом
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', finalMessage));
            });

            out.add(finalMessage); // Оставляем для совместимости, если нужно
        } else {
            // Команды тоже лучше выполнять в основном потоке внутри processInternalCommand
            processInternalCommand(text, from, userId, chatId, out);
        }
    }

    public void handleEdited(JsonObject msg, List<String> out) {
        if (!msg.has("text") || !msg.getAsJsonObject("chat").get("id").getAsString().equals(getChatID())) return;
        String from = msg.getAsJsonObject("from").get("first_name").getAsString();
        String text = msg.get("text").getAsString();
        messageCache.put(msg.get("message_id").getAsInt(), new CachedMessage(from, text));
        String format = plugin.getConfig().getString("minecraft_telegram_edited_message");
        out.add(format.replace("{telegram_name}", from).replace("{telegram_message}", text));
    }

    public void handleReaction(JsonObject reactionObj, List<String> out) {
        try {
            if (!reactionObj.getAsJsonObject("chat").get("id").getAsString().equals(getChatID())) return;
            int msgId = reactionObj.get("message_id").getAsInt();
            String reactorName = reactionObj.getAsJsonObject("user").get("first_name").getAsString();
            JsonArray newReactions = reactionObj.getAsJsonArray("new_reaction");
            if (newReactions.isEmpty()) return;
            String emoji = newReactions.get(newReactions.size() - 1).getAsJsonObject().get("emoji").getAsString();
            CachedMessage cached = messageCache.get(msgId);
            String author = (cached != null) ? cached.author : "Unknown";
            String originalText = (cached != null) ? cached.text : "...";
            String format = plugin.getConfig().getString("minecraft_telegram_reaction_received");
            out.add(format.replace("{telegram_name}", reactorName).replace("{reaction}", emoji).replace("{author}", author).replace("{message}", originalText));
        } catch (Exception ignored) {}
    }

    public void processInternalCommand(String text, String from, String userId, String chatId, List<String> out) {
        if (text.equalsIgnoreCase("/players")) {
            sendPlayersList(chatId);
        } else if (text.equalsIgnoreCase("/tps")) {
            sendTpsInfo(chatId);
        } else {
            handleTelegramCommand(from, userId, text, chatId, out);
        }
    }

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

    public ItemStack createMapItem(BufferedImage image, Player player) {
        if (player == null) return null;
        MapView view = Bukkit.createMap(player.getWorld());
        for (MapRenderer renderer : view.getRenderers()) view.removeRenderer(renderer);
        view.addRenderer(new TelegramPhotoRenderer(image));
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(view);
            meta.setDisplayName("§dФото с Telegram");
            mapItem.setItemMeta(meta);
        }
        return mapItem;
    }

    public void handlePhoto(JsonObject msg) {
        if (!msg.has("photo")) return;
        String caption = msg.has("caption") ? msg.get("caption").getAsString() : "";
        if (caption.isEmpty()) return;
        String playerName = caption.split(" ")[0];
        JsonArray photos = msg.getAsJsonArray("photo");
        String fileId = photos.get(photos.size() - 1).getAsJsonObject().get("file_id").getAsString();

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest getFileRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId))
                        .GET().build();
                HttpResponse<String> response = httpClient.send(getFileRequest, HttpResponse.BodyHandlers.ofString());
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.get("ok").getAsBoolean()) {
                    String filePath = json.getAsJsonObject("result").get("file_path").getAsString();
                    String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
                    java.io.InputStream in = new URI(downloadUrl).toURL().openStream();
                    BufferedImage image = javax.imageio.ImageIO.read(in);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null && player.isOnline()) {
                            ItemStack map = createMapItem(image, player);
                            player.getInventory().addItem(map).values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
                            player.sendMessage("§a[TG] Вам прислали фото!");
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
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

        // Добавляем команду в очередь для выполнения с перехватом
        commandQueue.add(new PendingCommand(text.startsWith("/") ? text.substring(1) : text, chat, from));
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

    public void processMessages(List<String> messages_) {
        List<String> messages;

        // 1. Определяем источник сообщений
        if (messages_ == null) {
            // Режим Polling: сами идем за списком
            messages = getNewMessages();
        } else {
            // Режим Webhook: берем то, что прислал сервер
            messages = messages_;
        }

        // 2. Отправка обычных сообщений в чат (для ОБОИХ режимов)
        if (messages != null && !messages.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String msg : messages) {
                    // Используем broadcastMessage или итерируем по игрокам
                    Bukkit.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
                }
            });
        }

        // 3. Команды с перехватом вывода
        synchronized (commandQueue) {
            if (commandQueue.isEmpty()) return;
            List<PendingCommand> toProcess = new ArrayList<>(commandQueue);
            commandQueue.clear();

            for (PendingCommand pending : toProcess) {
                // Команды ВСЕГДА выполняем в основном потоке
                Bukkit.getScheduler().runTask(plugin, () -> {
                    InterceptingSender captor = new InterceptingSender();

                    // Выполняем команду
                    plugin.getServer().dispatchCommand(captor, pending.command);

                    // Собираем ответ
                    List<String> captured = captor.getCapturedMessages();
                    String output = captured.isEmpty() ? "---" : String.join("\n", captured);

                    // Очистка от цветовых кодов (§)
                    output = output.replaceAll("(?i)§[0-9a-fk-orx]", "");

                    // Формируем сообщение
                    String configMsg = plugin.getConfig().getString("command_sent_message", "{user}, результат:\n{output}");
                    String finalMsg = configMsg.replace("{user}", pending.senderName).replace("{output}", output);

                    // Отправляем ответ назад в Telegram (асинхронно внутри SendTelegramFUNCTION)
                    SendTelegramFUNCTION(botToken, pending.chatId, finalMsg);
                });
            }
        }
    }

    public List<String> getBannedCommands() { return plugin.getConfig().getStringList("banned_commands"); }
    public String getChatID() { return plugin.getConfig().getString("telegram_chat_id"); }

    /**
     * Внутренний класс для перехвата сообщений консоли
     */
    private static class InterceptingSender implements ConsoleCommandSender {
        private final List<String> capturedMessages = new ArrayList<>();
        private final ConsoleCommandSender actualConsole = Bukkit.getConsoleSender();

        public List<String> getCapturedMessages() { return capturedMessages; }

        @Override public void sendMessage(@NotNull String message) { capturedMessages.add(message); }
        @Override public void sendMessage(@NotNull String[] messages) { Collections.addAll(capturedMessages, messages); }
        @Override public void sendMessage(@Nullable UUID sender, @NotNull String message) { capturedMessages.add(message); }
        @Override public void sendMessage(@Nullable UUID sender, @NotNull String[] messages) { Collections.addAll(capturedMessages, messages); }

        @Override public boolean isPermissionSet(@NotNull String name) { return actualConsole.isPermissionSet(name); }
        @Override public boolean isPermissionSet(@NotNull Permission perm) { return actualConsole.isPermissionSet(perm); }
        @Override public boolean hasPermission(@NotNull String name) { return actualConsole.hasPermission(name); }
        @Override public boolean hasPermission(@NotNull Permission perm) { return actualConsole.hasPermission(perm); }
        @Override public boolean isOp() { return true; }
        @NotNull @Override public Server getServer() { return Bukkit.getServer(); }
        @NotNull @Override public String getName() { return "TelegramConsole"; }
        @NotNull @Override public Spigot spigot() { return actualConsole.spigot(); }

        @Override public void setOp(boolean value) {}
        @NotNull @Override public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) { return actualConsole.addAttachment(plugin, name, value); }
        @NotNull @Override public PermissionAttachment addAttachment(@NotNull Plugin plugin) { return actualConsole.addAttachment(plugin); }
        @Nullable @Override public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) { return null; }
        @Nullable @Override public PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) { return null; }
        @Override public void removeAttachment(@NotNull PermissionAttachment attachment) {}
        @Override public void recalculatePermissions() {}
        @NotNull @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return actualConsole.getEffectivePermissions(); }
        @Override public boolean isConversing() { return false; }
        @Override public void acceptConversationInput(@NotNull String input) {}
        @Override public boolean beginConversation(@NotNull Conversation conversation) { return false; }
        @Override public void abandonConversation(@NotNull Conversation conversation) {}
        @Override public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent event) {}
        @Override public void sendRawMessage(@NotNull String message) { capturedMessages.add(message); }
        @Override public void sendRawMessage(@Nullable UUID sender, @NotNull String message) { capturedMessages.add(message); }
    }
}