package com.snk.server.api.controller;

import com.snk.server.domain.auth.AuthException;
import com.snk.server.domain.auth.RateLimitException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public void handleConstraintViolation() {
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		detail.setTitle("Validation failure");
		detail.setDetail(exception.getFieldErrors().stream()
			.map(e -> e.getField() + ": " + e.getDefaultMessage())
			.reduce((a, b) -> a + "; " + b)
			.orElse("Validation failed"));
		return detail;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ProblemDetail handleHttpMessageNotReadable() {
		ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		detail.setTitle("Malformed request");
		return detail;
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail handleResponseStatus(ResponseStatusException exception) {
		ProblemDetail detail = ProblemDetail.forStatus(exception.getStatusCode());
		detail.setTitle(exception.getReason());
		return detail;
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ProblemDetail handleOther(Exception exception) {
		log.error("Unhandled exception", exception);
		ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		detail.setTitle("Internal error");
		detail.setDetail(exception.getMessage());
		return detail;
	}

	@ExceptionHandler(AuthException.class)
	public ProblemDetail handleAuth(AuthException exception) {
		ProblemDetail detail = ProblemDetail.forStatus(exception.status());
		detail.setTitle(exception.code());
		detail.setProperty("code", exception.code());
		return detail;
	}

	@ExceptionHandler(RateLimitException.class)
	public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitException exception) {
		ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
		detail.setTitle("AUTH_RATE_LIMITED"); detail.setProperty("code", "AUTH_RATE_LIMITED");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds())).body(detail);
	}
}
