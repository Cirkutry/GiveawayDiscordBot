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
@Table(name = "scheduling")
public class Scheduling {

    @Id
    @Column(name = "guild_long_id", nullable = false)
    private Long guildLongId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "count_winners")
    private int countWinners;

    @Column(name = "create_giveaway", nullable = false)
    private Timestamp dateCreateGiveaway;

    @Column(name = "date_end_giveaway")
    private Timestamp dateEndGiveaway;

    @Column(name = "giveaway_title")
    private String giveawayTitle;

    @Column(name = "is_for_specific_role")
    private Boolean isForSpecificRole;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "created_user_id", nullable = false)
    private Long createdUserId;

    @Column(name = "url_image")
    private String urlImage;

    @Column(name = "min_participants")
    private Integer minParticipants;
}