package ru.florestdev.florestTelegramPRO;

import ru.florestdev.florestDiscordPro.FlorestDiscordPro;

public class TGToDiscord {
    public static  FlorestTelegramPRO telegramPRO;
    public static FlorestDiscordPro discordPro;

    public static String channel_id;

    public TGToDiscord(FlorestTelegramPRO telegramPRO, FlorestDiscordPro discordPro) {
        this.telegramPRO = telegramPRO;
        this.discordPro = discordPro;

        this.channel_id = discordPro.getConfig().getString("discord_channel_id");
    }

    public void sendMessage(String username, String message) {
        discordPro.getMethods().sendDiscordMessage(channel_id, telegramPRO.getConfig().getString("tg_discord_message").replace("{telegram_name}", username).replace("{telegram_message}", message));
    }

}
