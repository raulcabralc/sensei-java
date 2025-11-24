package com.sensei.app.controller;

import com.sensei.app.dtos.JwtResponseDTO;
import com.sensei.app.dtos.LoginRequestDTO;
import com.sensei.app.security.jwt.JwtTokenProvider;
import com.sensei.app.service.AuthService;
import com.sensei.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private AuthenticationManager authManager;
    private JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return this.authService.login(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());
    }
}
