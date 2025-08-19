package ru.florestdev.florestTelegramPRO;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class FlorestTelegramPRO extends JavaPlugin implements Listener {

    ChatListener listener = new ChatListener(this);

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        PlayerTracker tracker = new PlayerTracker(this);
        tracker.register();
        CommandTracker trackerCommand = new CommandTracker(this);
        boolean enabled_tracking = getConfig().getBoolean("processing_tracking_enable");
        if (enabled_tracking) {
            trackerCommand.register();
        }
        PluginCommand mainCommand = getCommand("floresttelegram");
        if (mainCommand != null) {
            mainCommand.setExecutor(new CommandHandler(this));
        } else {
            getLogger().warning("Команда /floresttelegram не найдена в plugin.yml!");
        }

        getLogger().info("Hey, bro! I just started now! How are you?");
        saveDefaultConfig();
        String botToken = getConfig().getString("telegram_bot_token");

        try {
            String bot_token = getConfig().getString("telegram_bot_token");
            String chat_id = getConfig().getString("telegram_chat_id");
            String message = getConfig().getString("hello_message");
            listener.SendTelegramFUNCTION(bot_token, chat_id, message);
        } catch (Exception e) {
            getLogger().severe("We didn't send message about server's starting! Config.yml is bad. Disabling..");
            getServer().getPluginManager().disablePlugin(this);
        }

        TelegramReciever telegramReceiver = new TelegramReciever(this, botToken);

        // Start polling for messages every 5 second
        BukkitScheduler sheduler = getServer().getScheduler();
        sheduler.runTaskTimerAsynchronously(this, telegramReceiver::processMessages, 0L, 5 * 20L); // 0 delay, 5 seconds period

    }

    @Override
    public void onDisable() {
        int was_players_ = getServer().getOnlinePlayers().size();
        String was_players = Integer.toString(was_players_);
        try {
            String bot_token = getConfig().getString("telegram_bot_token");
            String chat_id = getConfig().getString("telegram_chat_id");
            String message = getConfig().getString("goodbye_message").replace("{was_players}", was_players);
            listener.SendTelegramFUNCTION(bot_token, chat_id, message);
        } catch (Exception e) {
            getLogger().severe("We didn't send message about server's stopping.");
        }
        getLogger().info("Goodbye, dear server! I'll wait until you start it.");
    }
}