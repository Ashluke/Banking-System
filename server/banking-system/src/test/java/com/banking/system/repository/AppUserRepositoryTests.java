package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AppUserRepositoryTests {

    @Autowired
    private AppUserRepository appUserRepository;


    private AppUser createAppUser(String email, Role role) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed-password");
        appUser.setRole(role);

        return appUserRepository.save(appUser);
    }


    @Test
    void findByEmail_shouldReturnAppUser_whenEmailExists() {

        createAppUser("test@example.com", Role.USER);

        Optional<AppUser> result = appUserRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailNotFound() {

        Optional<AppUser> result = appUserRepository.findByEmail("missing@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {

        createAppUser("test@example.com", Role.USER);

        boolean exists = appUserRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailNotFound() {

        boolean exists = appUserRepository.existsByEmail("missing@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldPersistAppUser_withGeneratedIdAndCreatedAt() {

        AppUser saved = createAppUser("test@example.com", Role.ADMIN);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void save_shouldThrowException_whenEmailDuplicated() {

        createAppUser("duplicate@example.com", Role.USER);

        AppUser duplicate = new AppUser();
        duplicate.setEmail("duplicate@example.com");
        duplicate.setPasswordHash("another-hash");
        duplicate.setRole(Role.USER);

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> {
                appUserRepository.save(duplicate);
                appUserRepository.flush();
            }
        );
    }

    @Test
    void findAll_shouldReturnPagedAppUsers() {

        createAppUser("user1@example.com", Role.USER);
        createAppUser("user2@example.com", Role.USER);
        createAppUser("admin1@example.com", Role.ADMIN);

        Page<AppUser> result = appUserRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }
}