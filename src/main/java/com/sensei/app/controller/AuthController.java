package com.sensei.app.controller;

import com.sensei.app.dtos.CreateUserDTO;
import com.sensei.app.dtos.JwtResponseDTO;
import com.sensei.app.dtos.LoginRequestDTO;
import com.sensei.app.dtos.ResponseUserDTO;
import com.sensei.app.service.AuthService;
import com.sensei.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return this.authService.login(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());
    }

    /**
     * Cadastro público de professores e alunos. Único fluxo de criação de contas.
     */
    @PostMapping("/register")
    public ResponseEntity<ResponseUserDTO> register(@Valid @RequestBody CreateUserDTO userDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.createUser(userDTO));
    }
}
