package com.commercecore.global.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 테이블로 생성되지 않고, 상속받는 엔티티에 필드만 제공
@EntityListeners(AuditingEntityListener.class) // 엔티티 변화(생성/수정)를 감지하여 시간 기록
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 나중에 수정일이 필요하면 아래 주석 풀기
    // @LastModifiedDate
    // private LocalDateTime updatedAt;
}