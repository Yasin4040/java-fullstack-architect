# 通过日志排查OOM的完整步骤

> 不用dump文件，只用日志快速定位OOM

---

## 一、日志排查的优势和局限

| 方式 | 优势 | 局限 |
|------|------|------|
| **日志排查** | 快速、不需要大文件、线上可操作 | 只能定位大概范围，不能精确定位对象 |
| **MAT分析** | 精确定位对象和引用链 | 需要dump文件，大文件分析慢 |

**建议**：先日志排查定位范围，再用MAT精确定位。

---

## 二、需要配置的日志

### 1. OOM自动dump（必配）

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof
```

### 2. GC日志（必配）

```bash
# JDK 8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/logs/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=100M

# JDK 9+
-Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=100m
```

### 3. 应用日志（业务日志）

```yaml
# logback.xml
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 文件输出，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

## 三、日志排查四步走

### Step 1：查看OOM错误日志

**看什么**：
```bash
# 查看应用日志
grep -n "OutOfMemoryError" /logs/app.log
grep -n "OutOfMemoryError" /logs/app.log | tail -20
```

**关键信息**：
```
2024-03-15 09:23:45.123 [http-nio-8080-exec-5] ERROR c.e.service.FileSyncService - 文件同步失败
java.lang.OutOfMemoryError: Java heap space
    at java.base/java.io.ByteArrayOutputStream.grow(ByteArrayOutputStream.java:120)
    at java.base/java.io.ByteArrayOutputStream.ensureCapacity(ByteArrayOutputStream.java:95)
    at java.base/java.io.ByteArrayOutputStream.write(ByteArrayOutputStream.java:65)
    at org.apache.commons.net.ftp.FTPClient.downloadFile(FTPClient.java:456)
    at com.example.service.FileSyncService.syncFile(FileSyncService.java:78)
```

**分析**：
- OOM类型：`Java heap space`（堆OOM）
- 发生时间：`09:23:45`
- 发生位置：`FileSyncService.syncFile`第78行
- 操作：`FTPClient.downloadFile`

**面试话术**：
```
首先查看应用日志，搜索OutOfMemoryError，
发现是Java heap space，发生在FileSyncService的syncFile方法，
具体是在FTPClient.downloadFile时，分配byte数组失败。
```

---

### Step 2：分析GC日志

**看什么**：
```bash
# 查看GC日志
tail -100 /logs/gc.log

# 或者搜索Full GC
grep -n "Full GC" /logs/gc.log | tail -20
```

**关键信息**：
```
[2024-03-15T09:23:44.123+0800] GC(1234) Pause Full GC (Allocation Failure) 4096M->4095M(4096M) 2.345s
[2024-03-15T09:23:44.567+0800] GC(1235) Pause Full GC (Allocation Failure) 4096M->4096M(4096M) 2.456s
[2024-03-15T09:23:45.012+0800] GC(1236) Pause Full GC (Allocation Failure) 4096M->4096M(4096M) 2.567s
```

**分析**：
- 连续Full GC，内存回收不掉（4096M->4096M）
- Full GC耗时2秒+，系统卡顿
- 最后OOM

**面试话术**：
```
查看GC日志，发现OOM前连续多次Full GC，
堆内存从4G降到4G，一点都回收不掉，
每次Full GC耗时2秒多，最后OOM。
```

**GC日志关键指标**：

| 指标 | 含义 | 你的案例 |
|------|------|---------|
| `Pause Full GC` | 全量GC，STW | 连续多次 |
| `Allocation Failure` | 分配失败，触发GC | 内存不够 |
| `4096M->4096M` | GC前后内存 | 一点没回收 |
| `2.345s` | GC耗时 | 超过2秒，卡顿 |

---

### Step 3：查看业务日志上下文

**看什么**：
```bash
# 查看OOM前后的业务日志
grep -A 10 -B 10 "2024-03-15 09:23:45" /logs/app.log

# 或者按时间范围查看
sed -n '/2024-03-15 09:20:00/,/2024-03-15 09:25:00/p' /logs/app.log
```

**关键信息**：
```
2024-03-15 09:23:40.123 [scheduler-1] INFO  c.e.service.FileSyncService - 开始同步文件: /ftp/large_file_1.zip, 大小: 1024MB
2024-03-15 09:23:42.456 [scheduler-1] INFO  c.e.service.FileSyncService - 开始同步文件: /ftp/large_file_2.zip, 大小: 1024MB
2024-03-15 09:23:44.789 [scheduler-1] INFO  c.e.service.FileSyncService - 开始同步文件: /ftp/large_file_3.zip, 大小: 1024MB
2024-03-15 09:23:45.123 [http-nio-8080-exec-5] ERROR c.e.service.FileSyncService - 文件同步失败
java.lang.OutOfMemoryError: Java heap space
```

**分析**：
- OOM前连续同步3个大文件，每个1GB
- 内存占用：1GB + 1GB + 1GB = 3GB+
- 加上其他对象，超过4G堆内存

**面试话术**：
```
查看OOM前后的业务日志，
发现OOM前连续同步了3个大文件，每个1GB，
3个文件同时加载到内存，超过4G堆内存，导致OOM。
```

---

### Step 4：结合监控确认

**看什么**：
```bash
# 如果有Prometheus/Grafana监控，查看JVM内存曲线
# 或者查看应用自带的actuator端点
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**关键信息**：
```
时间        堆内存使用    堆内存总量
09:23:40    1.2GB        4GB
09:23:41    2.3GB        4GB
09:23:42    3.4GB        4GB
09:23:43    4.0GB        4GB  ← 满了
09:23:44    4.0GB        4GB  ← Full GC
09:23:45    OOM
```

**面试话术**：
```
结合监控，看到堆内存从1.2GB快速涨到4GB，
每分钟涨1GB，和文件同步的时间点完全吻合，
确认是文件同步导致的OOM。
```

---

## 四、完整排查流程图

```
发现OOM
    ↓
查看应用日志
    ↓ 搜索OutOfMemoryError
确认OOM类型和发生位置
    ↓
查看GC日志
    ↓ 搜索Full GC
确认连续Full GC，内存回收不掉
    ↓
查看业务日志上下文
    ↓ 看OOM前后做了什么
发现连续同步大文件
    ↓
结合监控确认
    ↓ 看内存曲线
确认文件同步导致内存飙升
    ↓
定位问题代码
    byte[] fileBytes = ftpClient.downloadFile(path)
```

---

## 五、面试话术（完整版）

```
【日志排查过程】

第一步，查看应用日志，搜索OutOfMemoryError，
发现是Java heap space，发生在FileSyncService的syncFile方法，
具体是在FTPClient.downloadFile时，分配byte数组失败。

第二步，查看GC日志，发现OOM前连续多次Full GC，
堆内存从4G降到4G，一点都回收不掉，
每次Full GC耗时2秒多，最后OOM。

第三步，查看OOM前后的业务日志，
发现OOM前连续同步了3个大文件，每个1GB，
3个文件同时加载到内存，超过4G堆内存。

第四步，结合监控，看到堆内存从1.2GB快速涨到4GB，
每分钟涨1GB，和文件同步的时间点完全吻合。

最终定位到问题：byte[] fileBytes = ftpClient.downloadFile(path)
大文件直接加载到内存，导致OOM。
```

**时长**：2-3分钟

---

## 六、常用命令速查

```bash
# 1. 查看OOM错误
grep -n "OutOfMemoryError" /logs/app.log

# 2. 查看Full GC
grep -n "Full GC" /logs/gc.log

# 3. 查看特定时间段的日志
sed -n '/2024-03-15 09:20:00/,/2024-03-15 09:25:00/p' /logs/app.log

# 4. 查看日志最后100行
tail -100 /logs/app.log

# 5. 实时查看日志
tail -f /logs/app.log

# 6. 统计错误次数
grep -c "OutOfMemoryError" /logs/app.log

# 7. 查看异常堆栈
grep -A 20 "OutOfMemoryError" /logs/app.log
```

---

## 七、日志排查 vs MAT分析

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 线上紧急恢复 | 日志排查 | 快速定位，不用传大文件 |
| 精确定位对象 | MAT分析 | 能看到具体对象和引用链 |
| 没有dump文件 | 日志排查 | 只能依赖日志 |
| 复杂内存泄漏 | MAT分析 | 需要看对象关系 |

**最佳实践**：
```
线上OOM → 先看日志快速定位 → 如果有dump再用MAT精确定位
```

---

## 八、记忆口诀

```
日志排查四步走：
应用日志看异常，
GC日志看回收，
业务日志看操作，
监控曲线看趋势。

OOM类型要分清，
Full GC连续行，
内存涨得快，
问题定位快。
```

---

## 九、实战练习

### 练习1：口述日志排查过程

要求：
- 按四步走流程
- 提到具体数字（3个文件，每个1GB，4G内存）
- 2-3分钟讲完

### 练习2：模拟面试

面试官：你没有dump文件，怎么通过日志排查OOM？

你：（按上面的话术回答）

---

**掌握标准**：能清晰描述日志排查的4个步骤，每个步骤看什么日志，得出什么结论。
