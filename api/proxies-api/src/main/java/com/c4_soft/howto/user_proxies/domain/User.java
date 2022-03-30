package com.c4_soft.howto.user_proxies.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String subject;

	@Column(nullable = false, unique = true)
	@Email
	private String email;

	@Column(nullable = false, unique = true)
	private String preferedUsername;

	@OneToMany(mappedBy = "grantedUser", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Proxy> grantedProxies = new ArrayList<>();

	@OneToMany(mappedBy = "grantingUser", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Proxy> grantingProxies = new ArrayList<>();

	public User(String subject, String email, String preferedUsername) {
		this.subject = subject;
		this.email = email;
		this.preferedUsername = preferedUsername;
	}
}
