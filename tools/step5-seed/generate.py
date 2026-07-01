#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
STEP 5 — 2024~2026상반기 실거래 백필 시뮬레이터 (결정론적).

3년치 ERP 운영을 일자순으로 시뮬레이션해 완결(CLOSED) 거래를 SQL 시드로 산출한다.
  구매(입고) → 생산 → 판매(수주→출하→계산서→입금) 전 체인 + 이동평균 원가 + 분개(차대균형).

정합성(조사 명세):
  - 판매완결 1건: sales_order(CLOSED)+line + delivery(SHIPPED)+line + goods_issue(POSTED,delivery_id)+line
    + stock_movement(GOODS_ISSUE,-,출고평균) + invoice(ISSUED,VAT10%)+line + payment(RECEIPT,POSTED)
    + 분개3(매출 차1200/대4100·2200, 매출원가 차5100/대1400, 입금 차1100/대1200)
    ※ 입금일 > 오늘이면 payment/입금분개 생략 + SO status=INVOICED (미수금으로 남음)
  - 생산완결: production_order(COMPLETED)+line + movement(PRODUCTION_OUT 부품- / PRODUCTION_IN 완제품+)
    + 분개(차1400/대1410 = 직접재료비)
  - 입고완결(부품): goods_receipt(POSTED)+line + movement(GOODS_RECEIPT,+,이동평균) + 분개(차1410/대2100)
    + payment(DISBURSEMENT,POSTED) + 분개(차2100/대1100)  ※ 지급일>오늘이면 미지급(생략)
  - 채번 {PREFIX}-yyyyMMdd-NNN (과거 날짜 → code_sequence 갱신 불필요)
  - stock_movement 는 매 이벤트, stock 캐시는 최종값만 UPSERT (SUM(delta)=qty 유지)
  - id 참조: 세션변수(@i_*/@c_*/@v_*/@a_*/@wh/@sp*) + LAST_INSERT_ID()

창고: WH-HQ 중앙창고 단일 기준. STEP4 공장창고 기초재고는 정적 보유.

사용법:  python3 generate.py [scale]   (scale=매출 축소배율, 기본 1.0)
출력:    ../../hwlee-erp/src/main/resources/db/migration/V60..V63__step5_*.sql
"""
import os
import sys
import re
import random
from datetime import date, timedelta

random.seed(20260702)

HERE = os.path.dirname(os.path.abspath(__file__))
ERP_MIG = os.path.normpath(os.path.join(HERE, "../../hwlee-erp/src/main/resources/db/migration"))
STEP4 = os.path.normpath(os.path.join(HERE, "../step4-seed"))
sys.path.insert(0, STEP4)
import generate as s4

AUDIT = "NOW(), 'system', NOW(), 'system'"
TODAY = date(2026, 7, 1)
# 모든 STEP5 트랜잭션 날짜는 오늘 전(≤ LAST)으로 제한 → 라이브 채번(오늘 이후)과 완전 분리.
# (그래서 code_sequence 갱신 불필요. 미입금/미지급은 오늘 시점 미수금·미지급으로 남음.)
LAST = date(2026, 6, 30)
SCALE = float(sys.argv[1]) if len(sys.argv) > 1 else 1.0
TARGET = {2024: 15_000_000_000, 2025: 20_000_000_000, 2026: 16_000_000_000}
TARGET = {y: int(v * SCALE) for y, v in TARGET.items()}


def q(s):
    return "'" + s.replace("'", "''") + "'"


def dt(d):
    return d.strftime("%Y-%m-%d")


def pk(d):
    return d.strftime("%Y%m%d")


def money(x):
    return f"{round(x, 2):.2f}"


def qty4(x):
    return f"{x:.4f}"

# =============================================================================
# 마스터 로드 (STEP4 카탈로그 재사용 + 기존 시드)
# =============================================================================
FINISHED = [
    ("ITEM-2026-0001", "hyunwoo 노트북 15\"", 800000, 1200000),
    ("ITEM-2026-0002", "hyunwoo 모니터 27\"", 200000, 350000),
]
n = 13
for cat, name, price, cost in s4.FINISHED_NEW:
    FINISHED.append((s4.item_code(n), name, cost, price)); n += 1

PART = [
    ("ITEM-2026-0003", "15\" LCD 패널", 200000), ("ITEM-2026-0004", "메인보드", 150000),
    ("ITEM-2026-0005", "16GB 메모리", 50000), ("ITEM-2026-0006", "512GB SSD", 150000),
    ("ITEM-2026-0007", "배터리팩", 80000), ("ITEM-2026-0008", "27\" LCD 패널", 250000),
    ("ITEM-2026-0009", "스케일러 보드", 80000), ("ITEM-2026-0010", "전원 보드", 40000),
    ("ITEM-2026-0011", "모니터 스탠드", 30000), ("ITEM-2026-0012", "후면 케이스", 20000),
]
n = 27
for cat, name, cost in s4.PART_NEW:
    PART.append((s4.item_code(n), name, cost)); n += 1

NAME2CODE = {name: code for code, name, *_ in FINISHED}
NAME2CODE.update({name: code for code, name, _ in PART})
CODE2COST = {code: cost for code, name, cost, *_ in FINISHED}
CODE2COST.update({code: cost for code, name, cost in PART})
FIN_PRICE = {code: price for code, name, cost, price in FINISHED}
ALL_ITEM_CODES = [c for c, *_ in FINISHED] + [c for c, *_ in PART]

BOM_BY_NAME = {
    "hyunwoo 노트북 15\"": [("15\" LCD 패널", 1), ("메인보드", 1), ("16GB 메모리", 2),
                          ("512GB SSD", 1), ("배터리팩", 1)],
    "hyunwoo 모니터 27\"": [("27\" LCD 패널", 1), ("스케일러 보드", 1), ("전원 보드", 1),
                          ("모니터 스탠드", 1), ("후면 케이스", 1)],
}
BOM_BY_NAME.update(s4.BOM)
BOM = {NAME2CODE[p]: [(NAME2CODE[cn], qn) for cn, qn in comps] for p, comps in BOM_BY_NAME.items()}
FIN_CODES = [c for c, *_ in FINISHED]


def parse_customers():
    custs = [("CUST-2026-0001", 100000000, "NET30"), ("CUST-2026-0002", 50000000, "NET60")]
    with open(os.path.join(ERP_MIG, "V53__step4_customers.sql"), encoding="utf-8") as f:
        txt = f.read()
    for m in re.finditer(r"\('(CUST-\d{4}-\d{4})',\s*'[^']*',\s*'[^']*',\s*'[^']*',\s*(\d+)\.00,\s*'([A-Z0-9]+)'", txt):
        custs.append((m.group(1), int(m.group(2)), m.group(3)))
    return custs


def parse_vendors():
    vends = [("VEND-2026-0001", "NET30")]
    with open(os.path.join(ERP_MIG, "V54__step4_vendors.sql"), encoding="utf-8") as f:
        txt = f.read()
    for m in re.finditer(r"\('(VEND-\d{4}-\d{4})',\s*'[^']*',\s*'[^']*',\s*'[^']*',\s*'([A-Z0-9]+)'", txt):
        vends.append((m.group(1), m.group(2)))
    return vends


CUSTOMERS = parse_customers()
VENDORS = parse_vendors()
PAY_DAYS = {"NET30": 30, "NET60": 60, "COD": 3, "PREPAID": 0}

# 세션변수명
def iv(code):
    return "@i_" + code.replace("ITEM-", "").replace("-", "_")


def cv(code):
    return "@c_" + code.replace("CUST-", "").replace("-", "_")


def vv(code):
    return "@v_" + code.replace("VEND-", "").replace("-", "_")


ACC = {"CASH": "1100", "AR": "1200", "INVENTORY": "1400", "RAW": "1410",
       "AP": "2100", "VAT": "2200", "SALES": "4100", "COGS": "5100"}
SP_EMAILS = ["kim@hyunwoo.com", "sales.mgr@hyunwoo.com", "sales.global@hyunwoo.com"]

# =============================================================================
# 재고 상태 (WH-HQ) — 초기값 = 기존 시드
# =============================================================================
inv = {}


def reset_inv():
    inv.clear()
    inv[NAME2CODE["hyunwoo 노트북 15\""]] = [50.0, 1000000.0]
    for code, name, cost in PART[:10]:
        inv[code] = [500.0, float(cost)]


def st(code):
    if code not in inv:
        inv[code] = [0.0, 0.0]
    return inv[code]


# 채번
_numctr = {}


def num(prefix, d):
    k = (prefix, pk(d))
    v = _numctr.get(k, 0) + 1
    _numctr[k] = v
    return f"{prefix}-{pk(d)}-{v:03d}"


# SQL 버킷 (연도별)
sql = {2024: [], 2025: [], 2026: []}
stats = {"sales": 0, "prod": 0, "receipt": 0, "je": 0, "open_ar": 0.0, "revenue": 0.0}


def emit(year, s):
    sql[year].append(s)


# =============================================================================
# 판매 계획
# =============================================================================
SALES_WEIGHT = {
    "hyunwoo 노트북 15\"": 10, "hyunwoo 모니터 27\"": 8, "hyunwoo 노트북 비즈니스 13\"": 8,
    "hyunwoo 노트북 프로 17\"": 6, "hyunwoo 모니터 24\"": 6, "hyunwoo 모니터 32\" 커브드": 4,
    "hyunwoo 데스크탑 사무용": 7, "hyunwoo 워크스테이션": 4, "hyunwoo 올인원PC 27\"": 4,
    "hyunwoo 태블릿 10\"": 5, "hyunwoo 태블릿 12\"": 4, "hyunwoo 멤브레인 키보드": 3,
    "hyunwoo 기계식 키보드": 3, "hyunwoo 무선 마우스": 3, "hyunwoo 게이밍 마우스": 3,
    "hyunwoo USB-C 도킹스테이션": 3,
}
WFIN = []
for code, name, cost, price in FINISHED:
    WFIN += [code] * SALES_WEIGHT.get(name, 3)


def cust_weight(limit):
    return 8 if limit >= 1_000_000_000 else 5 if limit >= 300_000_000 else 2 if limit >= 50_000_000 else 1


WCUST = []
for code, limit, pay in CUSTOMERS:
    WCUST += [(code, limit, pay)] * cust_weight(limit)


def months():
    for y in (2024, 2025, 2026):
        for mo in range(1, (6 if y == 2026 else 12) + 1):
            yield y, mo


def mdays(y, mo):
    nxt = date(y + 1, 1, 1) if mo == 12 else date(y, mo + 1, 1)
    return (nxt - date(y, mo, 1)).days


def plan_sales():
    sales = []
    for (y, mo) in months():
        nm = 6 if y == 2026 else 12
        month_target = (TARGET[y] / nm) * random.uniform(0.85, 1.15)
        nd = mdays(y, mo)
        acc = 0.0
        while acc < month_target:
            odate = date(y, mo, random.randint(1, nd))
            cust, limit, pay = random.choice(WCUST)
            want = random.uniform(20_000_000, 120_000_000)
            nlines = random.randint(1, 3)
            chosen, lines = set(), []
            for _ in range(nlines):
                fc = random.choice(WFIN)
                if fc in chosen:
                    continue
                chosen.add(fc)
                price = FIN_PRICE[fc]
                qn = max(1, round((want / nlines) / price))
                lines.append([fc, qn, price])
            subtotal = sum(qn * price for _, qn, price in lines)
            sales.append(dict(odate=odate, cust=cust, pay=pay, lines=lines, subtotal=subtotal))
            acc += subtotal
    sales.sort(key=lambda s: s["odate"])
    return sales


# =============================================================================
# 공급 이벤트 (입고/생산) — 판매 직전 호출, 재고 부족분 선확보
# =============================================================================
def do_receipt(code, qty, rdate, year):
    """부품 입고: stock_movement(+) + 매입분개(차1410/대2100) + 지급(출금)."""
    uc = round(float(CODE2COST[code]) * random.uniform(0.95, 1.05), 2)
    lt = round(qty * uc, 2)
    vend, vpay = random.choice(VENDORS)
    grn = num("GR", rdate)
    b = []
    b.append(f"INSERT INTO goods_receipt (number,vendor_id,warehouse_id,status,receipt_date,posted_at,"
             f"created_at,created_by,updated_at,updated_by) VALUES "
             f"({q(grn)},{vv(vend)},@wh,'POSTED',{q(dt(rdate))},{q(dt(rdate)+' 11:00:00')},{AUDIT});")
    b.append("SET @gr:=LAST_INSERT_ID();")
    b.append(f"INSERT INTO goods_receipt_line (goods_receipt_id,line_no,item_id,quantity,unit_cost,line_total,"
             f"created_at,created_by,updated_at,updated_by) VALUES "
             f"(@gr,1,{iv(code)},{qty4(qty)},{money(uc)},{money(lt)},{AUDIT});")
    b.append(f"INSERT INTO stock_movement (item_id,warehouse_id,qty_delta,unit_cost,reason,ref_type,ref_id,"
             f"moved_at,created_at,created_by,updated_at,updated_by) VALUES "
             f"({iv(code)},@wh,{qty4(qty)},{money(uc)},'GOODS_RECEIPT','GR',@gr,{q(dt(rdate)+' 11:00:00')},{AUDIT});")
    # 매입분개 (부품 → 원재료)
    jen = num("JE", rdate)
    b.append(_je(jen, rdate, f"매입 {grn}", "GR", "@gr",
                 [("RAW", lt, 0)], [("AP", 0, lt)]))
    # 지급(출금): 지급일 = rdate + vpay. <= TODAY 면 출금 처리
    pdate = rdate + timedelta(days=PAY_DAYS.get(vpay, 30))
    if pdate <= LAST:
        pyear = pdate.year
        pnn = num("PAY", pdate)
        b2 = [f"INSERT INTO payment (number,type,customer_id,vendor_id,amount,payment_date,status,posted_at,"
              f"description,created_at,created_by,updated_at,updated_by) VALUES "
              f"({q(pnn)},'DISBURSEMENT',NULL,{vv(vend)},{money(lt)},{q(dt(pdate))},'POSTED',"
              f"{q(dt(pdate)+' 14:00:00')},{q('매입대금 '+grn)},{AUDIT});",
              "SET @pay:=LAST_INSERT_ID();",
              _je(num("JE", pdate), pdate, f"출금 {pnn}", "PAY", "@pay",
                  [("AP", lt, 0)], [("CASH", 0, lt)])]
        emit(pyear, "\n".join(b2))
        stats["je"] += 1
    # 재고/통계
    s = st(code)
    if s[0] <= 0:
        s[1] = uc
    else:
        s[1] = round((s[0] * s[1] + qty * uc) / (s[0] + qty), 2)
    s[0] += qty
    emit(year, "\n".join(b))
    stats["receipt"] += 1
    stats["je"] += 1


def do_production(code, qty, pdate, year):
    """생산: 부품 소비(PRODUCTION_OUT) + 완제품 입고(PRODUCTION_IN) + 분개(차1400/대1410)."""
    pon = num("PO", pdate)
    total_cost = 0.0
    lines, outs = [], []
    ln = 0
    for comp, per in BOM[code]:
        ln += 1
        need = float(per) * qty
        cs = st(comp)
        avg = cs[1]
        cs[0] -= need                      # 출고(평균 불변)
        total_cost += avg * need
        lines.append(f"(@po,{ln},{iv(comp)},{money(need)},{money(avg)},{AUDIT})")
        outs.append(f"({iv(comp)},@wh,-{qty4(need)},{money(avg)},'PRODUCTION_OUT','PROD',@po,"
                    f"{q(dt(pdate)+' 15:00:00')},{AUDIT})")
    unit = round(total_cost / qty, 2) if qty else 0.0
    fs = st(code)
    if fs[0] <= 0:
        fs[1] = unit
    else:
        fs[1] = round((fs[0] * fs[1] + qty * unit) / (fs[0] + qty), 2)
    fs[0] += qty
    b = []
    b.append(f"INSERT INTO production_order (number,product_item_id,warehouse_id,quantity,status,order_date,"
             f"due_date,completed_at,created_at,created_by,updated_at,updated_by) VALUES "
             f"({q(pon)},{iv(code)},@wh,{money(qty)},'COMPLETED',{q(dt(pdate))},{q(dt(pdate))},"
             f"{q(dt(pdate)+' 15:00:00')},{AUDIT});")
    b.append("SET @po:=LAST_INSERT_ID();")
    b.append("INSERT INTO production_order_line (production_order_id,line_no,component_item_id,required_qty,"
             "issued_unit_cost,created_at,created_by,updated_at,updated_by) VALUES\n    "
             + ",\n    ".join(lines) + ";")
    b.append("INSERT INTO stock_movement (item_id,warehouse_id,qty_delta,unit_cost,reason,ref_type,ref_id,"
             "moved_at,created_at,created_by,updated_at,updated_by) VALUES\n    " + ",\n    ".join(outs) + ";")
    b.append(f"INSERT INTO stock_movement (item_id,warehouse_id,qty_delta,unit_cost,reason,ref_type,ref_id,"
             f"moved_at,created_at,created_by,updated_at,updated_by) VALUES "
             f"({iv(code)},@wh,{qty4(qty)},{money(unit)},'PRODUCTION_IN','PROD',@po,"
             f"{q(dt(pdate)+' 15:00:00')},{AUDIT});")
    if total_cost > 0:
        b.append(_je(num("JE", pdate), pdate, f"생산완료 {pon}", "PROD", "@po",
                     [("INVENTORY", total_cost, 0)], [("RAW", 0, total_cost)]))
        stats["je"] += 1
    emit(year, "\n".join(b))
    stats["prod"] += 1


def ensure_comp(code, need, by, year):
    s = st(code)
    if s[0] >= need:
        return
    short = need - s[0]
    # 대량 구매(수개월치 로트) — 잦은 발주 대신 큰 로트. 입고 빈도↓·현실적.
    lot = float(int(short * random.uniform(4.0, 8.0)) + 1)
    rdate = max(by - timedelta(days=random.randint(3, 8)), date(2024, 1, 2))
    do_receipt(code, lot, rdate, year)


def ensure_fin(code, need, by, year):
    s = st(code)
    if s[0] >= need:
        return
    short = need - s[0]
    # 배치 생산(여러 판매분 한 번에) — 생산 빈도↓.
    lot = int(short * random.uniform(2.5, 4.5)) + 1
    pdate = max(by - timedelta(days=random.randint(2, 5)), date(2024, 1, 3))
    for comp, per in BOM[code]:
        ensure_comp(comp, float(per) * lot, pdate, year)
    do_production(code, float(lot), pdate, year)


# =============================================================================
# 판매 SQL
# =============================================================================
def _je(number, d, desc, src, src_var, debits, credits):
    """journal_entry + journal_line 블록. debits/credits = [(ACC_key, debit, credit)]."""
    head = (f"INSERT INTO journal_entry (number,entry_date,description,status,source_type,source_id,posted_at,"
            f"created_at,created_by,updated_at,updated_by) VALUES "
            f"({q(number)},{q(dt(d))},{q(desc)},'POSTED',{q(src)},{src_var},{q(dt(d)+' 16:00:00')},{AUDIT});")
    lines, ln = [], 0
    for key, dr, crv in debits + credits:
        ln += 1
        lines.append(f"(@je,{ln},@a_{ACC[key]},{money(dr)},{money(crv)})")
    body = ("INSERT INTO journal_line (journal_entry_id,line_no,account_id,debit,credit,"
            "created_at,created_by,updated_at,updated_by) VALUES\n    "
            + ",\n    ".join(l[:-1] + f",{AUDIT})" for l in lines) + ";")
    return head + "\nSET @je:=LAST_INSERT_ID();\n" + body


def do_sale(s):
    odate = s["odate"]
    year = odate.year
    sdate = min(odate + timedelta(days=5), LAST)   # 출하일 (오늘 전으로 클램프)
    cdate = min(odate + timedelta(days=2), LAST)   # 확정일
    # 선공급
    for fc, qn, price in s["lines"]:
        ensure_fin(fc, float(qn), sdate, year)
    son = num("SO", odate)
    subtotal = s["subtotal"]
    b = []
    sp = f"(SELECT id FROM employee WHERE email={q(random.choice(SP_EMAILS))})"
    # 입금 여부
    paid_date = sdate + timedelta(days=PAY_DAYS.get(s["pay"], 30))
    closed = paid_date <= LAST
    status = "CLOSED" if closed else "INVOICED"
    b.append(f"INSERT INTO sales_order (number,customer_id,salesperson_id,quotation_id,status,order_date,"
             f"confirmed_at,total_amount,created_at,created_by,updated_at,updated_by) VALUES "
             f"({q(son)},{cv(s['cust'])},{sp},NULL,{q(status)},{q(dt(odate))},{q(dt(cdate)+' 09:00:00')},"
             f"{money(subtotal)},{AUDIT});")
    b.append("SET @so:=LAST_INSERT_ID();")
    sol_vars = []
    for i, (fc, qn, price) in enumerate(s["lines"], 1):
        lt = qn * price
        b.append(f"INSERT INTO sales_order_line (sales_order_id,line_no,item_id,order_qty,shipped_qty,"
                 f"invoiced_qty,unit_price,line_total,created_at,created_by,updated_at,updated_by) VALUES "
                 f"(@so,{i},{iv(fc)},{qty4(qn)},{qty4(qn)},{qty4(qn)},{money(price)},{money(lt)},{AUDIT});")
        b.append(f"SET @sol{i}:=LAST_INSERT_ID();")
        sol_vars.append(f"@sol{i}")
    # delivery
    dln = num("DLV", sdate)
    b.append(f"INSERT INTO delivery (number,sales_order_id,warehouse_id,status,shipped_date,"
             f"created_at,created_by,updated_at,updated_by) VALUES "
             f"({q(dln)},@so,@wh,'SHIPPED',{q(dt(sdate))},{AUDIT});")
    b.append("SET @dlv:=LAST_INSERT_ID();")
    dl = ",\n    ".join(f"(@dlv,{sol_vars[i]},{i+1},{qty4(qn)},{AUDIT})"
                        for i, (fc, qn, price) in enumerate(s["lines"]))
    b.append("INSERT INTO delivery_line (delivery_id,sales_order_line_id,line_no,quantity,"
             "created_at,created_by,updated_at,updated_by) VALUES\n    " + dl + ";")
    # goods_issue + movement (출고 평균)
    gin = num("GI", sdate)
    b.append(f"INSERT INTO goods_issue (number,warehouse_id,status,issue_date,reason,posted_at,delivery_id,"
             f"created_at,created_by,updated_at,updated_by) VALUES "
             f"({q(gin)},@wh,'POSTED',{q(dt(sdate))},'SHIPMENT',{q(dt(sdate)+' 10:00:00')},@dlv,{AUDIT});")
    b.append("SET @gi:=LAST_INSERT_ID();")
    gil, movs, total_cost = [], [], 0.0
    for i, (fc, qn, price) in enumerate(s["lines"], 1):
        avg = st(fc)[1]
        st(fc)[0] -= qn                    # 완제품 출고
        total_cost += avg * qn
        gil.append(f"(@gi,{i},{iv(fc)},{qty4(qn)},{AUDIT})")
        movs.append(f"({iv(fc)},@wh,-{qty4(qn)},{money(avg)},'GOODS_ISSUE','GI',@gi,"
                    f"{q(dt(sdate)+' 10:00:00')},{AUDIT})")
    b.append("INSERT INTO goods_issue_line (goods_issue_id,line_no,item_id,quantity,"
             "created_at,created_by,updated_at,updated_by) VALUES\n    " + ",\n    ".join(gil) + ";")
    b.append("INSERT INTO stock_movement (item_id,warehouse_id,qty_delta,unit_cost,reason,ref_type,ref_id,"
             "moved_at,created_at,created_by,updated_at,updated_by) VALUES\n    " + ",\n    ".join(movs) + ";")
    # invoice
    tax = round(subtotal * 0.10, 2)
    total = subtotal + tax
    inn = num("INV", sdate)
    b.append(f"INSERT INTO invoice (number,sales_order_id,status,invoice_date,subtotal,tax_amount,total_amount,"
             f"created_at,created_by,updated_at,updated_by) VALUES "
             f"({q(inn)},@so,'ISSUED',{q(dt(sdate))},{money(subtotal)},{money(tax)},{money(total)},{AUDIT});")
    b.append("SET @inv:=LAST_INSERT_ID();")
    il = ",\n    ".join(f"(@inv,{sol_vars[i]},{i+1},{qty4(qn)},{money(price)},{money(qn*price)},{AUDIT})"
                        for i, (fc, qn, price) in enumerate(s["lines"]))
    b.append("INSERT INTO invoice_line (invoice_id,sales_order_line_id,line_no,quantity,unit_price,line_total,"
             "created_at,created_by,updated_at,updated_by) VALUES\n    " + il + ";")
    # 매출 분개 (차1200 total / 대4100 subtotal, 대2200 tax)
    b.append(_je(num("JE", sdate), sdate, f"매출 {inn}", "INV", "@inv",
                 [("AR", total, 0)], [("SALES", 0, subtotal), ("VAT", 0, tax)]))
    stats["je"] += 1
    # 매출원가 분개 (차5100 / 대1400)
    if total_cost > 0:
        b.append(_je(num("JE", sdate), sdate, f"매출원가 {gin}", "GI", "@gi",
                     [("COGS", total_cost, 0)], [("INVENTORY", 0, total_cost)]))
        stats["je"] += 1
    # 입금 (완결 시)
    if closed:
        pnn = num("PAY", paid_date)
        b.append(f"INSERT INTO payment (number,type,customer_id,vendor_id,amount,payment_date,status,posted_at,"
                 f"description,created_at,created_by,updated_at,updated_by) VALUES "
                 f"({q(pnn)},'RECEIPT',{cv(s['cust'])},NULL,{money(total)},{q(dt(paid_date))},'POSTED',"
                 f"{q(dt(paid_date)+' 14:00:00')},{q('수금 '+inn)},{AUDIT});")
        b.append("SET @pay:=LAST_INSERT_ID();")
        b.append(_je(num("JE", paid_date), paid_date, f"입금 {pnn}", "PAY", "@pay",
                     [("CASH", total, 0)], [("AR", 0, total)]))
        stats["je"] += 1
    else:
        stats["open_ar"] += total
    emit(year, "\n".join(b))
    stats["sales"] += 1
    stats["revenue"] += subtotal


# =============================================================================
# 실행
# =============================================================================
def header_block():
    """세션변수 로드 (각 파일 앞)."""
    lines = ["-- 마스터 id 세션변수 로드 (이 파일 내에서 사용)"]
    lines.append("SET @wh := (SELECT id FROM warehouse WHERE code='WH-HQ');")
    for k, code in ACC.items():
        lines.append(f"SET @a_{code} := (SELECT id FROM account WHERE code='{code}');")
    for c in ALL_ITEM_CODES:
        lines.append(f"SET {iv(c)} := (SELECT id FROM item WHERE code='{c}');")
    for c, limit, pay in CUSTOMERS:
        lines.append(f"SET {cv(c)} := (SELECT id FROM customer WHERE code='{c}');")
    for c, pay in VENDORS:
        lines.append(f"SET {vv(c)} := (SELECT id FROM vendor WHERE code='{c}');")
    return "\n".join(lines)


def final_stock_sql():
    """최종 WH-HQ 재고 UPSERT (stock 캐시 = 시뮬 결과)."""
    lines = ["-- STEP5 종료 시점 WH-HQ 재고 확정 (stock_movement 누적과 일치)"]
    rows = []
    for code in ALL_ITEM_CODES:
        if code in inv:
            qn, avg = inv[code]
            qn = round(qn, 4)
            if qn < 0:
                qn = 0.0  # 방어(정상 시뮬이면 발생 안 함)
            rows.append(f"({iv(code)},@wh,{qty4(qn)},{money(avg)},0,{AUDIT})")
    lines.append("INSERT INTO stock (item_id,warehouse_id,qty_on_hand,average_cost,version,"
                 "created_at,created_by,updated_at,updated_by) VALUES\n    " + ",\n    ".join(rows)
                 + "\nON DUPLICATE KEY UPDATE qty_on_hand=VALUES(qty_on_hand), "
                 "average_cost=VALUES(average_cost), updated_at=NOW();")
    return "\n".join(lines)


def main():
    reset_inv()
    sales = plan_sales()
    for s in sales:
        do_sale(s)

    files = [
        ("V60__step5_txn_2024.sql", 2024, "2024년 실거래 (구매·생산·판매·입금 완결 체인)"),
        ("V61__step5_txn_2025.sql", 2025, "2025년 실거래"),
        ("V62__step5_txn_2026h1.sql", 2026, "2026년 상반기 실거래"),
    ]
    for fname, year, desc in files:
        body = "\n\n".join(sql[year])
        content = (f"-- STEP 5 (실무 리얼리즘 확장): {desc}\n"
                   f"-- 결정론적 시뮬레이터 산출 (tools/step5-seed/generate.py). 완결 상태·이동평균·차대균형.\n\n"
                   + header_block() + "\n\n" + body + "\n")
        with open(os.path.join(ERP_MIG, fname), "w", encoding="utf-8") as f:
            f.write(content)
        print(f"  wrote {fname}  ({content.count(chr(10))} lines, {len(sql[year])} 거래블록)")

    # 최종 재고 (별도 파일, 세션변수 필요)
    fs = ("-- STEP 5: 종료 시점 재고 확정 (WH-HQ)\n\n" + header_block() + "\n\n" + final_stock_sql() + "\n")
    with open(os.path.join(ERP_MIG, "V63__step5_final_stock.sql"), "w", encoding="utf-8") as f:
        f.write(fs)
    print(f"  wrote V63__step5_final_stock.sql")

    print("\n=== 요약 (scale={}) ===".format(SCALE))
    print(f"판매 {stats['sales']} / 생산 {stats['prod']} / 입고 {stats['receipt']} / 전표 {stats['je']}")
    print(f"매출(공급가 합): {stats['revenue']:,.0f}원  (목표 {sum(TARGET.values()):,})")
    print(f"오늘 시점 미수금(미입금 AR, VAT포함): {stats['open_ar']:,.0f}원")
    neg = [(c, inv[c][0]) for c in inv if inv[c][0] < -0.001]
    print(f"음수 재고 항목: {len(neg)}" + ("  ⚠️ " + str(neg[:5]) if neg else " (없음 ✓)"))


if __name__ == "__main__":
    main()
