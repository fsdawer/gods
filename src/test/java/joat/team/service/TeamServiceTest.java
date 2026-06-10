package joat.team.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.team.dto.CreateTeamRequest;
import joat.team.entity.TeamMember;
import joat.team.entity.TeamMemberId;
import joat.team.entity.TeamRole;
import joat.team.entity.TeamRoom;
import joat.team.repository.TeamMemberRepository;
import joat.team.repository.TeamMessageRepository;
import joat.team.repository.TeamRoomRepository;
import joat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @InjectMocks TeamServiceImpl teamService;
    @Mock TeamRoomRepository teamRoomRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock TeamMessageRepository teamMessageRepository;
    @Mock UserRepository userRepository;

    @Test
    void 존재하지_않는_팀방_조회시_예외가_발생한다() {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(teamRoomRepository.findById(teamId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeam(teamId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    void 멤버가_아닌_유저가_팀방_상세_조회시_TEAM_ACCESS_DENIED_예외가_발생한다() {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TeamRoom room = TeamRoom.of("테스트팀", UUID.randomUUID());
        given(teamRoomRepository.findById(teamId)).willReturn(Optional.of(room));
        given(teamMemberRepository.existsByTeamRoomIdAndUserId(teamId, userId)).willReturn(false);

        assertThatThrownBy(() -> teamService.getTeam(teamId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_ACCESS_DENIED);
    }

    @Test
    void OWNER가_아닌_유저가_팀방_삭제시_TEAM_FORBIDDEN_예외가_발생한다() {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TeamRoom room = TeamRoom.of("테스트팀", UUID.randomUUID());
        given(teamRoomRepository.findById(teamId)).willReturn(Optional.of(room));
        // isOwner: TeamMemberId 조회 → MEMBER 역할 반환
        TeamMember member = TeamMember.of(teamId, userId, TeamRole.MEMBER);
        given(teamMemberRepository.findById(new TeamMemberId(teamId, userId)))
                .willReturn(Optional.of(member));

        assertThatThrownBy(() -> teamService.deleteTeam(teamId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_FORBIDDEN);
    }

    @Test
    void 이미_팀방_멤버인_유저를_다시_추가하면_ALREADY_TEAM_MEMBER_예외가_발생한다() {
        UUID teamId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();

        TeamRoom room = TeamRoom.of("테스트팀", ownerId);
        given(teamRoomRepository.findById(teamId)).willReturn(Optional.of(room));

        // 요청자가 OWNER
        TeamMember owner = TeamMember.of(teamId, ownerId, TeamRole.OWNER);
        given(teamMemberRepository.findById(new TeamMemberId(teamId, ownerId)))
                .willReturn(Optional.of(owner));

        // 추가하려는 멤버가 이미 존재
        given(teamMemberRepository.existsByTeamRoomIdAndUserId(teamId, newUserId)).willReturn(true);

        joat.team.dto.AddMemberRequest request = new joat.team.dto.AddMemberRequest();
        // Lombok @NoArgsConstructor + private field → 리플렉션으로 userId 설정
        try {
            java.lang.reflect.Field f = request.getClass().getDeclaredField("userId");
            f.setAccessible(true);
            f.set(request, newUserId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThatThrownBy(() -> teamService.addMember(teamId, request, ownerId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_TEAM_MEMBER);
    }
}
