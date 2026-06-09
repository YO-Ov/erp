package com.hwlee.mes.workorder;

/**
 * 작업지시 상태.
 *
 * <pre>
 * RECEIVED ──start──▶ IN_PROGRESS ──complete──▶ COMPLETED
 *                       │    ▲
 *                    pause  resume
 *                       ▼    │
 *                      PAUSED
 *  (RECEIVED/IN_PROGRESS/PAUSED 에서 cancel → CANCELLED)
 * </pre>
 */
public enum WorkOrderStatus {
    RECEIVED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED
}
