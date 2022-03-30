package com.c4_soft.howto.user_proxies.web;

import java.util.Date;
import java.util.Optional;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

import com.c4_soft.howto.user_proxies.domain.Proxy;
import com.c4_soft.howto.user_proxies.web.dtos.ProxyDto;
import com.c4_soft.howto.user_proxies.web.dtos.ProxyEditDto;

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