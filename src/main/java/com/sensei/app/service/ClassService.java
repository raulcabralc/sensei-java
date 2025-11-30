package com.sensei.app.service;

import com.sensei.app.dtos.CreateClassDTO;
import com.sensei.app.dtos.UpdateClassDTO;
import com.sensei.app.exceptions.UserIsNotTeacherException;
import com.sensei.app.exceptions.UserNotFoundException;
import com.sensei.app.model.Class;
import com.sensei.app.repository.ClassRepository;
import com.sensei.app.utils.UserUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClassService {
    private final ClassRepository classRepository;
    private final UserUtils userUtils;

    public ClassService(ClassRepository classRepository, UserUtils userUtils) {
        this.classRepository = classRepository;
        this.userUtils = userUtils;
    }

    public List<Class> index() {
        return classRepository.findAll();
    }

    public Class createClass(CreateClassDTO classDTO) throws BadRequestException {
        try {
            if (!this.userUtils.userExists(classDTO.getTeacherId())) {
                throw new UserNotFoundException("The user " + classDTO.getTeacherId() + " does not exist");
            }

            if (!this.userUtils.isTeacher(classDTO.getTeacherId())) {
                throw new UserIsNotTeacherException("The user " + classDTO.getTeacherId() + " is not a teacher");
            }

            for (String studentId : classDTO.getStudentIds()) {
                if (!this.userUtils.userExists(studentId)) {
                    throw new UserNotFoundException("The user " + studentId + " does not exist");
                }
            }

            Class newClass = classDTO.toEntity();

            return this.classRepository.save(newClass);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public Class updateClass(UpdateClassDTO classDTO) throws BadRequestException {
        try {
            if (classDTO.getTeacherId() != null && !this.userUtils.userExists(classDTO.getTeacherId())) {
                throw new UserNotFoundException("The user " + classDTO.getTeacherId() + " does not exist");
            }

            if (classDTO.getTeacherId() != null && !this.userUtils.isTeacher(classDTO.getTeacherId())) {
                throw new UserIsNotTeacherException("The user " + classDTO.getTeacherId() + " is not a teacher");
            }

            if (classDTO.getStudentIds() != null) {
                for (String studentId : classDTO.getStudentIds()) {
                    if (!this.userUtils.userExists(studentId)) {
                        throw new UserNotFoundException("The user " + studentId + " does not exist");
                    }
                }
            }

            Class updatedClass = classDTO.toEntity();

            return this.classRepository.save(updatedClass);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public void addMaterialToClass(String classId, String materialId) throws BadRequestException {
        try {
            Class clazz = this.classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));

            if (clazz.getMaterialIds() == null) {
                clazz.setMaterialIds(new ArrayList<>());
            }

            if (!clazz.getMaterialIds().contains(materialId)) {
                clazz.getMaterialIds().add(materialId);
            }

            this.classRepository.save(clazz);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
