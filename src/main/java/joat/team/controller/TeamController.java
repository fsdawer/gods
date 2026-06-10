package joat.team.controller;

import jakarta.validation.Valid;
import joat.common.response.ApiResponse;
import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.team.dto.AddMemberRequest;
import joat.team.dto.CreateTeamRequest;
import joat.team.dto.RenameTeamRequest;
import joat.team.dto.TeamMessageResponse;
import joat.team.dto.TeamRoomResponse;
import joat.team.dto.UserSummaryResponse;
import joat.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 팀방 REST API 컨트롤러.
 *
 * <p>엔드포인트 목록:
 * <ul>
 *   <li>POST   /api/teams                          — 팀방 생성</li>
 *   <li>GET    /api/teams                          — 내 팀방 목록</li>
 *   <li>GET    /api/teams/{teamId}                 — 팀방 상세</li>
 *   <li>DELETE /api/teams/{teamId}                 — 팀방 삭제 (OWNER 전용)</li>
 *   <li>GET    /api/teams/{teamId}/members         — 팀원 목록 조회</li>
 *   <li>POST   /api/teams/{teamId}/members         — 팀원 추가 (OWNER 전용)</li>
 *   <li>DELETE /api/teams/{teamId}/members/{userId} — 팀원 제거</li>
 *   <li>GET    /api/teams/{teamId}/messages        — 채팅 메시지 조회</li>
 *   <li>PATCH  /api/teams/{teamId}/name            — 팀방 이름 변경 (OWNER 전용)</li>
 *   <li>DELETE /api/teams/{teamId}/posts/{postId}  — 팀 오너 게시물 강제 삭제</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * 팀방 생성.
     *
     * @param request 생성 요청 (이름 + 초대 멤버 목록)
     * @param userId  인증된 생성자 userId
     * @return 생성된 팀방 응답
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamRoomResponse> createTeam(
            @RequestBody @Valid CreateTeamRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(teamService.createTeam(request, userId));
    }

    /**
     * 내가 속한 팀방 목록 조회.
     *
     * @param userId 인증된 요청자 userId
     * @return 팀방 목록
     */
    @GetMapping
    public ApiResponse<List<TeamRoomResponse>> getMyTeams(
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(teamService.getMyTeams(userId));
    }

    /**
     * 팀방 상세 조회. 멤버만 조회 가능.
     *
     * @param teamId 팀방 ID
     * @param userId 인증된 요청자 userId
     * @return 팀방 상세 정보
     */
    @GetMapping("/{teamId}")
    public ApiResponse<TeamRoomResponse> getTeam(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(teamService.getTeam(teamId, userId));
    }

    /**
     * 팀방 삭제. OWNER만 가능.
     *
     * @param teamId 삭제할 팀방 ID
     * @param userId 인증된 요청자 userId
     */
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteTeam(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal UUID userId) {
        teamService.deleteTeam(teamId, userId);
        return ApiResponse.ok();
    }

    /**
     * 팀원 목록 조회. 멤버만 조회 가능.
     *
     * @param teamId 팀방 ID
     * @param userId 인증된 요청자 userId
     * @return 팀원 요약 목록
     */
    @GetMapping("/{teamId}/members")
    public ApiResponse<List<UserSummaryResponse>> getMembers(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(teamService.getMembers(teamId, userId));
    }

    /**
     * 팀원 추가. OWNER만 가능.
     *
     * @param teamId  팀방 ID
     * @param request 추가할 멤버 userId
     * @param userId  인증된 요청자 userId
     */
    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addMember(
            @PathVariable UUID teamId,
            @RequestBody @Valid AddMemberRequest request,
            @AuthenticationPrincipal UUID userId) {
        teamService.addMember(teamId, request, userId);
        return ApiResponse.ok();
    }

    /**
     * 팀원 제거. OWNER 또는 본인만 가능.
     *
     * @param teamId       팀방 ID
     * @param targetUserId 제거할 멤버 userId
     * @param userId       인증된 요청자 userId
     */
    @DeleteMapping("/{teamId}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UUID userId) {
        teamService.removeMember(teamId, targetUserId, userId);
        return ApiResponse.ok();
    }

    /**
     * 팀방 이름 변경. OWNER만 가능.
     *
     * @param teamId  팀방 ID
     * @param request 변경할 이름
     * @param userId  인증된 요청자 userId
     */
    @PatchMapping("/{teamId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> renameTeam(
            @PathVariable UUID teamId,
            @RequestBody @Valid RenameTeamRequest request,
            @AuthenticationPrincipal UUID userId) {
        teamService.renameTeam(teamId, request.getName(), userId);
        return ApiResponse.ok();
    }

    /**
     * 팀 오너 권한으로 게시물 강제 삭제. OWNER만 가능.
     *
     * @param teamId 팀방 ID
     * @param postId 삭제할 게시물 ID
     * @param userId 인증된 요청자 userId
     */
    @DeleteMapping("/{teamId}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteTeamPost(
            @PathVariable UUID teamId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal UUID userId) {
        teamService.deleteTeamPost(teamId, postId, userId);
        return ApiResponse.ok();
    }

    /**
     * 채팅 메시지 목록 조회. 멤버만 조회 가능.
     *
     * @param teamId 팀방 ID
     * @param limit  최대 조회 건수 (기본값 50)
     * @param userId 인증된 요청자 userId
     * @return 메시지 목록 (오름차순)
     */
    @GetMapping("/{teamId}/messages")
    public ApiResponse<List<TeamMessageResponse>> getMessages(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(teamService.getMessages(teamId, userId, limit));
    }

    /**
     * 팀 피드 조회. 팀 멤버들의 게시물을 최신순 커서 페이지네이션으로 반환한다.
     * 멤버만 조회 가능.
     *
     * @param teamId 팀방 ID
     * @param cursor 이전 페이지 마지막 포스트 UUID (첫 조회 시 생략)
     * @param limit  페이지당 포스트 수 (기본 20)
     * @param userId 인증된 요청자 userId
     * @return 커서 기반 포스트 목록
     */
    @GetMapping("/{teamId}/feed")
    public ApiResponse<CursorResponse<PostResponse>> getTeamFeed(
            @PathVariable UUID teamId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(teamService.getTeamFeed(teamId, userId, cursor, limit));
    }
}
