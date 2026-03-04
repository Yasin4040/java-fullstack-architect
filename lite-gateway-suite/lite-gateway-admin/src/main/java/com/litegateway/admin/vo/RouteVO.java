package com.litegateway.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 路由视图对象
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "路由视图对象")
public class RouteVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "系统代号")
    private String systemCode;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "服务地址")
    private String uri;

    @Schema(description = "断言地址")
    private String path;

    @Schema(description = "断言截取")
    private Integer stripPrefix;

    @Schema(description = "限流器")
    private String filterRateLimiterName;

    @Schema(description = "状态，0启用，1禁用")
    private String status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
