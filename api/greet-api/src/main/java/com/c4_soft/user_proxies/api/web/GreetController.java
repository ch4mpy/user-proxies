package com.c4_soft.user_proxies.api.web;

import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.c4_soft.user_proxies.api.security.ProxiesAuthentication;

@RestController
@RequestMapping("/greet")
@PreAuthorize("isAuthenticated()")
public class GreetController {

	@GetMapping()
	@PreAuthorize("hasAuthority('NICE_GUY')")
	public String getGreeting(ProxiesAuthentication auth) {
		return String
				.format(
						"Hi %s! You are granted with: %s and can proxy: %s.",
						auth.getToken().getPreferredUsername(),
						auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ", "[", "]")),
						auth.getProxies().keySet().stream().collect(Collectors.joining(", ", "[", "]")));
	}

	@GetMapping("/{otherSubject}")
	@PreAuthorize("onBehalfOf(#otherSubject).can('greet')")
	public String getGreetingOnBehalfOf(@PathVariable("otherSubject") String otherSubject, ProxiesAuthentication auth) {
		return String.format("Hi %s!", otherSubject);
	}
}