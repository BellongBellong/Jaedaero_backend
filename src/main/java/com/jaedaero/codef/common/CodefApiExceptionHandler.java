package com.jaedaero.codef.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts expected CODEF failures into an API response instead of a Tomcat error page. */
@RestControllerAdvice(basePackages = "com.jaedaero")
public class CodefApiExceptionHandler {

    @ExceptionHandler(CodefApiException.class)
    public ResponseEntity<Map<String, String>> handleCodefApiException(CodefApiException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        Map<String, String> response = new LinkedHashMap<>();
        response.put("code", "CODEF_REQUEST_FAILED");
        response.put("message", exception.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(CodefUserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(CodefUserNotFoundException exception) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("code", "USER_NOT_FOUND");
        response.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
