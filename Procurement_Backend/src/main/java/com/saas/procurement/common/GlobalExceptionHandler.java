package com.saas.procurement.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApi(ApiException ex) {
		return respond(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
		ApiError body = new ApiError(Instant.now(), 400, "Please check the highlighted fields.", fields);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	ResponseEntity<ApiError> handleUnreadable(Exception ex) {
		return respond(HttpStatus.BAD_REQUEST, "The request data is invalid.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		return respond(HttpStatus.FORBIDDEN, "You do not have access to this page.");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
		return respond(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too large.");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex) {
		return respond(HttpStatus.NOT_FOUND, "Not found.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
		return respond(HttpStatus.METHOD_NOT_ALLOWED, "This action is not supported.");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return respond(HttpStatus.CONFLICT, "This record is linked to other data and cannot be changed this way.");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
	}

	private static ResponseEntity<ApiError> respond(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
	}

}
