<script setup lang="ts">
import type { Tone } from '../domain/status'

// 상태 코드를 받아 색상 배지로 표시한다.
// { label, tone } 형태를 그대로 받아 재사용(작업지시·설비 공용).
// tone 은 Tone 유니온이라 오타나 없는 색상을 넘기면 타입 검사에서 걸린다.
withDefaults(
  defineProps<{
    label: string
    tone?: Tone
  }>(),
  { tone: 'neutral' },
)
</script>

<template>
  <span class="badge" :class="`tone-${tone}`">{{ label }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.6;
  color: #0b1220;
  background: var(--tone-neutral);
}
.tone-neutral {
  background: var(--tone-neutral);
  color: #f8fafc;
}
.tone-active {
  background: var(--tone-active);
}
.tone-warn {
  background: var(--tone-warn);
}
.tone-done {
  background: var(--tone-done);
}
.tone-muted {
  background: var(--tone-muted);
  color: #cbd5e1;
}
.tone-danger {
  background: var(--tone-danger);
  color: #fff;
}
</style>
