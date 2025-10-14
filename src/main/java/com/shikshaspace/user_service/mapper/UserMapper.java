package com.shikshaspace.user_service.mapper;


import com.shikshaspace.user_service.dto.KeycloakUserData;
import com.shikshaspace.user_service.dto.UserProfileRequest;
import com.shikshaspace.user_service.dto.UserProfileResponse;
import com.shikshaspace.user_service.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "keycloakId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isProfileComplete", constant = "false")
    User keycloakToEntity(KeycloakUserData keycloakUserData);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UserProfileRequest request, @MappingTarget User entity);

    UserProfileResponse entityToResponse(User user);
}
