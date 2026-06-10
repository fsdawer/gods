package joat.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 팀방 채팅 메시지 엔티티.
 * team_messages 테이블과 매핑된다.
 */
@Entity
@Table(name = "team_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 소속 팀방 ID */
    @Column(nullable = false)
    private UUID teamRoomId;

    /** 메시지 발신자 userId */
    @Column(nullable = false)
    private UUID senderId;

    /** 메시지 본문 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 메시지 전송 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 팀 메시지 생성.
     *
     * @param teamRoomId 소속 팀방 ID
     * @param senderId   발신자 userId
     * @param content    메시지 본문
     */
    public static TeamMessage of(UUID teamRoomId, UUID senderId, String content) {
        TeamMessage teamMessage = new TeamMessage();
        teamMessage.teamRoomId = teamRoomId;
        teamMessage.senderId = senderId;
        teamMessage.content = content;
        teamMessage.createdAt = LocalDateTime.now();
        return teamMessage;
    }
}
