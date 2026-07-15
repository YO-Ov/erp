<script setup>
// 앱 셸: 상단 헤더 + 라우터 뷰.
// 화면 전환은 vue-router 가 담당한다(<router-view/>).
</script>

<template>
  <header class="app-header">
    <div class="container header-inner">
      <RouterLink to="/work-orders" class="brand">
        <span class="brand-mark">MES</span>
        <span class="brand-sub">제조실행 시스템</span>
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/work-orders">작업지시</RouterLink>
        <RouterLink to="/equipments">설비 가동현황</RouterLink>
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
.nav a {
  color: var(--text-muted);
  font-weight: 600;
}
.nav a.router-link-active {
  color: var(--text);
}
</style>
