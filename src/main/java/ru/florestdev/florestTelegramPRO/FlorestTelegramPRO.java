package ru.florestdev.florestTelegramPRO;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import net.luckperms.api.LuckPerms;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class FlorestTelegramPRO extends JavaPlugin {

    Methods methods = new Methods(this);

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ChatListener(this, methods), this);
        PlayerTracker tracker = new PlayerTracker(this, methods);
        tracker.register();
        CommandTracker trackerCommand = new CommandTracker(this, methods);
        boolean enabled_tracking = getConfig().getBoolean("processing_tracking_enable");
        if (enabled_tracking) {
            trackerCommand.register();
        }
        PluginCommand mainCommand = getCommand("floresttelegram");
        if (mainCommand != null) {
            mainCommand.setExecutor(new CommandHandler(this, methods));
        } else {
            getLogger().warning("Команда /floresttelegram не найдена в plugin.yml!");
        }

        if (getConfig().getBoolean("enable_advancements")) {
            AchievementManager achievementManager = new AchievementManager(this, methods);
            getServer().getPluginManager().registerEvents(achievementManager, this);
        }

        if (getConfig().getBoolean("enable_tps_tracking")) {
            TPSListener tpsListener = new TPSListener(this);
            tpsListener.startTask();
        }

        getLogger().info("Hey, bro! I just started now! How are you?");
        saveDefaultConfig();
        String botToken = getConfig().getString("telegram_bot_token");

        if (getConfig().getBoolean("support_prefix")) {
            if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
                getLogger().severe("Your server doesn't have LuckPerms for support_prefix! Disabling..");
                getServer().getPluginManager().disablePlugin(this);
            } else  {
                RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    // ...
                } else {
                    getLogger().severe("Your server doesn't have LuckPerms for support_prefix! Disabling..");
                    getServer().getPluginManager().disablePlugin(this);
                }
            }
        }

        try {
            String bot_token = getConfig().getString("telegram_bot_token");
            String chat_id = getConfig().getString("telegram_chat_id");
            String message = getConfig().getString("hello_message");
            methods.SendTelegramFUNCTION(bot_token, chat_id, message);
        } catch (Exception e) {
            getLogger().severe("We didn't send message about server's starting! Config.yml is bad. Disabling..");
            getServer().getPluginManager().disablePlugin(this);
        }

        TelegramReciever telegramReceiver = new TelegramReciever(this, botToken);

        // Start polling for messages every 5 second
        BukkitScheduler sheduler = getServer().getScheduler();
        sheduler.runTaskTimerAsynchronously(this, telegramReceiver::processMessages, 0L, 5 * 20L); // 0 delay, 5 seconds period

        if (getConfig().getBoolean("desc_editing_bool")) {
            Date currentDate = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = formatter.format(currentDate);
            String bot_token = getConfig().getString("telegram_bot_token");
            String chat_id = getConfig().getString("telegram_chat_id");
            try {
                methods.setChatDescription(bot_token, chat_id, getConfig().getString("on_online_desc").replace("{players_online}", String.valueOf(getServer().getOnlinePlayers().size())).replace("{players_max}", String.valueOf(getServer().getMaxPlayers())).replace("{time}", formattedDate));
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onDisable() {
        int was_players_ = getServer().getOnlinePlayers().size();
        String was_players = Integer.toString(was_players_);


        try {
            String bot_token = getConfig().getString("telegram_bot_token");
            String chat_id = getConfig().getString("telegram_chat_id");
            String message = getConfig().getString("goodbye_message").replace("{was_players}", was_players);
            methods.SendTelegramFUNCTION(bot_token, chat_id, message);
            methods.setChatDescription(bot_token, chat_id, getConfig().getString("off_desc"));
        } catch (Exception e) {
            getLogger().severe("We didn't send message about server's stopping.");
        }
        getLogger().info("Goodbye, dear server! I'll wait until you start it.");
    }
}