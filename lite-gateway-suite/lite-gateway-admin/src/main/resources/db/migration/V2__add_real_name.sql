-- 添加真实名字段
-- Lite Gateway 数据库迁移脚本

-- 在 sys_user 表中添加 real_name 字段
ALTER TABLE `sys_user` ADD COLUMN `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名' AFTER `nickname`;

-- 更新默认管理员用户的真实姓名
UPDATE `sys_user` SET `real_name` = '管理员' WHERE `username` = 'admin';