import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 라우트 meta 에 넣는 값의 타입 — 선언해두면 route.meta.title 이 오타·타입 검사를 받는다.
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
  }
}

// 라우트는 지연 로딩(lazy)으로 두어 화면이 늘어나도 초기 번들이 커지지 않게 한다.
const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/work-orders' },
  {
    path: '/work-orders',
    name: 'work-order-list',
    component: () => import('../views/WorkOrderListView.vue'),
    meta: { title: '작업지시' },
  },
  {
    path: '/work-orders/new',
    name: 'work-order-create',
    component: () => import('../views/WorkOrderCreateView.vue'),
    meta: { title: '작업지시 접수' },
  },
  {
    path: '/work-orders/:id',
    name: 'work-order-detail',
    component: () => import('../views/WorkOrderDetailView.vue'),
    props: true,
    meta: { title: '작업지시 상세' },
  },
  {
    path: '/equipments',
    name: 'equipment-dashboard',
    component: () => import('../views/EquipmentDashboardView.vue'),
    meta: { title: '설비 가동현황' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `MES · ${to.meta.title}` : 'MES · 제조실행'
})

export default router
