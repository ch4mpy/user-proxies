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
