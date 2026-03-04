package com.litegateway.admin.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 白名单实体类
 * 对应数据库表 white_list
 */
@Data
@TableName("white_list")
public class WhiteList {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String path;

    private String description;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
