<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { workOrderApi } from '../api/workOrders'
import { errorMessage } from '../api/client'
import { qualityResult } from '../domain/status'
import StatusBadge from './StatusBadge.vue'
import type { DefectReason, QualityInspection } from '../types/api'

// 특정 작업지시의 품질검사 이력 + 신규 검사 등록.
const props = withDefaults(
  defineProps<{
    workOrderId: number | string
    defectReasons?: DefectReason[]
  }>(),
  { defectReasons: () => [] },
)

const inspections = ref<QualityInspection[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const submitting = ref(false)

// <input> 이 돌려주는 값은 늘 문자열이라 폼은 문자열로 들고 있다가 전송 직전에 숫자로 바꾼다.
const form = reactive({ inspectedQty: '', passedQty: '', defectQty: '', defectReasonId: '', note: '' })

async function load() {
  loading.value = true
  error.value = null
  try {
    inspections.value = await workOrderApi.inspections(props.workOrderId)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (form.inspectedQty === '' || form.passedQty === '') {
    error.value = '검사 수량과 합격 수량을 입력하세요.'
    return
  }
  const inspected = Number(form.inspectedQty)
  const passed = Number(form.passedQty)
  const defect = form.defectQty === '' ? inspected - passed : Number(form.defectQty)
  if (passed > inspected) {
    error.value = '합격 수량이 검사 수량보다 클 수 없습니다.'
    return
  }
  submitting.value = true
  error.value = null
  try {
    await workOrderApi.inspect(props.workOrderId, {
      inspectedQty: inspected,
      passedQty: passed,
      defectQty: defect,
      defectReasonId: form.defectReasonId ? Number(form.defectReasonId) : null,
      note: form.note || null,
    })
    form.inspectedQty = ''
    form.passedQty = ''
    form.defectQty = ''
    form.defectReasonId = ''
    form.note = ''
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}

function fmt(q: number | null | undefined) {
  return q == null ? '-' : Number(q).toLocaleString('ko-KR')
}
function fmtDateTime(dt: string | null | undefined) {
  return dt ? dt.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <h2>품질검사</h2>

    <p v-if="error" class="error">⚠ {{ error }}</p>

    <div v-if="inspections.length" class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>검사시각</th>
            <th>검사</th>
            <th>합격</th>
            <th>불량</th>
            <th>불량사유</th>
            <th>결과</th>
            <th>비고</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ins in inspections" :key="ins.id">
            <td class="muted nowrap">{{ fmtDateTime(ins.inspectedAt) }}</td>
            <td>{{ fmt(ins.inspectedQty) }}</td>
            <td>{{ fmt(ins.passedQty) }}</td>
            <td :class="{ error: Number(ins.defectQty) > 0 }">{{ fmt(ins.defectQty) }}</td>
            <td class="muted">{{ ins.defectReasonName || '-' }}</td>
            <td>
              <StatusBadge :label="qualityResult(ins.result).label" :tone="qualityResult(ins.result).tone" />
            </td>
            <td class="muted">{{ ins.note || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else-if="!loading" class="muted">등록된 검사 이력이 없습니다.</p>

    <!-- 신규 검사 등록 -->
    <div class="insp-form">
      <input v-model="form.inspectedQty" type="number" min="0" placeholder="검사 수량" />
      <input v-model="form.passedQty" type="number" min="0" placeholder="합격 수량" />
      <input v-model="form.defectQty" type="number" min="0" placeholder="불량(자동계산)" />
      <select v-model="form.defectReasonId">
        <option value="">불량사유(선택)</option>
        <option v-for="d in defectReasons" :key="d.id" :value="d.id">{{ d.name }}</option>
      </select>
      <input v-model="form.note" placeholder="비고" class="note" />
      <button @click="submit" :disabled="submitting">{{ submitting ? '등록 중…' : '검사 등록' }}</button>
    </div>
  </div>
</template>

<style scoped>
h2 {
  font-size: 15px;
  margin: 0 0 14px;
  color: var(--text-muted);
}
.insp-form {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--border);
}
input,
select {
  font: inherit;
  background: var(--bg-elevated);
  color: var(--text);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
}
input[type='number'] {
  width: 120px;
}
.note {
  flex: 1;
  min-width: 140px;
}
/* 검사 이력은 7컬럼이라 폰에서 접히면 되레 안 읽힌다 → 표만 가로 스크롤 */
.table-scroll {
  overflow-x: auto;
}
.table-scroll table {
  min-width: 640px;
}
.nowrap {
  white-space: nowrap;
}

@media (max-width: 640px) {
  /* 입력 폭 고정을 풀어 검사·합격·불량이 한 줄에 셋씩 들어가게 */
  .insp-form {
    gap: 8px;
  }
  .insp-form input[type='number'] {
    flex: 1;
    width: auto;
    min-width: 0;
  }
  .insp-form select,
  .insp-form .note,
  .insp-form button {
    flex-basis: 100%;
  }
}
</style>
