package com.sensei.app.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sensei.app.dtos.ApiErrorResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converte exceções em respostas no contrato único {@link ApiErrorResponse}.
 * Mensagens de exceções internas, de bibliotecas ou stack traces nunca são devolvidas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 400 ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fieldErrors = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST, "Validation failed.", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (e.getRootCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            return respond(HttpStatus.BAD_REQUEST, String.format("Invalid value '%s' for %s. Must be one of: %s",
                    ife.getValue(),
                    ife.getTargetType().getSimpleName(),
                    Arrays.toString(ife.getTargetType().getEnumConstants())), request);
        }

        return respond(HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid data type.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            return respond(HttpStatus.BAD_REQUEST, String.format("Invalid value '%s'. Allowed values: %s",
                    e.getValue(), Arrays.toString(e.getRequiredType().getEnumConstants())), request);
        }

        return respond(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'.", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Required part '" + e.getRequestPartName() + "' is missing.", request);
    }

    @ExceptionHandler({
            UnsupportedUpdateFieldException.class,
            InvalidPasswordException.class,
            InvalidRequestException.class,
            NoChangesMadeException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(RuntimeException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // ---------- 401 / 403 ----------

    /**
     * Falha de login (e-mail inexistente ou senha incorreta): mensagem genérica,
     * sem indicar qual dos dois está errado.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException e, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, SecurityErrorResponses.INVALID_CREDENTIALS_MESSAGE, request);
    }

    /**
     * Erro interno durante a autenticação (ex.: banco indisponível). Não é credencial inválida.
     */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleInternalAuthentication(InternalAuthenticationServiceException e,
                                                                         HttpServletRequest request) {
        log.error("Authentication could not be completed", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication could not be completed.", request);
    }

    /**
     * Operação proibida (@PreAuthorize ou verificação nos services). Mesmo corpo do JsonAccessDeniedHandler.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, SecurityErrorResponses.ACCESS_DENIED_MESSAGE, request);
    }

    // ---------- 404 / 409 / 502 ----------

    /**
     * Recurso inexistente ou oculto pela política de acesso (sem revelar qual dos dois).
     */
    @ExceptionHandler({
            UserNotFoundException.class,
            ClassDoesNotExistException.class,
            MaterialDoesNotExistException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            UserDeletionConflictException.class,
            ClassDeletionConflictException.class,
            InvalidStatusTransitionException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException e, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    /**
     * Falha em serviço externo (OpenAI). Mensagem genérica; a causa fica apenas no log do servidor.
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalService(ExternalServiceException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_GATEWAY, e.getMessage(), request);
    }

    // ---------- demais ----------

    /**
     * Exceções do Spring MVC que já definem um status HTTP (rota inexistente, método não suportado,
     * upload acima do limite, ...) mantêm o status com mensagem genérica. Qualquer outra exceção
     * é um erro interno: 500 genérico, com detalhes somente no log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());

            if (status != null && status.is4xxClientError()) {
                return respond(status, clientErrorMessage(status), request);
            }
        }

        log.error("Unexpected error processing {} {}", request.getMethod(), request.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", request);
    }

    private static String clientErrorMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Resource not found.";
            case METHOD_NOT_ALLOWED -> "HTTP method not supported for this resource.";
            case PAYLOAD_TOO_LARGE -> "Request exceeds the maximum allowed size.";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported media type.";
            case NOT_ACCEPTABLE -> "Requested response format is not supported.";
            default -> status.getReasonPhrase() + ".";
        };
    }

    private static ResponseEntity<ApiErrorResponse> respond(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status, message, request.getRequestURI()));
    }
}
