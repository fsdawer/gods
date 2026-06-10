package joat.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import joat.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 팀방 엔티티.
 * team_rooms 테이블과 매핑된다.
 */
@Entity
@Table(name = "team_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 팀방 이름 (최대 100자) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 팀방 생성자의 userId */
    @Column(name = "created_by", nullable = false)
    private UUID ownerId;

    /** 현재 멤버 수 (팀원 추가/제거 시 갱신) */
    @Column(nullable = false)
    private int memberCount;

    /**
     * 팀방 생성.
     *
     * @param name    팀방 이름
     * @param ownerId 생성자 userId
     */
    public static TeamRoom of(String name, UUID ownerId) {
        TeamRoom teamRoom = new TeamRoom();
        teamRoom.name = name;
        teamRoom.ownerId = ownerId;
        teamRoom.memberCount = 1;
        return teamRoom;
    }

    /** 멤버 수 증가 */
    public void incrementMemberCount() {
        this.memberCount++;
    }

    /** 멤버 수 감소 (최소 0) */
    public void decrementMemberCount() {
        if (this.memberCount > 0) this.memberCount--;
    }

    /**
     * 팀방 이름 변경.
     *
     * @param newName 변경할 이름
     */
    public void rename(String newName) {
        this.name = newName;
    }
}
