package com.example.icsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IcsmApplication {

	public static void main(String[] args) {
		SpringApplication.run(IcsmApplication.class, args);
	}

}
