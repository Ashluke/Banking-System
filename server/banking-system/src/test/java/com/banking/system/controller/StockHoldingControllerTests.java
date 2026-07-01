package com.banking.system.controller;

import com.banking.system.dto.request.StockHoldingRequestDto;
import com.banking.system.dto.response.StockHoldingResponseDto;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.StockHoldingService;

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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockHoldingController.class)
@Import(SecurityConfig.class)
public class StockHoldingControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private StockHoldingService stockHoldingService;

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

    private StockHoldingResponseDto holdingResponse() {
        return new StockHoldingResponseDto(
            1L, 1L, "AAPL",
            BigDecimal.valueOf(10), BigDecimal.valueOf(150.00),
            LocalDateTime.now()
        );
    }

    private StockHoldingRequestDto holdingRequest() {
        return new StockHoldingRequestDto(
            1L, "AAPL", BigDecimal.valueOf(10), BigDecimal.valueOf(150.00)
        );
    }


    // ===================== ADD =====================

    @Test
    void add_shouldReturn201_whenUserRole() throws Exception {

        when(stockHoldingService.addHolding(any(StockHoldingRequestDto.class)))
            .thenReturn(holdingResponse());

        mockMvc.perform(post("/api/holdings")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.symbol").value("AAPL"))
            .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void add_shouldReturn201_whenAdminRole() throws Exception {

        when(stockHoldingService.addHolding(any(StockHoldingRequestDto.class)))
            .thenReturn(holdingResponse());

        mockMvc.perform(post("/api/holdings")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isCreated());
    }

    @Test
    void add_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(post("/api/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isUnauthorized());

        verify(stockHoldingService, never()).addHolding(any());
    }

    @Test
    void add_shouldReturn400_whenUserIdMissing() throws Exception {

        StockHoldingRequestDto request = new StockHoldingRequestDto(
            null, "AAPL", BigDecimal.valueOf(10), BigDecimal.valueOf(150.00)
        );

        mockMvc.perform(post("/api/holdings")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(stockHoldingService, never()).addHolding(any());
    }

    @Test
    void add_shouldReturn400_whenSymbolMissing() throws Exception {

        StockHoldingRequestDto request = new StockHoldingRequestDto(
            1L, "", BigDecimal.valueOf(10), BigDecimal.valueOf(150.00)
        );

        mockMvc.perform(post("/api/holdings")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(stockHoldingService, never()).addHolding(any());
    }

    @Test
    void add_shouldReturn400_whenQuantityNotPositive() throws Exception {

        StockHoldingRequestDto request = new StockHoldingRequestDto(
            1L, "AAPL", BigDecimal.valueOf(-1), BigDecimal.valueOf(150.00)
        );

        mockMvc.perform(post("/api/holdings")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(stockHoldingService, never()).addHolding(any());
    }

    @Test
    void add_shouldReturn404_whenUserNotFound() throws Exception {

        when(stockHoldingService.addHolding(any(StockHoldingRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/holdings")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isNotFound());
    }


    // ===================== GET BY USER ID =====================

    @Test
    void getByUserId_shouldReturn200_whenAuthenticated() throws Exception {

        Page<StockHoldingResponseDto> page = new PageImpl<>(List.of(holdingResponse()));

        when(stockHoldingService.getByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/holdings/users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].symbol").value("AAPL"));
    }

    @Test
    void getByUserId_shouldReturn200_whenAdminRole() throws Exception {

        Page<StockHoldingResponseDto> page = new PageImpl<>(List.of(holdingResponse()));

        when(stockHoldingService.getByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/holdings/users/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getByUserId_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/holdings/users/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getByUserId_shouldReturn404_whenUserNotFound() throws Exception {

        when(stockHoldingService.getByUserId(eq(1L), any(Pageable.class)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/holdings/users/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }


    // ===================== GET BY ID =====================

    @Test
    void getById_shouldReturn200_whenOwner() throws Exception {

        when(stockHoldingService.getById(eq(1L), eq(1L))).thenReturn(holdingResponse());

        mockMvc.perform(get("/api/holdings/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("AAPL"))
            .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void getById_shouldReturn200_whenAdminRole() throws Exception {

        when(stockHoldingService.getById(eq(1L), eq(99L))).thenReturn(holdingResponse());

        mockMvc.perform(get("/api/holdings/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk());
    }

    @Test
    void getById_shouldReturn403_whenNotOwner() throws Exception {

        when(stockHoldingService.getById(eq(1L), eq(999L)))
            .thenThrow(new UnauthorizedActionException("You do not own this holding"));

        mockMvc.perform(get("/api/holdings/1")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {

        when(stockHoldingService.getById(eq(1L), eq(1L)))
            .thenThrow(new ResourceNotFoundException("Holding not found"));

        mockMvc.perform(get("/api/holdings/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/holdings/1"))
            .andExpect(status().isUnauthorized());
    }


    // ===================== UPDATE =====================

    @Test
    void update_shouldReturn200_whenAuthenticated() throws Exception {

        when(stockHoldingService.updateHolding(eq(1L), any(StockHoldingRequestDto.class)))
            .thenReturn(holdingResponse());

        mockMvc.perform(put("/api/holdings/1")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void update_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(put("/api/holdings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {

        when(stockHoldingService.updateHolding(eq(1L), any(StockHoldingRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("Holding not found"));

        mockMvc.perform(put("/api/holdings/1")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest())))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn400_whenSymbolMissing() throws Exception {

        StockHoldingRequestDto request = new StockHoldingRequestDto(
            1L, "", BigDecimal.valueOf(10), BigDecimal.valueOf(150.00)
        );

        mockMvc.perform(put("/api/holdings/1")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(stockHoldingService, never()).updateHolding(any(), any());
    }


    // ===================== DELETE =====================

    @Test
    void delete_shouldReturn204_whenAuthenticated() throws Exception {

        doNothing().when(stockHoldingService).deleteHolding(1L);

        mockMvc.perform(delete("/api/holdings/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNoContent());

        verify(stockHoldingService, times(1)).deleteHolding(1L);
    }

    @Test
    void delete_shouldReturn204_whenAdminRole() throws Exception {

        doNothing().when(stockHoldingService).deleteHolding(1L);

        mockMvc.perform(delete("/api/holdings/1")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(delete("/api/holdings/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("Holding not found"))
            .when(stockHoldingService).deleteHolding(1L);

        mockMvc.perform(delete("/api/holdings/1")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isNotFound());
    }
}