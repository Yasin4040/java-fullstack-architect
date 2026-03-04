# CompletableFuture异步编排详解

> CompletableFuture是Java 8引入的异步编程工具，支持函数式编程和复杂的异步任务编排

---

## 一、CompletableFuture是什么？

```
CompletableFuture = 可完成的Future

特点：
1. 实现了Future接口，支持异步计算
2. 支持函数式编程（lambda表达式）
3. 支持任务编排（串行、并行、组合）
4. 支持回调处理（成功、异常、完成）
5. 支持显式完成（手动设置结果）

对比Future：
- Future：只能阻塞获取结果，不能编排
- CompletableFuture：支持链式调用、组合、回调
```

---

## 二、创建CompletableFuture

### 1. 使用runAsync（无返回值）

```java
// 使用默认线程池（ForkJoinPool.commonPool）
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    // 异步执行任务，无返回值
    System.out.println("异步任务执行：" + Thread.currentThread().getName());
});

// 使用自定义线程池（推荐）
ExecutorService executor = Executors.newFixedThreadPool(4);
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    // 异步任务
}, executor);
```

### 2. 使用supplyAsync（有返回值）

```java
// 使用默认线程池
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // 异步执行任务，有返回值
    return "Hello CompletableFuture";
});

// 使用自定义线程池（推荐）
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "异步任务结果";
}, executor);
```

### 3. 手动创建并设置结果

```java
// 创建未完成的CompletableFuture
CompletableFuture<String> future = new CompletableFuture<>();

// 在其他线程中手动完成
new Thread(() -> {
    try {
        Thread.sleep(1000);
        future.complete("任务完成");  // 正常完成
        // 或 future.completeExceptionally(new Exception("出错了"));
    } catch (InterruptedException e) {
        future.completeExceptionally(e);
    }
}).start();

// 获取结果（会阻塞直到完成）
String result = future.get();
```

---

## 三、异步编排核心方法

### 串行执行（then系列）

```java
// thenRun：上一步执行完后执行，不依赖上一步结果，无返回值
CompletableFuture.supplyAsync(() -> "Hello")
    .thenRun(() -> System.out.println("上一步完成了"));

// thenAccept：上一步执行完后执行，消费上一步结果，无返回值
CompletableFuture.supplyAsync(() -> "Hello")
    .thenAccept(result -> System.out.println("结果：" + result));

// thenApply：上一步执行完后执行，使用上一步结果，有返回值
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(result -> result + " World")
    .thenApply(result -> result.toUpperCase())
    .thenAccept(System.out::println);  // 输出：HELLO WORLD
```

**对比**：

| 方法 | 输入 | 输出 | 用途 |
|------|------|------|------|
| `thenRun` | 无 | 无 | 只需要上一步完成，不需要结果 |
| `thenAccept` | 上一步结果 | 无 | 消费结果，无返回值 |
| `thenApply` | 上一步结果 | 有返回值 | 转换结果 |

---

### 并行执行（Async系列）

```java
// thenApplyAsync：异步执行（使用线程池）
CompletableFuture.supplyAsync(() -> {
    System.out.println("第一步：" + Thread.currentThread().getName());
    return "Hello";
}).thenApplyAsync(result -> {
    System.out.println("第二步：" + Thread.currentThread().getName());
    return result + " World";
}, executor);  // 可以指定线程池

// thenRunAsync / thenAcceptAsync 同理
```

**thenApply vs thenApplyAsync**：
- `thenApply`：同步执行，使用调用线程
- `thenApplyAsync`：异步执行，使用线程池

---

## 四、组合多个CompletableFuture

### 1. thenCompose（扁平化，避免嵌套）

```java
// 场景：先获取用户ID，再根据ID获取用户信息
// 第二个任务依赖第一个任务的结果

// 不好的写法（嵌套）
CompletableFuture<CompletableFuture<User>> nested = 
    getUserIdAsync().thenApply(id -> getUserByIdAsync(id));

// 好的写法（扁平化）
CompletableFuture<User> flat = 
    getUserIdAsync().thenCompose(id -> getUserByIdAsync(id));

// 实际代码
public CompletableFuture<String> getUserIdAsync() {
    return CompletableFuture.supplyAsync(() -> "user-123");
}

public CompletableFuture<User> getUserByIdAsync(String userId) {
    return CompletableFuture.supplyAsync(() -> new User(userId, "张三"));
}

// 使用
getUserIdAsync()
    .thenCompose(this::getUserByIdAsync)
    .thenAccept(user -> System.out.println(user.getName()));
```

### 2. thenCombine（合并两个独立任务的结果）

```java
// 场景：同时查询商品价格和库存，合并展示

CompletableFuture<Integer> priceFuture = CompletableFuture.supplyAsync(() -> {
    System.out.println("查询价格...");
    return 100;
});

CompletableFuture<Integer> stockFuture = CompletableFuture.supplyAsync(() -> {
    System.out.println("查询库存...");
    return 50;
});

// 合并两个结果
CompletableFuture<String> result = priceFuture.thenCombine(stockFuture, 
    (price, stock) -> "价格：" + price + "，库存：" + stock);

System.out.println(result.get());  // 价格：100，库存：50
```

### 3. allOf（等待所有任务完成）

```java
// 场景：并行调用多个服务，等全部返回后再处理

CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(100);
    return "服务A结果";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(200);
    return "服务B结果";
});

CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    sleep(150);
    return "服务C结果";
});

// 等待所有任务完成
CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);

// 所有任务完成后，获取结果
allFutures.thenRun(() -> {
    try {
        System.out.println(future1.get());
        System.out.println(future2.get());
        System.out.println(future3.get());
    } catch (Exception e) {
        e.printStackTrace();
    }
}).join();
```

### 4. anyOf（只要有一个完成）

```java
// 场景：同时调用多个相同服务，只要有一个返回就用

CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(300);
    return "服务A（慢）";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(100);
    return "服务B（快）";
});

CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    sleep(200);
    return "服务C（中）";
});

// 只要有一个完成
CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(future1, future2, future3);

// 获取最快返回的结果
String result = (String) anyFuture.get();
System.out.println("最快返回：" + result);  // 最快返回：服务B（快）
```

---

## 五、异常处理

### 1. exceptionally（捕获异常，返回默认值）

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (true) {
        throw new RuntimeException("出错了！");
    }
    return "正常结果";
}).exceptionally(ex -> {
    // 捕获异常，返回默认值
    System.out.println("异常：" + ex.getMessage());
    return "默认值";
});

System.out.println(future.get());  // 输出：默认值
```

### 2. handle（统一处理正常结果和异常）

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // 可能抛出异常
    return "正常结果";
}).handle((result, ex) -> {
    if (ex != null) {
        System.out.println("发生异常：" + ex.getMessage());
        return "异常时的默认值";
    }
    return result + "（处理成功）";
});
```

### 3. whenComplete（不修改结果，只处理）

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "结果";
}).whenComplete((result, ex) -> {
    // 记录日志，不修改结果
    if (ex != null) {
        log.error("任务执行失败", ex);
    } else {
        log.info("任务执行成功，结果：{}", result);
    }
});
```

---

## 六、实战案例：订单详情页异步编排

```java
/**
 * 订单详情页需要组装：订单信息 + 商品信息 + 用户信息 + 物流信息
 * 传统串行：4个服务调用 = 400ms
 * CompletableFuture并行：max(100, 80, 120, 60) = 120ms
 */
public class OrderDetailService {
    
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public OrderDetail getOrderDetail(String orderId) {
        long start = System.currentTimeMillis();
        
        // 1. 并行查询订单、商品、用户、物流
        CompletableFuture<OrderInfo> orderFuture = CompletableFuture
            .supplyAsync(() -> getOrderInfo(orderId), executor);
        
        CompletableFuture<ProductInfo> productFuture = orderFuture
            .thenCompose(order -> CompletableFuture.supplyAsync(
                () -> getProductInfo(order.getProductId()), executor));
        
        CompletableFuture<UserInfo> userFuture = orderFuture
            .thenCompose(order -> CompletableFuture.supplyAsync(
                () -> getUserInfo(order.getUserId()), executor));
        
        CompletableFuture<LogisticsInfo> logisticsFuture = orderFuture
            .thenCompose(order -> CompletableFuture.supplyAsync(
                () -> getLogisticsInfo(order.getLogisticsId()), executor));
        
        // 2. 等待所有查询完成
        CompletableFuture<OrderDetail> resultFuture = CompletableFuture
            .allOf(productFuture, userFuture, logisticsFuture)
            .thenApply(v -> {
                try {
                    return new OrderDetail(
                        orderFuture.get(),
                        productFuture.get(),
                        userFuture.get(),
                        logisticsFuture.get()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .exceptionally(ex -> {
                log.error("查询订单详情失败", ex);
                return null;
            });
        
        OrderDetail result = resultFuture.join();
        
        long end = System.currentTimeMillis();
        System.out.println("耗时：" + (end - start) + "ms");
        
        return result;
    }
    
    // 模拟服务调用
    private OrderInfo getOrderInfo(String orderId) {
        sleep(100);
        return new OrderInfo(orderId, "prod-1", "user-1", "log-1");
    }
    
    private ProductInfo getProductInfo(String productId) {
        sleep(80);
        return new ProductInfo(productId, "iPhone 15", 5999);
    }
    
    private UserInfo getUserInfo(String userId) {
        sleep(120);
        return new UserInfo(userId, "张三", "13800138000");
    }
    
    private LogisticsInfo getLogisticsInfo(String logisticsId) {
        sleep(60);
        return new LogisticsInfo(logisticsId, "已发货", "顺丰");
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 七、方法速查表

### 创建方法

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `runAsync(Runnable)` | 异步执行，无返回值 | CompletableFuture<Void> |
| `supplyAsync(Supplier)` | 异步执行，有返回值 | CompletableFuture<T> |
| `completedFuture(T)` | 创建已完成的Future | CompletableFuture<T> |
| `new CompletableFuture<>()` | 手动创建 | CompletableFuture<T> |

### 串行方法

| 方法 | 说明 | 输入 | 输出 |
|------|------|------|------|
| `thenRun(Runnable)` | 上步完成后执行 | 无 | 无 |
| `thenAccept(Consumer)` | 消费上步结果 | 上步结果 | 无 |
| `thenApply(Function)` | 转换上步结果 | 上步结果 | 有返回值 |
| `thenCompose(Function)` | 扁平化嵌套Future | 上步结果 | CompletableFuture |

### 组合方法

| 方法 | 说明 |
|------|------|
| `thenCombine(CF, BiFunction)` | 合并两个Future结果 |
| `allOf(CF...)` | 等待所有Future完成 |
| `anyOf(CF...)` | 任意一个Future完成 |

### 异常处理

| 方法 | 说明 |
|------|------|
| `exceptionally(Function)` | 捕获异常，返回默认值 |
| `handle(BiFunction)` | 统一处理结果和异常 |
| `whenComplete(BiConsumer)` | 不修改结果，只处理 |

---

## 八、面试话术（完整版）

### 简短版（1分钟）

```
CompletableFuture是Java 8的异步编程工具。

核心特点：
1. 支持函数式编程（lambda）
2. 支持任务编排（串行、并行、组合）
3. 支持回调和异常处理

常用方法：
- supplyAsync/runAsync：创建异步任务
- thenApply/thenAccept/thenRun：串行处理
- thenCompose：扁平化嵌套
- thenCombine：合并结果
- allOf/anyOf：等待多个任务
- exceptionally/handle：异常处理

实战：订单详情页并行查询多个服务，提升性能。
```

### 详细版（3-5分钟）

```
【CompletableFuture是什么】

CompletableFuture是Java 8引入的异步编程工具，
实现了Future接口，支持函数式编程和复杂的异步任务编排。

相比Future的优势：
- Future只能阻塞获取结果
- CompletableFuture支持链式调用、组合、回调

【创建方式】

1. supplyAsync：有返回值的异步任务
2. runAsync：无返回值的异步任务
3. 手动创建：new CompletableFuture<>()

推荐自定义线程池，避免使用默认的ForkJoinPool。

【异步编排】

串行处理：
- thenApply：转换结果
- thenAccept：消费结果
- thenRun：不需要结果，只需上步完成

并行处理：
- thenApplyAsync：异步执行

组合多个Future：
- thenCompose：扁平化嵌套（避免CompletableFuture<CompletableFuture<T>>）
- thenCombine：合并两个Future的结果
- allOf：等待所有任务完成
- anyOf：任意一个完成即可

【异常处理】

- exceptionally：捕获异常，返回默认值
- handle：统一处理正常结果和异常
- whenComplete：只处理不修改结果

【实战案例】

订单详情页需要查询订单、商品、用户、物流4个服务。
传统串行需要400ms，用CompletableFuture并行只需要120ms。

代码结构：
1. 用supplyAsync并行启动4个查询
2. 用allOf等待全部完成
3. 合并结果返回
4. 用exceptionally处理异常
```

---

## 九、高频面试题

### Q1：CompletableFuture和Future的区别？

```
Future：
- 只能阻塞获取结果（get()）
- 不能编排任务
- 不能处理回调

CompletableFuture：
- 支持链式调用
- 支持任务编排（串行、并行、组合）
- 支持回调和异常处理
- 支持显式完成
```

### Q2：thenApply和thenCompose的区别？

```
thenApply：转换结果
- 输入T，输出R
- 返回CompletableFuture<R>

thenCompose：扁平化
- 输入T，输出CompletableFuture<R>
- 返回CompletableFuture<R>（不是嵌套的）

使用场景：
- thenApply：简单转换结果
- thenCompose：第二个任务依赖第一个任务的结果
```

### Q3：allOf和anyOf的区别？

```
allOf：等待所有任务完成
- 返回CompletableFuture<Void>
- 需要手动获取每个Future的结果

anyOf：任意一个任务完成
- 返回CompletableFuture<Object>
- 返回最快完成的任务结果
```

### Q4：如何处理异常？

```
三种方式：

1. exceptionally：捕获异常，返回默认值
   .exceptionally(ex -> "默认值")

2. handle：统一处理结果和异常
   .handle((result, ex) -> ex != null ? "默认值" : result)

3. whenComplete：只处理不修改
   .whenComplete((result, ex) -> log.info("完成"))
```

### Q5：为什么要自定义线程池？

```
默认使用ForkJoinPool.commonPool()，
线程数等于CPU核数-1。

问题：
1. 如果任务是IO密集型，线程数可能不够
2. 如果commonPool被其他CompletableFuture占满，会阻塞

推荐：根据任务类型自定义线程池
```

---

## 十、记忆口诀

```
CompletableFuture异步王，
supply有值run无妨。

thenApply转换结果，
thenAccept消费忙，
thenRun不关心，
只等上步做完工。

thenCompose扁平化，
嵌套Future不用怕。
thenCombine来合并，
两个结果一起拿。

allOf等全部，
anyOf谁最快，
exceptionally捕异常，
异步编程真痛快！
```

---

**掌握标准**：能创建异步任务、进行串行并行编排、处理异常、说出常用方法的区别。
