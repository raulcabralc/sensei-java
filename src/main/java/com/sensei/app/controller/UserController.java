package com.sensei.app.controller;

import com.sensei.app.dtos.CreateUserDTO;
import com.sensei.app.dtos.ResponseUserDTO;
import com.sensei.app.dtos.UpdateUserDTO;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.model.User;
import com.sensei.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public List<User> home() {
        return this.userService.index();
    }

    @PostMapping("/create")
    public ResponseUserDTO createUser(@Valid @RequestBody CreateUserDTO userDTO) throws EmailAlreadyExistsException {
        return this.userService.createUser(userDTO);
    }

    @PatchMapping("/update/{id}")
    public ResponseUserDTO updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserDTO userDTO
    ) throws EmailAlreadyExistsException {
        return this.userService.updateUser(id, userDTO);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable String id) {
        this.userService.deleteUser(id);
    }
}
