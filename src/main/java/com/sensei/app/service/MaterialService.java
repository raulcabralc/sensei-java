package com.sensei.app.service;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.MaterialResponseDTO;
import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.enums.MaterialType;
import com.sensei.app.exceptions.ClassDoesNotExistException;
import com.sensei.app.exceptions.InvalidRequestException;
import com.sensei.app.exceptions.InvalidStatusTransitionException;
import com.sensei.app.exceptions.MaterialDoesNotExistException;
import com.sensei.app.model.Class;
import com.sensei.app.model.Material;
import com.sensei.app.repository.MaterialRepository;
import com.sensei.app.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Regras de acesso aos materiais:
 * - professor responsável pela turma: cria, lista (todos os status) e aprova;
 * - aluno matriculado: lista somente materiais APPROVED da turma.
 * Única transição de status permitida: WAITING -> APPROVED.
 */
@Service
public class MaterialService {
    private static final String MATERIAL_NOT_FOUND = "Material not found.";

    private final MaterialRepository materialRepository;
    private final ClassService classService;

    public MaterialService(MaterialRepository materialRepository, ClassService classService) {
        this.materialRepository = materialRepository;
        this.classService = classService;
    }

    public List<Material> index(String classId, AuthenticatedUser user) {
        Class clazz = this.classService.getVisibleClass(classId, user);

        if (ClassService.isOwner(clazz, user)) {
            return this.materialRepository.findByClassId(clazz.getId());
        }

        // Aluno matriculado: materiais pendentes nunca são retornados.
        return this.materialRepository.findByClassIdAndStatus(clazz.getId(), MaterialStatus.APPROVED);
    }

    /**
     * Cria um material WAITING em uma turma do professor autenticado.
     * Também usado pelo fluxo de resumo por IA: a autorização vale para ambos.
     */
    public MaterialResponseDTO createMaterial(CreateMaterialDTO materialDTO, AuthenticatedUser user) {
        AccessChecks.requireTeacher(user);
        AccessChecks.assertDeclaredTeacherMatches(materialDTO.getTeacherId(), user);

        Class clazz = this.classService.getOwnedClass(materialDTO.getClassId(), user);

        Material material = materialDTO.toEntity();
        material.setClassId(clazz.getId());
        material.setTeacherId(user.getId());

        if (material.getMaterialType() == MaterialType.BASIC) {
            if (material.getText() == null || material.getText().isBlank()) {
                throw new InvalidRequestException("Text cannot be empty in BASIC material");
            }

            material.setTasks(null);
        } else if (material.getMaterialType() == MaterialType.TASKS) {
            if (material.getTasks() == null || material.getTasks().isEmpty()) {
                throw new InvalidRequestException("Tasks cannot be empty in TASKS material");
            }

            material.setText(null);
        }

        material.setStatus(MaterialStatus.WAITING);

        this.materialRepository.save(material);

        this.classService.addMaterialToClass(clazz, material.getId());

        return toResponse(material);
    }

    /**
     * Aprovação pelo professor responsável pela turma do material.
     * Materiais de turmas de terceiros são tratados como inexistentes (404).
     */
    public MaterialResponseDTO changeStatus(String materialId, MaterialStatus newStatus, AuthenticatedUser user) {
        AccessChecks.requireTeacher(user);

        Material material = this.materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialDoesNotExistException(MATERIAL_NOT_FOUND));

        try {
            this.classService.getOwnedClass(material.getClassId(), user);
        } catch (ClassDoesNotExistException e) {
            throw new MaterialDoesNotExistException(MATERIAL_NOT_FOUND);
        }

        if (material.getStatus() != MaterialStatus.WAITING || newStatus != MaterialStatus.APPROVED) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition: " + material.getStatus() + " -> " + newStatus
                            + ". Allowed transition: WAITING -> APPROVED.");
        }

        material.setStatus(MaterialStatus.APPROVED);

        this.materialRepository.save(material);

        return toResponse(material);
    }

    private static MaterialResponseDTO toResponse(Material material) {
        return new MaterialResponseDTO(
                material.getId(),
                material.getClassId(),
                material.getTeacherId(),
                material.getMaterialType(),
                material.getTitle(),
                material.getDescription(),
                material.getText(),
                material.getTasks(),
                material.getStatus()
        );
    }
}
