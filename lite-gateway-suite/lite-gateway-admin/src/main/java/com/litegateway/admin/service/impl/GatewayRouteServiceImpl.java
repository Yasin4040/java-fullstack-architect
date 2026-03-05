package com.litegateway.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.litegateway.admin.common.web.PageBody;
import com.litegateway.admin.constants.RedisTypeConstants;
import com.litegateway.admin.constants.StringConstants;
import com.litegateway.admin.dto.InstanceDTO;
import com.litegateway.admin.dto.InterfaceDTO;
import com.litegateway.admin.dto.RouteDTO;
import com.litegateway.admin.query.InstanceQuery;
import com.litegateway.admin.query.RouteQuery;
import com.litegateway.admin.repository.entity.GatewayRoute;
import com.litegateway.admin.repository.mapper.GatewayRouteMapper;
import com.litegateway.admin.service.GatewayRouteService;
import com.litegateway.admin.vo.RouteVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 网关路由服务实现类
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 * 核心逻辑保留：Nacos 服务查询、路由 CRUD、Redis 操作
 * 移除了公司特定的注解和依赖
 * 使用 MyBatis-Plus 进行数据库操作
 */
@Slf4j
@Service
public class GatewayRouteServiceImpl extends ServiceImpl<GatewayRouteMapper, GatewayRoute> implements GatewayRouteService {

    @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}")
    private String serverAddr;

    @Value("${spring.profiles.active:local}")
    private String group;

    private final String clusterName = "DEFAULT";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.litegateway.admin.service.ConfigService configService;

    // Caffeine 缓存实例信息
    public static Cache<String, Instance> instanceCache;

    static {
        instanceCache = Caffeine.newBuilder()
                .expireAfterWrite(StringConstants.CAFFEINE_EXPIRE_TIME, TimeUnit.DAYS)
                .maximumSize(StringConstants.CAFFEINE_MAX_SIZE)
                .build();
    }

    @Override
    public void addRoute(RouteDTO routeDTO) {
        GatewayRoute route = new GatewayRoute();
        BeanUtils.copyProperties(routeDTO, route);
        
        // 生成唯一路由ID
        if (!StringUtils.hasText(route.getRouteId())) {
            route.setRouteId("route_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        route.setDeleted(0);
        
        this.save(route);
        log.info("Added route: {}", route.getName());
        
        // 增加配置版本号，通知 Core 模块更新
        configService.incrementVersion();
    }

    @Override
    public List<RouteDTO> routeList() {
        LambdaQueryWrapper<GatewayRoute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GatewayRoute::getDeleted, 0);
        wrapper.orderByDesc(GatewayRoute::getCreateTime);
        
        List<GatewayRoute> routes = this.list(wrapper);
        return routes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteRoute(Long id) {
        this.removeById(id);
        log.info("Deleted route: {}", id);
        
        // 增加配置版本号，通知 Core 模块更新
        configService.incrementVersion();
    }

    @Override
    public void updateRoute(RouteDTO routeDTO) {
        GatewayRoute route = new GatewayRoute();
        BeanUtils.copyProperties(routeDTO, route);
        route.setUpdateTime(LocalDateTime.now());
        
        this.updateById(route);
        log.info("Updated route: {}", route.getName());
        
        // 增加配置版本号，通知 Core 模块更新
        configService.incrementVersion();
    }

    @Override
    public RouteDTO getById(String id) {
        GatewayRoute route = this.getById(Long.valueOf(id));
        if (route == null) {
            return null;
        }
        return convertToDTO(route);
    }

    @Override
    public List<RouteVO> selectRouteList(RouteQuery query) {
        LambdaQueryWrapper<GatewayRoute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GatewayRoute::getDeleted, 0);

        // 状态过滤
        if (StrUtil.isNotBlank(query.getStatus()) && !"2".equals(query.getStatus())) {
            wrapper.eq(GatewayRoute::getStatus, Integer.valueOf(query.getStatus()));
        }

        // 名称模糊查询
        if (StrUtil.isNotBlank(query.getName())) {
            wrapper.like(GatewayRoute::getName, query.getName());
        }

        // URI 模糊查询ss
        if (StrUtil.isNotBlank(query.getUri())) {
            wrapper.like(GatewayRoute::getUri, query.getUri());
        }

        // Path 模糊查询
        if (StrUtil.isNotBlank(query.getPath())) {
            wrapper.like(GatewayRoute::getPath, query.getPath());
        }

        wrapper.orderByDesc(GatewayRoute::getCreateTime);


        List<GatewayRoute> list = this.list(wrapper);

        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void reloadConfig() {
        // 发布 Redis 消息通知网关刷新路由
        redisTemplate.convertAndSend(RedisTypeConstants.CHANNEL, RedisTypeConstants.ROUTE_UPDATE);
        log.info("Published route update message to Redis channel");
    }

    @Override
    public PageBody<RouteVO> selectRoutePageVo(RouteQuery query) {
        LambdaQueryWrapper<GatewayRoute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GatewayRoute::getDeleted, 0);
        
        // 状态过滤
        if (StrUtil.isNotBlank(query.getStatus()) && !"2".equals(query.getStatus())) {
            wrapper.eq(GatewayRoute::getStatus, Integer.valueOf(query.getStatus()));
        }
        
        // 名称模糊查询
        if (StrUtil.isNotBlank(query.getName())) {
            wrapper.like(GatewayRoute::getName, query.getName());
        }
        
        // URI 模糊查询ss
        if (StrUtil.isNotBlank(query.getUri())) {
            wrapper.like(GatewayRoute::getUri, query.getUri());
        }
        
        // Path 模糊查询
        if (StrUtil.isNotBlank(query.getPath())) {
            wrapper.like(GatewayRoute::getPath, query.getPath());
        }
        
        wrapper.orderByDesc(GatewayRoute::getCreateTime);
        
        Page<GatewayRoute> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<GatewayRoute> resultPage = this.page(page, wrapper);
        
        List<RouteVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageBody<>(
                resultPage.getTotal(),
                (int) resultPage.getPages(),
                query.getPageSize(),
                query.getPageNum(),
                voList
        );
    }

    private RouteDTO convertToDTO(GatewayRoute route) {
        RouteDTO dto = new RouteDTO();
        BeanUtils.copyProperties(route, dto);
        dto.setId(route.getId());
        dto.setStatus(String.valueOf(route.getStatus()));
        return dto;
    }

    private RouteVO convertToVO(GatewayRoute route) {
        RouteVO vo = new RouteVO();
        BeanUtils.copyProperties(route, vo);
        vo.setId(route.getId());
        vo.setStatus(String.valueOf(route.getStatus()));
        return vo;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName) {
        List<Instance> allInstances = new ArrayList<>();
        try {
            NamingService naming = NamingFactory.createNamingService(serverAddr);
            allInstances = naming.getAllInstances(serviceName, group);
            setCacheValue(allInstances);
        } catch (NacosException e) {
            log.error("Failed to get instances from Nacos: {}", e.getMessage());
        }
        return allInstances;
    }

    @Override
    public List<Instance> getAllInstancesPage(InstanceQuery query) {
        String serviceName = group + "@@" + query.getServiceName();
        String url = String.format("http://%s/nacos/v1/ns/catalog/instances?serviceName=%s&clusterName=%s&namespaceId=%s&pageSize=%d&pageNo=%d",
                serverAddr, serviceName, clusterName, group, query.getPageSize(), query.getPageNum());

        log.info("Query Nacos instances: {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isEmpty()) {
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode listNode = root.get("list");

            if (listNode != null && listNode.isArray()) {
                List<Instance> instances = new ArrayList<>();
                for (JsonNode node : listNode) {
                    Instance instance = objectMapper.treeToValue(node, Instance.class);
                    instances.add(instance);
                }
                setCacheValue(instances);
                return instances;
            }
        } catch (Exception e) {
            log.error("Failed to get instances page: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private void setCacheValue(List<Instance> instances) {
        if (instances != null && !instances.isEmpty()) {
            instances.forEach(x -> instanceCache.put(x.getInstanceId(), x));
        }
    }

    private Instance getCacheValue(String instanceId) {
        return instanceCache.getIfPresent(instanceId);
    }

    @Override
    public void updateInstanceWeight(InstanceDTO dto) {
        try {
            Instance instance = getCacheValue(dto.getInstanceId());
            if (instance == null) {
                log.warn("Instance not found in cache: {}", dto.getInstanceId());
                return;
            }
            instance.setWeight(dto.getWeight());

            NamingMaintainService maintainService = NamingMaintainFactory.createMaintainService(serverAddr);
            maintainService.updateInstance(instance.getServiceName(), group, instance);
            instanceCache.put(instance.getInstanceId(), instance);
            log.info("Updated instance weight: {}", dto.getInstanceId());
        } catch (NacosException e) {
            log.error("Failed to update instance weight: {}", e.getMessage());
        }
    }

    @Override
    public void updateInstanceEnabled(InstanceDTO dto) {
        try {
            Instance instance = getCacheValue(dto.getInstanceId());
            if (instance == null) {
                log.warn("Instance not found in cache: {}", dto.getInstanceId());
                return;
            }
            instance.setEnabled(dto.getEnabled());

            NamingMaintainService maintainService = NamingMaintainFactory.createMaintainService(serverAddr);
            maintainService.updateInstance(instance.getServiceName(), group, instance);
            instanceCache.put(instance.getInstanceId(), instance);
            log.info("Updated instance enabled: {}", dto.getInstanceId());
        } catch (NacosException e) {
            log.error("Failed to update instance enabled: {}", e.getMessage());
        }
    }

    @Override
    public List<InterfaceDTO> mapToInterfaceDTO(String result, String path, String summary, String prePath) {
        String newPrePath = prePath.replace("/**", "");
        List<InterfaceDTO> faces = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(result);
            JsonNode pathsNode = root.get("paths");

            if (pathsNode == null || !pathsNode.isObject()) {
                return faces;
            }

            Iterator<String> fieldNames = pathsNode.fieldNames();
            while (fieldNames.hasNext()) {
                String p = fieldNames.next();

                // 路径过滤
                if (path != null && !path.isEmpty() && !p.contains(path)) {
                    continue;
                }

                JsonNode pathNode = pathsNode.get(p);
                if (!pathNode.isObject()) {
                    continue;
                }

                // 获取第一个 HTTP 方法
                Iterator<String> methods = pathNode.fieldNames();
                if (!methods.hasNext()) {
                    continue;
                }

                String type = methods.next();
                JsonNode methodNode = pathNode.get(type);

                InterfaceDTO dto = new InterfaceDTO();
                dto.setPath(p);
                dto.setType(type.toUpperCase());

                if (methodNode.has("tags") && methodNode.get("tags").isArray() && methodNode.get("tags").size() > 0) {
                    dto.setTag(methodNode.get("tags").get(0).asText());
                }

                String summaryResult = methodNode.has("summary") ? methodNode.get("summary").asText() : "";

                // 摘要过滤
                if (summary != null && !summary.isEmpty()) {
                    if (summaryResult.contains(summary)) {
                        dto.setSummary(summaryResult);
                        faces.add(dto);
                    }
                } else {
                    dto.setSummary(summaryResult);
                    faces.add(dto);
                }
            }

            // 添加前缀路径并标记是否已添加
            faces.forEach(x -> x.setPath(newPrePath.concat(x.getPath())));

        } catch (Exception e) {
            log.error("Failed to parse interface info: {}", e.getMessage());
        }

        return faces;
    }
}
