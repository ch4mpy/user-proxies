package com.c4_soft.howto.user_proxies.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_proxies", uniqueConstraints = {
		@UniqueConstraint(name = "UniqueProxiedAndGrantedUsers", columnNames = { "granting_user_id", "granted_user_id" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proxy {
	@Id
	@GeneratedValue
	private Long id;

	@NotNull
	@ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "granting_user_id", updatable = false, nullable = false)
	private User grantingUser;

	@NotNull
	@ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "granted_user_id", updatable = false, nullable = false)
	private User grantedUser;

	@NotNull
	@ElementCollection(fetch = FetchType.EAGER)
	@Default
	private List<String> grants = new ArrayList<>();

	@NotNull
	@Column(name = "start_date", nullable = false, updatable = true)
	private Date start;

	@Column(name = "end_date", nullable = true, updatable = true)
	private Date end;
}
