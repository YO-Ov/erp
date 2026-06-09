package com.hwlee.mes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * hwlee-MES — 공장 현장 실행 시스템(현장계).
 *
 * <p>ERP(hwlee-erp, 8080)와 별도 프로세스·별도 DB(mes_db, 3308)로 운영되며,
 * ERP→MES 작업지시(REST 동기)와 MES→ERP 실적(Kafka 비동기)으로 연계된다.
 */
@SpringBootApplication
@EnableScheduling // Phase 14 — Outbox Publisher 폴링
public class MesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MesApplication.class, args);
    }
}
