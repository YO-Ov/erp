package com.hwlee.erp.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 전자결재 상태머신 단위 검증 — 순차 결재, 병렬 합의, 반려, 반송/재상신, 회수.
 * DB·조직 데이터 없이 도메인 로직만 본다.
 */
class ApprovalTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 7, 5, 10, 0);

    private Approval draftWith(ApprovalStep... steps) {
        Approval a = Approval.create("APV-20260705-001", ApprovalDocType.QUOTATION, 1L, "Q-1",
                "견적 발송 승인", new BigDecimal("1000000"));
        for (ApprovalStep s : steps) a.addStep(s);
        return a;
    }

    private ApprovalStep approval(int no, String who) {
        return ApprovalStep.of(no, ApprovalStepType.APPROVAL, who, who, "부서");
    }

    private ApprovalStep agreement(int no, String who) {
        return ApprovalStep.of(no, ApprovalStepType.AGREEMENT, who, who, "재무팀");
    }

    private ApprovalStep reference(int no, String who) {
        return ApprovalStep.of(no, ApprovalStepType.REFERENCE, who, who, "본부");
    }

    @Test
    @DisplayName("순차 결재: 1단계 승인 후 차례가 2단계로 넘어가고, 마지막 승인 시 APPROVED")
    void 순차_결재_완료() {
        Approval a = draftWith(approval(1, "mgr"), approval(2, "dir"));
        a.submit(T);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(a.getCurrentStep()).isEqualTo(1);

        a.act("mgr", ApprovalAction.APPROVE, null, T);
        assertThat(a.getCurrentStep()).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.PENDING);

        a.act("dir", ApprovalAction.APPROVE, null, T);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(a.getDecidedAt()).isEqualTo(T);
    }

    @Test
    @DisplayName("차례가 아닌 결재자는 처리할 수 없다")
    void 차례_아니면_거부() {
        Approval a = draftWith(approval(1, "mgr"), approval(2, "dir"));
        a.submit(T);
        assertThatThrownBy(() -> a.act("dir", ApprovalAction.APPROVE, null, T))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차례가 아닙니다");
    }

    @Test
    @DisplayName("반려하면 문서 전체가 REJECTED 로 종결된다")
    void 반려_종결() {
        Approval a = draftWith(approval(1, "mgr"), approval(2, "dir"));
        a.submit(T);
        a.act("mgr", ApprovalAction.REJECT, "금액 과다", T);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    @DisplayName("반송하면 DRAFT 로 되돌아가고 단계가 초기화되며, 재상신하면 처음부터 다시 진행")
    void 반송_후_재상신() {
        Approval a = draftWith(approval(1, "mgr"), approval(2, "dir"));
        a.submit(T);
        a.act("mgr", ApprovalAction.APPROVE, null, T);   // 1단계 통과
        a.act("dir", ApprovalAction.RETURN, "수정 필요", T);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(a.getReturnReason()).isEqualTo("수정 필요");
        assertThat(a.getSteps()).allMatch(s -> s.getStatus() == ApprovalStepStatus.PENDING);

        a.submit(T);   // 재상신
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(a.getCurrentStep()).isEqualTo(1);   // 다시 1단계부터
    }

    @Test
    @DisplayName("합의(병렬)는 순차 승인과 별개로 처리되며, 승인·합의가 모두 끝나야 APPROVED")
    void 병렬_합의() {
        Approval a = draftWith(approval(1, "mgr"), agreement(2, "fin"));
        a.submit(T);
        // 합의를 먼저 처리해도 됨(병렬)
        a.act("fin", ApprovalAction.APPROVE, null, T);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.PENDING);   // 승인 단계가 남음
        a.act("mgr", ApprovalAction.APPROVE, null, T);
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.APPROVED);  // 둘 다 끝 → 완료
    }

    @Test
    @DisplayName("합의자는 반송할 수 없다(반려만 가능)")
    void 합의자_반송불가() {
        Approval a = draftWith(approval(1, "mgr"), agreement(2, "fin"));
        a.submit(T);
        assertThatThrownBy(() -> a.act("fin", ApprovalAction.RETURN, null, T))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("반송할 수 없습니다");
    }

    @Test
    @DisplayName("참조(REFERENCE)는 결재권이 없어 완료 판정에 무관하다 — 결재만 끝나면 APPROVED")
    void 참조는_완료판정_무관() {
        Approval a = draftWith(approval(1, "mgr"), approval(2, "dir"), reference(3, "boss"));
        a.submit(T);
        a.act("mgr", ApprovalAction.APPROVE, null, T);
        a.act("dir", ApprovalAction.APPROVE, null, T);
        // 참조자(boss)가 아무것도 안 해도 결재는 완료된다.
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("참조자는 결재/합의할 수 없다(열람만)")
    void 참조자는_결재불가() {
        Approval a = draftWith(approval(1, "mgr"), reference(2, "boss"));
        a.submit(T);
        assertThatThrownBy(() -> a.act("boss", ApprovalAction.APPROVE, null, T))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차례가 아닙니다");
    }

    @Test
    @DisplayName("처리 전에는 상신자가 회수할 수 있으나, 한 단계라도 처리되면 회수 불가")
    void 회수_규칙() {
        Approval a = draftWith(approval(1, "mgr"), approval(2, "dir"));
        a.submit(T);
        Approval b = draftWith(approval(1, "mgr"), approval(2, "dir"));
        b.submit(T);
        b.act("mgr", ApprovalAction.APPROVE, null, T);

        a.withdraw();
        assertThat(a.getStatus()).isEqualTo(ApprovalStatus.WITHDRAWN);
        assertThatThrownBy(b::withdraw)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 결재가 진행된");
    }
}
