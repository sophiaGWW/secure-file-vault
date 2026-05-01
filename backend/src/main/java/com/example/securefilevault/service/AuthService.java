package com.example.securefilevault.service;

import com.example.securefilevault.dto.AuthResponse;
import com.example.securefilevault.dto.LoginRequest;
import com.example.securefilevault.dto.RegisterRequest;
import com.example.securefilevault.dto.UserResponse;
import com.example.securefilevault.exception.BusinessException;
import com.example.securefilevault.mapper.UserMapper;
import com.example.securefilevault.model.User;
import com.example.securefilevault.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        userMapper.insert(user);

        User savedUser = userMapper.findByUsername(username);
        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token, UserResponse.from(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }
}
