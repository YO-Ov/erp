package com.hwlee.erp.fi.journal;

/**
 * 전표 상태.
 *
 * <ul>
 *   <li>{@link #DRAFT} — 생성 직후, 라인 추가 가능. 차/대 검증 전.</li>
 *   <li>{@link #POSTED} — {@link JournalEntry#post} 가 차/대 균형을 검증하고 확정. 원장에 반영됨.</li>
 *   <li>{@link #CANCELLED} — 취소. 실무 회계는 직접 수정 대신 역분개를 추가하지만,
 *       헤더 상태도 같이 바꿔서 원본 전표가 "취소된 사실" 을 드러낸다 (append-only + 마커).</li>
 * </ul>
 */
public enum JournalEntryStatus {
    DRAFT,
    POSTED,
    CANCELLED
}
