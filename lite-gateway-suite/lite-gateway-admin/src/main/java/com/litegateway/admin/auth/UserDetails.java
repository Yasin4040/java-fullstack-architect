package com.litegateway.admin.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户详情（用于认证上下文）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色列表 */
    private List<String> roles;

    /** 是否启用 */
    private boolean enabled;

    /**
     * 检查是否具有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 检查是否具有管理员角色
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("ROLE_ADMIN");
    }
}
