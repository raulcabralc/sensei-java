package com.sensei.app.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sensei.app.exceptions.ClassDeletionConflictException;
import com.sensei.app.exceptions.ClassDoesNotExistException;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.exceptions.ExternalServiceException;
import com.sensei.app.exceptions.InvalidPasswordException;
import com.sensei.app.exceptions.InvalidRequestException;
import com.sensei.app.exceptions.InvalidStatusTransitionException;
import com.sensei.app.exceptions.MaterialDoesNotExistException;
import com.sensei.app.exceptions.NoChangesMadeException;
import com.sensei.app.exceptions.UnsupportedUpdateFieldException;
import com.sensei.app.exceptions.UserDeletionConflictException;
import com.sensei.app.exceptions.UserNotFoundException;
import com.sensei.app.security.SecurityErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    /**
     * Recurso inexistente ou oculto pela política de acesso (sem revelar qual dos dois).
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({
            UserNotFoundException.class,
            ClassDoesNotExistException.class,
            MaterialDoesNotExistException.class
    })
    public Map<String, String> handleNotFound(RuntimeException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            UnsupportedUpdateFieldException.class,
            InvalidPasswordException.class,
            InvalidRequestException.class
    })
    public Map<String, String> handleInvalidInput(RuntimeException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Map<String, String> handleMissingRequestPart(MissingServletRequestPartException e) {
        return Collections.singletonMap("error", "Required part '" + e.getRequestPartName() + "' is missing.");
    }

    /**
     * Operação incompatível com o estado atual ou com dependências existentes.
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({
            UserDeletionConflictException.class,
            ClassDeletionConflictException.class,
            InvalidStatusTransitionException.class
    })
    public Map<String, String> handleConflict(RuntimeException e) {
        return Collections.singletonMap("error", e.getMessage());
    }

    /**
     * Falha em serviço externo (OpenAI). Mensagem genérica; detalhes ficam apenas no log do servidor.
     */
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(ExternalServiceException.class)
    public Map<String, String> handleExternalServiceException(ExternalServiceException e) {
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
