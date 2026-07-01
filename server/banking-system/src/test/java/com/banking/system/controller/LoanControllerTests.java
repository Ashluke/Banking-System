package com.banking.system.controller;

import com.banking.system.dto.request.LoanRequestDto;
import com.banking.system.dto.response.LoanRepaymentResponseDto;
import com.banking.system.dto.response.LoanResponseDto;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.enums.LoanType;
import com.banking.system.model.enums.LoanStatus;
import com.banking.system.model.enums.RepaymentStatus;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.LoanService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@Import(SecurityConfig.class)
public class LoanControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private JWTService jwtService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }


    private UsernamePasswordAuthenticationToken userAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long appUserId) {
        return new UsernamePasswordAuthenticationToken(
            appUserId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private LoanResponseDto loanResponse(LoanStatus status) {
        return new LoanResponseDto(
            1L, 1L, 1L, LoanType.PERSONAL, status,
            BigDecimal.valueOf(50000), BigDecimal.valueOf(5.0), 12,
            BigDecimal.valueOf(2500), BigDecimal.valueOf(4375), BigDecimal.valueOf(52500),
            750, null, null, LocalDateTime.now()
        );
    }

    private LoanRepaymentResponseDto repaymentResponse(RepaymentStatus status) {
        return new LoanRepaymentResponseDto(
            1L, 1L, 1, BigDecimal.valueOf(4375),
            LocalDate.now().plusMonths(1), null, status
        );
    }


    // ===================== APPLY =====================

    @Test
    void apply_shouldReturn201_whenUserRole() throws Exception {

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, BigDecimal.valueOf(50000), 12);

        when(loanService.applyForLoan(any(LoanRequestDto.class), eq(1L)))
            .thenReturn(loanResponse(LoanStatus.PENDING));

        mockMvc.perform(post("/api/loans/apply")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.creditScoreAtApplication").value(750));
    }

    @Test
    void apply_shouldReturn403_whenAdminRole() throws Exception {

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, BigDecimal.valueOf(50000), 12);

        mockMvc.perform(post("/api/loans/apply")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(loanService, never()).applyForLoan(any(), any());
    }

    @Test
    void apply_shouldReturn401_whenUnauthenticated() throws Exception {

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, BigDecimal.valueOf(50000), 12);

        mockMvc.perform(post("/api/loans/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void apply_shouldReturn400_whenAmountBelowMinimum() throws Exception {

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, BigDecimal.valueOf(1000), 12);

        mockMvc.perform(post("/api/loans/apply")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(loanService, never()).applyForLoan(any(), any());
    }

    @Test
    void apply_shouldReturn400_whenAccountIdMissing() throws Exception {

        LoanRequestDto request = new LoanRequestDto(null, LoanType.PERSONAL, BigDecimal.valueOf(50000), 12);

        mockMvc.perform(post("/api/loans/apply")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(loanService, never()).applyForLoan(any(), any());
    }

    @Test
    void apply_shouldReturn404_whenAccountNotFound() throws Exception {

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, BigDecimal.valueOf(50000), 12);

        when(loanService.applyForLoan(any(LoanRequestDto.class), eq(1L)))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(post("/api/loans/apply")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void apply_shouldReturn400_whenAccountNotActive() throws Exception {

        LoanRequestDto request = new LoanRequestDto(1L, LoanType.PERSONAL, BigDecimal.valueOf(50000), 12);

        when(loanService.applyForLoan(any(LoanRequestDto.class), eq(1L)))
            .thenThrow(new InvalidAccountStateException("Account is not active"));

        mockMvc.perform(post("/api/loans/apply")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }


    // ===================== APPROVE =====================

    @Test
    void approve_shouldReturn200_whenAdmin() throws Exception {

        when(loanService.approveLoan(1L)).thenReturn(loanResponse(LoanStatus.APPROVED));

        mockMvc.perform(put("/api/loans/1/approve")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(put("/api/loans/1/approve")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(loanService, never()).approveLoan(any());
    }

    @Test
    void approve_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/loans/1/approve"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void approve_shouldReturn404_whenLoanNotFound() throws Exception {

        when(loanService.approveLoan(1L))
            .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(put("/api/loans/1/approve")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void approve_shouldReturn400_whenLoanNotInReviewableState() throws Exception {

        when(loanService.approveLoan(1L))
            .thenThrow(new InvalidAccountStateException("Loan cannot be approved in its current state"));

        mockMvc.perform(put("/api/loans/1/approve")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isBadRequest());
    }


    // ===================== REJECT =====================

    @Test
    void reject_shouldReturn200_whenAdmin() throws Exception {

        when(loanService.rejectLoan(1L)).thenReturn(loanResponse(LoanStatus.REJECTED));

        mockMvc.perform(put("/api/loans/1/reject")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void reject_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(put("/api/loans/1/reject")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(loanService, never()).rejectLoan(any());
    }

    @Test
    void reject_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/loans/1/reject"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reject_shouldReturn404_whenLoanNotFound() throws Exception {

        when(loanService.rejectLoan(1L))
            .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(put("/api/loans/1/reject")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }


    // ===================== DISBURSE =====================

    @Test
    void disburse_shouldReturn200_whenAdmin() throws Exception {

        when(loanService.disburseLoan(1L)).thenReturn(loanResponse(LoanStatus.ACTIVE));

        mockMvc.perform(put("/api/loans/1/disburse")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void disburse_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(put("/api/loans/1/disburse")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(loanService, never()).disburseLoan(any());
    }

    @Test
    void disburse_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/loans/1/disburse"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void disburse_shouldReturn404_whenLoanNotFound() throws Exception {

        when(loanService.disburseLoan(1L))
            .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(put("/api/loans/1/disburse")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void disburse_shouldReturn400_whenLoanNotApproved() throws Exception {

        when(loanService.disburseLoan(1L))
            .thenThrow(new InvalidAccountStateException("Only approved loans can be disbursed"));

        mockMvc.perform(put("/api/loans/1/disburse")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void disburse_shouldReturn400_whenAccountNotActive() throws Exception {

        when(loanService.disburseLoan(1L))
            .thenThrow(new InvalidAccountStateException("Account is not active"));

        mockMvc.perform(put("/api/loans/1/disburse")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isBadRequest());
    }


    // ===================== PAY =====================

    @Test
    void pay_shouldReturn200_whenUserRole() throws Exception {

        when(loanService.makeRepayment(eq(1L), eq(1L)))
            .thenReturn(repaymentResponse(RepaymentStatus.PAID));

        mockMvc.perform(post("/api/loans/1/pay")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void pay_shouldReturn403_whenAdminRole() throws Exception {

        mockMvc.perform(post("/api/loans/1/pay")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isForbidden());

        verify(loanService, never()).makeRepayment(any(), any());
    }

    @Test
    void pay_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(post("/api/loans/1/pay"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void pay_shouldReturn403_whenNotLoanOwner() throws Exception {

        when(loanService.makeRepayment(eq(1L), eq(999L)))
            .thenThrow(new UnauthorizedActionException("You do not own this loan"));

        mockMvc.perform(post("/api/loans/1/pay")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void pay_shouldReturn404_whenLoanNotFound() throws Exception {

        when(loanService.makeRepayment(eq(1L), eq(1L)))
            .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(post("/api/loans/1/pay")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void pay_shouldReturn400_whenNoOutstandingRepayment() throws Exception {

        when(loanService.makeRepayment(eq(1L), eq(1L)))
            .thenThrow(new InvalidAccountStateException("No outstanding repayment due"));

        mockMvc.perform(post("/api/loans/1/pay")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void pay_shouldReturn400_whenInsufficientBalance() throws Exception {

        when(loanService.makeRepayment(eq(1L), eq(1L)))
            .thenThrow(new InvalidAccountStateException("Insufficient balance for repayment"));

        mockMvc.perform(post("/api/loans/1/pay")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isBadRequest());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenOwner() throws Exception {

        when(loanService.getById(eq(1L), eq(1L), eq(false)))
            .thenReturn(loanResponse(LoanStatus.ACTIVE));

        mockMvc.perform(get("/api/loans/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getById_shouldReturn200_whenAdmin() throws Exception {

        when(loanService.getById(eq(1L), eq(99L), eq(true)))
            .thenReturn(loanResponse(LoanStatus.ACTIVE));

        mockMvc.perform(get("/api/loans/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getById_shouldReturn403_whenNotOwner() throws Exception {

        when(loanService.getById(eq(1L), eq(999L), eq(false)))
            .thenThrow(new UnauthorizedActionException("You do not own this loan"));

        mockMvc.perform(get("/api/loans/1")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(loanService.getById(eq(1L), eq(1L), eq(false)))
            .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(get("/api/loans/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/loans/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET BY USER ID =====================

    @Test
    void getByUserId_shouldReturn200_whenOwner() throws Exception {

        Page<LoanResponseDto> page = new PageImpl<>(List.of(loanResponse(LoanStatus.ACTIVE)));

        when(loanService.getByUserId(eq(1L), eq(1L), eq(false), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/loans/users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getByUserId_shouldReturn200_whenAdmin() throws Exception {

        Page<LoanResponseDto> page = new PageImpl<>(List.of(loanResponse(LoanStatus.ACTIVE)));

        when(loanService.getByUserId(eq(1L), eq(99L), eq(true), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/loans/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getByUserId_shouldReturn403_whenNotOwner() throws Exception {

        when(loanService.getByUserId(eq(1L), eq(999L), eq(false), any(Pageable.class)))
            .thenThrow(new UnauthorizedActionException("You do not have access to these loans"));

        mockMvc.perform(get("/api/loans/users/1")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getByUserId_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/loans/users/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET ALL =====================

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {

        Page<LoanResponseDto> page = new PageImpl<>(List.of(loanResponse(LoanStatus.PENDING)));

        when(loanService.getAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/loans")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getAll_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(get("/api/loans")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(loanService, never()).getAll(any());
    }

    @Test
    void getAll_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/loans"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== GET REPAYMENT SCHEDULE =====================

    @Test
    void getSchedule_shouldReturn200_whenOwner() throws Exception {

        List<LoanRepaymentResponseDto> schedule = List.of(
            repaymentResponse(RepaymentStatus.PENDING),
            repaymentResponse(RepaymentStatus.PENDING)
        );

        when(loanService.getRepaymentSchedule(eq(1L), eq(1L), eq(false)))
            .thenReturn(schedule);

        mockMvc.perform(get("/api/loans/1/schedule")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getSchedule_shouldReturn200_whenAdmin() throws Exception {

        List<LoanRepaymentResponseDto> schedule = List.of(
            repaymentResponse(RepaymentStatus.PAID),
            repaymentResponse(RepaymentStatus.PENDING)
        );

        when(loanService.getRepaymentSchedule(eq(1L), eq(99L), eq(true)))
            .thenReturn(schedule);

        mockMvc.perform(get("/api/loans/1/schedule")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSchedule_shouldReturn403_whenNotOwner() throws Exception {

        when(loanService.getRepaymentSchedule(eq(1L), eq(999L), eq(false)))
            .thenThrow(new UnauthorizedActionException("You do not own this loan"));

        mockMvc.perform(get("/api/loans/1/schedule")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getSchedule_shouldReturn404_whenLoanNotFound() throws Exception {

        when(loanService.getRepaymentSchedule(eq(1L), eq(1L), eq(false)))
            .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(get("/api/loans/1/schedule")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getSchedule_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/loans/1/schedule"))
            .andExpect(status().isUnauthorized());
    }
}