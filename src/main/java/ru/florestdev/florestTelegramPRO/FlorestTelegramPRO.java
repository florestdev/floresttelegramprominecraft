package ru.florestdev.florestTelegramPRO;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class FlorestTelegramPRO extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        PluginCommand reloadCommand = getCommand("reloadconfig");
        if (reloadCommand != null) {
            reloadCommand.setExecutor(new ReloadConfigCommand(this));
        } else {
            getLogger().warning("Команда /reloadconfig не найдена в plugin.yml!");
        }
        getLogger().info("Hey, bro! I just started now! How are you?");
        saveDefaultConfig();
        String botToken = getConfig().getString("telegram_bot_token");
        TelegramReciever telegramReceiver = new TelegramReciever(this, botToken);

        // Start polling for messages every 5 seconds
        BukkitScheduler sheduler = getServer().getScheduler();
        sheduler.runTaskTimerAsynchronously(this, telegramReceiver::processMessages, 0L, 5 * 20L); // 0 delay, 5 seconds period
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Goodbye, dear server! I'll wait until you start it.");
    }
}
