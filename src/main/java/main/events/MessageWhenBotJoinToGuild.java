package main.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.giveaway.buttons.ReactionsButton;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        try {
            EmbedBuilder welcome = new EmbedBuilder();
            welcome.setColor(Color.GREEN);
            welcome.addField("Bot Language", "Use: </language:941286272390037534>", false);
            welcome.addField("Giveaway", "Thanks for adding " + "**" + "Giveaway" + "** " + "bot to " + event.getGuild().getName() + "!\n", false);
            welcome.addField("Create Giveaway", "Use: </start:941286272390037535>", false);
            welcome.addField("Create predefined Giveaway", "Use: </predefined:1049647289779630080>", false);
            welcome.addField("Reroll Winner", "Use: </reroll:957624805446799452>", false);
            welcome.addField("Stop Giveaway manually", "Use: </stop:941286272390037536>", false);
            welcome.addField("List of commands", "Use: </help:941286272390037537>", false);
            welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
            welcome.addField("Information", "Our bot supports recovery of any Giveaway, upon request in support. " +
                    "Also, the bot automatically checks the lists of participants, even if the bot is turned off or there are problems in recording while working, " +
                    "it will automatically restore everything. This gives a 100% guarantee that each participant will be recorded.", false);

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
            buttons.add(Button.link("https://patreon.com/ghbots", "Patreon"));

            if (BotStartConfig.getMapLanguages().get(event.getGuild().getId()) != null) {
                if (BotStartConfig.getMapLanguages().get(event.getGuild().getId()).equals("eng")) {
                    buttons.add(Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.CHANGE_LANGUAGE, "Сменить язык ")
                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                } else {
                    buttons.add(Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.CHANGE_LANGUAGE, "Change language ")
                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                }
            } else {
                buttons.add(Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.CHANGE_LANGUAGE, "Сменить язык ")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            }

            DefaultGuildChannelUnion defaultChannel = event.getGuild().getDefaultChannel();

            if (defaultChannel != null) {
                if (defaultChannel.getType() == ChannelType.TEXT) {
                    TextChannel textChannel = defaultChannel.asTextChannel();
                    if (event.getGuild().getSelfMember().hasPermission(textChannel,
                            Permission.MESSAGE_SEND,
                            Permission.VIEW_CHANNEL,
                            Permission.MESSAGE_EMBED_LINKS)) {
                        defaultChannel
                                .asTextChannel()
                                .sendMessageEmbeds(welcome.build())
                                .setActionRow(buttons)
                                .queue();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Скорее всего нет `DefaultChannel`!");
            e.printStackTrace();
        }
    }

    //TODO: Сделать в таблице ON DELETE CASCADE
    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            languageRepository.deleteLanguage(event.getGuild().getId());
            activeGiveawayRepository.deleteActiveGiveaways(event.getGuild().getIdLong());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}