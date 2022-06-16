package com.c4_soft.user_proxies.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2AuthenticationConverter;
import com.c4_soft.springaddons.security.oauth2.config.Jwt2AuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.spring.GenericMethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Bean
    SynchronizedJwt2AuthenticationConverter<ProxiesAuthentication> authenticationConverter(
            Jwt2AuthoritiesConverter authoritiesConverter) {
        return jwt -> new ProxiesAuthentication(new ProxiesClaimSet(jwt.getClaims()), authoritiesConverter.convert(jwt), jwt.getTokenValue());
    }

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new GenericMethodSecurityExpressionHandler<>(ProxiesMethodSecurityExpressionRoot::new);
    }
}