package com.sensei.app.exceptions;

public class MaterialDoesNotExistException extends RuntimeException {
    public MaterialDoesNotExistException(String message) {
        super(message);
    }
}
