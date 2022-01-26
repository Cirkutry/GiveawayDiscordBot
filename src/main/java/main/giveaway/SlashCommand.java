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
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild() == null) return;

        if (event.getMember() == null) return;

        if (event.getName().equals("start")) {
            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {

                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(0x00FF00);
                errors.setDescription(jsonParsers.getLocale("message_gift_Need_Stop_Giveaway", event.getGuild().getId()));

                event.replyEmbeds(errors.build()).queue();
            } else {
                try {
                    TextChannel textChannel = null;
                    String title = null;
                    String count = null;
                    String time = null;

                    for (int i = 0; i < event.getOptions().size(); i++) {

                        if (title == null
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{1,2}[mмhчdд]")
                                && !event.getOptions().get(i).getAsString().matches("\\d{18}")
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{1,2}")
                                && event.getOptions().get(i).getAsString().matches(".{0,255}")
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{4}.[0-9]{2}.[0-9]{2}\\s[0-9]{2}:[0-9]{2}")) {
                            title = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("^[0-9]{1,2}$")) {
                            count = String.valueOf(event.getOptions().get(i).getAsLong());
                        }

                        if (event.getOptions().get(i).getAsString().matches("[0-9]{1,2}[mмhчdд]")
                                || event.getOptions().get(i).getAsString().matches("[0-9]{4}.[0-9]{2}.[0-9]{2}\\s[0-9]{2}:[0-9]{2}")) {
                            time = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("\\d{18}")) {
                            textChannel = event.getOptions().get(i).getAsGuildChannel().getGuild()
                                    .getTextChannelById(event.getOptions().get(i).getAsGuildChannel().getId());
                        }
                    }

                    GiveawayRegistry.getInstance().setGift(
                            event.getGuild().getIdLong(),
                            new Gift(event.getGuild().getIdLong(),
                                    textChannel == null ? event.getTextChannel().getIdLong() : textChannel.getIdLong(),
                                    activeGiveawayRepository,
                                    participantsRepository));

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
                                    time);

                    //Если время будет неверным. Сработает try catch
                } catch (Exception e) {
                    e.printStackTrace();
                    GiveawayRegistry.getInstance().removeGift(event.getGuild().getIdLong());

                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(0x00FF00);
                    errors.setDescription(jsonParsers.getLocale("slash_Errors", event.getGuild().getId()));

                    event.replyEmbeds(errors.build()).queue();
                }
            }

            return;
        }

        if (event.getName().equals("stop")) {
            if (!GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                EmbedBuilder notHas = new EmbedBuilder();
                notHas.setColor(0x00FF00);
                notHas.setDescription(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId()));

                event.replyEmbeds(notHas.build()).queue();
                return;
            }

            if (!event.getMember().hasPermission(event.getTextChannel(), Permission.ADMINISTRATOR)
                    && !event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {

                EmbedBuilder gift = new EmbedBuilder();
                gift.setColor(0x00FF00);
                gift.setDescription(jsonParsers.getLocale("message_gift_Not_Admin", event.getGuild().getId()));

                event.replyEmbeds(gift.build()).queue();
                return;
            }

            if (event.getOptions().isEmpty()) {
                EmbedBuilder stop = new EmbedBuilder();
                stop.setColor(0x00FF00);
                stop.setDescription(jsonParsers.getLocale("slash_Stop", event.getGuild().getId()));

                event.replyEmbeds(stop.build()).queue();

                GiveawayRegistry.getInstance()
                        .getActiveGiveaways().get(event.getGuild().getIdLong())
                        .stopGift(event.getGuild().getIdLong(),
                                GiveawayRegistry.getInstance().getCountWinners().get(event.getGuild().getIdLong()) == null ? 1 :
                                        Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners().get(event.getGuild().getIdLong()))
                        );
                return;
            }

            if (!event.getOptions().get(0).getAsString().matches("[0-9]{1,2}")) {
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(0x00FF00);
                errors.setDescription(jsonParsers.getLocale("slash_Errors", event.getGuild().getId()));

                event.replyEmbeds(errors.build()).queue();
                return;
            }

            EmbedBuilder stop = new EmbedBuilder();
            stop.setColor(0x00FF00);
            stop.setDescription(jsonParsers.getLocale("slash_Stop", event.getGuild().getId()));

            event.replyEmbeds(stop.build()).queue();

            GiveawayRegistry.getInstance()
                    .getActiveGiveaways().get(event.getGuild().getIdLong())
                    .stopGift(event.getGuild().getIdLong(), Integer.parseInt(event.getOptions().get(0).getAsString()));
            return;
        }

        //TODO: @Deprecated
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
                    .replaceAll("\\{0}", event.getOptions().get(0).getAsString()));

            event.replyEmbeds(button.build()).queue();

            Language language = new Language();
            language.setServerId(event.getGuild().getId());
            language.setLanguage(event.getOptions().get(0).getAsString());
            languageRepository.save(language);
            return;
        }

        if (event.getName().equals("list")) {

            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {

                StringBuilder stringBuilder = new StringBuilder();
                List<String> participantsList = GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong()).getListUsers();

                if (participantsList.isEmpty()) {
                    event.reply(jsonParsers.getLocale("slash_list_users_empty", event.getGuild().getId())).setEphemeral(true).queue();
                    return;
                }

                for (int i = 0; i < participantsList.size(); i++) {
                    if (stringBuilder.length() < 4000) {
                        stringBuilder.append(stringBuilder.length() == 0 ? "<@" : ", <@").append(participantsList.get(i)).append(">");
                    } else {
                        stringBuilder.append(" and others...");
                        break;
                    }
                }

                EmbedBuilder list = new EmbedBuilder();
                list.setColor(0x00FF00);
                list.setTitle(jsonParsers.getLocale("slash_list_users", event.getGuild().getId()));
                list.setDescription(stringBuilder);

                event.replyEmbeds(list.build()).queue();
            } else {
                EmbedBuilder list = new EmbedBuilder();
                list.setColor(0x00FF00);
                list.setDescription(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId()));

                event.replyEmbeds(list.build()).setEphemeral(true).queue();
            }
        }
    }
}