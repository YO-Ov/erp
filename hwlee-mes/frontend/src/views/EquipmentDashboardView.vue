<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { equipmentApi } from '../api/workOrders'
import { equipmentStatus, EQUIPMENT_STATUS, EQUIPMENT_STATUS_CODES } from '../domain/status'
import StatusBadge from '../components/StatusBadge.vue'

const equipments = ref([])
const utilByCode = ref({}) // equipmentCode → utilizationPercent
const loading = ref(false)
const error = ref(null)
const busyId = ref(null)

// 상태별 설비 수 요약
const summary = computed(() =>
  EQUIPMENT_STATUS_CODES.map((code) => ({
    code,
    ...EQUIPMENT_STATUS[code],
    count: equipments.value.filter((e) => e.status === code).length,
  })),
)

async function load() {
  loading.value = true
  error.value = null
  try {
    equipments.value = await equipmentApi.list()
    // 각 설비의 가동률을 병렬로 조회
    const utils = await Promise.all(
      equipments.value.map((e) =>
        equipmentApi.utilization(e.id).catch(() => null),
      ),
    )
    const map = {}
    for (const u of utils) {
      if (u) map[u.equipmentCode] = u.utilizationPercent
    }
    utilByCode.value = map
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function changeStatus(eq, status) {
  if (eq.status === status) return
  busyId.value = eq.id
  error.value = null
  try {
    const newStatus = await equipmentApi.changeStatus(eq.id, status)
    eq.status = newStatus // 반응성: 카드가 즉시 새 상태로 바뀐다
    // 상태가 바뀌면 가동률도 달라지므로 해당 설비만 다시 조회
    const u = await equipmentApi.utilization(eq.id).catch(() => null)
    if (u) utilByCode.value = { ...utilByCode.value, [u.equipmentCode]: u.utilizationPercent }
  } catch (e) {
    error.value = e.message
  } finally {
    busyId.value = null
  }
}

function util(code) {
  const v = utilByCode.value[code]
  return v == null ? null : Number(v)
}

// 가동률만 가볍게 다시 조회(설비 목록은 그대로 두고 게이지 값만 갱신).
async function refreshUtils() {
  const utils = await Promise.all(
    equipments.value.map((e) => equipmentApi.utilization(e.id).catch(() => null)),
  )
  const map = { ...utilByCode.value }
  for (const u of utils) {
    if (u) map[u.equipmentCode] = u.utilizationPercent
  }
  utilByCode.value = map
}

// ── 실시간 폴링 ──
// 가동률(오늘) = 가동시간(RUNNING) ÷ 부하시간(RUNNING+DOWN). 부하시간이 흐르면 값이 변하므로
// (가동 RUNNING 은 올리고, 고장 DOWN 은 내림), 그런 설비가 하나라도 있으면 2.5초마다 다시 읽어
// 게이지가 움직이는 게 눈에 보이게 한다. 대기·정비만 있는 설비는 값이 안 변하므로 폴링을 멈춘다.
const POLL_MS = 2500
let timer = null
const anyLoading = computed(() =>
  equipments.value.some((e) => e.status === 'RUNNING' || e.status === 'DOWN'),
)

function startPolling() {
  if (timer) return
  timer = setInterval(refreshUtils, POLL_MS)
}
function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}
watch(anyLoading, (loading) => (loading ? startPolling() : stopPolling()), { immediate: true })

onMounted(load)
onUnmounted(stopPolling) // 화면 떠날 때 타이머 정리(누수 방지)
</script>

<template>
  <section>
    <div class="page-head">
      <h1>설비 가동현황</h1>
      <div class="head-actions">
        <span v-if="anyLoading" class="live"><span class="dot"></span>실시간 가동률 갱신중</span>
        <button @click="load" :disabled="loading">{{ loading ? '불러오는 중…' : '새로고침' }}</button>
      </div>
    </div>

    <div class="summary">
      <div v-for="s in summary" :key="s.code" class="summary-card">
        <div class="summary-count">{{ s.count }}</div>
        <StatusBadge :label="s.label" :tone="s.tone" />
      </div>
    </div>

    <p v-if="error" class="error">⚠ {{ error }}</p>

    <div class="eq-grid">
      <div v-for="eq in equipments" :key="eq.id" class="eq-card" :class="`ring-${equipmentStatus(eq.status).tone}`">
        <div class="eq-top">
          <div>
            <div class="eq-name">{{ eq.name }}</div>
            <div class="muted mono">{{ eq.code }} · {{ eq.lineName }}</div>
          </div>
          <StatusBadge :label="equipmentStatus(eq.status).label" :tone="equipmentStatus(eq.status).tone" />
        </div>

        <div class="eq-util">
          <div class="util-head">
            <span class="muted">가동률(오늘)</span>
            <span v-if="util(eq.code) != null" class="util-num">{{ util(eq.code).toFixed(1) }}%</span>
            <span v-else class="util-num muted" title="오늘 가동·고장 시간이 아직 없어 집계 전입니다">집계 전</span>
          </div>
          <div class="progress">
            <div class="progress-bar" :style="{ width: Math.min(100, util(eq.code) ?? 0) + '%' }"></div>
          </div>
        </div>

        <div class="eq-actions">
          <button
            v-for="code in EQUIPMENT_STATUS_CODES"
            :key="code"
            class="mini"
            :class="{ on: eq.status === code }"
            :disabled="busyId === eq.id || eq.status === code"
            @click="changeStatus(eq, code)"
          >
            {{ equipmentStatus(code).label }}
          </button>
        </div>
      </div>
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
  gap: 12px;
  align-items: center;
}
.live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--tone-active);
}
.live .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--tone-active);
  animation: pulse 1.2s infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.25;
  }
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
.eq-grid {
  display: grid;
  /* min() — 280px 보다 좁은 기기에서 카드가 화면 밖으로 넘치지 않게 */
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 14px;
}
.eq-card {
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-left-width: 4px;
  border-radius: 12px;
  padding: 16px;
}
.ring-active {
  border-left-color: var(--tone-active);
}
.ring-neutral {
  border-left-color: var(--tone-neutral);
}
.ring-danger {
  border-left-color: var(--tone-danger);
}
.ring-warn {
  border-left-color: var(--tone-warn);
}
.eq-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}
.eq-name {
  font-weight: 600;
  font-size: 15px;
}
.eq-util {
  margin-bottom: 14px;
}
.util-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.util-num {
  font-weight: 700;
}
.progress {
  height: 8px;
  background: var(--bg-elevated);
  border-radius: 999px;
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: var(--tone-active);
  transition: width 0.3s;
}
.eq-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}
.mini {
  padding: 6px 4px;
  font-size: 12px;
  border-radius: 6px;
}
.mini.on {
  background: var(--accent-strong);
  border-color: var(--accent-strong);
  color: #04283a;
  font-weight: 700;
}
@media (max-width: 640px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
  .page-head {
    flex-wrap: wrap;
    gap: 10px;
  }
  /* 상태 버튼 4개는 폰 폭에서 글자가 눌린다 → 2×2 로 */
  .eq-actions {
    grid-template-columns: repeat(2, 1fr);
  }
  .mini {
    padding: 8px 4px;
  }
}
</style>
