# Lite Gateway 后端数据库集成计划

## 概述
为 lite-gateway-admin 和 lite-gateway-core 两个模块添加 MyBatis-Plus + MySQL 数据库支持，完善数据持久化能力。

## 目标
1. 添加 MyBatis-Plus 依赖和配置
2. 创建数据库表结构
3. 实现实体类、Mapper、Service 完整数据访问层
4. 替换现有的内存存储为数据库存储
5. 保持代码整洁，便于开源

---

## 第一阶段：添加依赖和配置

### 1.1 添加 MyBatis-Plus 依赖
**文件**: `lite-gateway-admin/pom.xml` 和 `lite-gateway-core/pom.xml`

添加以下依赖：
- mybatis-plus-boot-starter (3.5.5)
- mysql-connector-java (8.0.33)
- druid-spring-boot-starter (连接池)

### 1.2 数据库配置
**文件**: `application.yml`

配置数据源：
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:lite_gateway}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
```

---

## 第二阶段：数据库表设计

### 2.1 网关路由表 (gateway_route)
```sql
CREATE TABLE `gateway_route` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `route_id` varchar(64) NOT NULL COMMENT '路由ID',
  `name` varchar(128) DEFAULT NULL COMMENT '路由名称',
  `uri` varchar(256) NOT NULL COMMENT '目标URI',
  `path` varchar(256) DEFAULT NULL COMMENT '路径断言',
  `strip_prefix` int(11) DEFAULT '0' COMMENT '路径截取前缀数',
  `host` varchar(256) DEFAULT NULL COMMENT '主机断言',
  `remote_addr` varchar(256) DEFAULT NULL COMMENT '远程地址断言',
  `header` varchar(512) DEFAULT NULL COMMENT 'Header断言',
  `filter_rate_limiter_name` varchar(64) DEFAULT NULL COMMENT '限流器名称',
  `replenish_rate` int(11) DEFAULT NULL COMMENT '每秒补充令牌数',
  `burst_capacity` int(11) DEFAULT NULL COMMENT '令牌桶容量',
  `weight` int(11) DEFAULT NULL COMMENT '权重',
  `weight_name` varchar(64) DEFAULT NULL COMMENT '权重分组名',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0启用 1禁用',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_id` (`route_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关路由表';
```

### 2.2 IP黑名单表 (ip_blacklist)
```sql
CREATE TABLE `ip_blacklist` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ip` varchar(64) NOT NULL COMMENT 'IP地址',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP黑名单表';
```

### 2.3 白名单表 (white_list)
```sql
CREATE TABLE `white_list` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `path` varchar(256) NOT NULL COMMENT '路径',
  `description` varchar(256) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='白名单表';
```

### 2.4 系统用户表 (sys_user) - 可选
```sql
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0正常 1禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

---

## 第三阶段：代码实现

### 3.1 实体类 (Entity)

#### lite-gateway-admin 模块

**GatewayRoute.java**
```java
@Data
@TableName("gateway_route")
public class GatewayRoute {
    @TableId(type = IdType.AUTO)
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
    private Integer status;
    private String description;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

**IpBlacklist.java**
```java
@Data
@TableName("ip_blacklist")
public class IpBlacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ip;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
```

**WhiteList.java**
```java
@Data
@TableName("white_list")
public class WhiteList {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String path;
    private String description;
    private String createBy;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
```

### 3.2 Mapper 接口

**GatewayRouteMapper.java**
```java
@Mapper
public interface GatewayRouteMapper extends BaseMapper<GatewayRoute> {
    List<GatewayRoute> selectEnabledRoutes();
}
```

**GatewayRouteMapper.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.litegateway.admin.repository.mapper.GatewayRouteMapper">
    <select id="selectEnabledRoutes" resultType="com.litegateway.admin.repository.entity.GatewayRoute">
        SELECT * FROM gateway_route 
        WHERE status = 0 AND deleted = 0
        ORDER BY create_time DESC
    </select>
</mapper>
```

### 3.3 Service 层

**GatewayRouteService.java** (更新现有接口)
```java
public interface GatewayRouteService extends IService<GatewayRoute> {
    // 现有方法...
    
    // 新增方法
    List<GatewayRoute> getEnabledRoutes();
    void publishRouteUpdate();
}
```

**GatewayRouteServiceImpl.java** (更新实现)
```java
@Service
public class GatewayRouteServiceImpl extends ServiceImpl<GatewayRouteMapper, GatewayRoute> 
    implements GatewayRouteService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public List<GatewayRoute> getEnabledRoutes() {
        return baseMapper.selectEnabledRoutes();
    }
    
    @Override
    public void publishRouteUpdate() {
        redisTemplate.convertAndSend(RedisTypeConstants.CHANNEL, RedisTypeConstants.ROUTE_UPDATE);
    }
    
    // 其他方法实现...
}
```

### 3.4 配置类

**MybatisPlusConfig.java**
```java
@Configuration
@MapperScan("com.litegateway.admin.repository.mapper")
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

---

## 第四阶段：lite-gateway-core 模块集成

### 4.1 依赖添加
与 admin 模块相同，添加 MyBatis-Plus 依赖

### 4.2 数据同步
在 core 模块中：
1. 启动时从数据库加载路由、IP黑名单、白名单
2. 监听 Redis 消息，同步更新缓存

**DataSyncService.java**
```java
@Service
@Slf4j
public class DataSyncService {
    
    @Autowired
    private GatewayRouteMapper routeMapper;
    @Autowired
    private IpBlacklistMapper ipBlacklistMapper;
    @Autowired
    private WhiteListMapper whiteListMapper;
    
    @PostConstruct
    public void init() {
        syncAllData();
    }
    
    public void syncAllData() {
        syncRoutes();
        syncIpBlacklist();
        syncWhiteList();
    }
    
    public void syncRoutes() {
        List<GatewayRoute> routes = routeMapper.selectEnabledRoutes();
        // 更新路由缓存
        log.info("Synced {} routes", routes.size());
    }
    
    public void syncIpBlacklist() {
        List<IpBlacklist> list = ipBlacklistMapper.selectList(null);
        IpListCache.clear();
        list.forEach(item -> IpListCache.put(item.getIp(), item.getRemark()));
        log.info("Synced {} blacklisted IPs", list.size());
    }
    
    public void syncWhiteList() {
        List<WhiteList> list = whiteListMapper.selectList(null);
        WhiteListCache.clear();
        list.forEach(item -> WhiteListCache.put(item.getPath(), item.getDescription()));
        log.info("Synced {} white list items", list.size());
    }
}
```

---

## 第五阶段：Flyway 数据库迁移

### 5.1 添加 Flyway 依赖
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 5.2 迁移脚本
**文件**: `src/main/resources/db/migration/V1__init_schema.sql`

包含所有建表语句

---

## 实施步骤

1. **Step 1**: 添加依赖和基础配置
2. **Step 2**: 创建数据库表
3. **Step 3**: 实现 admin 模块的实体、Mapper、Service
4. **Step 4**: 更新 admin 模块 Controller，使用数据库操作
5. **Step 5**: 实现 core 模块的数据同步
6. **Step 6**: 测试验证

## 注意事项

1. 数据库连接信息使用环境变量配置
2. 敏感配置（密码）不提交到代码仓库
3. 使用逻辑删除而非物理删除
4. 添加适当的索引优化查询性能
5. 考虑添加数据库连接池监控
