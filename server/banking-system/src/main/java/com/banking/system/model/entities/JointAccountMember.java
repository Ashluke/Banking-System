package com.banking.system.model.entities;

import com.banking.system.model.enums.JointAccountRole;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "joint_account_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"bank_account_id", "user_id"}))
public class JointAccountMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JointAccountRole role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public JointAccountMember() {}

    public JointAccountMember(BankAccount bankAccount, User user, JointAccountRole role) {
        this.bankAccount = bankAccount;
        this.user = user;
        this.role = role;
    }

    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }
    public void setUser(User user) { this.user = user; }
    public void setRole(JointAccountRole role) { this.role = role; }

    public Long getId() { return id; }
    public BankAccount getBankAccount() { return bankAccount; }
    public User getUser() { return user; }
    public JointAccountRole getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}