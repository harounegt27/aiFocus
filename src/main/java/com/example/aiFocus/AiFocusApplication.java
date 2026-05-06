package com.example.aiFocus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiFocusApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiFocusApplication.class, args);
	}

}
