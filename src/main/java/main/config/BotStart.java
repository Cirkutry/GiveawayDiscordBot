package main.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.ParserClass;
import main.model.entity.Scheduling;
import main.model.entity.Settings;
import main.model.repository.SchedulingRepository;
import main.model.repository.SettingsRepository;
import main.service.GiveawayUpdateListUser;
import main.service.ScheduleStartService;
import main.service.SlashService;
import main.service.UploadGiveawaysService;
import main.threads.StopGiveawayHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class BotStart {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotStart.class.getName());

    public static final String activity = "/start | ";
    //String - guildLongId
    private static final ConcurrentMap<Long, Settings> mapLanguages = new ConcurrentHashMap<>();

    @Getter
    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //REPOSITORY
    private final UpdateController updateController;
    private final SchedulingRepository schedulingRepository;
    private final SettingsRepository settingsRepository;

    private final GiveawayUpdateListUser updateGiveawayByGuild;

    //Service
    private final SlashService slashService;
    private final ScheduleStartService scheduleStartService;
    private final UploadGiveawaysService uploadGiveawaysService;

    @Autowired
    public BotStart(UpdateController updateController,
                    SchedulingRepository schedulingRepository,
                    SettingsRepository settingsRepository,
                    GiveawayUpdateListUser updateGiveawayByGuild,
                    SlashService slashService,
                    ScheduleStartService scheduleStartService,
                    UploadGiveawaysService uploadGiveawaysService) {
        this.updateController = updateController;
        this.schedulingRepository = schedulingRepository;
        this.settingsRepository = settingsRepository;
        this.updateGiveawayByGuild = updateGiveawayByGuild;
        this.slashService = slashService;
        this.scheduleStartService = scheduleStartService;
        this.uploadGiveawaysService = uploadGiveawaysService;
    }

    @PostConstruct
    public void startBot() {
        try {
            CoreBot coreBot = new CoreBot(updateController);
            coreBot.init();

            //Загружаем GiveawayRegistry
            GiveawayRegistry.getInstance();
            //Устанавливаем языки
            setLanguages();
            getLocalizationFromDB();
            getSchedulingFromDB();

            List<GatewayIntent> intents = Arrays.asList(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGE_TYPING);

            jdaBuilder.disableCache(
                    CacheFlag.ACTIVITY,
                    CacheFlag.VOICE_STATE,
                    CacheFlag.EMOJI,
                    CacheFlag.STICKER,
                    CacheFlag.CLIENT_STATUS,
                    CacheFlag.MEMBER_OVERRIDES,
                    CacheFlag.ROLE_TAGS,
                    CacheFlag.FORUM_TAGS,
                    CacheFlag.ONLINE_STATUS,
                    CacheFlag.SCHEDULED_EVENTS
            );

            jdaBuilder.enableIntents(intents);
            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing("Starting..."));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new CoreBot(updateController));

            jda = jdaBuilder.build();
            jda.awaitReady();

            //Получаем Giveaway и пользователей. Устанавливаем данные
            uploadGiveawaysService.uploadGiveaways(updateController);

            //Обновить команды
            slashService.updateSlash(jda);

            System.out.println("DevMode: " + Config.isIsDev() + " Time Build: " + "20:22");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void getSchedulingFromDB() {
        try {
            List<Scheduling> schedulingList = schedulingRepository.findAll();
            GiveawayRegistry instance = GiveawayRegistry.getInstance();

            for (Scheduling scheduling : schedulingList) {
                String idSalt = scheduling.getIdSalt();
                instance.putScheduling(idSalt, scheduling);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

<<<<<<< HEAD
    private void updateSlashCommands() {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            //Get participants
            List<OptionData> participants = new ArrayList<>();
            participants.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Stop
            List<OptionData> optionsStop = new ArrayList<>();
            optionsStop.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to value set at start of Giveaway")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию установленное в Giveaway"));

            optionsStop.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Set language
            List<OptionData> optionsSettings = new ArrayList<>();
            optionsSettings.add(new OptionData(STRING, "language", "Set the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "язык")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота"));

            optionsSettings.add(new OptionData(STRING, "color", "Set the embed color. Example: #ff00ff")
                    .setName("color")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "цвет")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить цвет embed. Пример использования: #ff00ff"));

            //Scheduling Giveaway
            List<OptionData> optionsScheduling = new ArrayList<>();

            optionsScheduling.add(new OptionData(STRING, "start-time", "Set start time in UTC ±0 form. Example: 2023.04.29 17:00")
                    .setName("start-time")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "время-начала")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить время начала в формате UTC ±0. Пример использования: 2023.04.29 17:00"));

            optionsScheduling.add(new OptionData(CHANNEL, "channel", "Установить канал для Giveaway. По умолчанию канал в котором была запущена команда")
                    .setName("text-channel")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текстовый-канал")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "По умолчанию текстовый канал, в котором была выполнена команда."));

            optionsScheduling.add(new OptionData(STRING, "end-time", "Set end time in UTC ±0 form. Example: 2023.04.29 17:00")
                    .setName("end-time")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "время-окончания")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Укажите время окончания в формате UTC ±0. Пример использования: 2023.04.29 17:00"));

            optionsScheduling.add(new OptionData(STRING, "title", "Set title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsScheduling.add(new OptionData(ROLE, "mention", "Mention a specific @Role")
                    .setName("select")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "выбрать")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsScheduling.add(new OptionData(STRING, "role", "Set whether Giveaway is only for the role set in previous parameter")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен для определенной роли? Установите роль в предыдущем выборе"));

            optionsScheduling.add(new OptionData(ATTACHMENT, "image", "Set image used in Giveaway embed")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is lesser than this number")
                    .setName("min-participants")
                    .setMinValue(2)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            //Start Giveaway
            List<OptionData> optionsStart = new ArrayList<>();
            optionsStart.add(new OptionData(STRING, "title", "Set title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsStart.add(new OptionData(STRING, "duration", "Set duration. Example: 1h, 1d, 20m, 1h 30m 15s, 2022.08.18 13:48 (UTC ±0)")
                    .setName("duration")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "продолжительность")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить продолжительность. Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. UTC ±0"));

            optionsStart.add(new OptionData(ROLE, "mention", "Mention a specific @Role")
                    .setName("mention")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "упомянуть")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsStart.add(new OptionData(STRING, "role", "Set whether Giveaway is only for the role set in previous parameter")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway только для определенной роли? Установите роль в предыдущем выборе"));

            optionsStart.add(new OptionData(ATTACHMENT, "image", "Set image used in Giveaway embed")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min-participants")
                    .setMinValue(2)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            List<OptionData> predefined = new ArrayList<>();

            predefined.add(new OptionData(STRING, "title", "Set title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway")
                    .setRequired(true));

            predefined.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей")
                    .setRequired(true));

            predefined.add(new OptionData(ROLE, "role", "Set @Role from which all participants are selected from")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите @Роль, из которой будут выбраны все участники")
                    .setRequired(true));

            //endmessage
            List<OptionData> endMessageDate = new ArrayList<>();

            endMessageDate.add(new OptionData(STRING, "text", "Set text. Must contain @winner to properly parse")
                    .setName("text")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текст")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Задать текст. Должен содержать @winner для правильного разбора"));

            //giveaway-edit
            List<OptionData> giveawayEditData = new ArrayList<>();

            giveawayEditData.add(new OptionData(STRING, "duration", "Set duration. Example: 1h, 1d, 20m, 1h 30m 15s, 2022.08.18 13:48 (UTC ±0)")
                    .setName("duration")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "продолжительность")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            giveawayEditData.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей."));

            giveawayEditData.add(new OptionData(STRING, "title", "Set title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            giveawayEditData.add(new OptionData(ATTACHMENT, "image", "Set image used in Giveaway embed")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            giveawayEditData.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min-participants")
                    .setMinValue(1)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            giveawayEditData.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            List<OptionData> reroll = new ArrayList<>();
            reroll.add(new OptionData(STRING, "giveaway-id", "Giveaway ID or message ID with user mentions")
                    .setName("giveaway-id")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID или message ID с упоминаниями пользователей"));

            reroll.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей."));

            List<OptionData> botPermissions = new ArrayList<>();
            botPermissions.add(new OptionData(CHANNEL, "text-channel", "Check bot permissions in a specific channel")
                    .setName("text-channel")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текстовой-канал")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений определенного канала"));

            List<OptionData> cancelData = new ArrayList<>();
            cancelData.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            /*
             * Commands
             */

            CommandData checkCommand = Commands.slash("check-bot-permission", "Check bot permissions")
                    .addOptions(botPermissions)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений бота");

            CommandData settingsCommand = Commands.slash("settings", "Change bot settings")
                    .addOptions(optionsSettings)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройки бота")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData startCommand = Commands.slash("start", "Create Giveaway")
                    .addOptions(optionsStart)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание Giveaway");

            CommandData schedulingCommand = Commands.slash("scheduling", "Create scheduled Giveaway")
                    .addOptions(optionsScheduling)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание запланированного Giveaway");

            CommandData stopCommand = Commands.slash("stop", "Stop the Giveaway and announce winners")
                    .addOptions(optionsStop)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить Giveaway и определить победителей");

            CommandData helpCommand = Commands.slash("help", "Bot commands")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота");

            CommandData participantsCommand = Commands.slash("participants", "Get file with all participants")
                    .addOptions(participants)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить файл со всеми участниками");

            CommandData endMessage = Commands.slash("endmessage", "Set message announcing the winners")
                    .addOptions(endMessageDate)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройте сообщение с объявлением победителей, заменяя его на указанный текст.");

            CommandData rerollCommand = Commands.slash("reroll", "Reroll winners")
                    .addOptions(reroll)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Перевыбрать победителей");

            CommandData giveawayEdit = Commands.slash("edit", "Change Giveaway settings")
                    .addOptions(giveawayEditData)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Изменить настройки Giveaway")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData predefinedCommand = Commands.slash("predefined", "Gather participants and immediately hold a drawing for a specific @Role")
                    .addOptions(predefined)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Собрать участников и сразу провести розыгрыш для определенной @Роли")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData listCommand = Commands.slash("list", "List of all active and scheduled Giveaways")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Список всех активных и запланированных Giveaway")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData cancelCommand = Commands.slash("cancel", "Cancel Giveaway")
                    .addOptions(cancelData)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Отменить Giveaway");

            commands.addCommands(
                            listCommand,
                            checkCommand,
                            endMessage,
                            settingsCommand,
                            startCommand,
                            schedulingCommand,
                            stopCommand,
                            helpCommand,
                            participantsCommand,
                            rerollCommand,
                            giveawayEdit,
                            predefinedCommand,
                            cancelCommand)
                    .queue();

            System.out.println("Готово");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

=======
>>>>>>> main
    @Scheduled(fixedDelay = 120, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void updateActivity() {
        if (!Config.isIsDev()) {
            int serverCount = BotStart.jda.getGuilds().size();
            BotStart.jda.getPresence().setActivity(Activity.playing(BotStart.activity + serverCount + " guilds"));
        } else {
            BotStart.jda.getPresence().setActivity(Activity.playing("Develop"));
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void scheduleStartGiveaway() {
        try {
            scheduleStartService.scheduleStart(updateController, jda);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (String listLanguage : listLanguages) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguage + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = new JSONObject(new JSONTokener(reader));

                for (String o : jsonObject.keySet()) {
                    if (listLanguage.equals("rus")) {
                        ParserClass.russian.put(o, String.valueOf(jsonObject.get(o)));
                    } else {
                        ParserClass.english.put(o, String.valueOf(jsonObject.get(o)));
                    }
                }
                reader.close();
                inputStream.close();
                reader.close();
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 2, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void stopGiveawayTimer() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayDataList = new LinkedList<>(instance.getAllGiveaway());
        StopGiveawayHandler stopGiveawayHandler = new StopGiveawayHandler();
        for (Giveaway giveaway : giveawayDataList) {
            try {
                stopGiveawayHandler.handleGiveaway(giveaway);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Scheduled(fixedDelay = 150, initialDelay = 25, timeUnit = TimeUnit.SECONDS)
    public void updateUserList() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayDataList = new LinkedList<>(instance.getAllGiveaway());
        for (Giveaway giveaway : giveawayDataList) {
            try {
                updateGiveawayByGuild.updateGiveawayByGuild(giveaway);
                Thread.sleep(2000L);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void getLocalizationFromDB() {
        try {
            List<Settings> settingsList = settingsRepository.findAll();
            for (Settings settings : settingsList) {
                mapLanguages.put(settings.getServerId(), settings);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Map<Long, Settings> getMapLanguages() {
        return mapLanguages;
    }
}
