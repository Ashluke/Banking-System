package com.banking.system.services;

import com.banking.system.dto.request.StockHoldingRequestDto;
import com.banking.system.dto.response.StockHoldingResponseDto;
import com.banking.system.exception.DuplicateResourceException;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.exception.UnauthorizedActionException;
import com.banking.system.model.entities.StockHolding;
import com.banking.system.model.entities.User;
import com.banking.system.repository.StockHoldingRepository;
import com.banking.system.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class StockHoldingService {

    private final StockHoldingRepository stockHoldingRepository;
    private final UserRepository userRepository;

    public StockHoldingService(StockHoldingRepository stockHoldingRepository, UserRepository userRepository) {
        this.stockHoldingRepository = stockHoldingRepository;
        this.userRepository = userRepository;
    }

    // Add holding
    public StockHoldingResponseDto addHolding(StockHoldingRequestDto request) {

        User user = userRepository.findById(request.getUserId()).orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

        if (stockHoldingRepository.existsByUser_IdAndSymbol(user.getId(), request.getSymbol().toUpperCase())) {
            throw new DuplicateResourceException("Holding for " + request.getSymbol() + " already exists");
        }

        StockHolding holding = new StockHolding(
            user,
            request.getSymbol().toUpperCase(),
            request.getQuantity(),
            request.getPurchasePrice()
        );

        StockHolding saved = stockHoldingRepository.save(holding);

        return mapToResponse(saved);
    }

    // Get by user id
    public Page<StockHoldingResponseDto> getByUserId(Long userId, Pageable pageable) {

        userRepository.findById(userId).orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

        return stockHoldingRepository.findByUser_Id(userId, pageable)
            .map(this::mapToResponse);
    }

    // Get by id
    public StockHoldingResponseDto getById(Long id, Long appUserId) {

        StockHolding holding = stockHoldingRepository.findById(id).orElseThrow(() ->
            new ResourceNotFoundException("Holding not found"));

        if(!holding.getUser().getAppUser().getId().equals(appUserId)) {
            throw new UnauthorizedActionException("You do not own this stock");
        }

        return mapToResponse(holding);
    }

    // Update holding (quantity and purchase price only)
    public StockHoldingResponseDto updateHolding(Long id, StockHoldingRequestDto request) {

        StockHolding holding = stockHoldingRepository.findById(id).orElseThrow(() ->
            new ResourceNotFoundException("Holding not found"));

        holding.setQuantity(request.getQuantity());
        holding.setPurchasePrice(request.getPurchasePrice());

        StockHolding updated = stockHoldingRepository.save(holding);

        return mapToResponse(updated);
    }

    // Delete holding
    public void deleteHolding(Long id) {

        StockHolding holding = stockHoldingRepository.findById(id).orElseThrow(() ->
            new ResourceNotFoundException("Holding not found"));

        stockHoldingRepository.delete(holding);
    }

    // Mapper
    private StockHoldingResponseDto mapToResponse(StockHolding holding) {

        return new StockHoldingResponseDto(
            holding.getId(),
            holding.getUser().getId(),
            holding.getSymbol(),
            holding.getQuantity(),
            holding.getPurchasePrice(),
            holding.getPurchasedAt()
        );
    }
}