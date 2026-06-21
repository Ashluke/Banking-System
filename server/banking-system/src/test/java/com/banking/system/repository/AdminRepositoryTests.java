package com.banking.system.repository;

import com.banking.system.model.entities.Admin;
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
public class AdminRepositoryTests {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private AppUser createAppUser(String email) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed");
        appUser.setRole(Role.ADMIN);

        return appUserRepository.save(appUser);
    }


    @Test
    void findByAppUser_Id_shouldReturnAdmin_whenExists() {

        AppUser appUser = createAppUser("admin1@example.com");

        Admin admin = new Admin(appUser, "STAFF001", "John", "Doe");
        adminRepository.save(admin);

        Optional<Admin> result = adminRepository.findByAppUser_Id(appUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getStaffCode()).isEqualTo("STAFF001");
    }

    @Test
    void findByAppUser_Id_shouldReturnEmpty_whenNotFound() {

        Optional<Admin> result = adminRepository.findByAppUser_Id(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByAppUser_Id_shouldReturnTrue_whenAdminExists() {

        AppUser appUser = createAppUser("admin2@example.com");

        adminRepository.save(new Admin(appUser, "STAFF002", "Jane", "Smith"));

        boolean exists = adminRepository.existsByAppUser_Id(appUser.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByAppUser_Id_shouldReturnFalse_whenAdminNotFound() {

        AppUser appUser = createAppUser("admin3@example.com");

        boolean exists = adminRepository.existsByAppUser_Id(appUser.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldPersistAdmin_withGeneratedId() {

        AppUser appUser = createAppUser("admin4@example.com");

        Admin admin = new Admin(appUser, "STAFF004", "Alice", "Wong");

        Admin saved = adminRepository.save(admin);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAppUser().getId()).isEqualTo(appUser.getId());
    }

    @Test
    void findAll_shouldReturnPagedAdmins() {

        AppUser appUser1 = createAppUser("admin5@example.com");
        AppUser appUser2 = createAppUser("admin6@example.com");

        adminRepository.save(new Admin(appUser1, "STAFF005", "Bob", "Lee"));
        adminRepository.save(new Admin(appUser2, "STAFF006", "Carol", "Tan"));

        Page<Admin> result = adminRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}