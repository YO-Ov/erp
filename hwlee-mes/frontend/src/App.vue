<script setup lang="ts">
// 앱 셸: 상단 헤더 + 라우터 뷰.
// 화면 전환은 vue-router 가 담당한다(<router-view/>).
import { useRoute } from 'vue-router'

const route = useRoute()

// router-link-active 는 라우트 '계층'으로 판정한다. /work-orders 와 /work-orders/:id 는
// 서로 독립된 최상위 라우트라 부모·자식이 아니어서, 상세로 들어가면 탭 활성이 풀린다.
// 탭은 '섹션' 단위로 켜져야 하므로 경로 접두사로 직접 판정한다.
const inSection = (prefix: string) => route.path === prefix || route.path.startsWith(prefix + '/')
</script>

<template>
  <header class="app-header">
    <div class="container header-inner">
      <RouterLink to="/work-orders" class="brand">
        <span class="brand-mark">MES</span>
        <span class="brand-sub">제조실행 시스템</span>
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/work-orders" :class="{ 'tab-active': inSection('/work-orders') }">
          작업지시
        </RouterLink>
        <RouterLink to="/equipments" :class="{ 'tab-active': inSection('/equipments') }">
          설비 가동현황
        </RouterLink>
      </nav>
    </div>
  </header>

  <main class="container">
    <RouterView />
  </main>
</template>

<style scoped>
.app-header {
  border-bottom: 1px solid var(--border);
  background: var(--bg-panel);
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  padding-bottom: 14px;
}
.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.brand:hover {
  text-decoration: none;
}
.brand-mark {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--accent);
}
.brand-sub {
  color: var(--text-muted);
  font-size: 13px;
}
.nav {
  display: flex;
  gap: 6px;
}
.nav a {
  color: var(--text-muted);
  font-weight: 600;
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
}
.nav a:hover {
  text-decoration: none;
  color: var(--text);
  background: var(--bg-elevated);
}
/* 활성 탭 — 채움 버튼(.primary)과 경쟁하지 않도록 accent 를 옅게만 깔았다. */
.nav a.tab-active {
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 14%, transparent);
  border-color: color-mix(in srgb, var(--accent) 38%, transparent);
}
.nav a:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* 좁은 화면 — 브랜드 설명을 접어 탭 두 개가 헤더 안에 들어오게 한다.
   (탭이 밀려 잘리면 다른 화면으로 갈 방법이 아예 없어진다) */
@media (max-width: 640px) {
  .brand-sub {
    display: none;
  }
  .nav a {
    padding: 6px 10px;
    font-size: 13px;
  }
}
</style>
