package com.sensei.app.exceptions;

public class UserDeletionConflictException extends RuntimeException {
    public UserDeletionConflictException(String message) {
        super(message);
    }
}
