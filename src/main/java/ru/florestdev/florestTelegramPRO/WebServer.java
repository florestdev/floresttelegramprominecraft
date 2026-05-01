package ru.florestdev.florestTelegramPRO;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class WebServer {
    private final FlorestTelegramPRO tg;
    public HttpServer server = null;

    public WebServer(FlorestTelegramPRO tg) {
        this.tg = tg;
    }

    /**
     * Метод для обработки JSON от Вебхука
     */
    public List<String> getNewMessages(String body) {
        List<String> messages = new ArrayList<>();
        try {
            // В вебхуке корень JSON - это и есть сам объект Update
            JsonObject upd = JsonParser.parseString(body).getAsJsonObject();

            // Проверяем, что это вообще апдейт (наличие id)
            if (!upd.has("update_id")) return messages;

            if (upd.has("message")) {
                JsonObject msg = upd.getAsJsonObject("message");
                if (msg.has("photo")) {
                    tg.telegramReceiver.handlePhoto(msg);
                } else {
                    tg.telegramReceiver.handleMessage(msg, messages);
                }
            } else if (upd.has("edited_message")) {
                tg.telegramReceiver.handleEdited(upd.getAsJsonObject("edited_message"), messages);
            } else if (upd.has("message_reaction")) {
                tg.telegramReceiver.handleReaction(upd.getAsJsonObject("message_reaction"), messages);
            }

        } catch (Exception e) {
            tg.getLogger().warning("Ошибка парсинга JSON вебхука: " + e.getMessage());
        }
        return messages;
    }

    public void registerWebhookAsync() {
        tg.getServer().getScheduler().runTaskAsynchronously(tg, () -> {
            try {
                String token = tg.getConfig().getString("telegram_bot_token");
                String webhookUrl = tg.getConfig().getString("webhook-url");
                String allowedUpdates = "[\"message\",\"edited_message\",\"message_reaction\"]";

                String requestUrl = String.format(
                        "https://api.telegram.org/bot%s/setWebhook?url=%s&allowed_updates=%s",
                        token,
                        URLEncoder.encode(webhookUrl, StandardCharsets.UTF_8),
                        URLEncoder.encode(allowedUpdates, StandardCharsets.UTF_8)
                );

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(requestUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    tg.getLogger().info("§a[Telegram] Вебхук успешно зарегистрирован!");
                } else {
                    tg.getLogger().warning("§c[Telegram] Ошибка регистрации вебхука! Код: " + conn.getResponseCode());
                }
                conn.disconnect();
            } catch (Exception e) {
                tg.getLogger().severe("Ошибка при регистрации вебхука: " + e.getMessage());
            }
        });
    }

    public void startWebServer() {
        String address = tg.getConfig().getString("webhook-ip", "0.0.0.0");
        int port = tg.getConfig().getInt("webhook-port", 8080);

        try {
            server = HttpServer.create(new InetSocketAddress(address, port), 0);

            server.createContext("/webhook", exchange -> {
                try {
                    // 1. Читаем входящий JSON
                    java.io.InputStream inputStream = exchange.getRequestBody();
                    java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
                    String jsonString = scanner.hasNext() ? scanner.next() : "";

                    // 2. Отправляем ответ "OK"
                    String response = "OK";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();

                    // 3. Обрабатываем полученные данные
                    if (!jsonString.isEmpty()) {
                        List<String> messages = getNewMessages(jsonString);
                        // Прокидываем в ресивер для вывода в чат или выполнения команд
                        tg.telegramReceiver.processMessages(messages);
                    }
                } catch (Exception e) {
                    tg.getLogger().warning("Ошибка обработки запроса: " + e.getMessage());
                }
            });

            server.setExecutor(Executors.newFixedThreadPool(2));
            server.start();

            tg.getLogger().info("§a[Web] Сервер запущен на " + address + ":" + port);

            // Авто-регистрация в ТГ при старте
            registerWebhookAsync();

        } catch (IOException e) {
            tg.getLogger().severe("Не удалось запустить веб-сервер: " + e.getMessage());
        }
    }

    public void stopWebServer() {
        if (server != null) {
            server.stop(0);
            tg.getLogger().info("§7Веб-сервер остановлен.");
        }
    }
}