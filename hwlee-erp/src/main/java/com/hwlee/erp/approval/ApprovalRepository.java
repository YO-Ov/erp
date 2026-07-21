package com.hwlee.erp.approval;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    Optional<Approval> findByNumber(String number);

    /** 상신함 — 내가 올린 결재 문서. */
    Page<Approval> findByCreatedByOrderByIdDesc(String createdBy, Pageable pageable);

    /**
     * 상신함(기간) — 내가 올린 결재 중 상신일({@code requestedAt})이 기간 내인 것.
     *
     * <p>아직 상신하지 않은 DRAFT 는 {@code requestedAt} 이 null 이라 자연히 빠진다.
     * ("이번 달 상신한 결재" 라는 물음의 뜻에 맞다 — 작성만 해 둔 건 상신한 게 아니다.)
     */
    Page<Approval> findByCreatedByAndRequestedAtBetweenOrderByIdDesc(
            String createdBy, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * 결재함 — 내가 지금 처리할 차례인 문서(PENDING).
     * 순차 결재(현재 단계이면서 내 것)거나, 병렬 합의(내 것이고 미처리)인 단계가 있는 문서.
     */
    @Query("""
            select distinct a from Approval a join a.steps s
            where a.status = com.hwlee.erp.approval.ApprovalStatus.PENDING
              and s.approver = :username
              and s.status = com.hwlee.erp.approval.ApprovalStepStatus.PENDING
              and (
                    (s.type = com.hwlee.erp.approval.ApprovalStepType.APPROVAL and s.stepNo = a.currentStep)
                 or  s.type = com.hwlee.erp.approval.ApprovalStepType.AGREEMENT
              )
            order by a.id desc
            """)
    Page<Approval> findInboxOf(@Param("username") String username, Pageable pageable);

    /** 한 원본 문서(예: 견적)에 걸린 결재들 — 최신순. 중복 상신 차단·현재 상태 조회용. */
    List<Approval> findByDocTypeAndRefIdOrderByIdDesc(ApprovalDocType docType, Long refId);

    /** 여러 원본 문서의 결재들 — 최신순. 목록 화면에서 결재 상태를 배치로 덧입힐 때 쓴다(N+1 방지). */
    List<Approval> findByDocTypeAndRefIdInOrderByIdDesc(ApprovalDocType docType, Collection<Long> refIds);
}
