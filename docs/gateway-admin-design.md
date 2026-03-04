# 网关管理后台（Gateway Admin）设计方案

> Vue3 + SpringBoot 技术栈，用于管理 Spring Cloud Gateway 的动态路由、限流、日志等

---

## 一、系统架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              网关管理后台架构                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端层（Vue3）                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│   │   路由管理   │  │   限流配置   │  │   日志监控   │  │   系统管理   │       │
│   │   页面      │  │   页面      │  │   页面      │  │   页面      │       │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
│          │                │                │                │              │
│          └────────────────┴────────────────┴────────────────┘              │
│                                   │                                         │
│                          ┌────────┴────────┐                               │
│                          │   Axios + API    │                               │
│                          │   统一请求封装    │                               │
│                          └────────┬────────┘                               │
│                                   │                                         │
└───────────────────────────────────┼─────────────────────────────────────────┘
                                    │ HTTP/HTTPS
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           后端层（SpringBoot）                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Controller 层                                │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │RouteCtrl │ │RateCtrl  │ │LogCtrl   │ │UserCtrl  │ │SysCtrl   │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Service 层                                   │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │RouteSvc  │ │RateSvc   │ │LogSvc    │ │UserSvc   │ │SysSvc    │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          DAO 层                                      │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐                            │   │
│  │  │RouteMapper│ │RateMapper│ │LogMapper │  MyBatis-Plus              │   │
│  │  └──────────┘ └──────────┘ └──────────┘                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        核心功能模块                                   │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │ 路由同步器    │ │ 限流规则引擎  │ │ 日志收集器    │                │   │
│  │  │(RouteSync)   │ │(RateLimiter) │ │(LogCollector)│                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │ Nacos配置推送 │ │ Redis限流    │ │ Elasticsearch│                │   │
│  │  │(ConfigPub)   │ │(RedisRate)   │ │(日志存储)    │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              数据存储层                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   MySQL     │  │    Redis    │  │    Nacos    │  │ Elasticsearch│        │
│  │  (业务数据)  │  │  (限流缓存)  │  │  (配置中心)  │  │  (日志存储)  │        │
│  │             │  │             │  │             │  │              │        │
│  │ - 路由表    │  │ - 限流计数   │  │ - 动态路由   │  │ - 访问日志   │        │
│  │ - 限流规则  │  │ - Token桶   │  │ - 配置监听   │  │ - 错误日志   │        │
│  │ - 用户表    │  │ - 黑名单    │  │             │  │ - 性能日志   │        │
│  │ - 系统配置  │  │             │  │             │  │              │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Spring Cloud Gateway                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    动态路由配置（从Nacos读取）                         │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │   │
│  │  │ 路由规则  │ │ 过滤器链  │ │ 负载均衡  │ │ 限流熔断  │               │   │
│  │  │ Route    │ │ Filter   │ │ LB       │ │ Circuit  │               │   │
│  │  │          │ │          │ │          │ │ Breaker  │               │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心功能模块

### 1. 路由管理模块

```java
/**
 * 路由管理核心功能
 */
@Service
public class RouteService {
    
    @Autowired
    private RouteDefinitionRepository routeRepository;
    
    @Autowired
    private NacosConfigPublisher configPublisher;
    
    /**
     * 添加路由并同步到Gateway
     */
    public void addRoute(RouteDefinition route) {
        // 1. 保存到数据库
        routeRepository.save(route);
        
        // 2. 发布到Nacos配置中心
        configPublisher.publishRoute(route);
        
        // 3. Gateway监听Nacos配置变化，自动刷新路由
    }
    
    /**
     * 删除路由
     */
    public void deleteRoute(String routeId) {
        routeRepository.deleteById(routeId);
        configPublisher.removeRoute(routeId);
    }
}
```

**路由配置示例**：
```yaml
# 存储在Nacos的配置
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

---

### 2. 限流管理模块

```java
/**
 * 限流规则管理
 */
@Service
public class RateLimitService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RateLimitRuleRepository ruleRepository;
    
    /**
     * 基于Redis的令牌桶限流
     */
    public boolean isAllowed(String key, RateLimitRule rule) {
        // Redis Lua脚本实现令牌桶
        String luaScript = 
            "local rate = redis.call('hget', KEYS[1], 'rate');" +
            "local capacity = redis.call('hget', KEYS[1], 'capacity');" +
            "local tokens = redis.call('hget', KEYS[1], 'tokens');" +
            "local lastTime = redis.call('hget', KEYS[1], 'lastTime');" +
            // ... 令牌桶算法
            "return allowed;";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList("ratelimit:" + key)
        );
        
        return result != null && result == 1L;
    }
}
```

**限流规则表设计**：
```sql
CREATE TABLE rate_limit_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_id VARCHAR(64) NOT NULL COMMENT '路由ID',
    limit_type TINYINT NOT NULL COMMENT '限流类型：1-IP限流 2-用户限流 3-全局限流',
    key_prefix VARCHAR(64) COMMENT '限流key前缀',
    replenish_rate INT NOT NULL COMMENT '每秒补充令牌数',
    burst_capacity INT NOT NULL COMMENT '令牌桶容量',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流规则表';
```

---

### 3. 日志监控模块

```java
/**
 * 网关日志收集与查询
 */
@Service
public class GatewayLogService {
    
    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;
    
    /**
     * 保存访问日志到ES
     */
    public void saveAccessLog(GatewayAccessLog log) {
        elasticsearchTemplate.save(log);
    }
    
    /**
     * 查询访问日志
     */
    public Page<GatewayAccessLog> queryLogs(LogQueryRequest request) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        
        if (StringUtils.hasText(request.getRouteId())) {
            query.must(QueryBuilders.termQuery("routeId", request.getRouteId()));
        }
        
        if (StringUtils.hasText(request.getPath())) {
            query.must(QueryBuilders.wildcardQuery("path", "*" + request.getPath() + "*"));
        }
        
        if (request.getStartTime() != null && request.getEndTime() != null) {
            query.must(QueryBuilders.rangeQuery("timestamp")
                .gte(request.getStartTime())
                .lte(request.getEndTime()));
        }
        
        // 执行查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(query)
            .withPageable(PageRequest.of(request.getPage(), request.getSize()))
            .withSort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC))
            .build();
        
        return elasticsearchTemplate.queryForPage(searchQuery, GatewayAccessLog.class);
    }
}
```

**日志数据模型**：
```java
@Document(indexName = "gateway-logs-{yyyy.MM.dd}", type = "_doc")
public class GatewayAccessLog {
    
    @Id
    private String id;
    
    private String routeId;          // 路由ID
    private String path;             // 请求路径
    private String method;           // HTTP方法
    private String clientIp;         // 客户端IP
    private String userId;           // 用户ID
    private Long requestTime;        // 请求时间
    private Long responseTime;       // 响应时间
    private Long duration;           // 耗时(ms)
    private Integer statusCode;      // 状态码
    private Long requestSize;        // 请求大小
    private Long responseSize;       // 响应大小
    private String errorMsg;         // 错误信息
    
    // getters/setters
}
```

---

## 三、数据库设计

### 核心表结构

```sql
-- 1. 路由定义表
CREATE TABLE gateway_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_id VARCHAR(64) NOT NULL UNIQUE COMMENT '路由ID',
    route_name VARCHAR(128) NOT NULL COMMENT '路由名称',
    uri VARCHAR(256) NOT NULL COMMENT '目标URI',
    predicates JSON COMMENT '断言配置（JSON格式）',
    filters JSON COMMENT '过滤器配置（JSON格式）',
    metadata JSON COMMENT '元数据',
    order_num INT DEFAULT 0 COMMENT '优先级，数字越小优先级越高',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    description VARCHAR(512) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关路由表';

-- 2. 限流规则表
CREATE TABLE rate_limit_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    route_id VARCHAR(64) COMMENT '关联路由ID',
    limit_type TINYINT NOT NULL COMMENT '限流类型：1-IP 2-用户 3-全局',
    key_prefix VARCHAR(64) COMMENT '限流key前缀',
    replenish_rate INT NOT NULL COMMENT '每秒补充令牌数',
    burst_capacity INT NOT NULL COMMENT '令牌桶容量',
    requested_tokens INT DEFAULT 1 COMMENT '每次请求消耗令牌数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流规则表';

-- 3. 系统用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码（加密）',
    real_name VARCHAR(64) COMMENT '真实姓名',
    email VARCHAR(128) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 4. 操作日志表
CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(64) COMMENT '操作用户名',
    operation VARCHAR(64) COMMENT '操作类型',
    method VARCHAR(256) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(64) COMMENT 'IP地址',
    status TINYINT COMMENT '操作状态：0-失败 1-成功',
    error_msg TEXT COMMENT '错误信息',
    duration INT COMMENT '执行时长(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

---

## 四、前端设计（Vue3）

### 项目结构

```
gateway-admin-ui/
├── public/
├── src/
│   ├── api/                    # API接口封装
│   │   ├── route.ts           # 路由管理接口
│   │   ├── rateLimit.ts       # 限流管理接口
│   │   ├── log.ts             # 日志查询接口
│   │   └── user.ts            # 用户管理接口
│   │
│   ├── components/             # 公共组件
│   │   ├── RouteForm.vue      # 路由表单
│   │   ├── RateLimitForm.vue  # 限流规则表单
│   │   └── LogTable.vue       # 日志表格
│   │
│   ├── views/                  # 页面视图
│   │   ├── route/
│   │   │   ├── index.vue      # 路由列表
│   │   │   └── detail.vue     # 路由详情
│   │   ├── ratelimit/
│   │   │   └── index.vue      # 限流配置
│   │   ├── log/
│   │   │   └── index.vue      # 日志监控
│   │   └── system/
│   │       ├── user.vue       # 用户管理
│   │       └── setting.vue    # 系统设置
│   │
│   ├── router/                 # 路由配置
│   ├── store/                  # Pinia状态管理
│   ├── utils/                  # 工具函数
│   │   ├── request.ts         # Axios封装
│   │   └── auth.ts            # 权限工具
│   │
│   ├── App.vue
│   └── main.ts
│
├── package.json
├── vite.config.ts
└── tsconfig.json
```

### 核心页面设计

```vue
<!-- 路由管理页面 -->
<template>
  <div class="route-management">
    <!-- 搜索栏 -->
    <el-form :model="queryForm" inline>
      <el-form-item label="路由ID">
        <el-input v-model="queryForm.routeId" placeholder="请输入路由ID" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryForm.status">
          <el-option label="全部" value="" />
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button type="success" @click="handleAdd">新增路由</el-button>
      </el-form-item>
    </el-form>
    
    <!-- 路由列表 -->
    <el-table :data="routeList" v-loading="loading">
      <el-table-column prop="routeId" label="路由ID" width="150" />
      <el-table-column prop="routeName" label="路由名称" width="180" />
      <el-table-column prop="uri" label="目标URI" show-overflow-tooltip />
      <el-table-column prop="orderNum" label="优先级" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="success" link @click="handleSync(row)">同步</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      @change="handleQuery"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRouteList, syncRoute, deleteRoute } from '@/api/route'

const loading = ref(false)
const routeList = ref([])
const queryForm = reactive({
  routeId: '',
  status: ''
})
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询路由列表
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await getRouteList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    routeList.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

// 同步路由到Gateway
const handleSync = async (row: any) => {
  try {
    await syncRoute(row.routeId)
    ElMessage.success('同步成功')
  } catch (error) {
    ElMessage.error('同步失败')
  }
}

onMounted(() => {
  handleQuery()
})
</script>
```

---

## 五、技术选型

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue3 | 3.3+ | 组合式API |
| | TypeScript | 5.0+ | 类型安全 |
| | Element Plus | 2.3+ | UI组件库 |
| | Pinia | 2.1+ | 状态管理 |
| | Axios | 1.4+ | HTTP请求 |
| | Vite | 4.3+ | 构建工具 |
| **后端** | SpringBoot | 2.7+ | 基础框架 |
| | Spring Cloud Gateway | 3.1+ | 网关 |
| | Nacos | 2.2+ | 配置中心 |
| | MyBatis-Plus | 3.5+ | ORM框架 |
| | Redis | 6.2+ | 限流缓存 |
| | Elasticsearch | 7.17+ | 日志存储 |
| | MySQL | 8.0+ | 业务数据 |

---

## 六、部署架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              生产环境部署                                     │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │    Nginx    │
                              │  (负载均衡)  │
                              └──────┬──────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
            ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
            │  Gateway    │  │  Gateway    │  │  Gateway    │
            │  Instance 1 │  │  Instance 2 │  │  Instance 3 │
            │  (Docker)   │  │  (Docker)   │  │  (Docker)   │
            └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
                   │                │                │
                   └────────────────┼────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
            ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
            │  Admin      │ │  Admin      │ │  Admin      │
            │  Service 1  │ │  Service 2  │ │  Service 3  │
            │  (Docker)   │ │  (Docker)   │ │  (Docker)   │
            └─────────────┘ └─────────────┘ └─────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              中间件层                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Nacos     │  │    Redis    │  │    MySQL    │  │      ES     │        │
│  │  (集群)     │  │  (哨兵/集群) │  │  (主从)     │  │   (集群)    │        │
│  │             │  │             │  │             │  │             │        │
│  │ - 配置中心  │  │ - 限流缓存  │  │ - 业务数据  │  │ - 日志存储  │        │
│  │ - 服务发现  │  │ - Session   │  │ - 路由规则  │  │ - 日志搜索  │        │
│  │             │  │             │  │             │  │             │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 七、核心流程

### 动态路由更新流程

```
┌─────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Admin  │────▶│   MySQL     │────▶│   Nacos     │────▶│  Gateway    │
│  后台   │     │  (保存路由)  │     │  (推送配置)  │     │ (监听+刷新)  │
└─────────┘     └─────────────┘     └─────────────┘     └─────────────┘
     │                                                    │
     │ 1. 用户在后台新增/修改路由                          │
     │                                                    │
     │ 2. 保存到MySQL                                     │
     │                                                    │
     │ 3. 推送到Nacos配置中心                              │
     │                                                    │
     │ 4. Gateway监听Nacos配置变化                         │
     │                                                    │
     │ 5. Gateway自动刷新路由表                            │
     │                                                    │
     │ 6. 新路由生效                                       │
     │◀───────────────────────────────────────────────────┘
```

---

这个设计方案是否符合你的需求？需要我详细展开某个模块吗？
