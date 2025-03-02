package main.core.events;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class HelpCommand {

    private final static JSONParsers jsonParsers = new JSONParsers();

    public void help(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        String helpStart = jsonParsers.getLocale("help_start", guildId);
        String helpStop = jsonParsers.getLocale("help_stop", guildId);
        String helpScheduling = jsonParsers.getLocale("help_scheduling", guildId);
        String helpCancel = jsonParsers.getLocale("help_cancel", guildId);
        String helpReroll = jsonParsers.getLocale("help_reroll", guildId);
        String helpPredefined = jsonParsers.getLocale("help_predefined", guildId);
        String helpList = jsonParsers.getLocale("help_list", guildId);
        String helpLanguage = jsonParsers.getLocale("help_language", guildId);
        String helpParticipants = jsonParsers.getLocale("help_participants", guildId);
        String helpPermissions = jsonParsers.getLocale("help_permissions", guildId);
        String helpEdit = jsonParsers.getLocale("help_edit", guildId);
        String helpEndMessage = jsonParsers.getLocale("help_end_message", guildId);

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(Color.GREEN);
        info.setTitle("Giveaway");
        info.addField("Slash Commands",
                String.format("""
                                </start:941286272390037535> - %s
                                </stop:941286272390037536> - %s
                                </scheduling:1102283573349851166> - %s
                                </cancel:1102283573349851167> - %s
                                </reroll:957624805446799452> - %s
                                </predefined:1049647289779630080> - %s
                                </list:941286272390037538> - %s
                                </settings:1204911821056905277> - %s
                                </participants:952572018077892638> - %s
                                </check-bot-permission:1009065886335914054> - %s
                                </edit:1344440007603126304> - %s
                                </endmessage:1344440007603126303> - %s
                                """,
                        helpStart,
                        helpStop,
                        helpScheduling,
                        helpCancel,
                        helpReroll,
                        helpPredefined,
                        helpList,
                        helpLanguage,
                        helpParticipants,
                        helpPermissions,
                        helpEdit,
                        helpEndMessage), false);

        event.replyEmbeds(info.build()).setEphemeral(true).queue();

    }
}