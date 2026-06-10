package joat.team.repository;

import joat.team.entity.TeamMember;
import joat.team.entity.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 팀방 멤버 JPA 레포지토리.
 */
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    /**
     * 특정 팀방의 전체 멤버 목록을 반환한다.
     *
     * @param teamRoomId 팀방 ID
     * @return 해당 팀방의 멤버 목록
     */
    List<TeamMember> findByTeamRoomId(UUID teamRoomId);

    /**
     * 특정 유저가 해당 팀방의 멤버인지 확인한다.
     *
     * @param teamRoomId 팀방 ID
     * @param userId     확인할 userId
     * @return 멤버 여부
     */
    boolean existsByTeamRoomIdAndUserId(UUID teamRoomId, UUID userId);
}
