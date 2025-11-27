package com.sensei.app.exceptions;

public class UserIsNotTeacherException extends RuntimeException {
    public UserIsNotTeacherException(String message) {
        super(message);
    }
}
