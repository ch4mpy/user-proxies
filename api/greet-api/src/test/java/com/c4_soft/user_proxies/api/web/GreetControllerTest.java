package com.c4_soft.user_proxies.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.AutoConfigureSecurityAddons;
import com.c4_soft.user_proxies.api.security.ProxiesId;
import com.c4_soft.user_proxies.api.security.ProxiesId.Proxy;
import com.c4_soft.user_proxies.api.security.WebSecurityConfig;
import com.c4_soft.user_proxies.api.web.dto.Grant;

@WebMvcTest(GreetController.class)
@AutoConfigureSecurityAddons
@Import({ WebSecurityConfig.class })
class GreetControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	@ProxiesId(authorities = { "NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROFILE_READ }),
			@Proxy(onBehalfOf = "chose") })
	void testGreet() throws Exception {
		mockMvc
				.perform(get("/greet").secure(true))
				.andExpect(status().isOk())
				.andExpect(content().string("Hi Tonton Pirate! You are granted with: [NICE_GUY, AUTHOR] and can proxy: [chose, machin]."));
	}

	@Test
	@ProxiesId(authorities = { "ROLE_NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {})
	void testWithNiceAuthority() throws Exception {
		mockMvc.perform(get("/greet/ch4mpy").secure(true)).andExpect(status().isOk()).andExpect(content().string("Hi ch4mpy!"));
	}

	@Test
	@ProxiesId(authorities = { "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "ch4mpy", can = { Grant.PROFILE_READ, Grant.GREET }) })
	void testWithProxy() throws Exception {
		mockMvc.perform(get("/greet/ch4mpy").secure(true)).andExpect(status().isOk()).andExpect(content().string("Hi ch4mpy!"));
	}

	@Test
	@ProxiesId(authorities = { "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "ch4mpy", can = { Grant.PROFILE_READ, Grant.GREET }) })
	void testWithHimself() throws Exception {
		mockMvc.perform(get("/greet/Tonton Pirate").secure(true)).andExpect(status().isOk()).andExpect(content().string("Hi Tonton Pirate!"));
	}

	@Test
	@ProxiesId(authorities = { "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"))
	void testWithoutNiceAuthorityNorProxyNorHimself() throws Exception {
		mockMvc.perform(get("/greet/ch4mpy").secure(true)).andExpect(status().isForbidden());
	}

}
