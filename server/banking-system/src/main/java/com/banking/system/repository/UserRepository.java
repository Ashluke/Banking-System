package com.banking.system.repository;

import com.banking.system.model.entities.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByAppUser_Id(Long appUserId);

    boolean existsByAppUser_Id(Long appUserId);

    Page<User> findAll(Pageable pageable);
}
