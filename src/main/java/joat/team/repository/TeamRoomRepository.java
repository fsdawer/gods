package joat.team.repository;

import joat.team.entity.TeamRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * 팀방 JPA 레포지토리.
 */
public interface TeamRoomRepository extends JpaRepository<TeamRoom, UUID> {

    /**
     * 특정 유저가 멤버로 속한 팀방 목록을 최신순으로 반환한다.
     *
     * @param userId 조회 대상 userId
     * @return 해당 유저가 속한 팀방 목록
     */
    @Query("SELECT tr FROM TeamRoom tr JOIN TeamMember tm ON tr.id = tm.teamRoomId WHERE tm.userId = :userId ORDER BY tr.createdAt DESC")
    List<TeamRoom> findByMemberUserId(@Param("userId") UUID userId);
}
