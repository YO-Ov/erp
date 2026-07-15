<script setup>
import { ref, reactive, onMounted } from 'vue'
import { workOrderApi } from '../api/workOrders'
import { qualityResult } from '../domain/status'
import StatusBadge from './StatusBadge.vue'

// 특정 작업지시의 품질검사 이력 + 신규 검사 등록.
const props = defineProps({
  workOrderId: { type: [String, Number], required: true },
  defectReasons: { type: Array, default: () => [] },
})

const inspections = ref([])
const loading = ref(false)
const error = ref(null)
const submitting = ref(false)

const form = reactive({ inspectedQty: '', passedQty: '', defectQty: '', defectReasonId: '', note: '' })

async function load() {
  loading.value = true
  error.value = null
  try {
    inspections.value = await workOrderApi.inspections(props.workOrderId)
  } catch (e) {
    error.value = e.message
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
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

function fmt(q) {
  return q == null ? '-' : Number(q).toLocaleString('ko-KR')
}
function fmtDateTime(dt) {
  return dt ? dt.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <h2>품질검사</h2>

    <p v-if="error" class="error">⚠ {{ error }}</p>

    <table v-if="inspections.length">
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
          <td class="muted">{{ fmtDateTime(ins.inspectedAt) }}</td>
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
</style>
