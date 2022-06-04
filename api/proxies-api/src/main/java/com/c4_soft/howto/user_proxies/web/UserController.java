package com.c4_soft.howto.user_proxies.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.c4_soft.howto.user_proxies.domain.Proxy;
import com.c4_soft.howto.user_proxies.domain.User;
import com.c4_soft.howto.user_proxies.exceptions.ProxyUsersUnmodifiableException;
import com.c4_soft.howto.user_proxies.exceptions.ResourceNotFoundException;
import com.c4_soft.howto.user_proxies.jpa.ProxyRepository;
import com.c4_soft.howto.user_proxies.jpa.UserRepository;
import com.c4_soft.howto.user_proxies.security.ProxiesAuthentication;
import com.c4_soft.howto.user_proxies.web.dtos.ProxyDto;
import com.c4_soft.howto.user_proxies.web.dtos.ProxyEditDto;
import com.c4_soft.howto.user_proxies.web.dtos.UserCreateDto;
import com.c4_soft.howto.user_proxies.web.dtos.UserDto;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcAuthentication;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/users", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@RequiredArgsConstructor
@Tag(name = "Users", description = "CRUD operations for grants delegation between users")
public class UserController {
	private final UserRepository userRepo;
	private final UserMapper userMapper;
	private final ProxyRepository proxyRepo;
	private final UserProxyMapper proxyMapper;

	@GetMapping
	@Operation(description = "Retrieve collection of users.")
	@PreAuthorize("isAuthenticated()")
	public List<UserDto> retrieveByEmailOrPreferredUsernamePart(
			@RequestParam(name = "emailOrPreferredUsernamePart") @Parameter(description = "Mandatory and min length is 4. Case insensitive part of user e-mail or preferredUserName.") @Length(min = 4) String emailOrPreferredUsernamePart,
			ProxiesAuthentication auth) {
		return userRepo.findAll(UserRepository.searchSpec(emailOrPreferredUsernamePart)).stream().map(userMapper::toDto).toList();
	}

	@PostMapping
	@Operation(description = "Register a user in proxies service")
	@PreAuthorize("#auth.name == #dto.subject or hasAnyAuthority('USERS_ADMIN')")
	public ResponseEntity<?> create(@RequestBody @Valid UserCreateDto dto, ProxiesAuthentication auth) {
		final var user = new User();
		userMapper.update(user, dto);
		return ResponseEntity.created(URI.create(userRepo.save(user).getId().toString())).build();
	}

	@GetMapping("/{subject}")
	@Operation(description = "Retrieve a user by subject")
	@PreAuthorize("isAuthenticated()")
	public UserDto retrieveBySubject(
			@PathVariable(name = "subject", required = false) @Parameter(description = "User subject.") String subject,
			ProxiesAuthentication auth) {
		return userRepo
				.findBySubject(subject)
				.map(userMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("No user with subject %s", subject)));
	}

	@GetMapping("/{subject}/proxies/granted")
	@PreAuthorize("#auth.name == #subject or hasAnyAuthority('AUTHORIZATION_SERVER', 'USERS_ADMIN') or #auth.allows(#subject, 'READ_PROXIES')")
	public List<ProxyDto> retrieveGrantedProxies(
			@PathVariable(name = "subject", required = false) @Parameter(description = "User subject.") String subject,
			ProxiesAuthentication auth) {
		return proxyRepo
				.findAll(ProxyRepository.searchSpec(Optional.empty(), Optional.ofNullable(subject), Optional.empty()))
				.stream()
				.map(proxyMapper::toDto)
				.toList();
	}

	@GetMapping("/{subject}/proxies/granting")
	@PreAuthorize("#auth.name == #subject or hasAnyAuthority('USERS_ADMIN') or #auth.allows(#subject, 'READ_PROXIES')")
	public List<ProxyDto> retrieveGrantingProxies(
			@PathVariable(name = "subject", required = false) @Parameter(description = "User subject.") String subject,
			ProxiesAuthentication auth) {
		return proxyRepo
				.findAll(ProxyRepository.searchSpec(Optional.ofNullable(subject), Optional.empty(), Optional.empty()))
				.stream()
				.map(proxyMapper::toDto)
				.toList();
	}

	@PostMapping("/{grantingUserSubject}/proxies/granted/{grantedUserSubject}")
	@Operation(description = "Create grant delegation from \"granting user\" to \"granted user\".")
	@PreAuthorize("#auth.name == #grantingUserSubject or hasAnyAuthority('USERS_ADMIN') or #auth.allows(#grantingUserSubject, 'EDIT_PROXIES')")
	public ResponseEntity<?> createProxy(
			@PathVariable(name = "grantingUserSubject") @Parameter(description = "Proxied user subject") @NotEmpty String grantingUserSubject,
			@PathVariable(name = "grantedUserSubject") @Parameter(description = "Granted user subject") @NotEmpty String grantedUserSubject,
			@Valid @RequestBody ProxyEditDto dto,
			ProxiesAuthentication auth) {
		final var proxy = Proxy.builder().grantingUser(getUser(grantingUserSubject)).grantedUser(getUser(grantedUserSubject)).build();
		proxyMapper.update(proxy, dto);
		final var created = proxyRepo.save(proxy);
		proxyRepo.saveAll(processOverlaps(created));
		return ResponseEntity.created(URI.create(created.getId().toString())).build();
	}

	@PutMapping("/{grantingUserSubject}/proxies/granted/{grantedUserSubject}/{id}")
	@Operation(description = "Update grant delegation from \"granting user\" to \"granted user\".")
	@PreAuthorize("#auth.name == #grantingUserSubject or hasAnyAuthority('USERS_ADMIN') or #auth.allows(#grantingUserSubject, 'EDIT_PROXIES')")
	public ResponseEntity<?> updateProxy(
			@PathVariable(name = "grantingUserSubject") @Parameter(description = "Proxied user subject") @NotEmpty String grantingUserSubject,
			@PathVariable(name = "grantedUserSubject") @Parameter(description = "Granted user subject") @NotEmpty String grantedUserSubject,
			@PathVariable(name = "id") @Parameter(description = "proxy ID") Long id,
			@Valid @RequestBody ProxyEditDto dto,
			ProxiesAuthentication auth) {
		final var proxy = getProxy(id, grantingUserSubject, grantedUserSubject);
		proxyMapper.update(proxy, dto);
		proxyRepo.saveAll(processOverlaps(proxy));
		return ResponseEntity.accepted().build();
	}

	@DeleteMapping("/{grantingUserSubject}/proxies/granted/{grantedUserSubject}/{id}")
	@Operation(description = "Delete all grants \"granted user\" had to act on behalf of \"granting user\".")
	@PreAuthorize("#auth.name == #grantingUserSubject or hasAnyAuthority('USERS_ADMIN') or #auth.allows(#grantingUserSubject, 'EDIT_PROXIES')")
	public ResponseEntity<?> deleteProxy(
			@PathVariable(name = "grantingUserSubject") @Parameter(description = "Proxied user subject") @NotEmpty String grantingUserSubject,
			@PathVariable(name = "grantedUserSubject") @Parameter(description = "Granted user subject") @NotEmpty String grantedUserSubject,
			@PathVariable(name = "id") @Parameter(description = "proxy ID") Long id,
			OidcAuthentication<OidcToken> auth) {
		final var proxy = getProxy(id, grantingUserSubject, grantedUserSubject);
		proxyRepo.delete(proxy);
		return ResponseEntity.accepted().build();
	}

	private Proxy getProxy(Long id, String grantingUserSubject, String grantedUserSubject) {
		final var proxy = proxyRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("No user proxy with ID %s", id)));

		if (!proxy.getGrantingUser().getSubject().equals(grantingUserSubject) || !proxy.getGrantedUser().getSubject().equals(grantedUserSubject)) {
			throw new ProxyUsersUnmodifiableException();
		}

		return proxy;
	}

	private User getUser(String subject) {
		return userRepo.findBySubject(subject).orElseThrow(() -> new ResourceNotFoundException(String.format("No user with subject %s", subject)));
	}

	private List<Proxy> processOverlaps(Proxy proxy) {
		final var proxiesToCheck =
				proxyRepo
						.findAll(
								ProxyRepository
										.searchSpec(
												Optional.of(proxy.getGrantingUser().getSubject()),
												Optional.of(proxy.getGrantedUser().getSubject()),
												Optional.empty()));
		final var modifiedProxies = new ArrayList<Proxy>(proxiesToCheck.size());
		proxiesToCheck.forEach(existing -> {
			if (existing.getId() == proxy.getId()) {
				// skip provided proxy
			} else if (existing.getEnd() != null && existing.getEnd().before(proxy.getStart())) {
				// skip existing ending before provided starts
			} else if (proxy.getEnd() == null) {
				// provided proxy has no end
				if (existing.getStart().after(proxy.getStart()) || existing.getStart().equals(proxy.getStart())) {
					// any existing proxy starting after provided one is deleted
					proxyRepo.delete(existing);
				} else if (existing.getEnd() == null || existing.getEnd().after(proxy.getStart()) || existing.getEnd().equals(proxy.getStart())) {
					// shorten any proxy ending after provided one starts (because of preceding condition, we know it overlaps: starts before provided)
					existing.setEnd(new Date(proxy.getStart().getTime() - 1));
					modifiedProxies.add(existing);
				}
			} else if (existing.getStart().after(proxy.getEnd())) {
				// skip existing starting after provided starts
			} else {
				// existing ending before provided starts already skipped
				existing.setEnd(new Date(proxy.getStart().getTime() - 1L));
				modifiedProxies.add(existing);
			}
		});
		return modifiedProxies;
	}
}
