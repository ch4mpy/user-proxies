package com.c4_soft.user_proxies.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;
import com.c4_soft.user_proxies.api.security.Permission;

public class ProxyFixtures {

	private static Proxy userAToUserB() {
		return new Proxy(
				2L,
				UserFixtures.userA(),
				UserFixtures.userB(),
				List.of(Permission.values()),
				Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
				Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
	}

	private static Proxy userBToUserA() {
		return new Proxy(2L, UserFixtures.userB(), UserFixtures.userA(), List.of(Permission.PROFILE_READ), Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), null);
	}

	public static Map<String, List<Proxy>> all() {
		final var proxiesByGrantingUser = Stream.of(userAToUserB(), userBToUserA()).collect(Collectors.groupingBy(p -> p.getGrantingUser().getPreferredUsername()));
		for (final var proxiesUserProxies : proxiesByGrantingUser.entrySet()) {
			for (final var proxy : proxiesUserProxies.getValue()) {
				proxy.getGrantedUser().getGrantedProxies().add(proxy);
				proxy.getGrantingUser().getGrantingProxies().add(proxy);
			}
		}
		return proxiesByGrantingUser;
	}

	public static Map<String, List<Proxy>> setUp(ProxyRepository proxyRepository) {
		final var proxies = all();
		when(proxyRepository.findById(anyLong()))
				.thenAnswer(
						invocation -> proxies
								.entrySet()
								.stream()
								.map(Entry::getValue)
								.map(List::stream)
								.flatMap(p -> p)
								.filter(p -> p.getId().equals(invocation.getArgument(0)))
								.findAny());
		return proxies;
	}
}