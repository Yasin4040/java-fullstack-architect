package com.litegateway.admin.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码（可选） */
    private String captcha;

    /** 验证码Key（可选） */
    private String captchaKey;

    /** 选择的账号（可选） */
    private String selectAccount;
}
