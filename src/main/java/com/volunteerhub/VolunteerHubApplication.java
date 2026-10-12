package com.volunteerhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VolunteerHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(VolunteerHubApplication.class, args);
	}

}
