package com.sensei.app.service;

import com.sensei.app.dtos.CreateClassDTO;
import com.sensei.app.dtos.UpdateClassDTO;
import com.sensei.app.enums.UserRole;
import com.sensei.app.exceptions.ClassDeletionConflictException;
import com.sensei.app.exceptions.ClassDoesNotExistException;
import com.sensei.app.exceptions.InvalidRequestException;
import com.sensei.app.exceptions.NoChangesMadeException;
import com.sensei.app.exceptions.UnsupportedUpdateFieldException;
import com.sensei.app.model.Class;
import com.sensei.app.model.User;
import com.sensei.app.repository.ClassRepository;
import com.sensei.app.repository.MaterialRepository;
import com.sensei.app.repository.UserRepository;
import com.sensei.app.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regras de acesso às turmas:
 * - professor: cria, consulta, atualiza e exclui somente as próprias turmas;
 * - aluno: consulta somente as turmas em que está matriculado.
 * Turmas sem relação com o usuário são tratadas como inexistentes (404).
 */
@Service
public class ClassService {
    private static final String CLASS_NOT_FOUND = "Class not found.";

    private final ClassRepository classRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;

    public ClassService(ClassRepository classRepository,
                        MaterialRepository materialRepository,
                        UserRepository userRepository) {
        this.classRepository = classRepository;
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
    }

    public List<Class> index(AuthenticatedUser user) {
        if (AccessChecks.isTeacher(user)) {
            return this.classRepository.findByTeacherId(user.getId());
        }

        if (AccessChecks.isStudent(user)) {
            return this.classRepository.findByStudentIds(user.getId());
        }

        return List.of();
    }

    public Class createClass(CreateClassDTO classDTO, AuthenticatedUser user) {
        AccessChecks.requireTeacher(user);
        AccessChecks.assertDeclaredTeacherMatches(classDTO.getTeacherId(), user);

        List<String> studentIds = this.validateStudentIds(classDTO.getStudentIds());

        return this.classRepository.save(classDTO.toEntity(user.getId(), studentIds));
    }

    /**
     * Atualiza a turma existente (nunca cria outra). O professor responsável e os
     * materiais vinculados não podem ser alterados por este endpoint.
     */
    public Class updateClass(String classId, UpdateClassDTO classDTO, AuthenticatedUser user) {
        Class clazz = this.getOwnedClass(classId, user);

        if (!classDTO.getUnsupportedFields().isEmpty()) {
            throw new UnsupportedUpdateFieldException(
                    "Only 'subject', 'title', 'description' and 'studentIds' can be updated. Fields not allowed: "
                            + classDTO.getUnsupportedFields());
        }

        boolean changed = false;

        if (StringUtils.hasText(classDTO.getSubject())) {
            clazz.setSubject(classDTO.getSubject());
            changed = true;
        }

        if (StringUtils.hasText(classDTO.getTitle())) {
            clazz.setTitle(classDTO.getTitle());
            changed = true;
        }

        if (StringUtils.hasText(classDTO.getDescription())) {
            clazz.setDescription(classDTO.getDescription());
            changed = true;
        }

        if (classDTO.getStudentIds() != null) {
            clazz.setStudentIds(new ArrayList<>(this.validateStudentIds(classDTO.getStudentIds())));
            changed = true;
        }

        if (!changed) {
            throw new NoChangesMadeException("No fields were filled for update. Fields: [subject, title, description, studentIds]");
        }

        return this.classRepository.save(clazz);
    }

    /**
     * Exclui a própria turma somente se ela não tiver materiais (sem exclusão em cascata),
     * para não deixar materiais órfãos.
     */
    public void deleteClass(String classId, AuthenticatedUser user) {
        Class clazz = this.getOwnedClass(classId, user);

        boolean hasMaterials = (clazz.getMaterialIds() != null && !clazz.getMaterialIds().isEmpty())
                || this.materialRepository.existsByClassId(clazz.getId());

        if (hasMaterials) {
            throw new ClassDeletionConflictException("The class cannot be deleted while it has materials.");
        }

        this.classRepository.deleteById(clazz.getId());
    }

    /**
     * Turma pertencente ao professor autenticado. 403 se não for professor;
     * 404 se a turma não existir ou pertencer a outro professor.
     */
    public Class getOwnedClass(String classId, AuthenticatedUser user) {
        AccessChecks.requireTeacher(user);

        Class clazz = this.findClass(classId);

        if (!isOwner(clazz, user)) {
            throw new ClassDoesNotExistException(CLASS_NOT_FOUND);
        }

        return clazz;
    }

    /**
     * Turma visível ao usuário: o professor responsável ou um aluno matriculado.
     * 404 nos demais casos, sem revelar se a turma existe.
     */
    public Class getVisibleClass(String classId, AuthenticatedUser user) {
        Class clazz = this.findClass(classId);

        if (!isOwner(clazz, user) && !isEnrolled(clazz, user)) {
            throw new ClassDoesNotExistException(CLASS_NOT_FOUND);
        }

        return clazz;
    }

    static boolean isOwner(Class clazz, AuthenticatedUser user) {
        return AccessChecks.isTeacher(user) && user.getId().equals(clazz.getTeacherId());
    }

    static boolean isEnrolled(Class clazz, AuthenticatedUser user) {
        return AccessChecks.isStudent(user)
                && clazz.getStudentIds() != null
                && clazz.getStudentIds().contains(user.getId());
    }

    /**
     * Uso interno do MaterialService, depois de validar a propriedade da turma.
     */
    void addMaterialToClass(Class clazz, String materialId) {
        if (clazz.getMaterialIds() == null) {
            clazz.setMaterialIds(new ArrayList<>());
        }

        if (!clazz.getMaterialIds().contains(materialId)) {
            clazz.getMaterialIds().add(materialId);
        }

        this.classRepository.save(clazz);
    }

    private Class findClass(String classId) {
        if (!StringUtils.hasText(classId)) {
            throw new ClassDoesNotExistException(CLASS_NOT_FOUND);
        }

        return this.classRepository.findById(classId)
                .orElseThrow(() -> new ClassDoesNotExistException(CLASS_NOT_FOUND));
    }

    /**
     * Matrículas: somente IDs de usuários existentes com papel STUDENT. Remove duplicados.
     */
    private List<String> validateStudentIds(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }

        if (studentIds.stream().anyMatch(id -> !StringUtils.hasText(id))) {
            throw new InvalidRequestException("studentIds must not contain empty values.");
        }

        List<String> distinctIds = studentIds.stream().distinct().toList();

        Set<String> registeredStudents = this.userRepository.findAllById(distinctIds)
                .stream()
                .filter(student -> student.getRole() == UserRole.STUDENT)
                .map(User::getId)
                .collect(Collectors.toSet());

        List<String> invalidIds = distinctIds.stream()
                .filter(id -> !registeredStudents.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new InvalidRequestException("The following studentIds do not belong to registered students: " + invalidIds);
        }

        return distinctIds;
    }
}
