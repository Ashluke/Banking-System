package com.banking.system.services;

import com.banking.system.dto.request.JointAccountInviteRequestDto;
import com.banking.system.dto.response.JointAccountMemberResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.InvalidAccountStateException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.ActionType;
import com.banking.system.model.enums.JointAccountRole;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.JointAccountMemberRepository;
import com.banking.system.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JointAccountService {

    private final JointAccountMemberRepository jointAccountMemberRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public JointAccountService(
            JointAccountMemberRepository jointAccountMemberRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.jointAccountMemberRepository = jointAccountMemberRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    // Add co-owner (admin only — customer must visit branch)
    @Transactional
    public JointAccountMemberResponseDto addCoOwner(Long accountId, JointAccountInviteRequestDto request, Long adminAppUserId) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException("Cannot add co-owner to a closed account");
        }

        if (jointAccountMemberRepository.countByBankAccount_Id(accountId) >= 2) {
            throw new DuplicateResourceException("This account already has a co-owner");
        }

        User coOwner = userRepository.findById(request.getCoOwnerUserId()).orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

        if (jointAccountMemberRepository.existsByBankAccount_IdAndUser_Id(accountId, coOwner.getId())) {
            throw new DuplicateResourceException("User is already a member of this account");
        }

        // Prevent adding the primary owner as co-owner
        JointAccountMember primary = jointAccountMemberRepository
            .findByBankAccount_IdAndRole(accountId, JointAccountRole.PRIMARY)
            .orElseThrow(() -> new ResourceNotFoundException("Primary owner not found"));

        if (coOwner.getId().equals(primary.getUser().getId())) {
            throw new InvalidAccountStateException("Cannot add the primary owner as co-owner");
        }

        JointAccountMember member = new JointAccountMember(account, coOwner, JointAccountRole.CO_OWNER);
        JointAccountMember saved = jointAccountMemberRepository.save(member);

        account.setIsJoint(true);
        bankAccountRepository.save(account);

        auditLogService.logAction(adminAppUserId, coOwner.getAppUser().getId(), ActionType.ADD_CO_OWNER);

        return mapToResponse(saved);
    }

    // Remove co-owner
    @Transactional
    public void removeCoOwner(Long accountId, Long adminAppUserId) {

        BankAccount account = bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        JointAccountMember coOwner = jointAccountMemberRepository
            .findByBankAccount_IdAndRole(accountId, JointAccountRole.CO_OWNER)
            .orElseThrow(() -> new ResourceNotFoundException("Co-owner not found"));

        Long coOwnerAppUserId = coOwner.getUser().getAppUser().getId();

        jointAccountMemberRepository.delete(coOwner);

        account.setIsJoint(false);
        bankAccountRepository.save(account);

        auditLogService.logAction(adminAppUserId, coOwnerAppUserId, ActionType.REMOVE_CO_OWNER);
    }

    // Get members
    public List<JointAccountMemberResponseDto> getMembers(Long accountId, Long appUserId, boolean isAdmin) {

        bankAccountRepository.findById(accountId).orElseThrow(() ->
            new ResourceNotFoundException("Account not found"));

        if (!isAdmin && !isMember(accountId, appUserId)) {
            throw new UnauthorizedActionException("You do not have access to this account");
        }

        return jointAccountMemberRepository.findByBankAccount_Id(accountId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    // Check if user is a member
    public boolean isMember(Long accountId, Long appUserId) {

        return jointAccountMemberRepository.findByBankAccount_Id(accountId)
            .stream()
            .anyMatch(m -> m.getUser().getAppUser().getId().equals(appUserId));
    }

    // Mapper
    private JointAccountMemberResponseDto mapToResponse(JointAccountMember member) {

        return new JointAccountMemberResponseDto(
            member.getId(),
            member.getBankAccount().getId(),
            member.getUser().getId(),
            member.getUser().getFirstName(),
            member.getUser().getLastName(),
            member.getRole(),
            member.getJoinedAt()
        );
    }
}