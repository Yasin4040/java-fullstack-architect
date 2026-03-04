package com.litegateway.core.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户信息 DTO
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 * 移除了公司特定的字段，保留通用字段
 */
@Data
public class UserDTO {

    private Long id;
    private String account;
    private String nickname;
    private Integer userType;
    private List<String> scopes;
    private List<String> authorities;
    private List<String> resources;
    private String token;
    private String uid;

    /**
     * 从 JWT Claims 构建 UserDTO
     */
    public static UserDTO fromClaims(Map<String, Object> claims) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(claims.get("id") != null ? Long.valueOf(claims.get("id").toString()) : null);
        userDTO.setAccount((String) claims.get("sub"));
        userDTO.setNickname((String) claims.get("nickname"));
        userDTO.setUserType(claims.get("userType") != null ? Integer.parseInt(claims.get("userType").toString()) : null);
        userDTO.setUid((String) claims.get("uid"));

        // 处理 scope (可能是 String 或 List)
        Object scopeObj = claims.get("scope");
        if (scopeObj instanceof List) {
            userDTO.setScopes((List<String>) scopeObj);
        } else if (scopeObj instanceof String) {
            userDTO.setScopes(List.of(((String) scopeObj).split(" ")));
        }

        // 处理 authorities
        Object authoritiesObj = claims.get("authorities");
        if (authoritiesObj instanceof List) {
            userDTO.setAuthorities((List<String>) authoritiesObj);
        }

        return userDTO;
    }
}
