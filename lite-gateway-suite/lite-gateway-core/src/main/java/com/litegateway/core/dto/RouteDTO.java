package com.litegateway.core.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路由 DTO
 */
@Data
public class RouteDTO {

    private Long id;

    private String routeId;

    private String name;

    private String uri;

    private String path;

    private Integer stripPrefix;

    private String host;

    private String remoteAddr;

    private String header;

    private String filterRateLimiterName;

    private Integer replenishRate;

    private Integer burstCapacity;

    private Integer weight;

    private String weightName;

    private String status;

    private String description;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;
}
