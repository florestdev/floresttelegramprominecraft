package ru.florestdev.florestTelegramPRO;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class TPSListener {
    public final Plugin plugin;
    private final List<Long> tickTimes = new ArrayList<>(); // Список временных меток тиков
    private BukkitTask tpsTask; // Задача для периодического расчета TPS
    private static final HttpClient httpClient = HttpClient.newHttpClient(); // Один экземпляр HttpClient
    private long lastMeasurementTime = System.currentTimeMillis();

    private volatile double currentTPS = 20.0; // Текущий TPS

    public TPSListener(Plugin plugin) {
        this.plugin = plugin;
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

    public void startTPSUpdateTask() {
        if (tpsTask != null && !tpsTask.isCancelled()) {
            return; // Уже запущено
        }
        // Запуск каждые 20 тиков (1 секунда), задержка 20 тиков
        tpsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            long currentTime = System.currentTimeMillis();
            long interval = currentTime - lastMeasurementTime; // Продолжительность интервала в мс

            // Очищаем временные метки старше 1 секунды
            tickTimes.removeIf(tick -> tick < lastMeasurementTime);
            tickTimes.add(currentTime);

            // Расчет TPS
            int ticks = tickTimes.size();
            if (interval >= 1000) {
                currentTPS = (double) ticks * 1000 / interval;
                int minimum = plugin.getConfig().getInt("tps_min");
                if (currentTPS < minimum) {
                    String token = plugin.getConfig().getString("telegram_bot_token");
                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                    String message = plugin.getConfig().getString("message_tps_lagg").replace("{tps}", String.valueOf(currentTPS));
                    try {
                        SendTelegramFUNCTION(token, chat_id, message);
                    } catch (InterruptedException | IOException e) {
                        // ...
                    }
                }
            }
        }, 20L, 20L); // Старт через 1 сек, повтор каждые 1 сек
        plugin.getLogger().log(Level.INFO, "Simple TPS tracking started.");
    }

    public void startTask() {
        if (tpsTask == null || tpsTask.isCancelled()) {
            startTPSUpdateTask();
        }
    }

}
