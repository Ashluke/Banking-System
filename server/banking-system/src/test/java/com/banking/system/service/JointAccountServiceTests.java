package com.banking.system.service;

import com.banking.system.dto.request.JointAccountInviteRequestDto;
import com.banking.system.dto.response.AuditLogResponseDto;
import com.banking.system.dto.response.JointAccountMemberResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.JointAccountRole;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
import com.banking.system.repository.UserRepository;
import com.banking.system.services.AuditLogService;
import com.banking.system.services.JointAccountService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JointAccountServiceTests {

    @Mock
    private JointAccountMemberRepository jointAccountMemberRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private JointAccountService jointAccountService;


    // ===================== ADD CO-OWNER =====================

    @Test
    void addCoOwner_shouldAddMember_setJoint_andLogAction() {

        User primaryUser = mock(User.class);
        when(primaryUser.getId()).thenReturn(1L);

        AppUser coOwnerAppUser = mock(AppUser.class);
        when(coOwnerAppUser.getId()).thenReturn(20L);

        User coOwnerUser = mock(User.class);
        when(coOwnerUser.getId()).thenReturn(2L);
        when(coOwnerUser.getFirstName()).thenReturn("Jane");
        when(coOwnerUser.getLastName()).thenReturn("Doe");
        when(coOwnerUser.getAppUser()).thenReturn(coOwnerAppUser);

        BankAccount account = mock(BankAccount.class);
        when(account.getId()).thenReturn(5L);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        JointAccountMember primary = mock(JointAccountMember.class);
        when(primary.getUser()).thenReturn(primaryUser);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.countByBankAccount_Id(5L)).thenReturn(1);
        when(userRepository.findById(2L)).thenReturn(Optional.of(coOwnerUser));
        when(jointAccountMemberRepository.existsByBankAccount_IdAndUser_Id(5L, 2L)).thenReturn(false);
        when(jointAccountMemberRepository.findByBankAccount_IdAndRole(5L, JointAccountRole.PRIMARY))
            .thenReturn(Optional.of(primary));
        when(jointAccountMemberRepository.save(any(JointAccountMember.class))).thenAnswer(i -> {
            JointAccountMember m = i.getArgument(0);
            return m;
        });
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(account);
        when(auditLogService.logAction(eq(99L), eq(20L), eq(ActionType.ADD_CO_OWNER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 20L, ActionType.ADD_CO_OWNER, LocalDateTime.now()));

        JointAccountInviteRequestDto request = new JointAccountInviteRequestDto(2L);

        JointAccountMemberResponseDto result = jointAccountService.addCoOwner(5L, request, 99L);

        assertEquals(JointAccountRole.CO_OWNER, result.getRole());
        assertEquals(2L, result.getUserId());

        verify(jointAccountMemberRepository, times(1)).save(any(JointAccountMember.class));
        verify(account, times(1)).setIsJoint(true);
        verify(bankAccountRepository, times(1)).save(account);
        verify(auditLogService, times(1)).logAction(99L, 20L, ActionType.ADD_CO_OWNER);
    }

    @Test
    void addCoOwner_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            jointAccountService.addCoOwner(5L, new JointAccountInviteRequestDto(2L), 99L)
        );

        verify(jointAccountMemberRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldThrowException_whenAccountClosed() {

        BankAccount account = mock(BankAccount.class);
        when(account.getStatus()).thenReturn(AccountStatus.CLOSED);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStateException.class, () ->
            jointAccountService.addCoOwner(5L, new JointAccountInviteRequestDto(2L), 99L)
        );

        verify(jointAccountMemberRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldThrowException_whenAlreadyHasCoOwner() {

        BankAccount account = mock(BankAccount.class);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.countByBankAccount_Id(5L)).thenReturn(2);

        assertThrows(DuplicateResourceException.class, () ->
            jointAccountService.addCoOwner(5L, new JointAccountInviteRequestDto(2L), 99L)
        );

        verify(jointAccountMemberRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldThrowException_whenUserNotFound() {

        BankAccount account = mock(BankAccount.class);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.countByBankAccount_Id(5L)).thenReturn(1);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            jointAccountService.addCoOwner(5L, new JointAccountInviteRequestDto(2L), 99L)
        );

        verify(jointAccountMemberRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldThrowException_whenUserAlreadyMember() {

        BankAccount account = mock(BankAccount.class);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        User coOwnerUser = mock(User.class);
        when(coOwnerUser.getId()).thenReturn(2L);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.countByBankAccount_Id(5L)).thenReturn(1);
        when(userRepository.findById(2L)).thenReturn(Optional.of(coOwnerUser));
        when(jointAccountMemberRepository.existsByBankAccount_IdAndUser_Id(5L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
            jointAccountService.addCoOwner(5L, new JointAccountInviteRequestDto(2L), 99L)
        );

        verify(jointAccountMemberRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void addCoOwner_shouldThrowException_whenAddingPrimaryOwnerAsCoOwner() {

        User primaryUser = mock(User.class);
        when(primaryUser.getId()).thenReturn(1L);

        BankAccount account = mock(BankAccount.class);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        JointAccountMember primary = mock(JointAccountMember.class);
        when(primary.getUser()).thenReturn(primaryUser);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.countByBankAccount_Id(5L)).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(primaryUser));
        when(jointAccountMemberRepository.existsByBankAccount_IdAndUser_Id(5L, 1L)).thenReturn(false);
        when(jointAccountMemberRepository.findByBankAccount_IdAndRole(5L, JointAccountRole.PRIMARY))
            .thenReturn(Optional.of(primary));

        // Trying to add the primary owner (userId=1) as co-owner
        assertThrows(InvalidAccountStateException.class, () ->
            jointAccountService.addCoOwner(5L, new JointAccountInviteRequestDto(1L), 99L)
        );

        verify(jointAccountMemberRepository, never()).save(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }


    // ===================== REMOVE CO-OWNER =====================

    @Test
    void removeCoOwner_shouldDeleteMember_clearJoint_andLogAction() {

        AppUser coOwnerAppUser = mock(AppUser.class);
        when(coOwnerAppUser.getId()).thenReturn(20L);

        User coOwnerUser = mock(User.class);
        when(coOwnerUser.getAppUser()).thenReturn(coOwnerAppUser);

        BankAccount account = mock(BankAccount.class);

        JointAccountMember coOwner = mock(JointAccountMember.class);
        when(coOwner.getUser()).thenReturn(coOwnerUser);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_IdAndRole(5L, JointAccountRole.CO_OWNER))
            .thenReturn(Optional.of(coOwner));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(account);
        when(auditLogService.logAction(eq(99L), eq(20L), eq(ActionType.REMOVE_CO_OWNER)))
            .thenReturn(new AuditLogResponseDto(1L, 99L, 20L, ActionType.REMOVE_CO_OWNER, LocalDateTime.now()));

        jointAccountService.removeCoOwner(5L, 99L);

        verify(jointAccountMemberRepository, times(1)).delete(coOwner);
        verify(account, times(1)).setIsJoint(false);
        verify(bankAccountRepository, times(1)).save(account);
        verify(auditLogService, times(1)).logAction(99L, 20L, ActionType.REMOVE_CO_OWNER);
    }

    @Test
    void removeCoOwner_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            jointAccountService.removeCoOwner(5L, 99L)
        );

        verify(jointAccountMemberRepository, never()).delete(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }

    @Test
    void removeCoOwner_shouldThrowException_whenCoOwnerNotFound() {

        BankAccount account = mock(BankAccount.class);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_IdAndRole(5L, JointAccountRole.CO_OWNER))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            jointAccountService.removeCoOwner(5L, 99L)
        );

        verify(jointAccountMemberRepository, never()).delete(any());
        verify(auditLogService, never()).logAction(any(), any(), any());
    }


    // ===================== GET MEMBERS =====================

    @Test
    void getMembers_shouldReturnMembers_whenAdmin() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getFirstName()).thenReturn("John");
        when(user.getLastName()).thenReturn("Doe");

        BankAccount account = mock(BankAccount.class);
        when(account.getId()).thenReturn(5L);

        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getId()).thenReturn(1L);
        when(member.getBankAccount()).thenReturn(account);
        when(member.getUser()).thenReturn(user);
        when(member.getRole()).thenReturn(JointAccountRole.PRIMARY);
        when(member.getJoinedAt()).thenReturn(LocalDateTime.now());

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_Id(5L)).thenReturn(List.of(member));

        List<JointAccountMemberResponseDto> result = jointAccountService.getMembers(5L, 99L, true);

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals(JointAccountRole.PRIMARY, result.get(0).getRole());
        // Admin skips the membership check
        verify(jointAccountMemberRepository, times(1)).findByBankAccount_Id(5L);
    }

    @Test
    void getMembers_shouldReturnMembers_whenMember() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(10L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getFirstName()).thenReturn("John");
        when(user.getLastName()).thenReturn("Doe");
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = mock(BankAccount.class);
        when(account.getId()).thenReturn(5L);

        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getId()).thenReturn(1L);
        when(member.getBankAccount()).thenReturn(account);
        when(member.getUser()).thenReturn(user);
        when(member.getRole()).thenReturn(JointAccountRole.PRIMARY);
        when(member.getJoinedAt()).thenReturn(LocalDateTime.now());

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        // findByBankAccount_Id called twice — once for isMember check, once for mapping
        when(jointAccountMemberRepository.findByBankAccount_Id(5L)).thenReturn(List.of(member));

        List<JointAccountMemberResponseDto> result = jointAccountService.getMembers(5L, 10L, false);

        assertEquals(1, result.size());
    }

    @Test
    void getMembers_shouldThrowException_whenNotMember() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(10L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        BankAccount account = mock(BankAccount.class);

        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getUser()).thenReturn(user);

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jointAccountMemberRepository.findByBankAccount_Id(5L)).thenReturn(List.of(member));

        // appUserId 999L is not in the member list
        assertThrows(UnauthorizedActionException.class, () ->
            jointAccountService.getMembers(5L, 999L, false)
        );
    }

    @Test
    void getMembers_shouldThrowException_whenAccountNotFound() {

        when(bankAccountRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            jointAccountService.getMembers(5L, 10L, false)
        );

        verify(jointAccountMemberRepository, never()).findByBankAccount_Id(any());
    }


    // ===================== IS MEMBER =====================

    @Test
    void isMember_shouldReturnTrue_whenUserIsMember() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(10L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getUser()).thenReturn(user);

        when(jointAccountMemberRepository.findByBankAccount_Id(5L)).thenReturn(List.of(member));

        assertTrue(jointAccountService.isMember(5L, 10L));
    }

    @Test
    void isMember_shouldReturnFalse_whenUserIsNotMember() {

        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(10L);

        User user = mock(User.class);
        when(user.getAppUser()).thenReturn(appUser);

        JointAccountMember member = mock(JointAccountMember.class);
        when(member.getUser()).thenReturn(user);

        when(jointAccountMemberRepository.findByBankAccount_Id(5L)).thenReturn(List.of(member));

        assertFalse(jointAccountService.isMember(5L, 999L));
    }
}