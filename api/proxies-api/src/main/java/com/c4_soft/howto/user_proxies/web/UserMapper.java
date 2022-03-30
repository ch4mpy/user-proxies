package com.c4_soft.howto.user_proxies.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

import com.c4_soft.howto.user_proxies.domain.User;
import com.c4_soft.howto.user_proxies.web.dtos.UserCreateDto;
import com.c4_soft.howto.user_proxies.web.dtos.UserDto;

@Mapper(componentModel = ComponentModel.SPRING)
public interface UserMapper {

	UserDto toDto(User domain);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "grantingProxies", ignore = true)
	@Mapping(target = "grantedProxies", ignore = true)
	void update(@MappingTarget User domain, UserCreateDto dto);

}