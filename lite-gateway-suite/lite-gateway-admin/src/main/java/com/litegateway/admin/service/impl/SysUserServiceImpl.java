package com.litegateway.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.litegateway.admin.repository.entity.SysUser;
import com.litegateway.admin.repository.mapper.SysUserMapper;
import com.litegateway.admin.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 系统用户服务实现
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser validateCredentials(String username, String password) {
        SysUser user = getByUsername(username);

        if (user == null) {
            log.warn("User not found: {}", username);
            return null;
        }

        // 密码加密验证（使用 MD5，生产环境建议使用 BCrypt）
        String encryptedPassword = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8)
        );
        if (!encryptedPassword.equals(user.getPassword())) {
            log.warn("Invalid password for user: {}", username);
            return null;
        }

        return user;
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return getOne(wrapper);
    }
}
