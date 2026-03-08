package com.tecsup.app.micro.user.infrastructure.persistence.repository;

import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.domain.repository.UserRepository;
import com.tecsup.app.micro.user.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de Usuario (Adaptador)
 * Conecta el dominio con la infraestructura de persistencia
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository {
    
    private final JpaUserRepository jpaUserRepository;
    
    @Override
    public List<User> findAll() {
        log.debug("Finding all users");
        return jpaUserRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<User> findById(Long id) {
        log.debug("Finding user by id: {}", id);
        return jpaUserRepository.findById(id)
                .map(this::toDomain);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return jpaUserRepository.findByEmail(email)
                .map(this::toDomain);
    }
    
    @Override
    public User save(User user) {
        log.debug("Saving user: {}", user.getEmail());
        UserEntity entity = toEntity(user);
        UserEntity savedEntity = jpaUserRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public void deleteById(Long id) {
        log.debug("Deleting user by id: {}", id);
        jpaUserRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        log.debug("Checking if email exists: {}", email);
        return jpaUserRepository.existsByEmail(email);
    }
    
    // Mappers
    
    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .password(entity.getPassword())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private UserEntity toEntity(User user) {
        UserEntity entity = UserEntity.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .password(user.getPassword())
                .role(user.getRole())
                .build();
        
        if (user.getId() != null) {
            entity.setId(user.getId());
        }
        
        return entity;
    }
}
