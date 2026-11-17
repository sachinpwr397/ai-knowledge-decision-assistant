package com.sachin.aiassistant.exception;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,Object>> handleStatus(ResponseStatusException ex){return response(ex.getStatusCode(),ex.getReason()==null?"Request failed":ex.getReason());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex){
        String message=ex.getBindingResult().getFieldErrors().stream().findFirst().map(e->e.getField()+": "+e.getDefaultMessage()).orElse("Invalid request");
        return response(HttpStatus.BAD_REQUEST,message);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String,Object>> handleSize(MaxUploadSizeExceededException ex){return response(HttpStatus.PAYLOAD_TOO_LARGE,"PDF must be 10 MB or smaller.");}
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> handleOther(Exception ex){return response(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error. Check application logs.");}
    private ResponseEntity<Map<String,Object>> response(HttpStatusCode status,String message){
        Map<String,Object> body=new LinkedHashMap<>(); body.put("timestamp",Instant.now()); body.put("status",status.value()); body.put("error",message); return ResponseEntity.status(status).body(body);
    }
}
