// 백엔드 enum(WorkOrderStatus / EquipmentStatus) → 한글 라벨·색상 매핑.
// 코드값은 서버가 내려주는 그대로 두고, 표시에만 이 매핑을 쓴다.

import type { EquipmentStatus, QualityResult, WorkOrderStatus } from '../types/api'

/** StatusBadge 가 지원하는 색상 톤 — CSS 클래스 `tone-*` 와 짝이다. */
export type Tone = 'neutral' | 'active' | 'warn' | 'done' | 'muted' | 'danger'

export interface StatusMeta {
  label: string
  tone: Tone
}

export const WORK_ORDER_STATUS: Record<WorkOrderStatus, StatusMeta> = {
  RECEIVED: { label: '접수', tone: 'neutral' },
  IN_PROGRESS: { label: '진행중', tone: 'active' },
  PAUSED: { label: '일시정지', tone: 'warn' },
  COMPLETED: { label: '완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'muted' },
}

export const EQUIPMENT_STATUS: Record<EquipmentStatus, StatusMeta> = {
  RUNNING: { label: '가동', tone: 'active' },
  IDLE: { label: '대기', tone: 'neutral' },
  DOWN: { label: '고장/정지', tone: 'danger' },
  MAINTENANCE: { label: '정비', tone: 'warn' },
}

export function workOrderStatus(code: WorkOrderStatus | null | undefined): StatusMeta {
  return (code && WORK_ORDER_STATUS[code]) || { label: code || '-', tone: 'neutral' }
}

export function equipmentStatus(code: EquipmentStatus | null | undefined): StatusMeta {
  return (code && EQUIPMENT_STATUS[code]) || { label: code || '-', tone: 'neutral' }
}

// 설비 상태 변경 버튼으로 노출할 전체 상태 목록(순서 고정).
export const EQUIPMENT_STATUS_CODES: EquipmentStatus[] = ['RUNNING', 'IDLE', 'DOWN', 'MAINTENANCE']

export const QUALITY_RESULT: Record<QualityResult, StatusMeta> = {
  PASS: { label: '합격', tone: 'active' },
  FAIL: { label: '불합격', tone: 'danger' },
}
export function qualityResult(code: QualityResult | null | undefined): StatusMeta {
  return (code && QUALITY_RESULT[code]) || { label: code || '-', tone: 'neutral' }
}

/** 현장 실행 액션 — 아래 allowedActions 가 상태별로 가능한 것만 골라준다. */
export type WorkOrderAction = 'start' | 'pause' | 'resume' | 'report' | 'complete'

// 현재 상태에서 어떤 실행 액션이 가능한지 — 백엔드 상태전이 규칙과 일치시킨다.
// RECEIVED ──start──▶ IN_PROGRESS ──complete──▶ COMPLETED
//                       │    ▲
//                    pause  resume
//                       ▼    │
//                      PAUSED
export function allowedActions(code: WorkOrderStatus | null | undefined): WorkOrderAction[] {
  switch (code) {
    case 'RECEIVED':
      return ['start']
    case 'IN_PROGRESS':
      return ['pause', 'report', 'complete']
    case 'PAUSED':
      return ['resume']
    default:
      return []
  }
}
