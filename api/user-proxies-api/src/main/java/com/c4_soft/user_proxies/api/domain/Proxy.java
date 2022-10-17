package com.c4_soft.user_proxies.api.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.c4_soft.user_proxies.api.web.dto.Grant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_proxies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proxy {
	@Id
	@GeneratedValue
	private Long id;

	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "granting_user_id", updatable = false, nullable = false)
	private User grantingUser;

	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "granted_user_id", updatable = false, nullable = false)
	private User grantedUser;

	@NotNull
	@ElementCollection(fetch = FetchType.EAGER)
	@Default
	private List<Grant> grants = new ArrayList<>();

	@NotNull
	@Column(name = "start_date", nullable = false, updatable = true)
	private Date start;

	@Column(name = "end_date", nullable = true, updatable = true)
	private Date end;

	public void allow(Grant grant) {
		if (!grants.contains(grant)) {
			grants.add(grant);
		}
	}

	public void deny(Grant grant) {
		grants.remove(grant);
	}
	
	public boolean isActive() {
		final var now = new Date();
		return start.before(now) && (end == null || end.after(now));
	}
}