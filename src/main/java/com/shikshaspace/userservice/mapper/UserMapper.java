package com.shikshaspace.userservice.mapper;

import com.shikshaspace.userservice.domain.User;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.request.UpdateProfileRequest;
import com.shikshaspace.userservice.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

  UserResponse toResponse(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "keycloakId", ignore = true)
  @Mapping(target = "isActive", constant = "true")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  User toEntity(RegisterRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "keycloakId", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(UpdateProfileRequest request, @MappingTarget User user);
}
