package joat.user.controller;

import jakarta.validation.Valid;
import joat.common.response.ApiResponse;
import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.feed.service.PostService;
import joat.user.dto.FcmTokenRequest;
import joat.user.dto.FollowListResponse;
import joat.user.dto.UpdateProfileRequest;
import joat.user.dto.UserProfileResponse;
import joat.user.dto.UserSummary;
import joat.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 유저 프로필 및 팔로우 관련 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;

    /**
     * [엔드포인트] GET /api/users/search?q={nickname} — 닉네임으로 유저 검색
     * q 파라미터를 닉네임에 부분 일치(대소문자 무시)하는 유저 목록을 반환한다.
     * 인증된 요청이면 각 유저에 대한 isFollowing 여부를 포함한다.
     */
    @GetMapping("/search")
    public ApiResponse<List<UserSummary>> searchUsers(
        @RequestParam String q,
        @AuthenticationPrincipal UUID viewerId
    ) {
        return ApiResponse.ok(userService.searchByNickname(q, viewerId));
    }

    /**
     * [엔드포인트] GET /api/users/me — 내 프로필 조회
     * JWT에서 파싱된 userId로 내 프로필을 반환한다. isFollowing은 null.
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    /**
     * [엔드포인트] GET /api/users/me/followers — 내 팔로워 목록 조회
     * 인증된 유저를 팔로우하는 유저 목록을 반환한다. me.id 없이 JWT만으로 조회 가능.
     */
    @GetMapping("/me/followers")
    public ApiResponse<FollowListResponse> myFollowers(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(userService.getFollowers(userId, userId));
    }

    /**
     * [엔드포인트] GET /api/users/me/following — 내 팔로잉 목록 조회
     * 인증된 유저가 팔로우하는 유저 목록을 반환한다. me.id 없이 JWT만으로 조회 가능.
     */
    @GetMapping("/me/following")
    public ApiResponse<FollowListResponse> myFollowing(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(userService.getFollowing(userId, userId));
    }

    /**
     * [엔드포인트] GET /api/users/me/posts — 내 게시물 목록 조회 (커서 기반)
     * cursor 없이 요청하면 첫 페이지, cursor 포함 시 이후 페이지를 반환한다.
     */
    @GetMapping("/me/posts")
    public ApiResponse<CursorResponse<PostResponse>> myPosts(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) UUID cursor,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(postService.getMyPosts(userId, cursor, limit));
    }

    /**
     * [엔드포인트] PATCH /api/users/me — 내 프로필 수정
     * 변경할 필드만 전달하며, null인 필드는 기존값을 유지한다.
     */
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> update(
        @AuthenticationPrincipal UUID userId,
        @RequestBody UpdateProfileRequest req
    ) {
        return ApiResponse.ok(userService.updateProfile(userId, req));
    }

    /**
     * [엔드포인트] GET /api/users/{userId} — 타인 프로필 조회
     * 인증된 요청이면 isFollowing 포함, 비인증 요청이면 isFollowing = null.
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUser(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UUID viewerId
    ) {
        if (viewerId != null && !viewerId.equals(userId)) {
            return ApiResponse.ok(userService.getProfileForViewer(userId, viewerId));
        }
        return ApiResponse.ok(userService.getProfile(userId));
    }

    /**
     * [엔드포인트] GET /api/users/{userId}/posts — 타인 게시물 목록 조회 (커서 기반)
     * 해당 유저의 공개 게시물을 최신순으로 반환한다.
     */
    @GetMapping("/{userId}/posts")
    public ApiResponse<CursorResponse<PostResponse>> userPosts(
        @PathVariable UUID userId,
        @RequestParam(required = false) UUID cursor,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(postService.getUserPosts(userId, cursor, limit));
    }

    /**
     * [엔드포인트] POST /api/users/{userId}/follow — 팔로우
     * 자기 자신 팔로우 및 중복 팔로우 시 BusinessException이 발생한다.
     */
    @PostMapping("/{userId}/follow")
    public ApiResponse<Void> follow(
        @AuthenticationPrincipal UUID me,
        @PathVariable UUID userId
    ) {
        userService.follow(me, userId);
        return ApiResponse.ok();
    }

    /**
     * [엔드포인트] DELETE /api/users/{userId}/follow — 언팔로우
     * 팔로우 관계가 없으면 BusinessException이 발생한다.
     */
    @DeleteMapping("/{userId}/follow")
    public ApiResponse<Void> unfollow(
        @AuthenticationPrincipal UUID me,
        @PathVariable UUID userId
    ) {
        userService.unfollow(me, userId);
        return ApiResponse.ok();
    }

    /**
     * [엔드포인트] GET /api/users/{userId}/followers — 팔로워 목록 조회
     * 해당 유저를 팔로우하는 유저들의 UserSummary 목록을 반환한다.
     * 인증된 요청이면 각 유저에 대한 isFollowing 여부를 포함한다.
     */
    @GetMapping("/{userId}/followers")
    public ApiResponse<FollowListResponse> followers(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UUID viewerId
    ) {
        return ApiResponse.ok(userService.getFollowers(userId, viewerId));
    }

    /**
     * [엔드포인트] GET /api/users/{userId}/following — 팔로잉 목록 조회
     * 해당 유저가 팔로우하는 유저들의 UserSummary 목록을 반환한다.
     * 인증된 요청이면 각 유저에 대한 isFollowing 여부를 포함한다.
     */
    @GetMapping("/{userId}/following")
    public ApiResponse<FollowListResponse> following(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UUID viewerId
    ) {
        return ApiResponse.ok(userService.getFollowing(userId, viewerId));
    }

    /**
     * [엔드포인트] PATCH /api/users/me/fcm-token — FCM 디바이스 토큰 등록/갱신
     * 앱 시작 시 RN에서 호출하여 최신 토큰을 서버에 저장한다.
     * token을 null로 보내면 토큰을 제거하여 로그아웃 후 알림 수신을 중단한다.
     *
     * @param userId JWT에서 파싱된 요청자 UUID
     * @param req    FCM 토큰 (null 허용)
     */
    @PatchMapping("/me/fcm-token")
    public ApiResponse<Void> saveFcmToken(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody FcmTokenRequest req
    ) {
        userService.saveFcmToken(userId, req.getToken());
        return ApiResponse.ok();
    }
}
