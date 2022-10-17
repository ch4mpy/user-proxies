package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateDto implements Serializable {
	private static final long serialVersionUID = 1963318450007215498L;

	@NotNull
	private String subject;

	@NotNull
	@Email
	private String email;

	@NotNull
	private String preferredUsername;

}