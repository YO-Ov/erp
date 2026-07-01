#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
STEP 4 — 마스터 데이터 대량 확장 SQL 생성기 (결정론적).

기준 문서: doc/실무리얼리즘확장-STEP4-카탈로그.md
이 스크립트는 카탈로그를 인코딩해 Flyway 마이그레이션 SQL(V48~V58) + MES(V9) 을 산출한다.
정합성(개시재고=개시분개, code_sequence, BOM 부품명 유일성)을 코드로 강제한다.

사용법:  python3 generate.py
출력:    ../../hwlee-erp/src/main/resources/db/migration/V48..V58__step4_*.sql
         ../../hwlee-mes/src/main/resources/db/migration/V9__step4_equipment.sql
"""
import os
import random

random.seed(20260701)  # 결정론적 — 재실행해도 동일 SQL

HERE = os.path.dirname(os.path.abspath(__file__))
ERP_MIG = os.path.normpath(os.path.join(HERE, "../../hwlee-erp/src/main/resources/db/migration"))
MES_MIG = os.path.normpath(os.path.join(HERE, "../../hwlee-mes/src/main/resources/db/migration"))

AUDIT = "NOW(), 'system', NOW(), 'system'"          # created_at,created_by,updated_at,updated_by
AUDIT_DEL = "NOW(), 'system', NOW(), 'system', NULL"  # + deleted_at
PW_HASH = "$2y$10$fiDYm50/gYzbfsId2MFJqOYpNXfQwoGAX79657DhyryCMTzgbPRoO"  # pass1234


def sqlstr(s):
    return "'" + s.replace("'", "''") + "'"


def header(title, phase_note):
    return f"-- STEP 4 (실무 리얼리즘 확장): {title}\n-- {phase_note}\n\n"


# =============================================================================
# 1. 카테고리 (V48)
# =============================================================================
CAT_FINISHED = [  # (code, name, sort)
    ("DESKTOP", "데스크탑", 30), ("AIO", "올인원PC", 40), ("TABLET", "태블릿", 50),
    ("KEYBOARD", "키보드", 60), ("MOUSE", "마우스", 70), ("DOCK", "도킹스테이션", 80),
]
CAT_PART = [
    ("PART_DISPLAY", "디스플레이", 110), ("PART_BOARD", "보드", 120),
    ("PART_COMPUTE", "연산모듈(CPU/GPU)", 130), ("PART_MEMORY", "메모리", 140),
    ("PART_STORAGE", "저장장치", 150), ("PART_BATTERY", "배터리", 160),
    ("PART_POWER", "전원/어댑터", 170), ("PART_CHASSIS", "섀시/케이스", 180),
    ("PART_INPUT", "입력장치 부품", 190), ("PART_ETC", "기타 부품(케이블 등)", 900),
]


def gen_categories():
    lines = [header("품목 카테고리 확장 (완제품 6 + 부품 세분 10)",
                    "기존 3종(NOTEBOOK/MONITOR/PART) 위에 확장. PART 는 레거시로 INACTIVE 전환.")]
    rows = []
    for code, name, srt in CAT_FINISHED + CAT_PART:
        rows.append(f"    ({sqlstr(code)}, {sqlstr(name)}, {srt}, 'ACTIVE', {AUDIT_DEL})")
    lines.append("INSERT INTO item_category (code, name, sort_order, status, "
                 "created_at, created_by, updated_at, updated_by, deleted_at) VALUES\n"
                 + ",\n".join(rows) + ";\n\n")
    lines.append("-- 기존 PART 는 세분 카테고리로 대체되므로 레거시 처리(신규 등록 드롭다운에서 숨김).\n")
    lines.append("UPDATE item_category SET name = '부품(레거시)', sort_order = 990, status = 'INACTIVE', "
                 "updated_at = NOW(), updated_by = 'system' WHERE code = 'PART';\n")
    return "".join(lines)


# =============================================================================
# 2. 품목 — 완제품 14 신규 + 부품 24 신규 + 기존 부품 10 재분류 (V49)
# =============================================================================
# 완제품(신규): (category, name, price, cost)
FINISHED_NEW = [
    ("NOTEBOOK", "hyunwoo 노트북 비즈니스 13\"", 1200000, 800000),
    ("NOTEBOOK", "hyunwoo 노트북 프로 17\"", 2500000, 1700000),
    ("MONITOR", "hyunwoo 모니터 24\"", 250000, 150000),
    ("MONITOR", "hyunwoo 모니터 32\" 커브드", 550000, 350000),
    ("DESKTOP", "hyunwoo 데스크탑 사무용", 900000, 600000),
    ("DESKTOP", "hyunwoo 워크스테이션", 2200000, 1500000),
    ("AIO", "hyunwoo 올인원PC 27\"", 1300000, 900000),
    ("TABLET", "hyunwoo 태블릿 10\"", 500000, 320000),
    ("TABLET", "hyunwoo 태블릿 12\"", 800000, 550000),
    ("KEYBOARD", "hyunwoo 멤브레인 키보드", 30000, 15000),
    ("KEYBOARD", "hyunwoo 기계식 키보드", 90000, 50000),
    ("MOUSE", "hyunwoo 무선 마우스", 40000, 20000),
    ("MOUSE", "hyunwoo 게이밍 마우스", 70000, 40000),
    ("DOCK", "hyunwoo USB-C 도킹스테이션", 150000, 90000),
]
# 부품(신규): (category, name, cost). 이름은 기존 부품과 겹치지 않게(BOM JOIN=이름).
PART_NEW = [
    ("PART_DISPLAY", "LCD 패널 13\"", 180000),
    ("PART_DISPLAY", "LCD 패널 17\"", 300000),
    ("PART_DISPLAY", "LCD 패널 24\"", 120000),
    ("PART_DISPLAY", "LCD 패널 32\"", 350000),
    ("PART_DISPLAY", "LCD 패널 10\"(태블릿)", 90000),
    ("PART_DISPLAY", "LCD 패널 12\"(태블릿)", 140000),
    ("PART_BOARD", "데스크탑 메인보드", 130000),
    ("PART_COMPUTE", "CPU 모듈(보급형)", 180000),
    ("PART_COMPUTE", "CPU 모듈(고성능)", 350000),
    ("PART_COMPUTE", "GPU 모듈", 280000),
    ("PART_MEMORY", "메모리 8GB", 30000),
    ("PART_MEMORY", "메모리 32GB", 90000),
    ("PART_STORAGE", "SSD 256GB", 60000),
    ("PART_STORAGE", "SSD 1TB", 250000),
    ("PART_BATTERY", "태블릿 배터리", 60000),
    ("PART_POWER", "전원어댑터", 25000),
    ("PART_POWER", "내부 케이블세트", 8000),
    ("PART_CHASSIS", "노트북 케이스", 70000),
    ("PART_CHASSIS", "데스크탑 케이스", 50000),
    ("PART_CHASSIS", "AIO 케이스", 110000),
    ("PART_INPUT", "키캡세트", 6000),
    ("PART_INPUT", "기계식 스위치", 12000),
    ("PART_INPUT", "광센서 모듈", 9000),
    ("PART_INPUT", "마우스 하우징", 5000),
]
# 기존 부품 10종 재분류: (기존 name, 신규 category)
PART_RECLASSIFY = [
    ("15\" LCD 패널", "PART_DISPLAY"), ("27\" LCD 패널", "PART_DISPLAY"),
    ("메인보드", "PART_BOARD"), ("스케일러 보드", "PART_BOARD"), ("전원 보드", "PART_BOARD"),
    ("16GB 메모리", "PART_MEMORY"), ("512GB SSD", "PART_STORAGE"),
    ("배터리팩", "PART_BATTERY"), ("모니터 스탠드", "PART_CHASSIS"), ("후면 케이스", "PART_CHASSIS"),
]

ITEM_SEQ_START = 13  # 기존 ITEM-2026-0012 까지 사용 → 신규는 0013 부터 (code_sequence next=13)


def item_code(n):
    return f"ITEM-2026-{n:04d}"


def gen_items():
    lines = [header("품목 대량 확장 (완제품 14 + 부품 24 신규, 기존 부품 10 재분류)",
                    "카테고리(V48)·공장(V47) 이후. code=ITEM-2026-NNNN, 0013 부터 이어서 발급.")]
    n = ITEM_SEQ_START
    rows = []
    for cat, name, price, cost in FINISHED_NEW:
        rows.append(f"    ({sqlstr(item_code(n))}, {sqlstr(name)}, {sqlstr(cat)}, 'FINISHED', 'EA', "
                    f"{cost}.00, {price}.00, 'ACTIVE', {AUDIT_DEL})")
        n += 1
    for cat, name, cost in PART_NEW:
        rows.append(f"    ({sqlstr(item_code(n))}, {sqlstr(name)}, {sqlstr(cat)}, 'COMPONENT', 'EA', "
                    f"{cost}.00, {cost}.00, 'ACTIVE', {AUDIT_DEL})")
        n += 1
    lines.append("-- 1) 신규 품목 (완제품=FINISHED / 부품=COMPONENT). 부품 판매가는 원가와 동일(내부품).\n")
    lines.append("INSERT INTO item (code, name, category, item_type, unit, standard_cost, standard_price, "
                 "status, created_at, created_by, updated_at, updated_by, deleted_at) VALUES\n"
                 + ",\n".join(rows) + ";\n\n")
    lines.append(f"UPDATE code_sequence SET next_number = {n}, updated_at = NOW() "
                 f"WHERE prefix = 'ITEM' AND period_key = '2026';\n\n")
    lines.append("-- 2) 기존 부품 10종 재분류 (category: PART → 세분 카테고리). 이름/코드는 유지(BOM JOIN 보존).\n")
    for name, cat in PART_RECLASSIFY:
        lines.append(f"UPDATE item SET category = {sqlstr(cat)}, updated_at = NOW(), updated_by = 'system' "
                     f"WHERE name = {sqlstr(name)} AND item_type = 'COMPONENT';\n")
    return "".join(lines), n


# =============================================================================
# 3. BOM — 신규 완제품 14 (기존 노트북15/모니터27 은 V35/V45 유지) (V50)
# =============================================================================
# 완제품명 → [(부품명, 수량)]
BOM = {
    "hyunwoo 노트북 비즈니스 13\"": [("LCD 패널 13\"", 1), ("메인보드", 1), ("CPU 모듈(보급형)", 1),
                                  ("메모리 8GB", 1), ("SSD 256GB", 1), ("배터리팩", 1),
                                  ("노트북 케이스", 1), ("전원어댑터", 1), ("내부 케이블세트", 1)],
    "hyunwoo 노트북 프로 17\"": [("LCD 패널 17\"", 1), ("메인보드", 1), ("CPU 모듈(고성능)", 1),
                              ("GPU 모듈", 1), ("메모리 32GB", 1), ("SSD 1TB", 1), ("배터리팩", 1),
                              ("노트북 케이스", 1), ("전원어댑터", 1)],
    "hyunwoo 모니터 24\"": [("LCD 패널 24\"", 1), ("스케일러 보드", 1), ("전원 보드", 1),
                          ("모니터 스탠드", 1), ("후면 케이스", 1)],
    "hyunwoo 모니터 32\" 커브드": [("LCD 패널 32\"", 1), ("스케일러 보드", 1), ("전원 보드", 1),
                               ("모니터 스탠드", 1), ("후면 케이스", 1)],
    "hyunwoo 데스크탑 사무용": [("데스크탑 메인보드", 1), ("CPU 모듈(보급형)", 1), ("16GB 메모리", 1),
                           ("SSD 256GB", 1), ("데스크탑 케이스", 1), ("전원 보드", 1), ("전원어댑터", 1)],
    "hyunwoo 워크스테이션": [("데스크탑 메인보드", 1), ("CPU 모듈(고성능)", 1), ("GPU 모듈", 1),
                        ("메모리 32GB", 2), ("SSD 1TB", 1), ("데스크탑 케이스", 1), ("전원 보드", 1)],
    "hyunwoo 올인원PC 27\"": [("27\" LCD 패널", 1), ("데스크탑 메인보드", 1), ("CPU 모듈(보급형)", 1),
                           ("16GB 메모리", 1), ("512GB SSD", 1), ("AIO 케이스", 1), ("전원 보드", 1)],
    "hyunwoo 태블릿 10\"": [("LCD 패널 10\"(태블릿)", 1), ("메인보드", 1), ("CPU 모듈(보급형)", 1),
                         ("메모리 8GB", 1), ("SSD 256GB", 1), ("태블릿 배터리", 1), ("전원어댑터", 1)],
    "hyunwoo 태블릿 12\"": [("LCD 패널 12\"(태블릿)", 1), ("메인보드", 1), ("CPU 모듈(보급형)", 1),
                         ("16GB 메모리", 1), ("512GB SSD", 1), ("태블릿 배터리", 1), ("전원어댑터", 1)],
    "hyunwoo 멤브레인 키보드": [("키캡세트", 1), ("내부 케이블세트", 1)],
    "hyunwoo 기계식 키보드": [("키캡세트", 1), ("기계식 스위치", 1), ("내부 케이블세트", 1)],
    "hyunwoo 무선 마우스": [("광센서 모듈", 1), ("마우스 하우징", 1), ("내부 케이블세트", 1)],
    "hyunwoo 게이밍 마우스": [("광센서 모듈", 1), ("마우스 하우징", 1), ("내부 케이블세트", 1)],
    "hyunwoo USB-C 도킹스테이션": [("전원 보드", 1), ("내부 케이블세트", 1), ("전원어댑터", 1)],
}
# 존재하는 모든 부품명(기존10 + 신규24) — BOM 무결성 검증용
EXISTING_PARTS = {"15\" LCD 패널", "메인보드", "16GB 메모리", "512GB SSD", "배터리팩",
                  "27\" LCD 패널", "스케일러 보드", "전원 보드", "모니터 스탠드", "후면 케이스"}
ALL_PARTS = EXISTING_PARTS | {p[1] for p in PART_NEW}


def gen_bom():
    # 무결성: BOM 부품명이 전부 실재하는가
    for prod, comps in BOM.items():
        for cname, _ in comps:
            assert cname in ALL_PARTS, f"BOM 부품명 미존재: {cname} (in {prod})"
    # 부품명 유일성(신규 부품 이름끼리·기존과 충돌 없음)
    new_names = [p[1] for p in PART_NEW]
    assert len(new_names) == len(set(new_names)), "신규 부품명 중복"
    assert EXISTING_PARTS.isdisjoint(set(new_names)), "신규 부품명이 기존과 충돌"

    lines = [header("완제품 BOM 14종 (기존 노트북15/모니터27 은 유지)",
                    "부품명(name)으로 JOIN — 부품/완제품 이름 유일성 전제(V49). 완제품 1대당 소요량.")]
    for prod, comps in BOM.items():
        union = "\n        UNION ALL ".join(
            [f"SELECT {sqlstr(c)} AS cname, {q} AS qty" for c, q in comps])
        lines.append(
            "INSERT INTO bom (product_item_id, component_item_id, quantity, "
            "created_at, created_by, updated_at, updated_by)\n"
            "SELECT p.id, c.id, q.qty, NOW(), 'system', NOW(), 'system'\n"
            "  FROM item p\n"
            f"  JOIN (\n        {union}\n       ) q ON 1 = 1\n"
            "  JOIN item c ON c.name = q.cname AND c.item_type = 'COMPONENT'\n"
            f" WHERE p.name = {sqlstr(prod)} AND p.item_type = 'FINISHED';\n\n")
    return "".join(lines)


# =============================================================================
# 4. 창고 (V51) — WH-HQ 중앙물류 재배정 + 공장창고 3 신설
# =============================================================================
WAREHOUSES_NEW = [  # (code, name, factory_code, address)
    ("WH-SW", "수원공장창고", "FAC-01", "경기도 수원시 영통구 삼성로 129"),
    ("WH-GM", "구미공장창고", "FAC-02", "경상북도 구미시 1공단로 197"),
    ("WH-GJ", "광주공장창고", "FAC-03", "광주광역시 광산구 하남산단6번로 107"),
]


def gen_warehouses():
    lines = [header("창고 확장 (중앙물류 WH-HQ 재배정 + 공장창고 3 신설)",
                    "WH-HQ 는 공장 미소속(중앙물류)로 되돌리고, 공장별 창고 3개를 각 공장에 소속시킨다.")]
    lines.append("-- 1) 본사중앙창고: 공장 미소속(중앙물류 거점)으로 재배정. 이름도 명확화.\n")
    lines.append("UPDATE warehouse SET factory_id = NULL, name = '본사중앙창고', "
                 "updated_at = NOW(), updated_by = 'system' WHERE code = 'WH-HQ';\n\n")
    lines.append("-- 2) 공장별 창고 3개 신설 (각 공장에 소속).\n")
    for code, name, fac, addr in WAREHOUSES_NEW:
        lines.append(
            "INSERT INTO warehouse (code, name, address, factory_id, status, "
            "created_at, created_by, updated_at, updated_by, deleted_at)\n"
            f"SELECT {sqlstr(code)}, {sqlstr(name)}, {sqlstr(addr)}, f.id, 'ACTIVE', {AUDIT_DEL}\n"
            f"  FROM factory f WHERE f.code = {sqlstr(fac)};\n\n")
    return "".join(lines)


# =============================================================================
# 5. 부서 재편 + 확장 (V52)
# =============================================================================
# 신규 본부(레벨2, parent=DEPT-HQ)
DEPT_L2 = [
    ("DEPT-MGMT", "경영지원본부"), ("DEPT-SALESHQ", "영업본부"), ("DEPT-PRODHQ", "생산본부"),
    ("DEPT-LOGIS", "물류팀"), ("DEPT-RND", "연구개발팀"),
]
# 신규 팀(레벨3): (code, name, parent_code)
DEPT_L3 = [
    ("DEPT-GA", "총무팀", "DEPT-MGMT"),
    ("DEPT-SALES2", "국내영업2팀", "DEPT-SALESHQ"),
    ("DEPT-SALESGL", "해외영업팀", "DEPT-SALESHQ"),
    ("DEPT-PROD-SW", "수원생산팀", "DEPT-PRODHQ"),
    ("DEPT-PROD-GM", "구미생산팀", "DEPT-PRODHQ"),
    ("DEPT-QC", "품질관리팀", "DEPT-PRODHQ"),
]
# 기존 부서 재편: (code, 신규 name, 신규 parent_code)
DEPT_REORG = [
    ("DEPT-SALES", "국내영업1팀", "DEPT-SALESHQ"),
    ("DEPT-PRODUCTION", "광주생산팀", "DEPT-PRODHQ"),
    ("DEPT-FINANCE", "재무팀", "DEPT-MGMT"),
    ("DEPT-HR", "인사팀", "DEPT-MGMT"),
]


def gen_departments():
    lines = [header("조직(부서) 재편 + 확장 (본부-팀 2계층)",
                    "기존 flat 6부서를 본부-팀 트리로 재편. 기존 코드는 유지(직원 FK·user_role 매핑 보존).")]
    lines.append("-- 1) 본부/HQ직속 팀 (parent = DEPT-HQ).\n")
    for code, name in DEPT_L2:
        lines.append(
            "INSERT INTO department (code, name, parent_id, status, "
            "created_at, created_by, updated_at, updated_by, deleted_at)\n"
            f"SELECT {sqlstr(code)}, {sqlstr(name)}, h.id, 'ACTIVE', {AUDIT_DEL}\n"
            "  FROM department h WHERE h.code = 'DEPT-HQ';\n")
    lines.append("\n-- 2) 본부 하위 팀 (parent = 각 본부).\n")
    for code, name, parent in DEPT_L3:
        lines.append(
            "INSERT INTO department (code, name, parent_id, status, "
            "created_at, created_by, updated_at, updated_by, deleted_at)\n"
            f"SELECT {sqlstr(code)}, {sqlstr(name)}, p.id, 'ACTIVE', {AUDIT_DEL}\n"
            f"  FROM department p WHERE p.code = {sqlstr(parent)};\n")
    lines.append("\n-- 3) 기존 부서 재편 (이름·소속 변경. 코드 불변 → 기존 직원/권한 보존).\n")
    for code, name, parent in DEPT_REORG:
        lines.append(
            f"UPDATE department SET name = {sqlstr(name)}, "
            f"parent_id = (SELECT id FROM (SELECT id FROM department WHERE code = {sqlstr(parent)}) t), "
            f"updated_at = NOW(), updated_by = 'system' WHERE code = {sqlstr(code)};\n")
    return "".join(lines)


# =============================================================================
# 6. 고객 100 (기존 2 + 신규 98) (V53)
# =============================================================================
CUST_PREFIX = ["우진", "대성", "한빛", "신우", "미래로", "세종", "동방", "태평", "onnuri", "삼정",
               "가온", "누리", "다산", "명성", "백두", "서일", "예일", "청우", "하나로", "금강",
               "은성", "정우", "태산", "한결", "해든", "동해", "예성", "우성", "지성", "창신"]
CUST_SUFFIX = ["전자", "시스템", "테크", "유통", "컴퓨터", "산업", "디지털", "상사"]
REGIONS = ["서울시 강남구", "서울시 금천구", "경기도 성남시", "경기도 수원시", "경기도 안양시",
           "인천시 남동구", "대전시 유성구", "대구시 달서구", "부산시 해운대구", "광주시 광산구",
           "울산시 남구", "충북 청주시", "전북 전주시", "경남 창원시", "강원 춘천시"]
PAY_TERMS = ["NET30", "NET60", "COD", "PREPAID"]
PUBLIC_CUST = [  # 공공/교육 (파레토 상위, 대형 한도)
    "서울대학교", "한국과학기술원", "부산광역시청", "경기도교육청", "대전광역시청",
    "국립중앙도서관", "인천광역시교육청", "한국전력공사", "국민건강보험공단", "한국도로공사",
]


def biz_no():
    return f"{random.randint(100,999)}-{random.randint(10,99)}-{random.randint(10000,99999)}"


def gen_customers():
    lines = [header("고객 마스터 대량 확장 (기존 2 + 신규 98 = 100)",
                    "파레토: 상위 대형(공공·대기업)에 고액 한도, 다수 중소에 소액/현금. 확보연도 2024~2026 분산.")]
    # 이름 유일 조합
    combos = []
    for p in CUST_PREFIX:
        for s in CUST_SUFFIX:
            combos.append(p + s)
    random.shuffle(combos)

    seq = {2024: 1, 2025: 1, 2026: 3}   # 기존 CUST-2026-0001/0002 → 2026 은 3 부터
    used_names = {"신원전자", "한솔테크"}
    rows = []
    total = 98
    n_public = len(PUBLIC_CUST)
    n_private = total - n_public

    def pick_year(i):
        # 설립초기(2024) 다수, 이후 점증
        r = random.random()
        return 2024 if r < 0.5 else (2025 if r < 0.8 else 2026)

    def credit_for(is_large):
        if is_large:
            return random.choice([1000000000, 300000000])       # 10억 / 3억
        return random.choice([50000000, 50000000, 0])            # 5천만(다수) / 현금0

    idx = 0
    # 공공/교육 (전부 대형)
    for name in PUBLIC_CUST:
        y = pick_year(idx); idx += 1
        code = f"CUST-{y}-{seq[y]:04d}"; seq[y] += 1
        limit = random.choice([1000000000, 300000000])
        pay = random.choice(["NET30", "NET60"])
        rows.append((code, name, biz_no(), random.choice(REGIONS), limit, pay))
    # 민간
    large_left = 12  # 상위 대형(민간) 대략 12개 → 공공10 + 민간12 ≈ 상위 22
    ci = 0
    for _ in range(n_private):
        while combos[ci] in used_names:
            ci += 1
        name = combos[ci]; ci += 1; used_names.add(name)
        y = pick_year(idx); idx += 1
        code = f"CUST-{y}-{seq[y]:04d}"; seq[y] += 1
        is_large = large_left > 0
        if is_large:
            large_left -= 1
        rows.append((code, name, biz_no(), random.choice(REGIONS),
                     credit_for(is_large), random.choice(PAY_TERMS)))

    # 정렬(코드순 보기 좋게)
    rows.sort(key=lambda r: r[0])
    values = []
    for code, name, bno, addr, limit, pay in rows:
        values.append(f"    ({sqlstr(code)}, {sqlstr(name)}, {sqlstr(bno)}, {sqlstr(addr)}, "
                      f"{limit}.00, {sqlstr(pay)}, 'ACTIVE', {AUDIT_DEL})")
    lines.append("INSERT INTO customer (code, name, business_no, address, credit_limit, payment_terms, "
                 "status, created_at, created_by, updated_at, updated_by, deleted_at) VALUES\n"
                 + ",\n".join(values) + ";\n\n")
    # code_sequence 연도별 갱신
    lines.append("-- 채번 시퀀스: 연도별 마지막+1. (period_key 별 UPSERT)\n")
    lines.append("INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES\n"
                 f"    ('CUST', '2024', {seq[2024]}, NOW()),\n"
                 f"    ('CUST', '2025', {seq[2025]}, NOW()),\n"
                 f"    ('CUST', '2026', {seq[2026]}, NOW())\n"
                 "ON DUPLICATE KEY UPDATE next_number = VALUES(next_number), updated_at = NOW();\n")
    return "".join(lines)


# =============================================================================
# 7. 거래처/공급사 ~18 (기존 1 + 신규 17) (V54)
# =============================================================================
VENDORS_NEW = [
    "동진디스플레이", "우성반도체", "한빛스토리지", "대륙PCB", "정밀배터리", "삼우정밀",
    "코리아파워", "신성입력기기", "한일케이블", "미래메모리", "광성전자", "대한소재",
    "성진부품", "동양전자부품", "케이텍정밀", "우진화학소재", "한국사출",
]


def gen_vendors():
    lines = [header("거래처/공급사 확장 (기존 1 + 신규 17 ≈ 18)",
                    "부품군별 공급사. 설립 초기(2024)에 공급망 구축 → VEND-2024 로 확보.")]
    rows = []
    for i, name in enumerate(VENDORS_NEW, start=1):
        code = f"VEND-2024-{i:04d}"
        pay = random.choice(["NET30", "NET60"])
        rows.append(f"    ({sqlstr(code)}, {sqlstr(name)}, {sqlstr(biz_no())}, "
                    f"{sqlstr(random.choice(REGIONS))}, {sqlstr(pay)}, 'ACTIVE', {AUDIT_DEL})")
    lines.append("INSERT INTO vendor (code, name, business_no, address, payment_terms, status, "
                 "created_at, created_by, updated_at, updated_by, deleted_at) VALUES\n"
                 + ",\n".join(rows) + ";\n\n")
    lines.append("INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES\n"
                 f"    ('VEND', '2024', {len(VENDORS_NEW)+1}, NOW())\n"
                 "ON DUPLICATE KEY UPDATE next_number = VALUES(next_number), updated_at = NOW();\n")
    return "".join(lines)


# =============================================================================
# 8. 직원 105 신규 + 급여계약 106 (V55, V56) + 로그인 8 (V57)
# =============================================================================
SURNAMES = list("김이박최정강조윤장임한오서신권황안송전홍고문양손배백허유남심노정하곽성차주우구민류")
GIVEN = ["민준", "서연", "도윤", "지우", "예준", "하은", "지호", "수아", "지훈", "서윤", "현우", "지원",
         "준서", "다은", "건우", "채원", "우진", "유진", "성민", "예은", "재윤", "가은", "동현", "소율",
         "승현", "지아", "태윤", "하린", "정우", "지민", "형준", "수빈", "민재", "예린", "규현", "은지",
         "상현", "혜원", "진우", "나윤", "성호", "지연", "찬민", "서현", "종현", "민서", "재현", "윤서"]

# 로그인 신규 계정: (email, name, dept_code, position, role_code)
LOGIN_NEW = [
    ("sales.mgr@hyunwoo.com", None, "DEPT-SALES", "MANAGER", "SALES"),
    ("sales.global@hyunwoo.com", None, "DEPT-SALESGL", "SENIOR", "SALES"),
    ("purchase@hyunwoo.com", None, "DEPT-PURCHASE", "STAFF", "PURCHASING"),
    ("purchase.mgr@hyunwoo.com", None, "DEPT-PURCHASE", "MANAGER", "PURCHASING"),
    ("finance.mgr@hyunwoo.com", None, "DEPT-FINANCE", "MANAGER", "FINANCE"),
    ("hr.mgr@hyunwoo.com", None, "DEPT-HR", "MANAGER", "HR"),
    ("prod.sw@hyunwoo.com", None, "DEPT-PROD-SW", "MANAGER", "PRODUCTION"),
    ("prod.gm@hyunwoo.com", None, "DEPT-PROD-GM", "MANAGER", "PRODUCTION"),
]
# 비로그인 사무직 추가 배치: (dept_code, count, position)  — 로그인 인원과 합쳐 사무 신규 14
OFFICE_EXTRA = [
    ("DEPT-GA", 2, "STAFF"), ("DEPT-SALES2", 2, "STAFF"), ("DEPT-LOGIS", 2, "STAFF"),
    ("DEPT-RND", 2, "SENIOR"),
]  # 2+2+2+2 = 8 비로그인 사무 + 6 로그인(팀장/담당 중 사무) ... 아래에서 합산 계산
# 생산직 배치: (dept_code, count)  — position=STAFF
PROD_ALLOC = [
    ("DEPT-PROD-SW", 27), ("DEPT-PROD-GM", 27), ("DEPT-PRODUCTION", 25), ("DEPT-QC", 10),
]

POSITION_SALARY = {"STAFF": 3000000, "SENIOR": 3600000, "MANAGER": 4800000, "DIRECTOR": 6500000}
PROD_STAFF_SALARY = 2900000  # 생산직 기본급


def rand_name():
    return random.choice(SURNAMES) + random.choice(GIVEN)


def rand_hire_date():
    y = random.random()
    if y < 0.5:
        year = 2024
    elif y < 0.8:
        year = 2025
    else:
        year = 2026
    month = random.randint(1, 6) if year == 2026 else random.randint(1, 12)
    day = random.randint(1, 28)
    return f"{year}-{month:02d}-{day:02d}", year


def build_employees():
    """직원 105명 생성 → 리스트[dict]. 결정론적."""
    emps = []          # {code,name,email,dept,hire,pos,login,role,salary}
    seq = {2024: 1, 2025: 1, 2026: 6}   # 기존 EMP-2026-0001~0005 → 2026 은 6 부터
    used_email = set()

    def make(name, dept, pos, login=False, email=None, role=None, salary=None, prod=False):
        hire, year = rand_hire_date()
        code = f"EMP-{year}-{seq[year]:04d}"; seq[year] += 1
        if email is None:
            email = code.lower() + "@hyunwoo.com"
        assert email not in used_email, f"이메일 중복 {email}"
        used_email.add(email)
        if salary is None:
            salary = PROD_STAFF_SALARY if prod else POSITION_SALARY[pos]
        emps.append(dict(code=code, name=name, email=email, dept=dept, hire=hire,
                         pos=pos, login=login, role=role, salary=salary))

    # 로그인 신규 8
    for email, _, dept, pos, role in LOGIN_NEW:
        make(rand_name(), dept, pos, login=True, email=email, role=role)
    # 비로그인 사무 추가 8
    for dept, cnt, pos in OFFICE_EXTRA:
        for _ in range(cnt):
            make(rand_name(), dept, pos)
    # 생산직 89 (로그인 팀장 2명은 위에서 이미 생성 → 아래는 순수 현장직)
    for dept, cnt in PROD_ALLOC:
        for _ in range(cnt):
            make(rand_name(), dept, "STAFF", prod=True)
    return emps, seq


def gen_employees(emps, seq):
    lines = [header("직원 마스터 대량 확장 (기존 5 + 신규 105 = 110)",
                    "사무 18(로그인 일부)·생산직 92. 입사연도 2024~2026 분산 → EMP-YYYY-NNNN.")]
    rows = []
    for e in emps:
        rows.append(
            f"SELECT {sqlstr(e['code'])}, {sqlstr(e['name'])}, {sqlstr(e['email'])}, d.id, "
            f"{sqlstr(e['hire'])}, 'ACTIVE', {AUDIT_DEL} FROM department d WHERE d.code = {sqlstr(e['dept'])}")
    # 한 방에 넣기 위해 UNION ALL — department 조인이 각 행마다 다르므로 개별 SELECT 를 UNION
    lines.append("-- 직원 삽입 (부서코드로 department 조인). 로그인 계정은 V57 에서 별도 연결.\n")
    lines.append("INSERT INTO employee (code, name, email, department_id, hire_date, status, "
                 "created_at, created_by, updated_at, updated_by, deleted_at)\n"
                 + "\nUNION ALL\n".join(rows) + ";\n\n")
    lines.append("-- 채번 시퀀스: 입사연도별 마지막+1.\n")
    lines.append("INSERT INTO code_sequence (prefix, period_key, next_number, updated_at) VALUES\n"
                 f"    ('EMP', '2024', {seq[2024]}, NOW()),\n"
                 f"    ('EMP', '2025', {seq[2025]}, NOW()),\n"
                 f"    ('EMP', '2026', {seq[2026]}, NOW())\n"
                 "ON DUPLICATE KEY UPDATE next_number = VALUES(next_number), updated_at = NOW();\n")
    return "".join(lines)


def gen_contracts(emps):
    lines = [header("급여계약 — 신규 105 + 기존 관리자(admin) = 106 (전원 발효)",
                    "position 별 기본급, 209h. 발효일=입사일. 기존 4명(V31)은 이미 계약 보유.")]
    lines.append("-- 1) 관리자(admin) 계약 — V31 에서 누락됐으므로 여기서 발효(전원 급여계약 목표).\n")
    lines.append("INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours, "
                 "effective_from, effective_to, status, created_at, created_by, updated_at, updated_by)\n"
                 "SELECT e.id, 'DIRECTOR', 6500000.00, 209, '2024-01-01', NULL, 'ACTIVE', " + AUDIT + "\n"
                 "  FROM employee e WHERE e.email = 'admin@hyunwoo.com';\n\n")
    lines.append("-- 2) 신규 직원 105명 계약 (발효일 = 입사일).\n")
    rows = []
    for e in emps:
        rows.append(
            f"SELECT e.id, {sqlstr(e['pos'])}, {e['salary']}.00, 209, {sqlstr(e['hire'])}, NULL, 'ACTIVE', "
            + AUDIT + f" FROM employee e WHERE e.email = {sqlstr(e['email'])}")
    lines.append("INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours, "
                 "effective_from, effective_to, status, created_at, created_by, updated_at, updated_by)\n"
                 + "\nUNION ALL\n".join(rows) + ";\n")
    return "".join(lines)


def gen_login(emps):
    lines = [header("로그인 계정 8 신규 + 역할 매핑 (부서별 담당/팀장)",
                    "결재라인 대비. 비밀번호 pass1234(BCrypt). username=email. user_role 명시 부여.")]
    logins = [e for e in emps if e["login"]]
    # app_user
    rows = []
    for e in logins:
        rows.append(f"SELECT emp.id, {sqlstr(e['email'])}, {sqlstr(PW_HASH)}, TRUE, FALSE, " + AUDIT
                    + f" FROM employee emp WHERE emp.email = {sqlstr(e['email'])}")
    lines.append("-- 1) 로그인 계정.\n")
    lines.append("INSERT INTO app_user (employee_id, username, password_hash, enabled, account_locked, "
                 "created_at, created_by, updated_at, updated_by)\n"
                 + "\nUNION ALL\n".join(rows) + ";\n\n")
    # user_role
    lines.append("-- 2) 역할 매핑 (계정 → 역할).\n")
    rrows = []
    for e in logins:
        rrows.append(
            f"SELECT u.id, r.id FROM app_user u JOIN role r ON r.code = {sqlstr(e['role'])} "
            f"WHERE u.username = {sqlstr(e['email'])}")
    lines.append("INSERT INTO user_role (user_id, role_id)\n" + "\nUNION ALL\n".join(rrows) + ";\n")
    return "".join(lines)


# =============================================================================
# 9. 기초재고(부품) + 개시분개 (V58)
# =============================================================================
OPENING_QTY = 500  # 신규 부품 각 창고당 수량


def gen_opening_stock():
    # 총액 = OPENING_QTY × Σ(신규부품 원가) × 3개 공장창고
    per_wh = OPENING_QTY * sum(cost for _, _, cost in PART_NEW)
    total = per_wh * len(WAREHOUSES_NEW)
    part_names = [p[1] for p in PART_NEW]
    in_list = ", ".join(sqlstr(n) for n in part_names)

    lines = [header("신규 부품 기초재고 (3개 공장창고) + 개시 분개",
                    f"신규 부품 {len(PART_NEW)}종 × {OPENING_QTY}개 × 공장창고 3곳. 차)원재료(1410)/대)이익잉여금(3100). "
                    f"총액 = {total:,}원.")]
    lines.append("-- 신규 부품만(기존 부품은 V35/V45 에서 WH-HQ 적재 완료). 각 공장창고에 동일 수량 적재.\n")
    lines.append("-- 1) stock 캐시.\n")
    for code, name, fac, addr in WAREHOUSES_NEW:
        lines.append(
            "INSERT INTO stock (item_id, warehouse_id, qty_on_hand, average_cost, version, "
            "created_at, created_by, updated_at, updated_by)\n"
            f"SELECT c.id, w.id, {OPENING_QTY}, c.standard_cost, 0, {AUDIT}\n"
            f"  FROM item c JOIN warehouse w ON w.code = {sqlstr(code)}\n"
            f" WHERE c.item_type = 'COMPONENT' AND c.name IN ({in_list});\n\n")
    lines.append("-- 2) stock_movement 원장 (+OPENING).\n")
    for code, name, fac, addr in WAREHOUSES_NEW:
        lines.append(
            "INSERT INTO stock_movement (item_id, warehouse_id, qty_delta, unit_cost, reason, ref_type, "
            "ref_id, moved_at, created_at, created_by, updated_at, updated_by)\n"
            f"SELECT c.id, w.id, {OPENING_QTY}, c.standard_cost, 'ADJUSTMENT_PLUS', 'OPENING', NULL, NOW(), {AUDIT}\n"
            f"  FROM item c JOIN warehouse w ON w.code = {sqlstr(code)}\n"
            f" WHERE c.item_type = 'COMPONENT' AND c.name IN ({in_list});\n\n")
    lines.append("-- 3) 개시 분개 — 차)원재료(1410) / 대)이익잉여금(3100). 부품 기초재고 총액.\n")
    lines.append("INSERT INTO journal_entry (number, entry_date, description, status, source_type, source_id, "
                 "posted_at, created_at, created_by, updated_at, updated_by)\n"
                 "VALUES ('JE-STEP4-OPEN-01', '2024-01-01', 'STEP4 공장창고 부품 기초재고 이월', 'POSTED', "
                 "'MANUAL', NULL, NOW(), " + AUDIT + ");\n\n")
    lines.append("INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit, "
                 "created_at, created_by, updated_at, updated_by)\n"
                 f"SELECT je.id, 1, a.id, {total}.00, 0, {AUDIT}\n"
                 "  FROM journal_entry je JOIN account a ON a.code = '1410' "
                 "WHERE je.number = 'JE-STEP4-OPEN-01';\n\n")
    lines.append("INSERT INTO journal_line (journal_entry_id, line_no, account_id, debit, credit, "
                 "created_at, created_by, updated_at, updated_by)\n"
                 f"SELECT je.id, 2, a.id, 0, {total}.00, {AUDIT}\n"
                 "  FROM journal_entry je JOIN account a ON a.code = '3100' "
                 "WHERE je.number = 'JE-STEP4-OPEN-01';\n")
    return "".join(lines), total


# =============================================================================
# 10. MES 설비/작업자 (공장별) — MES V9
# =============================================================================
MES_EQUIP = [  # (code, name, line_name, factory_code)
    ("EQ-101", "SMT 라인 A", "수원 노트북라인", "FAC-01"),
    ("EQ-102", "조립 라인 A", "수원 AIO라인", "FAC-01"),
    ("EQ-201", "패널 조립 라인", "구미 모니터라인", "FAC-02"),
    ("EQ-202", "태블릿 조립 라인", "구미 태블릿라인", "FAC-02"),
    ("EQ-301", "본체 조립 라인", "광주 데스크탑라인", "FAC-03"),
    ("EQ-302", "주변기기 라인", "광주 주변기기라인", "FAC-03"),
]
MES_OPERATOR = [  # (code, name)
    ("OP-101", "수원작업자1"), ("OP-102", "수원작업자2"),
    ("OP-201", "구미작업자1"), ("OP-202", "구미작업자2"),
    ("OP-301", "광주작업자1"), ("OP-302", "광주작업자2"),
]


def gen_mes():
    lines = ["-- STEP 4 (실무 리얼리즘 확장): MES 설비/작업자 공장별 확장.\n"
             "-- 공장마다 라인 2개 + 작업자 2명. equipment.factory_code 로 ERP 공장에 귀속.\n\n"]
    erows = []
    for code, name, line, fac in MES_EQUIP:
        erows.append(f"    ({sqlstr(code)}, {sqlstr(name)}, {sqlstr(line)}, {sqlstr(fac)}, NOW(), NOW())")
    lines.append("INSERT INTO equipment (code, name, line_name, factory_code, created_at, updated_at) VALUES\n"
                 + ",\n".join(erows) + ";\n\n")
    orows = []
    for code, name in MES_OPERATOR:
        orows.append(f"    ({sqlstr(code)}, {sqlstr(name)}, NOW(), NOW())")
    lines.append("INSERT INTO operator (code, name, created_at, updated_at) VALUES\n"
                 + ",\n".join(orows) + ";\n")
    return "".join(lines)


# =============================================================================
# 메인
# =============================================================================
def write(path, content):
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"  wrote {os.path.relpath(path, HERE)}  ({content.count(chr(10))} lines)")


def main():
    print("STEP4 SQL 생성 시작...")
    items_sql, item_next = gen_items()
    emps, emp_seq = build_employees()
    stock_sql, stock_total = gen_opening_stock()

    files = [
        ("V48__step4_categories.sql", gen_categories()),
        ("V49__step4_items.sql", items_sql),
        ("V50__step4_bom.sql", gen_bom()),
        ("V51__step4_warehouses.sql", gen_warehouses()),
        ("V52__step4_departments.sql", gen_departments()),
        ("V53__step4_customers.sql", gen_customers()),
        ("V54__step4_vendors.sql", gen_vendors()),
        ("V55__step4_employees.sql", gen_employees(emps, emp_seq)),
        ("V56__step4_contracts.sql", gen_contracts(emps)),
        ("V57__step4_accounts_login.sql", gen_login(emps)),
        ("V58__step4_opening_stock.sql", stock_sql),
    ]
    for fname, content in files:
        write(os.path.join(ERP_MIG, fname), content)
    write(os.path.join(MES_MIG, "V9__step4_equipment.sql"), gen_mes())

    # 요약
    n_login = sum(1 for e in emps if e["login"])
    print("\n=== 요약 ===")
    print(f"완제품 신규: {len(FINISHED_NEW)} / 부품 신규: {len(PART_NEW)} (ITEM next={item_next})")
    print(f"BOM 완제품: {len(BOM)}")
    print(f"고객 신규: 98 / 거래처 신규: {len(VENDORS_NEW)}")
    print(f"직원 신규: {len(emps)} (로그인 {n_login}) / EMP next 2024={emp_seq[2024]},"
          f" 2025={emp_seq[2025]}, 2026={emp_seq[2026]}")
    print(f"부서 신규: {len(DEPT_L2)+len(DEPT_L3)} / 재편: {len(DEPT_REORG)}")
    print(f"창고 신규: {len(WAREHOUSES_NEW)}")
    print(f"기초재고 개시분개 총액: {stock_total:,}원")
    print(f"MES 설비 {len(MES_EQUIP)} / 작업자 {len(MES_OPERATOR)}")
    # 정합성: 부서 배치 인원 확인
    from collections import Counter
    dc = Counter(e["dept"] for e in emps)
    print("부서별 신규 인원:", dict(dc))


if __name__ == "__main__":
    main()
