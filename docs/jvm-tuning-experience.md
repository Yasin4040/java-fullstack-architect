# JVM调优实战经验知识卡片

> 基于真实案例，展现调优思路和效果

---

## 一、面试回答框架

```
1. 调优背景（什么系统，什么问题）
2. 调优前状况（GC频率、内存使用、性能指标）
3. 调优措施（改了哪些参数，为什么）
4. 调优效果（GC减少多少，性能提升多少）
5. 经验总结（学到了什么）
```

---

## 二、实战案例1：GC频繁调优（推荐）

### 背景

```
系统：Spring Boot电商订单系统
部署：4核8G容器，K8s环境
问题：高峰期频繁Full GC，接口响应慢，用户体验差
```

### 调优前状况

```
GC情况：
- Young GC：每分钟10-20次，每次50ms
- Full GC：每小时5-10次，每次2-3秒
- 堆内存：6G老年代经常占满

业务影响：
- 订单接口P99延迟从200ms涨到2s
- 高峰期偶尔OOM
```

### 调优措施

#### 1. 增大堆内存

```bash
# 调优前
-Xms2g -Xmx2g

# 调优后（容器有8G，堆给6G）
-Xms6g -Xmx6g
```

**原因**：原2G堆太小，老年代只有1.4G，很快占满

#### 2. 调整新生代比例

```bash
# 调优前（默认）
-XX:NewRatio=2  # 老年代:新生代 = 2:1

# 调优后
-XX:NewRatio=1  # 老年代:新生代 = 1:1
-XX:SurvivorRatio=8  # Eden:S0:S1 = 8:1:1
```

**原因**：电商系统短生命周期对象多，增大新生代减少晋升老年代

#### 3. 更换垃圾收集器

```bash
# 调优前（JDK8默认）
-XX:+UseParallelGC

# 调优后（低延迟）
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

**原因**：Parallel GC停顿时间长，G1可控停顿时间

#### 4. 配置OOM自动dump

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof
```

**原因**：方便后续排查

#### 5. 开启GC日志

```bash
# JDK8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/logs/gc.log

# JDK11+
-Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags
```

**原因**：监控GC情况，及时发现问题

### 调优效果

```
GC改善：
- Young GC：每分钟5-8次，每次30ms（频率降低，耗时减少）
- Full GC：每天1-2次，每次500ms（大幅降低）
- 老年代：稳定在3-4G，不再频繁占满

业务改善：
- 订单接口P99延迟：2s → 200ms（降低90%）
- 吞吐量提升：30%
- 再无OOM
```

### 经验总结

```
1. 先监控再调优，用GC日志和监控数据说话
2. 堆内存要给足，但不要超过物理内存的80%
3. 根据业务特点调整新生代比例
4. G1收集器适合大多数场景，停顿可控
5. 必须配置OOM自动dump，方便排查
```

---

## 三、实战案例2：元空间OOM调优

### 背景

```
系统：微服务网关，动态加载路由规则
部署：2核4G容器
问题：运行3-5天后OOM: Metaspace，必须重启
```

### 调优前状况

```
元空间：
- 默认128M，经常占满
- Full GC后元空间不释放
- 类加载数量持续增长

临时方案：
- 每天凌晨重启服务（治标不治本）
```

### 调优措施

#### 1. 增大元空间

```bash
# 调优前（默认）
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m

# 调优后
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
```

**效果**：延长OOM时间，但没解决根本问题

#### 2. 代码优化（根本解决）

```java
// 问题代码：每次都创建新的GroovyClassLoader
public Route loadRoute(String script) {
    GroovyClassLoader loader = new GroovyClassLoader();  // 每次都新建！
    Class<?> clazz = loader.parseClass(script);
    return (Route) clazz.newInstance();
}

// 优化后：复用ClassLoader，缓存编译好的类
private static final Map<String, Class<?>> routeCache = new ConcurrentHashMap<>();

public Route loadRoute(String script) {
    Class<?> clazz = routeCache.computeIfAbsent(script, s -> {
        GroovyClassLoader loader = new GroovyClassLoader();
        return loader.parseClass(s);
    });
    return (Route) clazz.newInstance();
}
```

**原因**：频繁创建ClassLoader导致类无法卸载

#### 3. 升级JDK版本

```bash
# 从JDK8升级到JDK11
```

**原因**：JDK11对元空间管理更好，类卸载更及时

### 调优效果

```
元空间：
- 稳定在200M左右，不再增长
- 服务运行3个月不重启

业务改善：
- 无需定时重启
- 路由加载速度提升50%
```

### 经验总结

```
1. 元空间OOM通常是类加载泄漏，要检查ClassLoader使用
2. 增大元空间只能缓解，代码优化才是根本
3. 动态语言（Groovy、JRuby）要特别注意ClassLoader复用
4. JDK11+对元空间管理更好，有条件建议升级
```

---

## 四、常用JVM参数速查表

### 堆内存配置

```bash
# 基础配置
-Xms4g                    # 初始堆内存
-Xmx4g                    # 最大堆内存（建议Xms=Xmx，避免动态调整）
-Xmn2g                    # 新生代大小
-XX:NewRatio=2           # 老年代:新生代 = 2:1
-XX:SurvivorRatio=8      # Eden:S0:S1 = 8:1:1
```

### 垃圾收集器选择

```bash
# JDK8
-XX:+UseG1GC             # G1收集器（推荐）
-XX:MaxGCPauseMillis=200 # 目标最大停顿时间

# JDK11+（可选ZGC，超低延迟）
-XX:+UseZGC
-XX:+ZGenerational       # JDK21+，分代ZGC
```

### 元空间配置

```bash
-XX:MetaspaceSize=256m    # 初始元空间
-XX:MaxMetaspaceSize=512m # 最大元空间
```

### OOP排查配置

```bash
-XX:+HeapDumpOnOutOfMemoryError      # OOM自动生成dump
-XX:HeapDumpPath=/logs/heapdump.hprof # dump文件路径
```

### GC日志配置

```bash
# JDK8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/logs/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=100M

# JDK11+
-Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=100m
```

### 其他常用

```bash
-XX:+DisableExplicitGC   # 禁止System.gc()
-XX:+AlwaysPreTouch       # 启动时分配所有内存，减少运行时开销
-XX:+ParallelRefProcEnabled # 并行处理引用，加快GC
```

---

## 五、面试话术（完整版）

```
【JVM调优实战经验】

我做过两个主要的JVM调优：

第一个是对电商订单系统的GC调优。
原系统4核8G容器，堆只给2G，高峰期频繁Full GC，
订单接口P99延迟涨到2秒。

我采取的调优措施：
1. 增大堆内存到6G，减少GC频率
2. 调整新生代比例为1:1，因为电商短生命周期对象多
3. 从Parallel GC换成G1 GC，控制停顿时间在200ms内
4. 配置OOM自动dump和GC日志，方便监控

调优效果：
Full GC从每小时10次降到每天1-2次，
接口P99延迟从2秒降到200ms，吞吐量提升30%。

第二个是对网关服务的元空间调优。
服务运行3-5天就OOM: Metaspace，必须重启。

我发现是动态加载路由规则时，每次都创建新的GroovyClassLoader，
导致类无法卸载。优化后复用ClassLoader并缓存编译好的类，
同时增大元空间到512M，升级JDK11。

调优效果：
元空间稳定在200M，服务运行3个月不重启。

经验总结：
调优前要先监控，用数据说话；
堆内存要给足但不要超过物理内存80%；
G1适合大多数场景；
元空间问题通常是类加载泄漏，要检查代码。
```

**时长**：3-4分钟

---

## 六、高频面试题

### Q1：JVM调优的思路是什么？

```
1. 监控现状：用GC日志、监控工具了解当前状况
2. 确定目标：降低GC频率？减少停顿时间？防止OOM？
3. 分析原因：是内存太小？收集器不合适？还是代码问题？
4. 调整参数：改堆大小、换收集器、调比例
5. 验证效果：对比调优前后的GC日志和性能指标
6. 持续监控：调优不是一次性的，要持续观察
```

### Q2：怎么选择垃圾收集器？

```
- 吞吐量优先：Parallel GC（后台计算）
- 低延迟优先：G1 GC（Web应用，推荐）
- 超低延迟：ZGC/Shenandoah（金融交易，JDK11+）

大部分场景用G1就够了，停顿时间可控，吞吐量也不错。
```

### Q3：堆内存设置多大合适？

```
- 最小：至少2G，否则GC太频繁
- 最大：不超过物理内存的80%，留内存给系统和其他进程
- 容器环境：考虑容器内存限制，不要超过容器上限
- 经验：4核8G容器，堆给6G；8核16G容器，堆给12G
```

### Q4：新生代和老年代比例怎么调？

```
- 默认NewRatio=2（老年代:新生代=2:1）
- 短生命周期对象多（Web应用）：增大新生代，NewRatio=1
- 长生命周期对象多（缓存系统）：减小新生代，NewRatio=3
- 也可以用-Xmn直接指定新生代大小
```

### Q5：调优后怎么验证效果？

```
对比指标：
1. GC频率：Young GC、Full GC次数
2. GC耗时：平均耗时、最大耗时、P99耗时
3. 吞吐量：每秒处理请求数
4. 响应时间：接口P99延迟
5. 内存使用：堆内存、老年代、元空间使用率

工具：
- GC日志分析：GCViewer、GCEasy
- 性能监控：Prometheus + Grafana
- 压测验证：JMeter
```

---

## 七、记忆口诀

```
JVM调优六步走：
监控现状定目标，
分析原因调参数，
验证效果持续跟。

堆内存，给要足，
八成物理是上限。
G1收集器最通用，
停顿可控吞吐量。

元空间，看类加载，
ClassLoader要复用。
OOM自动dump配，
排查问题不用愁。
```

---

**掌握标准**：能讲2个调优案例，包含背景、措施、效果，能回答常见调优问题。
