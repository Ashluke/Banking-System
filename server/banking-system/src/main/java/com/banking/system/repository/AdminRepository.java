package com.banking.system.repository;

import com.banking.system.model.entities.Admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    Optional<Admin> findByAppUser_Id(Long appUserId);

    boolean existsByAppUser_Id(Long appUserId);

    Page<Admin> findAll(Pageable pageable);
}
