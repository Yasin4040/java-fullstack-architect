# MAT分析dump文件的具体操作步骤

> 手把手教你用MAT定位OOM问题

---

## 一、获取dump文件

### 方式1：OOM自动生成（推荐）

```bash
# JVM参数配置（生产环境必配）
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof
```

OOM时会自动生成：`/logs/heapdump.hprof`

### 方式2：手动生成

```bash
# 查看Java进程ID
jps

# 生成dump文件
jmap -dump:format=b,file=/tmp/heap.hprof <pid>

# 或
jcmd <pid> GC.heap_dump /tmp/heap.hprof
```

---

## 二、MAT工具安装

### 下载

```
官网：https://eclipse.dev/mat/
下载：Memory Analyzer (MAT)
```

### 安装

```
1. 解压下载的zip文件
2. 双击MemoryAnalyzer.exe启动
3. 修改内存配置（分析大dump需要大内存）
   修改MemoryAnalyzer.ini：
   -Xmx8g  # 根据dump大小调整，建议dump大小的1.5倍
```

---

## 三、分析步骤（核心）

### Step 1：打开dump文件

```
File → Open Heap Dump → 选择.hprof文件
```

等待解析完成（大文件可能需要几分钟）

---

### Step 2：查看Histogram（直方图）

**操作**：
```
点击 "Histogram" 图标
或
Window → Histogram
```

**看什么**：
```
Class Name                    | Objects | Shallow Heap | Retained Heap
------------------------------|---------|--------------|--------------
byte[]                        | 1,234   | 2,345,678    | 3,456,789,012  ← 关注这个
java.lang.String              | 56,789  | 1,234,567    | 123,456,789
java.util.HashMap$Node        | 45,678  | 987,654      | 98,765,432
...
```

**分析**：
- 按 `Retained Heap` 降序排列
- 找到占用内存最多的类
- **你的案例**：`byte[]` 应该排在第一，占80%+

**面试话术**：
```
打开Histogram，按Retained Heap排序，
发现byte[]数组占用了80%的内存，约3.5GB，
这就是OOM的罪魁祸首。
```

---

### Step 3：查看Dominator Tree（支配树）

**操作**：
```
点击 "Dominator Tree" 图标
或
Window → Dominator Tree
```

**看什么**：
```
Class Name                    | Retained Heap | Percentage
------------------------------|---------------|------------
byte[1048576000] @ 0x12345678 | 1,048,576,000 | 25.00%
byte[1048576000] @ 0x23456789 | 1,048,576,000 | 25.00%
byte[524288000] @ 0x34567890  | 524,288,000   | 12.50%
java.util.ArrayList @ 0x...   | 2,100,000,000 | 50.00%      ← 找到持有者
...
```

**分析**：
- 找到最大的byte[]对象
- 看是被谁持有的（Path to GC Roots）
- **你的案例**：应该看到ArrayList持有多个byte[]

**面试话术**：
```
查看Dominator Tree，找到最大的几个byte[]，
每个约1GB，发现它们都被一个ArrayList持有，
这个ArrayList占用了50%的堆内存。
```

---

### Step 4：Path to GC Roots（查找引用链）

**操作**：
```
1. 在Dominator Tree中右键点击可疑对象
2. 选择 "Path to GC Roots" → "exclude weak/soft references"
3. 或选择 "Merge Shortest Paths to GC Roots"
```

**看什么**：
```
ArrayList @ 0x12345678 (50%)
  └── FileSyncService @ 0x23456789
        └── syncFile() method
              └── ftpPath = "/data/large_file.zip"
```

**分析**：
- 找到谁持有这个对象（GC Roots）
- 定位到具体类和方法
- **你的案例**：应该定位到`FileSyncService.syncFile()`

**面试话术**：
```
右键byte[]，选择Path to GC Roots，
排除弱引用后，发现是被FileSyncService的syncFile方法持有，
具体是ftpClient.downloadFile()返回的byte[]。
```

---

### Step 5：查看Leak Suspects（泄漏怀疑报告）

**操作**：
```
点击 "Leak Suspects" 图标
或
Window → Leak Suspects
```

**看什么**：
```
Leak Suspects Report
====================

Problem Suspect 1
-----------------
One instance of "java.util.ArrayList" loaded by "..." occupies 2,147,483,648 (50%) bytes.
The memory is accumulated in one instance of "byte[]" loaded by "...".

Keywords
--------
java.util.ArrayList
byte[]
com.example.service.FileSyncService
```

**分析**：
- MAT自动分析可能的内存泄漏点
- 直接给出怀疑对象和关键词
- **你的案例**：应该直接指出`FileSyncService`和`byte[]`

**面试话术**：
```
查看Leak Suspects报告，MAT自动分析出
FileSyncService中的ArrayList占用了50%内存，
里面存储了大量的byte[]，确认是文件同步功能导致的OOM。
```

---

## 四、完整分析流程图

```
打开dump文件
    ↓
Histogram（直方图）
    ↓ 按Retained Heap排序
发现byte[]占80%内存
    ↓
Dominator Tree（支配树）
    ↓ 找最大的对象
发现ArrayList持有多个byte[]
    ↓
Path to GC Roots（引用链）
    ↓ 定位持有者
定位到FileSyncService.syncFile()
    ↓
Leak Suspects（泄漏报告）
    ↓ 确认问题
确认是downloadFile()导致的OOM
```

---

## 五、面试话术（完整版）

```
【MAT分析过程】

第一步，打开Histogram直方图，按Retained Heap降序排列，
发现byte[]数组占用了3.5GB，约80%的堆内存。

第二步，查看Dominator Tree支配树，找到最大的几个byte[]，
每个约1GB，发现它们都被一个ArrayList持有。

第三步，右键byte[]，选择Path to GC Roots，排除弱引用后，
发现是被FileSyncService的syncFile方法持有。

第四步，查看Leak Suspects报告，MAT自动分析确认
FileSyncService中的byte[]是内存泄漏点。

最终定位到代码：byte[] fileBytes = ftpClient.downloadFile(path)
大文件直接加载到内存，导致OOM。
```

**时长**：1-2分钟

---

## 六、常见问题

### Q：dump文件太大打不开？

```
解决方案：
1. 修改MemoryAnalyzer.ini，增大-Xmx
2. 使用命令行分析：
   ./ParseHeapDump.sh heap.hprof org.eclipse.mat.api:suspects
3. 只分析部分数据（不推荐）
```

### Q：Histogram和Dominator Tree的区别？

```
Histogram：按类统计，看哪种类型对象最多
Dominator Tree：按对象统计，看哪个对象最大

先Histogram找类型，再Dominator Tree找具体对象
```

### Q：Shallow Heap和Retained Heap的区别？

```
Shallow Heap：对象本身占用的内存
Retained Heap：对象本身 + 引用的其他对象的总内存

看Retained Heap更准确，表示释放该对象能回收多少内存
```

---

## 七、记忆口诀

```
MAT分析四步走：
Histogram看类型，
Dominator找大对象，
GC Roots定位持有者，
Leak Suspects确认泄漏点。

Shallow是自身，
Retained是全部，
排序看Retained，
泄漏无处藏。
```

---

## 八、实战练习

### 练习1：口述MAT分析过程

要求：
- 按四步走流程
- 提到具体数字（byte[]占80%，3.5GB）
- 1-2分钟讲完

### 练习2：模拟面试

面试官：你怎么用MAT分析dump文件？

你：（按上面的话术回答）

---

**掌握标准**：能清晰描述MAT分析的4个步骤，每个步骤看什么，得出什么结论。
