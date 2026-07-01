package com.banking.system.repository;

import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.enums.JointAccountRole;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JointAccountMemberRepository extends JpaRepository<JointAccountMember, Long> {

    List<JointAccountMember> findByBankAccount_Id(Long bankAccountId);

    Optional<JointAccountMember> findByBankAccount_IdAndUser_Id(Long bankAccountId, Long userId);

    boolean existsByBankAccount_IdAndUser_Id(Long bankAccountId, Long userId);

    Optional<JointAccountMember> findByBankAccount_IdAndRole(Long bankAccountId, JointAccountRole role);

    int countByBankAccount_Id(Long bankAccountId);
}