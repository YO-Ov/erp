package com.hwlee.erp.batch.job;

/**
 * 배치 잡/파라미터 이름 상수. 런처·스케줄러·잡 설정이 같은 문자열을 공유한다.
 */
public final class BatchJobNames {

    private BatchJobNames() {
    }

    /** 일일 매출 마감 잡. 파라미터: {@link #PARAM_CLOSING_DATE}. */
    public static final String DAILY_SALES_CLOSING = "dailySalesClosingJob";

    /** 월말 결산 잡(재고 평가 + 채권 노령화). 파라미터: {@link #PARAM_CLOSING_DATE}. */
    public static final String MONTH_END_CLOSING = "monthEndClosingJob";

    /** 마감 기준일(yyyy-MM-dd). 식별 파라미터. */
    public static final String PARAM_CLOSING_DATE = "closingDate";

    /** 매 실행 고유값(epoch millis). 같은 기준일 재실행을 허용하기 위한 비식별 트리거. */
    public static final String PARAM_RUN_ID = "run.id";
}
