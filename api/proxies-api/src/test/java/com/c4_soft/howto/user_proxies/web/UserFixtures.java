package com.c4_soft.howto.user_proxies.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.c4_soft.howto.user_proxies.domain.User;
import com.c4_soft.howto.user_proxies.jpa.UserRepository;

public class UserFixtures {
	public static User admin() {
		return new User(1L, "admin-a", "ch4mp@c4-soft.com", "admin", new ArrayList<>(), new ArrayList<>());
	}

	public static User userA() {
		return new User(2L, "user-a", "jwacongne@c4-soft.com", "tonton-pirate", new ArrayList<>(), new ArrayList<>());
	}

	public static User userB() {
		return new User(3L, "user-b", "jwacongne@gmail.com", "ch4mpy", new ArrayList<>(), new ArrayList<>());
	}

	public static Map<String, User> all() {
		return Stream.of(admin(), userA(), userB()).collect(Collectors.toMap(User::getSubject, u -> u));
	}

	public static Map<String, User> setUp(UserRepository userRepository) {
		final var users = UserFixtures.all();
		when(userRepository.findBySubject(anyString())).thenAnswer(invocation -> Optional.ofNullable(users.get(invocation.getArgument(0))));
		return users;
	}
}