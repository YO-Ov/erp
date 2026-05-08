package com.hwlee.erp;

import org.springframework.boot.SpringApplication;

public class TestHwleeErpApplication {

	public static void main(String[] args) {
		SpringApplication.from(HwleeErpApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
