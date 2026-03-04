# RouteConfigProvider 路由配置提供者设计 Spec

## 设计目标
剥离 Nacos 配置中心的强依赖，设计通用的 RouteConfigProvider 接口，支持：
1. **静态路由模式**（默认）：YAML/Properties 文件配置，无外部依赖
2. **数据库路由模式**：MySQL 存储，支持动态刷新
3. **Nacos 路由模式**（可选）：动态配置，需显式开启
4. **Redis 路由模式**（可选）：高性能缓存场景

**核心理念**：Spring Cloud Gateway = 增强版 Nginx，可以完全不依赖注册中心

---

## 核心接口设计

### 1. RouteConfigProvider 接口

```java
/**
 * 路由配置提供者接口
 * 支持多种路由数据源：静态配置、数据库、Nacos、Redis 等
 */
public interface RouteConfigProvider {
    
    /**
     * 获取提供者唯一标识
     * 如：static、database、nacos、redis
     */
    String getProviderId();
    
    /**
     * 获取提供者名称
     */
    String getProviderName();
    
    /**
     * 是否支持动态刷新
     */
    boolean isDynamic();
    
    /**
     * 加载所有路由定义
     * @return 路由定义列表
     */
    List<RouteDefinition> loadRoutes();
    
    /**
     * 保存路由（动态提供者实现）
     * @param route 路由定义
     */
    void saveRoute(RouteDefinition route);
    
    /**
     * 删除路由（动态提供者实现）
     * @param routeId 路由 ID
     */
    void deleteRoute(String routeId);
    
    /**
     * 监听路由变更（动态提供者实现）
     * @param listener 变更监听器
     */
    void addChangeListener(RouteChangeListener listener);
    
    /**
     * 刷新路由（动态提供者实现）
     */
    void refresh();
    
    /**
     * 健康检查
     */
    boolean healthCheck();
}

/**
 * 路由变更监听器
 */
public interface RouteChangeListener {
    void onRoutesChanged(List<RouteDefinition> routes);
}
```

### 2. 路由定义扩展模型

```java
/**
 * 网关路由定义（扩展 Spring Cloud Gateway 的 RouteDefinition）
 */
@Data
@Builder
public class GatewayRouteDefinition {
    
    /**
     * 路由 ID
     */
    private String id;
    
    /**
     * 路由名称（用于展示）
     */
    private String name;
    
    /**
     * 目标 URI
     * 如：http://localhost:8080 或 lb://service-name
     */
    private String uri;
    
    /**
     * 路由优先级（数字越小优先级越高）
     */
    private Integer order;
    
    /**
     * 路径断言
     */
    private List<String> paths;
    
    /**
     * 主机断言
     */
    private List<String> hosts;
    
    /**
     * 方法断言（GET/POST/PUT/DELETE）
     */
    private List<String> methods;
    
    /**
     * Header 断言
     */
    private Map<String, String> headers;
    
    /**
     * 查询参数断言
     */
    private Map<String, String> queryParams;
    
    /**
     * 远程地址断言
     */
    private List<String> remoteAddrs;
    
    /**
     * 权重配置（用于灰度发布）
     */
    private WeightConfig weight;
    
    /**
     * 过滤器配置
     */
    private List<FilterConfig> filters;
    
    /**
     * 元数据（扩展信息）
     */
    private Map<String, Object> metadata;
    
    /**
     * 状态：enabled/disabled
     */
    private String status;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

/**
 * 权重配置
 */
@Data
public class WeightConfig {
    private String group;
    private Integer weight;
}

/**
 * 过滤器配置
 */
@Data
public class FilterConfig {
    private String name;
    private Map<String, String> args;
}
```

---

## 静态路由配置提供者（默认实现）

### 1. StaticRouteConfigProvider

```java
/**
 * 静态路由配置提供者
 * 从 YAML/Properties 文件加载路由配置
 * 无需任何外部依赖，开箱即用
 */
@Component
@ConditionalOnProperty(name = "gateway.route.provider", havingValue = "static", matchIfMissing = true)
@Order(1)
@Slf4j
public class StaticRouteConfigProvider implements RouteConfigProvider {
    
    @Autowired
    private StaticRouteProperties staticRouteProperties;
    
    @Override
    public String getProviderId() {
        return "static";
    }
    
    @Override
    public String getProviderName() {
        return "Static Configuration Provider";
    }
    
    @Override
    public boolean isDynamic() {
        return false;
    }
    
    @Override
    public List<RouteDefinition> loadRoutes() {
        log.info("Loading routes from static configuration...");
        
        List<RouteDefinition> routes = new ArrayList<>();
        List<GatewayRouteDefinition> staticRoutes = staticRouteProperties.getRoutes();
        
        if (CollectionUtils.isEmpty(staticRoutes)) {
            log.warn("No static routes configured");
            return routes;
        }
        
        for (GatewayRouteDefinition routeDef : staticRoutes) {
            if (!"enabled".equals(routeDef.getStatus())) {
                continue;
            }
            RouteDefinition route = convertToRouteDefinition(routeDef);
            routes.add(route);
            log.info("Loaded static route: {} -> {}", routeDef.getId(), routeDef.getUri());
        }
        
        return routes;
    }
    
    @Override
    public void saveRoute(RouteDefinition route) {
        throw new UnsupportedOperationException("Static provider does not support save");
    }
    
    @Override
    public void deleteRoute(String routeId) {
        throw new UnsupportedOperationException("Static provider does not support delete");
    }
    
    @Override
    public void addChangeListener(RouteChangeListener listener) {
        log.warn("Static provider does not support change listeners");
    }
    
    @Override
    public void refresh() {
        log.warn("Static provider does not support refresh");
    }
    
    @Override
    public boolean healthCheck() {
        return true;
    }
    
    private RouteDefinition convertToRouteDefinition(GatewayRouteDefinition routeDef) {
        RouteDefinition route = new RouteDefinition();
        route.setId(routeDef.getId());
        route.setUri(URI.create(routeDef.getUri()));
        route.setOrder(routeDef.getOrder());
        
        List<PredicateDefinition> predicates = new ArrayList<>();
        
        // Path 断言
        if (CollectionUtils.isNotEmpty(routeDef.getPaths())) {
            for (String path : routeDef.getPaths()) {
                PredicateDefinition predicate = new PredicateDefinition();
                predicate.setName("Path");
                predicate.addArg("_genkey_0", path);
                predicates.add(predicate);
            }
        }
        
        // Method 断言
        if (CollectionUtils.isNotEmpty(routeDef.getMethods())) {
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setName("Method");
            for (int i = 0; i < routeDef.getMethods().size(); i++) {
                predicate.addArg("_genkey_" + i, routeDef.getMethods().get(i));
            }
            predicates.add(predicate);
        }
        
        route.setPredicates(predicates);
        
        // 过滤器
        List<FilterDefinition> filters = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(routeDef.getFilters())) {
            for (FilterConfig filterConfig : routeDef.getFilters()) {
                FilterDefinition filter = new FilterDefinition();
                filter.setName(filterConfig.getName());
                filter.setArgs(filterConfig.getArgs());
                filters.add(filter);
            }
        }
        route.setFilters(filters);
        
        return route;
    }
}
```

### 2. 静态路由配置属性

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.routes")
public class StaticRouteProperties {
    
    /**
     * 静态路由列表
     */
    private List<GatewayRouteDefinition> routes = new ArrayList<>();
}
```

---

## 数据库路由配置提供者

```java
/**
 * 数据库路由配置提供者
 * 从 MySQL 加载路由配置，支持动态刷新
 */
@Component
@ConditionalOnProperty(name = "gateway.route.provider", havingValue = "database")
@Order(2)
@Slf4j
public class DatabaseRouteConfigProvider implements RouteConfigProvider {
    
    @Autowired
    private RouteRepository routeRepository;
    
    @Autowired
    private RouteMessagePublisher messagePublisher;
    
    private List<RouteChangeListener> listeners = new ArrayList<>();
    
    @Override
    public String getProviderId() {
        return "database";
    }
    
    @Override
    public String getProviderName() {
        return "Database Configuration Provider";
    }
    
    @Override
    public boolean isDynamic() {
        return true;
    }
    
    @Override
    public List<RouteDefinition> loadRoutes() {
        log.info("Loading routes from database...");
        
        List<GatewayRoute> routes = routeRepository.findByStatus("enabled");
        
        return routes.stream()
                .map(this::convertToRouteDefinition)
                .collect(Collectors.toList());
    }
    
    @Override
    public void saveRoute(RouteDefinition route) {
        GatewayRoute entity = convertToEntity(route);
        routeRepository.save(entity);
        notifyListeners();
    }
    
    @Override
    public void deleteRoute(String routeId) {
        routeRepository.deleteById(routeId);
        notifyListeners();
    }
    
    @Override
    public void addChangeListener(RouteChangeListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void refresh() {
        notifyListeners();
    }
    
    @Override
    public boolean healthCheck() {
        try {
            routeRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void notifyListeners() {
        List<RouteDefinition> routes = loadRoutes();
        listeners.forEach(listener -> listener.onRoutesChanged(routes));
    }
    
    // convert methods...
}
```

---

## Nacos 路由配置提供者（可选）

```java
/**
 * Nacos 路由配置提供者
 * 从 Nacos Config 加载路由配置，支持动态刷新
 * 需要显式开启：gateway.route.provider=nacos
 */
@Component
@ConditionalOnProperty(name = "gateway.route.provider", havingValue = "nacos")
@Order(3)
@Slf4j
public class NacosRouteConfigProvider implements RouteConfigProvider {
    
    @Autowired
    private NacosConfigManager nacosConfigManager;
    
    private List<RouteChangeListener> listeners = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        // 监听 Nacos 配置变更
        nacosConfigManager.addListener("gateway-routes", new Listener() {
            @Override
            public void receiveConfigInfo(String config) {
                log.info("Received route config change from Nacos");
                notifyListeners();
            }
        });
    }
    
    @Override
    public String getProviderId() {
        return "nacos";
    }
    
    @Override
    public String getProviderName() {
        return "Nacos Configuration Provider";
    }
    
    @Override
    public boolean isDynamic() {
        return true;
    }
    
    @Override
    public List<RouteDefinition> loadRoutes() {
        String config = nacosConfigManager.getConfig("gateway-routes");
        return parseRoutes(config);
    }
    
    @Override
    public void saveRoute(RouteDefinition route) {
        // 保存到 Nacos
        String config = serializeRoutes(loadRoutes());
        nacosConfigManager.publishConfig("gateway-routes", config);
    }
    
    @Override
    public void deleteRoute(String routeId) {
        List<RouteDefinition> routes = loadRoutes().stream()
                .filter(r -> !r.getId().equals(routeId))
                .collect(Collectors.toList());
        nacosConfigManager.publishConfig("gateway-routes", serializeRoutes(routes));
    }
    
    @Override
    public void addChangeListener(RouteChangeListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void refresh() {
        notifyListeners();
    }
    
    @Override
    public boolean healthCheck() {
        return nacosConfigManager.checkHealth();
    }
    
    private void notifyListeners() {
        listeners.forEach(listener -> listener.onRoutesChanged(loadRoutes()));
    }
    
    // parse and serialize methods...
}
```

---

## 路由配置管理器

```java
/**
 * 路由配置管理器
 * 统一管理路由配置，支持多提供者切换
 */
@Component
@Slf4j
public class RouteConfigManager {
    
    @Autowired
    private List<RouteConfigProvider> providers;
    
    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    
    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;
    
    private RouteConfigProvider currentProvider;
    
    @PostConstruct
    public void init() {
        // 选择当前配置的提供者
        String providerType = getConfiguredProviderType();
        currentProvider = providers.stream()
                .filter(p -> p.getProviderId().equals(providerType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No route provider found: " + providerType));
        
        log.info("Using route provider: {}", currentProvider.getProviderName());
        
        // 如果是动态提供者，添加变更监听
        if (currentProvider.isDynamic()) {
            currentProvider.addChangeListener(this::onRoutesChanged);
        }
        
        // 加载初始路由
        loadRoutes();
    }
    
    /**
     * 加载所有路由
     */
    public void loadRoutes() {
        List<RouteDefinition> routes = currentProvider.loadRoutes();
        
        // 清空现有路由
        // 注意：这里需要根据具体实现来清空
        
        // 保存新路由
        for (RouteDefinition route : routes) {
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
        }
        
        log.info("Loaded {} routes from {}", routes.size(), currentProvider.getProviderName());
    }
    
    /**
     * 保存路由
     */
    public void saveRoute(RouteDefinition route) {
        if (!currentProvider.isDynamic()) {
            throw new UnsupportedOperationException("Current provider does not support dynamic routes");
        }
        currentProvider.saveRoute(route);
    }
    
    /**
     * 删除路由
     */
    public void deleteRoute(String routeId) {
        if (!currentProvider.isDynamic()) {
            throw new UnsupportedOperationException("Current provider does not support dynamic routes");
        }
        currentProvider.deleteRoute(routeId);
    }
    
    /**
     * 刷新路由
     */
    public void refresh() {
        if (currentProvider.isDynamic()) {
            currentProvider.refresh();
        } else {
            loadRoutes();
        }
    }
    
    /**
     * 获取当前提供者
     */
    public RouteConfigProvider getCurrentProvider() {
        return currentProvider;
    }
    
    /**
     * 获取所有可用提供者
     */
    public List<RouteConfigProvider> getAllProviders() {
        return providers;
    }
    
    private void onRoutesChanged(List<RouteDefinition> routes) {
        log.info("Routes changed, reloading...");
        loadRoutes();
    }
    
    private String getConfiguredProviderType() {
        // 从配置读取，默认 static
        return System.getProperty("gateway.route.provider", "static");
    }
}
```

---

## 配置示例

### 1. 静态路由配置（默认，无外部依赖）

```yaml
# application.yml
gateway:
  route:
    provider: static  # 默认就是 static，可省略
  
  routes:
    - id: user-service
      name: 用户服务
      uri: http://localhost:8081
      status: enabled
      paths:
        - /api/user/**
      methods:
        - GET
        - POST
      filters:
        - name: StripPrefix
          args:
            parts: "1"
      description: 用户相关接口
      
    - id: order-service
      name: 订单服务
      uri: http://localhost:8082
      status: enabled
      paths:
        - /api/order/**
      filters:
        - name: StripPrefix
          args:
            parts: "1"
        - name: RequestRateLimiter
          args:
            rate-limiter: redis
            key-resolver: "#{@ipKeyResolver}"
            redis-rate-limiter.replenishRate: "10"
            redis-rate-limiter.burstCapacity: "20"
```

### 2. 数据库路由配置

```yaml
# application.yml
gateway:
  route:
    provider: database

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gateway
    username: root
    password: password
```

### 3. Nacos 路由配置（可选）

```yaml
# application.yml
gateway:
  route:
    provider: nacos

spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        data-id: gateway-routes
        group: DEFAULT_GROUP
```

---

## 使用模式对比

| 模式 | 配置方式 | 动态刷新 | 外部依赖 | 适用场景 |
|------|----------|----------|----------|----------|
| **static** | YAML/Properties | ❌ | 无 | 单体应用、开发环境、简单部署 |
| **database** | MySQL | ✅ | MySQL | 需要动态管理路由的生产环境 |
| **nacos** | Nacos Config | ✅ | Nacos | 已有 Nacos 基础设施的微服务架构 |
| **redis** | Redis | ✅ | Redis | 高性能缓存场景 |

---

## 核心优势

1. **零依赖启动**：默认静态配置模式，无需任何外部服务
2. **渐进式扩展**：需要动态配置时再引入数据库或 Nacos
3. **统一抽象**：业务代码不感知具体配置来源
4. **灵活切换**：通过配置即可切换不同提供者
5. **降级能力**：动态提供者故障时可降级到静态配置