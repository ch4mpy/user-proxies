package com.c4_soft.howto.keycloak;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.c4_soft.howto.user_proxies.web.dtos.ProxyDto;
import com.c4_soft.howto.user_proxies.web.dtos.TokenResponseDto;

public class ProxiesMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
	private static final String AUTHORIZATION_SERVER_BASE_URI = "proxies-service.users-endpoint-uri";
	private static final String PROXIES_SERVICE_CLIENT_SECRET = "proxies-service.client-secret";
	private static final String PROXIES_SERVICE_CLIENT_NAME = "proxies-service.client-name";
	private static final String PROVIDER_ID = "c4-soft.com";
	private static final String PROXIES_SERVICE_BASE_URI = "proxies-service.authorization-uri";
	private static Logger logger = Logger.getLogger(ProxiesMapper.class);

	private final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	private final Map<String, WebClient> webClientByBaseUri = new HashMap<>();
	private long expiresAt = 0L;
	private Optional<TokenResponseDto> token = Optional.empty();

	public ProxiesMapper() {
		ProviderConfigProperty property;

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_BASE_URI);
		property.setLabel("Proxies service base URI");
		property.setHelpText("Base URI for REST service to fetch proxies from");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("https://bravo-ch4mp:4204/users");
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_CLIENT_NAME);
		property.setLabel("Proxies mapper client-name");
		property.setHelpText("Proxies mapper client-name");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("proxies-mapper");
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_CLIENT_SECRET);
		property.setLabel("Proxies mapper client-secret");
		property.setHelpText("Proxies mapper client-secret");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(AUTHORIZATION_SERVER_BASE_URI);
		property.setLabel("Authorization endpoint");
		property.setHelpText("Token end-point for authorizing proxies mapper");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("https://bravo-ch4mp:9443/auth/realms/master/protocol/openid-connect/token");
		configProperties.add(property);
	}

	@Override
	public IDToken transformIDToken(
			IDToken token,
			ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession,
			UserSessionModel userSession,
			ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public AccessToken transformAccessToken(
			AccessToken token,
			ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession,
			UserSessionModel userSession,
			ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public AccessToken transformUserInfoToken(
			AccessToken token,
			ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession,
			UserSessionModel userSession,
			ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getDisplayType() {
		return "User proxies mapper";
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getHelpText() {
		return "Adds a \"proxies\" private claim containing a map of authorizations the user has to act on behalf of other users (one collection of grant IDs per user subject)";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	private <T extends IDToken> T transform(
			T token,
			ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession,
			UserSessionModel userSession,
			ClientSessionContext clientSessionCtx) {
		final var proxies = getGrantsByProxiedUserSubject(mappingModel, token);
		token.getOtherClaims().put("proxies", proxies);
		setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);
		return token;

	}

	private WebClient getWebClient(String baseUri) {
		return webClientByBaseUri.computeIfAbsent(baseUri, (String k) -> WebClient.builder().baseUrl(baseUri).build());
	}

	private Map<String, List<String>> getGrantsByProxiedUserSubject(ProtocolMapperModel mappingModel, IDToken token) {
		final var baseUri = mappingModel.getConfig().get(PROXIES_SERVICE_BASE_URI);
		try {
			final Optional<ProxyDto[]> dtos =
					Optional.ofNullable(getWebClient(baseUri).get().uri("/{userSubject}/proxies/granted", token.getSubject()).headers(headers -> {
						headers.setBearerAuth(getClientAuthorizationBearer(mappingModel));
					}).retrieve().bodyToMono(ProxyDto[].class).block());

			return dtos.map(Stream::of).map(s -> s.collect(Collectors.toMap(ProxyDto::getGrantingUserSubject, ProxyDto::getGrants))).orElse(Map.of());
		} catch (final WebClientResponseException e) {
			logger.warn("Failed to fetch user proxies", e);
			return Map.of();
		}
	}

	private String getClientAuthorizationBearer(ProtocolMapperModel mappingModel) {
		final var now = new Date().getTime();
		final var baseUri = mappingModel.getConfig().get(AUTHORIZATION_SERVER_BASE_URI);
		final var username = mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_NAME);
		final var password = mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_SECRET);
		if (expiresAt < now) {
			token = Optional.ofNullable(getWebClient(baseUri).post().headers(headers -> {
				headers.setBasicAuth(username, password);
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			})
					.body(BodyInserters.fromFormData("scope", "openid").with("grant_type", "client_credentials"))
					.retrieve()
					.bodyToMono(TokenResponseDto.class)
					.block());
			expiresAt = now + 1000L * token.map(TokenResponseDto::getExpiresIn).orElse(0L);
		}
		return token.map(TokenResponseDto::getAccessToken).orElse(null);
	}
}
