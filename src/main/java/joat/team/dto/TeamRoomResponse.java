package joat.team.dto;

import joat.team.entity.TeamRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 팀방 목록/상세 조회 응답 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRoomResponse {

    private UUID id;
    private String name;
    private UUID ownerId;
    private int memberCount;
    private LocalDateTime createdAt;

    /**
     * TeamRoom 엔티티 → 응답 DTO 변환.
     *
     * @param teamRoom 팀방 엔티티
     * @return 응답 DTO
     */
    public static TeamRoomResponse from(TeamRoom teamRoom) {
        return TeamRoomResponse.builder()
                .id(teamRoom.getId())
                .name(teamRoom.getName())
                .ownerId(teamRoom.getOwnerId())
                .memberCount(teamRoom.getMemberCount())
                .createdAt(teamRoom.getCreatedAt())
                .build();
    }
}
