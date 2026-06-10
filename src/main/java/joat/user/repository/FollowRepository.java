package joat.user.repository;

import joat.user.entity.Follow;
import joat.user.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    // 특정 유저가 팔로우하는 모든 대상 조회 (팔로잉 목록)
    List<Follow> findByFollowerId(UUID followerId);
    // 특정 유저를 팔로우하는 모든 유저 조회 (팔로워 목록)
    List<Follow> findByFollowingId(UUID followingId);
    // 팔로우 관계 존재 여부 확인 (중복 팔로우 방지, 언팔로우 검증)
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    // 팔로잉 수 (내가 팔로우하는 수)
    long countByFollowerId(UUID followerId);
    // 팔로워 수 (나를 팔로우하는 수)
    long countByFollowingId(UUID followingId);
}
