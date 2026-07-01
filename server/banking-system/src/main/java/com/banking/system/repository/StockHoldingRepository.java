package com.banking.system.repository;

import com.banking.system.model.entities.StockHolding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockHoldingRepository extends JpaRepository<StockHolding, Long> {

    List<StockHolding> findByUser_Id(Long userId);

    Page<StockHolding> findByUser_Id(Long userId, Pageable pageable);

    boolean existsByUser_IdAndSymbol(Long userId, String symbol);
}