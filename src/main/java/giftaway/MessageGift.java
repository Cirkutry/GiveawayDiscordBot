package giftaway;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import startbot.Statcord;

public class MessageGift extends ListenerAdapter {

  private static final String GIFT_START = "!gift start";
  private static final String GIFT_START_TITLE = "gift start\\s.{10,255}$";
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
        long guild = event.getGuild().getIdLong();
        Gift gift;
        gift = new Gift();

        if (!event.getMember().hasPermission(event.getChannel(), Permission.ADMINISTRATOR)) {
          event.getChannel().sendMessage("You are not Admin").queue();
          return;
        }

        if ((message.equals(prefix2)
            || messageWithOutPrefix.matches(GIFT_START_TITLE))
            && !gift.hasGift(guild)) {

          gift.setGift(guild, new Gift(event.getGuild()));
          if (messageSplit.length >= 3) {
            gift.startGift(event.getGuild(), event.getChannel(), messageSplit[2]);
            return;
          } else {
            gift.startGift(event.getGuild(), event.getChannel(), null);
          }
        }

        if ((message.equals(prefix3)
            || messageWithOutPrefix.matches(GIFT_STOP_COUNT))
            && gift.hasGift(guild)) {
          gift = gift.getGift(event.getGuild().getIdLong());

          if (messageSplit.length == 3) {
            gift.stopGift(event.getGuild(), event.getChannel(),
                Integer.parseInt(messageSplit[messageSplit.length - 1]));
            return;
          }
          gift.stopGift(event.getGuild(), event.getChannel(), Integer.parseInt("1"));
          return;
        }
      }

      if (message.equals(prefix4) && event.getAuthor().getId().equals("250699265389625347")) {
        Gift gift;
        gift = new Gift();
        EmbedBuilder getCount = new EmbedBuilder();
        getCount.setTitle("Giveaway count");
        getCount.setColor(0x00FF00);
        getCount.setDescription("Active: `" + gift.getGiveAwayCount() + "`");
        event.getChannel().sendMessage(getCount.build()).queue();
        getCount.clear();
      }
    }
  }
}