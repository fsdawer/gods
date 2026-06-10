package joat.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 JPA 엔티티가 상속받는 기반 클래스.
 * Spring Data JPA Auditing을 이용해 createdAt, updatedAt을 자동 관리한다.
 * JpaAuditingConfig에서 @EnableJpaAuditing이 활성화되어 있어야 동작한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 엔티티 최초 저장 시각 (수정 불가)

    @LastModifiedDate
    private LocalDateTime updatedAt; // 엔티티 마지막 수정 시각
}
