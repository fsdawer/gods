package joat.team.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.feed.dto.CreatePostRequest;
import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.feed.service.PostService;
import joat.team.dto.AddMemberRequest;
import joat.team.dto.CreateTeamRequest;
import joat.team.dto.TeamMessageResponse;
import joat.team.dto.TeamRoomResponse;
import joat.team.dto.UserSummaryResponse;
import joat.team.entity.TeamMember;
import joat.team.entity.TeamMemberId;
import joat.team.entity.TeamMessage;
import joat.team.entity.TeamRole;
import joat.team.entity.TeamRoom;
import joat.team.repository.TeamMemberRepository;
import joat.team.repository.TeamMessageRepository;
import joat.team.repository.TeamRoomRepository;
import joat.user.entity.User;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TeamService 구현체.
 * 팀방 CRUD, 멤버 관리, 메시지 저장/조회를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamServiceImpl implements TeamService {

    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMessageRepository teamMessageRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    /**
     * 팀방 생성.
     * 생성자는 OWNER 역할로 추가되고, 초대 멤버들은 MEMBER 역할로 추가된다.
     *
     * @param request 생성 요청 (이름 + 초대 멤버 목록)
     * @param ownerId 생성자 userId
     * @return 생성된 팀방 응답 DTO
     */
    @Override
    @Transactional
    public TeamRoomResponse createTeam(CreateTeamRequest request, UUID ownerId) {
        TeamRoom teamRoom = TeamRoom.of(request.getName(), ownerId);
        teamRoomRepository.save(teamRoom);
        UUID teamRoomId = teamRoom.getId();

        // 생성자를 OWNER로 추가
        teamMemberRepository.save(TeamMember.of(teamRoomId, ownerId, TeamRole.OWNER));

        // 초대 멤버를 MEMBER로 추가
        for (UUID memberId : request.getMemberIds()) {
            if (!memberId.equals(ownerId)) {
                teamMemberRepository.save(TeamMember.of(teamRoomId, memberId, TeamRole.MEMBER));
                teamRoom.incrementMemberCount();
            }
        }

        return TeamRoomResponse.from(teamRoom);
    }

    /**
     * 내가 속한 팀방 목록 조회.
     *
     * @param userId 조회 요청자 userId
     * @return 팀방 목록
     */
    @Override
    public List<TeamRoomResponse> getMyTeams(UUID userId) {
        return teamRoomRepository.findByMemberUserId(userId).stream()
                .map(TeamRoomResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 팀방 상세 조회. 멤버가 아니면 TEAM_ACCESS_DENIED 예외를 던진다.
     *
     * @param teamId      팀방 ID
     * @param userId 요청자 userId
     * @return 팀방 응답 DTO
     */
    @Override
    public TeamRoomResponse getTeam(UUID teamId, UUID userId) {
        TeamRoom teamRoom = findRoomOrThrow(teamId);
        requireMember(teamId, userId);
        return TeamRoomResponse.from(teamRoom);
    }

    /**
     * 팀방 삭제. OWNER만 삭제할 수 있다. 팀방 삭제 시 관련 멤버/메시지도 CASCADE로 제거된다.
     *
     * @param teamId      삭제할 팀방 ID
     * @param userId 요청자 userId
     */
    @Override
    @Transactional
    public void deleteTeam(UUID teamId, UUID userId) {
        findRoomOrThrow(teamId);
        requireOwner(teamId, userId);
        teamRoomRepository.deleteById(teamId);
    }

    /**
     * 팀원 추가. OWNER만 추가할 수 있다.
     * 이미 멤버이면 ALREADY_TEAM_MEMBER 예외를 던진다.
     *
     * @param teamId      팀방 ID
     * @param request     추가할 멤버 userId
     * @param userId 요청자 userId
     */
    @Override
    @Transactional
    public void addMember(UUID teamId, AddMemberRequest request, UUID userId) {
        TeamRoom teamRoom = findRoomOrThrow(teamId);
        requireOwner(teamId, userId);

        UUID newUserId = request.getUserId();
        if (teamMemberRepository.existsByTeamRoomIdAndUserId(teamId, newUserId)) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }

        teamMemberRepository.save(TeamMember.of(teamId, newUserId, TeamRole.MEMBER));
        teamRoom.incrementMemberCount();
    }

    /**
     * 팀원 제거. OWNER 또는 본인만 제거할 수 있다.
     * 멤버가 아니면 NOT_TEAM_MEMBER 예외를 던진다.
     *
     * @param teamId       팀방 ID
     * @param targetUserId 제거할 멤버 userId
     * @param userId  요청자 userId
     */
    @Override
    @Transactional
    public void removeMember(UUID teamId, UUID targetUserId, UUID userId) {
        TeamRoom teamRoom = findRoomOrThrow(teamId);

        boolean isSelf = userId.equals(targetUserId);
        boolean isOwner = isOwner(teamId, userId);

        if (!isSelf && !isOwner) {
            throw new BusinessException(ErrorCode.TEAM_FORBIDDEN);
        }

        TeamMemberId memberId = new TeamMemberId(teamId, targetUserId);
        if (!teamMemberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }

        teamMemberRepository.deleteById(memberId);
        teamRoom.decrementMemberCount();
    }

    /**
     * 채팅 메시지 목록 조회. 멤버가 아니면 TEAM_ACCESS_DENIED 예외를 던진다.
     * 닉네임 N+1 방지를 위해 발신자 목록을 배치 조회한다.
     *
     * @param teamId      팀방 ID
     * @param userId 요청자 userId
     * @param limit       최대 조회 건수
     * @return 메시지 목록 (오름차순)
     */
    @Override
    public List<TeamMessageResponse> getMessages(UUID teamId, UUID userId, int limit) {
        requireMember(teamId, userId);

        List<TeamMessage> messages = teamMessageRepository.findByTeamRoomIdOrderByCreatedAtAsc(
                teamId, PageRequest.of(0, limit));

        Set<UUID> senderIds = messages.stream()
                .map(TeamMessage::getSenderId)
                .collect(Collectors.toSet());

        Map<UUID, String> nicknameMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return messages.stream()
                .map(m -> TeamMessageResponse.from(m, nicknameMap.getOrDefault(m.getSenderId(), "알 수 없음")))
                .collect(Collectors.toList());
    }

    /**
     * 채팅 메시지 저장. WebSocket 핸들러에서 호출된다.
     * 발신자가 멤버인지 확인 후 저장한다.
     *
     * @param teamId   팀방 ID
     * @param senderId 발신자 userId
     * @param content  메시지 본문
     * @return 저장된 메시지 응답 DTO
     */
    @Override
    @Transactional
    public TeamMessageResponse saveMessage(UUID teamId, UUID senderId, String content) {
        requireMember(teamId, senderId);
        TeamMessage teamMessage = teamMessageRepository.save(TeamMessage.of(teamId, senderId, content));
        String nickname = userRepository.findById(senderId)
                .map(User::getNickname)
                .orElse("알 수 없음");
        return TeamMessageResponse.from(teamMessage, nickname);
    }

    /**
     * 팀 피드 조회.
     * 팀 멤버 UUID 목록을 조회한 뒤 PostService에 위임하여 커서 기반 포스트 목록을 반환한다.
     *
     * @param teamId 팀방 ID
     * @param userId 요청자 userId (멤버 여부 확인)
     * @param cursor 이전 페이지 마지막 포스트 UUID (null이면 첫 페이지)
     * @param limit  페이지당 포스트 수
     * @return CursorResponse(포스트 목록, nextCursor, hasNext)
     */
    @Override
    public CursorResponse<PostResponse> getTeamFeed(UUID teamId, UUID userId, UUID cursor, int limit) {
        requireMember(teamId, userId);
        List<UUID> memberIds = teamMemberRepository.findByTeamRoomId(teamId)
                .stream()
                .map(TeamMember::getUserId)
                .collect(Collectors.toList());
        return postService.getFeedForMembers(memberIds, cursor, limit);
    }

    /**
     * 팀원 목록 조회. 멤버가 아니면 TEAM_ACCESS_DENIED 예외를 던진다.
     * TeamMember 목록 → UserRepository 배치 조회 → DTO 변환.
     *
     * @param teamId 팀방 ID
     * @param userId 요청자 userId (멤버 여부 확인용)
     * @return 팀원 요약 목록 (id, nickname, profileImageUrl)
     */
    @Override
    public List<UserSummaryResponse> getMembers(UUID teamId, UUID userId) {
        findRoomOrThrow(teamId);
        requireMember(teamId, userId);

        List<UUID> memberIds = teamMemberRepository.findByTeamRoomId(teamId)
                .stream()
                .map(TeamMember::getUserId)
                .collect(Collectors.toList());

        return userRepository.findAllById(memberIds).stream()
                .map(UserSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 팀방 이름 변경. OWNER가 아니면 TEAM_FORBIDDEN 예외를 던진다.
     *
     * @param teamId  팀방 ID
     * @param newName 변경할 이름
     * @param userId  요청자 userId
     */
    @Override
    @Transactional
    public void renameTeam(UUID teamId, String newName, UUID userId) {
        TeamRoom teamRoom = findRoomOrThrow(teamId);
        requireOwner(teamId, userId);
        teamRoom.rename(newName);
    }

    /**
     * 팀 오너 권한으로 게시물 삭제. OWNER가 아니면 TEAM_FORBIDDEN 예외를 던진다.
     * 본인 게시물이 아니어도 삭제 가능하다.
     *
     * @param teamId  팀방 ID
     * @param postId  삭제할 게시물 ID
     * @param ownerId 요청자 userId (OWNER 검증용)
     */
    @Override
    @Transactional
    public void deleteTeamPost(UUID teamId, UUID postId, UUID ownerId) {
        findRoomOrThrow(teamId);
        requireOwner(teamId, ownerId);
        postService.forceDeletePost(postId);
    }




    /**
     * 팀방 게시물 작성.
     * 팀원이 아니면 TEAM_ACCESS_DENIED 예외를 던진다.
     * 작성된 게시물에는 teamId가 설정되어 공개 피드/태그 검색에서 제외된다.
     *
     * @param teamId 팀방 ID
     * @param userId 작성자 userId (팀원 검증 포함)
     * @param req    게시물 내용, 이미지 URL 목록, 해시태그 목록
     * @return 생성된 PostResponse
     */
    @Override
    @Transactional
    public PostResponse createTeamPost(UUID teamId, UUID userId, CreatePostRequest req) {
        findRoomOrThrow(teamId);
        requireMember(teamId, userId);
        // teamId가 포함된 요청 객체를 PostService에 위임
        CreatePostRequest teamReq = new CreatePostRequest(
            req.getContent(),
            req.getImageUrls(),
            req.getTagNames(),
            null,   // 팀방 게시물은 투두 인증 포스트 불가
            teamId
        );
        return postService.createPost(userId, teamReq);
    }

    /**
     * 유저가 팀방 멤버인지 확인한다.
     *
     * @param teamId 팀방 ID
     * @param userId 확인할 유저 UUID
     * @return 멤버이면 true, 아니면 false
     */
    @Override
    public boolean isMember(UUID teamId, UUID userId) {
        return teamMemberRepository.existsByTeamRoomIdAndUserId(teamId, userId);
    }

    // ---- 내부 헬퍼 ----

    /** 팀방 조회. 없으면 TEAM_NOT_FOUND 예외. */
    private TeamRoom findRoomOrThrow(UUID teamId) {
        return teamRoomRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    /** 멤버 여부 확인. 아니면 TEAM_ACCESS_DENIED 예외. */
    private void requireMember(UUID teamId, UUID userId) {
        if (!teamMemberRepository.existsByTeamRoomIdAndUserId(teamId, userId)) {
            throw new BusinessException(ErrorCode.TEAM_ACCESS_DENIED);
        }
    }

    /** OWNER 여부 확인. 아니면 TEAM_FORBIDDEN 예외. */
    private void requireOwner(UUID teamId, UUID userId) {
        if (!isOwner(teamId, userId)) {
            throw new BusinessException(ErrorCode.TEAM_FORBIDDEN);
        }
    }

    /**
     * 해당 userId가 팀방의 OWNER인지 반환한다.
     *
     * @param teamId 팀방 ID
     * @param userId 확인할 userId
     * @return OWNER 여부
     */
    private boolean isOwner(UUID teamId, UUID userId) {
        return teamMemberRepository.findById(new TeamMemberId(teamId, userId))
                .map(m -> m.getRole() == TeamRole.OWNER)
                .orElse(false);
    }
}
