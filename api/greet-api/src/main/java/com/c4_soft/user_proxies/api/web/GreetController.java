package com.c4_soft.user_proxies.api.web;

import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.c4_soft.user_proxies.api.security.ProxiesAuthentication;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path = "/greet", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Tag(name = "Greet")
public class GreetController {

	@GetMapping()
	@PreAuthorize("hasAuthority('NICE')")
	public GreetDto getGreeting(ProxiesAuthentication auth) {
		return new GreetDto(
						"Hi %s! You are granted with: %s and can proxy: %s.".formatted(
						auth.getClaims().getPreferredUsername(),
						auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ", "[", "]")),
						auth.getClaims().getProxies().keySet().stream().collect(Collectors.joining(", ", "[", "]"))));
	}

	@GetMapping("/{username}")
	@PreAuthorize("is(#username) or isNice() or onBehalfOf(#username).can('GREET')")
	public GreetDto getGreetingOnBehalfOf(@PathVariable("username") String username) {
		return new GreetDto("Hi %s!".formatted(username));
	}
}