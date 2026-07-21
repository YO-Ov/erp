package com.hwlee.erp.master.employee;

/** 부서별 인원 수 집계 프로젝션 — 인사 대시보드용. */
public interface DeptHeadcount {
    /** 부서명. */
    String getName();

    /** 그 부서 소속 사원 수. */
    long getCount();
}
