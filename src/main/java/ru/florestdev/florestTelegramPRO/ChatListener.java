package ru.florestdev.florestTelegramPRO;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;


public class ChatListener implements Listener {

    private final FlorestTelegramPRO main; // Ссылка на ваш главный класс плагина
    private static final HttpClient httpClient = HttpClient.newHttpClient(); // Один экземпляр HttpClient

    public ChatListener(FlorestTelegramPRO main) {
        this.main = main;
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
            main.getLogger().info("Successful sending.");
        }
        else {
            main.getLogger().info("Own bad! We can't send message to Telegram APIs.");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Код обработки сообщения в чате
        String message = event.getMessage(); // Получаем сообщение
        Player player = event.getPlayer(); // Получаем игрока, отправившего сообщение
        String playerName = player.getName(); // Получаем имя игрока

        // Пример: Выводим сообщение в консоль
        String token = main.getConfig().getString("telegram_bot_token");
        String chat_id = main.getConfig().getString("telegram_chat_id");
        String messageFormat = main.getConfig().getString("telegram_message_format", "[MC] {player}: {message}"); // Второй аргумент - значение по умолчанию, если опция отсутствует в конфиге.
        String formattedMessage = messageFormat.replace("{player}", playerName).replace("{message}", message);
        try {
            SendTelegramFUNCTION(token, chat_id, formattedMessage);
        } catch (IOException e) {
            main.getLogger().severe("IOException при отправке сообщения в Telegram: " + e.getMessage());
        } catch (InterruptedException e) {
            main.getLogger().severe("InterruptedException при отправке сообщения в Telegram: " + e.getMessage());
            Thread.currentThread().interrupt(); // Важно: прерываем текущий поток
        }
    }
}