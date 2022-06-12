package com.c4_soft.user_proxies.api.keycloak;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.c4_soft.user_proxies.api.web.dto.ProxyDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProxiesClient {

	private static final Map<UserProxiesClientConfig, UserProxiesClient> instances = new HashMap<>();

	private final UserProxiesClientConfig config;
	private final WebClient tokenWebClient;
	private final WebClient userProxiesWebClient;
	private long expiresAt = 0L;
	private Optional<TokenResponseDto> token = Optional.empty();

	private UserProxiesClient(UserProxiesClientConfig config) {
		this.config = config;
		this.tokenWebClient = WebClient.builder().baseUrl(config.getAuthorizationUri()).build();
		this.userProxiesWebClient = WebClient.builder().baseUrl(config.getUserProxiesBaseUri()).build();
	}

	public Map<String, List<String>> getPermissionsByProxiedUsernameFor(String tenantPreferredUsername) {
		try {
			final Optional<ProxyDto[]> dtos = Optional
					.ofNullable(userProxiesWebClient.get().uri("/{username}/proxies/granted", tenantPreferredUsername)
							.headers(this::setBearer).retrieve().bodyToMono(ProxyDto[].class).block());
			dtos.ifPresent(d -> log.debug("Got proxies {}", Stream.of(d).toList()));
			
			return dtos.map(Stream::of)
					.map(s -> s.collect(Collectors.toMap(ProxyDto::getGrantingUsername, ProxyDto::getGrants)))
					.orElse(Map.of());
		} catch (final Exception e) {
			log.error("Failed to fetch user proxies: {}", e);
			return Map.of();
		}
	}

	private HttpHeaders setBearer(HttpHeaders headers) {
		getClientAccessToken().ifPresent(str -> {
			headers.setBearerAuth(str);
		});
		return headers;
	}

	private Optional<String> getClientAccessToken() {
		final var now = new Date().getTime();
		if (expiresAt < now) {
			try {
				log.debug("Get client access token with {}", config.getUsername());
				token = Optional.ofNullable(tokenWebClient.post().headers(headers -> {
					headers.setBasicAuth(config.getUsername(), config.getPassword());
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}).body(BodyInserters.fromFormData("scope", "openid profile").with("grant_type", "client_credentials"))
						.retrieve().bodyToMono(TokenResponseDto.class).block());
				expiresAt = now + 1000L * token.map(TokenResponseDto::getExpiresIn).orElse(0L);
			} catch (final Exception e) {
				log.error("Failed to get client authorization-token: {}", e);
				return Optional.empty();
			}
		}
		return token.map(TokenResponseDto::getAccessToken);
	}

	public static UserProxiesClient getInstance(UserProxiesClientConfig config) {
		return instances.computeIfAbsent(config, c -> {
			log.info("Building UserProxiesClient with {}", c);
			return new UserProxiesClient(c);
		});
	}
}
