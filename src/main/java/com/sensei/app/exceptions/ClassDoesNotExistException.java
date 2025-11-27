package com.sensei.app.exceptions;

public class ClassDoesNotExistException extends RuntimeException {
    public ClassDoesNotExistException(String message) {
        super(message);
    }
}
