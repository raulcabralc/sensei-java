package com.sensei.app.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.exceptions.NoChangesMadeException;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return errors;
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public Map<String, String> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException e) {

        return Collections.singletonMap("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {

        Throwable rootCause = e.getRootCause();

        if (rootCause instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String enumName = ife.getTargetType().getSimpleName();
                String validValues = Arrays.toString(ife.getTargetType().getEnumConstants());

                return Collections.singletonMap("error",
                        String.format("Invalid value '%s' for %s. Must be one of: %s",
                                ife.getValue(),
                                enumName,
                                validValues
                        )
                );
            }
        }

        return Collections.singletonMap("error", "Malformed JSON request or invalid data type.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NoChangesMadeException.class)
    public Map<String, String> handleNoChangesMadeException(NoChangesMadeException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public Map<String, String> handleBadRequestException(BadRequestException e) {
        return Collections.singletonMap("error", e.getMessage());
    }
}
