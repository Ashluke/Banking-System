package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.BankAccount;
import com.banking.system.model.entities.JointAccountMember;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.AccountStatus;
import com.banking.system.model.enums.JointAccountRole;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class JointAccountMemberRepositoryTests {

    @Autowired
    private JointAccountMemberRepository jointAccountMemberRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    private User createUser(String email) {

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash("hashed");
        appUser.setRole(Role.USER);
        appUser = appUserRepository.save(appUser);

        User user = new User();
        user.setAppUser(appUser);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("0917" + System.nanoTime() % 1000000000L);
        user.setAddress("123 Main St");

        return userRepository.save(user);
    }

    private BankAccount createAccount(User user) {
        BankAccount account = new BankAccount(user, BigDecimal.ZERO, AccountStatus.ACTIVE);
        return bankAccountRepository.save(account);
    }


    @Test
    void findByBankAccount_Id_shouldReturnAllMembers_forThatAccount() {

        User owner = createUser("owner1@example.com");
        User coOwner = createUser("coowner1@example.com");
        BankAccount account = createAccount(owner);

        jointAccountMemberRepository.save(new JointAccountMember(account, owner, JointAccountRole.PRIMARY));
        jointAccountMemberRepository.save(new JointAccountMember(account, coOwner, JointAccountRole.CO_OWNER));

        List<JointAccountMember> result = jointAccountMemberRepository.findByBankAccount_Id(account.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(m -> m.getUser().getId())
            .containsExactlyInAnyOrder(owner.getId(), coOwner.getId());
    }

    @Test
    void findByBankAccount_Id_shouldReturnEmpty_whenNoMembers() {

        User user = createUser("owner2@example.com");
        BankAccount account = createAccount(user);

        List<JointAccountMember> result = jointAccountMemberRepository.findByBankAccount_Id(account.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByBankAccount_IdAndUser_Id_shouldReturnMember_whenExists() {

        User user = createUser("owner3@example.com");
        BankAccount account = createAccount(user);

        jointAccountMemberRepository.save(new JointAccountMember(account, user, JointAccountRole.PRIMARY));

        Optional<JointAccountMember> result = jointAccountMemberRepository
            .findByBankAccount_IdAndUser_Id(account.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(JointAccountRole.PRIMARY);
    }

    @Test
    void findByBankAccount_IdAndUser_Id_shouldReturnEmpty_whenNotFound() {

        User user = createUser("owner4@example.com");
        BankAccount account = createAccount(user);

        Optional<JointAccountMember> result = jointAccountMemberRepository
            .findByBankAccount_IdAndUser_Id(account.getId(), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void existsByBankAccount_IdAndUser_Id_shouldReturnTrue_whenMemberExists() {

        User user = createUser("owner5@example.com");
        BankAccount account = createAccount(user);

        jointAccountMemberRepository.save(new JointAccountMember(account, user, JointAccountRole.PRIMARY));

        boolean exists = jointAccountMemberRepository.existsByBankAccount_IdAndUser_Id(account.getId(), user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByBankAccount_IdAndUser_Id_shouldReturnFalse_whenNotFound() {

        User user = createUser("owner6@example.com");
        BankAccount account = createAccount(user);

        boolean exists = jointAccountMemberRepository.existsByBankAccount_IdAndUser_Id(account.getId(), user.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void findByBankAccount_IdAndRole_shouldReturnPrimaryOwner() {

        User owner = createUser("owner7@example.com");
        User coOwner = createUser("coowner7@example.com");
        BankAccount account = createAccount(owner);

        jointAccountMemberRepository.save(new JointAccountMember(account, owner, JointAccountRole.PRIMARY));
        jointAccountMemberRepository.save(new JointAccountMember(account, coOwner, JointAccountRole.CO_OWNER));

        Optional<JointAccountMember> result = jointAccountMemberRepository
            .findByBankAccount_IdAndRole(account.getId(), JointAccountRole.PRIMARY);

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(owner.getId());
    }

    @Test
    void countByBankAccount_Id_shouldReturnCorrectCount() {

        User owner = createUser("owner8@example.com");
        User coOwner = createUser("coowner8@example.com");
        BankAccount account = createAccount(owner);

        jointAccountMemberRepository.save(new JointAccountMember(account, owner, JointAccountRole.PRIMARY));
        jointAccountMemberRepository.save(new JointAccountMember(account, coOwner, JointAccountRole.CO_OWNER));

        int count = jointAccountMemberRepository.countByBankAccount_Id(account.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void save_shouldThrowException_whenDuplicateMember() {

        User user = createUser("owner9@example.com");
        BankAccount account = createAccount(user);

        jointAccountMemberRepository.save(new JointAccountMember(account, user, JointAccountRole.PRIMARY));

        assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> {
                jointAccountMemberRepository.save(new JointAccountMember(account, user, JointAccountRole.CO_OWNER));
                jointAccountMemberRepository.flush();
            }
        );
    }
}