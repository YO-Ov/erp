package com.hwlee.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Phase 9 — 야간 마감 배치 스케줄러
public class HwleeErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(HwleeErpApplication.class, args);
	}

}
