# contracts/ — ERP ↔ MES 인터페이스 계약

ERP(`hwlee-erp`)와 MES(`hwlee-mes`)는 **별도 시스템**이라 서로의 코드/DB를 직접 보지 못한다.
두 시스템이 어긋나지 않으려면 **주고받을 데이터의 모양을 먼저 합의**해야 한다 — 그 합의를 이 디렉토리에 둔다.

```
contracts/
├─ openapi/   ← 동기 REST API 명세 (ERP → MES 호출)
│   └─ erp-to-mes-workorder.yaml      (Phase 12)
└─ events/    ← 비동기 메시지 스키마 (MES → ERP, Kafka)
    ├─ mes-production-completed.json  (Phase 14)
    └─ mes-quality-defect.json        (Phase 15)
```

## 채우는 시점
- **Phase 12**: `openapi/erp-to-mes-workorder.yaml` — 작업지시 수신 REST 계약.
- **Phase 14**: `events/mes-production-completed.json` — 생산 실적 이벤트(Kafka) 스키마.
- **Phase 15**: `events/mes-quality-defect.json` — 불량 발생 이벤트 스키마.

## 원칙
- 계약이 곧 두 시스템의 약속이다. **계약을 먼저 바꾸고** 양쪽 구현을 맞춘다.
- 메시지에는 **멱등 키**(중복 처리 방지용 고유 식별자)와 **발생 시각**을 포함한다.
