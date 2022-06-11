package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyDto implements Serializable {
	private static final long serialVersionUID = 7382907575214377114L;

	@NotNull
	private Long id;

	@NotEmpty
	@NotNull
	private String grantingUsername;

	@NotEmpty
	@NotNull
	private String grantedUsername;

	@NotNull
	private List<String> grants;

	@NotNull
	private Long start;

	private Long end;
}