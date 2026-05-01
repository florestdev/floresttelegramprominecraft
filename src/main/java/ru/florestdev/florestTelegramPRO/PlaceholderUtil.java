package ru.florestdev.florestTelegramPRO;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderUtil {

    /**
     * Парсит плейсхолдеры в строке для конкретного игрока
     * @param player игрок (может быть null для серверных плейсхолдеров)
     * @param text строка с плейсхолдерами вида %player_name%
     * @return строка с замененными плейсхолдерами
     */
    public static String parsePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return text;

        // Если PlaceholderAPI нет или текст не содержит плейсхолдеров - возвращаем как есть
        if (!containsPlaceholders(text)) return text;

        // Парсим через PlaceholderAPI
        // Можно передать null для серверных плейсхолдеров (%server_online% и т.д.)
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Проверяет, есть ли в строке плейсхолдеры (содержит %)
     */
    private static boolean containsPlaceholders(String text) {
        return text.contains("%");
    }
}