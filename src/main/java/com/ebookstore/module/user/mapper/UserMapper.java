package com.ebookstore.module.user.mapper;

import com.ebookstore.module.user.dto.UserDTO;
import com.ebookstore.module.user.entity.Role;
import com.ebookstore.module.user.entity.User;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "active", source = "active")
    @Mapping(target = "roles",  expression = "java(mapRoles(user.getRoles()))")
    UserDTO toDTO(User user);

    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
