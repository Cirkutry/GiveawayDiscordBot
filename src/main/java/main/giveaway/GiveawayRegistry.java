package main.giveaway;

import lombok.Getter;
import lombok.Setter;
import main.jsonparser.JSONParsers;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class GiveawayRegistry {

    //Возвращает GiveawayData по long id
    private static final Map<Long, GiveawayData> giveawayDataMap = new HashMap<>();
    private static final JSONParsers jsonParsers = new JSONParsers();
    private static volatile GiveawayRegistry giveawayRegistry;

    private GiveawayRegistry() {
    }

    @Getter
    @Setter
    public static class GiveawayData {

        private Gift gift;
        private String idMessagesWithGiveawayButtons;
        private String channelId;
        private String messageId;
        private String countWinners;
        private String title;
        private Timestamp endGiveawayDate;
        private Long roleId;
        private Boolean isForSpecificRole;
        private String urlImage;
        private String idUserWhoCreateGiveaway;

        public GiveawayData(Gift gift) {
            this.gift = gift;
        }

        private GiveawayData() {
        }

    }

    public static GiveawayRegistry getInstance() {
        if (giveawayRegistry == null) {
            synchronized (GiveawayRegistry.class) {
                if (giveawayRegistry == null) {
                    giveawayRegistry = new GiveawayRegistry();
                }
            }
        }
        return giveawayRegistry;
    }

    public String getIdUserWhoCreateGiveaway(long guildId) {
        return giveawayDataMap.get(guildId).getIdUserWhoCreateGiveaway();
    }

    public void putIdUserWhoCreateGiveaway(long guildId, String userId) {
        giveawayDataMap.get(guildId).setIdUserWhoCreateGiveaway(userId);
    }

    public String getUrlImage(long guildId) {
        return giveawayDataMap.get(guildId).getUrlImage();
    }

    public void putUrlImage(long guildId, String urlImage) {
        giveawayDataMap.get(guildId).setUrlImage(urlImage);
    }

    public Boolean getIsForSpecificRole(long guildId) {
        return giveawayDataMap.get(guildId).getIsForSpecificRole();
    }

    public void putIsForSpecificRole(long guildId, Boolean is_for_specific_role) {
        giveawayDataMap.get(guildId).setIsForSpecificRole(is_for_specific_role);
    }

    public Long getRoleId(long guildId) {
        return giveawayDataMap.get(guildId).getRoleId();
    }

    public void putRoleId(long guildId, Long roleIdLong) {
        giveawayDataMap.get(guildId).setRoleId(roleIdLong);
    }

    public Gift getGift(long guildId) {
        return giveawayDataMap.get(guildId).getGift();
    }

    public boolean hasGift(long guildId) {
        return giveawayDataMap.containsKey(guildId);
    }

    public String getIdMessagesWithGiveawayButtons(long guildId) {
        return giveawayDataMap.get(guildId).getIdMessagesWithGiveawayButtons();
    }

    public Timestamp getEndGiveawayDate(long guildId) {
        return giveawayDataMap.get(guildId).getEndGiveawayDate();
    }

    public String getCountWinners(long guildId) {
        return giveawayDataMap.get(guildId).getCountWinners();
    }

    public String getMessageId(long guildId) {
        return giveawayDataMap.get(guildId).getMessageId();
    }

    public String getTitle(long guildId) {
        return giveawayDataMap.get(guildId).getTitle();
    }

    public void putEndGiveawayDate(long guildId, Timestamp timestamp) {
        giveawayDataMap.get(guildId).setEndGiveawayDate(timestamp);
    }

    public void putMessageId(long guildId, String messageId) {
        giveawayDataMap.get(guildId).setMessageId(messageId);
    }

    public void putChannelId(long guildId, String channelId) {
        giveawayDataMap.get(guildId).setChannelId(channelId);
    }

    public void putIdMessagesWithGiveawayButtons(long guildId, String idMessageWithButton) {
        giveawayDataMap.get(guildId).setIdMessagesWithGiveawayButtons(idMessageWithButton);
    }

    public void putTitle(long guildId, String title) {
        giveawayDataMap.get(guildId).setTitle(title);
    }

    public void putGift(long guildId, Gift gift) {
        giveawayDataMap.put(guildId, new GiveawayData(gift));
    }

    public void putCountWinners(long guildId, String countWinners) {
        giveawayDataMap.get(guildId).setCountWinners(countWinners);
    }

    public void removeGift(long guildId) {
        giveawayDataMap.remove(guildId);
    }

    public void removeGuildFromGiveaway(long guildId) {
        giveawayDataMap.remove(guildId);
    }

    //TODO: Удалять из Базы данных нужно ещё
    public String removeGiftExceptions(long guildId) {
        giveawayDataMap.remove(guildId);
        return jsonParsers.getLocale("giveaway_registry_Error", String.valueOf(guildId));
    }
}