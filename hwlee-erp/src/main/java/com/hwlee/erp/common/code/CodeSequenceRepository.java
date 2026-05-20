package com.hwlee.erp.common.code;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, Long> {

    /**
     * 비관적 쓰기 락(SELECT ... FOR UPDATE)으로 (prefix, year) 행을 점유한다.
     * 동일 (prefix, year) 를 요청한 다른 트랜잭션은 이 트랜잭션이 끝날 때까지 대기한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CodeSequence s where s.prefix = :prefix and s.year = :year")
    Optional<CodeSequence> findForUpdate(@Param("prefix") String prefix, @Param("year") int year);
}
