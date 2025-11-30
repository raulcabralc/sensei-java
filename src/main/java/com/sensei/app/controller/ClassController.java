package com.sensei.app.controller;

import com.sensei.app.dtos.CreateClassDTO;
import com.sensei.app.dtos.UpdateClassDTO;
import com.sensei.app.model.Class;
import com.sensei.app.service.ClassService;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class")
public class ClassController {
    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping("/")
    public ResponseEntity<List<com.sensei.app.model.Class>> index() throws BadRequestException {
        try {
            List<com.sensei.app.model.Class> classes = this.classService.index();

            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Class> createClass(
            @RequestBody CreateClassDTO classDTO
    ) throws BadRequestException {
        try {
            Class classCreated = this.classService.createClass(classDTO);

            return ResponseEntity.ok(classCreated);
        } catch(Exception e) {
            throw new BadRequestException(e);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<Class> updateClass(
            @RequestBody UpdateClassDTO classDTO
    ) throws BadRequestException {
        try {
            Class classCreated = this.classService.updateClass(classDTO);

            return ResponseEntity.ok(classCreated);
        } catch(Exception e) {
            throw new BadRequestException(e);
        }
    }
}
