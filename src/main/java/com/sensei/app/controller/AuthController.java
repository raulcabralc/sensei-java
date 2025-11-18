package com.sensei.app.controller;

import com.sensei.app.service.UserService;
import org.springframework.stereotype.Controller;

@Controller("/auth")
public class AuthController {
    private UserService userService;

//    @PostMapping("/login")
//    public String login(@RequestParam("username") String username, @RequestParam("password") String password) {
//
//
//        return;
//    }
}
