package com.c4_soft.howto.user_proxies.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManagerResolver;

import com.c4_soft.howto.user_proxies.EnableSpringDataWebSupportTestConf;
import com.c4_soft.howto.user_proxies.WithProxiesOidcToken;
import com.c4_soft.howto.user_proxies.WithProxiesOidcToken.Grant;
import com.c4_soft.howto.user_proxies.domain.Proxy;
import com.c4_soft.howto.user_proxies.domain.User;
import com.c4_soft.howto.user_proxies.jpa.ProxyRepository;
import com.c4_soft.howto.user_proxies.jpa.UserRepository;
import com.c4_soft.springaddons.security.oauth2.config.synchronised.OidcServletApiSecurityConfig;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.MockMvcSupport;

@WebMvcTest(controllers = { UserController.class })
@Import({ EnableSpringDataWebSupportTestConf.class, MockMvcSupport.class, OidcServletApiSecurityConfig.class })
@ComponentScan(basePackageClasses = { UserProxyMapper.class })
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
	@WithProxiesOidcToken()
	void whenAuthenticatedWithoutRequiredAuthoritiesNorProxiesThenForbidden() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isForbidden());
	}

	@Test
	@WithProxiesOidcToken(authorities = "AUTHORIZATION_SERVER")
	void whenAuthenticatedAsAuthorizationServerThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = "USERS_ADMIN")
	void whenAuthenticatedAsAdminThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = {}, claims = @OpenIdClaims(sub = "machin"))
	void whenAuthenticatedAsProxiedUserThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = { @Grant(onBehalfOf = "machin", can = { "READ_PROXIES" }) })
	void whenGrantedWithEditProxiesForProxiedUserThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = {
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
	@WithProxiesOidcToken()
	void whenAuthenticatedWithoutRequiredAuthoritiesNorProxiesThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

	@Test
	@WithProxiesOidcToken(authorities = "AUTHORIZATION_SERVER")
	void whenAuthenticatedAsAuthorizationServerThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

	@Test
	@WithProxiesOidcToken(authorities = "USERS_ADMIN")
	void whenAuthenticatedAsAdminThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = {}, claims = @OpenIdClaims(sub = "machin"))
	void whenAuthenticatedAsProxiedUserThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = { @Grant(onBehalfOf = "machin", can = { "READ_PROXIES" }) })
	void whenGrantedWithEditProxiesForProxiedUserThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@WithProxiesOidcToken(authorities = {}, claims = @OpenIdClaims(sub = "truc"), grants = {
			@Grant(onBehalfOf = "machin", can = { "DO_SOMETHING" }), // right granting user but wrong grant
			@Grant(onBehalfOf = "bidule", can = { "READ_PROXIES" }) }) // right grant but wrong granting user
	void whenNotGrantedWithEditProxiesThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUserSubject}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

}
