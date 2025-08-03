package ru.florestdev.florestTelegramPRO;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
public class ReloadConfigCommand implements CommandExecutor {

    private final FlorestTelegramPRO plugin;

    public ReloadConfigCommand(FlorestTelegramPRO plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reloadconfig")) {
            if (sender.hasPermission("floresttelegram.reloadconfig")) { // Проверка прав
                plugin.reloadConfig(); // Перезагрузка конфигурации
                sender.sendMessage("Конфигурация FlorestTelegram перезагружена."); // Отправка сообщения игроку
                return true;
            } else {
                sender.sendMessage("У вас нет прав для выполнения этой команды."); // Сообщение об отсутствии прав
                return true;
            }
        }
        return false;
    }
}