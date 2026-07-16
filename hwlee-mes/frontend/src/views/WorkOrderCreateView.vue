<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { workOrderApi } from '../api/workOrders'
import { useWorkOrderStore } from '../stores/workOrders'

const router = useRouter()
const store = useWorkOrderStore()

// 작업지시 헤더 + 소요자재(BOM) 라인.
// lines 는 배열이고, v-for + push/splice 로 행을 동적으로 늘리고 줄인다(Vue 반응성).
const form = reactive({
  erpOrderNo: '',
  productCode: '',
  productName: '',
  quantity: '',
  plannedDate: '',
  lines: [{ componentCode: '', componentName: '', requiredQty: '', unit: 'EA' }],
})

const submitting = ref(false)
const error = ref(null)

function addLine() {
  form.lines.push({ componentCode: '', componentName: '', requiredQty: '', unit: 'EA' })
}
function removeLine(idx) {
  form.lines.splice(idx, 1)
}

function validate() {
  if (!form.erpOrderNo.trim()) return 'ERP 수주번호를 입력하세요.'
  if (!form.productCode.trim()) return '제품코드를 입력하세요.'
  if (!form.productName.trim()) return '제품명을 입력하세요.'
  if (!form.quantity || Number(form.quantity) <= 0) return '수량은 1 이상이어야 합니다.'
  // 부분 입력된 자재 라인 방지
  for (const [i, l] of form.lines.entries()) {
    const any = l.componentCode || l.componentName || l.requiredQty
    if (any && (!l.componentCode.trim() || !l.componentName.trim() || !(Number(l.requiredQty) > 0))) {
      return `${i + 1}번 자재 라인의 코드·이름·소요량을 모두 채우세요.`
    }
  }
  return null
}

async function submit() {
  const msg = validate()
  if (msg) {
    error.value = msg
    return
  }
  submitting.value = true
  error.value = null
  try {
    // 완전히 채워진 자재 라인만 전송.
    // ⚠️ 백엔드 WorkOrderReceiveRequest 의 필드명은 `components` (응답 DTO 는 `lines`).
    const components = form.lines
      .filter((l) => l.componentCode.trim() && l.componentName.trim() && Number(l.requiredQty) > 0)
      .map((l) => ({
        componentCode: l.componentCode.trim(),
        componentName: l.componentName.trim(),
        requiredQty: Number(l.requiredQty),
        unit: l.unit || 'EA',
      }))

    const created = await workOrderApi.receive({
      erpOrderNo: form.erpOrderNo.trim(),
      productCode: form.productCode.trim(),
      productName: form.productName.trim(),
      quantity: Number(form.quantity),
      plannedDate: form.plannedDate || null,
      components,
    })
    await store.fetchAll() // 목록 갱신
    router.push(`/work-orders/${created.id}`) // 방금 만든 상세로 이동
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <RouterLink to="/work-orders" class="muted">← 목록</RouterLink>
        <h1>작업지시 접수</h1>
      </div>
    </div>

    <p v-if="error" class="error">⚠ {{ error }}</p>

    <form @submit.prevent="submit">
      <div class="panel">
        <h2>기본 정보</h2>
        <div class="fields">
          <label>
            ERP 수주번호 *
            <input v-model="form.erpOrderNo" placeholder="예: PO-20260716-001" />
          </label>
          <label>
            계획일
            <input v-model="form.plannedDate" type="date" />
          </label>
          <label>
            제품코드 *
            <input v-model="form.productCode" placeholder="예: ITEM-2026-0001" />
          </label>
          <label>
            제품명 *
            <input v-model="form.productName" placeholder="예: hyunwoo 노트북 15&quot;" />
          </label>
          <label>
            수량 *
            <input v-model="form.quantity" type="number" min="1" placeholder="예: 50" />
          </label>
        </div>
      </div>

      <div class="panel">
        <div class="lines-head">
          <h2>소요 자재 (BOM)</h2>
          <button type="button" @click="addLine">+ 자재 추가</button>
        </div>
        <table class="only-desktop">
          <thead>
            <tr>
              <th style="width: 28%">부품코드</th>
              <th style="width: 34%">부품명</th>
              <th style="width: 16%">소요량</th>
              <th style="width: 14%">단위</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(l, idx) in form.lines" :key="idx">
              <td><input v-model="l.componentCode" placeholder="ITEM-…" /></td>
              <td><input v-model="l.componentName" placeholder="부품명" /></td>
              <td><input v-model="l.requiredQty" type="number" min="0" /></td>
              <td><input v-model="l.unit" placeholder="EA" /></td>
              <td>
                <button type="button" class="del" @click="removeLine(idx)" :disabled="form.lines.length === 1">
                  ✕
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 좁은 화면: 입력칸 5개짜리 표는 폰에서 못 쓴다 → 라인마다 블록으로 -->
        <div class="line-cards">
          <div v-for="(l, idx) in form.lines" :key="idx" class="line-card">
            <div class="line-card-head">
              <span class="muted">자재 {{ idx + 1 }}</span>
              <button type="button" class="del" @click="removeLine(idx)" :disabled="form.lines.length === 1">
                ✕
              </button>
            </div>
            <label>
              부품코드
              <input v-model="l.componentCode" placeholder="ITEM-…" />
            </label>
            <label>
              부품명
              <input v-model="l.componentName" placeholder="부품명" />
            </label>
            <div class="line-card-row">
              <label>
                소요량
                <input v-model="l.requiredQty" type="number" min="0" />
              </label>
              <label>
                단위
                <input v-model="l.unit" placeholder="EA" />
              </label>
            </div>
          </div>
        </div>
        <p class="muted hint">자재 라인은 선택 사항입니다. 비워두면 자재 없이 접수됩니다.</p>
      </div>

      <div class="form-actions">
        <RouterLink to="/work-orders"><button type="button">취소</button></RouterLink>
        <button type="submit" class="primary" :disabled="submitting">
          {{ submitting ? '접수 중…' : '작업지시 접수' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.page-head {
  margin-bottom: 20px;
}
h1 {
  font-size: 22px;
  margin: 8px 0 0;
}
h2 {
  font-size: 15px;
  margin: 0 0 14px;
  color: var(--text-muted);
}
.panel {
  margin-bottom: 16px;
}
.fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
}
input {
  font: inherit;
  background: var(--bg-elevated);
  color: var(--text);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 9px 12px;
}
input[type='number'] {
  width: 100%;
}
.lines-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.lines-head h2 {
  margin: 0;
}
td input {
  width: 100%;
}
.del {
  padding: 6px 10px;
  color: var(--tone-danger);
}
.hint {
  margin: 12px 0 0;
  font-size: 12px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
.form-actions a:hover {
  text-decoration: none;
}
/* ── 모바일 자재 입력 블록 (640px 이하에서만 켠다 — 아래 미디어쿼리) ── */
.line-cards {
  display: none;
  flex-direction: column;
  gap: 12px;
}
.line-card {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.line-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}
.line-card-row {
  display: flex;
  gap: 10px;
}
.line-card-row label {
  flex: 1;
  min-width: 0;
}
.line-card input {
  width: 100%;
}

@media (max-width: 640px) {
  .line-cards {
    display: flex;
  }
  .fields {
    grid-template-columns: 1fr;
  }
  .lines-head {
    gap: 10px;
  }
  /* 접수·취소는 폭을 나눠 갖게 — 오른쪽 끝에 몰리면 한 손 조작이 어렵다 */
  .form-actions {
    justify-content: stretch;
  }
  .form-actions > * {
    flex: 1;
  }
  .form-actions button {
    width: 100%;
  }
}
</style>
