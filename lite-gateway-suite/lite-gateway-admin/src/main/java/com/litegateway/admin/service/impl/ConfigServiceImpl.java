package com.litegateway.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.litegateway.admin.dto.GatewayConfigDTO;
import com.litegateway.admin.dto.IpBlackDTO;
import com.litegateway.admin.dto.RouteDTO;
import com.litegateway.admin.dto.WhiteListDTO;
import com.litegateway.admin.repository.entity.GatewayRoute;
import com.litegateway.admin.repository.entity.IpBlacklist;
import com.litegateway.admin.repository.entity.WhiteList;
import com.litegateway.admin.repository.mapper.GatewayRouteMapper;
import com.litegateway.admin.repository.mapper.IpBlacklistMapper;
import com.litegateway.admin.repository.mapper.WhiteListMapper;
import com.litegateway.admin.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 配置服务实现类
 */
@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private GatewayRouteMapper routeMapper;

    @Autowired
    private IpBlacklistMapper ipBlacklistMapper;

    @Autowired
    private WhiteListMapper whiteListMapper;

    // 配置版本号，每次配置变更时递增
    private final AtomicLong configVersion = new AtomicLong(System.currentTimeMillis());

    @Override
    public GatewayConfigDTO getGatewayConfig() {
        GatewayConfigDTO config = new GatewayConfigDTO();
        config.setVersion(configVersion.get());

        // 获取启用的路由列表
        List<GatewayRoute> routes = routeMapper.selectEnabledRoutes();
        List<RouteDTO> routeDTOs = routes.stream().map(this::convertToRouteDTO).collect(Collectors.toList());
        config.setRoutes(routeDTOs);

        // 获取IP黑名单
        LambdaQueryWrapper<IpBlacklist> ipWrapper = new LambdaQueryWrapper<>();
        ipWrapper.eq(IpBlacklist::getDeleted, 0);
        List<IpBlacklist> ipList = ipBlacklistMapper.selectList(ipWrapper);
        List<IpBlackDTO> ipBlackDTOs = ipList.stream().map(this::convertToIpBlackDTO).collect(Collectors.toList());
        config.setIpBlacklist(ipBlackDTOs);

        // 获取白名单
        LambdaQueryWrapper<WhiteList> whiteWrapper = new LambdaQueryWrapper<>();
        whiteWrapper.eq(WhiteList::getDeleted, 0);
        List<WhiteList> whiteList = whiteListMapper.selectList(whiteWrapper);
        List<WhiteListDTO> whiteListDTOs = whiteList.stream().map(this::convertToWhiteListDTO).collect(Collectors.toList());
        config.setWhiteList(whiteListDTOs);

        log.info("Returning gateway config: version={}, routes={}, ipBlacklist={}, whiteList={}",
                config.getVersion(), routeDTOs.size(), ipBlackDTOs.size(), whiteListDTOs.size());

        return config;
    }

    @Override
    public Long getConfigVersion() {
        return configVersion.get();
    }

    @Override
    public void incrementVersion() {
        long newVersion = configVersion.incrementAndGet();
        log.info("Config version incremented to: {}", newVersion);
    }

    private RouteDTO convertToRouteDTO(GatewayRoute route) {
        RouteDTO dto = new RouteDTO();
        BeanUtils.copyProperties(route, dto);
        dto.setId(route.getId());
        dto.setStatus(String.valueOf(route.getStatus()));
        return dto;
    }

    private IpBlackDTO convertToIpBlackDTO(IpBlacklist ip) {
        IpBlackDTO dto = new IpBlackDTO();
        BeanUtils.copyProperties(ip, dto);
        return dto;
    }

    private WhiteListDTO convertToWhiteListDTO(WhiteList white) {
        WhiteListDTO dto = new WhiteListDTO();
        BeanUtils.copyProperties(white, dto);
        return dto;
    }
}
