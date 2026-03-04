# 线程池7个参数详解 + 设置公式

> 线程池是Java并发编程的核心，7个参数必须烂熟于心

---

## 一、7个参数速记表

| 序号 | 参数名 | 类型 | 作用 | 记忆口诀 |
|------|--------|------|------|---------|
| 1 | corePoolSize | int | 核心线程数 | 核心工人 |
| 2 | maximumPoolSize | int | 最大线程数 | 最多工人 |
| 3 | keepAliveTime | long | 空闲线程存活时间 | 闲置多久 |
| 4 | unit | TimeUnit | 时间单位 | 时间单位 |
| 5 | workQueue | BlockingQueue | 任务等待队列 | 排队仓库 |
| 6 | threadFactory | ThreadFactory | 创建线程的工厂 | 工人制造 |
| 7 | handler | RejectedExecutionHandler | 拒绝策略 | 满员咋办 |

**记忆口诀**：
```
核心最大存活期，队列工厂拒绝器
```

---

## 二、7个参数详解

### 1. corePoolSize（核心线程数）

```java
/**
 * 核心线程数，即使空闲也保留的线程数
 * 除非设置了allowCoreThreadTimeOut
 */
```

**特点**：
- 线程池初始化后，默认不会创建核心线程（惰性创建）
- 提交任务时，如果当前线程数 < corePoolSize，创建新线程
- 核心线程即使空闲，也不会被回收（除非设置allowCoreThreadTimeOut）

**设置建议**：
```
CPU密集型：core = CPU核数 + 1
IO密集型：core = CPU核数 * 2
混合任务：根据实际压测调整
```

---

### 2. maximumPoolSize（最大线程数）

```java
/**
 * 线程池允许的最大线程数
 * 当队列满时，会创建非核心线程，直到达到maximumPoolSize
 */
```

**特点**：
- 当workQueue满时，继续提交任务会创建新线程（非核心线程）
- 线程数达到maximumPoolSize后，再提交任务触发拒绝策略
- 非核心线程空闲超过keepAliveTime会被回收

**设置建议**：
```
max = core * 2 或 core * 3
根据系统资源和任务特性调整
不要超过系统能承载的线程数（避免OOM）
```

---

### 3. keepAliveTime（空闲线程存活时间）

```java
/**
 * 非核心线程的空闲存活时间
 * 超过这个时间没有任务执行，非核心线程会被回收
 */
```

**特点**：
- 只针对非核心线程（线程数 > corePoolSize时的多余线程）
- 核心线程默认不会超时回收
- 可以设置allowCoreThreadTimeOut(true)让核心线程也超时回收

**设置建议**：
```
默认60秒
短任务：30秒
长任务：5分钟
```

---

### 4. unit（时间单位）

```java
/**
 * keepAliveTime的时间单位
 */
TimeUnit.SECONDS      // 秒
TimeUnit.MILLISECONDS // 毫秒
TimeUnit.MINUTES      // 分钟
```

---

### 5. workQueue（任务等待队列）

```java
/**
 * 用于保存等待执行的任务的阻塞队列
 * 当线程数达到corePoolSize后，新任务进入队列等待
 */
```

**常用队列类型**：

| 队列类型 | 特点 | 使用场景 |
|---------|------|---------|
| **ArrayBlockingQueue** | 有界数组队列 | 防止OOM，推荐 |
| **LinkedBlockingQueue** | 无界链表队列（默认Integer.MAX_VALUE） | 可能导致OOM，慎用 |
| **SynchronousQueue** | 不存储元素，直接提交给线程 | 高吞吐，配合大maxPoolSize |
| **PriorityBlockingQueue** | 优先级队列 | 任务有优先级 |
| **DelayQueue** | 延迟队列 | 定时任务 |

**推荐**：
```
有界队列：new ArrayBlockingQueue<>(1000)
避免无界队列导致OOM
```

---

### 6. threadFactory（线程工厂）

```java
/**
 * 用于创建新线程的工厂
 * 可以自定义线程名称、优先级、守护状态等
 */
```

**默认实现**：
```java
Executors.defaultThreadFactory()
// 创建线程：pool-1-thread-1, pool-1-thread-2...
```

**自定义实现**（推荐）：
```java
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("order-pool-%d")     // 自定义线程名
    .setDaemon(false)                    // 非守护线程
    .setPriority(Thread.NORM_PRIORITY)   // 正常优先级
    .build();

// 或使用Guava的ThreadFactoryBuilder
```

**自定义的好处**：
- 线程名称有意义，方便日志排查
- 可以设置UncaughtExceptionHandler处理异常

---

### 7. handler（拒绝策略）

```java
/**
 * 当线程数和队列都满时，新任务的拒绝策略
 */
```

**JDK提供的4种拒绝策略**：

| 策略 | 行为 | 使用场景 |
|------|------|---------|
| **AbortPolicy**（默认） | 抛出RejectedExecutionException | 快速失败，通知调用者 |
| **CallerRunsPolicy** | 由调用线程（提交任务的线程）自己执行 | 降低提交速度，保护系统 |
| **DiscardPolicy** | 静默丢弃任务 | 不重要任务，可丢弃 |
| **DiscardOldestPolicy** | 丢弃队列最老的任务，尝试提交新任务 | 新任务更重要 |

**自定义拒绝策略**（推荐）：
```java
new RejectedExecutionHandler() {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 1. 记录日志
        log.error("任务被拒绝: {}", r.toString());
        
        // 2. 保存到数据库或消息队列，稍后重试
        saveToDb(r);
        
        // 3. 或抛出自定义异常
        throw new CustomRejectedException("系统繁忙，请稍后重试");
    }
};
```

---

## 三、线程池执行流程图

```
提交任务
    ↓
当前线程数 < corePoolSize ?
    ↓ 是
创建核心线程，执行任务
    ↓ 否
workQueue 未满 ?
    ↓ 是
任务进入队列等待
    ↓ 否
当前线程数 < maximumPoolSize ?
    ↓ 是
创建非核心线程，执行任务
    ↓ 否
执行拒绝策略
```

**流程口诀**：
```
先核心，再队列，后非核心，最后拒绝
```

---

## 四、参数设置公式（面试必背）

### 1. CPU密集型任务

```java
// 计算密集型：加密、压缩、复杂算法
corePoolSize = CPU核数 + 1
maximumPoolSize = corePoolSize
workQueue = new ArrayBlockingQueue<>(100)

// 原因：
// CPU密集型任务占用CPU，线程太多会导致上下文切换
// +1是为了防止某个线程阻塞时，CPU有空闲
```

### 2. IO密集型任务

```java
// IO密集型：网络请求、文件读写、数据库操作
corePoolSize = CPU核数 * 2
maximumPoolSize = corePoolSize * 2
workQueue = new ArrayBlockingQueue<>(1000)
keepAliveTime = 60L

// 原因：
// IO密集型任务等待时间长，多线程可以提高CPU利用率
// 线程在等待IO时，其他线程可以用CPU
```

### 3. 混合型任务

```java
// 既有CPU计算，又有IO操作
corePoolSize = CPU核数
maximumPoolSize = CPU核数 * 3
workQueue = new ArrayBlockingQueue<>(500)

// 最好拆分成两个线程池：
// 一个CPU密集型，一个IO密集型
```

### 4. 实际计算公式

```java
// 更精确的计算公式（来自《Java并发编程实战》）
N = CPU核数
U = 目标CPU利用率（0 <= U <= 1）
W = 任务平均等待时间（IO等待）
C = 任务平均计算时间

// 最优线程数
optimalThreads = N * U * (1 + W/C)

// 示例：
// 4核CPU，目标利用率100%，W=100ms，C=20ms
// optimalThreads = 4 * 1 * (1 + 100/20) = 24
```

---

## 五、完整示例代码

### 正确创建线程池（推荐）

```java
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.*;

public class ThreadPoolConfig {
    
    // 获取CPU核数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    
    /**
     * IO密集型线程池
     */
    public static ThreadPoolExecutor ioIntensivePool() {
        int corePoolSize = CPU_COUNT * 2;
        int maximumPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        
        // 有界队列，防止OOM
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1000);
        
        // 自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("io-pool-%d")
            .setUncaughtExceptionHandler((t, e) -> {
                System.err.println("线程异常: " + t.getName() + ", 异常: " + e.getMessage());
            })
            .build();
        
        // 自定义拒绝策略
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            workQueue,
            threadFactory,
            handler
        );
        
        // 允许核心线程超时回收
        executor.allowCoreThreadTimeOut(true);
        
        // 预启动所有核心线程
        executor.prestartAllCoreThreads();
        
        return executor;
    }
    
    /**
     * CPU密集型线程池
     */
    public static ThreadPoolExecutor cpuIntensivePool() {
        int corePoolSize = CPU_COUNT + 1;
        
        return new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize,  // max = core，不创建非核心线程
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),  // 小队列
            new ThreadFactoryBuilder().setNameFormat("cpu-pool-%d").build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
```

---

## 六、面试话术（完整版）

### 简短版（1分钟）

```
线程池7个参数：

1. corePoolSize：核心线程数
2. maximumPoolSize：最大线程数
3. keepAliveTime：非核心线程空闲存活时间
4. unit：时间单位
5. workQueue：任务等待队列
6. threadFactory：线程工厂
7. handler：拒绝策略

执行流程：先核心，再队列，后非核心，最后拒绝。

设置公式：
- CPU密集型：core = CPU + 1
- IO密集型：core = CPU * 2
```

### 详细版（3-5分钟）

```
【7个参数】

1. corePoolSize：核心线程数，即使空闲也保留
2. maximumPoolSize：最大线程数，队列满时创建非核心线程
3. keepAliveTime：非核心线程空闲存活时间
4. unit：时间单位
5. workQueue：任务等待队列，推荐有界队列ArrayBlockingQueue
6. threadFactory：线程工厂，建议自定义线程名称
7. handler：拒绝策略，默认AbortPolicy抛异常

【执行流程】

提交任务后：
1. 如果当前线程数 < core，创建核心线程执行任务
2. 如果线程数 >= core，任务进入workQueue等待
3. 如果队列满，且线程数 < max，创建非核心线程
4. 如果线程数 >= max，执行拒绝策略

口诀：先核心，再队列，后非核心，最后拒绝。

【设置公式】

CPU密集型（加密、计算）：
- core = CPU核数 + 1
- max = core
- 原因：CPU密集型占用CPU，线程太多导致上下文切换

IO密集型（网络、文件、数据库）：
- core = CPU核数 * 2
- max = core * 2
- 原因：IO等待时线程阻塞，多线程提高CPU利用率

【注意事项】

1. 队列一定要用有界的，防止OOM
2. 自定义线程工厂，方便排查问题
3. 自定义拒绝策略，记录日志或保存任务
4. 根据实际压测调整参数，不要生搬硬套
```

---

## 七、高频面试题

### Q1：线程池7个参数是什么？

```
corePoolSize、maximumPoolSize、keepAliveTime、unit、
workQueue、threadFactory、handler

口诀：核心最大存活期，队列工厂拒绝器
```

### Q2：线程池的执行流程？

```
1. 当前线程数 < core：创建核心线程
2. 当前线程数 >= core：任务进入队列
3. 队列满，线程数 < max：创建非核心线程
4. 队列满，线程数 >= max：执行拒绝策略

口诀：先核心，再队列，后非核心，最后拒绝
```

### Q3：为什么要用线程池？不用会怎样？

```
好处：
1. 减少线程创建销毁的开销
2. 控制并发线程数
3. 提高响应速度（线程已存在）
4. 便于线程管理（监控、统计）

不用的问题：
1. 频繁创建销毁线程，开销大
2. 线程数不可控，可能导致OOM
3. 无法统一管理
```

### Q4：四种拒绝策略的区别？

```
AbortPolicy：抛异常（默认）
CallerRunsPolicy：调用者线程执行
DiscardPolicy：静默丢弃
DiscardOldestPolicy：丢弃最老任务

推荐：CallerRunsPolicy，可以降低提交速度，保护系统
```

### Q5：为什么不要用Executors创建线程池？

```
Executors的缺陷：
1. newFixedThreadPool：使用无界队列LinkedBlockingQueue，
   任务堆积可能导致OOM
2. newCachedThreadPool：允许创建无限线程，可能导致OOM
3. newSingleThreadExecutor：同样使用无界队列

推荐：直接使用ThreadPoolExecutor构造函数，
指定有界队列，控制最大线程数
```

---

## 八、记忆口诀

```
线程池，七参数，
核心最大存活期。
队列工厂拒绝器，
一个不能少。

执行流程记清楚：
先核心，再队列，
后非核心，最后拒绝。

CPU密集型核加一，
IO密集型核乘二，
有界队列防OOM，
自定义工厂好排查。
```

---

**掌握标准**：能流利说出7个参数，描述执行流程，给出设置公式。
