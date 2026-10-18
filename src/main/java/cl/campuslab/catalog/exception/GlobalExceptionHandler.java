package cl.campuslab.catalog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        LocalDateTime.now(),
                        404,
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleBusinessError(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "No se pudo completar la operación."
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ErrorResponse(
                        LocalDateTime.now(),
                        exception.getStatusCode().value(),
                        message
                ));
    }

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String message
    ) {
    }
}