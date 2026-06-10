package joat.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "post_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(PostTagId.class)
public class PostTag {

    @Id
    @Column(name = "post_id")
    private UUID postId; // 태그가 붙은 게시물 UUID

    @Id
    @Column(name = "tag_id")
    private UUID tagId; // 연결된 태그 UUID

    public static PostTag of(UUID postId, UUID tagId) {
        PostTag postTag = new PostTag();
        postTag.postId = postId;
        postTag.tagId = tagId;
        return postTag;
    }
}
