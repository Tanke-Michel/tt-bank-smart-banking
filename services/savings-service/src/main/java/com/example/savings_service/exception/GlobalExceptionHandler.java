package com.example.savings_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field = ((FieldError) e).getField();
            fieldErrors.put(field, e.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("Validation Failed")
                .message("One or more fields are invalid")
                .fieldErrors(fieldErrors).timestamp(LocalDateTime.now()).build());
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGroupNotFound(GroupNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Group Not Found", ex.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Member Not Found", ex.getMessage());
    }

    @ExceptionHandler(ContributionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContribNotFound(ContributionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Contribution Not Found", ex.getMessage());
    }

    @ExceptionHandler(PayoutNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePayoutNotFound(PayoutNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Payout Not Found", ex.getMessage());
    }

    @ExceptionHandler(GroupFullException.class)
    public ResponseEntity<ErrorResponse> handleGroupFull(GroupFullException ex) {
        return build(HttpStatus.CONFLICT, "Group Full", ex.getMessage());
    }

    @ExceptionHandler(AlreadyMemberException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyMember(AlreadyMemberException ex) {
        return build(HttpStatus.CONFLICT, "Already a Member", ex.getMessage());
    }

    @ExceptionHandler(NotGroupMemberException.class)
    public ResponseEntity<ErrorResponse> handleNotMember(NotGroupMemberException ex) {
        return build(HttpStatus.FORBIDDEN, "Not a Group Member", ex.getMessage());
    }

    @ExceptionHandler(GroupNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleNotActive(GroupNotActiveException ex) {
        return build(HttpStatus.CONFLICT, "Group Not Active", ex.getMessage());
    }

    @ExceptionHandler(InvalidGroupStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidGroupStateException ex) {
        return build(HttpStatus.CONFLICT, "Invalid Group State", ex.getMessage());
    }

    @ExceptionHandler(WalletServiceException.class)
    public ResponseEntity<ErrorResponse> handleWalletService(WalletServiceException ex) {
        HttpStatus status = switch (ex.getStatusCode()) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default  -> HttpStatus.BAD_GATEWAY;
        };
        return build(status, "Wallet Operation Failed", ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AuthorizationDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access Denied",
                "You do not have permission to access this resource");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value()).error(error).message(message)
                .timestamp(LocalDateTime.now()).build());
    }
}
