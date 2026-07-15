<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { workOrderApi, masterApi } from '../api/workOrders'
import { workOrderStatus, allowedActions } from '../domain/status'
import { useWorkOrderStore } from '../stores/workOrders'
import StatusBadge from '../components/StatusBadge.vue'
import QualityInspectionPanel from '../components/QualityInspectionPanel.vue'

const props = defineProps({ id: { type: [String, Number], required: true } })
const router = useRouter()
const store = useWorkOrderStore()

const wo = ref(null)
const loading = ref(false)
const error = ref(null)
const busy = ref(false) // 실행 액션 진행 중 잠금

// 마스터 데이터(시작 시 설비·작업자 선택용)
const equipments = ref([])
const operators = ref([])

// 시작 폼
const startForm = reactive({ equipmentId: '', operatorId: '' })
// 실적 등록 폼
const reportForm = reactive({ goodQty: '', defectQty: '', defectReasonId: '' })
const defectReasons = ref([])

const actions = computed(() => (wo.value ? allowedActions(wo.value.status) : []))
const status = computed(() => workOrderStatus(wo.value?.status))

async function load() {
  loading.value = true
  error.value = null
  try {
    wo.value = await workOrderApi.findById(props.id)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function loadMasters() {
  try {
    ;[equipments.value, operators.value, defectReasons.value] = await Promise.all([
      masterApi.equipments(),
      masterApi.operators(),
      masterApi.defectReasons(),
    ])
  } catch (e) {
    // 마스터 로딩 실패는 치명적이지 않게 — 액션 시 서버가 다시 검증한다.
    console.warn('마스터 데이터 로딩 실패:', e.message)
  }
}

// 액션 실행 공통 래퍼: busy 잠금 + 결과를 화면·목록 store 양쪽에 반영.
async function run(fn) {
  busy.value = true
  error.value = null
  try {
    const updated = await fn()
    wo.value = updated
    store.replaceOne(updated) // 목록 화면도 최신 상태로
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

function doStart() {
  if (!startForm.equipmentId || !startForm.operatorId) {
    error.value = '설비와 작업자를 선택하세요.'
    return
  }
  run(() =>
    workOrderApi.start(props.id, {
      equipmentId: Number(startForm.equipmentId),
      operatorId: Number(startForm.operatorId),
    }),
  )
}
const doPause = () => run(() => workOrderApi.pause(props.id))
const doResume = () => run(() => workOrderApi.resume(props.id))
const doComplete = () => {
  if (!confirm('작업을 완료 처리할까요? 완료 후에는 되돌릴 수 없습니다.')) return
  run(() => workOrderApi.complete(props.id))
}

function doReport() {
  if (reportForm.goodQty === '') {
    error.value = '양품 수량을 입력하세요.'
    return
  }
  run(() =>
    workOrderApi.reportResult(props.id, {
      goodQty: Number(reportForm.goodQty),
      defectQty: reportForm.defectQty === '' ? 0 : Number(reportForm.defectQty),
      defectReasonId: reportForm.defectReasonId ? Number(reportForm.defectReasonId) : null,
    }),
  ).then(() => {
    reportForm.goodQty = ''
    reportForm.defectQty = ''
    reportForm.defectReasonId = ''
  })
}

function fmt(q) {
  return q == null ? '-' : Number(q).toLocaleString('ko-KR')
}
function fmtDateTime(dt) {
  return dt ? dt.replace('T', ' ').slice(0, 19) : '-'
}

// ── 실시간 폴링 ──
// 백엔드 생산 시뮬레이터가 IN_PROGRESS 작업지시를 자동 생산하므로,
// 진행 중일 때는 2.5초마다 조용히(스피너 없이) 다시 읽어 생산량이 차오르는 걸 실시간으로 보여준다.
// 진행 중이 아니면 폴링을 멈춘다(불필요한 호출 방지).
const POLL_MS = 2500
let pollTimer = null

async function silentReload() {
  try {
    const fresh = await workOrderApi.findById(props.id)
    wo.value = fresh
    store.replaceOne(fresh)
  } catch {
    // 폴링 실패는 조용히 무시(다음 틱에 재시도)
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(silentReload, POLL_MS)
}
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 상태가 바뀔 때마다 폴링을 켜고 끈다: IN_PROGRESS 일 때만 실시간 갱신.
watch(
  () => wo.value?.status,
  (s) => (s === 'IN_PROGRESS' ? startPolling() : stopPolling()),
)

onMounted(() => {
  load()
  loadMasters()
})
onUnmounted(stopPolling) // 화면 떠날 때 타이머 정리(누수 방지)
</script>

<template>
  <section v-if="wo">
    <div class="page-head">
      <div>
        <RouterLink to="/work-orders" class="muted">← 목록</RouterLink>
        <h1>
          <span class="mono">{{ wo.workOrderNo }}</span>
          <StatusBadge :label="status.label" :tone="status.tone" />
          <span v-if="wo.status === 'IN_PROGRESS'" class="live"><span class="dot"></span>실시간 생산중</span>
        </h1>
      </div>
      <button @click="load()" :disabled="loading || busy">새로고침</button>
    </div>

    <p v-if="error" class="error">⚠ {{ error }}</p>

    <div class="grid">
      <!-- 기본 정보 -->
      <div class="panel">
        <h2>기본 정보</h2>
        <dl class="info">
          <dt>제품</dt>
          <dd>{{ wo.productName }} <span class="muted mono">({{ wo.productCode }})</span></dd>
          <dt>ERP 수주번호</dt>
          <dd class="mono">{{ wo.erpOrderNo || '-' }}</dd>
          <dt>지시 수량</dt>
          <dd>{{ fmt(wo.quantity) }}</dd>
          <dt>계획일</dt>
          <dd>{{ wo.plannedDate || '-' }}</dd>
          <dt>배정 설비 / 작업자</dt>
          <dd class="mono">{{ wo.assignedEquipmentCode || '-' }} / {{ wo.assignedOperatorCode || '-' }}</dd>
          <dt>시작 / 종료</dt>
          <dd>{{ fmtDateTime(wo.startedAt) }} ~ {{ fmtDateTime(wo.finishedAt) }}</dd>
        </dl>
      </div>

      <!-- 생산 실적 -->
      <div class="panel">
        <h2>생산 실적</h2>
        <div class="metrics">
          <div class="metric">
            <div class="metric-num">{{ fmt(wo.producedQty) }}</div>
            <div class="muted">생산</div>
          </div>
          <div class="metric">
            <div class="metric-num" :class="{ bad: Number(wo.defectQty) > 0 }">{{ fmt(wo.defectQty) }}</div>
            <div class="muted">불량</div>
          </div>
          <div class="metric">
            <div class="metric-num">{{ fmt(wo.quantity) }}</div>
            <div class="muted">지시</div>
          </div>
        </div>
        <div class="progress" v-if="Number(wo.quantity) > 0">
          <div
            class="progress-bar"
            :style="{ width: Math.min(100, (Number(wo.producedQty) / Number(wo.quantity)) * 100) + '%' }"
          ></div>
        </div>
      </div>
    </div>

    <!-- 소요 자재(BOM 라인) -->
    <div class="panel" v-if="wo.lines && wo.lines.length">
      <h2>소요 자재</h2>
      <table>
        <thead>
          <tr><th>#</th><th>부품</th><th>소요량</th><th>단위</th></tr>
        </thead>
        <tbody>
          <tr v-for="l in wo.lines" :key="l.lineNo">
            <td class="muted">{{ l.lineNo }}</td>
            <td>{{ l.componentName }} <span class="muted mono">({{ l.componentCode }})</span></td>
            <td>{{ fmt(l.requiredQty) }}</td>
            <td class="muted">{{ l.unit }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 현장 실행 액션 -->
    <div class="panel exec">
      <h2>현장 실행</h2>

      <p v-if="actions.length === 0" class="muted">
        현재 상태(<b>{{ status.label }}</b>)에서는 가능한 실행 동작이 없습니다.
      </p>

      <!-- 시작: 설비·작업자 선택 -->
      <div v-if="actions.includes('start')" class="action-row">
        <select v-model="startForm.equipmentId">
          <option value="">설비 선택</option>
          <option v-for="e in equipments" :key="e.id" :value="e.id">
            {{ e.name }} ({{ e.code }})
          </option>
        </select>
        <select v-model="startForm.operatorId">
          <option value="">작업자 선택</option>
          <option v-for="o in operators" :key="o.id" :value="o.id">
            {{ o.name }} ({{ o.code }})
          </option>
        </select>
        <button class="primary" @click="doStart" :disabled="busy">작업 시작</button>
      </div>

      <div v-if="actions.includes('pause')" class="action-row">
        <button @click="doPause" :disabled="busy">일시정지</button>
      </div>
      <div v-if="actions.includes('resume')" class="action-row">
        <button class="primary" @click="doResume" :disabled="busy">작업 재개</button>
      </div>

      <!-- 실적 등록 -->
      <div v-if="actions.includes('report')" class="action-row report">
        <input v-model="reportForm.goodQty" type="number" min="0" placeholder="양품 수량" />
        <input v-model="reportForm.defectQty" type="number" min="0" placeholder="불량 수량" />
        <select v-model="reportForm.defectReasonId">
          <option value="">불량사유(선택)</option>
          <option v-for="d in defectReasons" :key="d.id" :value="d.id">{{ d.name }}</option>
        </select>
        <button @click="doReport" :disabled="busy">실적 등록</button>
      </div>

      <div v-if="actions.includes('complete')" class="action-row">
        <button class="primary" @click="doComplete" :disabled="busy">작업 완료</button>
      </div>
    </div>

    <!-- 품질검사 -->
    <QualityInspectionPanel :work-order-id="wo.id" :defect-reasons="defectReasons" />
  </section>

  <section v-else-if="loading" class="muted">불러오는 중…</section>
  <section v-else-if="error" class="error">⚠ {{ error }}</section>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
h1 {
  font-size: 22px;
  margin: 8px 0 0;
  display: flex;
  align-items: center;
  gap: 12px;
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
h2 {
  font-size: 15px;
  margin: 0 0 14px;
  color: var(--text-muted);
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.panel {
  margin-bottom: 16px;
}
.info {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 8px 12px;
  margin: 0;
}
.info dt {
  color: var(--text-muted);
}
.info dd {
  margin: 0;
}
.metrics {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}
.metric-num {
  font-size: 26px;
  font-weight: 700;
}
.metric-num.bad {
  color: var(--tone-danger);
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
.exec .action-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
select,
input {
  font: inherit;
  background: var(--bg-elevated);
  color: var(--text);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
}
input[type='number'] {
  width: 130px;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
