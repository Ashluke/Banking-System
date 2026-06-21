package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private AppUser createAppUser(String email) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed");
        appUser.setRole(Role.USER);

        return appUserRepository.save(appUser);
    }


    @Test
    void findByAppUser_Id_shouldReturnUser_whenExists() {

        AppUser appUser = createAppUser("user1@example.com");

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("09171111111");
        user.setAddress("123 Main St");
        userRepository.save(user);

        Optional<User> result = userRepository.findByAppUser_Id(appUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void findByAppUser_Id_shouldReturnEmpty_whenNotFound() {

        Optional<User> result = userRepository.findByAppUser_Id(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByAppUser_Id_shouldReturnTrue_whenUserExists() {

        AppUser appUser = createAppUser("user2@example.com");

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setPhoneNumber("09172222222");
        user.setAddress("456 Side St");
        userRepository.save(user);

        boolean exists = userRepository.existsByAppUser_Id(appUser.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByAppUser_Id_shouldReturnFalse_whenUserNotFound() {

        AppUser appUser = createAppUser("user3@example.com");

        boolean exists = userRepository.existsByAppUser_Id(appUser.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldPersistUser_withGeneratedId() {

        AppUser appUser = createAppUser("user4@example.com");

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("Alice");
        user.setLastName("Wong");
        user.setPhoneNumber("09174444444");
        user.setAddress("789 Far St");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAppUser().getId()).isEqualTo(appUser.getId());
    }

    @Test
    void save_shouldThrowException_whenPhoneNumberDuplicated() {

        AppUser appUser1 = createAppUser("user5@example.com");

        User user1 = new User();
        user1.setAppUser(appUser1);
        user1.setFirstName("Bob");
        user1.setLastName("Lee");
        user1.setPhoneNumber("09175555555");
        user1.setAddress("111 Same St");
        userRepository.save(user1);

        AppUser appUser2 = createAppUser("user6@example.com");

        User user2 = new User();
        user2.setAppUser(appUser2);
        user2.setFirstName("Carol");
        user2.setLastName("Tan");
        user2.setPhoneNumber("09175555555");
        user2.setAddress("222 Other St");

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> {
                userRepository.save(user2);
                userRepository.flush();
            }
        );
    }

    @Test
    void findAll_shouldReturnPagedUsers() {

        AppUser appUser1 = createAppUser("user7@example.com");
        AppUser appUser2 = createAppUser("user8@example.com");

        User user1 = new User();
        user1.setAppUser(appUser1);
        user1.setFirstName("Dave");
        user1.setLastName("Kim");
        user1.setPhoneNumber("09177777777");
        user1.setAddress("333 Up St");
        userRepository.save(user1);

        User user2 = new User();
        user2.setAppUser(appUser2);
        user2.setFirstName("Eve");
        user2.setLastName("Ng");
        user2.setPhoneNumber("09178888888");
        user2.setAddress("444 Down St");
        userRepository.save(user2);

        Page<User> result = userRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void save_shouldThrowException_whenAppUserAlreadyHasUserProfile() {

        AppUser appUser = createAppUser("user9@example.com");

        User user1 = new User();
        user1.setAppUser(appUser);
        user1.setFirstName("Frank");
        user1.setLastName("Cruz");
        user1.setPhoneNumber("09179999999");
        user1.setAddress("555 One St");
        userRepository.save(user1);

        User user2 = new User();
        user2.setAppUser(appUser);
        user2.setFirstName("George");
        user2.setLastName("Reyes");
        user2.setPhoneNumber("09170000000");
        user2.setAddress("666 Two St");

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> {
                userRepository.save(user2);
                userRepository.flush();
            }
        );
    }
}