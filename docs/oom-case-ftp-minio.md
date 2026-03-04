# OOM实战案例：FTP文件同步到MinIO

> 真实项目经验，可直接用于面试

---

## 一、案例概述（STAR法则）

| 项目 | 内容 |
|------|------|
| **S-背景** | 文件同步服务，定时从FTP拉取文件上传到MinIO |
| **T-问题** | 大文件同步时频繁Full GC，最终OOM |
| **A-排查** | 代码审查+MAT分析，发现byte[]数组问题 |
| **R-解决** | 改为流式复制，内存占用从4G降到200M |

---

## 二、详细面试话术（3-4分钟版）

```
【背景】
我们有一个文件同步服务，定时从FTP服务器拉取文件，上传到MinIO对象存储。
系统用Spring Boot开发，部署在2核4G的容器里，每天同步几千个文件，
大小从几KB到几个GB不等。

【问题】
上线后发现，同步大文件（超过500MB）时，服务频繁Full GC，
最后抛出OOM：java.lang.OutOfMemoryError: Java heap space，
容器被K8s重启，同步任务中断。

【排查过程】

第一步：查看日志和监控
- 日志显示OOM发生在文件上传时
- 监控显示堆内存飙升到3.5G，然后GC不掉，直接OOM
- 配置了-XX:+HeapDumpOnOutOfMemoryError，拿到了dump文件

第二步：分析代码
查看原来的文件同步代码：

    // 问题代码
    public void syncFile(String ftpPath) {
        // 从FTP下载到本地byte数组
        byte[] fileBytes = ftpClient.downloadFile(ftpPath);  // 大文件直接加载到内存！
        
        // 上传到MinIO
        minioClient.putObject(bucket, objectName, fileBytes);  // 又占用一份内存！
    }

问题很明显：
- 一个1GB的文件，byte[]占用1GB
- MinIO上传时可能还要复制一份，总共2GB+
- 4G堆内存，同步2个大文件就OOM了

第三步：MAT分析dump确认
用MAT打开dump文件：
- Histogram显示byte[]数组占用了80%内存
- Dominator Tree找到最大的byte[]，正好是文件大小
- 确认是文件下载时创建的byte[]

【解决方案】

改成流式复制，不加载完整文件到内存：

    // 优化后的代码
    public void syncFile(String ftpPath) {
        // 使用流式下载
        InputStream ftpInputStream = ftpClient.retrieveFileStream(ftpPath);
        
        // 流式上传到MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(ftpInputStream, size, -1)
                .build()
        );
        
        // 及时关闭流
        ftpInputStream.close();
    }

关键点：
1. FTP用retrieveFileStream()获取输入流，不是downloadFile()
2. MinIO用stream()方法直接上传输入流
3. 8KB缓冲区逐块读写，不占用大量内存
4. 用完及时关闭流，防止连接泄漏

【效果】
- 内存占用从4G峰值降到200M稳定
- 可以同步10GB大文件也不OOM
- Full GC从每小时几十次降到每天几次
- 同步速度还提升了30%（少了内存拷贝）

【总结】
这次经历让我深刻认识到：
1. 大文件处理必须用流，不能一次性加载到内存
2. byte[]虽然方便，但大文件就是内存杀手
3. 第三方客户端要仔细看API，选择流式方法
4. 生产环境必须配置OOM自动dump，方便排查
```

---

## 三、代码对比

### ❌ 问题代码（OOM版本）

```java
@Service
public class FileSyncService {
    
    @Autowired
    private FTPClient ftpClient;
    
    @Autowired
    private MinioClient minioClient;
    
    public void syncFile(String ftpPath, String bucket, String objectName) {
        try {
            // 问题1：大文件直接加载到内存！
            byte[] fileBytes = ftpClient.downloadFile(ftpPath);
            
            // 问题2：MinIO上传又要复制一份！
            minioClient.putObject(
                bucket, 
                objectName, 
                new ByteArrayInputStream(fileBytes),
                fileBytes.length,
                null
            );
            
            // 问题3：byte[]一直占用内存，直到GC
            
        } catch (Exception e) {
            log.error("同步失败", e);
        }
    }
}
```

**问题分析**：
- 1GB文件 → byte[]占1GB
- MinIO内部可能再复制 → 共2GB+
- 同步多个文件 → 内存累积 → OOM

---

### ✅ 优化代码（流式版本）

```java
@Service
public class FileSyncService {
    
    @Autowired
    private FTPClient ftpClient;
    
    @Autowired
    private MinioClient minioClient;
    
    public void syncFile(String ftpPath, String bucket, String objectName) {
        InputStream ftpInputStream = null;
        try {
            // 1. 获取FTP文件流（不下载到本地）
            ftpInputStream = ftpClient.retrieveFileStream(ftpPath);
            
            // 2. 获取文件大小
            long fileSize = ftpClient.listFiles(ftpPath)[0].getSize();
            
            // 3. 流式上传到MinIO（8KB缓冲区）
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(ftpInputStream, fileSize, -1)  // -1表示未知partSize
                    .contentType("application/octet-stream")
                    .build()
            );
            
            // 4. 完成FTP传输
            ftpClient.completePendingCommand();
            
            log.info("同步成功: {}", ftpPath);
            
        } catch (Exception e) {
            log.error("同步失败: {}", ftpPath, e);
            throw new RuntimeException("文件同步失败", e);
        } finally {
            // 5. 及时关闭流
            if (ftpInputStream != null) {
                try {
                    ftpInputStream.close();
                } catch (IOException e) {
                    log.warn("关闭流失败", e);
                }
            }
        }
    }
}
```

**优化点**：
- 8KB缓冲区逐块读写
- 内存占用恒定，不随文件大小增长
- 及时关闭流，防止连接泄漏

---

## 四、进阶优化（加分项）

### 1. 断点续传

```java
public void syncFileWithResume(String ftpPath, String bucket, String objectName) {
    // 检查MinIO是否已有部分数据
    long existingSize = getExistingSize(bucket, objectName);
    
    // FTP从断点续传
    ftpClient.setRestartOffset(existingSize);
    InputStream ftpInputStream = ftpClient.retrieveFileStream(ftpPath);
    
    // MinIO追加写入
    minioClient.uploadObject(
        UploadObjectArgs.builder()
            .bucket(bucket)
            .object(objectName)
            .filename(tempFile)  // 先下载到本地临时文件
            .build()
    );
}
```

### 2. 限速控制（防止打满带宽）

```java
// 使用Guava RateLimiter限流
RateLimiter rateLimiter = RateLimiter.create(10 * 1024 * 1024); // 10MB/s

public void syncFileWithLimit(String ftpPath) {
    InputStream throttledStream = new ThrottledInputStream(
        ftpClient.retrieveFileStream(ftpPath),
        rateLimiter
    );
    minioClient.putObject(..., throttledStream, ...);
}
```

### 3. 异步批量处理

```java
@Async("taskExecutor")
public CompletableFuture<Void> syncFileAsync(String ftpPath) {
    return CompletableFuture.runAsync(() -> syncFile(ftpPath));
}

// 批量提交，控制并发数
public void batchSync(List<String> ftpPaths) {
    List<CompletableFuture<Void>> futures = ftpPaths.stream()
        .map(this::syncFileAsync)
        .collect(Collectors.toList());
    
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
}
```

---

## 五、面试要点提炼

### 核心知识点

| 要点 | 说明 |
|------|------|
| **OOM原因** | byte[]加载大文件到内存，MinIO再复制一份 |
| **排查工具** | MAT分析dump，发现byte[]占80%内存 |
| **解决方案** | 流式复制，8KB缓冲区，不占用大量内存 |
| **关键API** | FTP用retrieveFileStream()，MinIO用stream() |
| **效果** | 内存从4G降到200M，支持10GB大文件 |

### 加分项

```
1. 提到K8s容器重启，体现云原生经验
2. 提到-XX:+HeapDumpOnOutOfMemoryError，体现JVM调优
3. 提到流式API的选择，体现对第三方库的深入理解
4. 提到断点续传、限速控制，体现工程化思维
```

---

## 六、常见问题应对

### Q：为什么不直接用FTP的downloadFile方法？

```
downloadFile()内部也是用流，但它把流读取到byte[]返回，
对于大文件就会OOM。retrieveFileStream()直接返回流，
让我们自己控制读取过程，可以边读边写，不占用大量内存。
```

### Q：流式上传失败怎么重试？

```
FTP流是一次性的，失败了需要重新获取。
可以包装一个可重试的InputStream，或者先下载到本地临时文件，
上传成功后再删除临时文件。
```

### Q：怎么监控同步进度？

```
包装InputStream，重写read()方法，记录已读取字节数，
定期输出进度日志。或者用ProgressListener回调。
```

---

## 七、记忆口诀

```
大文件，别用byte[]，
流式处理是正道。
FTP取流不下载，
MinIO上传用stream。

8KB缓冲区，
内存占用不增长。
及时关闭流，
连接泄漏要预防。
```

---

**掌握标准**：能3-4分钟讲完这个案例，包含背景、问题、排查、解决、效果。

**建议**：根据自己的实际项目，修改细节（文件大小、技术栈、具体数字），让故事更真实。
