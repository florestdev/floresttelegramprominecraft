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
    private final Methods methods;

    public CommandHandler(FlorestTelegramPRO plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
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
                    methods.restrictChat(bot_token, chat_id);
                    sender.sendMessage("Запрос отправлен.");
                } catch (Exception e) {
                    plugin.getLogger().info("Plugin's error: " + e.getMessage());
                }
                sender.sendMessage("Чат был закрыт!");
            } else if (subcommand.equalsIgnoreCase("open")) {
                String bot_token = plugin.getConfig().getString("telegram_bot_token");
                String chat_id = plugin.getConfig().getString("telegram_chat_id");
                try {
                    methods.unrestrictChat(bot_token, chat_id);
                    sender.sendMessage("Запрос отправлен.");
                } catch (Exception e) {
                    plugin.getLogger().info("Plugin's error: " + e.getMessage());
                }
            } else if (subcommand.equalsIgnoreCase("ban")) {
                if (args.length == 2) {
                    String bot_token = plugin.getConfig().getString("telegram_bot_token");
                    String chat_id = plugin.getConfig().getString("telegram_chat_id");
                    try {
                        methods.banTelegramUser(bot_token, chat_id, args[1]);
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
                        methods.unbanTelegramUser(bot_token, chat_id, args[1]);
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
                        methods.revokeUserRights(bot_token, chat_id, args[1]);
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
                        methods.restoreUserRights(bot_token, chat_id, args[1]);
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