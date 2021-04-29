package giveaway;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.Statcord;

public class Reactions extends ListenerAdapter {

  private final JSONParsers jsonParsers = new JSONParsers();
  public static final String emojiPresent = "\uD83C\uDF81";

  @Override
  public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {

    if (event.getUser().isBot()) {
      return;
    }

    if (GiveawayRegistry.getInstance()
        .getIdMessagesWithGiveawayEmoji().get(event.getGuild().getIdLong()) == null) {
      return;
    }

    if (!event.getGuild().getSelfMember()
        .hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
      event.getChannel().sendMessage(jsonParsers
          .getLocale("reactions_bot_dont_have_permissions", event.getGuild().getId())
          .replaceAll("\\{0}", event.getGuild().getSelfMember().getUser().getName()))
          .queue();
      return;
    }
    try {
      //Что делает этот код...:
      if (!event.getReactionEmote().isEmoji()) {
        event.getReaction().removeReaction(event.getUser()).queue();
        return;
      }
      //

      String emoji = event.getReactionEmote().getEmoji();

      if (!emoji.equals(emojiPresent)
          && GiveawayRegistry.getInstance()
          .getIdMessagesWithGiveawayEmoji().get(event.getGuild().getIdLong())
          != null) {
        event.getReaction().removeReaction(event.getUser()).queue();
      }

      long guild = event.getGuild().getIdLong();

      if (emoji.equals(emojiPresent)
          && GiveawayRegistry.getInstance()
          .getIdMessagesWithGiveawayEmoji().get(event.getGuild().getIdLong())
          != null
          && GiveawayRegistry.getInstance().hasGift(guild)) {

        if (GiveawayRegistry.getInstance()
            .getActiveGiveaways().get(event.getGuild().getIdLong())
            .getListUsersHash(event.getUser().getId())
            == null) {

          GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
              .addUserToPoll(event.getMember().getUser(), event.getGuild(), event.getChannel());

          Statcord.commandPost("gift", event.getUser().getId());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}