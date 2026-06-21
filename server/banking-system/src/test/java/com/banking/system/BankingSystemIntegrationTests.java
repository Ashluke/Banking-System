package com.banking.system;

import com.banking.system.dto.request.AppUserCreateRequestDto;
import com.banking.system.dto.request.AppUserLoginRequestDto;
import com.banking.system.dto.request.BankAccountCreateRequestDto;
import com.banking.system.dto.request.UserCreateRequestDto;
import com.banking.system.model.entities.Admin;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.enums.Role;
import com.banking.system.repository.AdminRepository;
import com.banking.system.repository.AppUserRepository;
import com.banking.system.util.PasswordUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@ActiveProfiles("test")
public class BankingSystemIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AdminRepository adminRepository;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    private AppUser seedAdmin(String email, String rawPassword) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash(PasswordUtil.hashPassword(rawPassword));
        appUser.setRole(Role.ADMIN);

        AppUser savedAppUser = appUserRepository.save(appUser);

        // Must also create the Admin entity so AuditLogService.logAction() can find it
        Admin admin = new Admin(savedAppUser, "SEED-001", "Seed", "Admin");
        adminRepository.save(admin);

        return savedAppUser;
    }

    @Test
    void fullFlow_registerLoginCreateAccount_shouldSucceedEndToEnd() throws Exception {

        // 1. Seed an admin directly (bypassing the chicken-and-egg register-requires-admin problem)
        seedAdmin("seedadmin@example.com", "adminPass123");

        // 2. Login as that admin to get a real JWT
        AppUserLoginRequestDto loginRequest = new AppUserLoginRequestDto(
            "seedadmin@example.com", "adminPass123"
        );

        String loginResponse = mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());

        // 3. Use the real JWT to register a new USER account (admin-only endpoint)
        AppUserCreateRequestDto registerRequest = new AppUserCreateRequestDto(
            "newuser@example.com", "userPass123", Role.USER
        );

        String registerResponse = mockMvc.perform(post("/api/app-users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long newAppUserId = objectMapper.readTree(registerResponse).get("id").asLong();

        // 4. Create the User profile for that AppUser, still as admin
        UserCreateRequestDto userProfileRequest = new UserCreateRequestDto(
            newAppUserId, "Jane", "Doe", "09171234567", "123 Main St"
        );

        String userResponse = mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userProfileRequest)))
            .andExpect(status().isCreated())
            .andDo(print())
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long newUserId = objectMapper.readTree(userResponse).get("id").asLong();

        // 5. Create a bank account for that user, still as admin
        BankAccountCreateRequestDto accountRequest = new BankAccountCreateRequestDto();
        accountRequest.setUserId(newUserId);

        mockMvc.perform(post("/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void protectedEndpoint_shouldReturn401_withoutToken() throws Exception {

        mockMvc.perform(get("/api/admins"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn401_withInvalidToken() throws Exception {

        mockMvc.perform(get("/api/admins")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn401_withWrongPassword() throws Exception {

        seedAdmin("realuser@example.com", "correctPassword123");

        AppUserLoginRequestDto loginRequest = new AppUserLoginRequestDto(
            "realuser@example.com", "wrongPassword"
        );

        mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_shouldReturn403_whenCallerIsNotAdmin() throws Exception {

        // Seed a regular USER and log in as them
        AppUser user = new AppUser();
        user.setEmail("regularuser@example.com");
        user.setPasswordHash(PasswordUtil.hashPassword("userPass123"));
        user.setRole(Role.USER);
        appUserRepository.save(user);

        AppUserLoginRequestDto loginRequest = new AppUserLoginRequestDto(
            "regularuser@example.com", "userPass123"
        );

        String loginResponse = mockMvc.perform(post("/api/app-users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        AppUserCreateRequestDto registerRequest = new AppUserCreateRequestDto(
            "shouldnotwork@example.com", "password123", Role.USER
        );

        mockMvc.perform(post("/api/app-users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isForbidden());
    }
}