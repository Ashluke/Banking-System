package com.banking.system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.banking.system.model.enums.Role;

import java.security.Key;
import java.util.Date;

@Service
public class JWTService {
    
    @Value("${jwt.secret}")
    private String SECRET;

    private Key key;

    private final long EXPIRATION = 1000 * 60 * 60 * 10;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generatedToken(Long appUserId, String email, Role role) {

        return Jwts.builder()
            .setSubject(email)
            .claim("role", role.name())
            .claim("appUserId", appUserId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public Long extractAppUserId(String token) {

        return parse(token)
            .getBody()
            .get("appUserId", Long.class);
    }

    public Role extractRole(String token) {
        String roleStr = parse(token).getBody().get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } 
        
        catch (Exception e) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token);
    }
}
