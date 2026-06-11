package joat.feed.service;

import joat.feed.entity.Post;
import joat.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포스트 저장 전용 헬퍼. REQUIRES_NEW로 즉시 커밋하여
 * 호출 직후 post_tags FK 제약을 충족시킨다.
 *
 * PostServiceImpl이 태그를 동기 처리하려면 포스트가 먼저 DB에 커밋되어야 한다.
 * Spring AOP 한계(자기 호출 시 트랜잭션 미적용)를 우회하기 위해 별도 빈으로 분리했다.
 */
@Service
@RequiredArgsConstructor
public class PostSaveHelper {

    private final PostRepository postRepository;

    /**
     * 포스트를 즉시 커밋하는 저장 메서드.
     * REQUIRES_NEW로 독립 트랜잭션에서 실행되므로 호출 반환 시 포스트가 DB에 확정된다.
     *
     * @param post 저장할 Post 엔티티
     * @return DB에 커밋된 Post 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Post saveAndCommit(Post post) {
        return postRepository.save(post);
    }
}
