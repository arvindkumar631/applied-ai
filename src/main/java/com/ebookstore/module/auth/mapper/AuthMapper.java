package com.ebookstore.module.auth.mapper;

import com.ebookstore.module.auth.dto.AuthResponse;
import com.ebookstore.module.user.entity.Role;
import com.ebookstore.module.user.entity.User;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "userId",   source = "id")
    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "roles",    expression = "java(mapRoles(user.getRoles()))")
    @Mapping(target = "accessToken",  ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "tokenType",    ignore = true)
    @Mapping(target = "expiresIn",    ignore = true)
    AuthResponse toAuthResponse(User user);

    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
