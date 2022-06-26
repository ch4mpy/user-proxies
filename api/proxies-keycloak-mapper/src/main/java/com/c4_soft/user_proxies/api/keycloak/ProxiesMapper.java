package com.c4_soft.user_proxies.api.keycloak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

public class ProxiesMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
	private static final String AUTHORIZATION_URI = "proxies-service.authorization-uri";
	private static final String PROXIES_SERVICE_CLIENT_SECRET = "proxies-service.client-secret";
	private static final String PROXIES_SERVICE_CLIENT_NAME = "proxies-service.client-name";
	private static final String PROVIDER_ID = "c4-soft.com";
	private static final String PROXIES_SERVICE_BASE_URI = "proxies-service.users-endpoint-uri";

	private final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	public ProxiesMapper() {
		ProviderConfigProperty property;

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_BASE_URI);
		property.setLabel("Proxies service base URI");
		property.setHelpText("Base URI for REST service to fetch proxies from");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("https://localhost:8443/users");
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_CLIENT_NAME);
		property.setLabel("Proxies mapper client-name");
		property.setHelpText("Proxies mapper client-name");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("user-proxies-mapper");
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_CLIENT_SECRET);
		property.setLabel("Proxies mapper client-secret");
		property.setHelpText("Proxies mapper client-secret");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(AUTHORIZATION_URI);
		property.setLabel("Authorization endpoint");
		property.setHelpText("Token end-point for authorizing proxies mapper");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("https://localhost:9443/auth/realms/master/protocol/openid-connect/token");
		configProperties.add(property);
	}

	@Override
	public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession keycloakSession,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
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
		return "Adds a \"proxies\" private claim containing a map of permissions the \"tenant\" (current user) has to act on behalf of \"proxied\" users (one collection of permissions per proxied user preferred_username)";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	private <T extends IDToken> T transform(T token, ProtocolMapperModel mappingModel, KeycloakSession keycloakSession,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		final var clientConfig = UserProxiesClientConfig.builder()
				.authorizationUri(mappingModel.getConfig().get(AUTHORIZATION_URI))
				.username(mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_NAME))
				.password(mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_SECRET))
				.userProxiesBaseUri(mappingModel.getConfig().get(PROXIES_SERVICE_BASE_URI)).build();
		final var who = Optional.ofNullable(userSession.getUser().getUsername()).orElse("");
		if (who == null || who.length() == 0) {
		} else {
			final var proxies = UserProxiesClient.getInstance(clientConfig).getPermissionsByProxiedUsernameFor(who);
			token.getOtherClaims().put("proxies", proxies);
			setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);
		}
		return token;

	}
}