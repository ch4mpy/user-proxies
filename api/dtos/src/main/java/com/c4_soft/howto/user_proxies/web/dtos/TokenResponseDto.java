package com.c4_soft.howto.user_proxies.web.dtos;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@XmlRootElement
@Data
public class TokenResponseDto implements Serializable {
	private static final long serialVersionUID = 4995510591526512450L;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private Long expiresIn;

	@JsonProperty("refresh_expires_in")
	private Long refreshExpiresIn;

	@JsonProperty("token_type")
	private String tokenType;

	@JsonProperty("id_token")
	private String idToken;

	@JsonProperty("not-before-policy")
	private Long notBeforePolicy;

	private String scope;
}
