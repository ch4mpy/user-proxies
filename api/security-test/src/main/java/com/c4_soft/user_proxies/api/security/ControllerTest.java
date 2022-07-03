package com.c4_soft.user_proxies.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.c4_soft.springaddons.security.oauth2.test.mockmvc.AutoConfigureSecurityAddons;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureSecurityAddons
@Import(WebSecurityConfig.class)
public @interface ControllerTest {
}
