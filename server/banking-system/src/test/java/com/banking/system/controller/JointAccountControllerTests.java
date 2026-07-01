package com.banking.system.controller;

import com.banking.system.dto.request.JointAccountInviteRequestDto;
import com.banking.system.dto.response.JointAccountMemberResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.enums.JointAccountRole;
import com.banking.system.security.JWTService;
import com.banking.system.security.SecurityConfig;
import com.banking.system.services.JointAccountService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JointAccountController.class)
@Import(SecurityConfig.class)
public class JointAccountControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private JointAccountService jointAccountService;

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

    private JointAccountMemberResponseDto memberResponse(JointAccountRole role) {
        return new JointAccountMemberResponseDto(
            1L, 5L, 2L, "Jane", "Doe", role, LocalDateTime.now()
        );
    }


    // ===================== ADD CO-OWNER =====================

    @Test
    void addCoOwner_shouldReturn201_whenAdmin() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        when(jointAccountService.addCoOwner(eq(5L), any(JointAccountInviteRequestDto.class), eq(99L)))
            .thenReturn(memberResponse(JointAccountRole.CO_OWNER));

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("CO_OWNER"))
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void addCoOwner_shouldReturn403_whenUserRole() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(userAuth(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verify(jointAccountService, never()).addCoOwner(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldReturn401_whenUnauthenticated() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addCoOwner_shouldReturn400_whenCoOwnerUserIdMissing() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(null);

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(jointAccountService, never()).addCoOwner(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldReturn404_whenAccountNotFound() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        when(jointAccountService.addCoOwner(eq(5L), any(JointAccountInviteRequestDto.class), eq(99L)))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void addCoOwner_shouldReturn404_whenUserNotFound() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        when(jointAccountService.addCoOwner(eq(5L), any(JointAccountInviteRequestDto.class), eq(99L)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void addCoOwner_shouldReturn409_whenAlreadyHasCoOwner() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        when(jointAccountService.addCoOwner(eq(5L), any(JointAccountInviteRequestDto.class), eq(99L)))
            .thenThrow(new DuplicateResourceException("This account already has a co-owner"));

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void addCoOwner_shouldReturn400_whenAccountClosed() throws Exception {

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        when(jointAccountService.addCoOwner(eq(5L), any(JointAccountInviteRequestDto.class), eq(99L)))
            .thenThrow(new InvalidAccountStateException("Cannot add co-owner to a closed account"));

        mockMvc.perform(post("/api/accounts/5/joint/invite")
                .with(authentication(adminAuth(99L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }


    // ===================== REMOVE CO-OWNER =====================

    @Test
    void removeCoOwner_shouldReturn204_whenAdmin() throws Exception {

        doNothing().when(jointAccountService).removeCoOwner(eq(5L), eq(99L));

        mockMvc.perform(delete("/api/accounts/5/joint/remove")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNoContent());

        verify(jointAccountService, times(1)).removeCoOwner(5L, 99L);
    }

    @Test
    void removeCoOwner_shouldReturn403_whenUserRole() throws Exception {

        mockMvc.perform(delete("/api/accounts/5/joint/remove")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isForbidden());

        verify(jointAccountService, never()).removeCoOwner(any(), any());
    }

    @Test
    void removeCoOwner_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(delete("/api/accounts/5/joint/remove"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void removeCoOwner_shouldReturn404_whenAccountNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("Account not found"))
            .when(jointAccountService).removeCoOwner(eq(5L), eq(99L));

        mockMvc.perform(delete("/api/accounts/5/joint/remove")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void removeCoOwner_shouldReturn404_whenCoOwnerNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("Co-owner not found"))
            .when(jointAccountService).removeCoOwner(eq(5L), eq(99L));

        mockMvc.perform(delete("/api/accounts/5/joint/remove")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }


    // ===================== GET MEMBERS =====================

    @Test
    void getMembers_shouldReturn200_whenAdmin() throws Exception {

        List<JointAccountMemberResponseDto> members = List.of(
            memberResponse(JointAccountRole.PRIMARY),
            memberResponse(JointAccountRole.CO_OWNER)
        );

        when(jointAccountService.getMembers(eq(5L), eq(99L), eq(true))).thenReturn(members);

        mockMvc.perform(get("/api/accounts/5/joint/members")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].role").value("PRIMARY"))
            .andExpect(jsonPath("$[1].role").value("CO_OWNER"));
    }

    @Test
    void getMembers_shouldReturn200_whenMember() throws Exception {

        List<JointAccountMemberResponseDto> members = List.of(
            memberResponse(JointAccountRole.PRIMARY)
        );

        when(jointAccountService.getMembers(eq(5L), eq(1L), eq(false))).thenReturn(members);

        mockMvc.perform(get("/api/accounts/5/joint/members")
                .with(authentication(userAuth(1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getMembers_shouldReturn403_whenNotMember() throws Exception {

        when(jointAccountService.getMembers(eq(5L), eq(999L), eq(false)))
            .thenThrow(new UnauthorizedActionException("You do not have access to this account"));

        mockMvc.perform(get("/api/accounts/5/joint/members")
                .with(authentication(userAuth(999L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getMembers_shouldReturn404_whenAccountNotFound() throws Exception {

        when(jointAccountService.getMembers(eq(5L), eq(99L), eq(true)))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/5/joint/members")
                .with(authentication(adminAuth(99L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getMembers_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/accounts/5/joint/members"))
            .andExpect(status().isUnauthorized());
    }
}