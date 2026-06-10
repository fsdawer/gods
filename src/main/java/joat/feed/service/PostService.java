package joat.feed.service;

import joat.feed.dto.CreatePostRequest;
import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.feed.dto.UpdatePostRequest;

import java.util.List;
import java.util.UUID;

/**
 * 피드(게시물) 서비스 인터페이스.
 * 포스트 생성/조회/삭제, 좋아요, 팔로잉 피드/탐색 피드를 담당한다.
 */
public interface PostService {

    /**
     * 포스트 생성 (자유 포스트 또는 투두 인증 포스트).
     *
     * @param userId 작성자 UUID
     * @param req    내용, 이미지 URL 목록, 해시태그 목록, todoId(인증 포스트만)
     * @return 생성된 PostResponse
     */
    PostResponse createPost(UUID userId, CreatePostRequest req);

    /**
     * 팔로잉 기반 홈 피드 조회 (커서 기반 페이지네이션).
     *
     * @param userId 조회자 UUID
     * @param limit  페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getFeed(UUID userId, int limit);

    /**
     * 전체 탐색 피드 조회 (최신순, 커서 기반).
     *
     * @param limit 페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getExplore(int limit);

    /**
     * 포스트 단건 조회.
     *
     * @param postId 조회할 포스트 UUID
     * @return PostResponse
     */
    PostResponse getPost(UUID postId);

    /**
     * 포스트 삭제 (본인 포스트만 가능).
     *
     * @param postId      삭제할 포스트 UUID
     * @param userId 요청자 UUID (소유자 검증용)
     */
    void deletePost(UUID postId, UUID userId);

    /**
     * 좋아요 추가.
     *
     * @param postId 대상 포스트 UUID
     * @param userId 좋아요 누르는 유저 UUID
     */
    void like(UUID postId, UUID userId);

    /**
     * 좋아요 취소.
     *
     * @param postId 대상 포스트 UUID
     * @param userId 좋아요 취소 유저 UUID
     */
    void unlike(UUID postId, UUID userId);

    /**
     * 특정 유저의 게시물 수 조회 (프로필 통계용).
     *
     * @param userId 조회할 유저 UUID
     * @return 게시물 수
     */
    long countPosts(UUID userId);

    /**
     * 내 게시물 목록 조회 (커서 기반 페이지네이션).
     *
     * @param userId 조회자 UUID
     * @param cursor 이전 페이지 마지막 포스트 UUID (첫 조회 시 null)
     * @param limit  페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getMyPosts(UUID userId, UUID cursor, int limit);

    /**
     * 특정 유저의 공개 게시물 목록 조회 (커서 기반 페이지네이션).
     * 타인 프로필 화면에서 해당 유저의 포스트를 조회할 때 사용한다.
     *
     * @param targetUserId 조회할 유저 UUID
     * @param cursor       이전 페이지 마지막 포스트 UUID (첫 조회 시 null)
     * @param limit        페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getUserPosts(UUID targetUserId, UUID cursor, int limit);

    /**
     * 게시물 수정 (소유자만 가능).
     *
     * @param postId  수정할 포스트 UUID
     * @param userId  요청자 UUID (소유자 검증용)
     * @param req     수정할 content, imageUrls, tagNames
     * @return 수정된 PostResponse
     */
    PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest req);

    /**
     * 특정 태그가 달린 게시물 목록 조회 (커서 기반 페이지네이션).
     *
     * @param tagName 태그 이름 (소문자 정규화됨)
     * @param cursor  이전 페이지 마지막 포스트 UUID (첫 조회 시 null)
     * @param limit   페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getPostsByTag(String tagName, UUID cursor, int limit);

    /**
     * 특정 유저 목록의 게시물 피드 조회 (팀 피드용, 커서 기반).
     *
     * @param memberIds 조회할 유저 UUID 목록
     * @param cursor    이전 페이지 마지막 포스트 UUID (첫 조회 시 null)
     * @param limit     페이지당 포스트 수
     * @return CursorResponse(data, nextCursor, hasNext)
     */
    CursorResponse<PostResponse> getFeedForMembers(List<UUID> memberIds, UUID cursor, int limit);

    /**
     * 팀 오너 권한으로 포스트 강제 삭제.
     * 소유자 여부와 관계없이 포스트를 삭제한다. 팀 오너 검증은 호출자가 담당한다.
     *
     * @param postId 삭제할 포스트 UUID
     */
    void forceDeletePost(UUID postId);

    /**
     * 태그 처리 실패 마킹 — TagDlqConsumer가 DLQ 이벤트 수신 시 호출.
     * tag_status를 FAILED로 전환하고 pending_tag_names를 저장한다.
     * TagRetryBatch가 이 데이터를 읽어 재처리를 시도한다.
     *
     * @param postId   대상 포스트 UUID
     * @param tagNames 재처리에 필요한 태그명 목록
     */
    void markTagFailed(UUID postId, List<String> tagNames);

    /**
     * 태그 처리 완료 마킹 — TagServiceImpl.processTags() 성공 후 호출.
     * tag_status를 DONE으로 전환하고 pending_tag_names를 초기화한다.
     *
     * @param postId 대상 포스트 UUID
     */
    void markTagDone(UUID postId);
}
