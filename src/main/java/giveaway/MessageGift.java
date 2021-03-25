package giveaway;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import startbot.Statcord;

public class MessageGift extends ListenerAdapter {

  private static final String GIFT_START = "!gift start";
  private static final String GIFT_START_TITLE = "gift start\\s.{0,255}$";
  private static final String GIFT_STOP = "!gift stop";
  private static final String GIFT_STOP_COUNT = "gift stop\\s[0-9]+";
  private static final String GIFT_COUNT = "!gift count";

  @Override
  public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    String message = event.getMessage().getContentRaw().toLowerCase().trim();
    if (message.equals("")) {
      return;
    }

    if (!event.getGuild().getSelfMember()
        .hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    String[] messageSplit = message.split(" ", 3);
    int length = message.length();
    String messageWithOutPrefix = message.substring(1, length);

    String prefix2 = GIFT_START;
    String prefix3 = GIFT_STOP;
    String prefix4 = GIFT_COUNT;

    if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
      prefix2 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift start";
      prefix3 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift stop";
      prefix4 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift count";
    }

    //TODO: Нужно всё тестировать!
    if ((message.contains("!gift start ") && (message.length() - 11) >= 256)) {
      event.getChannel().sendMessage("The title must not be longer than 255 characters!").queue();
      return;
    }

    if (message.equals(prefix2)
        || message.equals(prefix3)
        || messageWithOutPrefix.matches(GIFT_STOP_COUNT)
        || message.equals(prefix4)
        || messageWithOutPrefix.matches(GIFT_START_TITLE)) {

      if (!messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        Statcord.commandPost(message.substring(1), event.getAuthor().getId());
      }

      if (messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        Statcord.commandPost("gift start", event.getAuthor().getId());
      }

      if (message.equals(prefix2)
          || message.equals(prefix3)
          || messageWithOutPrefix.matches(GIFT_STOP_COUNT)
          || messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        long guildLongId = event.getGuild().getIdLong();

        if (!event.getMember().hasPermission(event.getChannel(), Permission.ADMINISTRATOR)
            && !event.getMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
          event.getChannel().sendMessage("You are not Admin or you can't managed messages!").queue();
          return;
        }

        if ((message.equals(prefix2) || messageWithOutPrefix.matches(GIFT_START_TITLE))
            && GiveawayRegistry.getInstance().hasGift(guildLongId)) {
          event.getChannel().sendMessage("First you need to stop Giveaway").queue();
          return;
        }

        if ((message.equals(prefix2) || messageWithOutPrefix.matches(GIFT_START_TITLE))
            && !GiveawayRegistry.getInstance().hasGift(guildLongId)) {
          GiveawayRegistry.getInstance().setGift(event.getGuild().getIdLong(), new Gift(event.getGuild()));
          if (messageSplit.length >= 3) {

            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), messageSplit[2]);
            return;
          } else {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), null);
          }
        }

        if ((message.equals(prefix3)
            || messageWithOutPrefix.matches(GIFT_STOP_COUNT))
            && GiveawayRegistry.getInstance().hasGift(guildLongId)) {

          if (messageSplit.length == 3) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .stopGift(event.getGuild(), event.getChannel(),
                    Integer.parseInt(messageSplit[messageSplit.length - 1]));
            return;
          }
          GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
              .stopGift(event.getGuild(), event.getChannel(), Integer.parseInt("1"));
          return;
        }
      }

      if (message.equals(prefix4) && event.getAuthor().getId().equals("250699265389625347")) {
        EmbedBuilder getCount = new EmbedBuilder();
        getCount.setTitle("Giveaway count");
        getCount.setColor(0x00FF00);
        getCount.setDescription("Active: `" + GiveawayRegistry.getInstance().getGiveAwayCount() + "`");
        event.getChannel().sendMessage(getCount.build()).queue();
        getCount.clear();
      }
    }
  }
}