import { useEffect, useState } from 'react'
import { Navigate, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import {
  SD_ROLES,
  MM_VIEW_ROLES,
  MM_WRITE_ROLES,
  PP_ROLES,
  FI_ROLES,
  STOCK_VIEW_ROLES,
  STOCK_MOVEMENT_ROLES,
  EMP_VIEW_ROLES,
  HR_ROLES,
  REPORT_ROLES,
  CREDIT_ROLES,
  BOM_ROLES,
  ADMIN_ROLES,
} from './auth/routeRoles'
import { applyTheme, getInitialTheme, type Theme } from './theme'
import ProtectedRoute from './auth/ProtectedRoute'
import LoginView from './views/LoginView'
import QuotationListView from './views/QuotationListView'
import QuotationDetailView from './views/QuotationDetailView'
import QuotationCreateView from './views/QuotationCreateView'
import SalesOrderListView from './views/SalesOrderListView'
import SalesOrderDetailView from './views/SalesOrderDetailView'
import SalesOrderCreateView from './views/SalesOrderCreateView'
import DeliveryListView from './views/DeliveryListView'
import DeliveryDetailView from './views/DeliveryDetailView'
import DeliveryCreateView from './views/DeliveryCreateView'
import InvoiceListView from './views/InvoiceListView'
import InvoiceDetailView from './views/InvoiceDetailView'
import InvoiceCreateView from './views/InvoiceCreateView'
import PurchaseOrderListView from './views/PurchaseOrderListView'
import PurchaseOrderDetailView from './views/PurchaseOrderDetailView'
import PurchaseOrderCreateView from './views/PurchaseOrderCreateView'
import GoodsReceiptListView from './views/GoodsReceiptListView'
import GoodsReceiptDetailView from './views/GoodsReceiptDetailView'
import GoodsReceiptCreateView from './views/GoodsReceiptCreateView'
import ProductionOrderListView from './views/ProductionOrderListView'
import ProductionOrderDetailView from './views/ProductionOrderDetailView'
import ProductionOrderCreateView from './views/ProductionOrderCreateView'
import JournalEntryListView from './views/JournalEntryListView'
import JournalEntryDetailView from './views/JournalEntryDetailView'
import JournalEntryCreateView from './views/JournalEntryCreateView'
import PaymentListView from './views/PaymentListView'
import PaymentDetailView from './views/PaymentDetailView'
import PaymentCreateView from './views/PaymentCreateView'
import AccountListView from './views/AccountListView'
import DashboardView from './views/DashboardView'
import ApprovalListView from './views/ApprovalListView'
import ApprovalDetailView from './views/ApprovalDetailView'
import StockListView from './views/StockListView'
import StockMovementListView from './views/StockMovementListView'
import EmployeeListView from './views/EmployeeListView'
import EmployeeDetailView from './views/EmployeeDetailView'
import PayrollListView from './views/PayrollListView'
import PayrollDetailView from './views/PayrollDetailView'
import SalesReportView from './views/SalesReportView'
import InventoryReportView from './views/InventoryReportView'
import IncomeStatementView from './views/IncomeStatementView'
import AssistantView from './views/AssistantView'
import CreditRequestListView from './views/CreditRequestListView'
import CreditRequestDetailView from './views/CreditRequestDetailView'
import CreditRequestCreateView from './views/CreditRequestCreateView'
import BomListView from './views/BomListView'
import AdminUserListView from './views/AdminUserListView'
import AdminRoleListView from './views/AdminRoleListView'

// 역할 상수는 auth/routeRoles.ts 로 옮겼다 — 로그인 직후 'from' 경로가 그 계정에
// 허용되는지 판단하려면 라우트 JSX 바깥에서도 경로→역할을 알아야 하기 때문.
// ⚠️ 아래 <Route> 를 추가/수정하면 routeRoles.ts 의 ROUTE_ROLES 도 같이 고칠 것.

// 좌측 사이드바 — 부서별 섹션으로 묶은 네비.
// ⚠️ 메뉴 노출은 '부서 소속' 기준으로 좁게 보여준다(이전 Thymeleaf 방식).
//    실제 접근 권한(ProtectedRoute·API)은 더 넓다 — 예: SALES 는 재고를 조회할 수 있지만
//    사이드바에는 안 띄운다. 필요하면 URL 로 접근 가능. 여기선 화면을 부서별로 깔끔하게 유지한다.
function Sidebar({ onNavigate }: { onNavigate: () => void }) {
  const { hasRole } = useAuth()

  const inSD = hasRole('SALES', 'ADMIN')
  const inMM = hasRole('PURCHASING', 'ADMIN')
  const inPP = hasRole('PRODUCTION', 'ADMIN')
  const inFI = hasRole('FINANCE', 'ADMIN')
  const inHR = hasRole('HR', 'ADMIN')
  // 리포트는 재무 섹션에 두되 임원(DIRECTOR)도 열람하므로 조건을 조금 넓힌다.
  const canReport = hasRole('FINANCE', 'DIRECTOR', 'ADMIN')
  const canAdmin = hasRole('ADMIN')

  // 메뉴를 누르면(특히 모바일에서) 사이드바를 닫는다.
  const link = (to: string, label: string) => (
    <NavLink to={to} className="nav-link" onClick={onNavigate}>
      {label}
    </NavLink>
  )

  return (
    <aside className="sidebar">
      <NavLink to="/dashboard" className="sidebar-brand" onClick={onNavigate}>
        ERP
      </NavLink>
      <nav className="sidebar-nav">
        {link('/dashboard', '대시보드')}

        {inSD && (
          <>
            <div className="nav-section">영업 (SD)</div>
            {link('/quotations', '견적')}
            {link('/sales-orders', '수주')}
            {link('/deliveries', '출하')}
            {link('/invoices', '청구')}
            {link('/credit-requests', '여신')}
          </>
        )}

        {inMM && (
          <>
            <div className="nav-section">구매·자재 (MM)</div>
            {link('/purchase-orders', '발주')}
            {link('/goods-receipts', '입고')}
            {link('/stocks', '현재고')}
            {link('/stock-movements', '이동이력')}
          </>
        )}

        {inPP && (
          <>
            <div className="nav-section">생산 (PP)</div>
            {link('/production-orders', '생산')}
            {link('/boms', 'BOM')}
          </>
        )}

        {(inFI || canReport) && (
          <>
            <div className="nav-section">재무 (FI)</div>
            {inFI && link('/journal-entries', '전표')}
            {inFI && link('/payments', '입출금')}
            {inFI && link('/accounts', '계정과목')}
            {canReport && link('/reports/sales', '리포트')}
          </>
        )}

        {inHR && (
          <>
            <div className="nav-section">인사 (HR)</div>
            {link('/employees', '사원')}
            {link('/payroll-runs', '급여대장')}
          </>
        )}

        <div className="nav-section">공통</div>
        {link('/approvals', '전자결재')}
        {link('/assistant', 'AI 챗봇')}
        {canAdmin && link('/admin/users', '관리자')}
      </nav>
    </aside>
  )
}

// 상단바 — 햄버거(모바일) · 사용자 · 테마 토글 · 로그아웃.
function Topbar({
  theme,
  onToggleTheme,
  onToggleNav,
}: {
  theme: Theme
  onToggleTheme: () => void
  onToggleNav: () => void
}) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function onLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="topbar">
      <button className="hamburger sm" onClick={onToggleNav} aria-label="메뉴 열기">
        ☰
      </button>
      <div className="topbar-spacer" />
      <span className="muted" style={{ fontSize: 13 }}>
        {user?.username} <span style={{ opacity: 0.7 }}>({user?.roles.join(', ')})</span>
      </span>
      <button
        className="theme-toggle sm"
        onClick={onToggleTheme}
        aria-label="테마 전환"
        title={theme === 'dark' ? '라이트 모드로' : '다크 모드로'}
      >
        {theme === 'dark' ? '☀️' : '🌙'}
      </button>
      <button className="sm" onClick={onLogout}>
        로그아웃
      </button>
    </div>
  )
}

// 로그인 직후 착지 화면 = 대시보드.
// 예전엔 무조건 견적(SD)이라 재무·구매·생산 역할은 로그인하자마자 '권한 없음'을 봤다.
// 대시보드는 역할 무관이라 그 문제가 사라진다(미인증이면 ProtectedRoute 가 /login 으로).
function HomeRedirect() {
  return <Navigate to="/dashboard" replace />
}

function Footer() {
  return (
    <footer
      style={{
        borderTop: '1px solid var(--border)',
        color: 'var(--text-muted)',
        fontSize: 12,
        textAlign: 'center',
        padding: '18px 20px',
      }}
    >
      © 2026 HYUNWOO ERP · 통합 자원관리 시스템
    </footer>
  )
}

export default function App() {
  const { user } = useAuth()
  const [theme, setTheme] = useState<Theme>(getInitialTheme)
  const [navOpen, setNavOpen] = useState(false)
  const location = useLocation()

  // 페이지를 이동하면 모바일에서 열려 있던 사이드바를 닫는다.
  useEffect(() => {
    setNavOpen(false)
  }, [location.pathname])

  function toggleTheme() {
    const next: Theme = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    applyTheme(next)
  }

  const routes = (
      <Routes>
        <Route path="/login" element={<LoginView />} />

        <Route
          path="/quotations"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <QuotationListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quotations/new"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <QuotationCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quotations/:id/edit"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <QuotationCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quotations/:id"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <QuotationDetailView />
            </ProtectedRoute>
          }
        />

        <Route
          path="/sales-orders"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <SalesOrderListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sales-orders/new"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <SalesOrderCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sales-orders/:id/edit"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <SalesOrderCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sales-orders/:id"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <SalesOrderDetailView />
            </ProtectedRoute>
          }
        />

        {/* 출하 */}
        <Route
          path="/deliveries"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <DeliveryListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/deliveries/new"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <DeliveryCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/deliveries/:id"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <DeliveryDetailView />
            </ProtectedRoute>
          }
        />

        {/* 청구 */}
        <Route
          path="/invoices"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <InvoiceListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/invoices/new"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <InvoiceCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/invoices/:id"
          element={
            <ProtectedRoute roles={SD_ROLES}>
              <InvoiceDetailView />
            </ProtectedRoute>
          }
        />

        {/* 발주 (조회는 넓은 역할, 작성/수정은 구매/관리자) */}
        <Route
          path="/purchase-orders"
          element={
            <ProtectedRoute roles={MM_VIEW_ROLES}>
              <PurchaseOrderListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/new"
          element={
            <ProtectedRoute roles={MM_WRITE_ROLES}>
              <PurchaseOrderCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/:id/edit"
          element={
            <ProtectedRoute roles={MM_WRITE_ROLES}>
              <PurchaseOrderCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/:id"
          element={
            <ProtectedRoute roles={MM_VIEW_ROLES}>
              <PurchaseOrderDetailView />
            </ProtectedRoute>
          }
        />

        {/* 입고 (구매/관리자 전용 — 조회·쓰기 동일) */}
        <Route
          path="/goods-receipts"
          element={
            <ProtectedRoute roles={MM_WRITE_ROLES}>
              <GoodsReceiptListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/goods-receipts/new"
          element={
            <ProtectedRoute roles={MM_WRITE_ROLES}>
              <GoodsReceiptCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/goods-receipts/:id"
          element={
            <ProtectedRoute roles={MM_WRITE_ROLES}>
              <GoodsReceiptDetailView />
            </ProtectedRoute>
          }
        />

        {/* 생산 작업지시 (생산/관리자 전용) */}
        <Route
          path="/production-orders"
          element={
            <ProtectedRoute roles={PP_ROLES}>
              <ProductionOrderListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/production-orders/new"
          element={
            <ProtectedRoute roles={PP_ROLES}>
              <ProductionOrderCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/production-orders/:id"
          element={
            <ProtectedRoute roles={PP_ROLES}>
              <ProductionOrderDetailView />
            </ProtectedRoute>
          }
        />

        {/* 재무 전표 (재무/관리자 전용) */}
        <Route
          path="/journal-entries"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <JournalEntryListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/journal-entries/new"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <JournalEntryCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/journal-entries/:id"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <JournalEntryDetailView />
            </ProtectedRoute>
          }
        />

        {/* 입금/출금 (재무/관리자 전용) */}
        <Route
          path="/payments"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <PaymentListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payments/new"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <PaymentCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payments/:id"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <PaymentDetailView />
            </ProtectedRoute>
          }
        />

        {/* 계정과목 (재무/관리자 전용) */}
        <Route
          path="/accounts"
          element={
            <ProtectedRoute roles={FI_ROLES}>
              <AccountListView />
            </ProtectedRoute>
          }
        />

        {/* 재고 조회 (읽기 전용). 현재고=영업까지, 이동이력=구매/관리자. */}
        <Route
          path="/stocks"
          element={
            <ProtectedRoute roles={STOCK_VIEW_ROLES}>
              <StockListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stock-movements"
          element={
            <ProtectedRoute roles={STOCK_MOVEMENT_ROLES}>
              <StockMovementListView />
            </ProtectedRoute>
          }
        />

        {/* 인사: 사원 (조회는 넓게, 상세의 계약·근태는 HR/ADMIN 만 로드) */}
        <Route
          path="/employees"
          element={
            <ProtectedRoute roles={EMP_VIEW_ROLES}>
              <EmployeeListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/employees/:id"
          element={
            <ProtectedRoute roles={EMP_VIEW_ROLES}>
              <EmployeeDetailView />
            </ProtectedRoute>
          }
        />

        {/* 인사: 급여대장 (HR/ADMIN 전용) */}
        <Route
          path="/payroll-runs"
          element={
            <ProtectedRoute roles={HR_ROLES}>
              <PayrollListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payroll-runs/:id"
          element={
            <ProtectedRoute roles={HR_ROLES}>
              <PayrollDetailView />
            </ProtectedRoute>
          }
        />

        {/* 리포트 (재무/임원/관리자) — 매출·재고·손익 */}
        <Route
          path="/reports/sales"
          element={
            <ProtectedRoute roles={REPORT_ROLES}>
              <SalesReportView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports/inventory"
          element={
            <ProtectedRoute roles={REPORT_ROLES}>
              <InventoryReportView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports/income-statement"
          element={
            <ProtectedRoute roles={REPORT_ROLES}>
              <IncomeStatementView />
            </ProtectedRoute>
          }
        />

        {/* 여신 상향 요청 (영업·재무·관리자 조회, 생성은 영업·관리자) */}
        <Route
          path="/credit-requests"
          element={
            <ProtectedRoute roles={CREDIT_ROLES}>
              <CreditRequestListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/credit-requests/new"
          element={
            <ProtectedRoute roles={CREDIT_ROLES}>
              <CreditRequestCreateView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/credit-requests/:id"
          element={
            <ProtectedRoute roles={CREDIT_ROLES}>
              <CreditRequestDetailView />
            </ProtectedRoute>
          }
        />

        {/* BOM 조회 (생산·관리자) */}
        <Route
          path="/boms"
          element={
            <ProtectedRoute roles={BOM_ROLES}>
              <BomListView />
            </ProtectedRoute>
          }
        />

        {/* 관리자 (사용자·역할) — ADMIN 전용 */}
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute roles={ADMIN_ROLES}>
              <AdminUserListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/roles"
          element={
            <ProtectedRoute roles={ADMIN_ROLES}>
              <AdminRoleListView />
            </ProtectedRoute>
          }
        />

        {/* AI 어시스턴트 챗봇 — 로그인만 하면 누구나(전 부서 공용). */}
        <Route
          path="/assistant"
          element={
            <ProtectedRoute>
              <AssistantView />
            </ProtectedRoute>
          }
        />

        {/* 대시보드·전자결재 — 로그인만 하면 누구나. 처리 권한은 결재선이 정한다. */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/approvals"
          element={
            <ProtectedRoute>
              <ApprovalListView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/approvals/:id"
          element={
            <ProtectedRoute>
              <ApprovalDetailView />
            </ProtectedRoute>
          }
        />

        {/* 기본 진입 → 대시보드 (미인증이면 /login) */}
        <Route path="*" element={<HomeRedirect />} />
      </Routes>
  )

  // 미인증(로그인 화면)은 사이드바 셸 없이 렌더한다.
  if (!user) return routes

  return (
    <div className={`app-shell${navOpen ? ' nav-open' : ''}`}>
      <Sidebar onNavigate={() => setNavOpen(false)} />
      <div className="sidebar-backdrop" onClick={() => setNavOpen(false)} />
      <div className="app-main">
        <Topbar
          theme={theme}
          onToggleTheme={toggleTheme}
          onToggleNav={() => setNavOpen((v) => !v)}
        />
        <div style={{ flex: 1 }}>{routes}</div>
        <Footer />
      </div>
    </div>
  )
}
