package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.demo.entity.JWTToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.JWTTokenRepository;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTTokenRepository jwtTokenRepository;

    private AuthService authService;

    private User customer;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtTokenRepository, SECRET);
        customer = new User(1, "chandan", "c@x.io",
                new BCryptPasswordEncoder().encode("secret"), Role.CUSTOMER,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void constructor_throws_whenSecretIsShorterThan64Bytes() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthService(userRepository, jwtTokenRepository, "too-short"));
    }

    @Test
    void authenticate_returnsUser_whenCredentialsAreValid() {
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.of(customer));

        User result = authService.authenticate("chandan", "secret");

        assertEquals(customer, result);
    }

    @Test
    void authenticate_throws_whenPasswordIsWrong() {
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.of(customer));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.authenticate("chandan", "wrong"));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void authenticate_throws_whenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.authenticate("ghost", "x"));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void generateToken_reusesUnexpiredToken() {
        JWTToken existing = new JWTToken(9, customer, "existing-token", LocalDateTime.now().plusHours(1));
        when(jwtTokenRepository.findByUserId(1)).thenReturn(existing);

        String token = authService.generateToken(customer);

        assertEquals("existing-token", token);
        verify(jwtTokenRepository, never()).save(any(JWTToken.class));
        verify(jwtTokenRepository, never()).delete(any(JWTToken.class));
    }

    @Test
    void generateToken_createsNewToken_whenNoTokenStored() {
        when(jwtTokenRepository.findByUserId(1)).thenReturn(null);

        String token = authService.generateToken(customer);

        assertTrue(token != null && !token.isEmpty());
        verify(jwtTokenRepository).save(any(JWTToken.class));
    }

    @Test
    void generateToken_replacesExpiredToken() {
        JWTToken expired = new JWTToken(9, customer, "stale-token", LocalDateTime.now().minusHours(1));
        when(jwtTokenRepository.findByUserId(1)).thenReturn(expired);

        String token = authService.generateToken(customer);

        assertNotEquals("stale-token", token);
        verify(jwtTokenRepository).delete(expired);
        verify(jwtTokenRepository).save(any(JWTToken.class));
    }

    @Test
    void validateToken_returnsTrue_forStoredUnexpiredToken() {
        when(jwtTokenRepository.findByUserId(1)).thenReturn(null);
        String token = authService.generateToken(customer);

        JWTToken stored = new JWTToken(9, customer, token, LocalDateTime.now().plusHours(1));
        when(jwtTokenRepository.findByToken(token)).thenReturn(Optional.of(stored));

        assertTrue(authService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalse_whenTokenNotInDatabase() {
        when(jwtTokenRepository.findByUserId(1)).thenReturn(null);
        String token = authService.generateToken(customer);

        when(jwtTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertFalse(authService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalse_whenStoredTokenExpired() {
        when(jwtTokenRepository.findByUserId(1)).thenReturn(null);
        String token = authService.generateToken(customer);

        JWTToken expired = new JWTToken(9, customer, token, LocalDateTime.now().minusMinutes(1));
        when(jwtTokenRepository.findByToken(token)).thenReturn(Optional.of(expired));

        assertFalse(authService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalse_forGarbageToken() {
        assertFalse(authService.validateToken("not-a-jwt"));
    }

    @Test
    void extractUsername_readsSubjectFromToken() {
        when(jwtTokenRepository.findByUserId(1)).thenReturn(null);
        String token = authService.generateToken(customer);

        assertEquals("chandan", authService.extractUsername(token));
    }

    @Test
    void logout_deletesStoredToken() {
        authService.logout(customer);

        verify(jwtTokenRepository).deleteByUserId(1);
    }
}
