package com.litegateway.admin.security;

import com.litegateway.admin.repository.entity.SysUser;
import com.litegateway.admin.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现
 * 用于 Spring Security 加载用户信息
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserService sysUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        SysUser user = sysUserService.getByUsername(username);

        if (user == null) {
            log.error("User not found: {}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            log.error("User is disabled: {}", username);
            throw new UsernameNotFoundException("User is disabled: " + username);
        }

        // 构建 Spring Security 的 UserDetails
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}
