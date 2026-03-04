package com.litegateway.admin.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IP黑名单实体类
 * 对应数据库表 ip_blacklist
 */
@Data
@TableName("ip_blacklist")
public class IpBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ip;

    private String remark;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
