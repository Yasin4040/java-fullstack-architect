# OOM排查实战经验知识卡片

> 🎯 目标：讲一个真实的OOM排查故事，展现排查思路和解决问题的能力

---

## 一、面试回答框架（STAR法则）

```
S - Situation（背景）：什么系统，什么场景
T - Task（任务）：OOM现象，你的职责
A - Action（行动）：排查步骤，用了什么工具
R - Result（结果）：怎么解决的，有什么效果
```

---

## 二、标准话术模板（可直接使用）

### 版本1：堆OOM（最常见）

```
【背景】
我们有一个Spring Boot的订单系统，每天处理几十万笔订单，
高峰期QPS达到500左右，部署在4核8G的服务器上。

【问题】
某天早上9点高峰期，系统突然频繁Full GC，最后抛出OOM：
java.lang.OutOfMemoryError: Java heap space
导致订单接口大量超时，用户无法下单。

【排查过程】

第一步：确认OOM类型
查看日志，确认是堆内存溢出，不是元空间或栈溢出。

第二步：保留现场
JVM参数配置了-XX:+HeapDumpOnOutOfMemoryError，
自动生成dump文件，我立即下载到本地分析。

第三步：分析dump文件
用MAT（Memory Analyzer Tool）打开dump文件：
1. 查看Histogram，发现byte[]数组占用了70%内存
2. 查看Dominator Tree，找到最大的对象是一个ArrayList
3. 查看Path to GC Roots，发现是订单导出功能导致的

第四步：定位代码问题
查看订单导出代码：
- 一次性查询所有订单（可能几十万条）
- 全部加载到内存，转成Excel
- 没有分页，没有流式处理

第五步：验证猜想
查看监控，确认OOM都发生在用户点击"导出订单"时。

【解决方案】
1. 紧急方案：暂时关闭导出功能，保证核心下单流程
2. 根本方案：
   - 分页查询，每次1000条
   - 使用EasyExcel流式写入，不占用大量内存
   - 限制单次导出数量（最多1万条）
   - 大导出任务异步处理，走消息队列

【效果】
- 当天14点重新上线，系统稳定
- 内存占用从6G降到2G
- Full GC从每小时10次降到每天1-2次

【总结】
这次OOM让我认识到：
1. 大数据量操作一定要分页和流式处理
2. 生产环境必须配置HeapDump参数
3. 监控告警要及时，不能等用户反馈
```

---

### 版本2：元空间OOM（适合展示深度）

```
【背景】
我们有一个基于Spring Cloud的微服务系统，使用JDK8，
经常动态加载一些业务规则类（Groovy脚本）。

【问题】
服务运行3-5天后，会出现OOM：
java.lang.OutOfMemoryError: Metaspace
重启后恢复，但过几天又出现。

【排查过程】

第一步：确认OOM类型
日志显示Metaspace溢出，不是堆内存问题。

第二步：监控元空间使用
用jstat查看元空间：
jstat -gcutil pid 1000
发现元空间使用率持续增长，Full GC后也不下降。

第三步：分析类加载情况
用jcmd查看类加载：
jcmd pid VM.classloader_stats
发现加载的类数量持续增长，从1万涨到5万。

第四步：定位问题
检查代码发现：
- 用Groovy动态编译规则脚本
- 每次编译都生成新的ClassLoader
- 旧的ClassLoader和类没有被回收
- 导致元空间不断膨胀

第五步：验证
写了一个测试程序，模拟频繁编译Groovy脚本，
复现了元空间OOM问题。

【解决方案】
1. 缓存ClassLoader，不要每次都创建新的
2. 使用Groovy的Script对象池，复用编译好的类
3. 限制规则脚本数量，定期清理不用的规则
4. 升级JDK8到JDK11，使用G1收集器，元空间管理更好

【效果】
- 元空间稳定在200M左右
- 服务可以稳定运行几个月不重启
- 规则加载速度提升50%

【总结】
元空间OOM通常和类加载有关，要关注：
1. 动态代理、反射、脚本语言（Groovy、JRuby）
2. ClassLoader是否正确释放
3. JDK版本（JDK8元空间容易OOM，JDK11改善很多）
```

---

## 三、OOM类型速查表

| OOM类型 | 错误信息 | 常见原因 | 排查重点 |
|---------|---------|---------|---------|
| **堆OOM** | Java heap space | 对象太多、内存泄漏、大对象 | dump文件分析 |
| **元空间OOM** | Metaspace | 类加载过多、ClassLoader泄漏 | 类加载统计 |
| **栈OOM** | StackOverflowError | 无限递归、方法调用链太长 | 线程栈分析 |
| **直接内存OOM** | Direct buffer memory | NIO使用不当、未释放 | 堆外内存监控 |
| **GC overhead** | GC overhead limit exceeded | GC效率太低、内存不足 | GC日志分析 |
| **无法创建线程** | Unable to create new native thread | 线程数超过系统限制 | 线程数监控 |

---

## 四、排查工具箱

### 1. 命令行工具（线上排查）

```bash
# 查看JVM内存概况
jmap -heap pid

# 生成dump文件
jmap -dump:format=b,file=heap.hprof pid

# 查看GC情况
jstat -gcutil pid 1000

# 查看类加载
jcmd pid VM.classloader_stats

# 查看线程栈
jstack pid > thread.txt
```

### 2. 可视化工具（线下分析）

| 工具 | 用途 | 特点 |
|------|------|------|
| **MAT** | 分析dump文件 | 功能强大，免费 |
| **VisualVM** | 监控JVM | JDK自带，简单易用 |
| **JProfiler** | 性能分析 | 商业软件，功能全面 |
| **Arthas** | 线上诊断 | 阿里巴巴开源，不用重启 |

### 3. Arthas实战（线上不重启排查）

```bash
# 安装Arthas
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar

# 查看内存使用
memory

# 查看堆中对象
heapdump /tmp/dump.hprof

# 查看大对象
vmtool --action getInstances --className java.lang.String --limit 10

# 查看方法执行时间
trace com.example.OrderService exportOrders '#cost>1000'
```

---

## 五、JVM参数配置（生产环境必配）

```bash
# 堆内存配置
-Xms4g -Xmx4g           # 初始和最大堆内存，建议相同避免动态调整
-XX:NewRatio=2          # 老年代:新生代 = 2:1
-XX:SurvivorRatio=8     # Eden:S0:S1 = 8:1:1

# OOM时自动生成dump文件（必配！）
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof

# GC日志（JDK9+用Xlog）
-Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=100m

# 元空间（JDK8）
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=256m

# 栈空间
-Xss1m
```

---

## 六、高频面试题

### Q1：OOM怎么排查？

```
排查步骤：
1. 看日志确认OOM类型（堆/元空间/栈）
2. 保留现场（dump文件、GC日志）
3. 分析dump文件（MAT找大对象）
4. 定位代码（找到内存泄漏点）
5. 修复验证（本地复现，修复后压测）

工具：MAT分析dump，jstat看GC，Arthas线上诊断
```

### Q2：怎么预防OOM？

```
预防手段：
1. 代码层面：
   - 大数据量分页查询
   - 流式处理，不一次性加载
   - 及时关闭资源（IO、连接）
   - 慎用缓存，设置过期时间

2. JVM参数：
   - 合理设置堆内存大小
   - 配置OOM自动dump
   - 开启GC日志

3. 监控告警：
   - 内存使用率超过80%告警
   - Full GC次数异常告警
   - 接口响应时间告警
```

### Q3：内存泄漏和内存溢出的区别？

```
内存溢出（OOM）：内存不够用了，无法分配新对象
- 可能是正常的，比如数据量太大
- 也可能是泄漏导致的

内存泄漏：对象不再使用，但GC无法回收
- 比如静态集合持有对象引用
- 比如未关闭的连接
- 泄漏最终会导致OOM

关系：内存泄漏 → 可用内存减少 → 内存溢出
```

### Q4：MAT分析dump文件的步骤？

```
1. 打开dump文件
2. 查看Histogram，找占用内存最多的类
3. 查看Dominator Tree，找最大的对象
4. 查看Path to GC Roots，找谁持有引用
5. 结合代码，定位泄漏点
```

### Q5：线上服务OOM了，不能重启怎么排查？

```
用Arthas：
1. attach到进程
2. memory命令查看内存使用
3. heapdump命令生成dump（不重启）
4. dashboard命令实时监控
5. trace命令分析方法耗时

如果必须重启：
- 先配置-XX:+HeapDumpOnOutOfMemoryError
- 重启后等下次OOM自动生成dump
```

---

## 七、记忆口诀

```
OOM排查三板斧：
一看日志定类型，
二抓dump分析清，
三改代码验证行。

生产环境必配置：
HeapDumpOnOOM要开启，
GC日志不能停，
监控告警要及时。

大数据量要注意：
分页查询是基本，
流式处理省内存，
缓存设置过期期。
```

---

## 八、实战练习

### 练习1：口述你的OOM故事

要求：
- 用STAR法则
- 包含具体数字（QPS、内存大小、时间）
- 3-5分钟讲完

### 练习2：模拟面试

面试官：你们系统遇到过OOM吗？怎么排查的？

你：（用上面的话术回答）

---

**掌握标准**：能讲一个真实的OOM排查故事，包含背景、现象、排查步骤、解决方案、效果总结。

**加分项**：提到Arthas、MAT、JVM参数配置、监控告警等实际工具。
