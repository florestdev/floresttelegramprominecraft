package ru.florestdev.florestTelegramPRO;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import ru.florestdev.florestDiscordPro.FlorestDiscordPro;

public class ServerStarted implements Listener {

    public static FlorestTelegramPRO telegramPRO;

    public ServerStarted(FlorestTelegramPRO telegramPRO) {
        this.telegramPRO = telegramPRO;
    }

    @EventHandler
    public void onServerStarted(ServerLoadEvent event) {
        Server server = Bukkit.getServer();
        Plugin plugin = server.getPluginManager().getPlugin("FlorestDiscordPro");
        if (plugin instanceof FlorestDiscordPro) {
            telegramPRO.getLogger().info("Initialized the TG-Discord friendship.");
            telegramPRO.updateTgToDiscord(new TGToDiscord(telegramPRO, (FlorestDiscordPro) plugin));
        }
    }
}
