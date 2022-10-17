package com.c4_soft.user_proxies.api.web;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.exceptions.ProxyUsersUnmodifiableException;
import com.c4_soft.user_proxies.api.exceptions.ResourceNotFoundException;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;
import com.c4_soft.user_proxies.api.jpa.UserRepository;
import com.c4_soft.user_proxies.api.web.dto.Grant;
import com.c4_soft.user_proxies.api.web.dto.ProxyDto;
import com.c4_soft.user_proxies.api.web.dto.ProxyEditDto;
import com.c4_soft.user_proxies.api.web.dto.UserCreateDto;
import com.c4_soft.user_proxies.api.web.dto.UserDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

@Validated
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
	@PreAuthorize("hasAuthority('USERS_ADMIN')")
	public List<UserDto> retrieveByEmailOrPreferredUsernamePart(
			@RequestParam(name = "emailOrPreferredUsernamePart") @Parameter(description = "Mandatory. Case insensitive part of user e-mail or preferredUserName.") @NotEmpty String emailOrPreferredUsernamePart) {
		return userRepo.findAll(UserRepository.searchSpec(emailOrPreferredUsernamePart)).stream().map(userMapper::toDto)
				.toList();
	}

	@GetMapping("/usernames")
	@Operation(description = "Retrieve usernames like")
	@PreAuthorize("isAuthenticated()")
	public List<String> retrieveUsernamesLike(
			@RequestParam(name = "preferredUsernamePart") @Parameter(description = "Mandatory. Case insensitive part of preferredUserName.") @Length(min = 3) String preferredUsernamePart) {
		return userRepo.findAll(UserRepository.searchSpec(preferredUsernamePart)).stream().map(User::getPreferredUsername)
				.toList();
	}

	@PostMapping
	@Operation(description = "Register a user in proxies service")
	@PreAuthorize("is(#dto.preferredUsername) or hasAuthority('USERS_ADMIN')")
	public ResponseEntity<Void> create(@RequestBody @Valid UserCreateDto dto) {
		final var user = new User();
		userMapper.update(user, dto);
		return ResponseEntity.created(URI.create(URLEncoder.encode(userRepo.save(user).getPreferredUsername(), StandardCharsets.UTF_8))).build();
	}

	@GetMapping("/{username}")
	@Operation(description = "Retrieve a user by preferredUsername")
	@PreAuthorize("is(#username) or hasAuthority('USERS_ADMIN') or onBehalfOf(#username).can('PROFILE_READ')")
	public UserDto retrieveByPreferredUsername(
			@PathVariable(name = "username", required = false) @Parameter(description = "User preferredUsername.") String username) {
		return userRepo.findByPreferredUsername(username).map(userMapper::toDto).orElseThrow(
				() -> new ResourceNotFoundException(String.format("No user with preferred_username %s", username)));
	}

	@GetMapping("/{username}/proxies/granted")
	@PreAuthorize("is(#username) or hasAnyAuthority('TOKEN_ISSUER', 'USERS_ADMIN') or onBehalfOf(#username).can('PROXIES_READ')")
	public List<ProxyDto> retrieveGrantedProxies(
			@PathVariable(name = "username", required = false) @Parameter(description = "User preferredUsername.") String username,
			@RequestParam(name = "isActiveOnly", defaultValue = "false") boolean isActiveOnly) {
		return proxyRepo
				.findAll(ProxyRepository.searchSpec(Optional.empty(), Optional.ofNullable(username), Optional.empty()))
				.stream().filter(p -> !isActiveOnly || p.isActive()).map(proxyMapper::toDto).toList();
	}

	@GetMapping("/{username}/proxies/granting")
	@PreAuthorize("is(#username) or hasAnyAuthority('USERS_ADMIN') or onBehalfOf(#username).can('PROXIES_READ')")
	public List<ProxyDto> retrieveGrantingProxies(
			@PathVariable(name = "username", required = false) @Parameter(description = "User preferredUsername.") String username) {
		return proxyRepo
				.findAll(ProxyRepository.searchSpec(Optional.ofNullable(username), Optional.empty(), Optional.empty()))
				.stream().map(proxyMapper::toDto).toList();
	}

	@PostMapping("/{grantingUsername}/proxies/granted/{grantedUsername}")
	@Operation(description = "Create grant delegation from \"granting user\" to \"granted user\".")
	@PreAuthorize("is(#grantingUsername) or hasAuthority('USERS_ADMIN') or onBehalfOf(#grantingUsername).can('PROXIES_EDIT')")
	public ResponseEntity<Void> createProxy(
			@PathVariable(name = "grantingUsername") @Parameter(description = "Proxied user preferredUsername") @NotEmpty String grantingUsername,
			@PathVariable(name = "grantedUsername") @Parameter(description = "Granted user preferredUsername") @NotEmpty String grantedUsername,
			@Valid @RequestBody ProxyEditDto dto) {
		final var proxy = Proxy.builder().grantingUser(getUser(grantingUsername)).grantedUser(getUser(grantedUsername))
				.build();
		proxyMapper.update(proxy, dto);

		// add required READ_PROFILE grant if missing (granted user should always be
		// able to retrieve granting user profile basic data)
		proxy.allow(Grant.PROFILE_READ);

		// persist new proxy (and get a DB ID)
		final var created = proxyRepo.save(proxy);

		// process and save proxies overlaps
		proxyRepo.saveAll(processOverlaps(proxy));

		return ResponseEntity.created(URI.create(created.getId().toString())).build();
	}

	@PutMapping("/{grantingUsername}/proxies/granted/{grantedUsername}/{id}")
	@Operation(description = "Update grant delegation from \"granting user\" to \"granted user\".")
	@PreAuthorize("is(#grantingUsername) or hasAuthority('USERS_ADMIN') or onBehalfOf(#grantingUsername).can('PROXIES_EDIT')")
	public ResponseEntity<Void> updateProxy(
			@PathVariable(name = "grantingUsername") @Parameter(description = "Proxied user preferredUsername") @NotEmpty String grantingUsername,
			@PathVariable(name = "grantedUsername") @Parameter(description = "Granted user preferredUsername") @NotEmpty String grantedUsername,
			@PathVariable(name = "id") @Parameter(description = "proxy ID") Long id,
			@Valid @RequestBody ProxyEditDto dto) {
		final var proxy = getProxy(id, grantingUsername, grantedUsername);
		proxyMapper.update(proxy, dto);
		proxy.allow(Grant.PROFILE_READ);
		proxyRepo.saveAll(processOverlaps(proxy));
		return ResponseEntity.accepted().build();
	}

	@DeleteMapping("/{grantingUsername}/proxies/granted/{grantedUsername}/{id}")
	@Operation(description = "Delete all grants \"granted user\" had to act on behalf of \"granting user\".")
	@PreAuthorize("is(#grantingUsername) or hasAuthority('USERS_ADMIN') or onBehalfOf(#grantingUsername).can('PROXIES_EDIT')")
	public ResponseEntity<Void> deleteProxy(
			@PathVariable(name = "grantingUsername") @Parameter(description = "Proxied user preferredUsername") @NotEmpty String grantingUsername,
			@PathVariable(name = "grantedUsername") @Parameter(description = "Granted user preferredUsername") @NotEmpty String grantedUsername,
			@PathVariable(name = "id") @Parameter(description = "proxy ID") Long id) {
		final var proxy = getProxy(id, grantingUsername, grantedUsername);
		proxyRepo.delete(proxy);
		return ResponseEntity.accepted().build();
	}

	private Proxy getProxy(Long id, String grantingUsername, String grantedUsername) {
		final var proxy = proxyRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("No user proxy with ID %s", id)));

		if (!proxy.getGrantingUser().getPreferredUsername().equals(grantingUsername)
				|| !proxy.getGrantedUser().getPreferredUsername().equals(grantedUsername)) {
			throw new ProxyUsersUnmodifiableException();
		}

		return proxy;
	}

	private User getUser(String preferredUsername) {
		return userRepo.findByPreferredUsername(preferredUsername).orElseThrow(() -> new ResourceNotFoundException(
				String.format("No user with preferredUsername %s", preferredUsername)));
	}

	List<Proxy> processOverlaps(Proxy proxy) {
		final var proxiesToCheck = proxyRepo
				.findAll(ProxyRepository.searchSpec(Optional.of(proxy.getGrantingUser().getPreferredUsername()),
						Optional.of(proxy.getGrantedUser().getPreferredUsername()), Optional.empty()));
		final var modifiedProxies = new ArrayList<Proxy>(proxiesToCheck.size());
		proxiesToCheck.forEach(existing -> {
			if (Objects.equals(existing.getId(), proxy.getId())) {
				// skip provided proxy
			} else if (existing.getEnd() != null && existing.getEnd().before(proxy.getStart())) {
				// skip existing ending before provided starts
			} else if (proxy.getEnd() == null) {
				// provided proxy has no end
				if (existing.getStart().after(proxy.getStart()) || existing.getStart().equals(proxy.getStart())) {
					// any existing proxy starting after provided one is deleted
					proxyRepo.delete(existing);
				} else if (existing.getEnd() == null || existing.getEnd().after(proxy.getStart())
						|| existing.getEnd().equals(proxy.getStart())) {
					// shorten any proxy ending after provided one starts (because of preceding
					// condition, we know it overlaps: starts before provided)
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