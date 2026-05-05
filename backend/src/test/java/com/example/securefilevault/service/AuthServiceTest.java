package com.example.securefilevault.service;

import com.example.securefilevault.dto.AuthResponse;
import com.example.securefilevault.dto.RegisterRequest;
import com.example.securefilevault.exception.BusinessException;
import com.example.securefilevault.mapper.UserMapper;
import com.example.securefilevault.model.User;
import com.example.securefilevault.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerFailsWhenInviteCodeIsNotConfigured() {
        UserMapper userMapper = mock(UserMapper.class);
        AuthService authService = new AuthService(
                userMapper,
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                ""
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(registerRequest("demo-user", "right-code"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerFailsWhenInviteCodeDoesNotMatch() {
        UserMapper userMapper = mock(UserMapper.class);
        AuthService authService = new AuthService(
                userMapper,
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                "right-code"
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(registerRequest("demo-user", "wrong-code"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerSucceedsWhenInviteCodeMatches() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userMapper, passwordEncoder, jwtService, "right-code");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("demo-user");
        savedUser.setPasswordHash("encoded-password");
        savedUser.setRole("USER");

        when(userMapper.findByUsername("demo-user")).thenReturn(null, savedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest("demo-user", "right-code"));

        assertEquals("jwt-token", response.getToken());
        assertEquals("demo-user", response.getUser().getUsername());
        verify(userMapper).insert(any(User.class));
    }

    private RegisterRequest registerRequest(String username, String inviteCode) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("password123");
        request.setInviteCode(inviteCode);
        return request;
    }
}
