package com.ncu.chat.common;

import lombok.Getter;

/**
 * 业务异常，消息可安全展示给前端
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
