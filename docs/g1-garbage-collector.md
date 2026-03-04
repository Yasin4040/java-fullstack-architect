# G1收集器知识卡片

> G1（Garbage First）是JDK9+默认收集器，低延迟+高吞吐的平衡选择

---

## 一、为什么叫Garbage First？

```
G1 = Garbage First（垃圾优先）

核心思想：
- 不再区分新生代和老年代（逻辑上还有，物理上没有）
- 将堆划分为多个Region（区域）
- 每次GC优先回收垃圾最多的Region
- 最大化回收效率，控制停顿时间
```

**对比传统收集器**：
```
CMS/G1之前：
- 新生代和老年代是连续的内存区域
- Minor GC只回收新生代
- Full GC回收整个堆

G1：
- 堆划分为多个大小相等的Region（1-32MB，2的幂）
- Region可以动态扮演Eden、Survivor、Old角色
- 每次GC选择垃圾最多的Region回收
- Mixed GC可以同时回收新生代+部分老年代
```

---

## 二、G1的核心特点

### 特点1：Region分区（物理不分代）

```
┌─────────────────────────────────────────────────────────┐
│                    G1 堆内存布局                         │
├─────────────────────────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐     │
│  │ E   │ │ S   │ │ E   │ │ O   │ │ E   │ │ O   │     │
│  │Eden │ │Surv │ │Eden │ │ Old │ │Eden │ │ Old │     │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘     │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐     │
│  │ O   │ │ H   │ │ S   │ │ O   │ │ E   │ │ O   │     │
│  │ Old │ │Humong│ │Surv │ │ Old │ │Eden │ │ Old │     │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘     │
│                                                         │
│  E = Eden Region（新生代）                              │
│  S = Survivor Region（幸存区）                          │
│  O = Old Region（老年代）                               │
│  H = Humongous Region（大对象，>0.5个Region）           │
└─────────────────────────────────────────────────────────┘
```

**关键点**：
- Region大小：1MB、2MB、4MB、8MB、16MB、32MB（默认根据堆大小自动计算）
- Region角色动态变化：今天做Eden，下次GC后可能变成Old
- Humongous Region：存放大对象（>Region/2），直接进入老年代

---

### 特点2：可预测的停顿时间

```
核心参数：
-XX:MaxGCPauseMillis=200  # 目标最大停顿时间，默认200ms

实现原理：
1. G1会记录每个Region的回收耗时
2. 根据历史数据预测回收时间
3. 每次GC只选择足够多的Region回收，确保不超过目标时间
4. 不是每次都能精确控制，但长期统计会趋近目标值
```

**对比CMS**：
```
CMS：尽量缩短停顿时间，但无法控制（可能几十ms，也可能几秒）
G1：可以设置目标停顿时间，尽量保证（不是绝对保证）
```

---

### 特点3：Mixed GC（混合收集）

```
传统收集器：
- Minor GC：只回收新生代
- Major GC：只回收老年代
- Full GC：回收整个堆

G1的GC类型：
1. Young GC（年轻代GC）
   - 只回收Eden和Survivor Region
   - 并行复制算法

2. Mixed GC（混合GC）⭐
   - 回收新生代 + 部分老年代Region
   - 选择垃圾最多的老年代Region回收
   - 控制停顿时间，不是全量回收

3. Full GC（全量GC）
   - 单线程Serial Old收集器
   - 尽量避免
```

**Mixed GC触发条件**：
```
1. 老年代占用达到阈值（默认45%）
2. 距离上次Mixed GC超过一定时间
3. G1会根据停顿时间目标，选择回收哪些老年代Region
```

---

### 特点4：Remembered Set（记忆集）

```
问题：
Region之间可能有引用关系，回收一个Region时，
怎么知道其他Region是否有对象引用它？

解决方案：Remembered Set（RSet）
- 每个Region都有一个RSet
- RSet记录哪些其他Region的对象引用了本Region的对象
- 回收时只需要扫描RSet，不需要扫描整个堆

实现：
- 使用Card Table（卡表）实现
- 写屏障（Write Barrier）维护RSet
```

---

### 特点5：Concurrent Marking（并发标记）

```
G1的标记过程（类似CMS，但更优）：

1. Initial Mark（初始标记）
   - STW，标记GC Roots直接关联的对象
   - 耗时短，随Young GC一起执行

2. Root Region Scan（根区域扫描）
   - 并发，扫描Survivor区引用老年代的对象

3. Concurrent Mark（并发标记）
   - 并发，遍历整个堆标记存活对象

4. Remark（重新标记）
   - STW，处理并发期间变化的对象
   - 使用SATB（Snapshot-At-The-Beginning）算法

5. Cleanup（清理）
   - 并发，统计Region垃圾占比，排序
   - STW，更新RSet，重置空Region
```

---

## 三、G1 vs CMS 对比

| 对比项 | G1 | CMS |
|--------|-----|-----|
| **内存布局** | Region分区 | 连续的新生代/老年代 |
| **停顿时间** | 可预测（设置目标） | 不可预测 |
| **老年代回收** | Mixed GC（部分回收） | Full GC或Concurrent Mark |
| **碎片问题** | 少（复制算法整理） | 多（标记清除算法） |
| **CPU占用** | 稍高（维护RSet） | 较低 |
| **内存占用** | 稍高（RSet开销） | 较低 |
| **JDK版本** | JDK9+默认 | JDK9废弃，JDK14移除 |

---

## 四、G1适用场景

```
✅ 推荐用G1：
- 堆内存较大（>4G）
- 需要控制停顿时间（<500ms）
- 不想频繁Full GC
- JDK9+新项目

❌ 不推荐用G1：
- 堆内存很小（<2G），Serial/Parallel就够了
- 需要极致吞吐量，不在乎停顿（用Parallel）
- 需要极致低延迟（<10ms，用ZGC/Shenandoah）
```

---

## 五、G1常用参数

```bash
# 启用G1（JDK9+默认已启用）
-XX:+UseG1GC

# 目标最大停顿时间（默认200ms）
-XX:MaxGCPauseMillis=200

# Region大小（默认自动计算，2的幂）
-XX:G1HeapRegionSize=4m

# 老年代占用达到多少触发Mixed GC（默认45%）
-XX:InitiatingHeapOccupancyPercent=45

# Mixed GC时老年代Region回收比例（默认10%）
-XX:G1MixedGCCountTarget=10

# 触发Full GC的堆占用率（默认45%）
-XX:G1ReservePercent=10
```

---

## 六、面试话术（完整版）

### 简短版（1分钟）

```
G1（Garbage First）是JDK9+默认垃圾收集器。

为什么叫Garbage First？
- 它将堆划分为多个Region
- 每次GC优先回收垃圾最多的Region
- 最大化回收效率

核心特点：
1. Region分区，物理上不分代
2. 可预测停顿时间（设置MaxGCPauseMillis）
3. Mixed GC同时回收新生代+部分老年代
4. 使用Remembered Set避免全堆扫描

适用场景：大堆内存（>4G），需要控制停顿时间的应用。
```

### 详细版（3-5分钟）

```
【为什么叫Garbage First】

G1将堆划分为多个大小相等的Region（1-32MB），
每个Region可以动态扮演Eden、Survivor、Old角色。

每次GC时，G1会优先选择垃圾最多的Region进行回收，
也就是"Garbage First"——垃圾优先，最大化回收效率。

【核心特点】

1. Region分区
   - 物理上不再区分连续的新生代和老年代
   - Region角色动态变化
   - 大对象（>0.5Region）直接进入Humongous Region

2. 可预测的停顿时间
   - 通过-XX:MaxGCPauseMillis设置目标停顿时间（默认200ms）
   - G1会记录每个Region的回收耗时
   - 每次GC选择足够多的Region，确保不超过目标时间

3. Mixed GC
   - 不仅回收新生代，还回收部分老年代Region
   - 选择垃圾最多的老年代Region，控制停顿时间
   - 避免Full GC

4. Remembered Set
   - 每个Region维护RSet，记录外部引用
   - 回收时只需扫描RSet，不需要全堆扫描
   - 提高回收效率

【与CMS对比】

G1相比CMS的优势：
- 可预测停顿时间（CMS无法控制）
- 减少内存碎片（G1用复制算法整理）
- Mixed GC避免Full GC

劣势：
- CPU和内存开销稍高（维护RSet）

【适用场景】

- 堆内存>4G
- 需要控制停顿时间（<500ms）
- JDK9+新项目

如果追求极致低延迟（<10ms），可以用ZGC。
```

---

## 七、高频面试题

### Q1：G1为什么叫Garbage First？

```
G1将堆划分为多个Region，
每次GC优先选择垃圾最多的Region回收，
也就是"垃圾优先"，最大化回收效率。
```

### Q2：G1和CMS的区别？

```
1. 内存布局：G1是Region分区，CMS是连续的新生代/老年代
2. 停顿时间：G1可预测，CMS不可预测
3. 碎片问题：G1少（复制算法），CMS多（标记清除）
4. 回收方式：G1是Mixed GC，CMS是Concurrent Mark
5. 开销：G1 CPU和内存开销稍高
```

### Q3：G1的Mixed GC是什么？

```
Mixed GC是G1特有的GC类型：
- 不仅回收新生代（Eden+Survivor）
- 还回收部分老年代Region
- 选择垃圾最多的老年代Region
- 控制停顿时间，避免Full GC

触发条件：老年代占用达到阈值（默认45%）
```

### Q4：G1的Remembered Set是什么？

```
RSet是G1用来记录Region间引用的数据结构：
- 每个Region都有一个RSet
- 记录哪些其他Region的对象引用了本Region
- 回收时只需扫描RSet，不需要全堆扫描
- 提高回收效率，但增加了内存开销
```

### Q5：G1的停顿时间一定能达到目标吗？

```
不一定，但长期统计会趋近目标值。

G1会根据历史数据预测回收时间，
选择足够多的Region确保不超过目标时间。

但如果垃圾产生速度大于回收速度，
还是会触发Full GC，停顿时间就无法保证了。
```

---

## 八、记忆口诀

```
G1收集器叫垃圾优先，
Region分区灵活变。
停顿时间可预测，
Mixed GC防Full。

RSet记录跨区引，
并发标记效率高。
大堆低延用G1，
JDK9后默认选。
```

---

**掌握标准**：能讲清G1的命名原因、核心特点（Region、停顿时间、Mixed GC）、与CMS的区别。
