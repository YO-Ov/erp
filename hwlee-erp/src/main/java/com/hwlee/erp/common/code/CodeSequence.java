package com.hwlee.erp.common.code;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

/**
 * (prefix, year) 별 다음 발급 번호를 관리하는 시퀀스 테이블.
 *
 * <p>코드 자동 생성 시 비관적 락(SELECT ... FOR UPDATE)으로 행을 점유한 뒤
 * next_number 를 1 증가시켜 번호 중복을 방지한다.
 *
 * <p>BaseEntity 를 상속하지 않는 이유: 이 테이블은 마스터/트랜잭션이 아닌
 * "운영용 카운터" 라서 createdBy 등 audit 4컬럼이 의미가 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "code_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_code_sequence_prefix_year", columnNames = {"prefix", "year"})
)
public class CodeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "prefix", nullable = false, length = 16)
    private String prefix;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "next_number", nullable = false)
    private int nextNumber;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CodeSequence initial(String prefix, int year) {
        CodeSequence seq = new CodeSequence();
        seq.prefix = prefix;
        seq.year = year;
        seq.nextNumber = 1;
        return seq;
    }

    /**
     * 현재 next_number 를 반환하고 1 증가시킨다.
     * 호출 측에서는 반드시 비관적 락으로 행을 점유한 상태여야 한다.
     */
    int issueAndIncrement() {
        int issued = this.nextNumber;
        this.nextNumber = issued + 1;
        return issued;
    }
}
