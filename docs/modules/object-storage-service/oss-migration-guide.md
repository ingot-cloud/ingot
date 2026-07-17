# OSS 对象存储迁移指南

## 概述

本指南帮助你将现有的 MinIO 实现迁移到新的抽象层，或从 MinIO 切换到 RustFS。

## 迁移场景

### 场景 1：现有 MinIO 代码迁移到新抽象层

#### 影响范围

✅ **无需修改** - 新的实现完全向后兼容，现有代码可以继续工作

#### 可选优化

如果你的代码中直接使用了 `MinioService`，可以考虑改为使用 `S3Client` 接口以获得更好的灵活性：

**优化前：**
```java
@Service
@RequiredArgsConstructor
public class BucketService {
    private final MinioService minioService;  // 直接依赖具体实现
    
    public void init(String bucket) {
        minioService.createBucket(bucket);
    }
}
```

**优化后：**
```java
@Service
@RequiredArgsConstructor
public class BucketService {
    private final S3Client s3Client;  // 依赖接口，更灵活
    
    public void init(String bucket) {
        s3Client.createBucket(bucket);
    }
}
```

**优势：**
- 代码可以在 MinIO 和 RustFS 之间无缝切换
- 更好的可测试性（可以 Mock S3Client）
- 符合依赖倒置原则

### 场景 2：从 MinIO 切换到 RustFS

#### 步骤 1：修改依赖

编辑 `build.gradle`：

```gradle
dependencies {
    // 注释或删除 MinIO 依赖
    // implementation project(':ingot-framework:ingot-oss-minio')
    
    // 添加 RustFS 依赖
    implementation project(':ingot-framework:ingot-oss-rustfs')
    
    // 其他依赖保持不变...
}
```

#### 步骤 2：修改配置

编辑 `application.yml` 或 `application.properties`：

**删除 MinIO 配置：**
```yaml
# ingot:
#   oss:
#     minio:
#       url: http://localhost:9000
#       access-key: minioadmin
#       secret-key: minioadmin
#       expired-time: 300
```

**添加 RustFS 配置：**
```yaml
ingot:
  oss:
    rustfs:
      url: http://your-rustfs-server:8080
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1  # 根据你的服务器配置
      expired-time: 300
```

#### 步骤 3：数据迁移（如有必要）

如果需要迁移现有数据：

```java
@Service
@RequiredArgsConstructor
public class OssDataMigrationService {
    private final MinioService sourceService;  // 源服务
    private final RustfsService targetService; // 目标服务
    
    public void migrateBucket(String bucketName) {
        // 1. 创建目标 Bucket
        if (!targetService.bucketExists(bucketName)) {
            targetService.createBucket(bucketName);
        }
        
        // 2. 列举所有对象
        List<MinioItem> items = sourceService.getAllObjectsByPrefix(bucketName, "", true);
        
        // 3. 逐个复制
        for (MinioItem item : items) {
            try (InputStream inputStream = sourceService.getObject(bucketName, item.getObjectName())) {
                targetService.putObject(bucketName, item.getObjectName(), inputStream);
                log.info("Migrated: {}/{}", bucketName, item.getObjectName());
            } catch (Exception e) {
                log.error("Failed to migrate: {}/{}", bucketName, item.getObjectName(), e);
            }
        }
    }
}
```

#### 步骤 4：验证

1. **重新编译项目**
   ```bash
   ./gradlew clean build
   ```

2. **启动应用**
   ```bash
   ./gradlew :your-service:bootRun
   ```

3. **验证功能**
   - 测试文件上传
   - 测试文件下载
   - 测试 URL 生成

### 场景 3：同时支持多个存储（高级）

如果需要在同一个应用中同时使用 MinIO 和 RustFS：

#### 步骤 1：添加两个依赖

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-oss-minio')
    implementation project(':ingot-framework:ingot-oss-rustfs')
}
```

#### 步骤 2：配置两个存储

```yaml
ingot:
  oss:
    minio:
      url: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      expired-time: 300
    rustfs:
      url: http://localhost:8080
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1
      expired-time: 300
```

#### 步骤 3：显式指定使用哪个实现

```java
@Service
public class MultiOssService {
    // MinIO 客户端
    @Autowired
    @Qualifier("minioService")
    private S3Client minioClient;
    
    // RustFS 客户端
    @Autowired
    @Qualifier("rustfsService")
    private S3Client rustfsClient;
    
    public void uploadToMinIO(String bucket, String fileName, InputStream stream) throws Exception {
        minioClient.putObject(bucket, fileName, stream);
    }
    
    public void uploadToRustFS(String bucket, String fileName, InputStream stream) throws Exception {
        rustfsClient.putObject(bucket, fileName, stream);
    }
}
```

**注意：** 默认情况下，Spring 只会注入一个 `OssService` 实现。如果需要同时使用多个，需要使用 `@Qualifier` 注解。

## 配置对照表

| 配置项 | MinIO | RustFS | 说明 |
|--------|-------|--------|------|
| url | ✅ | ✅ | 服务地址 |
| access-key | ✅ | ✅ | 访问密钥 |
| secret-key | ✅ | ✅ | 秘密密钥 |
| expired-time | ✅ | ✅ | URL 过期时间（秒） |
| region | ❌ | ✅ | AWS 区域（RustFS 必需） |
| endpoint.enable | ✅ | ✅ | 是否启用端点 |
| endpoint.name | ✅ | ✅ | 端点名称 |

## API 对照表

### 业务层 API（OssService）

所有方法在 MinIO 和 RustFS 中完全相同：

| 方法 | MinIO | RustFS | 说明 |
|------|-------|--------|------|
| uploadFile() | ✅ | ✅ | 上传文件 |
| getFile() | ✅ | ✅ | 下载文件 |
| getObjectURL(url) | ✅ | ✅ | 获取访问 URL（默认过期时间） |
| getObjectURL(url, expire) | ✅ | ✅ | 获取访问 URL（自定义过期时间） |

### 底层 API（S3Client）

所有方法在 MinIO 和 RustFS 中完全相同：

| 方法 | MinIO | RustFS | 说明 |
|------|-------|--------|------|
| createBucket() | ✅ | ✅ | 创建 Bucket |
| bucketExists() | ✅ | ✅ | 检查 Bucket 是否存在 |
| removeBucket() | ✅ | ✅ | 删除 Bucket |
| putObject() | ✅ | ✅ | 上传对象 |
| getObject() | ✅ | ✅ | 获取对象 |
| getPresignedObjectUrl() | ✅ | ✅ | 获取预签名 URL |
| removeObject() | ✅ | ✅ | 删除对象 |

## 兼容性说明

### 完全兼容

以下功能在新版本中完全兼容：

- ✅ 所有 `OssService` 接口方法
- ✅ `@OssUrl` 注解
- ✅ `@OssSaveUrl` 注解
- ✅ `OSSResult` 返回类型
- ✅ `OssObjectInfo` 对象信息
- ✅ `OssPathParser` 路径解析

### MinioService 特有方法

以下方法是 `MinioService` 特有的，在 `S3Client` 接口中没有定义：

- `getAllBuckets()` - 获取所有 Bucket
- `getBucket(String)` - 获取指定 Bucket
- `getAllObjectsByPrefix()` - 根据前缀查询对象
- `getObjectInfo()` - 获取对象元数据

如果你的代码使用了这些方法，有两个选择：

1. **继续使用 MinioService**（不推荐）
2. **迁移到 S3Client 通用方法**（推荐）

## 常见问题

### Q1: 切换存储后，数据库中的 URL 还能用吗？

**A:** 可以，但需要注意：

- 如果你存储的是**相对路径**（如 `bucket/file.txt`）：✅ 完全兼容
- 如果你存储的是**完整 URL**（如 `http://localhost:9000/bucket/file.txt`）：❌ 需要更新

**建议：** 始终在数据库中存储相对路径，使用 `@OssUrl` 注解自动转换。

### Q2: 切换后性能会受影响吗？

**A:** 不会。新的抽象层没有引入额外的性能开销，只是在方法调用上增加了一层接口转发，这对性能的影响可以忽略不计。

### Q3: 可以在不停机的情况下切换吗？

**A:** 可以，但需要规划：

1. 部署新版本应用（使用新配置）
2. 验证新应用工作正常
3. 迁移历史数据（可选）
4. 切换流量到新应用

**蓝绿部署建议：**
- 蓝环境：继续使用 MinIO
- 绿环境：使用 RustFS
- 逐步切换流量

### Q4: 如何回滚？

**A:** 非常简单：

1. 修改 `build.gradle`，换回原来的依赖
2. 修改配置文件，换回原来的配置
3. 重新部署

由于业务代码没有改动，回滚非常安全。

## 测试清单

切换存储后，建议测试以下功能：

- [ ] 文件上传
- [ ] 文件下载
- [ ] URL 生成（GET）
- [ ] URL 过期时间
- [ ] 大文件上传（>100MB）
- [ ] 并发上传
- [ ] Bucket 操作
- [ ] 错误处理

## 最佳实践

### 1. 使用环境变量

```yaml
ingot:
  oss:
    rustfs:
      url: ${OSS_URL:http://localhost:8080}
      access-key: ${OSS_ACCESS_KEY}
      secret-key: ${OSS_SECRET_KEY}
```

### 2. 配置不同环境

```yaml
# application-dev.yml
ingot:
  oss:
    minio:  # 开发环境使用 MinIO
      url: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin

# application-prod.yml
ingot:
  oss:
    rustfs:  # 生产环境使用 RustFS
      url: https://oss.example.com
      access-key: ${OSS_ACCESS_KEY}
      secret-key: ${OSS_SECRET_KEY}
```

### 3. 监控和日志

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoredFileService {
    private final OssService ossService;
    
    public OSSResult uploadFile(String bucket, MultipartFile file) {
        long startTime = System.currentTimeMillis();
        try {
            OSSResult result = ossService.uploadFile(bucket, fileName, inputStream);
            long duration = System.currentTimeMillis() - startTime;
            log.info("File uploaded: bucket={}, file={}, duration={}ms", bucket, fileName, duration);
            return result;
        } catch (Exception e) {
            log.error("File upload failed: bucket={}, file={}", bucket, fileName, e);
            throw e;
        }
    }
}
```

## 技术支持

如果在迁移过程中遇到问题，可以：

1. 查看相关文档：
   - [OSS 架构设计](oss-architecture.md)
   - [OSS 使用示例](oss-usage-example.md)
   - [重构总结](oss-refactoring-summary.md)

2. 检查配置是否正确
3. 查看应用日志
4. 验证网络连接

## 总结

从 MinIO 迁移到新的抽象层，或从 MinIO 切换到 RustFS，都非常简单：

1. **依赖调整** - 修改 build.gradle
2. **配置修改** - 修改 application.yml
3. **业务代码** - 无需修改！

整个过程可以在几分钟内完成，且完全无风险。
