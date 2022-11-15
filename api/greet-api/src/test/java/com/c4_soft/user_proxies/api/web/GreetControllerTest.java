package com.c4_soft.user_proxies.api.web;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.MockMvcSupport;
import com.c4_soft.user_proxies.api.security.WithSecurity;
import com.c4_soft.user_proxies.api.security.ProxiesId;
import com.c4_soft.user_proxies.api.security.ProxiesId.Proxy;
import com.c4_soft.user_proxies.api.web.dto.Grant;

@WebMvcTest(GreetController.class)
@WithSecurity
class GreetControllerTest {

	@Autowired
	MockMvcSupport mockMvc;

	@Test
	@ProxiesId(authorities = { "NICE", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROFILE_READ }),
			@Proxy(onBehalfOf = "chose") })
	void testGreet() throws Exception {
		mockMvc
				.get("/greet")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message", is("Hi Tonton Pirate! You are granted with: [NICE, AUTHOR] and can proxy: [chose, machin].")));
	}

	@Test
	@ProxiesId(authorities = { "NICE", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {})
	void testWithNiceAuthority() throws Exception {
		mockMvc.get("/greet/ch4mpy").andExpect(status().isOk()).andExpect(jsonPath("$.message", is("Hi ch4mpy!")));
	}

	@Test
	@ProxiesId(authorities = { "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "ch4mpy", can = { Grant.PROFILE_READ, Grant.GREET }) })
	void testWithProxy() throws Exception {
		mockMvc.get("/greet/ch4mpy").andExpect(status().isOk()).andExpect(jsonPath("$.message", is("Hi ch4mpy!")));
	}

	@Test
	@ProxiesId(authorities = { "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "ch4mpy", can = { Grant.PROFILE_READ, Grant.GREET }) })
	void testWithHimself() throws Exception {
		mockMvc.get("/greet/Tonton Pirate").andExpect(status().isOk()).andExpect(jsonPath("$.message", is("Hi Tonton Pirate!")));
	}

	@Test
	@ProxiesId(authorities = { "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"))
	void testWithoutNiceAuthorityNorProxyNorHimself() throws Exception {
		mockMvc.get("/greet/ch4mpy").andExpect(status().isForbidden());
	}
}
