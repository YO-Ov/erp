# 로컬 LLM 기반 ERP 업무 자동화(Agent) — 설계 문서

> 로컬 LLM(Ollama)을 "두뇌"로 쓰되, **판단은 파이썬 코드가 하고 AI는 자연어→구조화 JSON 추출만** 하는 ERP 업무 자동화 에이전트 설계.
> hwlee님 방향: "AI에게 복잡한 다단계 비즈니스 판단을 통째로 맡기지 않는다. 역할을 쪼갠다."
> ✅ **1단계 프로토타입 구현·검증 완료 (2026-07-07).** 위치=저장소와 별도 폴더 `erp-agent/`(자바 ERP 저장소 밖, 이 맥미니 전용). Ollama `qwen2.5:7b`로 수주→재고→생산→발주 전체 분기 정상. 실행: `cd erp-agent && .venv/bin/python main.py`.
> ✅ **조회형(읽기) 액션 3종 추가 (같은 날):** `CHECK_APPROVAL`(내 결재 상태) · `CHECK_ORDER`(수주 조회, 고객필터) · `CHECK_STOCK`. "조회페이지 대신 채팅으로 업무"의 첫 실현.
> ✅ **2단계 휴먼 인 더 루프 추가 (같은 날):** 쓰기 액션(ORDER)은 **계획 미리보기(읽기로 재고→생산→발주 분기 계산) → y/n → 승인 시에만 실행**. `Plan(summary, apply)` 도입, `_order`→`_plan_order`(읽기 계획/쓰기 실행 분리), `main.py` 확인게이트. 읽기 액션은 확인 없이 즉시. **잘못 알아들어도 n으로 차단 = 쓰기 안전장치** (plan-then-apply 패턴).
> ✅ **결재 상신(SUBMIT_APPROVAL) 액션 추가 (같은 날):** "PAY-2026-0015 상신해줘"→ 쓰기라 확인게이트 자동. 계획 미리보기에 **실 ERP 전결 규정 재현**(금액별 결재선: <1천만 팀장 / 1천만~5천만 팀장→본부장 / ≥5천만 팀장→본부장→대표→재무합의). "채팅으로 결재 올리기" 실현. 스키마에 `ref_no` 필드 추가.
> ✅ **환경별 ERP 연결 계층 추가 (같은 날):** "코드는 하나, 대상은 환경변수로". `config.py`(`ERP_MODE`=mock|http, `ERP_BASE_URL`, 로그인) + `.env.example` + `HttpERPClient`(실 ERP HTTP 골격, requests 세션 폼로그인) + `make_erp_client()` 팩토리. `ERP_MODE=mock` 기본이라 무회귀. **추천: Oracle Cloud 공개 URL 하나로 통일** → 로컬/외부/에이전트 전부 `.env` 한 줄만 다름. 실 엔드포인트 매핑은 `hwlee-erp` 컨트롤러 확정 후(§7·§9 참조).
> ⚠️ 아래 코드블록은 초기(4액션·확인게이트 이전) 기준 참고용 — **실제 동작 코드는 `erp-agent/` 파일이 최신**.

---

## 0. 이 문서를 만든 배경 (대화 경위)

- 원래 계획: **Phase 17 자연어 데이터 검색(Text-to-SQL)** = 로컬 Ollama로 "미수금 조회" 같은 **읽기** 질의. (STUDY-PLAN Part 3, 미착수)
- hwlee님이 확장 아이디어 제시: "노트북 100대 견적 뽑아줘 → 결재 상신 → 수주 → 재고 없으면 생산 → 부품 없으면 발주" 까지 **AI가 처리**. 사실상 담당자를 대체하는 **행동형 에이전트**.
- 정리된 결론:
  - 이건 "검색(읽기)"이 아니라 **행동(쓰기)** = **Agentic AI**(도구 호출 + 자율 실행 루프). Text-to-SQL과 별개 기술.
  - 로컬 7B 모델은 **다단계 조건 분기 오케스트레이션이 불안정**(재고 있으면 A, 없으면 B→C 판단을 놓침). 이건 Claude/GPT급이라야 안정적.
  - 하지만 **비용 없이** 하려면 → **역할을 쪼갠다**: AI는 NLP(자연어 이해)만, **비즈니스 흐름 판단은 파이썬 코드가** 결정론적으로.
- **채택 방식** = 아래 "방식 2".

### 세 가지 접근 비교

| 방식 | 비용 | 자율성 | 학습가치 | 채택 |
|---|---|---|---|---|
| 1. 모델 등급 쪼개기 (Ollama 조각 + 유료 오케스트레이터) | 저렴하나 유료 필요 | 높음 | 높음 | 후순위 |
| **2. 파이썬이 판단 + Ollama는 NLP 추출만** | **완전 무료** | 중간(흐름 고정) | **매우 높음** | **★ 채택** |
| 3. 캐스케이드 (Ollama 먼저, 어려우면 유료 승격) | 최소 유료 | 높음 | 높음(복잡) | 나중에 |

> 방식 2는 실무의 "안전한 AI 기능"이 대부분 취하는 형태다. "AI가 전부 판단"은 오작동·책임 문제로 프로덕션에서 잘 안 쓴다. 흐름을 파이썬으로 짜는 것 = ERP 실무 로직을 직접 설계하는 것 = 이 학습 프로젝트의 목적과 정확히 일치.

---

## 1. 역할 경계 원칙 (이 시스템의 헌법)

- **LLM은 절대 실행·판단하지 않는다.** 오직 `자연어 → 검증된 JSON` 변환만. 확신 없으면 `UNKNOWN`.
- **모든 비즈니스 판단은 파이썬 코드가 한다.** 재고 부족→생산→자재 부족→발주 분기는 전부 `orchestrator.py`의 if/else.
- **필수 인자 검증도 코드가 한다.** LLM이 뽑은 값(수량 누락 등)을 믿지 않고 재검증.
- **ERP 접근은 한 파일(`erp_client.py`)에만 격리** → 나중에 Mock을 실제 HTTP로 바꾸면 끝.

---

## 2. 시스템 아키텍처

```
┌──────────────────────────────────────────────────────┐
│  이 맥미니 (M2) — "AI 계층"만 설치                       │
│                                                        │
│   👤 "노트북 100대 수주해줘"                             │
│         │                                              │
│   ┌─────▼──────────────────┐                           │
│   │ ① 의도 추출기 (LLM)      │  Ollama + Qwen2.5-7B     │
│   │   자연어 → 검증된 JSON    │  Pydantic 스키마 강제     │
│   │   판단 X, 추출만 O        │  실패 시 UNKNOWN 안전복귀 │
│   └─────┬──────────────────┘                           │
│         │  IntentPayload(action=ORDER, item=…, qty=100) │
│   ┌─────▼──────────────────┐                           │
│   │ ② 오케스트레이터         │  ★ 순수 파이썬 = "두뇌"    │
│   │   워크플로우 제어         │  if/else · 상태 분기      │
│   │   재고→생산→MRP 분기      │  전부 여기서 결정          │
│   └─────┬──────────────────┘                           │
│         │  HTTP 호출                                    │
│   ┌─────▼──────────────────┐                           │
│   │ ③ ERP 클라이언트         │  지금 = Mock 함수         │
│   │                        │  나중 = requests          │
│   └─────────┼──────────────┘                           │
└─────────────┼──────────────────────────────────────────┘
              │  HTTP (나중에 연결)
       ┌──────▼───────┐
       │ Oracle Cloud  │  ERP/MES (Spring, 실제 API)
       └──────────────┘
```

**물리적 배치**: AI 계층은 **이 맥미니에만** 설치(Ollama + 파이썬). ERP/MES 웹서비스는 **Oracle Cloud**에 배포. 둘은 HTTP로 통신.

---

## 3. 예시 시나리오 (전체 흐름)

1. 사용자: `"신원전자에 노트북 100대 수주해줘"`
2. **로컬 LLM** → `{"action":"ORDER","item":"노트북","quantity":100,"customer":"신원전자"}` (추출만)
3. **파이썬 오케스트레이터**:
   - 수주 생성 → `check_inventory()` 호출
   - (코드 판단) 재고 부족 → `create_production_order()` 로 생산 요청 분기
   - 자재 확인 → 부족 시 `create_purchase_order()` 로 MRP(자재 구매) 분기
4. 결과를 사람이 읽을 로그로 반환

**분기 로직은 전부 파이썬**이 결정. LLM은 2번 한 줄만 담당.

---

## 4. 컴포넌트 설계

프로젝트 구조(예정 위치 = 저장소와 **별도 폴더** `erp-agent/`):

```
erp-agent/
├── requirements.txt
├── schemas.py        # ① LLM이 채울 JSON 스키마 (Pydantic)
├── extractor.py      # ① 의도 추출기 (Ollama + LangChain)
├── erp_client.py     # ③ ERP API (지금은 Mock)
├── orchestrator.py   # ② 워크플로우 제어 (두뇌)
└── main.py           # REPL 진입점
```

> **의도적으로 LangChain의 Agent(자율 실행 루프)는 쓰지 않는다.** 그건 AI에게 판단을 다시 넘기는 것이라 목적에 반함. LangChain은 오직 **구조화 추출(`with_structured_output`)** 에만 사용.

### ① `schemas.py` — LLM의 유일한 산출물

```python
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class ActionType(str, Enum):
    ORDER = "ORDER"              # 수주
    QUOTE = "QUOTE"             # 견적
    CHECK_STOCK = "CHECK_STOCK"  # 재고조회
    UNKNOWN = "UNKNOWN"          # ★ 로컬 모델 환각 방어용 탈출구


class IntentPayload(BaseModel):
    """LLM이 자연어에서 추출해야 하는 전부. 필드를 '작게' 유지하는 게 7B 안정성의 핵심."""
    action: ActionType = Field(description="사용자가 원하는 업무 동작")
    item: Optional[str] = Field(default=None, description="품목명 (예: 노트북)")
    quantity: Optional[int] = Field(default=None, description="수량 (숫자만)")
    customer: Optional[str] = Field(default=None, description="고객사명")
```

### ② `extractor.py` — 자연어 → JSON (LLM은 여기까지만)

```python
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from schemas import ActionType, IntentPayload

_SYSTEM = """너는 ERP 명령어 파서다. 사용자의 한국어 문장에서 '업무 의도(action)'와 '인자'만 뽑아 JSON으로 반환한다.

엄격한 규칙:
- 절대 판단하거나 실행하지 마라. 오직 추출만 한다.
- 확실하지 않으면 action을 반드시 UNKNOWN으로 둔다. 값을 지어내지 마라.
- quantity는 문장에 있는 숫자만. 없으면 null.
- item/customer는 문장에 나온 '한국어 표현 그대로' 유지한다. 절대 번역하거나 다른 언어로 바꾸지 마라. 없으면 null."""

# ⚠️ few-shot의 JSON 중괄호가 프롬프트 템플릿 변수로 오인되지 않도록 '메시지 객체'로 넣는다.
#    튜플 ("ai", "{...}") 로 넣으면 { } 를 변수로 해석해 KeyError 발생(초기 구현 시 실제로 겪음).
_FEWSHOT = [
    HumanMessage("신원전자에 노트북 100대 수주해줘"),
    AIMessage('{"action":"ORDER","item":"노트북","quantity":100,"customer":"신원전자"}'),
    HumanMessage("모니터 재고 얼마나 남았어?"),
    AIMessage('{"action":"CHECK_STOCK","item":"모니터","quantity":null,"customer":null}'),
    HumanMessage("점심 뭐 먹지?"),
    AIMessage('{"action":"UNKNOWN","item":null,"quantity":null,"customer":null}'),
]


class IntentExtractor:
    def __init__(self, model: str = "qwen2.5:7b", base_url: str = "http://localhost:11434"):
        llm = ChatOllama(model=model, temperature=0, base_url=base_url, num_ctx=4096)
        # 마지막 human 턴만 템플릿({q}), 나머지는 리터럴 메시지 → 중괄호 충돌 없음
        prompt = ChatPromptTemplate.from_messages(
            [SystemMessage(_SYSTEM), *_FEWSHOT, ("human", "{q}")]
        )
        # with_structured_output → Ollama의 JSON schema 모드로 스키마 강제 + Pydantic 검증
        self.chain = prompt | llm.with_structured_output(IntentPayload)

    def extract(self, user_input: str) -> IntentPayload:
        try:
            return self.chain.invoke({"q": user_input})
        except Exception:
            # 스키마 위반·파싱 실패 → 조용히 UNKNOWN 으로 안전 실패 (앱을 죽이지 않는다)
            return IntentPayload(action=ActionType.UNKNOWN)
```

> **구현 시 겪은 2가지 함정 (해결됨)**:
> 1. **few-shot JSON 중괄호 → 템플릿 변수 오인**: `("ai", "{...}")` 튜플로 넣으면 LangChain이 `{"action"}`을 변수로 해석해 `KeyError`. 전부 예외→UNKNOWN으로 빠짐. → **메시지 객체(`AIMessage`)로 해결.**
> 2. **Qwen2.5의 중국어 번역**: `노트북→笔记本电脑`로 번역해 Mock 재고(`"노트북"` 키)와 불일치. → **시스템 프롬프트에 "번역 금지" 규칙 + few-shot 한국어 앵커로 해결.**

### ③ `erp_client.py` — ERP API (지금은 Mock, 나중에 requests로 교체)

```python
class MockERPClient:
    """실제 Oracle Cloud ERP 대신 인메모리로 흉내낸다.
    실전 전환 시: 각 메서드 본문만 requests.post(f"{BASE}/api/...") 로 바꾸면 끝."""

    def __init__(self):
        self._stock = {"노트북": 30, "모니터": 500}            # 노트북=재고부족 유도
        self._bom = {"노트북": {"CPU": 20, "RAM": 999, "SSD": 999}}  # CPU=자재부족 유도
        self._seq = 0

    def _no(self, prefix: str) -> str:
        self._seq += 1
        return f"{prefix}-2026-{self._seq:04d}"

    def create_sales_order(self, customer, item, qty):
        return {"order_no": self._no("SO"), "customer": customer, "item": item, "qty": qty}

    def check_inventory(self, item, qty):
        avail = self._stock.get(item, 0)
        return {"item": item, "required": qty, "available": avail, "enough": avail >= qty}

    def issue_goods(self, order_no, item, qty):
        self._stock[item] = self._stock.get(item, 0) - qty
        return {"order_no": order_no, "shipped": qty, "remaining": self._stock[item]}

    def create_production_order(self, item, qty):
        return {"prod_no": self._no("PROD"), "item": item, "qty": qty}

    def check_materials(self, item, qty):
        bom = self._bom.get(item, {})
        shortages = [
            {"component": c, "required": qty, "available": have}
            for c, have in bom.items() if have < qty
        ]
        return {"item": item, "shortages": shortages, "ok": not shortages}

    def create_purchase_order(self, component, qty):
        return {"po_no": self._no("PO"), "component": component, "qty": qty}
```

### ② `orchestrator.py` — 워크플로우 제어 (★ 진짜 두뇌)

```python
from schemas import IntentPayload, ActionType
from erp_client import MockERPClient


class Orchestrator:
    def __init__(self, erp: MockERPClient):
        self.erp = erp

    def handle(self, intent: IntentPayload) -> list[str]:
        # LLM 결과를 '믿지 않고' 코드가 라우팅한다
        if intent.action == ActionType.UNKNOWN:
            return ["❓ 이해하지 못했습니다. 예) '신원전자 노트북 100대 수주해줘'"]
        if intent.action == ActionType.CHECK_STOCK:
            return self._check_stock(intent)
        if intent.action == ActionType.ORDER:
            return self._order(intent)
        return [f"🚧 '{intent.action.value}'는 아직 구현되지 않았습니다."]

    def _check_stock(self, intent) -> list[str]:
        if not intent.item:
            return ["⚠️ 품목이 필요합니다."]
        inv = self.erp.check_inventory(intent.item, 0)
        return [f"📦 {intent.item} 재고: {inv['available']}개"]

    def _order(self, intent) -> list[str]:
        # ── 필수 인자 검증도 코드가 (LLM 신뢰 X) ──
        if not intent.item or not intent.quantity:
            return ["⚠️ 수주에는 품목과 수량이 모두 필요합니다."]
        item, qty = intent.item, intent.quantity
        customer = intent.customer or "미지정고객"
        log: list[str] = []

        # 1) 수주 생성
        so = self.erp.create_sales_order(customer, item, qty)
        log.append(f"📝 수주 생성: {so['order_no']} ({customer} / {item} {qty}대)")

        # 2) 재고 확인 → 3) 분기
        inv = self.erp.check_inventory(item, qty)
        log.append(f"📦 재고 확인: 필요 {qty} / 보유 {inv['available']}")
        if inv["enough"]:
            gi = self.erp.issue_goods(so["order_no"], item, qty)
            log.append(f"🚚 즉시 출고 완료 (잔여 {gi['remaining']}대)")
            return log

        # ── 재고 부족 → 생산 파이프라인 ──
        shortfall = qty - inv["available"]
        log.append(f"⚠️ 재고 부족 {shortfall}대 → 생산 진입")
        prod = self.erp.create_production_order(item, shortfall)
        log.append(f"🏭 생산지시: {prod['prod_no']} ({item} {shortfall}대)")

        # ── 자재 확인 → 부족 시 발주 (MRP) ──
        mat = self.erp.check_materials(item, shortfall)
        if mat["ok"]:
            log.append("✅ 자재 충분 — 생산 진행 가능")
        else:
            for s in mat["shortages"]:
                need = s["required"] - s["available"]
                po = self.erp.create_purchase_order(s["component"], need)
                log.append(f"🛒 자재부족 {s['component']} → 구매발주 {po['po_no']} ({need}개)")
        return log
```

### `main.py` — REPL

```python
from extractor import IntentExtractor
from erp_client import MockERPClient
from orchestrator import Orchestrator


def main():
    extractor = IntentExtractor()          # ① LLM
    orch = Orchestrator(MockERPClient())   # ② + ③
    print("ERP 에이전트 프로토타입 (종료: quit)\n")
    while True:
        try:
            user = input("👤 > ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if user.lower() in {"quit", "exit", "q"}:
            break
        if not user:
            continue
        intent = extractor.extract(user)                     # 자연어 → JSON
        print(f"🤖 추출: {intent.model_dump_json(exclude_none=True)}")
        for line in orch.handle(intent):                     # 파이썬이 흐름 제어
            print("   " + line)
        print()


if __name__ == "__main__":
    main()
```

### `requirements.txt`

```text
langchain-ollama>=0.2.0
langchain-core>=0.3.0
pydantic>=2.0
```

---

## 5. 예상 실행 결과

```
👤 > 신원전자에 노트북 100대 수주해줘
🤖 추출: {"action":"ORDER","item":"노트북","quantity":100,"customer":"신원전자"}
   📝 수주 생성: SO-2026-0001 (신원전자 / 노트북 100대)
   📦 재고 확인: 필요 100 / 보유 30
   ⚠️ 재고 부족 70대 → 생산 진입
   🏭 생산지시: PROD-2026-0002 (노트북 70대)
   🛒 자재부족 CPU → 구매발주 PO-2026-0003 (50개)

👤 > 모니터 재고 얼마나 남았어?
🤖 추출: {"action":"CHECK_STOCK","item":"모니터"}
   📦 모니터 재고: 500개

👤 > 오늘 기분 어때?
🤖 추출: {"action":"UNKNOWN"}
   ❓ 이해하지 못했습니다. 예) '신원전자 노트북 100대 수주해줘'
```

LLM은 첫 줄(JSON)만 만들고, 재고→생산→발주 분기는 전부 파이썬이 결정.

---

## 6. 로컬 7B 안정화 기법 (코드에 반영됨)

| 기법 | 이유 |
|---|---|
| **스키마를 작게** (4필드) | 7B는 필드 15개짜리 JSON에서 자주 틀림. 작을수록 정확 |
| `temperature=0` | 추출 작업은 창의성 불필요 → 결정론적으로 |
| **UNKNOWN 탈출구** | 모르면 지어내지 말고 UNKNOWN. 환각 방어 핵심 |
| **few-shot 2~3개** | 예시가 정확도를 크게 올림 |
| **Pydantic 검증 + 안전 실패** | 스키마 위반 시 앱이 죽지 않고 UNKNOWN 처리 |
| **필수 인자 재검증** | LLM이 quantity를 빠뜨려도 코드가 잡음 |
| Qwen2.5-7B > Llama3-8B | 한국어·JSON 준수도가 더 안정적(권장) |

**모델**: `qwen2.5:7b`(≈5GB, M2 16GB 무난). 비교 후보 `llama3:8b`. Ollama ≥ 0.5 필요(JSON schema 모드).

---

## 7. Oracle Cloud 이관 시 바뀌는 것 (경계가 깔끔)

**AI 계층(맥미니)은 그대로.** `erp_client.py` 한 파일만 교체:

```python
import requests
BASE = "http://<oracle-cloud-ip>:8080"

class ERPClient:
    def check_inventory(self, item, qty):
        r = requests.get(f"{BASE}/api/inventory", params={"item": item}, timeout=5)
        avail = r.json()["available"]
        return {"item": item, "required": qty, "available": avail, "enough": avail >= qty}
    # create_sales_order → requests.post(f"{BASE}/api/sales-orders", json=...) ...
```

`main.py`의 `MockERPClient()` → `ERPClient()` 로 교체하면 실제 ERP 연결. **AI는 맥미니, ERP는 Oracle Cloud** 분리 유지.

> ⚠️ 실제 ERP API의 실 엔드포인트/요청 스키마는 `hwlee-erp` 컨트롤러 확인 후 매핑 필요(예: `/api/sales-orders`, ATP, `/api/production-orders`, 입고/발주). 인증(로그인 세션/토큰)도 이 계층에서 처리.

---

## 8. 단계 계획 (권장 순서)

1. ✅ **베이스라인 구현·실행 (2026-07-07 완료)** — `erp-agent/` 6파일 + `ollama pull qwen2.5:7b` + venv(Python 3.14). 수주→재고부족→생산→자재부족→발주 전체 분기, 재고조회, UNKNOWN 안전처리 전부 검증. 설치된 버전: `langchain-ollama 1.1.0`, `langchain-core 1.4.8`, `pydantic 2.13.4`.
2. ✅ **휴먼 인 더 루프 게이트 (2026-07-07 완료)** — `Plan(summary, apply)` 패턴. 쓰기 액션은 계획 미리보기 → y/n → 승인 시에만 실행. 승인(y)/취소(n)/읽기(즉시) 3케이스 검증.
3. **쓰기 액션 확장** — QUOTE(견적), 결재 상신, 다품목 수주, 취소 등. ← **다음** (확인게이트 있어 안전)
4. **실 ERP 연결** — `erp_client.py`를 Oracle Cloud 실제 API로 교체 + 인증.
5. **정확도 보강** — Qwen2.5 vs Llama3-8B 비교, 품목명→품목코드 매핑, 예외 케이스 few-shot 추가.
6. (선택) **Text-to-SQL 읽기 계층(구 Phase 17)** 을 CHECK/조회 액션에 접목.

---

## 9. 미결정 / 열린 질문

- 실제 ERP API 매핑표(엔드포인트·요청/응답 스키마) — 구현 착수 시 `hwlee-erp` 컨트롤러에서 확정.
- 인증 방식(세션 쿠키 vs 토큰) — AI 계층에서 로그인 처리 방법.
- 품목명 매핑 — 사용자가 "노트북"이라 해도 실제 품목코드(ITEM-xxx)로 변환 필요. 초기엔 이름 매칭, 후에 마스터 조회.
- UI — 지금은 CLI(REPL). 후에 웹/챗 인터페이스 여부.
