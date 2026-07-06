-- 거래처 취급품목(구매정보레코드) — "이 거래처가 이 품목을 공급한다" + 매입단가/리드타임.
-- 입고는 이 매핑에 있는 (거래처×품목) 조합만 허용한다.

CREATE TABLE vendor_item (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    vendor_id      BIGINT       NOT NULL,
    item_id        BIGINT       NOT NULL,
    supply_price   DECIMAL(15,2) NOT NULL,
    lead_time_days INT          NOT NULL DEFAULT 0,
    status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)  NOT NULL,
    created_by     VARCHAR(64)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    updated_by     VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_item UNIQUE (vendor_id, item_id),
    CONSTRAINT fk_vendor_item_vendor FOREIGN KEY (vendor_id) REFERENCES vendor (id),
    CONSTRAINT fk_vendor_item_item   FOREIGN KEY (item_id)   REFERENCES item (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 시드: 부품(COMPONENT)만 거래처에 매핑(완제품은 생산 대상이라 매입하지 않는다).
-- 각 부품을 서로 다른 승수(1·7·11)로 최대 3개 거래처에 분산 매핑 → 부품마다 ≥1개 공급처 보장.
-- 매입단가 = 표준원가, 리드타임 = 5 + (거래처id mod 10) 일. 결정론적이라 재실행 시 동일.
INSERT INTO vendor_item (vendor_id, item_id, supply_price, lead_time_days, status,
                         created_at, created_by, updated_at, updated_by)
SELECT v.id, i.id,
       ROUND(COALESCE(i.standard_cost, 0), 2),
       5 + (v.id MOD 10),
       'ACTIVE', NOW(6), 'system', NOW(6), 'system'
FROM item i
JOIN vendor v
  ON v.id IN ((i.id MOD 18) + 1, ((i.id * 7) MOD 18) + 1, ((i.id * 11) MOD 18) + 1)
WHERE i.item_type = 'COMPONENT'
  AND i.status = 'ACTIVE'
  AND v.status = 'ACTIVE';
