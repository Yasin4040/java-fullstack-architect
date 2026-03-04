package com.litegateway.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.litegateway.admin.repository.entity.SysUser;

/**
 * 系统用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 验证用户凭据
     * @param username 用户名
     * @param password 密码
     * @return 用户信息，验证失败返回 null
     */
    SysUser validateCredentials(String username, String password);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);
}
