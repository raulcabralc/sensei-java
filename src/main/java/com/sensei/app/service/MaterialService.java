package com.sensei.app.service;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.MaterialResponseDTO;
import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.enums.MaterialType;
import com.sensei.app.exceptions.ClassDoesNotExistException;
import com.sensei.app.exceptions.TeacherDoesNotExistException;
import com.sensei.app.exceptions.UserIsNotTeacherException;
import com.sensei.app.model.Material;
import com.sensei.app.repository.ClassRepository;
import com.sensei.app.repository.MaterialRepository;
import com.sensei.app.utils.UserUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final ClassRepository classRepository;
    private final UserUtils userUtils;
    private final ClassService classService;

    public MaterialService(
            MaterialRepository materialRepository,
            ClassRepository classRepository,
            UserUtils userUtils,
            ClassService classService
    ) {
        this.materialRepository = materialRepository;
        this.classRepository = classRepository;
        this.userUtils = userUtils;
        this.classService = classService;
    }

    public List<Material> index(String classId) {
        if (classId == null) {
            throw new ClassDoesNotExistException("ClassId is required");
        }

        return this.materialRepository.findByClassId(classId);
    }

    public MaterialResponseDTO createMaterial(CreateMaterialDTO materialDTO)
            throws ClassDoesNotExistException,
            TeacherDoesNotExistException,
            UserIsNotTeacherException {
        try {
            Material material = materialDTO.toEntity();

            if (material.getMaterialType() == MaterialType.BASIC) {
                if (material.getText() == null || material.getText().isEmpty()) {
                    throw new BadRequestException("Text cannot be empty in BASIC material");
                }

                material.setText(material.getText());
                material.setTasks(null);
            } else if (material.getMaterialType() == MaterialType.TASKS) {
                if (material.getTasks() == null || material.getTasks().isEmpty()) {
                    throw new BadRequestException("Tasks cannot be empty in TASKS material");
                }

                material.setTasks(material.getTasks());
                material.setText(null);
            }

            boolean classExists = this.classRepository.existsById(material.getClassId());

            if (!classExists) {
                throw new ClassDoesNotExistException("The class " + material.getClassId() + " does not exist");
            }

            if (!userUtils.isTeacher(material.getTeacherId())) {
                throw new UserIsNotTeacherException("The user " + material.getTeacherId() + " is not a teacher");
            }

            this.materialRepository.save(material);

            this.classService.addMaterialToClass(material.getClassId(), material.getId());

            return new MaterialResponseDTO(
                    material.getId(),
                    material.getClassId(),
                    material.getTeacherId(),
                    material.getMaterialType(),
                    material.getTitle(),
                    material.getDescription(),
                    material.getText(),
                    material.getTasks(),
                    MaterialStatus.WAITING
            );
        } catch  (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
