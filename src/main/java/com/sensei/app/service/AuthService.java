package com.sensei.app.service;

import com.sensei.app.dtos.JwtResponseDTO;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.security.jwt.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AuthenticationManager authManager, JwtTokenProvider jwtTokenProvider) {
        this.authManager = authManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public ResponseEntity<JwtResponseDTO> login(String email, String password) throws EmailAlreadyExistsException {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email, password
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new JwtResponseDTO(jwt));
    }
}
