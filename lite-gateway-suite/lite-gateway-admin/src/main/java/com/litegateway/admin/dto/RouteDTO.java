package com.litegateway.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路由数据传输对象
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@Schema(description = "路由数据传输对象")
public class RouteDTO {

    @Schema(description = "id")
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

    @Schema(description = "断言主机")
    private String host;

    @Schema(description = "断言远程地址")
    private String remoteAddr;

    @Schema(description = "断言Headers")
    private String header;

    @Schema(description = "限流器")
    private String filterRateLimiterName;

    @Schema(description = "每秒流量")
    private Integer replenishRate;

    @Schema(description = "令牌总量")
    private Integer burstCapacity;

    @Schema(description = "状态，0启用，1禁用")
    private String status;

    @Schema(description = "请求参数")
    private String requestParameter;

    @Schema(description = "重写Path路径")
    private String rewritePath;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "权重名称", hidden = true)
    private String weightName;

    @Schema(description = "权重", hidden = true)
    private Integer weight;
}
