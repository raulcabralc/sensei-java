package com.sensei.app.service;

import com.sensei.app.enums.UserRole;
import com.sensei.app.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

/**
 * Verificações de papel compartilhadas pelos services de turmas, materiais e IA.
 * Repetem no service as restrições de papel dos controllers, para que nenhum fluxo
 * (inclusive chamadas entre services) as contorne.
 */
final class AccessChecks {
    private AccessChecks() {
    }

    static boolean isTeacher(AuthenticatedUser user) {
        return user != null && user.getRole() == UserRole.TEACHER;
    }

    static boolean isStudent(AuthenticatedUser user) {
        return user != null && user.getRole() == UserRole.STUDENT;
    }

    /** 403 se o usuário não for professor. */
    static void requireTeacher(AuthenticatedUser user) {
        if (!isTeacher(user)) {
            throw new AccessDeniedException("Only teachers can perform this operation.");
        }
    }

    /**
     * teacherId enviado pelo cliente nunca autoriza nada: é opcional e, se informado,
     * precisa ser o próprio professor autenticado (403 caso contrário).
     */
    static void assertDeclaredTeacherMatches(String declaredTeacherId, AuthenticatedUser user) {
        if (StringUtils.hasText(declaredTeacherId) && !declaredTeacherId.equals(user.getId())) {
            throw new AccessDeniedException("teacherId must match the authenticated teacher.");
        }
    }
}
