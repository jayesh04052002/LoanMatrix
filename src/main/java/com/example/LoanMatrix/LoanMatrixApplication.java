package com.example.LoanMatrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@SpringBootApplication
@EnableAsync

public class LoanMatrixApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoanMatrixApplication.class, args);
	}
}
