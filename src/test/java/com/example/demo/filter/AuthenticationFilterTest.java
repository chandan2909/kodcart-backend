package com.example.demo.filter;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private PrintWriter writer;

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AuthenticationFilter(authService, userRepository);
        lenient().when(response.getWriter()).thenReturn(writer);
    }

    private User user(int id, Role role) {
        return new User(id, role == Role.ADMIN ? "admin" : "chandan", "x@x.io", "hash", role,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void allowsUnauthenticatedPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void handlesPreflightWithCorsHeaders() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("Access-Control-Allow-Origin", "http://localhost:5174");
        verify(response).setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        verify(response).setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(writer).write("Unauthorized: Invalid or missing token");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("authToken", "bad") });
        when(authService.validateToken("bad")).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsTokenForMissingUser() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("authToken", "good") });
        when(authService.validateToken("good")).thenReturn(true);
        when(authService.extractUsername("good")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(writer).write("Unauthorized: User not found");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void forbidsCustomersFromAdminPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/admin/business/overall");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("authToken", "good") });
        when(authService.validateToken("good")).thenReturn(true);
        when(authService.extractUsername("good")).thenReturn("chandan");
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.of(user(1, Role.CUSTOMER)));

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(writer).write("Forbidden: Admin access required");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void forbidsAdminsFromCustomerPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/cart/items");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("authToken", "good") });
        when(authService.validateToken("good")).thenReturn(true);
        when(authService.extractUsername("good")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(2, Role.ADMIN)));

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(writer).write("Forbidden: Customer access required");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void authorizesCustomerOnCustomerPath() throws Exception {
        User customer = user(1, Role.CUSTOMER);
        when(request.getRequestURI()).thenReturn("/api/cart/items");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("authToken", "good") });
        when(authService.validateToken("good")).thenReturn(true);
        when(authService.extractUsername("good")).thenReturn("chandan");
        when(userRepository.findByUsername("chandan")).thenReturn(Optional.of(customer));

        filter.doFilter(request, response, chain);

        verify(request).setAttribute("authenticatedUser", customer);
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void authorizesAdminOnAdminPath() throws Exception {
        User admin = user(2, Role.ADMIN);
        when(request.getRequestURI()).thenReturn("/admin/business/overall");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("authToken", "good") });
        when(authService.validateToken("good")).thenReturn(true);
        when(authService.extractUsername("good")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        filter.doFilter(request, response, chain);

        verify(request).setAttribute("authenticatedUser", admin);
        verify(chain).doFilter(request, response);
    }
}
