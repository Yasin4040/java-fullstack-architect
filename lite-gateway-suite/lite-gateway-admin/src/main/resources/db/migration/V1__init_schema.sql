-- 初始化数据库表结构
-- Lite Gateway 数据库迁移脚本

-- 网关路由表
CREATE TABLE IF NOT EXISTS `gateway_route` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `route_id` varchar(64) NOT NULL COMMENT '路由ID',
  `name` varchar(128) DEFAULT NULL COMMENT '路由名称',
  `uri` varchar(256) NOT NULL COMMENT '目标URI',
  `path` varchar(256) DEFAULT NULL COMMENT '路径断言',
  `strip_prefix` int(11) DEFAULT '0' COMMENT '路径截取前缀数',
  `host` varchar(256) DEFAULT NULL COMMENT '主机断言',
  `remote_addr` varchar(256) DEFAULT NULL COMMENT '远程地址断言',
  `header` varchar(512) DEFAULT NULL COMMENT 'Header断言',
  `filter_rate_limiter_name` varchar(64) DEFAULT NULL COMMENT '限流器名称',
  `replenish_rate` int(11) DEFAULT NULL COMMENT '每秒补充令牌数',
  `burst_capacity` int(11) DEFAULT NULL COMMENT '令牌桶容量',
  `weight` int(11) DEFAULT NULL COMMENT '权重',
  `weight_name` varchar(64) DEFAULT NULL COMMENT '权重分组名',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0启用 1禁用',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_id` (`route_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关路由表';

-- IP黑名单表
CREATE TABLE IF NOT EXISTS `ip_blacklist` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ip` varchar(64) NOT NULL COMMENT 'IP地址',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP黑名单表';

-- 白名单表
CREATE TABLE IF NOT EXISTS `white_list` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `path` varchar(256) NOT NULL COMMENT '路径',
  `description` varchar(256) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='白名单表';

-- 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0正常 1禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 插入默认管理员用户 (密码: 123456)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 0);