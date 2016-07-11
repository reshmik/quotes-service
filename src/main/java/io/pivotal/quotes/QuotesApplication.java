package io.pivotal.quotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Microservice to fetch current quotes.
 * 
 * Spring Boot application to provide a service to retrieve current Quote information. 
 * The application registers with a registry service - Eureka.
 * 
 * @author Reshmi Krishna
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
public class QuotesApplication {

@Bean
@LoadBalanced
RestTemplate restTemplate() {
	return new RestTemplate();
}

	@Bean
	public AlwaysSampler defaultSampler() {
		return new AlwaysSampler();
	}


	public static void main(String[] args) {
		SpringApplication.run(QuotesApplication.class, args);
	}
}

