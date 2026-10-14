package com.Job_Queue.JQ_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JqServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JqServiceApplication.class, args);
	}

}
