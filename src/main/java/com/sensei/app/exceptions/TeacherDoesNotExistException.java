package com.sensei.app.exceptions;

public class TeacherDoesNotExistException extends RuntimeException {
    public TeacherDoesNotExistException(String message) {
        super(message);
    }
}
