package com.hwlee.erp.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 마스터 데이터 공통 베이스 — code + status + deletedAt 를 추가로 가진다.
 *
 * <p>Soft Delete의 어노테이션({@code @SQLDelete}, {@code @SQLRestriction})은
 * 테이블명이 필요하므로 각 자식 엔티티(Customer/Item/...)에서 직접 선언한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntityWithCode extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 30, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MasterStatus status = MasterStatus.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected void assignCode(String code) {
        if (this.code != null) {
            throw new IllegalStateException("code 는 한 번만 할당할 수 있다. 기존: " + this.code);
        }
        this.code = code;
    }

    public void changeStatus(MasterStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("status 는 null 일 수 없다.");
        }
        this.status = newStatus;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
