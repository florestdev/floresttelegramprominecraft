package ru.florestdev.florestTelegramPRO;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CommandHandler implements CommandExecutor {

    private final FlorestTelegramPRO plugin;

    public CommandHandler(FlorestTelegramPRO plugin) {
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Пожалуйста, укажите подкоманду! (пжпжпжпж)");
        } else {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                sender.sendMessage("Дада, мы перезагрузили плагин.");
            } else if (subcommand.equalsIgnoreCase("close")) {
                String bot_token = plugin.getConfig().getString("telegram_bot_token");
                String chat_id = plugin.getConfig().getString("telegram_chat_id");
                try {
                    restrictChat(bot_token, chat_id);
                    sender.sendMessage("Запрос отправлен.");
                } catch (Exception e) {
                    plugin.getLogger().info("Plugin's error: " + e.getMessage());
                }
                sender.sendMessage("Чат был закрыт!");
            } else if (subcommand.equalsIgnoreCase("open")) {
                String bot_token = plugin.getConfig().getString("telegram_bot_token");
                String chat_id = plugin.getConfig().getString("telegram_chat_id");
                try {
                    unrestrictChat(bot_token, chat_id);
                    sender.sendMessage("Запрос отправлен.");
                } catch (Exception e) {
                    plugin.getLogger().info("Plugin's error: " + e.getMessage());
                }
            } else if (subcommand.equalsIgnoreCase("ban")) {
                if (args.length == 2) {
                    String bot_token = plugin.getConfig().getString("telegram_bot_token");
                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                    try {
                        banTelegramUser(bot_token, chat_id, args[1]);
                        sender.sendMessage("Запрос отправлен.");
                    } catch (Exception e) {
                        plugin.getLogger().info("Plugin's error: " + e.getMessage());
                    }
                } else {
                    sender.sendMessage("Должно быть два аргумента: подкоманда и ID челика для бана.");
                }
            } else if (subcommand.equalsIgnoreCase("unban")) {
                if (args.length == 2) {
                    String bot_token = plugin.getConfig().getString("telegram_bot_token");
                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                    try {
                        unbanTelegramUser(bot_token, chat_id, args[1]);
                        sender.sendMessage("Запрос отправлен.");
                    } catch (Exception e) {
                        plugin.getLogger().info("Plugin's error: " + e.getMessage());
                    }
                } else {
                    sender.sendMessage("Должно быть два аргумента: подкоманда и ID челика для разбана.");
                }
            } else if (subcommand.equalsIgnoreCase("mute")) {
                if (args.length == 2) {
                    String bot_token = plugin.getConfig().getString("telegram_bot_token");
                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                    try {
                        revokeUserRights(bot_token, chat_id, args[1]);
                        sender.sendMessage("Запрос отправлен.");
                    } catch (Exception e) {
                        plugin.getLogger().info("Plugin's error: " + e.getMessage());
                    }
                } else {
                    sender.sendMessage("Должно быть два аргумента: подкоманда и ID челика для мьюта!");
                }
            } else if (subcommand.equalsIgnoreCase("unmute")) {
                if (args.length == 2) {
                    String bot_token = plugin.getConfig().getString("telegram_bot_token");
                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                    try {
                        restoreUserRights(bot_token, chat_id, args[1]);
                        sender.sendMessage("Запрос отправлен.");
                    } catch (Exception e) {
                        plugin.getLogger().info("Plugin's error: " + e.getMessage());
                    }
                } else {
                    sender.sendMessage("Должно быть два аргумента: подкоманда и ID челика для размьюта!");
                }
            } else {
                sender.sendMessage("Неизвестная подкоманда, брат. Usage: /ftp [open/close/ban/unban/reload/mute/unmute] <ID если надо>");
            }
        }
        return false;
    }
}