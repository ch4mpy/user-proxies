package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
	private static final long serialVersionUID = -3684504868042067509L;

	@NotNull
	private Long id;

	@NotNull
	private String subject;

	@NotNull
	@Email
	private String email;

	@NotNull
	private String preferredUsername;

}