package com.sensei.app.controller;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.MaterialResponseDTO;
import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.model.Material;
import com.sensei.app.service.MaterialService;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/{classId}")
    public ResponseEntity<List<Material>> index(
            @PathVariable("classId") String classId
    ) throws BadRequestException {
        try {
            List<Material> materials = this.materialService.index(classId);

            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<MaterialResponseDTO> createMaterial(
            @Valid @RequestBody CreateMaterialDTO materialDTO
    ) throws BadRequestException {
        try {
            MaterialResponseDTO material = this.materialService.createMaterial(materialDTO);

            return ResponseEntity.ok(material);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @PostMapping("/status/{materialId}/{status}")
    public ResponseEntity<MaterialResponseDTO> changeStatus(
            @PathVariable("materialId") String materialId,
            @PathVariable("status") MaterialStatus status
    ) throws BadRequestException {
        try {
            MaterialResponseDTO material = this.materialService.changeStatus(materialId, status);

            return ResponseEntity.ok(material);
        } catch (Exception e) {
            throw new BadRequestException(e.getCause());
        }
    }
}
