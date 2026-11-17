package com.sensei.app.exceptions;

public class ClassDeletionConflictException extends RuntimeException {
    public ClassDeletionConflictException(String message) {
        super(message);
    }
}
