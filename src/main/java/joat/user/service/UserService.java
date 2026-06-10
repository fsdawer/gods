package joat.user.service;

import joat.user.dto.FollowListResponse;
import joat.user.dto.UpdateProfileRequest;
import joat.user.dto.UserProfileResponse;
import joat.user.dto.UserSummary;

import java.util.List;
import java.util.UUID;

/**
 * 유저 서비스 인터페이스.
 * 프로필 조회/수정, 팔로우/언팔로우, 팔로워/팔로잉 목록 조회를 담당한다.
 */
public interface UserService {

    /**
     * 유저 프로필 조회 (내 프로필 또는 비인증 타인 프로필 조회 시).
     * isFollowing은 null로 반환된다.
     *
     * @param userId 조회할 유저의 UUID
     * @return UserProfileResponse (isFollowing = null)
     */
    UserProfileResponse getProfile(UUID userId);

    /**
     * 타인 프로필 조회 — 요청자 기준 팔로우 여부 포함.
     * GET /api/users/{userId} 인증 요청 시 사용한다.
     *
     * @param targetUserId 조회할 유저의 UUID
     * @param viewerId     요청자 UUID (팔로우 여부 계산용)
     * @return UserProfileResponse (isFollowing 포함)
     */
    UserProfileResponse getProfileForViewer(UUID targetUserId, UUID viewerId);

    /**
     * 내 프로필 수정.
     *
     * @param userId 수정할 유저의 UUID (JWT에서 파싱)
     * @param req    변경할 필드 (null이면 해당 필드 유지)
     * @return 수정 후 UserProfileResponse
     */
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req);

    /**
     * 팔로우.
     *
     * @param followerId  팔로우 요청자 UUID
     * @param followingId 팔로우 대상 UUID
     */
    void follow(UUID followerId, UUID followingId);

    /**
     * 언팔로우.
     *
     * @param followerId  언팔로우 요청자 UUID
     * @param followingId 언팔로우 대상 UUID
     */
    void unfollow(UUID followerId, UUID followingId);

    /**
     * 팔로워 목록 조회 (나를 팔로우하는 유저들).
     * 각 유저에 요청자 기준 isFollowing 여부를 포함한다.
     *
     * @param userId   대상 유저 UUID
     * @param viewerId 요청자 UUID (팔로우 여부 계산용, null 허용)
     * @return FollowListResponse (UserSummary 목록)
     */
    FollowListResponse getFollowers(UUID userId, UUID viewerId);

    /**
     * 팔로잉 목록 조회 (내가 팔로우하는 유저들).
     * 각 유저에 요청자 기준 isFollowing 여부를 포함한다.
     *
     * @param userId   대상 유저 UUID
     * @param viewerId 요청자 UUID (팔로우 여부 계산용, null 허용)
     * @return FollowListResponse (UserSummary 목록)
     */
    FollowListResponse getFollowing(UUID userId, UUID viewerId);

    /**
     * 닉네임으로 유저 검색.
     * 입력된 문자열을 포함하는 닉네임을 가진 유저 목록을 반환한다.
     *
     * @param q        검색어 (닉네임 부분 일치)
     * @param viewerId 요청자 UUID (팔로우 여부 계산용, null 허용)
     * @return UserSummary 목록
     */
    List<UserSummary> searchByNickname(String q, UUID viewerId);
}
