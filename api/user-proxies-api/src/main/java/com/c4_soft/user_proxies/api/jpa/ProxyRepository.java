package com.c4_soft.user_proxies.api.jpa;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.domain.Proxy_;
import com.c4_soft.user_proxies.api.domain.User_;

public interface ProxyRepository extends JpaRepository<Proxy, Long>, JpaSpecificationExecutor<Proxy> {
	static Specification<Proxy> searchSpec(Optional<String> grantingUsername, Optional<String> grantedUsername, Optional<Date> date) {
		final var specs =
				Stream
						.of(
								Optional.of(endsAfter(date.orElse(new Date()))),
								grantingUsername.map(ProxyRepository::grantingUserPreferredUsernameLike),
								grantedUsername.map(ProxyRepository::grantedUserPreferredUsernameLike),
								date.map(ProxyRepository::startsBefore))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.toList();
		var spec = Specification.where(specs.get(0));
		for (var i = 1; i < specs.size(); ++i) {
			spec = spec.and(specs.get(i));
		}
		return spec;
	}

	static Specification<Proxy> endsAfter(Date date) {
		return (root, query, cb) -> cb.or(cb.isNull(root.get(Proxy_.end)), cb.greaterThanOrEqualTo(root.get(Proxy_.end), date));
	}

	static Specification<Proxy> grantingUserPreferredUsernameLike(String grantingUsername) {
		return (root, query, cb) -> cb.like(root.get(Proxy_.grantingUser).get(User_.preferredUsername), grantingUsername);
	}

	static Specification<Proxy> grantedUserPreferredUsernameLike(String grantedUsername) {
		return (root, query, cb) -> cb.like(root.get(Proxy_.grantedUser).get(User_.preferredUsername), grantedUsername);
	}

	static Specification<Proxy> startsBefore(Date date) {
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(Proxy_.start), date);
	}
}