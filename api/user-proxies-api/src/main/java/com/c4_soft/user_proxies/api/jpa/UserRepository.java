package com.c4_soft.user_proxies.api.jpa;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.domain.User_;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findBySubject(String subject);

	static Specification<User> searchSpec(String emailOrPreferredUsername) {
		if (!StringUtils.hasText(emailOrPreferredUsername)) {
			return null;
		}

		return Specification
				.where(attibuteLikeIgnoreCase(User_.EMAIL, emailOrPreferredUsername))
				.or(attibuteLikeIgnoreCase(User_.PREFERRED_USERNAME, emailOrPreferredUsername));
	}

	private static Specification<User> attibuteLikeIgnoreCase(String attributeName, String needle) {
		return (root, query, criteriaBuilder) -> criteriaBuilder
				.like(criteriaBuilder.lower(root.get(attributeName)), String.format("%%%s%%", needle.trim().toLowerCase()));
	}
}