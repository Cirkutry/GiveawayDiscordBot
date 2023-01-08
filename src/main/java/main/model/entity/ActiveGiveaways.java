package main.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "active_giveaways")
public class ActiveGiveaways {

    @Id
    @Column(name = "guild_long_id", nullable = false)
    private Long guildLongId;

    @Column(name = "channel_id_long", nullable = false)
    private Long channelIdLong;

    @Column(name = "count_winners")
    private int countWinners;

    @Column(name = "date_end_giveaway")
    private Timestamp dateEndGiveaway;

    @Column(name = "giveaway_title")
    private String giveawayTitle;

    @Column(name = "message_id_long", nullable = false)
    private Long messageIdLong;

    @Column(name = "is_for_specific_role")
    private Boolean isForSpecificRole;

    @Column(name = "role_id_long")
    private Long roleIdLong;

    @Column(name = "id_user_who_create_giveaway", nullable = false)
    private Long idUserWhoCreateGiveaway;

    @Column(name = "url_image")
    private String urlImage;

}