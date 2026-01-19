package com.company.ops_hub_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpsHubApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpsHubApiApplication.class, args);
	}

}
