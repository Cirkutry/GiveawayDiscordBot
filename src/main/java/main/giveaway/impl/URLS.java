package main.giveaway.impl;

public interface URLS {

    static String setGiveawayMessageUrl(final long guildIdLong, final long textChannelId, final long messageIdLong) {
        return String.format("https://discord.com/channels/%s/%s/%s", guildIdLong, textChannelId, messageIdLong);
    }
}
