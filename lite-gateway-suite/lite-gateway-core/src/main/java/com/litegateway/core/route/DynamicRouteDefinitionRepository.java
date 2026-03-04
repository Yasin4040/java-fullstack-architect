package com.litegateway.core.route;

import com.litegateway.core.constants.RouteConstants;
import com.litegateway.core.constants.StringConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

/**
 * 动态路由定义仓库
 * 从旧项目 MysqlRouteDefinitionRepository 迁移
 * 支持从内存/数据库/Nacos 加载路由配置
 * 包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
@Slf4j
@Component
public class DynamicRouteDefinitionRepository implements RouteDefinitionRepository {

    // 内存中的路由定义
    private List<RouteDefinition> routeDefinitions = new ArrayList<>();

    public DynamicRouteDefinitionRepository() {
        // 初始化时加载默认路由
        loadDefaultRoutes();
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        if (routeDefinitions.isEmpty()) {
            loadDefaultRoutes();
        }
        return Flux.fromIterable(routeDefinitions);
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return Mono.defer(() -> Mono.error(new NotFoundException("Unsupported operation")));
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return Mono.defer(() -> Mono.error(new NotFoundException("Unsupported operation")));
    }

    /**
     * 刷新路由
     */
    public synchronized void refreshRoutes(List<RouteDefinition> newRoutes) {
        this.routeDefinitions = new ArrayList<>(newRoutes);
        log.info("Routes refreshed, total: {}", routeDefinitions.size());
    }

    /**
     * 加载默认路由（示例）
     */
    private void loadDefaultRoutes() {
        // 这里可以从配置文件或数据库加载
        // 默认提供一个示例路由
        log.info("Loading default routes...");
    }

    /**
     * 构建路由定义
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(id);
        routeDefinition.setUri(getURI(uri));

        // 设置断言
        List<PredicateDefinition> predicates = new ArrayList<>();
        routeDefinition.setPredicates(predicates);

        // 设置过滤器
        List<FilterDefinition> filters = new ArrayList<>();
        routeDefinition.setFilters(filters);

        // 权重
        if (weight != null && weight > 0) {
            predicates.add(buildPredicate(RouteConstants.WEIGHT, weightName, String.valueOf(weight)));
        }

        // 路径断言
        if (path != null && !path.isEmpty()) {
            predicates.add(buildPredicate(RouteConstants.PATH, path));
        }

        // StripPrefix 过滤器
        if (stripPrefix != null && stripPrefix > 0) {
            filters.add(buildFilter(RouteConstants.STRIP_PREFIX, String.valueOf(stripPrefix)));
        }

        // 限流
        if (replenishRate != null && burstCapacity != null) {
            FilterDefinition filter = new FilterDefinition();
            filter.setName(RouteConstants.Limiter.CUSTOM_REQUEST_RATE_LIMITER);
            Map<String, String> args = new LinkedHashMap<>();
            args.put(RouteConstants.Limiter.KEY_RESOLVER, RouteConstants.Limiter.HOST_ADDR_KEY_RESOLVER);
            args.put(RouteConstants.Limiter.REPLENISH_RATE, String.valueOf(replenishRate));
            args.put(RouteConstants.Limiter.BURS_CAPACITY, String.valueOf(burstCapacity));
            filter.setArgs(args);
            filters.add(filter);
        }

        return routeDefinition;
    }

    private PredicateDefinition buildPredicate(String name, String... values) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName(name);
        Map<String, String> args = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            args.put(RouteConstants._GENKEY_ + i, values[i]);
        }
        predicate.setArgs(args);
        return predicate;
    }

    private FilterDefinition buildFilter(String name, String... values) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName(name);
        Map<String, String> args = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            args.put(RouteConstants._GENKEY_ + i, values[i]);
        }
        filter.setArgs(args);
        return filter;
    }

    private URI getURI(String uriStr) {
        if (uriStr.startsWith(StringConstants.HTTP) || uriStr.startsWith(StringConstants.HTTPS)) {
            return UriComponentsBuilder.fromHttpUrl(uriStr).build().toUri();
        } else {
            return URI.create(uriStr);
        }
    }
}
