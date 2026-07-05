package com.hwlee.erp.approval.dto;

/**
 * 결재 처리(승인/반려/반송) 요청 — 의견(comment)은 선택. 반려/반송 시 사유로 쓰인다.
 */
public record ApprovalActionRequest(String comment) {
}
