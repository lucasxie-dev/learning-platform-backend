package dev.lucasxie.learning.common.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.lucasxie.learning.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleBusinessException(BusinessException exception) {
		return ApiResponse.failure(exception.getCode(), exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
			.collect(Collectors.joining("; "));

		return ApiResponse.failure(ErrorCode.VALIDATION_FAILED.name(), message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
		String message = exception.getConstraintViolations()
			.stream()
			.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
			.collect(Collectors.joining("; "));

		return ApiResponse.failure(ErrorCode.VALIDATION_FAILED.name(), message);
	}

	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException exception) {
		return ApiResponse.failure(ErrorCode.COMMON_FORBIDDEN.name(), exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiResponse<Void> handleException(Exception exception) {
		return ApiResponse.failure(ErrorCode.COMMON_INTERNAL_ERROR.name(), exception.getMessage());
	}
}
