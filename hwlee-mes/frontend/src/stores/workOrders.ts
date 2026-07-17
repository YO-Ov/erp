import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { workOrderApi } from '../api/workOrders'
import { errorMessage } from '../api/client'
import type { WorkOrder, WorkOrderStatus } from '../types/api'

// 작업지시 목록 상태 관리.
// Pinia 는 Vue 의 반응성(ref/computed)을 그대로 쓴다 —
// store 의 items 가 바뀌면 그걸 참조하는 모든 화면이 자동 갱신된다.
export const useWorkOrderStore = defineStore('workOrders', () => {
  const items = ref<WorkOrder[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 상태별 건수(대시보드 요약 카드용)
  // Partial — 아직 한 건도 없는 상태는 키 자체가 없다(그래서 읽는 쪽에서 `|| 0`).
  const countByStatus = computed<Partial<Record<WorkOrderStatus, number>>>(() => {
    const acc: Partial<Record<WorkOrderStatus, number>> = {}
    for (const wo of items.value) {
      acc[wo.status] = (acc[wo.status] || 0) + 1
    }
    return acc
  })

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      items.value = await workOrderApi.list()
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }

  // 목록 안의 특정 작업지시 한 건을 최신값으로 교체(실행 액션 후 사용).
  function replaceOne(updated: WorkOrder) {
    const idx = items.value.findIndex((w) => w.id === updated.id)
    if (idx >= 0) items.value[idx] = updated
  }

  return { items, loading, error, countByStatus, fetchAll, replaceOne }
})
