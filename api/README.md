# Keycloak mapper & RESTful API

We'll build a maven project with a few modules:
- a spring-boot resource-server to expose and manage user proxies stored with JPA
- a Keycloak mapper which will retrieve user proxies from preceding web-service and add it to access & ID tokens
- libraries shared between modules
- a second resource server serving as secured micro-service sample

## [`spring-addons`](https://github.com/ch4mpy/spring-addons)
`spring-security-oauth2-test-webmvc-addons` and `spring-security-oauth2-test-webflux-addons` might be the easiest way to configure spring-boot OpenID secured REST APIs (respectively servlet and reactive).

Let's start our project with one of proposed [maven archetypes](https://github.com/ch4mpy/spring-addons/tree/master/archetypes).

Create a `user-proxies` workspace directory for this tutorial, `cd` to it and generate api maven project generation from archetype:
```bash
mkdir user-proxies
cd user-proxies
mvn archetype:generate \
  -DarchetypeCatalog=remote \
  -DarchetypeGroupId=com.c4-soft.springaddons \
  -DarchetypeArtifactId=spring-webmvc-archetype-multimodule \
  -DarchetypeVersion=5.1.5 \
  -DgroupId=com.c4-soft.user-proxies \
  -DartifactId=api \
  -Dversion=1.0.0-SNAPSHOT \
  -Dapi-artifactId=user-proxies-api \
  -Dapi-path=user-proxies
```

## Additional modules
We will create several resource-servers and all will use custom security layer with "user-proxies".

In parent pom, add `security`, `security-test`, `keycloak-mapper` and `greet-api` modules with dependency management for the first two. Also add keycloak version as property:
```xml
	<modules>
		<module>dtos</module>
		<module>exceptions</module>
		<module>security</module>
		<module>security-test</module>
		<module>user-proxies-api</module>
		<module>proxies-keycloak-mapper</module>
		<module>greet-api</module>
	</modules>
	
	<properties>
		<keycloak.version>19.0.1</keycloak.version>
		...
	</properties>
	
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>com.c4-soft.user-proxies.api</groupId>
				<artifactId>security</artifactId>
				<version>${project.version}</version>
			</dependency>
			<dependency>
				<groupId>com.c4-soft.user-proxies.api</groupId>
				<artifactId>security-test</artifactId>
				<version>${project.version}</version>
			</dependency>
...
```
Please refer to the source for those modules `pom.xml` details

Also, in `user-proxies-api` replace dependencies on `spring-addons-webmvc-jwt-resource-server` and `spring-addons-webmvc-jwt-test` by dependencies on `security` and `security-test` modules.

## Grants
To avoid circular dependencied for grants enum, we'll add it to DTOs modules (it is not an entity and is used a bit of every where). A cleaner approach would be creating a dedicated module.
```java
package com.c4_soft.user_proxies.api.web.dto;

public enum Grant {
	GREET, PROFILE_READ, PROXIES_READ, PROXIES_EDIT
}
```

## `security` module
This module will hold shared security classes and configuration. Spring-addons provides with a lot of auto-configuration, but we need more than standard Role Based Access Control. We'll see how to override some of default conf:
- specialized `OpenidClaimSet` to access JWT `proxies` private-claim
- specialized `OAuthentication` which exposes proxies in addition to user name and roles
- add proxies DSL to security SpEL

Let's first define the domain model:
```java
package com.c4_soft.user_proxies.api.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import com.c4_soft.user_proxies.api.web.dto.Grant;

import lombok.Data;

@Data
public class Proxy implements Serializable {
	private static final long serialVersionUID = 6372863392726898339L;
	
	private final String proxiedUsername;
	private final String tenantUsername;
	private final Set<Grant> permissions;

	public Proxy(String proxiedUsername, String tenantUsername, Set<Grant> permissions) {
		this.proxiedUsername = proxiedUsername;
		this.tenantUsername = tenantUsername;
		this.permissions = Collections.unmodifiableSet(permissions);
	}

	public boolean can(Grant permission) {
		return permissions.contains(permission);
	}

	public boolean can(String permission) {
		return this.can(Grant.valueOf(permission));
	}
}
```

Then extend `OpenidClaimSet` to add proxies private-claim parsing:
```java
package com.c4_soft.user_proxies.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.c4_soft.springaddons.security.oauth2.OpenidClaimSet;
import com.c4_soft.user_proxies.api.web.dto.Grant;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProxiesClaimSet extends OpenidClaimSet {
	private static final long serialVersionUID = 38784488788537111L;

	private final Map<String, Proxy> proxies;

	public ProxiesClaimSet(Map<String, Object> claims) {
		super(claims);
		this.proxies = getProxies(this).stream().collect(Collectors.toMap(Proxy::getProxiedUsername, p -> p));
	}

	private static List<Proxy> getProxies(OpenidClaimSet claims) {
		@SuppressWarnings("unchecked")
		final var proxiesClaim = (Map<String, List<String>>) claims.get("proxies");
		if (proxiesClaim == null) {
			return List.of();
		}
		return proxiesClaim.entrySet().stream().map(e -> new Proxy(e.getKey(), claims.getPreferredUsername(), e.getValue().stream().map(Grant::valueOf).collect(Collectors.toSet()))).toList();
	}

	public Proxy getProxyFor(String username) {
		return proxies.getOrDefault(username, new Proxy(username, getName(), Set.of()));
	}
}
```

Then extends `OAuthentication<ProxiesClaimSet>` to:
- override `getName()`  to return preferred_username instead of subject
- add `hasName(String preferredUsername)` to check if current username is provided one
- add `Proxy getProxyFor(String proxiedUsername)` to ease access to a given proxy from ProxiesClaimSet:
```java
package com.c4_soft.user_proxies.api.security;

import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;

import com.c4_soft.springaddons.security.oauth2.OAuthentication;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProxiesAuthentication extends OAuthentication<ProxiesClaimSet> {
	private static final long serialVersionUID = 6856299734098317908L;

	public ProxiesAuthentication(ProxiesClaimSet claims, Collection<? extends GrantedAuthority> authorities, String bearerString) {
		super(claims, authorities, bearerString);
	}
	
	@Override
	public String getName() {
		return getClaims().getPreferredUsername();
	}
	
	public boolean hasName(String preferredUsername) {
		return Objects.equals(getName(), preferredUsername);
	}

	public Proxy getProxyFor(String proxiedUsername) {
		return this.getClaims().getProxyFor(proxiedUsername);
	}
}
```

Update WebSecurityConfig to:
- provide a `claimsConverter` bean returning `ProxiesClaimSet` instead of `OpenidClaimSet`
- provide an `authenticationBuilder` returning `ProxiesAuthentication` instances instead of default `OAuthentication<OpenidClaimSet>`
- build a `C4MethodSecurityExpressionHandler` bean with a `ProxiesMethodSecurityExpressionRoot` provider. This specialised `C4MethodSecurityExpressionRoot` should expose:
  * `boolean is(String preferredUsername)` to check if current user has given username
  * `Proxy onBehalfOf(String proxiedUsername)` to access the proxy current user has to act on behalf given other user
  * `boolean isNice()` to check if current user is granted any of a fixed authorities set

Last, define a `ProxiesMethodSecurityExpressionRoot` bean to add proxies DSL to security SpEL:
```java
package com.c4_soft.user_proxies.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.c4_soft.springaddons.security.oauth2.config.OAuth2AuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.config.OAuth2ClaimsConverter;
import com.c4_soft.springaddons.security.oauth2.config.synchronised.OAuth2AuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.spring.C4MethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
	
	@Bean
	OAuth2ClaimsConverter<ProxiesClaimSet> claimsConverter() {
		return ProxiesClaimSet::new;
	}

    @Bean
    OAuth2AuthenticationBuilder<ProxiesAuthentication> authenticationBuilder(
    		OAuth2ClaimsConverter<ProxiesClaimSet> claimsConverter,
    		OAuth2AuthoritiesConverter authoritiesConverter) {
        return (bearerString, claims) -> {
        	final var claimSet = claimsConverter.convert(claims);
        	return new ProxiesAuthentication(claimSet, authoritiesConverter.convert(claims), bearerString);
    	};
    }

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new C4MethodSecurityExpressionHandler(ProxiesMethodSecurityExpressionRoot::new);
    }
    
    static class ProxiesMethodSecurityExpressionRoot extends C4MethodSecurityExpressionRoot {
	
		public boolean is(String preferredUsername) {
			return Objects.equals(getAuthentication().getName(), preferredUsername);
		}
	
		public Proxy onBehalfOf(String proxiedUsername) {
			return get(ProxiesAuthentication.class).map(a -> a.getProxyFor(proxiedUsername))
					.orElse(new Proxy(proxiedUsername, getAuthentication().getName(), Set.of()));
		}
	
		public boolean isNice() {
			return hasAnyAuthority("ROLE_NICE_GUY", "SUPER_COOL");
		}
	}
}
```

## `security-test` module
This module will hold an annotation to populate tests security context with the `ProxiesAuthentication` defined in `security` module.

```java
package com.c4_soft.user_proxies.api.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;

import com.c4_soft.springaddons.security.oauth2.test.annotations.AbstractAnnotatedAuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.user_proxies.api.web.dto.Grant;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = ProxiesId.AuthenticationFactory.class)
public @interface ProxiesId {

	@AliasFor("authorities")
	String[] value() default {};

	@AliasFor("value")
	String[] authorities() default {};

	OpenIdClaims claims() default @OpenIdClaims();

	String bearerString() default "machin.truc.chose";

	Proxy[] proxies() default {};

	@AliasFor(annotation = WithSecurityContext.class)
	TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Proxy {

		String onBehalfOf();

		Grant[] can() default { Grant.PROFILE_READ };
	}

	public static final class AuthenticationFactory
			extends AbstractAnnotatedAuthenticationBuilder<ProxiesId, ProxiesAuthentication> {
		@Override
		public ProxiesAuthentication authentication(ProxiesId annotation) {
			final var claims = super.claims(annotation.claims());
			@SuppressWarnings("unchecked")
			final var proxiesclaim = Optional.ofNullable((Map<String, List<String>>) claims.get("proxies"))
					.orElse(new HashMap<>());
			for (final var p : annotation.proxies()) {
				proxiesclaim.put(p.onBehalfOf(), Stream.of(p.can()).map(Grant::toString).toList());
			}
			claims.put("proxies", proxiesclaim);

			return new ProxiesAuthentication(new ProxiesClaimSet(claims), super.authorities(annotation.authorities()), annotation.bearerString());
		}
	}
}
```
We'll also define a meta annotation to import spring-addons auto-configuration plus our custom WebSecurityConfig:
```java
package com.c4_soft.user_proxies.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.c4_soft.springaddons.security.oauth2.test.webmvc.jwt.AutoConfigureAddonsSecurityWebmvcJwt;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureAddonsSecurityWebmvcJwt
@Import(WebSecurityConfig.class)
public @interface WithSecurity {
}
```

## DTOs module
Delete `SampleResponseDto` and `SampleResponseDto`. 

Only Grant enum and `ProxyDto` are needed in this lib (others are not shared between modules and will be defined in each micro-service)
```java
package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyDto implements Serializable {
	private static final long serialVersionUID = 7382907575214377114L;

	@NotNull
	private Long id;

	@NotEmpty
	@NotNull
	private String grantingUsername;

	@NotEmpty
	@NotNull
	private String grantedUsername;

	@NotNull
	private List<Grant> grants;

	@NotNull
	private Long start;

	private Long end;
}
```

## Exceptions module
We'll use one additional exception:
```java
package com.c4_soft.user_proxies.api.exceptions;

public class ProxyUsersUnmodifiableException extends RuntimeException {
	private static final long serialVersionUID = 4419101891617503927L;

	public ProxyUsersUnmodifiableException() {
		super("Proxy users cannot be modified");
	}
}
```

And add this handling to CustomExceptionHandler:
```java
	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	@ExceptionHandler(ProxyUsersUnmodifiableException.class)
	protected void handleProxyUsersUnmodifiable(ProxyUsersUnmodifiableException ex, WebRequest request) {
		logger.info(ex.getMessage());
	}
```

## user-proxies-api
This module is a good starting point for a servlet spring-boot resource-server with JPA persistence and OpenID security.
Lets adapt it by steps.

First 
- rename spring-boot app main class from `SampleApi` to `UserProxiesApi`
- remove `WebSecurityConfig` from `UserProxiesApi` (it is now defined in `security` module)

### domain entities
- rename `SampleEntity` to `User`
- create a `Proxy` entity along to `User`

```java
package com.c4_soft.user_proxies.api.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "UK_USER_SUBJECT", columnNames = "subject"),
		@UniqueConstraint(name = "UK_USER_EMAIL", columnNames = "email"),
		@UniqueConstraint(name = "UK_USER_USERNAME", columnNames = "username") })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String subject;

	@Column(nullable = false)
	@Email
	private String email;

	@Column(name = "username", nullable = false)
	private String preferredUsername;

	@OneToMany(mappedBy = "grantedUser", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Proxy> grantedProxies = new ArrayList<>();

	@OneToMany(mappedBy = "grantingUser", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Proxy> grantingProxies = new ArrayList<>();

	public User(String subject, String email, String preferredUsername) {
		this.subject = subject;
		this.email = email;
		this.preferredUsername = preferredUsername;
	}
}
```
```java
package com.c4_soft.user_proxies.api.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.c4_soft.user_proxies.api.web.dto.Grant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_proxies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proxy {
	@Id
	@GeneratedValue
	private Long id;

	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "granting_user_id", updatable = false, nullable = false)
	private User grantingUser;

	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "granted_user_id", updatable = false, nullable = false)
	private User grantedUser;

	@NotNull
	@ElementCollection(fetch = FetchType.EAGER)
	@Default
	private List<Grant> grants = new ArrayList<>();

	@NotNull
	@Column(name = "start_date", nullable = false, updatable = true)
	private Date start;

	@Column(name = "end_date", nullable = true, updatable = true)
	private Date end;

	public void allow(Grant grant) {
		if (!grants.contains(grant)) {
			grants.add(grant);
		}
	}

	public void deny(Grant grant) {
		grants.remove(grant);
	}
	
	public boolean isActive() {
		final var now = new Date();
		return start.before(now) && (end == null || end.after(now));
	}
}
```
So, yes, I just promoted low coupling from domain classes to JPA entities (and I'm fine with that).

### JPA persistence layer
- rename `SampleEntityRepository` to `UserRepository`
- create a `ProxyRepository`

```java
package com.c4_soft.user_proxies.api.jpa;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.domain.User_;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findByPreferredUsername(String preferredUsername);

	static Specification<User> searchSpec(String emailOrPreferredUsername) {
		if (!StringUtils.hasText(emailOrPreferredUsername)) {
			return null;
		}

		return Specification
				.where(attibuteLikeIgnoreCase(User_.EMAIL, emailOrPreferredUsername))
				.or(attibuteLikeIgnoreCase(User_.PREFERRED_USERNAME, emailOrPreferredUsername));
	}

	private static Specification<User> attibuteLikeIgnoreCase(String attributeName, String needle) {
		return (root, query, criteriaBuilder) -> criteriaBuilder
				.like(criteriaBuilder.lower(root.get(attributeName)), String.format("%%%s%%", needle.trim().toLowerCase()));
	}
}
```
```java
package com.c4_soft.user_proxies.api.jpa;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.domain.Proxy_;
import com.c4_soft.user_proxies.api.domain.User_;

public interface ProxyRepository extends JpaRepository<Proxy, Long>, JpaSpecificationExecutor<Proxy> {
	static Specification<Proxy> searchSpec(Optional<String> grantingUsername, Optional<String> grantedUsername, Optional<Date> date) {
		final var specs =
				Stream
						.of(
								Optional.of(endsAfter(date.orElse(new Date()))),
								grantingUsername.map(ProxyRepository::grantingUserPreferredUsernameLike),
								grantedUsername.map(ProxyRepository::grantedUserPreferredUsernameLike),
								date.map(ProxyRepository::startsBefore))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.toList();
		var spec = Specification.where(specs.get(0));
		for (var i = 1; i < specs.size(); ++i) {
			spec = spec.and(specs.get(i));
		}
		return spec;
	}

	static Specification<Proxy> endsAfter(Date date) {
		return (root, query, cb) -> cb.or(cb.isNull(root.get(Proxy_.end)), cb.greaterThanOrEqualTo(root.get(Proxy_.end), date));
	}

	static Specification<Proxy> grantingUserPreferredUsernameLike(String grantingUsername) {
		return (root, query, cb) -> cb.like(root.get(Proxy_.grantingUser).get(User_.preferredUsername), grantingUsername);
	}

	static Specification<Proxy> grantedUserPreferredUsernameLike(String grantedUsername) {
		return (root, query, cb) -> cb.like(root.get(Proxy_.grantedUser).get(User_.preferredUsername), grantedUsername);
	}

	static Specification<Proxy> startsBefore(Date date) {
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(Proxy_.start), date);
	}
}
```

### web layer
#### DTOs
```java
package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyEditDto implements Serializable {
	private static final long serialVersionUID = 7381717131881105091L;

	@NotNull
	private List<Grant> grants;

	@NotNull
	private Long start;

	private Long end;
}
```
```java
package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateDto implements Serializable {
	private static final long serialVersionUID = 1963318450007215498L;

	@NotNull
	private String subject;

	@NotNull
	@Email
	private String email;

	@NotNull
	private String preferredUsername;

}
```
```java
package com.c4_soft.user_proxies.api.web.dto;

import java.io.Serializable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
	private static final long serialVersionUID = -3684504868042067509L;

	@NotNull
	private Long id;

	@NotNull
	private String subject;

	@NotNull
	@Email
	private String email;

	@NotNull
	private String preferredUsername;

}
```

### Domain entities <=> DTOs with mapstruct mappers
We need two mappers to turn domain entities into DTOs. We'll use mapstruct for that:
```java
package com.c4_soft.user_proxies.api.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.web.dto.UserCreateDto;
import com.c4_soft.user_proxies.api.web.dto.UserDto;

@Mapper(componentModel = ComponentModel.SPRING)
public interface UserMapper {

	UserDto toDto(User domain);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "grantingProxies", ignore = true)
	@Mapping(target = "grantedProxies", ignore = true)
	void update(@MappingTarget User domain, UserCreateDto dto);

}
```
```java
package com.c4_soft.user_proxies.api.web;

import java.util.Date;
import java.util.Optional;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.web.dto.ProxyDto;
import com.c4_soft.user_proxies.api.web.dto.ProxyEditDto;

@Mapper(componentModel = ComponentModel.SPRING)
public interface UserProxyMapper {

	@Mapping(target = "grantingUsername", source = "grantingUser.preferredUsername")
	@Mapping(target = "grantedUsername", source = "grantedUser.preferredUsername")
	ProxyDto toDto(Proxy domain);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "grantingUser", ignore = true)
	@Mapping(target = "grantedUser", ignore = true)
	void update(@MappingTarget Proxy domain, ProxyEditDto dto);

	default Date toDate(Long epoch) {
		return Optional.ofNullable(epoch).map(Date::new).orElse(null);
	}

	default Long toEpoch(Date date) {
		return Optional.ofNullable(date).map(Date::getTime).orElse(null);
	}
}
```

### `@RestController`
We'll expose a @RestController for users and user-proxies CRUD operations:
```java
package com.c4_soft.user_proxies.api.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

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
		return ResponseEntity.created(URI.create(userRepo.save(user).getPreferredUsername())).build();
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
```

### application.properties
```
server.port=9443
server.shutdown=graceful

spring.datasource.url=jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.hbm2ddl.charset_name=UTF-8

com.c4-soft.springaddons.security.issuers[0].location=https://localhost:8443/realms/master
com.c4-soft.springaddons.security.issuers[0].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.issuers[1].location=https://mc-ch4mp.local:8443/realms/master
com.c4-soft.springaddons.security.issuers[1].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.issuers[2].location=https://bravo-ch4mp:8443/realms/master
com.c4-soft.springaddons.security.issuers[2].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.cors[0].path=/users/**
com.c4-soft.springaddons.security.permit-all=/actuator/health/readiness,/actuator/health/liveness,/v3/api-docs/**

management.endpoint.health.probes.enabled=true
management.health.readinessstate.enabled=true
management.health.livenessstate.enabled=true
management.endpoints.web.exposure.include=*
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Unit tests
Controllers will use auto-mapping from proxies IDs to Proxy instances. Unfortunately, this is not supported by @WebMvcTest out of the box and requires additional conf:
```java
package com.c4_soft.user_proxies.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;

/**
 * Avoid MethodArgumentConversionNotSupportedException with repos MockBean
 *
 * @author Jérôme Wacongne &lt;ch4mp#64;c4-soft.com&gt;
 */
@TestConfiguration
public class EnableSpringDataWebSupportTestConf {
	@Bean
	WebMvcConfigurer configurer(ProxyRepository sampleRepo) {
		return new WebMvcConfigurer() {
			@Override
			public void addFormatters(FormatterRegistry registry) {
				registry.addConverter(String.class, Proxy.class, id -> sampleRepo.findById(Long.valueOf(id)).get());
			}
		};
	}
}
```

We'll also need a few User and Proxy fixtures:
```java
package com.c4_soft.user_proxies.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;
import com.c4_soft.user_proxies.api.web.dto.Grant;

public class ProxyFixtures {

	private static Proxy userAToUserB() {
		return new Proxy(
				2L,
				UserFixtures.userA(),
				UserFixtures.userB(),
				List.of(Grant.values()),
				Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
				Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
	}

	private static Proxy userBToUserA() {
		return new Proxy(2L, UserFixtures.userB(), UserFixtures.userA(), List.of(Grant.PROFILE_READ), Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), null);
	}

	public static Map<String, List<Proxy>> all() {
		final var proxiesByGrantingUser = Stream.of(userAToUserB(), userBToUserA()).collect(Collectors.groupingBy(p -> p.getGrantingUser().getPreferredUsername()));
		for (final var proxiesUserProxies : proxiesByGrantingUser.entrySet()) {
			for (final var proxy : proxiesUserProxies.getValue()) {
				proxy.getGrantedUser().getGrantedProxies().add(proxy);
				proxy.getGrantingUser().getGrantingProxies().add(proxy);
			}
		}
		return proxiesByGrantingUser;
	}

	public static Map<String, List<Proxy>> setUp(ProxyRepository proxyRepository) {
		final var proxies = all();
		when(proxyRepository.findById(anyLong()))
				.thenAnswer(
						invocation -> proxies
								.entrySet()
								.stream()
								.map(Entry::getValue)
								.map(List::stream)
								.flatMap(p -> p)
								.filter(p -> p.getId().equals(invocation.getArgument(0)))
								.findAny());
		return proxies;
	}
}
```
```java
package com.c4_soft.user_proxies.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.jpa.UserRepository;

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
		return Stream.of(admin(), userA(), userB()).collect(Collectors.toMap(User::getPreferredUsername, u -> u));
	}

	public static Map<String, User> setUp(UserRepository userRepository) {
		final var users = UserFixtures.all();
		when(userRepository.findByPreferredUsername(anyString())).thenAnswer(invocation -> Optional.ofNullable(users.get(invocation.getArgument(0))));
		return users;
	}
}
```

And now, actual controller test:
```java
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
import com.c4_soft.user_proxies.api.EnableSpringDataWebSupportTestConf;
import com.c4_soft.user_proxies.api.ProxyFixtures;
import com.c4_soft.user_proxies.api.UserFixtures;
import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;
import com.c4_soft.user_proxies.api.jpa.UserRepository;
import com.c4_soft.user_proxies.api.security.WithSecurity;
import com.c4_soft.user_proxies.api.security.ProxiesId;
import com.c4_soft.user_proxies.api.security.ProxiesId.Proxy;
import com.c4_soft.user_proxies.api.web.dto.Grant;

@WebMvcTest(controllers = { UserController.class })
@WithSecurity
@Import({ EnableSpringDataWebSupportTestConf.class, UserMapperImpl.class, UserProxyMapperImpl.class })
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

	Map<String, List<com.c4_soft.user_proxies.api.domain.Proxy>> proxies;

	@BeforeEach
	public void setUp() {
		users = UserFixtures.setUp(userRepository);
		proxies = ProxyFixtures.setUp(proxyRepository);
	}

	// @formatter:off
	// Test access to UserController::retrieveGrantedProxies which is secured with:
	/** is(#username) or hasAnyAuthority('AUTHORIZATION_SERVER', 'USERS_ADMIN') or onBehalfOf(#username).can('READ_PROXIES') */
	// @formatter:on
	@Test
	void whenAnonymousThenUnauthorized() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isUnauthorized());
	}

	@Test
	@ProxiesId()
	void whenAuthenticatedWithoutRequiredAuthoritiesNorProxiesThenForbidden() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isForbidden());
	}

	@Test
	@ProxiesId(authorities = "TOKEN_ISSUER")
	void whenAuthenticatedAsAuthorizationServerThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = "USERS_ADMIN")
	void whenAuthenticatedAsAdminThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = {}, claims = @OpenIdClaims(preferredUsername = "machin"))
	void whenAuthenticatedAsProxiedUserThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = {}, claims = @OpenIdClaims(preferredUsername = "truc"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROXIES_READ }) })
	void whenGrantedWithEditProxiesForProxiedUserThenCanGetUserProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = {}, claims = @OpenIdClaims(preferredUsername = "truc"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROFILE_READ }), // right granting user but wrong grant
			@Proxy(onBehalfOf = "bidule", can = { Grant.PROFILE_READ, Grant.PROXIES_READ }) }) // right grant but wrong granting user
	void whenNotGrantedWithEditProxiesThenForbidden() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granted", "machin").andExpect(status().isForbidden());
	}

	// @formatter:off
	// Test access to UserController::retrieveGrantingProxies which is secured with:
	/** is(#username) or hasAnyAuthority('USERS_ADMIN') or onBehalfOf(#username).can('READ_PROXIES') */
	// @formatter:on
	@Test
	void whenAnonymousThenUnauthorizedToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isUnauthorized());
	}

	@Test
	@ProxiesId()
	void whenAuthenticatedWithoutRequiredAuthoritiesNorProxiesThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

	@Test
	@ProxiesId(authorities = "AUTHORIZATION_SERVER")
	void whenAuthenticatedAsAuthorizationServerThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isForbidden());
	}

	@Test
	@ProxiesId(authorities = "USERS_ADMIN")
	void whenAuthenticatedAsAdminThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = {}, claims = @OpenIdClaims(preferredUsername = "machin"))
	void whenAuthenticatedAsProxiedUserThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = {}, claims = @OpenIdClaims(preferredUsername = "truc"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROFILE_READ, Grant.PROXIES_READ }) })
	void whenGrantedWithEditProxiesForProxiedUserThenCanGetUserProxiesToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isOk());
	}

	@Test
	@ProxiesId(authorities = {}, claims = @OpenIdClaims(preferredUsername = "truc"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROFILE_READ }), // right granting user but wrong grant
			@Proxy(onBehalfOf = "bidule", can = { Grant.PROFILE_READ, Grant.PROXIES_READ }) }) // right grant but wrong granting user
	void whenNotGrantedWithEditProxiesThenForbiddenToGrantingProxies() throws Exception {
		mockMvc.get("/users/{grantingUsername}/proxies/granting", "machin").andExpect(status().isForbidden());
	}
}
```

Test application.properties:
```
com.c4-soft.springaddons.test.web.default-media-type=application/json
com.c4-soft.springaddons.test.web.default-charset=utf-8
```

## Keycloak mapper
We actually have two concerns here:
- retrieve proxies from user-proxies-api (consume a web-service)
- add retrieved proxies to Keycloak tokens

### Keycloak mapper project structure
Two resources are required:
- `src/main/resources/META-INF/jboss-deployment-structure.xml`
```xml
<jboss-deployment-structure>
    <deployment>
        <dependencies>
            <module name="org.keycloak.keycloak-services" />
        </dependencies>
    </deployment>
</jboss-deployment-structure>
```
- `src/main/resources/META-INF/services/org.keycloak.protocol.ProtocolMapper`
```
com.c4_soft.user_proxies.api.keycloak.ProxiesMapper
```

Here is pom with 
- required Keycloak dependencies 
- spring's WebClient
- maven shade plugin
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.c4-soft.user-proxies</groupId>
		<artifactId>api</artifactId>
		<version>1.0.0-SNAPSHOT</version>
		<relativePath>..</relativePath>
	</parent>
    <groupId>com.c4-soft.user-proxies.api</groupId>
	<artifactId>proxies-keycloak-mapper</artifactId>
	<packaging>jar</packaging>
	<name>proxies-keycloak-mapper</name>
	<description>Keycloak mapper to add "proxies" private claim to tokens</description>

	<dependencies>
		<dependency>
            <groupId>com.c4-soft.user-proxies.api</groupId>
            <artifactId>dtos</artifactId>
		</dependency>

		<!-- provided keycloak dependencies -->
		<dependency>
			<groupId>org.keycloak</groupId>
			<artifactId>keycloak-server-spi</artifactId>
			<version>${keycloak.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>org.keycloak</groupId>
			<artifactId>keycloak-server-spi-private</artifactId>
			<version>${keycloak.version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>org.keycloak</groupId>
			<artifactId>keycloak-services</artifactId>
			<version>${keycloak.version}</version>
			<scope>provided</scope>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webflux</artifactId>
			<exclusions>
				<exclusion>
					<groupId>ch.qos.logback</groupId>
					<artifactId>logback-classic</artifactId>
				</exclusion>
			</exclusions>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<executions>
					<!-- Run shade goal on package phase -->
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>
```

### `user-proxies-api` web-client
We'll start with a web-client with no dependency on Keycloak, just in case we'd like to switch authorization-server later on:
```java
package com.c4_soft.user_proxies.api.keycloak;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.c4_soft.user_proxies.api.web.dto.ProxyDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProxiesClient {

	private static final Map<UserProxiesClientConfig, UserProxiesClient> instances = new HashMap<>();

	private final UserProxiesClientConfig config;
	private final WebClient tokenWebClient;
	private final WebClient userProxiesWebClient;
	private long expiresAt = 0L;
	private Optional<TokenResponseDto> token = Optional.empty();

	private UserProxiesClient(UserProxiesClientConfig config) {
		this.config = config;
		this.tokenWebClient = WebClient.builder().baseUrl(config.getAuthorizationUri()).build();
		this.userProxiesWebClient = WebClient.builder().baseUrl(config.getUserProxiesBaseUri()).build();
	}

	public Map<String, List<String>> getPermissionsByProxiedUsernameFor(String tenantPreferredUsername) {
		try {
			final Optional<ProxyDto[]> dtos = Optional
					.ofNullable(userProxiesWebClient.get().uri("/{username}/proxies/granted", tenantPreferredUsername)
							.headers(this::setBearer).retrieve().bodyToMono(ProxyDto[].class).block());
			dtos.ifPresent(d -> log.debug("Got proxies {}", Stream.of(d).toList()));
			
			return dtos.map(Stream::of)
					.map(s -> s.collect(Collectors.toMap(ProxyDto::getGrantingUsername, ProxyDto::getGrants)))
					.orElse(Map.of());
		} catch (final Exception e) {
			log.error("Failed to fetch user proxies: {}", e);
			return Map.of();
		}
	}

	private HttpHeaders setBearer(HttpHeaders headers) {
		getClientAccessToken().ifPresent(str -> {
			headers.setBearerAuth(str);
		});
		return headers;
	}

	private Optional<String> getClientAccessToken() {
		final var now = new Date().getTime();
		if (expiresAt < now) {
			try {
				log.debug("Get client access token with {}", config.getUsername());
				token = Optional.ofNullable(tokenWebClient.post().headers(headers -> {
					headers.setBasicAuth(config.getUsername(), config.getPassword());
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}).body(BodyInserters.fromFormData("scope", "openid profile").with("grant_type", "client_credentials"))
						.retrieve().bodyToMono(TokenResponseDto.class).block());
				expiresAt = now + 1000L * token.map(TokenResponseDto::getExpiresIn).orElse(0L);
			} catch (final Exception e) {
				log.error("Failed to get client authorization-token: {}", e);
				return Optional.empty();
			}
		}
		return token.map(TokenResponseDto::getAccessToken);
	}

	public static UserProxiesClient getInstance(UserProxiesClientConfig config) {
		return instances.computeIfAbsent(config, c -> {
			log.info("Building UserProxiesClient with {}", c);
			return new UserProxiesClient(c);
		});
	}
}
```

With this configuration class
```java
package com.c4_soft.user_proxies.api.keycloak;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserProxiesClientConfig implements Serializable {
	private static final long serialVersionUID = 5417717966238971990L;

	private final String authorizationUri;
	private final String username;
	private final String password;
	private final String userProxiesBaseUri;
}
```

And a DTO for OpenID token response:
```java
package com.c4_soft.user_proxies.api.keycloak;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@XmlRootElement
@Data
public class TokenResponseDto implements Serializable {
	private static final long serialVersionUID = 4995510591526512450L;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private Long expiresIn;

	@JsonProperty("refresh_expires_in")
	private Long refreshExpiresIn;

	@JsonProperty("token_type")
	private String tokenType;

	@JsonProperty("id_token")
	private String idToken;

	@JsonProperty("not-before-policy")
	private Long notBeforePolicy;

	private String scope;
}
```

### Keycloak mapper to add private-claim to tokens
```java
package com.c4_soft.user_proxies.api.keycloak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxiesMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
	private static final String AUTHORIZATION_URI = "proxies-service.authorization-uri";
	private static final String PROXIES_SERVICE_CLIENT_SECRET = "proxies-service.client-secret";
	private static final String PROXIES_SERVICE_CLIENT_NAME = "proxies-service.client-name";
	private static final String PROVIDER_ID = "c4-soft.com";
	private static final String PROXIES_SERVICE_BASE_URI = "proxies-service.users-endpoint-uri";

	private final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	public ProxiesMapper() {
		ProviderConfigProperty property;

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_BASE_URI);
		property.setLabel("Proxies service base URI");
		property.setHelpText("Base URI for REST service to fetch proxies from");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("https://localhost:8443/users");
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_CLIENT_NAME);
		property.setLabel("Proxies mapper client-name");
		property.setHelpText("Proxies mapper client-name");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("user-proxies-mapper");
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(PROXIES_SERVICE_CLIENT_SECRET);
		property.setLabel("Proxies mapper client-secret");
		property.setHelpText("Proxies mapper client-secret");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		configProperties.add(property);

		property = new ProviderConfigProperty();
		property.setName(AUTHORIZATION_URI);
		property.setLabel("Authorization endpoint");
		property.setHelpText("Token end-point for authorizing proxies mapper");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setDefaultValue("https://localhost:9443/auth/realms/master/protocol/openid-connect/token");
		configProperties.add(property);
	}

	@Override
	public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession keycloakSession,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel,
			KeycloakSession keycloakSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		return transform(token, mappingModel, keycloakSession, userSession, clientSessionCtx);
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getDisplayType() {
		return "User proxies mapper";
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getHelpText() {
		return "Adds a \"proxies\" private claim containing a map of permissions the \"tenant\" (current user) has to act on behalf of \"proxied\" users (one collection of permissions per proxied user preferred_username)";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	private <T extends IDToken> T transform(T token, ProtocolMapperModel mappingModel, KeycloakSession keycloakSession,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		final var clientConfig = UserProxiesClientConfig.builder()
				.authorizationUri(mappingModel.getConfig().get(AUTHORIZATION_URI))
				.username(mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_NAME))
				.password(mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_SECRET))
				.userProxiesBaseUri(mappingModel.getConfig().get(PROXIES_SERVICE_BASE_URI)).build();
		final var who = Optional.ofNullable(userSession.getUser().getUsername()).orElse("");
		if (who == null || who.length() == 0) {
			log.warn("Empty username for user subject {}", token.getSubject());
		} else {
			log.debug("Call UserProxiesClient for {}", who);
			final var proxies = UserProxiesClient.getInstance(clientConfig).getPermissionsByProxiedUsernameFor(who);
			token.getOtherClaims().put("proxies", proxies);
			setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);
		}
		return token;

	}
}
```

Now, you can 
- `mvn package` 
- copy shaded jar to following keycloak folder, depending on Keycloak version:
  * < 18:  `standalone/deployments/` (should auto deploy, even if Keycloak already started)
  * >= 18: `providers/` (needs a restart)
- open `Clients` > `user-proxies-client` > `Mappers` > `Create` in Keycloak admin console to configure the now available `User proxies mapper`. Required secret is available from `Clients` > `user-proxies-mapper` > `Credentials`

## Other resource-server sample
To demo that proxies security SpEL can be used also in modules that do not have access to User and proxy entities (sololy the JWT claims are used), we'll create a new `greet` module:

### Controller
```java
package com.c4_soft.user_proxies.api.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GreetDto {

	private String message;
	
}
```
``` java
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
	@PreAuthorize("hasAuthority('NICE_GUY')")
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
```

### spring-boot main class
``` java
package com.c4_soft.user_proxies.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GreetApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GreetApiApplication.class, args);
	}

}
```
As security config is defined in a sub-package of main class, it will be auto-scanned.

### application.properties
```
server.port=9445
server.shutdown=graceful

com.c4-soft.springaddons.security.issuers[0].location=https://localhost:8443/realms/master
com.c4-soft.springaddons.security.issuers[0].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.issuers[1].location=https://mc-ch4mp.local:8443/realms/master
com.c4-soft.springaddons.security.issuers[1].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.issuers[2].location=https://bravo-ch4mp:8443/realms/master
com.c4-soft.springaddons.security.issuers[2].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.cors[0].path=/greet/**
com.c4-soft.springaddons.security.permit-all=/actuator/health/readiness,/actuator/health/liveness,/v3/api-docs/**

management.endpoint.health.probes.enabled=true
management.health.readinessstate.enabled=true
management.health.livenessstate.enabled=true
management.endpoints.web.exposure.include=*
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Unit tests
``` java
@WebMvcTest(GreetController.class)
@WithSecurity
class GreetControllerTest {

	@Autowired
	MockMvcSupport mockMvc;

	@Test
	@ProxiesId(authorities = { "NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {
			@Proxy(onBehalfOf = "machin", can = { Grant.PROFILE_READ }),
			@Proxy(onBehalfOf = "chose") })
	void testGreet() throws Exception {
		mockMvc
				.get("/greet")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message", is("Hi Tonton Pirate! You are granted with: [NICE_GUY, AUTHOR] and can proxy: [chose, machin].")));
	}

	@Test
	@ProxiesId(authorities = { "ROLE_NICE_GUY", "AUTHOR" }, claims = @OpenIdClaims(preferredUsername = "Tonton Pirate"), proxies = {})
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
```
With same test properties as user-proxies-api:
```properties
com.c4-soft.springaddons.test.web.default-media-type=application/json
com.c4-soft.springaddons.test.web.default-charset=utf-8
```

## Generate OpenAPI spec files

- Edit `user-proxies-api` and `greet-api` pom files to update integration-test.port with what is defined in `application.properties` files : respectively `<integration-tests.port>9443</integration-tests.port>` and `<integration-tests.port>9445</integration-tests.port>`
- run `mvn clean install -Popenapi`
