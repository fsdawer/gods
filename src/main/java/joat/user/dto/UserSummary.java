package joat.user.dto;

import joat.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 팔로워/팔로잉 목록 항목 응답 DTO.
 * GET /api/users/{userId}/followers, GET /api/users/{userId}/following 목록 항목에 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummary {
    /** 유저 UUID */
    private UUID id;
    /** 앱 내 표시 이름 */
    private String nickname;
    /** S3 프로필 이미지 URL (없으면 null) */
    private String profileImageUrl;
    /** 자기소개 (없으면 null) */
    private String bio;
    /**
     * 요청자가 이 유저를 팔로우하고 있는지 여부.
     * 비인증 요청 시 null.
     */
    private Boolean isFollowing;

    /**
     * User 엔티티와 팔로우 여부로 UserSummary를 생성한다.
     *
     * @param user        User 엔티티
     * @param isFollowing 요청자의 팔로우 여부 (null 허용)
     * @return UserSummary
     */
    public static UserSummary from(User user, Boolean isFollowing) {
        return UserSummary.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .isFollowing(isFollowing)
                .build();
    }
}
