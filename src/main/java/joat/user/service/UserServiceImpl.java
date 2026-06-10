package joat.user.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.feed.service.PostService;
import joat.user.dto.FollowListResponse;
import joat.user.dto.UpdateProfileRequest;
import joat.user.dto.UserProfileResponse;
import joat.user.dto.UserSummary;
import joat.user.entity.Follow;
import joat.user.entity.FollowId;
import joat.user.entity.User;
import joat.user.repository.FollowRepository;
import joat.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UserService 구현체.
 * 유저 프로필 관리 및 팔로우 관계를 처리한다.
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostService postService;

    public UserServiceImpl(UserRepository userRepository,
                           FollowRepository followRepository,
                           @Lazy PostService postService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postService = postService;
    }

    /**
     * [프로필 조회 플로우 — 내 프로필 / 비인증 타인 프로필]
     * users 테이블에서 userId로 User 엔티티를 조회하여 DTO로 변환한다.
     * isFollowing은 null로 반환된다.
     *
     * 입력: userId (UUID) — JWT 또는 경로변수에서 전달
     * 호출: UserRepository.findById(userId), PostService.countPosts, FollowRepository.count*
     * 반환: UserProfileResponse (isFollowing = null)
     * 예외: USER_NOT_FOUND (해당 userId의 유저가 없을 때)
     */
    @Override
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUser(userId);
        long postCount = postService.countPosts(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long followerCount = followRepository.countByFollowingId(userId);
        return UserProfileResponse.from(user, postCount, followingCount, followerCount);
    }

    /**
     * [타인 프로필 조회 플로우 — 팔로우 여부 포함]
     * GET /api/users/{userId} 인증 요청 시 사용한다.
     * viewerId 기준으로 targetUserId를 팔로우하고 있는지 여부를 계산하여 포함한다.
     *
     * 입력: targetUserId (조회 대상 UUID), viewerId (요청자 UUID)
     * 호출: UserRepository.findById, PostService.countPosts, FollowRepository.count*, FollowRepository.existsByFollowerIdAndFollowingId
     * 반환: UserProfileResponse (isFollowing 포함)
     * 예외: USER_NOT_FOUND
     */
    @Override
    public UserProfileResponse getProfileForViewer(UUID targetUserId, UUID viewerId) {
        User user = findUser(targetUserId);
        long postCount = postService.countPosts(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);
        long followerCount = followRepository.countByFollowingId(targetUserId);
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(viewerId, targetUserId);
        return UserProfileResponse.from(user, postCount, followingCount, followerCount, isFollowing);
    }

    /**
     * [프로필 수정 플로우]
     * 1. userId로 User 엔티티 조회
     * 2. 전달된 필드만 업데이트 (null 필드는 기존값 유지)
     * 3. JPA dirty checking으로 자동 UPDATE 쿼리 실행
     * 4. 수정된 User를 DTO로 변환하여 반환
     *
     * 입력: userId (JWT에서 파싱), req (nickname/profileImageUrl/bio 중 변경할 것만)
     * 호출: UserRepository.findById → User.updateProfile
     * 반환: 수정 후 UserProfileResponse
     */
    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = findUser(userId);
        user.updateProfile(req.getNickname(), req.getProfileImageUrl(), req.getBio());
        long postCount = postService.countPosts(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long followerCount = followRepository.countByFollowingId(userId);
        return UserProfileResponse.from(user, postCount, followingCount, followerCount);
    }

    /**
     * [팔로우 플로우]
     * 1. 자기 자신 팔로우 여부 검증 (followerId == followingId → CANNOT_FOLLOW_SELF)
     * 2. 팔로우 대상 유저 존재 확인 (없으면 USER_NOT_FOUND)
     * 3. 중복 팔로우 검증 (이미 팔로우 중이면 ALREADY_FOLLOWING)
     * 4. follows 테이블에 (followerId, followingId) 행 삽입
     *
     * 입력: followerId (요청자 UUID), followingId (대상 UUID)
     * 호출: FollowRepository.existsByFollowerIdAndFollowingId → FollowRepository.save
     * 반환: void
     */
    @Override
    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        findUser(followingId);
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BusinessException(ErrorCode.ALREADY_FOLLOWING);
        }
        followRepository.save(Follow.of(followerId, followingId));
    }

    /**
     * [언팔로우 플로우]
     * 1. 팔로우 관계 존재 여부 확인 (없으면 NOT_FOLLOWING)
     * 2. follows 테이블에서 (followerId, followingId) 행 삭제
     *
     * 입력: followerId (요청자 UUID), followingId (대상 UUID)
     * 호출: FollowRepository.existsByFollowerIdAndFollowingId → FollowRepository.deleteById
     * 반환: void
     */
    @Override
    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BusinessException(ErrorCode.NOT_FOLLOWING);
        }
        followRepository.deleteById(new FollowId(followerId, followingId));
    }

    /**
     * [팔로워 목록 조회 플로우]
     * follows 테이블에서 following_id = userId인 행들을 조회하고,
     * 각 유저의 User 엔티티를 배치 로딩하여 UserSummary 목록을 반환한다.
     *
     * 입력: userId (조회 대상 UUID), viewerId (팔로우 여부 계산용, null 허용)
     * 호출: FollowRepository.findByFollowingId → UserRepository.findAllById
     *       → viewerId가 있으면 FollowRepository.findByFollowerId(viewerId)로 팔로우 여부 계산
     * 반환: FollowListResponse (UserSummary 목록)
     */
    @Override
    public FollowListResponse getFollowers(UUID userId, UUID viewerId) {
        // follows 테이블에서 following_id = userId인 row 조회 → "나를 팔로우하는 사람들"의 Follow 객체 목록
        List<UUID> followerIds = followRepository.findByFollowingId(userId)
            .stream()
            .map(Follow::getFollowerId) // Follow 객체에서 팔로워의 UUID만 추출
            .toList();                  // UUID 리스트로 변환 (다음 배치 조회에 사용)

        // followerIds로 WHERE id IN (...) 한 번 실행 → N+1 없이 User 전체 정보 한 번에 로딩
        List<User> users = userRepository.findAllById(followerIds);
        // viewer(현재 앱 사용자)가 팔로우하는 ID Set → 각 팔로워 카드의 "팔로우 중" 버튼 상태 계산에 사용
        Set<UUID> viewerFollowingIds = loadViewerFollowingIds(viewerId);

        List<UserSummary> summaries = users.stream()
            .map(u -> UserSummary.from(
                u,
                // viewerId가 null(비로그인)이면 팔로우 여부를 알 수 없으므로 null 전달
                // viewerId가 있으면 viewerFollowingIds에 이 유저가 포함되는지로 맞팔 여부 판단
                viewerId != null ? viewerFollowingIds.contains(u.getId()) : null
            ))
            .toList();

        return new FollowListResponse(summaries); // 팔로워 목록 + 각자의 맞팔 여부를 담아 반환
    }

    /**
     * [팔로잉 목록 조회 플로우]
     * follows 테이블에서 follower_id = userId인 행들을 조회하고,
     * 각 유저의 User 엔티티를 배치 로딩하여 UserSummary 목록을 반환한다.
     *
     * 입력: userId (조회 대상 UUID), viewerId (팔로우 여부 계산용, null 허용)
     * 호출: FollowRepository.findByFollowerId → UserRepository.findAllById
     *       → viewerId가 있으면 FollowRepository.findByFollowerId(viewerId)로 팔로우 여부 계산
     * 반환: FollowListResponse (UserSummary 목록)
     */
    @Override
    public FollowListResponse getFollowing(UUID userId, UUID viewerId) {
        List<UUID> followingIds = followRepository.findByFollowerId(userId)
            .stream()
            .map(Follow::getFollowingId)
            .toList();

        List<User> users = userRepository.findAllById(followingIds);
        Set<UUID> viewerFollowingIds = loadViewerFollowingIds(viewerId);

        List<UserSummary> summaries = users.stream()
            .map(u -> UserSummary.from(u, viewerId != null ? viewerFollowingIds.contains(u.getId()) : null))
            .toList();

        return new FollowListResponse(summaries);
    }

    /**
     * [팔로우 여부 계산용 Set 로딩]
     * viewerId가 null이면 빈 Set을 반환하여 isFollowing 계산을 건너뛴다.
     * viewerId가 있으면 해당 유저가 팔로우하는 UUID Set을 한 번에 로딩한다.
     *
     * 입력: viewerId (null 허용)
     * 호출: FollowRepository.findByFollowerId(viewerId)
     * 반환: 팔로잉 UUID Set (viewerId null이면 빈 Set)
     */
    private Set<UUID> loadViewerFollowingIds(UUID viewerId) {
        if (viewerId == null) return Set.of();
        return followRepository.findByFollowerId(viewerId)
            .stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toSet());
    }

    /**
     * [닉네임 검색 플로우]
     * 1. 닉네임 부분 일치로 users 테이블 조회
     * 2. viewerId가 있으면 팔로우 여부 Set 로딩
     * 3. 각 유저를 UserSummary로 변환
     *
     * 입력: q (검색어), viewerId (팔로우 여부 계산용, null 허용)
     * 호출: UserRepository.findByNicknameContainingIgnoreCase → loadViewerFollowingIds
     * 반환: UserSummary 목록
     */
    @Override
    public List<UserSummary> searchByNickname(String q, UUID viewerId) {
        List<User> users = userRepository.findByNicknameContainingIgnoreCase(q);
        Set<UUID> viewerFollowingIds = loadViewerFollowingIds(viewerId);
        return users.stream()
            .map(u -> UserSummary.from(u, viewerId != null ? viewerFollowingIds.contains(u.getId()) : null))
            .toList();
    }

    /**
     * [유저 조회 공통 메서드]
     * userId로 users 테이블을 조회하고, 없으면 USER_NOT_FOUND 예외를 던진다.
     *
     * 입력: id (UUID)
     * 호출: UserRepository.findById
     * 반환: User 엔티티
     */
    private User findUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
