package com.sensei.app.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.exceptions.InvalidPasswordException;
import com.sensei.app.exceptions.NoChangesMadeException;
import com.sensei.app.exceptions.UnsupportedUpdateFieldException;
import com.sensei.app.exceptions.UserDeletionConflictException;
import com.sensei.app.exceptions.UserNotFoundException;
import com.sensei.app.security.SecurityErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public Map<String, String> handleUserNotFoundException(UserNotFoundException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({UnsupportedUpdateFieldException.class, InvalidPasswordException.class})
    public Map<String, String> handleInvalidUserInput(RuntimeException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserDeletionConflictException.class)
    public Map<String, String> handleUserDeletionConflictException(UserDeletionConflictException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    /**
     * Falha de login (e-mail inexistente ou senha incorreta): mensagem genérica,
     * sem indicar qual dos dois está errado.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SecurityErrorResponses.body(HttpStatus.UNAUTHORIZED, SecurityErrorResponses.INVALID_CREDENTIALS_MESSAGE, request));
    }

    /**
     * Erro interno durante a autenticação (ex.: banco indisponível). Não é credencial inválida.
     */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<Map<String, Object>> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SecurityErrorResponses.body(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication could not be completed.", request));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            String validValues = Arrays.toString(ex.getRequiredType().getEnumConstants());
            return Collections.singletonMap("error",
                    String.format("Invalid value '%s'. Allowed values: %s", ex.getValue(), validValues));
        }

        return Collections.singletonMap("error", "Invalid argument type: " + ex.getMessage());
    }
}
