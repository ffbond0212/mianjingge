package com.mianjingge.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码集中定义
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;
}
