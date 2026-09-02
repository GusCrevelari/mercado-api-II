package br.com.fiap.mercadomvc.api;

import br.com.fiap.mercadomvc.api.dto.ApiErrorResponse;
import br.com.fiap.mercadomvc.api.exception.ApiValidationException;
import br.com.fiap.mercadomvc.exception.MercadoNaoEncontradoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = MercadoRestController.class)
public class MercadoRestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Dados invalidos", fieldErrors);
    }

    @ExceptionHandler(ApiValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleApiValidation(ApiValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getFieldErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMalformedJson() {
        return error(HttpStatus.BAD_REQUEST, "JSON malformado", Map.of());
    }

    @ExceptionHandler(MercadoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(MercadoNaoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    private ApiErrorResponse error(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
