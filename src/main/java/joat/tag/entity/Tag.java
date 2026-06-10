package joat.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // UUID v7 기반 기본키

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 소문자 정규화된 태그명 (예: "갓생루틴"), 중복 불가

    @Column(nullable = false)
    private int postCount; // 이 태그가 사용된 게시물 수 (트렌딩 정렬에 사용)

    public static Tag of(String name) {
        Tag tag = new Tag();
        tag.name = name;
        return tag;
    }

    public void incrementPostCount() {
        this.postCount++;
    }
}
