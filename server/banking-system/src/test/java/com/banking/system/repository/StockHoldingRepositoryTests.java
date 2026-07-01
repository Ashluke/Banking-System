package com.banking.system.repository;

import com.banking.system.model.entities.AppUser;
import com.banking.system.model.entities.StockHolding;
import com.banking.system.model.entities.User;
import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class StockHoldingRepositoryTests {

    @Autowired
    private StockHoldingRepository stockHoldingRepository;

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


    @Test
    void findByUser_Id_list_shouldReturnAllHoldings_forThatUser() {

        User user = createUser("user1@example.com");
        User otherUser = createUser("user2@example.com");

        stockHoldingRepository.save(new StockHolding(user, "AAPL", new BigDecimal("10.00"), new BigDecimal("150.00")));
        stockHoldingRepository.save(new StockHolding(user, "GOOGL", new BigDecimal("5.00"), new BigDecimal("2800.00")));
        stockHoldingRepository.save(new StockHolding(otherUser, "TSLA", new BigDecimal("3.00"), new BigDecimal("700.00")));

        List<StockHolding> result = stockHoldingRepository.findByUser_Id(user.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(StockHolding::getSymbol)
            .containsExactlyInAnyOrder("AAPL", "GOOGL");
    }

    @Test
    void findByUser_Id_list_shouldReturnEmpty_whenUserHasNoHoldings() {

        User user = createUser("user3@example.com");

        List<StockHolding> result = stockHoldingRepository.findByUser_Id(user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByUser_Id_paged_shouldReturnPagedHoldings() {

        User user = createUser("user4@example.com");

        stockHoldingRepository.save(new StockHolding(user, "AAPL", new BigDecimal("10.00"), new BigDecimal("150.00")));
        stockHoldingRepository.save(new StockHolding(user, "GOOGL", new BigDecimal("5.00"), new BigDecimal("2800.00")));
        stockHoldingRepository.save(new StockHolding(user, "TSLA", new BigDecimal("3.00"), new BigDecimal("700.00")));

        Page<StockHolding> firstPage = stockHoldingRepository.findByUser_Id(user.getId(), PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void existsByUser_IdAndSymbol_shouldReturnTrue_whenHoldingExists() {

        User user = createUser("user5@example.com");

        stockHoldingRepository.save(new StockHolding(user, "AAPL", new BigDecimal("10.00"), new BigDecimal("150.00")));

        boolean exists = stockHoldingRepository.existsByUser_IdAndSymbol(user.getId(), "AAPL");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUser_IdAndSymbol_shouldReturnFalse_whenHoldingNotFound() {

        User user = createUser("user6@example.com");

        boolean exists = stockHoldingRepository.existsByUser_IdAndSymbol(user.getId(), "AAPL");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByUser_IdAndSymbol_shouldReturnFalse_whenSymbolBelongsToDifferentUser() {

        User user = createUser("user7@example.com");
        User otherUser = createUser("user8@example.com");

        stockHoldingRepository.save(new StockHolding(otherUser, "AAPL", new BigDecimal("10.00"), new BigDecimal("150.00")));

        boolean exists = stockHoldingRepository.existsByUser_IdAndSymbol(user.getId(), "AAPL");

        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldPersistStockHolding_withGeneratedIdAndTimestamp() {

        User user = createUser("user9@example.com");

        StockHolding holding = new StockHolding(user, "AAPL", new BigDecimal("10.00"), new BigDecimal("150.00"));

        StockHolding saved = stockHoldingRepository.save(holding);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPurchasedAt()).isNotNull();
        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
    }
}