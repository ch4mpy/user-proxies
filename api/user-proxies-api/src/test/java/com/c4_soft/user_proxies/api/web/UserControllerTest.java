package com.c4_soft.user_proxies.api.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManagerResolver;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.MockMvcSupport;
import com.c4_soft.user_proxies.api.ControllerTest;
import com.c4_soft.user_proxies.api.ProxyFixtures;
import com.c4_soft.user_proxies.api.UserFixtures;
import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;
import com.c4_soft.user_proxies.api.jpa.UserRepository;
import com.c4_soft.user_proxies.api.security.WithProxiesAuth;
import com.c4_soft.user_proxies.api.security.WithProxiesAuth.Grant;

@WebMvcTest(controllers = { UserController.class })
@ControllerTest
@Import({ UserMapperImpl.class, UserProxyMapperImpl.class })
class UserControllerTests {
	@Autowired
	MockMvcSupport mockMvc;

	@MockBean
	AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver;

	@MockBean
	UserRepository userRepository;

	@MockBean
	ProxyRepository proxyRepository;

	@Autowired
	UserProxyMapper userProxyMapper;

	Map<String, User> users;

	Map<String, List<Proxy>> proxies;

	@BeforeEach
	public void setUp() {
		users = UserFixtures.setUp(userRepository);
		proxies = ProxyFixtures.setUp(proxyRepository);
	}

	// @formatter:off
	// Test access to UserController::retrieveGrantedProxies which is secured with:
	/** #token.subject == #subject or hasAnyAuthority('AUTHORIZATION_SERVER', 'USERS_ADMIN') or #token.allows(#subject, 'READ_PROXIES') */
	// @formatter:on
	@Test
	void whenAnonymousThenUnauthorized() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isUnauthorized());
	}

	@Test
	@WithProxiesAuth()
	void whenAuthenticatedWithoutRequiredAuthoritiesNorProxiesThenForbidden() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isForbidden());
	}

	@Test
	@WithProxiesAuth(authorities = "TOKEN_ISSUER")
	void whenAuthenticatedAsAuthorizationServerThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = "USERS_ADMIN")
	void whenAuthenticatedAsAdminThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = {}, claims = @OpenIdClaims(sub = "machin"))
	void whenAuthenticatedAsProxiedUserThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = {
			@Grant(onBehalfOf = "machin", can = { "READ_PROXIES" }) })
	void whenGrantedWithEditProxiesForProxiedUserThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = {
			@Grant(onBehalfOf = "machin", can = { "DO_SOMETHING" }), // right granting user but wrong grant
			@Grant(onBehalfOf = "bidule", can = { "READ_PROXIES" }) }) // right grant but wrong granting user
	void whenNotGrantedWithEditProxiesThenForbidden() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isForbidden());
	}

	// @formatter:off
	// Test access to UserController::retrieveGrantingProxies which is secured with:
	/** #token.subject == #subject or hasAnyAuthority('USERS_ADMIN') or #token.allows(#subject, 'READ_PROXIES') */
	// @formatter:on
	@Test
	void whenAnonymousThenUnauthorizedToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isUnauthorized());
	}

	@Test
	@WithProxiesAuth()
	void whenAuthenticatedWithoutRequiredAuthoritiesNorProxiesThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

	@Test
	@WithProxiesAuth(authorities = "AUTHORIZATION_SERVER")
	void whenAuthenticatedAsAuthorizationServerThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

	@Test
	@WithProxiesAuth(authorities = "USERS_ADMIN")
	void whenAuthenticatedAsAdminThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = {}, claims = @OpenIdClaims(sub = "machin"))
	void whenAuthenticatedAsProxiedUserThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = {
			@Grant(onBehalfOf = "machin", can = { "READ_PROXIES" }) })
	void whenGrantedWithEditProxiesForProxiedUserThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesAuth(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = {
			@Grant(onBehalfOf = "machin", can = { "DO_SOMETHING" }), // right granting user but wrong grant
			@Grant(onBehalfOf = "bidule", can = { "READ_PROXIES" }) }) // right grant but wrong granting user
	void whenNotGrantedWithEditProxiesThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

}