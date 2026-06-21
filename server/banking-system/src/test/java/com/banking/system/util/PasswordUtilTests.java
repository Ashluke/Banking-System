package com.banking.system.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTests {

    @Test
    void hashPassword_shouldReturnNonNullHash() {

        String hash = PasswordUtil.hashPassword("password123");

        assertNotNull(hash);
        assertFalse(hash.isBlank());
    }

    @Test
    void hashPassword_shouldNotReturnPlainText() {

        String hash = PasswordUtil.hashPassword("password123");

        assertNotEquals("password123", hash);
    }

    @Test
    void hashPassword_shouldProduceDifferentHashes_forSamePassword() {

        String hash1 = PasswordUtil.hashPassword("password123");
        String hash2 = PasswordUtil.hashPassword("password123");

        // BCrypt uses a random salt each time, so hashes should differ
        assertNotEquals(hash1, hash2);
    }

    @Test
    void matches_shouldReturnTrue_whenPasswordIsCorrect() {

        String hash = PasswordUtil.hashPassword("password123");

        assertTrue(PasswordUtil.matches("password123", hash));
    }

    @Test
    void matches_shouldReturnFalse_whenPasswordIsIncorrect() {

        String hash = PasswordUtil.hashPassword("password123");

        assertFalse(PasswordUtil.matches("wrongPassword", hash));
    }

    @Test
    void matches_shouldReturnFalse_whenPasswordIsEmpty() {

        String hash = PasswordUtil.hashPassword("password123");

        assertFalse(PasswordUtil.matches("", hash));
    }

    @Test
    void matches_shouldBeCaseSensitive() {

        String hash = PasswordUtil.hashPassword("Password123");

        assertFalse(PasswordUtil.matches("password123", hash));
    }
}