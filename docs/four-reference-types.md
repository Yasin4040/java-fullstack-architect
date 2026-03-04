# 四种引用类型知识卡片

> 🎯 目标：3分钟内讲清强/软/弱/虚引用，区分使用场景

---

## 一、为什么需要四种引用？

```
问题：
Object obj = new Object();  // 这种是强引用
即使内存不够了，GC也不会回收，可能导致OOM

解决方案：
Java提供4种引用级别，让开发者控制对象生命周期
→ 强引用 > 软引用 > 弱引用 > 虚引用
```

---

## 二、四种引用对比表（核心）

| 引用类型 | 回收时机 | 用途 | 实现类 | 是否可能OOM |
|---------|---------|------|--------|-----------|
| **强引用** | 永不回收（除非=null） | 普通对象 | 默认 | ✅ 可能 |
| **软引用** | 内存不足时回收 | 缓存 | SoftReference | ❌ 不会 |
| **弱引用** | 下次GC时回收 | 临时对象、防止内存泄漏 | WeakReference | ❌ 不会 |
| **虚引用** | 随时可能回收（无法获取对象） | 跟踪对象被回收 | PhantomReference | ❌ 不会 |

---

## 三、详细解析 + 代码示例

### 1. 强引用（Strong Reference）

```java
// 最常见的引用
Object obj = new Object();

// 特点：
// 1. 默认就是强引用
// 2. 即使OOM也不回收
// 3. 需要手动置为null才能回收

obj = null;  // 这样才能被GC回收
```

**使用场景**：普通对象，占绝大多数

**面试要点**：
```
强引用是默认引用类型，只要对象有强引用指向，
即使内存不足，JVM宁愿抛出OOM也不会回收。
需要手动将引用置为null，才能被GC回收。
```

---

### 2. 软引用（Soft Reference）

```java
// 创建软引用
SoftReference<byte[]> softRef = new SoftReference<>(new byte[1024 * 1024 * 10]); // 10MB

// 获取对象
byte[] data = softRef.get();
if (data != null) {
    // 使用数据
} else {
    // 已被回收，需要重新加载
    data = loadFromDatabase();
    softRef = new SoftReference<>(data);
}
```

**使用场景**：内存敏感的缓存
- 图片缓存
- 页面缓存
- 大数据临时缓存

**面试要点**：
```
软引用在内存充足时不会被回收，内存不足时会被回收。
非常适合做缓存：有内存就用，没内存就释放，不会导致OOM。
```

**经典应用**：Android图片缓存
```java
// Android中大量使用软引用做图片缓存
private Map<String, SoftReference<Bitmap>> imageCache = new HashMap<>();

public Bitmap getImage(String url) {
    SoftReference<Bitmap> ref = imageCache.get(url);
    if (ref != null && ref.get() != null) {
        return ref.get();  // 缓存命中
    }
    // 缓存失效或被回收，重新加载
    Bitmap bitmap = downloadImage(url);
    imageCache.put(url, new SoftReference<>(bitmap));
    return bitmap;
}
```

---

### 3. 弱引用（Weak Reference）

```java
// 创建弱引用
WeakReference<Object> weakRef = new WeakReference<>(new Object());

// 获取对象（可能为null）
Object obj = weakRef.get();

System.gc();  // 建议GC（实际不一定立即执行）

// 下次获取可能为null
obj = weakRef.get();  // 可能返回null
```

**使用场景**：
- 防止内存泄漏（特别是ThreadLocal）
- 临时对象跟踪
- WeakHashMap的key

**面试要点**：
```
弱引用比软引用更弱，下次GC时无论内存是否充足都会被回收。
主要用来防止内存泄漏，比如ThreadLocal就用弱引用避免内存泄漏。
```

**经典应用1：ThreadLocal防止内存泄漏**
```java
// ThreadLocal的实现
static class ThreadLocalMap {
    // Entry继承WeakReference，key是弱引用
    static class Entry extends WeakReference<ThreadLocal<?>> {
        Object value;
        
        Entry(ThreadLocal<?> k, Object v) {
            super(k);  // key是弱引用
            value = v; // value是强引用
        }
    }
}

// 原理：
// ThreadLocal对象没有强引用时（方法结束），key会被GC回收
// 但value还是强引用，需要手动remove()清理
```

**经典应用2：WeakHashMap**
```java
// key是弱引用，当key没有其他强引用时，会被自动回收
WeakHashMap<Object, String> map = new WeakHashMap<>();
Object key = new Object();
map.put(key, "value");

key = null;  // 去掉强引用
System.gc(); 

// 一段时间后，map中的entry会被自动清除
System.out.println(map.size());  // 可能为0
```

---

### 4. 虚引用（Phantom Reference）

```java
// 虚引用必须配合ReferenceQueue使用
ReferenceQueue<Object> queue = new ReferenceQueue<>();
PhantomReference<Object> phantomRef = new PhantomReference<>(new Object(), queue);

// 虚引用的get()永远返回null
Object obj = phantomRef.get();  // 永远返回null

// 用途：跟踪对象被回收的时机
// 当对象被回收时，虚引用会被放入ReferenceQueue
Reference<?> ref = queue.poll();
if (ref != null) {
    // 对象已被回收，可以做一些清理工作
    // 比如释放堆外内存
}
```

**使用场景**：
- 跟踪对象被回收的时机
- 释放堆外内存（DirectByteBuffer）
- 资源清理的回调机制

**面试要点**：
```
虚引用最弱，随时可能被回收，且get()永远返回null。
必须配合ReferenceQueue使用，用于跟踪对象被回收的时机，
主要用于释放堆外内存等资源清理工作。
```

**经典应用：DirectByteBuffer释放堆外内存**
```java
// DirectByteBuffer使用虚引用释放堆外内存
class DirectByteBuffer {
    private Cleaner cleaner;  // 内部使用PhantomReference
    
    DirectByteBuffer(int cap) {
        // 分配堆外内存
        long base = unsafe.allocateMemory(cap);
        
        // 创建Cleaner（内部是PhantomReference）
        cleaner = Cleaner.create(this, new Deallocator(base, cap));
    }
    
    // 当DirectByteBuffer被GC回收时
    // Cleaner的虚引用被触发，调用Deallocator释放堆外内存
}
```

---

## 四、四种引用关系图

```
引用强度（从高到低）：

强引用 ──────────────────────────────> 永不回收（除非=null）
   │
   ▼
软引用 ──────内存不足时───────────────> 回收（适合做缓存）
   │
   ▼
弱引用 ──────下次GC时───────────────> 回收（防止内存泄漏）
   │
   ▼
虚引用 ──────随时可能回收─────────────> 回收（跟踪回收时机）
```

---

## 五、面试话术（背诵版）

### 简短版（1分钟）

```
Java有四种引用类型：

1. 强引用：默认引用，永不回收，可能导致OOM
2. 软引用：内存不足时回收，适合做缓存
3. 弱引用：下次GC时回收，防止内存泄漏（如ThreadLocal）
4. 虚引用：随时可能回收，用于跟踪对象回收时机（如释放堆外内存）

引用强度：强 > 软 > 弱 > 虚
```

### 详细版（3分钟）

```
Java提供四种引用类型，让开发者更灵活地控制对象生命周期：

【强引用】
默认的引用类型，如Object obj = new Object()。
只要强引用存在，对象永远不会被回收，即使OOM也不回收。
需要手动置为null才能释放。

【软引用】SoftReference
内存充足时不回收，内存不足时回收。
适合做缓存，比如图片缓存、页面缓存。
有内存就用，没内存就释放，不会导致OOM。

【弱引用】WeakReference
比软引用更弱，下次GC时无论内存是否充足都会被回收。
主要用来防止内存泄漏，比如ThreadLocal就用弱引用作为key，
避免线程池场景下内存泄漏。

【虚引用】PhantomReference
最弱的引用，随时可能被回收，且get()永远返回null。
必须配合ReferenceQueue使用，用于跟踪对象被回收的时机。
主要用于释放堆外内存，比如DirectByteBuffer的Cleaner机制。

总结：引用强度强>软>弱>虚，根据场景选择合适的引用类型。
```

---

## 六、高频面试题

### Q1：四种引用的区别？使用场景？

```
区别：
- 强引用：永不回收
- 软引用：内存不足回收
- 弱引用：下次GC回收
- 虚引用：随时回收，用于跟踪

场景：
- 强引用：普通对象
- 软引用：缓存
- 弱引用：防止内存泄漏
- 虚引用：资源清理
```

### Q2：软引用和弱引用的区别？

```
核心区别：回收时机不同

软引用：内存不足时才回收，适合做缓存
弱引用：下次GC就回收，不管内存是否充足

软引用存活时间更长，弱引用更容易被回收。
```

### Q3：ThreadLocal为什么用弱引用？还会有内存泄漏吗？

```
ThreadLocal的key用弱引用，是为了防止key的内存泄漏。
当ThreadLocal对象没有强引用时，key会被GC回收。

但value还是强引用，如果线程一直存活（如线程池），
value就不会被回收，导致内存泄漏。

解决方案：使用完ThreadLocal后，手动调用remove()方法。
```

### Q4：虚引用有什么作用？get()为什么返回null？

```
作用：跟踪对象被回收的时机，用于资源清理（如堆外内存）。

get()返回null的原因：
虚引用设计目的就是跟踪回收，不是为了获取对象。
如果get()能获取对象，就会形成强引用，违背设计初衷。
```

### Q5：WeakHashMap的原理？

```
WeakHashMap的key是弱引用。
当key没有其他强引用时，下次GC会被回收，
对应的entry也会从map中自动移除。

适合用作临时缓存，key不再使用时自动清理。
```

---

## 七、记忆口诀

```
强引用，默认的，永不回收要小心。
软引用，做缓存，内存不足才释放。
弱引用，防泄漏，下次GC就回收。
虚引用，跟踪用，get永远返回空。

强软弱虚强度降，合理选择不OOM。
ThreadLocal用弱引，手动remove防泄漏。
DirectBuffer虚引用，堆外内存自动清。
```

---

## 八、易错点提醒

| 错误说法 | 正确说法 |
|---------|---------|
| 软引用GC时就回收 | 软引用是**内存不足时**才回收 |
| 弱引用和软引用一样 | 弱引用**下次GC就回收**，不管内存 |
| 虚引用可以获取对象 | 虚引用**get()永远返回null** |
| 用了弱引用就不会内存泄漏 | ThreadLocal的**value还是强引用**，要remove() |
| 四种引用都在堆中 | 引用对象在堆，**引用本身也在堆** |

---

## 九、实战代码：缓存实现

```java
/**
 * 基于软引用的图片缓存
 * 内存不足时自动释放，不会OOM
 */
public class ImageCache {
    private Map<String, SoftReference<Bitmap>> cache = new HashMap<>();
    
    public void put(String url, Bitmap bitmap) {
        cache.put(url, new SoftReference<>(bitmap));
    }
    
    public Bitmap get(String url) {
        SoftReference<Bitmap> ref = cache.get(url);
        if (ref != null) {
            Bitmap bitmap = ref.get();
            if (bitmap != null) {
                return bitmap;  // 缓存命中
            }
            // 被回收了，清理key
            cache.remove(url);
        }
        return null;  // 缓存未命中
    }
}
```

---

**掌握标准**：能区分四种引用，讲清使用场景，能结合ThreadLocal、缓存等实际案例。

**关联知识**：结合JVM内存结构、GC机制、内存泄漏排查一起回答，展现深度。
