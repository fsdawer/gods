package joat.team.dto;

import joat.team.entity.TeamMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 응답 DTO.
 * REST 조회 및 WebSocket 브로드캐스트 모두에 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMessageResponse {

    private UUID id;
    private UUID teamRoomId;
    private UUID senderId;
    private String senderNickname;
    private String content;
    private LocalDateTime createdAt;

    /**
     * TeamMessage 엔티티 → 응답 DTO 변환.
     *
     * @param teamMessage    채팅 메시지 엔티티
     * @param senderNickname 발신자 닉네임 (User 조회 결과)
     * @return 응답 DTO
     */
    public static TeamMessageResponse from(TeamMessage teamMessage, String senderNickname) {
        return TeamMessageResponse.builder()
                .id(teamMessage.getId())
                .teamRoomId(teamMessage.getTeamRoomId())
                .senderId(teamMessage.getSenderId())
                .senderNickname(senderNickname)
                .content(teamMessage.getContent())
                .createdAt(teamMessage.getCreatedAt())
                .build();
    }
}
