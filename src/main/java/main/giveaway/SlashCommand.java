package main.giveaway;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.messagesevents.MessageInfoHelp;
import main.model.entity.Language;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) return;

        if (!event.isFromGuild()) {
            EmbedBuilder fromGuild = new EmbedBuilder();
            fromGuild.setColor(0x00FF00);
            fromGuild.setDescription("The bot supports `/slash commands` only in guilds!");
            event.replyEmbeds(fromGuild.build()).queue();
            return;
        }

        if (event.getMember() == null) return;

        if (event.getName().equals("start")) {
            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                event.reply(jsonParsers.getLocale("message_gift_Need_Stop_Giveaway", event.getGuild().getId())).queue();
            } else {
                try {
                    TextChannel textChannel = null;
                    String title = null;
                    String count = null;
                    String time = null;
                    Long role = null;
                    boolean isOnlyForSpecificRole = false;

                    for (int i = 0; i < event.getOptions().size(); i++) {

                        if (title == null
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{1,2}[mмhчdд]")
                                && !event.getOptions().get(i).getAsString().matches("\\d{18}")
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{1,2}")
                                && event.getOptions().get(i).getAsString().matches(".{0,255}")
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{4}.[0-9]{2}.[0-9]{2}\\s[0-9]{2}:[0-9]{2}")
                                && !event.getOptions().get(i).getAsString().equals("yes")) {
                            title = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("[0-9]{1,2}")) {
                            count = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("[0-9]{1,2}[mмhчdд]")
                                || event.getOptions().get(i).getAsString().matches("[0-9]{4}.[0-9]{2}.[0-9]{2}\\s[0-9]{2}:[0-9]{2}")) {
                            time = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getType().equals(OptionType.CHANNEL)) {
                            textChannel = event.getOptions().get(i).getAsGuildChannel().getGuild()
                                    .getTextChannelById(event.getOptions().get(i).getAsGuildChannel().getId());
                        }

                        if (event.getOptions().get(i).getType().equals(OptionType.ROLE)) {
                            role = event.getOptions().get(i).getAsRole().getIdLong();
                        }

                        if (event.getOptions().get(i).getAsString().equals("yes")) {
                            isOnlyForSpecificRole = true;
                        }

                    }

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setColor(0xFF0000);

                    if (role == null && isOnlyForSpecificRole) {
                        embedBuilder.setDescription(jsonParsers.getLocale("slash_error_only_for_this_role", event.getGuild().getId()) + role + "`");
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    if (role != null && role == event.getGuild().getIdLong() && isOnlyForSpecificRole) {
                        embedBuilder.setDescription(jsonParsers.getLocale("slash_error_role_can_not_be_everyone", event.getGuild().getId()));
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    GiveawayRegistry.getInstance().setGift(
                            event.getGuild().getIdLong(),
                            new Gift(event.getGuild().getIdLong(),
                                    textChannel == null ? event.getTextChannel().getIdLong() : textChannel.getIdLong(), activeGiveawayRepository, participantsRepository));

                    if (!event.getGuild().getSelfMember()
                            .hasPermission(textChannel == null ? event.getTextChannel() : textChannel, Permission.MESSAGE_SEND) ||
                            !event.getGuild().getSelfMember()
                                    .hasPermission(textChannel == null ? event.getTextChannel() : textChannel, Permission.MESSAGE_EMBED_LINKS)) {
                        return;
                    }

                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                    event.getGuild(),
                                    textChannel == null ? event.getTextChannel() : textChannel,
                                    title,
                                    count,
                                    time,
                                    role,
                                    !isOnlyForSpecificRole ? null : isOnlyForSpecificRole);

                    //Если время будет неверным. Сработает try catch
                } catch (Exception e) {
                    e.printStackTrace();
                    GiveawayRegistry.getInstance().removeGift(event.getGuild().getIdLong());
                    event.reply(jsonParsers.getLocale("slash_Errors", event.getGuild().getId())).queue();
                }
            }

            return;
        }

        if (event.getName().equals("stop")) {
            if (!GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                event.reply(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId())).queue();
                return;
            }

            if (!event.getMember().hasPermission(event.getTextChannel(), Permission.ADMINISTRATOR)
                    && !event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                event.reply(jsonParsers.getLocale("message_gift_Not_Admin", event.getGuild().getId())).queue();
                return;
            }

            if (event.getOptions().isEmpty()) {
                event.reply(jsonParsers.getLocale("slash_Stop", event.getGuild().getId())).queue();
                GiveawayRegistry.getInstance()
                        .getActiveGiveaways().get(event.getGuild().getIdLong())
                        .stopGift(event.getGuild().getIdLong(),
                                GiveawayRegistry.getInstance().getCountWinners().get(event.getGuild().getIdLong()) == null ? 1 :
                                        Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners().get(event.getGuild().getIdLong()))
                        );
                return;
            }

            if (!event.getOptions().get(0).getAsString().matches("[0-9]{1,2}")) {
                event.reply(jsonParsers.getLocale("slash_Errors", event.getGuild().getId())).queue();
                return;
            }

            event.reply(jsonParsers.getLocale("slash_Stop", event.getGuild().getId())).queue();
            GiveawayRegistry.getInstance()
                    .getActiveGiveaways().get(event.getGuild().getIdLong())
                    .stopGift(event.getGuild().getIdLong(), Integer.parseInt(event.getOptions().get(0).getAsString()));
            return;
        }

        if (event.getName().equals("help")) {

            String p = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!" :
                    BotStartConfig.getMapPrefix().get(event.getGuild().getId());

            new MessageInfoHelp().buildMessage(
                    p,
                    event.getTextChannel(),
                    event.getUser().getAvatarUrl(),
                    event.getGuild().getId(),
                    event.getUser().getName(), event);
            return;
        }

        //0 - bot
        if (event.getName().equals("language")) {

            if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {

                EmbedBuilder notAdmin = new EmbedBuilder();
                notAdmin.setColor(0x00FF00);
                notAdmin.setDescription(jsonParsers.getLocale("language_change_Not_Admin", event.getGuild().getId())
                        .replaceAll("\\{0}", event.getOptions().get(0).getAsString()));

                event.replyEmbeds(notAdmin.build()).setEphemeral(true).queue();
                return;
            }

            BotStartConfig.getMapLanguages().put(event.getGuild().getId(), event.getOptions().get(0).getAsString());

            EmbedBuilder button = new EmbedBuilder();
            button.setColor(0x00FF00);
            button.setDescription(jsonParsers.getLocale("button_Language", event.getGuild().getId())
                    .replaceAll("\\{0}", event.getOptions().get(0).getAsString().equals("rus")
                            ? "Русский"
                            : "English"));

            event.replyEmbeds(button.build()).setEphemeral(true).queue();

            Language language = new Language();
            language.setServerId(event.getGuild().getId());
            language.setLanguage(event.getOptions().get(0).getAsString());
            languageRepository.save(language);
            return;
        }

        if (event.getName().equals("list")) {

            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {

                StringBuilder stringBuilder = new StringBuilder();
                List<String> participantsList = new ArrayList<>(GiveawayRegistry.getInstance()
                        .getActiveGiveaways().get(event.getGuild().getIdLong())
                        .getListUsers());

                if (participantsList.isEmpty()) {
                    EmbedBuilder list = new EmbedBuilder();
                    list.setColor(Color.GREEN);
                    list.setDescription(jsonParsers.getLocale("slash_list_users_empty", event.getGuild().getId()));
                    event.replyEmbeds(list.build()).setEphemeral(true).queue();
                    return;
                }

                for (int i = 0; i < participantsList.size(); i++) {
                    stringBuilder.append(stringBuilder.length() == 0 ? "<@" : ", <@").append(participantsList.get(i)).append(">");
                }

                EmbedBuilder list = new EmbedBuilder();
                list.setColor(0x00FF00);
                list.setTitle(jsonParsers.getLocale("slash_list_users", event.getGuild().getId()));
                list.setDescription(stringBuilder);

                event.replyEmbeds(list.build()).queue();
            } else {
                EmbedBuilder noGiveaway = new EmbedBuilder();
                noGiveaway.setColor(Color.GREEN);
                noGiveaway.setDescription(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId()));
                event.replyEmbeds(noGiveaway.build()).setEphemeral(true).queue();
            }
            return;
        }

        if (event.getName().equals("patreon")) {
            EmbedBuilder patreon = new EmbedBuilder();
            patreon.setColor(Color.YELLOW);
            patreon.setTitle("Patreon", "https://www.patreon.com/ghbots");
            patreon.setDescription("If you want to finacially support my work and motivate me to keep adding more\n" +
                    "features, put more effort and time into this and other bots, click [here](https://www.patreon.com/ghbots)");
            event.replyEmbeds(patreon.build()).setEphemeral(true).queue();
        }
    }
}