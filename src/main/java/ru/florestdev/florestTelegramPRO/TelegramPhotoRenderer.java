package ru.florestdev.florestTelegramPRO;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer; // Оригинальный класс из Bukkit
import org.bukkit.map.MapView;
import java.awt.image.BufferedImage;

// Переименовываем, чтобы не было конфликта с импортированным MapRenderer
public class TelegramPhotoRenderer extends MapRenderer {

    private final BufferedImage image;
    private boolean rendered = false;

    public TelegramPhotoRenderer(BufferedImage image) {
        // super(true) говорит Bukkit, что этот рендер контекстный (для каждого игрока свой),
        // но в нашем случае это просто хороший тон.
        super(false);
        this.image = image;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return;

        // Оптимизация: MapPalette.resizeImage уже умеет масштабировать и подгонять цвета под палитру Minecraft
        // Тебе не нужно вручную делать getScaledInstance
        BufferedImage minecraftReadyImage = MapPalette.resizeImage(image);

        // Отрисовываем готовую картинку
        canvas.drawImage(0, 0, minecraftReadyImage);

        rendered = true;
    }
}