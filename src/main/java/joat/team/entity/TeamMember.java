package joat.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 팀방 멤버 엔티티.
 * team_members 테이블과 매핑되며, (teamRoomId, userId) 복합 기본키를 사용한다.
 */
@Entity
@Table(name = "team_members")
@IdClass(TeamMemberId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    @Id
    @Column(name = "team_room_id")
    private UUID teamRoomId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    /** 역할: OWNER 또는 MEMBER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role;

    /** 팀 합류 시각 */
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /**
     * 팀 멤버 생성.
     *
     * @param teamRoomId 소속 팀방 ID
     * @param userId     멤버 userId
     * @param role       역할 (OWNER | MEMBER)
     */
    public static TeamMember of(UUID teamRoomId, UUID userId, TeamRole role) {
        TeamMember teamMember = new TeamMember();
        teamMember.teamRoomId = teamRoomId;
        teamMember.userId = userId;
        teamMember.role = role;
        teamMember.joinedAt = LocalDateTime.now();
        return teamMember;
    }
}
