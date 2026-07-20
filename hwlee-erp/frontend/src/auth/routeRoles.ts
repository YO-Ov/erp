import type { Role } from '../types/api'

// ─────────────────────────────────────────────────────────────
// 화면별 필요 역할 — App.tsx 의 <Route> 선언과 1:1 로 맞춘 단일 출처.
//
// 왜 별도 파일인가:
//   로그인 직후 "원래 가려던 경로(from)"로 보내기 전에, 그 경로가 이 계정에
//   허용되는지 **미리** 판단해야 하기 때문. 그러려면 라우트 JSX 바깥에서도
//   경로→역할을 조회할 수 있어야 한다.
//
// ⚠️ App.tsx 에 라우트를 추가/수정하면 아래 ROUTE_ROLES 도 같이 고칠 것.
//    (빠뜨리면 '역할 제한 없음'으로 취급되어 로그인 직후 '권한 없음' 화면을 볼 수 있다)
// ─────────────────────────────────────────────────────────────

// SD 전 모듈(견적·수주·출하·청구)은 백엔드가 SALES/ADMIN 만 허용한다.
export const SD_ROLES: readonly Role[] = ['SALES', 'ADMIN']
// 발주(MM)는 조회 역할이 넓고, 쓰기는 PURCHASING/ADMIN 만(백엔드와 동일하게 프론트도 분리).
export const MM_VIEW_ROLES: readonly Role[] = [
  'SALES',
  'PURCHASING',
  'PRODUCTION',
  'FINANCE',
  'DIRECTOR',
  'ADMIN',
]
export const MM_WRITE_ROLES: readonly Role[] = ['PURCHASING', 'ADMIN']
// 생산(PP)은 PRODUCTION/ADMIN 전용.
export const PP_ROLES: readonly Role[] = ['PRODUCTION', 'ADMIN']
// 재무(FI) 전 모듈(전표·입출금·계정과목)은 FINANCE/ADMIN 전용 — 조회·쓰기 구분 없음.
export const FI_ROLES: readonly Role[] = ['FINANCE', 'ADMIN']
// 현재고 조회는 영업까지 넓다(출하 위해 재고를 봐야 함). 이동이력은 구매/관리자만.
export const STOCK_VIEW_ROLES: readonly Role[] = ['SALES', 'PURCHASING', 'ADMIN']
export const STOCK_MOVEMENT_ROLES: readonly Role[] = ['PURCHASING', 'ADMIN']
// 사원 조회는 넓다(백엔드도 관리부서 전반 허용). 급여계약·근태·급여대장은 HR/ADMIN 전용(민감정보).
export const EMP_VIEW_ROLES: readonly Role[] = ['SALES', 'PURCHASING', 'FINANCE', 'HR', 'ADMIN']
export const HR_ROLES: readonly Role[] = ['HR', 'ADMIN']
// 전사 리포트(매출·재고·손익)는 재무·관리자 + 임원(DIRECTOR) 열람.
export const REPORT_ROLES: readonly Role[] = ['FINANCE', 'DIRECTOR', 'ADMIN']
// 여신 상향 요청: 조회는 영업·재무·관리자, 생성은 영업·관리자(화면 내 분기).
export const CREDIT_ROLES: readonly Role[] = ['SALES', 'FINANCE', 'ADMIN']
// BOM 조회는 생산·관리자.
export const BOM_ROLES: readonly Role[] = ['PRODUCTION', 'ADMIN']
// 관리자(사용자·역할)는 ADMIN 전용.
export const ADMIN_ROLES: readonly Role[] = ['ADMIN']

// 경로 패턴 → 필요 역할. **위에서부터 첫 일치**가 이긴다.
//   → '/purchase-orders/new'(쓰기)를 '/purchase-orders/:id'(조회)보다 먼저 둬야 한다.
// 역할 제한이 없는 화면(대시보드·전자결재·챗봇)은 여기 없다 → rolesForPath 가 null 을 준다.
const ROUTE_ROLES: readonly (readonly [string, readonly Role[]])[] = [
  ['/quotations/new', SD_ROLES],
  ['/quotations/:id/edit', SD_ROLES],
  ['/quotations/:id', SD_ROLES],
  ['/quotations', SD_ROLES],
  ['/sales-orders/new', SD_ROLES],
  ['/sales-orders/:id/edit', SD_ROLES],
  ['/sales-orders/:id', SD_ROLES],
  ['/sales-orders', SD_ROLES],
  ['/deliveries/new', SD_ROLES],
  ['/deliveries/:id', SD_ROLES],
  ['/deliveries', SD_ROLES],
  ['/invoices/new', SD_ROLES],
  ['/invoices/:id', SD_ROLES],
  ['/invoices', SD_ROLES],
  ['/purchase-orders/new', MM_WRITE_ROLES],
  ['/purchase-orders/:id/edit', MM_WRITE_ROLES],
  ['/purchase-orders/:id', MM_VIEW_ROLES],
  ['/purchase-orders', MM_VIEW_ROLES],
  ['/goods-receipts/new', MM_WRITE_ROLES],
  ['/goods-receipts/:id', MM_WRITE_ROLES],
  ['/goods-receipts', MM_WRITE_ROLES],
  ['/production-orders/new', PP_ROLES],
  ['/production-orders/:id', PP_ROLES],
  ['/production-orders', PP_ROLES],
  ['/journal-entries/new', FI_ROLES],
  ['/journal-entries/:id', FI_ROLES],
  ['/journal-entries', FI_ROLES],
  ['/payments/new', FI_ROLES],
  ['/payments/:id', FI_ROLES],
  ['/payments', FI_ROLES],
  ['/accounts', FI_ROLES],
  ['/stocks', STOCK_VIEW_ROLES],
  ['/stock-movements', STOCK_MOVEMENT_ROLES],
  ['/employees/:id', EMP_VIEW_ROLES],
  ['/employees', EMP_VIEW_ROLES],
  ['/payroll-runs/:id', HR_ROLES],
  ['/payroll-runs', HR_ROLES],
  ['/reports/sales', REPORT_ROLES],
  ['/reports/inventory', REPORT_ROLES],
  ['/reports/income-statement', REPORT_ROLES],
  ['/credit-requests/new', CREDIT_ROLES],
  ['/credit-requests/:id', CREDIT_ROLES],
  ['/credit-requests', CREDIT_ROLES],
  ['/boms', BOM_ROLES],
  ['/admin/users', ADMIN_ROLES],
  ['/admin/roles', ADMIN_ROLES],
]

// ':id' 는 슬래시 없는 한 조각으로 본다. 정규식 특수문자는 이스케이프.
function toRegExp(pattern: string): RegExp {
  const body = pattern
    .split('/')
    .map((seg) => (seg.startsWith(':') ? '[^/]+' : seg.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    .join('/')
  return new RegExp(`^${body}/?$`)
}

const COMPILED = ROUTE_ROLES.map(([p, roles]) => [toRegExp(p), roles] as const)

/** 경로에 필요한 역할. 역할 제한이 없거나 모르는 경로면 null. */
export function rolesForPath(pathname: string): readonly Role[] | null {
  for (const [re, roles] of COMPILED) {
    if (re.test(pathname)) return roles
  }
  return null
}

/** 이 역할들로 해당 경로에 들어갈 수 있는가. (제한 없는 경로는 항상 true) */
export function canAccessPath(pathname: string, roles: readonly Role[]): boolean {
  const required = rolesForPath(pathname)
  if (!required) return true
  return required.some((r) => roles.includes(r))
}
