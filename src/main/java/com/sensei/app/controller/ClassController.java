package com.sensei.app.controller;

import com.sensei.app.dtos.CreateClassDTO;
import com.sensei.app.dtos.UpdateClassDTO;
import com.sensei.app.model.Class;
import com.sensei.app.security.AuthenticatedUser;
import com.sensei.app.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class")
public class ClassController {
    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * Professor: suas turmas. Aluno: turmas em que está matriculado.
     */
    @GetMapping("/")
    public ResponseEntity<List<Class>> index(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(this.classService.index(currentUser));
    }

    /**
     * Turma do professor responsável ou de um aluno matriculado; 404 nos demais casos.
     */
    @GetMapping("/{classId}")
    public ResponseEntity<Class> getClassById(
            @PathVariable("classId") String classId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.classService.getVisibleClass(classId, currentUser));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Class> createClass(
            @Valid @RequestBody CreateClassDTO classDTO,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.classService.createClass(classDTO, currentUser));
    }

    @PatchMapping("/update/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Class> updateClass(
            @PathVariable("classId") String classId,
            @Valid @RequestBody UpdateClassDTO classDTO,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.classService.updateClass(classId, classDTO, currentUser));
    }

    @DeleteMapping("/delete/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteClass(
            @PathVariable("classId") String classId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        this.classService.deleteClass(classId, currentUser);
    }
}
