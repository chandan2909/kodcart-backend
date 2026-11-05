package com.example.demo.adminservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.JWTTokenRepository;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTTokenRepository jwtTokenRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User customer(int id) {
        return new User(id, "chandan", "c@x.io", "hash", Role.CUSTOMER,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void modifyUser_throws_whenUserMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.modifyUser(1, "new", null, null));

        assertEquals("User not found", ex.getMessage());
        verify(jwtTokenRepository, never()).deleteByUserId(anyInt());
    }

    @Test
    void modifyUser_updatesProvidedFieldsAndInvalidatesTokens() {
        User user = customer(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = adminUserService.modifyUser(1, "chandan2", "new@x.io", "ADMIN");

        assertEquals("chandan2", updated.getUsername());
        assertEquals("new@x.io", updated.getEmail());
        assertEquals(Role.ADMIN, updated.getRole());
        verify(jwtTokenRepository).deleteByUserId(1);
        verify(userRepository).save(user);
    }

    @Test
    void modifyUser_keepsFieldsWhenArgumentsBlank() {
        User user = customer(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = adminUserService.modifyUser(1, "", "", "");

        assertEquals("chandan", updated.getUsername());
        assertEquals("c@x.io", updated.getEmail());
        assertEquals(Role.CUSTOMER, updated.getRole());
    }

    @Test
    void modifyUser_throws_whenRoleInvalid() {
        User user = customer(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.modifyUser(1, null, null, "SUPERUSER"));

        assertEquals("Invalid role: SUPERUSER", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_returnsUser_whenFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer(1)));

        assertEquals("chandan", adminUserService.getUserById(1).getUsername());
    }

    @Test
    void getUserById_throws_whenMissing() {
        when(userRepository.findById(9)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.getUserById(9));

        assertEquals("User not found", ex.getMessage());
    }
}
