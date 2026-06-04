package dev.lucasxie.learning.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final String code;

	private final String message;

	public BusinessException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		this(errorCode.name(), message);
	}
}
