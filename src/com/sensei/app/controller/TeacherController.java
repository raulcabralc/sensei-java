package com.sensei.app.controller;

@RestController
@RequestMapping("/teacher")
public class TeacherController {
    @GetMapping()
    String home() {
        return "Teacher Home";
    }
}
