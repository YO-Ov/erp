<script setup>
import { onMounted, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useWorkOrderStore } from '../stores/workOrders'
import { workOrderStatus, WORK_ORDER_STATUS } from '../domain/status'
import StatusBadge from '../components/StatusBadge.vue'

const store = useWorkOrderStore()
const { items, loading, error, countByStatus } = storeToRefs(store)

// 요약 카드로 보여줄 상태 순서
const summaryOrder = ['RECEIVED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED']
const summary = computed(() =>
  summaryOrder.map((code) => ({
    code,
    ...WORK_ORDER_STATUS[code],
    count: countByStatus.value[code] || 0,
  })),
)

function fmtQty(q) {
  if (q == null) return '-'
  return Number(q).toLocaleString('ko-KR')
}
function fmtDate(d) {
  return d || '-'
}

onMounted(() => store.fetchAll())
</script>

<template>
  <section>
    <div class="page-head">
      <h1>작업지시</h1>
      <div class="head-actions">
        <RouterLink to="/work-orders/new"><button class="primary">+ 작업지시 접수</button></RouterLink>
        <button @click="store.fetchAll()" :disabled="loading">
          {{ loading ? '불러오는 중…' : '새로고침' }}
        </button>
      </div>
    </div>

    <!-- 상태별 요약 카드 -->
    <div class="summary">
      <div v-for="s in summary" :key="s.code" class="summary-card">
        <div class="summary-count">{{ s.count }}</div>
        <StatusBadge :label="s.label" :tone="s.tone" />
      </div>
    </div>

    <p v-if="error" class="error">⚠ {{ error }}</p>

    <div class="panel">
      <table>
        <thead>
          <tr>
            <th>작업지시번호</th>
            <th>ERP 수주</th>
            <th>제품</th>
            <th>수량</th>
            <th>계획일</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!loading && items.length === 0">
            <td colspan="6" class="muted" style="text-align: center; padding: 32px">
              작업지시가 없습니다.
            </td>
          </tr>
          <tr v-for="wo in items" :key="wo.id">
            <td class="mono">
              <RouterLink :to="`/work-orders/${wo.id}`">{{ wo.workOrderNo }}</RouterLink>
            </td>
            <td class="mono muted">{{ wo.erpOrderNo || '-' }}</td>
            <td>
              {{ wo.productName }}
              <span class="muted mono">({{ wo.productCode }})</span>
            </td>
            <td>{{ fmtQty(wo.quantity) }}</td>
            <td class="muted">{{ fmtDate(wo.plannedDate) }}</td>
            <td>
              <StatusBadge
                :label="workOrderStatus(wo.status).label"
                :tone="workOrderStatus(wo.status).tone"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.head-actions {
  display: flex;
  gap: 10px;
}
.head-actions a:hover {
  text-decoration: none;
}
h1 {
  font-size: 22px;
  margin: 0;
}
.summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.summary-card {
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}
.summary-count {
  font-size: 28px;
  font-weight: 700;
}
@media (max-width: 640px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
