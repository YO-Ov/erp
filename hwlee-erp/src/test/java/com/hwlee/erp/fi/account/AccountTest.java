package com.hwlee.erp.fi.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 계정과목 도메인 — 트리 구조와 유형-방향 파생 규칙 검증.
 */
class AccountTest {

    @Test
    @DisplayName("AccountType 은 NormalSide 를 그대로 가진다 — 자산/비용=차변, 부채/자본/수익=대변")
    void 유형이_정상_잔액_방향을_파생한다() {
        assertThat(AccountType.ASSET.getNormalSide()).isEqualTo(NormalSide.DEBIT);
        assertThat(AccountType.EXPENSE.getNormalSide()).isEqualTo(NormalSide.DEBIT);
        assertThat(AccountType.LIABILITY.getNormalSide()).isEqualTo(NormalSide.CREDIT);
        assertThat(AccountType.EQUITY.getNormalSide()).isEqualTo(NormalSide.CREDIT);
        assertThat(AccountType.REVENUE.getNormalSide()).isEqualTo(NormalSide.CREDIT);
    }

    @Test
    @DisplayName("Account.create — 정상 입력으로 계정 생성")
    void 정상_입력으로_계정_생성() {
        Account ar = Account.create("1200", "매출채권", AccountType.ASSET, null, true);

        assertThat(ar.getCode()).isEqualTo("1200");
        assertThat(ar.getName()).isEqualTo("매출채권");
        assertThat(ar.getType()).isEqualTo(AccountType.ASSET);
        assertThat(ar.normalSide()).isEqualTo(NormalSide.DEBIT);
        assertThat(ar.isPostable()).isTrue();
        assertThat(ar.getParent()).isNull();
    }

    @Test
    @DisplayName("자기참조 트리 — 자식 계정이 부모를 참조")
    void 자식_계정이_부모를_참조한다() {
        Account asset = Account.create("1000", "자산", AccountType.ASSET, null, false);
        Account cash = Account.create("1100", "현금", AccountType.ASSET, asset, true);

        assertThat(cash.getParent()).isSameAs(asset);
        assertThat(asset.isPostable()).as("헤더 계정은 postable=false").isFalse();
        assertThat(cash.isPostable()).as("말단 계정은 postable=true").isTrue();
    }

    @Test
    @DisplayName("code 또는 name 이 비면 생성 실패")
    void 빈_코드나_이름은_거부된다() {
        assertThatThrownBy(() -> Account.create("", "현금", AccountType.ASSET, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
        assertThatThrownBy(() -> Account.create("1100", " ", AccountType.ASSET, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Account.create("1100", "현금", null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }
}
