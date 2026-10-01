package com.teamforge;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import java.util.Map;
@RestControllerAdvice
class ApiErrors {
 @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
 @ResponseStatus(HttpStatus.BAD_REQUEST)
 Map<String,String> invalidInput(Exception exception) { return Map.of("message", "Invalid request fields"); }
 @ExceptionHandler(AuthenticationException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
 Map<String,String> invalidCredentials(Exception exception) { return Map.of("message", "Invalid email or password"); }
}
