package com.banking.system;

import com.banking.system.dto.request.AdminCreateRequestDto;
import com.banking.system.dto.request.AppUserLoginRequestDto;
import com.banking.system.dto.request.BankAccountCreateRequestDto;
import com.banking.system.dto.request.DepositRequestDto;
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

        Admin admin = new Admin(savedAppUser, "SEED-001", "Seed", "Admin");
        adminRepository.save(admin);

        return savedAppUser;
    }

    private String loginAndGetToken(String email, String password) throws Exception {

        AppUserLoginRequestDto loginRequest = new AppUserLoginRequestDto(email, password);

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
        return token;
    }

    private Long registerUserAndGetUserId(String adminToken, String email, String firstName) throws Exception {

        String uniquePhone = "09" + String.format("%09d", Math.abs(email.hashCode()) % 1_000_000_000);

        UserCreateRequestDto userRequest = new UserCreateRequestDto(
            email, "userPass123", firstName, "Doe", uniquePhone, "123 Main St"
        );

        String userResponse = mockMvc.perform(post("/api/users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(userResponse).get("id").asLong();
    }

    private Long createAccountAndGetAccountId(String adminToken, Long userId) throws Exception {

        BankAccountCreateRequestDto accountRequest = new BankAccountCreateRequestDto();
        accountRequest.setUserId(userId);

        String accountResponse = mockMvc.perform(post("/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.balance").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(accountResponse).get("id").asLong();
    }


    // ===================== FULL FLOW — USER + ACCOUNT =====================

    @Test
    void fullFlow_registerLoginCreateAccount_shouldSucceedEndToEnd() throws Exception {

        seedAdmin("seedadmin@example.com", "adminPass123");
        String adminToken = loginAndGetToken("seedadmin@example.com", "adminPass123");

        Long newUserId = registerUserAndGetUserId(adminToken, "jane@example.com", "Jane");

        createAccountAndGetAccountId(adminToken, newUserId);
    }

    @Test
    void fullFlow_registerAdmin_shouldSucceedEndToEnd() throws Exception {

        seedAdmin("superadmin@example.com", "adminPass123");
        String adminToken = loginAndGetToken("superadmin@example.com", "adminPass123");

        AdminCreateRequestDto adminRequest = new AdminCreateRequestDto(
            "newadmin@example.com", "adminPass456", "STAFF002", "Alice", "Smith"
        );

        mockMvc.perform(post("/api/admins/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.staffCode").value("STAFF002"))
            .andExpect(jsonPath("$.firstName").value("Alice"))
            .andExpect(jsonPath("$.email").value("newadmin@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }


    // ===================== FULL FLOW — TRANSACTIONS + ANALYTICS =====================

    @Test
    void fullFlow_depositAndFetchInsights_shouldSucceedEndToEnd() throws Exception {

        seedAdmin("txnadmin@example.com", "adminPass123");
        String adminToken = loginAndGetToken("txnadmin@example.com", "adminPass123");

        Long userId = registerUserAndGetUserId(adminToken, "txnuser@example.com", "Mark");
        Long accountId = createAccountAndGetAccountId(adminToken, userId);

        String userToken = loginAndGetToken("txnuser@example.com", "userPass123");

        DepositRequestDto depositRequest = new DepositRequestDto();
        depositRequest.setAccountId(accountId);
        depositRequest.setAmount(new java.math.BigDecimal("5000.00"));

        mockMvc.perform(post("/api/transactions/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
            .andExpect(status().isCreated());

        // Fetch insights as the owner — should succeed
        mockMvc.perform(get("/api/analytics/insights/" + accountId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isOk());
    }

    @Test
    void analyticsEndpoint_shouldReturn403_whenNotAccountOwner() throws Exception {

        seedAdmin("analyticsadmin@example.com", "adminPass123");
        String adminToken = loginAndGetToken("analyticsadmin@example.com", "adminPass123");

        Long ownerId = registerUserAndGetUserId(adminToken, "owner@example.com", "Owner");
        Long accountId = createAccountAndGetAccountId(adminToken, ownerId);

        // Register a second, unrelated user
        registerUserAndGetUserId(adminToken, "intruder@example.com", "Intruder");
        String intruderToken = loginAndGetToken("intruder@example.com", "userPass123");

        mockMvc.perform(get("/api/analytics/insights/" + accountId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
            .andExpect(status().isForbidden());
    }


    // ===================== AUTH EDGE CASES =====================

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
    void registerUser_shouldReturn403_whenCallerIsNotAdmin() throws Exception {

        AppUser user = new AppUser();
        user.setEmail("regularuser@example.com");
        user.setPasswordHash(PasswordUtil.hashPassword("userPass123"));
        user.setRole(Role.USER);
        appUserRepository.save(user);

        String userToken = loginAndGetToken("regularuser@example.com", "userPass123");

        UserCreateRequestDto request = new UserCreateRequestDto(
            "shouldnotwork@example.com", "password123", "Test", "User", "09171234567", "123 St"
        );

        mockMvc.perform(post("/api/users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void registerAdmin_shouldReturn403_whenCallerIsNotAdmin() throws Exception {

        AppUser user = new AppUser();
        user.setEmail("regularuser2@example.com");
        user.setPasswordHash(PasswordUtil.hashPassword("userPass123"));
        user.setRole(Role.USER);
        appUserRepository.save(user);

        String userToken = loginAndGetToken("regularuser2@example.com", "userPass123");

        AdminCreateRequestDto request = new AdminCreateRequestDto(
            "shouldnotwork@example.com", "password123", "STAFF999", "Bad", "Actor"
        );

        mockMvc.perform(post("/api/admins/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void registerUser_shouldReturn409_whenEmailAlreadyTaken() throws Exception {

        seedAdmin("adminfordup@example.com", "adminPass123");
        String adminToken = loginAndGetToken("adminfordup@example.com", "adminPass123");

        UserCreateRequestDto request = new UserCreateRequestDto(
            "duplicate@example.com", "password123", "First", "User", "09171111111", "Addr 1"
        );

        mockMvc.perform(post("/api/users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        UserCreateRequestDto duplicate = new UserCreateRequestDto(
            "duplicate@example.com", "password456", "Second", "User", "09172222222", "Addr 2"
        );

        mockMvc.perform(post("/api/users/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isConflict());
    }


    // ===================== LOAN MODULE — REQUIRES PYTHON ANALYTICS SERVICE RUNNING =====================
    // NOTE: These tests call the real analytics.service.url and require the FastAPI
    // service to be running locally during the test run, since credit score is fetched
    // from Python during loan application. If the service is not running, these tests
    // will fail with a connection error rather than a business logic failure.

    @Test
    void loanFlow_applyAndReject_shouldSucceedEndToEnd() throws Exception {

        seedAdmin("loanadmin@example.com", "adminPass123");
        String adminToken = loginAndGetToken("loanadmin@example.com", "adminPass123");

        Long userId = registerUserAndGetUserId(adminToken, "loanuser@example.com", "Loanee");
        Long accountId = createAccountAndGetAccountId(adminToken, userId);

        String userToken = loginAndGetToken("loanuser@example.com", "userPass123");

        // Deposit enough history so credit score isn't the absolute minimum
        DepositRequestDto depositRequest = new DepositRequestDto();
        depositRequest.setAccountId(accountId);
        depositRequest.setAmount(new java.math.BigDecimal("60000.00"));

        mockMvc.perform(post("/api/transactions/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
            .andExpect(status().isCreated());

        String loanRequestJson = """
            {
              "bankAccountId": %d,
              "type": "PERSONAL",
              "amount": 40000.00,
              "termMonths": 12
            }
            """.formatted(accountId);

        // Apply for the loan — result may vary based on actual credit score calculation
        // from the live Python analytics service. Either a successful creation (201) or
        // a correctly rejected low-score application (400) is valid behavior here.
        int status = mockMvc.perform(post("/api/loans/apply")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanRequestJson))
            .andReturn()
            .getResponse()
            .getStatus();

        // Either it's created (201) or correctly rejected for low credit score / duplicate loan (400)
        assertTrue(status == 201 || status == 400);
    }

    @Test
    void loanEndpoint_shouldReturn403_whenAdminTriesToApply() throws Exception {

        seedAdmin("loanadmin2@example.com", "adminPass123");
        String adminToken = loginAndGetToken("loanadmin2@example.com", "adminPass123");

        Long userId = registerUserAndGetUserId(adminToken, "loanuser2@example.com", "Loanee2");
        Long accountId = createAccountAndGetAccountId(adminToken, userId);

        String loanRequestJson = """
            {
              "bankAccountId": %d,
              "type": "PERSONAL",
              "amount": 40000.00,
              "termMonths": 12
            }
            """.formatted(accountId);

        // Admin role is not permitted to apply for loans — only USER role can
        mockMvc.perform(post("/api/loans/apply")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanRequestJson))
            .andExpect(status().isForbidden());
    }
}