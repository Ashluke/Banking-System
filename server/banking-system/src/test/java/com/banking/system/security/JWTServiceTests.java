package com.banking.system.security;

import com.banking.system.model.enums.Role;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.security.Key;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import static org.junit.jupiter.api.Assertions.*;

public class JWTServiceTests {

    private final JWTService jwtService = new JWTService();


    @Test
    void generatedToken_shouldProduceNonNullToken() {

        String token = jwtService.generatedToken(1L, "test@example.com", Role.USER);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractAppUserId_shouldReturnCorrectId() {

        String token = jwtService.generatedToken(42L, "test@example.com", Role.USER);

        Long extractedId = jwtService.extractAppUserId(token);

        assertEquals(42L, extractedId);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {

        String token = jwtService.generatedToken(1L, "admin@example.com", Role.ADMIN);

        Role extractedRole = jwtService.extractRole(token);

        assertEquals(Role.ADMIN, extractedRole);
    }

    @Test
    void isValid_shouldReturnTrue_forValidToken() {

        String token = jwtService.generatedToken(1L, "test@example.com", Role.USER);

        assertTrue(jwtService.isValid(token));
    }

    @Test
    void isValid_shouldReturnFalse_forMalformedToken() {

        assertFalse(jwtService.isValid("not.a.valid.token"));
    }

    @Test
    void isValid_shouldReturnFalse_forTamperedToken() {

        String token = jwtService.generatedToken(1L, "test@example.com", Role.USER);

        // Flip a character in the payload to corrupt the signature
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtService.isValid(tampered));
    }

    @Test
    void isValid_shouldReturnFalse_forTokenSignedWithDifferentKey() {

        Key differentKey = Keys.hmacShaKeyFor(
            "a-completely-different-secret-key-that-is-long-enough".getBytes()
        );

        String foreignToken = Jwts.builder()
            .setSubject("test@example.com")
            .claim("role", "USER")
            .claim("appUserId", 1L)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 100000))
            .signWith(differentKey, SignatureAlgorithm.HS256)
            .compact();

        assertFalse(jwtService.isValid(foreignToken));
    }

    @Test
    void isValid_shouldReturnFalse_forExpiredToken() {

        // Build a token manually with the same secret JWTService uses, but already expired
        Key key = Keys.hmacShaKeyFor("bankingsystemsupersecretjwtkey2026secure".getBytes());

        String expiredToken = Jwts.builder()
            .setSubject("test@example.com")
            .claim("role", "USER")
            .claim("appUserId", 1L)
            .setIssuedAt(new Date(System.currentTimeMillis() - 1000000))
            .setExpiration(new Date(System.currentTimeMillis() - 500000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        assertFalse(jwtService.isValid(expiredToken));
    }

    @Test
    void extractAppUserId_shouldThrowException_whenTokenInvalid() {

        assertThrows(Exception.class, () ->
            jwtService.extractAppUserId("not.a.valid.token")
        );
    }
}