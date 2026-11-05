package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User newUser() {
        return new User("chandan", "c@x.io", "plain-password", Role.CUSTOMER,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void register_encodesPasswordAndSaves_whenUsernameAndEmailAreFree() {
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("c@x.io")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser(newUser());

        assertEquals("chandan", saved.getUsername());
        assertNotEquals("plain-password", saved.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("plain-password", saved.getPassword()));
        verify(userRepository).save(saved);
    }

    @Test
    void register_throws_whenUsernameAlreadyTaken() {
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.of(newUser()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.registerUser(newUser()));

        assertEquals("Username is already taken", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyRegistered() {
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("c@x.io")).thenReturn(Optional.of(newUser()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.registerUser(newUser()));

        assertEquals("Email is already registered", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
