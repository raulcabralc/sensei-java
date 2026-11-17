package com.sensei.app.controller;

import com.sensei.app.dtos.ResponseUserDTO;
import com.sensei.app.dtos.UpdateUserDTO;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.security.AuthenticatedUser;
import com.sensei.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lista de usuários (sem dados sensíveis). Restrita a professores,
     * que precisam dos IDs de alunos para montar turmas.
     */
    @GetMapping("/")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ResponseUserDTO> home() {
        return this.userService.index();
    }

    /**
     * Dados do próprio usuário autenticado, incluindo o ID persistido.
     */
    @GetMapping("/me")
    public ResponseUserDTO me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return this.userService.getUser(currentUser.getId());
    }

    @PatchMapping("/update/{id}")
    public ResponseUserDTO updateUser(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserDTO userDTO
    ) throws EmailAlreadyExistsException {
        return this.userService.updateUser(id, currentUser.getId(), userDTO);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        this.userService.deleteUser(id, currentUser.getId());
    }
}
