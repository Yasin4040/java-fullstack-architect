# AQS核心原理知识卡片

> AQS（AbstractQueuedSynchronizer）是JUC包的基石，ReentrantLock、CountDownLatch、Semaphore都基于它实现

---

## 一、AQS是什么？

```
AQS = AbstractQueuedSynchronizer（抽象队列同步器）

地位：
- Java并发包（JUC）的核心基础组件
- 位于java.util.concurrent.locks包下
- 作者是Doug Lea（并发编程大师）

作用：
- 提供了一个框架，用于实现依赖先进先出（FIFO）等待队列的阻塞锁和同步器
- 子类只需要实现少量方法，就能实现自定义同步器
```

**基于AQS实现的类**：
```
锁：
- ReentrantLock（可重入锁）
- ReentrantReadWriteLock（读写锁）

同步器：
- CountDownLatch（倒计时门闩）
- Semaphore（信号量）
- CyclicBarrier（循环栅栏）
```

---

## 二、AQS核心结构

AQS主要由三部分组成：

```
┌─────────────────────────────────────────────────────────────┐
│                     AQS 核心结构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌───────────────────────────────┐  │
│  │   state（状态）  │    │      CLH 等待队列              │  │
│  │  （volatile int）│    │  （FIFO双向队列）               │  │
│  │                 │    │                               │  │
│  │  0 = 未锁定      │    │  ┌─────┐   ┌─────┐   ┌─────┐ │  │
│  │  1 = 已锁定      │    │  │Node │ ←→│Node │ ←→│Node │ │  │
│  │  >1 = 重入次数   │    │  │Thread│   │Thread│   │Thread│ │  │
│  │                 │    │  │prev │   │prev │   │prev │ │  │
│  │  通过CAS修改     │    │  │next │   │next │   │next │ │  │
│  └─────────────────┘    │  └─────┘   └─────┘   └─────┘ │  │
│                         └───────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │              独占/共享模式支持                          │ │
│  │  - 独占模式：ReentrantLock                              │ │
│  │  - 共享模式：Semaphore、CountDownLatch                  │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、核心组件详解

### 1. state（同步状态）

```java
/**
 * 同步状态，volatile保证可见性
 * 子类通过getState()、setState()、compareAndSetState()操作
 */
private volatile int state;
```

**不同同步器的state含义**：

| 同步器 | state含义 | 示例 |
|--------|----------|------|
| **ReentrantLock** | 锁的重入次数 | 0=未锁定，1=锁定，2=重入1次 |
| **Semaphore** | 剩余可用许可数 | 10=还能允许10个线程 |
| **CountDownLatch** | 剩余需要countDown的次数 | 3=还需调用3次countDown |

**修改state**：
```java
// 必须通过CAS操作，保证原子性
protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

---

### 2. CLH队列（等待队列）

```
CLH = Craig, Landin, and Hagersten（三个发明人的名字）

特点：
- FIFO双向链表
- 每个等待线程封装成一个Node节点
- 头节点是获取到锁的线程，其他节点在等待
```

**Node节点结构**：
```java
static final class Node {
    // 共享模式
    static final Node SHARED = new Node();
    // 独占模式
    static final Node EXCLUSIVE = null;
    
    // 等待状态
    volatile int waitStatus;
    static final int CANCELLED =  1;  // 取消
    static final int SIGNAL    = -1;  // 后继节点需要唤醒
    static final int CONDITION = -2;  // 在Condition队列中
    static final int PROPAGATE = -3;  // 共享模式传播
    
    // 双向链表指针
    volatile Node prev;  // 前驱节点
    volatile Node next;  // 后继节点
    
    // 等待的线程
    volatile Thread thread;
    
    // Condition队列的下个节点
    Node nextWaiter;
}
```

**队列结构**：
```
┌─────────────────────────────────────────────────────────────┐
│                      CLH 队列示意图                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   head                        tail                          │
│    │                           │                            │
│    ▼                           ▼                            │
│  ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐                  │
│  │Node │←──→│Node │←──→│Node │←──→│Node │                  │
│  │持有锁│    │等待 │    │等待 │    │等待 │                  │
│  │T1   │    │T2   │    │T3   │    │T4   │                  │
│  │next │───→│next │───→│next │───→│next │                  │
│  │prev │←───│prev │←───│prev │←───│prev │                  │
│  └─────┘    └─────┘    └─────┘    └─────┘                  │
│                                                             │
│  T1获取到锁，正在执行                                        │
│  T2、T3、T4在队列中等待                                      │
│  每个Node对应一个等待线程                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 3. 独占 vs 共享模式

```
AQS支持两种同步模式：

┌─────────────────────────────────────────────────────────────┐
│                      独占模式（Exclusive）                    │
├─────────────────────────────────────────────────────────────┤
│  特点：同一时间只有一个线程能获取同步状态                       │
│  实现：ReentrantLock                                        │
│                                                             │
│  获取：acquire(int arg)                                     │
│  释放：release(int arg)                                     │
│                                                             │
│  场景：锁，互斥访问临界资源                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      共享模式（Shared）                       │
├─────────────────────────────────────────────────────────────┤
│  特点：同一时间多个线程能获取同步状态                           │
│  实现：Semaphore、CountDownLatch                             │
│                                                             │
│  获取：acquireShared(int arg)                               │
│  释放：releaseShared(int arg)                               │
│                                                             │
│  场景：资源池，多个线程可同时访问                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、核心方法流程

### 1. acquire（获取锁）流程

```java
// ReentrantLock.lock() 最终调用
public final void acquire(int arg) {
    // 1. 尝试获取锁（子类实现tryAcquire）
    if (!tryAcquire(arg) &&
        // 2. 获取失败，加入等待队列
        acquireQueued(
            // 3. 将当前线程包装成Node，加入队列
            addWaiter(Node.EXCLUSIVE), 
            arg
        )
    ) {
        // 4. 被中断过，恢复中断状态
        selfInterrupt();
    }
}
```

**流程图**：
```
┌─────────────────────────────────────────────────────────────┐
│                    acquire 流程图                            │
└─────────────────────────────────────────────────────────────┘

调用acquire()
    │
    ▼
┌─────────────────┐
│ tryAcquire(arg) │ ← 子类实现，尝试获取锁
│  尝试获取锁      │
└─────────────────┘
    │
   成功 ────────────┐
    │               │
    ▼               ▼
  获取成功    ┌─────────────────┐
  继续执行    │ addWaiter()     │
              │ 包装成Node节点   │
              │ 加入CLH队列      │
              └─────────────────┘
                    │
                    ▼
              ┌─────────────────┐
              │ acquireQueued() │
              │ 在队列中等待     │
              │ 自旋/CAS/阻塞    │
              └─────────────────┘
                    │
                   被唤醒
                    │
                    ▼
              ┌─────────────────┐
              │ tryAcquire()    │
              │ 再次尝试获取     │
              └─────────────────┘
                    │
                   成功
                    │
                    ▼
              获取成功，出队
              继续执行
```

---

### 2. release（释放锁）流程

```java
// ReentrantLock.unlock() 最终调用
public final boolean release(int arg) {
    // 1. 尝试释放锁（子类实现tryRelease）
    if (tryRelease(arg)) {
        Node h = head;
        // 2. 释放成功，唤醒后继节点
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

**流程图**：
```
┌─────────────────────────────────────────────────────────────┐
│                    release 流程图                            │
└─────────────────────────────────────────────────────────────┘

调用release()
    │
    ▼
┌─────────────────┐
│ tryRelease(arg) │ ← 子类实现，尝试释放锁
│  尝试释放锁      │
└─────────────────┘
    │
   失败 ────────────┐
    │               │
    ▼               ▼
  释放失败    ┌─────────────────┐
  返回false   │ unparkSuccessor │
              │ 唤醒后继节点     │
              │ LockSupport     │
              │ .unpark(thread) │
              └─────────────────┘
                    │
                    ▼
              后继线程被唤醒
              从park()返回
              继续竞争锁
```

---

## 五、AQS设计精髓

### 1. 模板方法模式

```
AQS使用模板方法模式，定义了算法骨架：

┌─────────────────────────────────────────────────────────────┐
│                    模板方法模式                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  AQS（抽象类）                                               │
│  ├── acquire()【模板方法】                                   │
│  │   ├── tryAcquire()【子类实现】◄── ReentrantLock实现      │
│  │   ├── addWaiter()【AQS实现】                              │
│  │   └── acquireQueued()【AQS实现】                          │
│  │                                                           │
│  └── release()【模板方法】                                   │
│      ├── tryRelease()【子类实现】◄── ReentrantLock实现      │
│      └── unparkSuccessor()【AQS实现】                        │
│                                                             │
│  子类只需要实现tryAcquire/tryRelease，其他AQS都搞定了        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. 子类需要实现的方法

| 方法 | 说明 | 使用场景 |
|------|------|---------|
| `tryAcquire(int arg)` | 独占获取 | ReentrantLock |
| `tryRelease(int arg)` | 独占释放 | ReentrantLock |
| `tryAcquireShared(int arg)` | 共享获取 | Semaphore |
| `tryReleaseShared(int arg)` | 共享释放 | CountDownLatch |
| `isHeldExclusively()` | 是否独占 | Condition使用 |

---

## 六、面试话术（完整版）

### 简短版（1分钟）

```
AQS是AbstractQueuedSynchronizer，JUC包的基石。

核心组成：
1. state：volatile int，同步状态
2. CLH队列：FIFO双向链表，存放等待线程
3. 独占/共享模式

原理：
- 获取锁：CAS修改state，失败则加入CLH队列等待
- 释放锁：修改state，唤醒队列中下一个线程

基于AQS：ReentrantLock、CountDownLatch、Semaphore
```

### 详细版（3-5分钟）

```
【AQS是什么】

AQS是AbstractQueuedSynchronizer，抽象队列同步器，
是Java并发包JUC的核心基础组件，作者是Doug Lea。

【核心结构】

AQS有三个核心部分：

1. state（同步状态）
   - volatile int类型，保证可见性
   - 不同同步器含义不同：
     * ReentrantLock：重入次数
     * Semaphore：剩余许可数
     * CountDownLatch：剩余计数
   - 通过CAS操作修改

2. CLH队列（等待队列）
   - FIFO双向链表
   - 每个等待线程封装成Node节点
   - 包含waitStatus、prev、next、thread等字段

3. 独占/共享模式
   - 独占：同一时间只有一个线程能获取（ReentrantLock）
   - 共享：同一时间多个线程能获取（Semaphore）

【核心原理】

获取锁（acquire）：
1. 调用tryAcquire尝试获取（子类实现）
2. 失败则调用addWaiter，将线程包装成Node加入CLH队列
3. 调用acquireQueued，在队列中自旋/阻塞等待
4. 被唤醒后再次尝试获取，成功则出队执行

释放锁（release）：
1. 调用tryRelease尝试释放（子类实现）
2. 释放成功，调用unparkSuccessor唤醒后继节点
3. 后继线程被唤醒，继续竞争锁

【设计精髓】

AQS使用模板方法模式：
- 定义了acquire/release的算法骨架
- 子类只需要实现tryAcquire/tryRelease
- 排队、唤醒、中断处理等复杂逻辑AQS都实现了

【基于AQS的类】

锁：ReentrantLock、ReentrantReadWriteLock
同步器：CountDownLatch、Semaphore、CyclicBarrier
```

---

## 七、高频面试题

### Q1：AQS是什么？

```
AQS是AbstractQueuedSynchronizer，抽象队列同步器，
JUC包的核心基础组件，提供了同步状态的获取/释放、
线程排队、唤醒等基础机制。
```

### Q2：AQS的核心组件有哪些？

```
1. state：volatile int，同步状态
2. CLH队列：FIFO双向链表，存放等待线程
3. 独占/共享模式支持
```

### Q3：AQS为什么用CLH队列？

```
CLH队列的优点：
1. FIFO，保证公平性
2. 双向链表，方便节点删除和唤醒
3. 每个节点自旋检查前驱状态，减少竞争
4. 支持独占和共享两种模式
```

### Q4：ReentrantLock和AQS的关系？

```
ReentrantLock内部有一个Sync类继承AQS：
- Sync实现了tryAcquire/tryRelease
- lock()调用acquire()
- unlock()调用release()
- AQS负责排队、唤醒等通用逻辑
```

### Q5：AQS如何保证线程安全？

```
1. state用volatile修饰，保证可见性
2. 修改state用CAS操作，保证原子性
3. 队列操作用自旋+CAS，保证线程安全
4. 线程阻塞用LockSupport.park/unpark
```

---

## 八、记忆口诀

```
AQS，队列同步器，
state状态记心里。
CLH队列排好队，
独占共享都支持。

获取锁，先CAS，
失败入队别放弃。
前驱唤醒我继续，
直到成功才出去。

释放锁，改state，
唤醒后继接上去。
模板方法真精妙，
子类只需写逻辑。
```

---

**掌握标准**：能说清AQ +