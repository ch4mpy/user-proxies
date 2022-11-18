package gatewaycom.c4_soft.user_proxies.api;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	/**
	 * With following conf, users-API, greet-API and keycloak are all accessible
	 * through gateway (https://localhost:8080) as well as directly (respectively
	 * https://localhost:9445, https://localhost:9443 and https://localhost:8443)
	 * 
	 * @param builder
	 * @return
	 */
	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder, Function<GatewayFilterSpec, UriSpec> corsFilters) {
		return builder.routes().route(p -> p.path("/users/**").filters(corsFilters).uri("https://localhost:9443"))
				.route(p -> p.path("/greet/**").filters(corsFilters).uri("https://localhost:9445"))
				.route(p -> p.path("/realms/**").filters(corsFilters).uri("https://localhost:8443"))
				.build();
	}

	@Bean
	Function<GatewayFilterSpec, UriSpec> corsFilters() {
		// use something more restrictive in production
		return f -> f
				.setResponseHeader("Access-Control-Allow-Origin", "*")
				.setResponseHeader("Access-Control-Allow-Methods", "*")
				.setResponseHeader("Access-Control-Allow-headers", "*")
				.setResponseHeader("Access-Control-Expose-Headers", "*");
	}
}
