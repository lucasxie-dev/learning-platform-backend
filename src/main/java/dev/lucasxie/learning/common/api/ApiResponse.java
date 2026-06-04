package dev.lucasxie.learning.common.api;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

	private boolean success;

	private String code;

	private String message;

	private T data;

	private Instant timestamp;

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "SUCCESS", "Success", data, Instant.now());
	}

	public static ApiResponse<Void> success() {
		return new ApiResponse<>(true, "SUCCESS", "Success", null, Instant.now());
	}

	public static ApiResponse<Void> failure(String code, String message) {
		return new ApiResponse<>(false, code, message, null, Instant.now());
	}
}
