package com.litegateway.admin.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证配置响应
 * 供前端获取当前认证模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthConfigResponse {

    /** 认证类型：jwt | oauth2 | ldap */
    private String authType;

    /** 是否启用 OAuth2 */
    private Boolean oauth2Enabled;

    /** 是否启用 LDAP */
    private Boolean ldapEnabled;

    /** OAuth2 授权端点（OAuth2 模式下有效） */
    private String oauth2AuthorizationUri;
}
