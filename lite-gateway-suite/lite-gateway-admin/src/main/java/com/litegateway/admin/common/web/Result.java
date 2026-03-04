package com.litegateway.admin.common.web;

import com.litegateway.admin.common.exception.BizException;
import com.litegateway.admin.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 统一响应结果封装
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@Slf4j
@Builder
@NoArgsConstructor
public class Result<T> {

    @Schema(description = "响应状态码")
    private String code;

    @Schema(description = "响应状态信息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    public Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /*静态方法,用于快速构建结果*/
    public static <T> Result<T> ok() {
        return new Result<>(getSuccessCode(), getSuccessMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(getSuccessCode(), getSuccessMessage(), data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(getSuccessCode(), message, data);
    }

    public static <T> Result<T> failure(ErrorCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(code, message, null);
    }

    /*扩展方法，参考Optional类，用于结果的判断、转换、异常处理等*/

    /**
     * 判断http调用是否成功
     */
    @JsonIgnore
    public boolean isOk() {
        return getSuccessCode().equals(this.code);
    }

    /**
     * 如果Http调用成功调用该方法
     */
    @JsonIgnore
    public void ifOk(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        if (isOk()) {
            consumer.accept(data);
        }
    }

    /**
     * 判断是否有返回数据
     */
    @JsonIgnore
    public boolean isPresent() {
        return data != null;
    }

    /**
     * http调用成功且有返回值消费
     */
    @JsonIgnore
    public void ifPresent(Consumer<? super T> consumer) {
        throwIfNotOk();
        Objects.requireNonNull(consumer);
        if (isPresent()) {
            consumer.accept(data);
        }
    }

    /**
     * http调用成功返回data，否则按照返回信息抛出异常
     */
    @JsonIgnore
    public T getOrThrow() {
        return getOrThrow(() -> new BizException(this.code, this.message));
    }

    /**
     * http调用成功返回data，否则按照返回信息抛出自定义异常
     */
    @JsonIgnore
    public T getOrThrow(Supplier<BizException> supplier) {
        if (!isOk()) {
            throw supplier.get();
        }
        return data;
    }

    /**
     * 同时满足调用成功，返回数据，否则抛出自定义异常
     */
    @JsonIgnore
    public T orElseThrow(Supplier<BizException> supplier) {
        throwIfNotOk();
        Objects.requireNonNull(supplier);
        if (!isPresent()) {
            throw supplier.get();
        }
        return data;
    }

    /**
     * 如果调用失败抛出错误信息
     */
    @JsonIgnore
    public void throwIfNotOk() {
        throwIfNotOk(() -> new BizException(code, message));
    }

    /**
     * 调用失败，抛出自定义错误信息
     */
    @JsonIgnore
    public void throwIfNotOk(Supplier<BizException> supplier) {
        Objects.requireNonNull(supplier);
        if (!isOk()) {
            throw supplier.get();
        }
    }

    @JsonIgnore
    public Result<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (Objects.isNull(data)) {
            return this;
        } else {
            return predicate.test(data) ? this : new Result<>(code, message, null);
        }
    }

    @JsonIgnore
    public T orElse(T other) {
        throwIfNotOk();
        return data != null ? data : other;
    }

    @JsonIgnore
    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (Objects.isNull(data)) {
            return new Result<>(code, message, null);
        } else {
            return new Result<>(code, message, mapper.apply(data));
        }
    }

    /* 成功码配置 - 从 ErrorCodeService 获取 */

    private static String successCode = "00000";
    private static String successMessage = "成功";

    /**
     * 设置成功码（由 ErrorCodeService 初始化时调用）
     */
    public static void setSuccessCode(String code, String message) {
        successCode = code;
        successMessage = message;
    }

    /**
     * 获取成功码
     */
    public static String getSuccessCode() {
        return successCode;
    }

    /**
     * 获取成功消息
     */
    public static String getSuccessMessage() {
        return successMessage;
    }
}
