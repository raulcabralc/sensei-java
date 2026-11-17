package com.sensei.app.controller;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.MaterialResponseDTO;
import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.model.Material;
import com.sensei.app.security.AuthenticatedUser;
import com.sensei.app.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /**
     * Professor responsável: todos os materiais da turma. Aluno matriculado: somente APPROVED.
     */
    @GetMapping("/{classId}")
    public ResponseEntity<List<Material>> index(
            @PathVariable("classId") String classId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.materialService.index(classId, currentUser));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MaterialResponseDTO> createMaterial(
            @Valid @RequestBody CreateMaterialDTO materialDTO,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.materialService.createMaterial(materialDTO, currentUser));
    }

    @PostMapping("/status/{materialId}/{status}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MaterialResponseDTO> changeStatus(
            @PathVariable("materialId") String materialId,
            @PathVariable("status") MaterialStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.materialService.changeStatus(materialId, status, currentUser));
    }
}
