package joat.team.dto;

import joat.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 팀원 목록 조회 응답 DTO.
 * 유저의 요약 정보(id, nickname, profileImageUrl)만 포함한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

    private UUID id;
    private String nickname;
    private String profileImageUrl;

    /**
     * User 엔티티 → 요약 응답 DTO 변환.
     *
     * @param user 유저 엔티티
     * @return 요약 응답 DTO
     */
    public static UserSummaryResponse from(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
