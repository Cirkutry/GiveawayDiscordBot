package main.giveaway;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import main.config.BotStart;
import main.jsonparser.JSONParsers;
import main.model.entity.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.aventrix.jnanoid.jnanoid.NanoIdUtils.DEFAULT_NUMBER_GENERATOR;

public class GiveawayUtils {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    public static final String ISO_TIME_REGEX = "^\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2}$"; //2021.11.16 16:00
    public static final String TIME_REGEX = "(\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2})|(\\d{1,2}[smhdсмдч]|\\s)+";
    public static final JSONParsers jsonParsers = new JSONParsers();
    public static final char[] DEFAULT_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    public static boolean checkPermissions(GuildChannel guildChannel, Member selfMember) {
        return selfMember.hasPermission(guildChannel,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_HISTORY,
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND);
    }

    public static String getSalt(int size) {
        return NanoIdUtils.randomNanoId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, size);
    }

    public static long getSeconds(String time) {
        String[] splitTime = time.split("\\s+");
        long seconds = 0;
        for (String s : splitTime) {
            long localTime = Long.parseLong(s.substring(0, s.length() - 1));
            String symbol = s.substring(s.length() - 1);
            switch (symbol) {
                case "m", "м" -> seconds += localTime * 60;
                case "h", "ч" -> seconds += localTime * 3600;
                case "d", "д" -> seconds += localTime * 86400;
                case "s", "с" -> seconds += localTime;
            }
        }
        return seconds;
    }

    public static Timestamp timeProcessor(String time) {
        if (time == null) return null;
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
        long toEpochSecond = localDateTime.toEpochSecond(offset);
        return new Timestamp(toEpochSecond * 1000);
    }

    public static Color getUserColor(long guildId) {
        Settings settings = BotStart.getMapLanguages().get(guildId);
        if (settings != null) {
            String colorHex = settings.getColorHex();
            if (colorHex != null) {
                return Color.decode(colorHex);
            } else {
                return Color.GREEN;
            }
        } else {
            return Color.GREEN;
        }
    }

    @Nullable
    public static String getGuildText(long guildId) {
        Settings settings = BotStart.getMapLanguages().get(guildId);
        if (settings != null) {
            return settings.getText();
        } else {
            return null;
        }
    }

    public static boolean isISOTimeCorrect(@NotNull String time) {
        return time.matches(GiveawayUtils.ISO_TIME_REGEX);
    }

    public static boolean isTimeCorrect(@NotNull String time) {
        return time.matches(GiveawayUtils.TIME_REGEX);
    }

    public static boolean isTimeBefore(String time) {
        LocalDateTime localDateTime = LocalDateTime.parse(time, FORMATTER);
        LocalDateTime now = Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime();
        return localDateTime.isBefore(now);
    }

    public static String setEndingWord(int num, long guildId) {
        String language = "eng";

        Settings settings = BotStart.getMapLanguages().get(guildId);
        if (settings != null) {
            language = settings.getLanguage();
        }
        return switch (num % 10) {
            case 1 -> language.equals("eng") ? "Winner" : "Победитель";
            case 2, 3, 4 -> language.equals("eng") ? "Winners" : "Победителя";
            default -> language.equals("eng") ? "Winners" : "Победителей";
        };
    }

    public static String getDiscordUrlMessage(final long guildIdLong, final long textChannelId, final long messageIdLong) {
        return String.format("https://discord.com/channels/%s/%s/%s", guildIdLong, textChannelId, messageIdLong);
    }
}
