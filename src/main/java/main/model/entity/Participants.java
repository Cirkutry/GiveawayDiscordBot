package main.model.entity;


import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "participants")
public class Participants {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "guild_long_id")
    private ActiveGiveaways guildLongId;

    @Column(name = "user_long_id", nullable = false)
    private Long userIdLong;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    public Participants(ActiveGiveaways guildLongId, Long userIdLong, String nickName) {
        this.guildLongId = guildLongId;
        this.userIdLong = userIdLong;
        this.nickName = nickName;
    }
}