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

    <div class="panel only-desktop">
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

    <!-- 좁은 화면: 6컬럼 표는 폰에서 못 읽는다 → 행을 카드로 -->
    <div class="wo-cards">
      <p v-if="!loading && items.length === 0" class="panel muted empty">작업지시가 없습니다.</p>
      <RouterLink v-for="wo in items" :key="wo.id" :to="`/work-orders/${wo.id}`" class="wo-card">
        <div class="wo-card-top">
          <span class="mono wo-no">{{ wo.workOrderNo }}</span>
          <StatusBadge
            :label="workOrderStatus(wo.status).label"
            :tone="workOrderStatus(wo.status).tone"
          />
        </div>
        <div class="wo-product">
          {{ wo.productName }}
          <span class="muted mono">({{ wo.productCode }})</span>
        </div>
        <dl class="wo-meta">
          <div><dt>수량</dt><dd>{{ fmtQty(wo.quantity) }}</dd></div>
          <div><dt>계획일</dt><dd>{{ fmtDate(wo.plannedDate) }}</dd></div>
          <div><dt>ERP 수주</dt><dd class="mono">{{ wo.erpOrderNo || '-' }}</dd></div>
        </dl>
      </RouterLink>
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

/* ── 모바일 카드 목록 (640px 이하에서만 켠다 — 아래 미디어쿼리) ── */
.wo-cards {
  display: none;
  flex-direction: column;
  gap: 10px;
}
.wo-cards .empty {
  text-align: center;
}
.wo-card {
  display: block;
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  color: var(--text);
}
.wo-card:hover {
  text-decoration: none;
  border-color: var(--accent);
}
.wo-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.wo-no {
  color: var(--accent);
  font-weight: 600;
}
.wo-product {
  margin-bottom: 12px;
}
/* 라벨 위 / 값 아래로 쌓아 좁은 폭에서도 줄바꿈이 지저분해지지 않게 한다. */
.wo-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin: 0;
}
.wo-meta dt {
  color: var(--text-muted);
  font-size: 12px;
}
.wo-meta dd {
  margin: 0;
}

@media (max-width: 640px) {
  .wo-cards {
    display: flex;
  }
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
  .page-head {
    flex-wrap: wrap;
    gap: 12px;
  }
  .head-actions {
    width: 100%;
  }
  /* 폰에서는 두 버튼이 폭을 반씩 나눠 갖게 — 잘리지 않고 터치 타깃도 커진다 */
  .head-actions > * {
    flex: 1;
  }
  .head-actions button {
    width: 100%;
  }
}
</style>
