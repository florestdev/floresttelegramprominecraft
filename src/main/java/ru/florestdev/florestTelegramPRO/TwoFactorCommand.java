package ru.florestdev.florestTelegramPRO;

import net.kyori.adventure.platform.facet.Facet;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TwoFactorCommand implements CommandExecutor {
    /**
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return
     */

    private FlorestTelegramPRO telegramPRO;

    public TwoFactorCommand(FlorestTelegramPRO plugin) {
        this.telegramPRO = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Command usage: /2fa <enable/disable/code> <ID TG/code>");
            return true;
        }
        String arg = args[0];
        if (arg.equalsIgnoreCase("disable")) {
            if (args.length == 1) {
                TwoFactorDatabase.TwoFactorData data = telegramPRO.getTwoFactorDatabase().getByUsername(sender.getName());
                if (data == null || !data.enabled) {
                    sender.sendMessage(ChatColor.RED + telegramPRO.getConfig().getString("2fa_disabled"));
                    return true;
                }
                // Только потом удаляем
                telegramPRO.getTwoFactorDatabase().delete(sender.getName());
                sender.sendMessage(ChatColor.RED + telegramPRO.getConfig().getString("2fa_disabled"));
                return true;
            } else {
                if (sender.hasPermission("2fa.admin")) {
                    String username = args[1];
                    TwoFactorDatabase.TwoFactorData data = telegramPRO.getTwoFactorDatabase().getByUsername(username);
                    if (data == null || !data.enabled) {
                        sender.sendMessage(ChatColor.RED + telegramPRO.getConfig().getString("2fa_disabled"));
                        return true;
                    }
                    // Только потом удаляем
                    telegramPRO.getTwoFactorDatabase().delete(username);
                    sender.sendMessage(ChatColor.RED + telegramPRO.getConfig().getString("2fa_disabled"));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "No access!");
                    return true;
                }
            }
        } else if (arg.equalsIgnoreCase("enable")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /2fa enable <TG ID>\nGET YOUR TG ID FROM BOT @GetsMyIDBot");
                return true;
            }
            telegramPRO.methods.sendTelegramMessageToUser(telegramPRO.getConfig().getString("telegram_bot_token"), args[1], telegramPRO.getConfig().getString("2fa_connected").replace("{player}", sender.getName()));
            sender.sendMessage(ChatColor.GREEN + telegramPRO.getConfig().getString("2fa_connected_minecraft"));
            telegramPRO.getTwoFactorDatabase().saveOrUpdate(sender.getName(), args[1], "0", 0, true);
            return true;
        } else if (arg.equalsIgnoreCase("code")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /2fa code <code>");
                return true;
            }
            if (telegramPRO.getTwoFactorDatabase().getByUsername(sender.getName()) == null || !telegramPRO.getTwoFactorDatabase().getByUsername(sender.getName()).enabled) {
                sender.sendMessage(ChatColor.RED + telegramPRO.getConfig().getString("2fa_inactive"));
                return true;
            }

            if (telegramPRO.getTwoFactorDatabase().getByUsername(sender.getName()).code.equalsIgnoreCase("0")) {
                sender.sendMessage(ChatColor.RED + telegramPRO.getConfig().getString("2fa_passed"));
                return true;
            }

            if (telegramPRO.getTwoFactorDatabase().getByUsername(sender.getName()).isCodeValid(args[1], System.currentTimeMillis())) {
                sender.sendMessage(ChatColor.GREEN + telegramPRO.getConfig().getString("2fa_verified"));
                telegramPRO.getTwoFactorHandler().unfreezePlayer((Player) sender);
                telegramPRO.getTwoFactorDatabase().updateCode(sender.getName(), "0", 0);
                return  true;
            } else {
                Player sender_as_player = (Player) sender;
                sender_as_player.kickPlayer(ChatColor.RED + telegramPRO.getConfig().getString("2fa_failed"));
            }

        }

        return true;
    }
}
