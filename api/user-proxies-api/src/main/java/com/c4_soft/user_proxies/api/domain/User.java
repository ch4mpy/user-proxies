package com.c4_soft.user_proxies.api.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "UK_USER_SUBJECT", columnNames = "subject"),
		@UniqueConstraint(name = "UK_USER_EMAIL", columnNames = "email"),
		@UniqueConstraint(name = "UK_USER_USERNAME", columnNames = "username") })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String subject;

	@Column(nullable = false)
	@Email
	private String email;

	@Column(name = "username", nullable = false)
	private String preferredUsername;

	@OneToMany(mappedBy = "grantedUser", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Proxy> grantedProxies = new ArrayList<>();

	@OneToMany(mappedBy = "grantingUser", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Proxy> grantingProxies = new ArrayList<>();

	public User(String subject, String email, String preferredUsername) {
		this.subject = subject;
		this.email = email;
		this.preferredUsername = preferredUsername;
	}
}