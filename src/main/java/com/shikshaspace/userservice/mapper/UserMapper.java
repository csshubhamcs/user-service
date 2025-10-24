package com.shikshaspace.userservice.mapper;

import com.shikshaspace.userservice.domain.User;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.request.UpdateProfileRequest;
import com.shikshaspace.userservice.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for User entity and DTOs. Handles conversion between domain models and API DTOs.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

  /** Convert User entity to UserResponse DTO. */
  UserResponse toResponse(User user);

  /** Convert RegisterRequest to User entity. Ignores ID and audit fields (set by service layer). */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "keycloakId", ignore = true)
  @Mapping(target = "emailVerified", constant = "false")
  @Mapping(target = "isActive", constant = "true")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  User toEntity(RegisterRequest request);

  /**
   * Update existing User entity with UpdateProfileRequest data. Only updates non-null fields from
   * request.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "keycloakId", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "emailVerified", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(UpdateProfileRequest request, @MappingTarget User user);
}
