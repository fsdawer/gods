package joat.team.service;

import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.team.dto.AddMemberRequest;
import joat.team.dto.CreateTeamRequest;
import joat.team.dto.TeamMessageResponse;
import joat.team.dto.TeamRoomResponse;
import joat.team.dto.UserSummaryResponse;

import java.util.List;
import java.util.UUID;

/**
 * 팀방 서비스 인터페이스.
 * 팀방 CRUD, 멤버 관리, 메시지 조회를 담당한다.
 */
public interface TeamService {

    /**
     * 팀방 생성. 생성자는 자동으로 OWNER 역할로 추가된다.
     *
     * @param request 생성 요청 (이름 + 초대 멤버 목록)
     * @param ownerId 생성자 userId
     * @return 생성된 팀방 응답 DTO
     */
    TeamRoomResponse createTeam(CreateTeamRequest request, UUID ownerId);

    /**
     * 내가 속한 팀방 목록 조회.
     *
     * @param userId 조회 요청자 userId
     * @return 팀방 목록
     */
    List<TeamRoomResponse> getMyTeams(UUID userId);

    /**
     * 팀방 상세 조회. 멤버만 조회할 수 있다.
     *
     * @param teamId    팀방 ID
     * @param userId 요청자 userId
     * @return 팀방 응답 DTO
     */
    TeamRoomResponse getTeam(UUID teamId, UUID userId);

    /**
     * 팀방 삭제. OWNER만 삭제할 수 있다.
     *
     * @param teamId    삭제할 팀방 ID
     * @param userId 요청자 userId
     */
    void deleteTeam(UUID teamId, UUID userId);

    /**
     * 팀원 추가. OWNER만 추가할 수 있다.
     *
     * @param teamId    팀방 ID
     * @param request   추가할 멤버 userId
     * @param userId 요청자 userId
     */
    void addMember(UUID teamId, AddMemberRequest request, UUID userId);

    /**
     * 팀원 제거. OWNER 또는 본인만 제거할 수 있다.
     *
     * @param teamId    팀방 ID
     * @param targetUserId 제거할 멤버 userId
     * @param userId 요청자 userId
     */
    void removeMember(UUID teamId, UUID targetUserId, UUID userId);

    /**
     * 채팅 메시지 목록 조회. 멤버만 조회할 수 있다.
     *
     * @param teamId    팀방 ID
     * @param userId 요청자 userId
     * @param limit     최대 조회 건수
     * @return 메시지 목록 (오름차순)
     */
    List<TeamMessageResponse> getMessages(UUID teamId, UUID userId, int limit);

    /**
     * 채팅 메시지 저장 후 응답 DTO 반환. WebSocket 핸들러에서 호출된다.
     *
     * @param teamId   팀방 ID
     * @param senderId 발신자 userId
     * @param content  메시지 본문
     * @return 저장된 메시지 응답 DTO
     */
    TeamMessageResponse saveMessage(UUID teamId, UUID senderId, String content);

    /**
     * 팀원 목록 조회. 멤버만 조회할 수 있다.
     *
     * @param teamId 팀방 ID
     * @param userId 요청자 userId (멤버 여부 확인용)
     * @return 팀원 요약 목록 (id, nickname, profileImageUrl)
     */
    List<UserSummaryResponse> getMembers(UUID teamId, UUID userId);

    /**
     * 팀방 이름 변경. OWNER만 가능.
     *
     * @param teamId  팀방 ID
     * @param newName 변경할 이름
     * @param userId  요청자 userId
     */
    void renameTeam(UUID teamId, String newName, UUID userId);

    /**
     * 팀 오너 권한으로 게시물 삭제.
     * OWNER만 가능하며 본인 게시물이 아니어도 삭제할 수 있다.
     *
     * @param teamId  팀방 ID
     * @param postId  삭제할 게시물 ID
     * @param ownerId 요청자 userId (OWNER 검증용)
     */
    void deleteTeamPost(UUID teamId, UUID postId, UUID ownerId);

    /**
     * 팀 피드 조회. 팀 멤버들의 게시물을 최신순 커서 페이지네이션으로 반환한다.
     *
     * @param teamId   팀방 ID
     * @param userId   요청자 userId (멤버 여부 확인용)
     * @param cursor   이전 페이지 마지막 포스트 UUID (첫 조회 시 null)
     * @param limit    페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getTeamFeed(UUID teamId, UUID userId, UUID cursor, int limit);
}
