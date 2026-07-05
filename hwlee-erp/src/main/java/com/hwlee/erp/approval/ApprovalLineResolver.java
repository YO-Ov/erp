package com.hwlee.erp.approval;

import com.hwlee.erp.master.department.Department;
import com.hwlee.erp.master.department.DepartmentRepository;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.security.user.AppUser;
import com.hwlee.erp.security.user.AppUserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 결재선 자동 구성기 — 전결 규정 + 조직도 트리로 결재 단계 목록을 만든다.
 *
 * <p>① 상신자 부서에서 조직 트리를 타고 올라가며 각 노드의 부서장을 순차 결재(APPROVAL)
 * 단계로 모은다(전결 레벨이 정한 결재자 수만큼). ② 규정이 재무 합의를 요구하면 재무팀장을
 * 병렬 합의(AGREEMENT) 단계로 덧붙인다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalLineResolver {

    private static final String FINANCE_DEPT_CODE = "DEPT-FINANCE";

    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * 결재선 생성. 결과가 비어 있으면(=적용할 결재자를 못 찾음) 호출 측이 상신을 막아야 한다.
     */
    public List<ApprovalStep> resolve(String requester, ApprovalRule rule) {
        AppUser user = appUserRepository.findByUsername(requester)
                .orElseThrow(() -> new IllegalStateException("상신자 계정을 찾을 수 없습니다: " + requester));
        Department start = user.getEmployee().getDepartment();

        List<ApprovalStep> steps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int stepNo = 1;

        // ⓞ 고정 결재 부서(예: 여신=재무팀장): 상신자 조직과 무관하게 지정 부서장이 결재.
        //    지정 부서장이 없거나 상신자 본인이면 상위로 밀린다.
        if (rule.getFixedApproverDeptCode() != null && !rule.getFixedApproverDeptCode().isBlank()) {
            Department fixed = departmentRepository.findByCode(rule.getFixedApproverDeptCode()).orElse(null);
            for (Department d = fixed; d != null && steps.isEmpty(); d = d.getParent()) {
                Employee mgr = d.getManager();
                if (mgr == null || mgr.getEmail().equals(requester)) continue;
                steps.add(ApprovalStep.of(1, ApprovalStepType.APPROVAL, mgr.getEmail(), mgr.getName(), d.getName()));
            }
            addReferenceIfAny(steps, rule, requester);
            return steps;
        }

        // ① 순차 결재: 조직 트리 상향, 각 노드 부서장을 결재자로(자기 자신·중복·미지정 제외).
        int target = rule.getApprovalLevel().targetApprovers();
        int approverCount = 0;
        for (Department d = start; d != null && approverCount < target; d = d.getParent()) {
            Employee mgr = d.getManager();
            if (mgr == null) continue;                       // 부서장 미지정 → 상위로
            String email = mgr.getEmail();
            if (email.equals(requester)) continue;           // 자기 자신 → 상위로(팀장 상신 시 본부장부터)
            if (!seen.add(email)) continue;                  // 중복 결재자 방지
            steps.add(ApprovalStep.of(stepNo++, ApprovalStepType.APPROVAL, email, mgr.getName(), d.getName()));
            approverCount++;
        }

        // ② 병렬 합의: 재무 통제가 필요한 고액 건은 재무팀장을 합의자로.
        if (rule.isRequireFinanceAgreement()) {
            departmentRepository.findByCode(FINANCE_DEPT_CODE).ifPresent(fin -> {
                Employee fm = fin.getManager();
                if (fm != null && !fm.getEmail().equals(requester) && !seen.contains(fm.getEmail())) {
                    steps.add(ApprovalStep.of(steps.size() + 1, ApprovalStepType.AGREEMENT,
                            fm.getEmail(), fm.getName(), fin.getName()));
                }
            });
        }

        // ③ 참조(열람): 지정된 참조 부서장을 결재권 없는 통보 단계로 덧붙인다.
        addReferenceIfAny(steps, rule, requester);
        return steps;
    }

    /** 규칙에 참조 부서가 지정돼 있으면 그 부서장을 REFERENCE 단계로 추가(자기 자신·이미 포함된 사람 제외). */
    private void addReferenceIfAny(List<ApprovalStep> steps, ApprovalRule rule, String requester) {
        String refCode = rule.getReferenceDeptCode();
        if (refCode == null || refCode.isBlank()) return;
        departmentRepository.findByCode(refCode).ifPresent(d -> {
            Employee mgr = d.getManager();
            if (mgr == null || mgr.getEmail().equals(requester)) return;
            boolean already = steps.stream().anyMatch(s -> s.getApprover().equals(mgr.getEmail()));
            if (!already)
                steps.add(ApprovalStep.of(steps.size() + 1, ApprovalStepType.REFERENCE,
                        mgr.getEmail(), mgr.getName(), d.getName()));
        });
    }

    /** 문서 종류·금액에 맞는 전결 규정을 고른다. 규정이 없으면 null. */
    public ApprovalRule pickRule(List<ApprovalRule> rules, BigDecimal amount) {
        return rules.stream().filter(r -> r.covers(amount)).findFirst().orElse(null);
    }
}
