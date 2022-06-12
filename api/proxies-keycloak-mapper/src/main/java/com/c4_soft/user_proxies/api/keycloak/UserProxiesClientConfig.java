package com.c4_soft.user_proxies.api.keycloak;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserProxiesClientConfig implements Serializable {
	private static final long serialVersionUID = 5417717966238971990L;

	private final String authorizationUri;
	private final String username;
	private final String password;
	private final String userProxiesBaseUri;
}