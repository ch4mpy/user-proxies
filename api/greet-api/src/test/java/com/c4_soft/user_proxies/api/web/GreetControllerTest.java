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
import com.c4_soft.user_proxies.api.security.WebSecurityConfig;
import com.c4_soft.user_proxies.api.security.WithProxiesAuth;
import com.c4_soft.user_proxies.api.security.WithProxiesAuth.Grant;

@WebMvcTest(GreetController.class)
@AutoConfigureSecurityAddons
@Import({ WebSecurityConfig.class })
class GreetControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	@WithProxiesAuth(authorities = { "NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), grants = {
			@Grant(onBehalfOf = "machin", can = { "truc", "bidule" }),
			@Grant(onBehalfOf = "chose") })
	void testGreet() throws Exception {
		mockMvc
				.perform(get("/greet").secure(true))
				.andExpect(status().isOk())
				.andExpect(content().string("Hi Tonton Pirate! You are granted with: [NICE_GUY, AUTHOR] and can proxy: [chose, machin]."));
	}

	@Test
	@WithProxiesAuth(authorities = { "NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), grants = {
			@Grant(onBehalfOf = "ch4mpy", can = { "greet" }) })
	void testWithProxy() throws Exception {
		mockMvc.perform(get("/greet/ch4mpy").secure(true)).andExpect(status().isOk()).andExpect(content().string("Hi ch4mpy!"));
	}

	@Test
	@WithProxiesAuth(authorities = { "NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"))
	void testWithoutProxy() throws Exception {
		mockMvc.perform(get("/greet/ch4mpy").secure(true)).andExpect(status().isForbidden());
	}

}
