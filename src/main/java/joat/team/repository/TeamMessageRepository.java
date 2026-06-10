package joat.team.repository;

import joat.team.entity.TeamMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 팀방 채팅 메시지 JPA 레포지토리.
 */
public interface TeamMessageRepository extends JpaRepository<TeamMessage, UUID> {

    /**
     * 특정 팀방의 메시지를 생성 시각 오름차순으로 반환한다 (페이지네이션 적용).
     *
     * @param teamRoomId 팀방 ID
     * @param pageable   페이지네이션 정보
     * @return 메시지 목록 (오름차순)
     */
    List<TeamMessage> findByTeamRoomIdOrderByCreatedAtAsc(UUID teamRoomId, Pageable pageable);
}
