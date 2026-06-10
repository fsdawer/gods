package joat.user.dto;

import joat.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 유저 프로필 응답 DTO.
 * GET /api/users/me 및 GET /api/users/{userId} 응답에 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    /** 유저 UUID */
    private UUID id;
    /** 앱 내 표시 이름 */
    private String nickname;
    /** S3 프로필 이미지 URL (없으면 null) */
    private String profileImageUrl;
    /** 자기소개 (없으면 null) */
    private String bio;
    /** 작성 게시물 수 */
    private long postCount;
    /** 팔로잉 수 (내가 팔로우하는 수) */
    private long followingCount;
    /** 팔로워 수 (나를 팔로우하는 수) */
    private long followerCount;
    /**
     * 요청자가 이 유저를 팔로우하고 있는지 여부.
     * GET /api/users/me 또는 비인증 요청 시 null.
     */
    private Boolean isFollowing;

    /**
     * 카운트와 팔로우 여부를 포함한 전체 생성 팩토리 메서드.
     *
     * @param user           User 엔티티
     * @param postCount      게시물 수
     * @param followingCount 팔로잉 수
     * @param followerCount  팔로워 수
     * @param isFollowing    요청자의 팔로우 여부 (null 허용)
     * @return UserProfileResponse
     */
    public static UserProfileResponse from(User user, long postCount, long followingCount,
                                           long followerCount, Boolean isFollowing) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .postCount(postCount)
                .followingCount(followingCount)
                .followerCount(followerCount)
                .isFollowing(isFollowing)
                .build();
    }

    /**
     * isFollowing 없이 카운트 포함 생성 (내 프로필용).
     *
     * @param user           User 엔티티
     * @param postCount      게시물 수
     * @param followingCount 팔로잉 수
     * @param followerCount  팔로워 수
     * @return UserProfileResponse (isFollowing = null)
     */
    public static UserProfileResponse from(User user, long postCount, long followingCount, long followerCount) {
        return from(user, postCount, followingCount, followerCount, null);
    }

    /**
     * 카운트 없이 기본값 0으로 생성 (내부 용도).
     *
     * @param user User 엔티티
     * @return UserProfileResponse (카운트 0, isFollowing null)
     */
    public static UserProfileResponse from(User user) {
        return from(user, 0, 0, 0, null);
    }
}
