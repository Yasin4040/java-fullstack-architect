package com.litegateway.core.dto;

import lombok.Data;

import java.util.List;

/**
 * 网关配置 DTO
 * 从 Admin 模块获取的配置数据
 */
@Data
public class GatewayConfigDTO {

    private Long version;

    private List<RouteDTO> routes;

    private List<IpBlackDTO> ipBlacklist;

    private List<WhiteListDTO> whiteList;
}
