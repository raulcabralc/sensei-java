package com.sensei.app.controller;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.MaterialResponseDTO;
import com.sensei.app.service.MaterialService;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/material")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
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
}
