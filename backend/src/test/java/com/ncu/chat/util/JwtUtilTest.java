package com.ncu.chat.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-unit-testing-256bits!!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateToken_success() {
        String token = jwtUtil.generateToken(1L, "testuser");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parseToken_success() {
        String token = jwtUtil.generateToken(1L, "testuser");
        Claims claims = jwtUtil.parseToken(token);
        
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
        assertEquals("testuser", claims.get("username"));
    }

    @Test
    void getUserIdFromToken_success() {
        String token = jwtUtil.generateToken(1L, "testuser");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        assertEquals(1L, userId);
    }

    @Test
    void getUsernameFromToken_success() {
        String token = jwtUtil.generateToken(1L, "testuser");
        String username = jwtUtil.getUsernameFromToken(token);
        
        assertEquals("testuser", username);
    }

    @Test
    void validateToken_validToken() {
        String token = jwtUtil.generateToken(1L, "testuser");
        boolean isValid = jwtUtil.validateToken(token);
        
        assertTrue(isValid);
    }

    @Test
    void validateToken_invalidToken() {
        boolean isValid = jwtUtil.validateToken("invalid.token.here");
        
        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_notExpired() {
        String token = jwtUtil.generateToken(1L, "testuser");
        boolean isExpired = jwtUtil.isTokenExpired(token);
        
        assertFalse(isExpired);
    }
}
