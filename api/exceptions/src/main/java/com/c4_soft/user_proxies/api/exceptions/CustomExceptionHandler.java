package com.c4_soft.user_proxies.api.exceptions;

import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        final String msg =
                "Payload validation failure:"
                        + ex.getBindingResult().getAllErrors().stream().map(ObjectError::toString).collect(Collectors.joining("\n  * ", "\n  * ", ""));

        return handleExceptionInternal(ex, msg, headers, status, request);
    }

	@Override
	protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		return super.handleMissingPathVariable(ex, headers, HttpStatus.NOT_FOUND, request);
	}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    protected void handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        logger.info(ex.getMessage());
    }

	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	@ExceptionHandler(ProxyUsersUnmodifiableException.class)
	protected void handleProxyUsersUnmodifiable(ProxyUsersUnmodifiableException ex, WebRequest request) {
		logger.info(ex.getMessage());
	}
	
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(BadRequestException.class)
	protected void handleBadRequest(BadRequestException ex, WebRequest request) {
		logger.info(ex.getMessage());
	}
	
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(ConstraintViolationException.class)
	protected void handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
		logger.info(ex.getMessage());
	}
	
}
