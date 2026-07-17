import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import type { Role } from './types/api'
import { useAuth } from './auth/AuthContext'
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

// SD 전 모듈(견적·수주·출하·청구)은 백엔드가 SALES/ADMIN 만 허용한다.
const SD_ROLES: readonly Role[] = ['SALES', 'ADMIN']
// 발주(MM)는 조회 역할이 넓고, 쓰기는 PURCHASING/ADMIN 만(백엔드와 동일하게 프론트도 분리).
const MM_VIEW_ROLES: readonly Role[] = [
  'SALES',
  'PURCHASING',
  'PRODUCTION',
  'FINANCE',
  'DIRECTOR',
  'ADMIN',
]
const MM_WRITE_ROLES: readonly Role[] = ['PURCHASING', 'ADMIN']
// 생산(PP)은 PRODUCTION/ADMIN 전용.
const PP_ROLES: readonly Role[] = ['PRODUCTION', 'ADMIN']
// 재무(FI) 전 모듈(전표·입출금·계정과목)은 FINANCE/ADMIN 전용 — 조회·쓰기 구분 없음.
const FI_ROLES: readonly Role[] = ['FINANCE', 'ADMIN']

// 앱 셸: 로그인 상태에서만 상단 헤더(네비 + 로그아웃)를 보여준다.
function Header() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  // 권한 있는 메뉴만 노출한다.
  const canSD = hasRole(...SD_ROLES)
  const canMMView = hasRole(...MM_VIEW_ROLES)
  const canMMWrite = hasRole(...MM_WRITE_ROLES) // 입고는 구매/관리자 전용
  const canPP = hasRole(...PP_ROLES)
  const canFI = hasRole(...FI_ROLES)

  async function onLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header
      style={{ borderBottom: '1px solid var(--border)', background: 'var(--bg-panel)' }}
    >
      <div
        className="container"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          paddingTop: 14,
          paddingBottom: 14,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 22 }}>
          <NavLink
            to="/quotations"
            style={{ fontWeight: 800, fontSize: 18, color: 'var(--accent)' }}
          >
            ERP
          </NavLink>
          <nav style={{ display: 'flex', gap: 16 }}>
            {/* 대시보드·전자결재는 역할 무관 — 결재선이 누가 처리할지 정한다. */}
            <NavLink to="/dashboard">대시보드</NavLink>
            {canSD && <NavLink to="/quotations">견적</NavLink>}
            {canSD && <NavLink to="/sales-orders">수주</NavLink>}
            {canSD && <NavLink to="/deliveries">출하</NavLink>}
            {canSD && <NavLink to="/invoices">청구</NavLink>}
            {canMMView && <NavLink to="/purchase-orders">발주</NavLink>}
            {canMMWrite && <NavLink to="/goods-receipts">입고</NavLink>}
            {canPP && <NavLink to="/production-orders">생산</NavLink>}
            {canFI && <NavLink to="/journal-entries">전표</NavLink>}
            {canFI && <NavLink to="/payments">입출금</NavLink>}
            {canFI && <NavLink to="/accounts">계정과목</NavLink>}
            <NavLink to="/approvals">전자결재</NavLink>
          </nav>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="muted" style={{ fontSize: 13 }}>
            {user.username}{' '}
            <span style={{ opacity: 0.7 }}>({user.roles.join(', ')})</span>
          </span>
          <button className="sm" onClick={onLogout}>
            로그아웃
          </button>
        </div>
      </div>
    </header>
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
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header />
      <div style={{ flex: 1 }}>
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
      </div>
      <Footer />
    </div>
  )
}
