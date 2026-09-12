package com.jee.shop.user.mapper;

import com.jee.shop.user.dto.UserDto;
import com.jee.shop.user.entity.UserEntity;

/**
 * User Mapper.
 * Converts between UserEntity (JPA) and UserDto (REST API).
 *
 * Lernziel: Mapper Pattern, Entity-DTO Separation
 */
public class UserMapper {

    /**
     * Converts UserEntity to UserDto.
     *
     * @param user the JPA entity
     * @return the DTO
     */
    public static UserDto toDto(UserEntity user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * Converts UserDto to UserEntity.
     * Note: This is used primarily for testing; entity creation happens via UserEntity constructor.
     *
     * @param dto the DTO
     * @return the JPA entity
     */
    public static UserEntity toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        UserEntity user = new UserEntity();
        user.setId(dto.id());
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setRole(dto.role());
        user.setCreatedAt(dto.createdAt());
        user.setUpdatedAt(dto.updatedAt());

        return user;
    }
}

