package com.sensei.app.utils;

import com.sensei.app.enums.UserRole;
import com.sensei.app.exceptions.TeacherDoesNotExistException;
import com.sensei.app.exceptions.UserNotFoundException;
import com.sensei.app.model.User;
import com.sensei.app.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserUtils {
    private final UserRepository userRepository;

    public UserUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean userExists(String userId) {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getId().equals(userId);
    }

    public boolean isTeacher(String teacherId) {
        User teacher = this.userRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherDoesNotExistException("The teacher " + teacherId + " does not exist"));

        return teacher.getRole() == UserRole.TEACHER;
    }

    public boolean isStudent(String studentId) {
        User teacher = this.userRepository.findById(studentId)
                .orElseThrow(() -> new TeacherDoesNotExistException("The teacher " + studentId + " does not exist"));

        return teacher.getRole() == UserRole.STUDENT;
    }
}