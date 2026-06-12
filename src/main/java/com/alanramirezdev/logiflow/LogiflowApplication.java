package com.alanramirezdev.logiflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LogiflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogiflowApplication.class, args);
	}

}