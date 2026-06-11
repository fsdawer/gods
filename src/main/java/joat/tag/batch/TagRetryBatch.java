package joat.tag.batch;

import joat.feed.entity.Post;
import joat.feed.entity.TagStatus;
import joat.feed.repository.PostRepository;
import joat.feed.service.PostService;
import joat.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 태그 처리 실패 포스트 배치 재처리기.
 * tag_status = FAILED 인 포스트를 매일 새벽 3시에 재처리한다.
 *
 * 처리 흐름:
 * 1. PostRepository로 tag_status = FAILED 포스트 전체 조회
 * 2. pending_tag_names가 없으면 건너뜀 (재처리할 태그 정보 없음)
 * 3. TagService.processTags() 재호출 → 성공 시 PostService.markTagDone 호출
 * 4. 실패 시 로그만 남기고 다음 포스트로 진행 (한 건 실패가 전체를 블락하지 않음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagRetryBatch {

    private final PostRepository postRepository;
    private final TagService tagService;
    private final PostService postService;

    /**
     * 새벽 3시 배치 실행 — FAILED 포스트 태그 재처리.
     * 각 포스트는 독립적인 트랜잭션(TagService.processTags 내부)으로 처리되므로
     * 한 건 실패가 다른 건에 영향을 주지 않는다.
     * processTags 성공 후 markTagDone을 호출해 tag_status를 DONE으로 전환한다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void retryFailedTags() {
        List<Post> failedPosts = postRepository.findByTagStatus(TagStatus.FAILED);
        if (failedPosts.isEmpty()) return;

        log.info("[TagRetryBatch] {} FAILED post(s) found. Starting retry.", failedPosts.size());
        int succeeded = 0;

        for (Post post : failedPosts) {
            String[] pending = post.getPendingTagNames();
            if (pending == null || pending.length == 0) {
                log.warn("[TagRetryBatch] postId={} has no pending tag names, skipping.", post.getId());
                continue;
            }
            try {
                tagService.processTags(post.getId(), Arrays.asList(pending));
                postService.markTagDone(post.getId());
                succeeded++;
            } catch (Exception e) {
                log.error("[TagRetryBatch] Retry failed for postId={}: {}", post.getId(), e.getMessage());
            }
        }

        log.info("[TagRetryBatch] Done. {}/{} succeeded.", succeeded, failedPosts.size());
    }
}
