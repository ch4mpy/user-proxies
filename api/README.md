# Keycloak mapper & RESTful API

We'll build a maven project with a few modules:
- a spring-boot resource-server to expose and manage user proxies stored with JPA
- a keycloak mapper which will retrieve user proxies from preceding web-service and add it to access & ID tokens
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
  -DarchetypeVersion=4.3.5 \
  -DgroupId=com.c4-soft.user-proxies \
  -DartifactId=api \
  -Dversion=1.0.0-SNAPSHOT \
  -Dapi-artifactId=user-proxies-api \
  -Dapi-path=user-proxies
```

## additional modules
In parent pom, add `security`, `security-test`, `keycloak-mapper` and `greet-api` modules with dependency management for the first two:
```xml
	<modules>
		<module>dtos</module>
		<module>exceptions</module>
		<module>security</module>
		<module>security-test</module>
		<module>user-proxies-api</module>
		<module>keycloak-mapper</module>
		<module>greet-api</module>
	</modules>
...
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>com.c4-soft.user-proxies.api</groupId>
				<artifactId>security</artifactId>
				<version>1.0.0-SNAPSHOT</version>
			</dependency>
			<dependency>
				<groupId>com.c4-soft.user-proxies.api</groupId>
				<artifactId>security-test</artifactId>
				<version>1.0.0-SNAPSHOT</version>
			</dependency>
...
```

Here are the poms for those modules:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.c4-soft.user-proxies</groupId>
		<artifactId>api</artifactId>
		<version>1.0.0-SNAPSHOT</version>
		<relativePath>..</relativePath>
	</parent>
	<groupId>com.c4-soft.user-proxies.api</groupId>
	<artifactId>security</artifactId>
	<description>Shared security configuration</description>

	<dependencies>
		<dependency>
			<groupId>com.c4-soft.springaddons</groupId>
			<artifactId>spring-security-oauth2-webmvc-addons</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-config</artifactId>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
	</dependencies>

</project>
```
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.c4-soft.user-proxies</groupId>
		<artifactId>api</artifactId>
		<version>1.0.0-SNAPSHOT</version>
		<relativePath>..</relativePath>
	</parent>
	<groupId>com.c4-soft.user-proxies.api</groupId>
	<artifactId>security-test</artifactId>
	<description>Shared annotation to populate test security-context</description>

	<dependencies>
		<dependency>
			<groupId>com.c4-soft.user-proxies.api</groupId>
			<artifactId>security</artifactId>
		</dependency>
		<dependency>
			<groupId>com.c4-soft.springaddons</groupId>
			<artifactId>spring-security-oauth2-test-webmvc-addons</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
	</dependencies>
</project>
```
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

	<properties>
		<keycloak.version>18.0.0</keycloak.version>
	</properties>

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
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.c4-soft.user-proxies</groupId>
		<artifactId>api</artifactId>
		<version>1.0.0-SNAPSHOT</version>
		<relativePath>..</relativePath>
	</parent>
	<groupId>com.c4-soft.user-proxies.api</groupId>
	<artifactId>greet-api</artifactId>

	<properties>
		<ca-certificates.binding>${project.parent.basedir}/bindings/ca-certificates</ca-certificates.binding>
	</properties>

	<dependencies>
		<dependency>
            <groupId>com.c4-soft.user-proxies.api</groupId>
			<artifactId>exceptions</artifactId>
		</dependency>
		<dependency>
            <groupId>com.c4-soft.user-proxies.api</groupId>
            <artifactId>security</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		
		<dependency>
			<groupId>io.swagger.core.v3</groupId>
			<artifactId>swagger-annotations</artifactId>
			<scope>provided</scope>
		</dependency>

		<dependency>
			<groupId>org.mapstruct</groupId>
			<artifactId>mapstruct</artifactId>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok-mapstruct-binding</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-native</artifactId>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<dependency>
            <groupId>com.c4-soft.user-proxies.api</groupId>
            <artifactId>security-test</artifactId>
            <scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

	<profiles>
		<profile>
			<id>build-native-image</id>
			<build>
				<plugins>
					<plugin>
						<groupId>org.springframework.experimental</groupId>
						<artifactId>spring-aot-maven-plugin</artifactId>
					</plugin>
					<plugin>
						<groupId>org.hibernate.orm.tooling</groupId>
						<artifactId>hibernate-enhance-maven-plugin</artifactId>
					</plugin>
				</plugins>
			</build>
		</profile>
		<profile>
			<id>openapi</id>
			<properties>
				<integration-tests.port>8444</integration-tests.port>
			</properties>
			<dependencies>
				<dependency>
					<groupId>org.springdoc</groupId>
					<artifactId>springdoc-openapi-ui</artifactId>
				</dependency>
			</dependencies>
			<build>
				<plugins>
					<plugin>
						<groupId>org.springdoc</groupId>
						<artifactId>springdoc-openapi-maven-plugin</artifactId>
					</plugin>
				</plugins>
			</build>
		</profile>
	</profiles>

</project>
```

Also, in `user-proxies-api` replace dependencies on `spring-security-oauth2-webmvc-addons` and `spring-security-oauth2-test-webmvc-addons` by dependencies on `security` and `security-test` modules.

## `security` module
This module will hold shared security configuration

Define `Proxy` and `ProxiesAuthentication` classes:
```java
package com.c4_soft.user_proxies.api.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class Proxy {
	private final String proxiedSubject;
	private final String tenantSubject;
	private final Set<String> permissions;

	public Proxy(String proxiedSubject, String tenantSubject, Collection<String> permissions) {
		this.proxiedSubject = proxiedSubject;
		this.tenantSubject = tenantSubject;
		this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
	}

	public boolean can(String permission) {
		return permissions.contains(permission);
	}
}
```
```java
package com.c4_soft.user_proxies.api.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

import com.c4_soft.springaddons.security.oauth2.oidc.OidcAuthentication;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProxiesAuthentication extends OidcAuthentication<OidcToken> {
	private static final long serialVersionUID = 6856299734098317908L;

	private final Map<String, Proxy> proxies;

	public ProxiesAuthentication(OidcToken token, Collection<? extends GrantedAuthority> authorities, Map<String, Proxy> proxies, String bearerString) {
		super(token, authorities, bearerString);
		this.proxies = Collections.unmodifiableMap(proxies);
	}

	public Proxy getProxyFor(String proxiedUserSubject) {
		return this.proxies.getOrDefault(proxiedUserSubject, new Proxy(proxiedUserSubject, getToken().getSubject(), List.of()));
	}
}
```

Update WebSecurityConfig to provide an authentication converter returning ProxiesAuthentication instances insteadof default `OidcAuthentication<OidcToken>`.

Last, define a `MethodSecurityExpressionHandler` bean to add proxies DSL to security SpEL

This gives following security-config:
```java
package com.c4_soft.user_proxies.api.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2AuthenticationConverter;
import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2OidcTokenConverter;
import com.c4_soft.springaddons.security.oauth2.config.JwtGrantedAuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;
import com.c4_soft.springaddons.security.oauth2.spring.GenericMethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

	public interface ProxiesConverter extends Converter<Jwt, Map<String, Proxy>> {
	}

	@Bean
	public ProxiesConverter proxiesConverter() {
		return jwt -> {
			@SuppressWarnings("unchecked")
			final var proxiesClaim = (Map<String, List<String>>) jwt.getClaims().get("proxies");
			if (proxiesClaim == null) {
				return Map.of();
			}
			return proxiesClaim.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new Proxy(e.getKey(), jwt.getSubject(), e.getValue())));
		};
	}
	
	@Bean
	public SynchronizedJwt2AuthenticationConverter<ProxiesAuthentication> authenticationConverter(
			JwtGrantedAuthoritiesConverter authoritiesConverter,
			SynchronizedJwt2OidcTokenConverter<OidcToken> tokenConverter,
			ProxiesConverter proxiesConverter) {
		return jwt -> new ProxiesAuthentication(tokenConverter.convert(jwt), authoritiesConverter.convert(jwt), proxiesConverter.convert(jwt), jwt.getTokenValue());
	}
	
	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		return new GenericMethodSecurityExpressionHandler<>(ProxiesMethodSecurityExpressionRoot::new);
	}
	
	final class ProxiesMethodSecurityExpressionRoot extends GenericMethodSecurityExpressionRoot<ProxiesAuthentication> {
		public ProxiesMethodSecurityExpressionRoot() {
			super(ProxiesAuthentication.class);
		}
	
		public Proxy onBehalfOf(String proxiedUserSubject) {
			return getAuth().getProxyFor(proxiedUserSubject);
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;

import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;
import com.c4_soft.springaddons.security.oauth2.test.annotations.AbstractAnnotatedAuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithProxiesAuth.AuthenticationFactory.class)
public @interface WithProxiesAuth {

	@AliasFor("authorities")
	String[] value() default { "ROLE_USER" };

	@AliasFor("value")
	String[] authorities() default { "ROLE_USER" };

	OpenIdClaims claims() default @OpenIdClaims();

	String bearerString() default "machin.truc.chose";

	Grant[] grants() default {};

	@AliasFor(annotation = WithSecurityContext.class)
	TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

	public static final class AuthenticationFactory extends AbstractAnnotatedAuthenticationBuilder<WithProxiesAuth, ProxiesAuthentication> {
		@Override
		public ProxiesAuthentication authentication(WithProxiesAuth annotation) {
			final var claims = super.claims(annotation.claims());
			@SuppressWarnings("unchecked")
			final var proxiesclaim = Optional.ofNullable((Map<String, List<String>>) claims.get("proxies")).orElse(new HashMap<>());
			for (final var p : annotation.grants()) {
				proxiesclaim.put(p.onBehalfOf(), List.of(p.can()));
			}
			claims.put("proxies", proxiesclaim);
			return new ProxiesAuthentication(new OidcToken(claims), super.authorities(annotation.authorities()), annotation.bearerString());
		}
	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Grant {

		String onBehalfOf();

		String[] can() default {};
	}
}
```

## DTOs module
Delete `SampleResponseDto` and `SampleResponseDto`. 

Only `ProxyDto` is needed in this lib (others are not shared between modules and will be defined in each micro-service)
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
	private String grantingUserSubject;

	@NotEmpty
	@NotNull
	private String grantedUserSubject;

	@NotNull
	private List<String> grants;

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
- remove `WebSecurityConfig` from `UserProxiesApi`

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
import javax.validation.constraints.Email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String subject;

	@Column(nullable = false, unique = true)
	@Email
	private String email;

	@Column(nullable = false, unique = true)
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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_proxies", uniqueConstraints = {
		@UniqueConstraint(name = "UniqueProxiedAndGrantedUsers", columnNames = { "granting_user_id", "granted_user_id" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proxy {
	@Id
	@GeneratedValue
	private Long id;

	@NotNull
	@ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "granting_user_id", updatable = false, nullable = false)
	private User grantingUser;

	@NotNull
	@ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "granted_user_id", updatable = false, nullable = false)
	private User grantedUser;

	@NotNull
	@ElementCollection(fetch = FetchType.EAGER)
	@Default
	private List<String> grants = new ArrayList<>();

	@NotNull
	@Column(name = "start_date", nullable = false, updatable = true)
	private Date start;

	@Column(name = "end_date", nullable = true, updatable = true)
	private Date end;
}
```
So, yes, I just promoted low coupling from domain classes to JPA annotations (and I'm fine with that).

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

	Optional<User> findBySubject(String subject);

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
	static Specification<Proxy> searchSpec(Optional<String> grantingUserSubject, Optional<String> grantedUserSubject, Optional<Date> date) {
		final var specs =
				Stream
						.of(
								Optional.of(endsAfter(date.orElse(new Date()))),
								grantingUserSubject.map(ProxyRepository::grantingUserSubjectLike),
								grantedUserSubject.map(ProxyRepository::grantedUserSubjectLike),
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

	static Specification<Proxy> grantingUserSubjectLike(String grantingUserSubject) {
		return (root, query, cb) -> cb.like(root.get(Proxy_.grantingUser).get(User_.subject), grantingUserSubject);
	}

	static Specification<Proxy> grantedUserSubjectLike(String grantedUserSubject) {
		return (root, query, cb) -> cb.like(root.get(Proxy_.grantedUser).get(User_.subject), grantedUserSubject);
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
	private List<String> grants;

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

	@Mapping(target = "grantingUserSubject", source = "grantingUser.subject")
	@Mapping(target = "grantedUserSubject", source = "grantedUser.subject")
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

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.exceptions.ProxyUsersUnmodifiableException;
import com.c4_soft.user_proxies.api.exceptions.ResourceNotFoundException;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;
import com.c4_soft.user_proxies.api.jpa.UserRepository;
import com.c4_soft.user_proxies.api.security.ProxiesAuthentication;
import com.c4_soft.user_proxies.api.web.dto.ProxyDto;
import com.c4_soft.user_proxies.api.web.dto.ProxyEditDto;
import com.c4_soft.user_proxies.api.web.dto.UserCreateDto;
import com.c4_soft.user_proxies.api.web.dto.UserDto;
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
	@PreAuthorize("#auth.name == #dto.subject or hasAuthority('USERS_ADMIN')")
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
	@PreAuthorize("#auth.name == #subject or hasAnyAuthority('TOKEN_ISSUER', 'USERS_ADMIN') or isGranted(#subject, 'READ_PROXIES')")
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
	@PreAuthorize("#auth.name == #subject or hasAnyAuthority('USERS_ADMIN') or isGranted(#subject, 'READ_PROXIES')")
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
	@PreAuthorize("#auth.name == #grantingUserSubject or hasAuthority('USERS_ADMIN') or isGranted(#grantingUserSubject, 'EDIT_PROXIES')")
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
	@PreAuthorize("#auth.name == #grantingUserSubject or hasAuthority('USERS_ADMIN') or isGranted(#grantingUserSubject, 'EDIT_PROXIES')")
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
	@PreAuthorize("#auth.name == #grantingUserSubject or hasAuthority('USERS_ADMIN') or isGranted(#grantingUserSubject, 'EDIT_PROXIES')")
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
```

### application.properties
```
server.port=8443
server.shutdown=graceful

spring.datasource.url=jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.hbm2ddl.charset_name=UTF-8

com.c4-soft.springaddons.security.token-issuers[0].location=https://localhost:9443/auth/realms/master
com.c4-soft.springaddons.security.token-issuers[0].authorities.claims=realm_access.roles,resource_access.user-proxies-client.roles,resource_access.user-proxies-mapper.roles
com.c4-soft.springaddons.security.token-issuers[0].authorities.prefix=
com.c4-soft.springaddons.security.token-issuers[0].authorities.caze=
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

Edit @ControllerTest to add Web security-conf import:
```java
package com.c4_soft.user_proxies.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.c4_soft.springaddons.security.oauth2.test.mockmvc.AutoConfigureSecurityAddons;
import com.c4_soft.user_proxies.api.security.WebSecurityConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureSecurityAddons
@Import({ EnableSpringDataWebSupportTestConf.class, WebSecurityConfig.class })
public @interface ControllerTest {
}
```

We'll also need a few User and Proxy fixtures:
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
		return Stream.of(admin(), userA(), userB()).collect(Collectors.toMap(User::getSubject, u -> u));
	}

	public static Map<String, User> setUp(UserRepository userRepository) {
		final var users = UserFixtures.all();
		when(userRepository.findBySubject(anyString())).thenAnswer(invocation -> Optional.ofNullable(users.get(invocation.getArgument(0))));
		return users;
	}
}
```
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

public class ProxyFixtures {

	private static Proxy userAToUserB() {
		return new Proxy(
				2L,
				UserFixtures.userA(),
				UserFixtures.userB(),
				List.of("EDIT_PROXIES"),
				Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
				Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
	}

	private static Proxy userBToUserA() {
		return new Proxy(2L, UserFixtures.userB(), UserFixtures.userA(), List.of(), Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), null);
	}

	public static Map<String, List<Proxy>> all() {
		final var proxiesByGrantingUser = Stream.of(userAToUserB(), userBToUserA()).collect(Collectors.groupingBy(p -> p.getGrantingUser().getSubject()));
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
```

Test application.properties:
```
com.c4-soft.springaddons.test.web.default-media-type=application/json
com.c4-soft.springaddons.test.web.default-charset=utf-8
```

## Keycloak mapper

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

Then we'll need a DTO for Keycloak token response:
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

Last, the source for `com.c4_soft.user_proxies.keycloak.ProxiesMapper`:
```java
package com.c4_soft.user_proxies.api.keycloak;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.c4_soft.user_proxies.api.web.dto.ProxyDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxiesMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
	private static final String AUTHORIZATION_SERVER_BASE_URI = "proxies-service.users-endpoint-uri";
	private static final String PROXIES_SERVICE_CLIENT_SECRET = "proxies-service.client-secret";
	private static final String PROXIES_SERVICE_CLIENT_NAME = "proxies-service.client-name";
	private static final String PROVIDER_ID = "c4-soft.com";
	private static final String PROXIES_SERVICE_BASE_URI = "proxies-service.authorization-uri";

	private final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	private final Map<String, WebClient> webClientByBaseUri = new HashMap<>();
	private long expiresAt = 0L;
	private Optional<TokenResponseDto> token = Optional.empty();

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
		property.setName(AUTHORIZATION_SERVER_BASE_URI);
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
		return "Adds a \"proxies\" private claim containing a map of authorizations the user has to act on behalf of other users (one collection of grant IDs per user subject)";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	private <T extends IDToken> T transform(T token, ProtocolMapperModel mappingModel, KeycloakSession keycloakSession,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
		final var proxies = getGrantsByProxiedUserSubject(mappingModel, token);
		token.getOtherClaims().put("proxies", proxies);
		setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);
		return token;

	}

	private WebClient getWebClient(String baseUri) {
		return webClientByBaseUri.computeIfAbsent(baseUri, (String k) -> WebClient.builder().baseUrl(baseUri).build());
	}

	private Map<String, List<String>> getGrantsByProxiedUserSubject(ProtocolMapperModel mappingModel, IDToken token) {
		final var baseUri = mappingModel.getConfig().get(PROXIES_SERVICE_BASE_URI);
		try {
			final Optional<ProxyDto[]> dtos = Optional.ofNullable(getWebClient(baseUri).get()
					.uri("/{userSubject}/proxies/granted", token.getSubject()).headers(headers -> setBearer(headers, mappingModel)).retrieve().bodyToMono(ProxyDto[].class).block());

			return dtos.map(Stream::of)
					.map(s -> s.collect(Collectors.toMap(ProxyDto::getGrantingUserSubject, ProxyDto::getGrants)))
					.orElse(Map.of());
		} catch (final Exception e) {
			log.error("Failed to fetch user proxies: {}", e);
			return Map.of();
		}
	}
	
	private HttpHeaders setBearer(HttpHeaders headers, ProtocolMapperModel mappingModel) {
		getClientAccessToken(mappingModel).ifPresent(str -> {
			headers.setBearerAuth(str);
			log.debug("Authorization: {}", headers.get(HttpHeaders.AUTHORIZATION));
		});
		return headers;
	}

	private Optional<String> getClientAccessToken(ProtocolMapperModel mappingModel) {
		final var now = new Date().getTime();
		final var baseUri = mappingModel.getConfig().get(AUTHORIZATION_SERVER_BASE_URI);
		final var username = mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_NAME);
		final var password = mappingModel.getConfig().get(PROXIES_SERVICE_CLIENT_SECRET);
		if (expiresAt < now) {
			try {
				token = Optional.ofNullable(getWebClient(baseUri).post().headers(headers -> {
					headers.setBasicAuth(username, password);
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}).body(BodyInserters.fromFormData("scope", "openid").with("grant_type", "client_credentials"))
						.retrieve().bodyToMono(TokenResponseDto.class).block());
				expiresAt = now + 1000L * token.map(TokenResponseDto::getExpiresIn).orElse(0L);
			} catch (final Exception e) {
				log.error("Failed to get client authorization-token: {}", e);
				return Optional.empty();
			}
		}
		return token.map(TokenResponseDto::getAccessToken);
	}
}
```

Now, you can 
- `mvn package` 
- copy shaded jar to your keycloak `standalone/deployments/` folder
- open `Clients` > `user-proxies-client` > `Mappers` > `Create` in Keycloak admin console to configure our `User proxies mapper`

## Other resource-server sample
To demo that proxies security SpEL can be used also in modules that do not have access to User and proxy entities (sololy the JWT claims are used), we'll create a new `greet` module:

### Controller
``` java
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
	@PreAuthorize("isGranted(#otherSubject, 'greet')")
	public String getGreetingOnBehalfOf(@PathVariable("otherSubject") String otherSubject, ProxiesAuthentication auth) {
		return String.format("Hi %s!", otherSubject);
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

### application.properties
```
server.port=8444
server.shutdown=graceful

com.c4-soft.springaddons.security.authorities[0].authorization-server-location=http://localhost:9443/auth/realms/master
com.c4-soft.springaddons.security.authorities[0].claims=realm_access.roles,resource_access.user-proxies-client.roles
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
```

## Generate OpenAPI spec files

- Edit `user-proxies-api` pom file to update integration-test.port with what is defined in application.properties : `<integration-tests.port>8443</integration-tests.port>`
- run `mvn clean install -Popenapi`