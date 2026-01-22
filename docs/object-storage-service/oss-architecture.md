# OSS 对象存储架构设计

## 设计目标

1. **统一接口** - 业务层使用统一的 `OssService` 接口，不关心底层实现
2. **易于扩展** - 支持快速接入新的 S3 兼容存储
3. **无缝切换** - 通过配置切换存储实现，业务代码零改动
4. **代码复用** - S3 兼容存储的公共逻辑统一实现

## 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                       业务层 (Business Layer)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ FileService  │  │ UserService  │  │ OrderService │ ...  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                  │               │
│         └─────────────────┼──────────────────┘               │
│                           ▼                                  │
├─────────────────────────────────────────────────────────────┤
│                   接口层 (Interface Layer)                   │
│              ┌────────────────────────┐                      │
│              │   OssService (接口)    │                      │
│              └────────────────────────┘                      │
│                   - uploadFile()                             │
│                   - getFile()                                │
│                   - getObjectURL()                           │
├─────────────────────────────────────────────────────────────┤
│                  抽象层 (Abstraction Layer)                  │
│              ┌────────────────────────┐                      │
│              │ AbstractS3OssService   │                      │
│              └───────────┬────────────┘                      │
│                          │ 实现公共逻辑                       │
│                          │ 依赖 S3Client 接口                │
│                          ▼                                   │
│              ┌────────────────────────┐                      │
│              │   S3Client (接口)      │                      │
│              └────────────────────────┘                      │
│                   - createBucket()                           │
│                   - putObject()                              │
│                   - getObject()                              │
│                   - getPresignedObjectUrl()                  │
├─────────────────────────────────────────────────────────────┤
│                  实现层 (Implementation Layer)               │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │  MinioOssService     │      │  RustfsOssService    │    │
│  │  (继承Abstract...)   │      │  (继承Abstract...)   │    │
│  └──────────┬───────────┘      └──────────┬───────────┘    │
│             │                              │                │
│             ▼                              ▼                │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │   MinioService       │      │   RustfsService      │    │
│  │  (实现S3Client)      │      │  (实现S3Client)      │    │
│  └──────────┬───────────┘      └──────────┬───────────┘    │
│             │                              │                │
├─────────────┼──────────────────────────────┼────────────────┤
│             ▼                              ▼                │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │  MinIO Java SDK      │      │  AWS S3 Java SDK     │    │
│  └──────────────────────┘      └──────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 核心类图

```
┌─────────────────────────┐
│    «interface»          │
│      OssService         │
├─────────────────────────┤
│ + uploadFile()          │
│ + getFile()             │
│ + getObjectURL()        │
│ + getObjectURL(expire)  │
└───────────▲─────────────┘
            │
            │ 实现
            │
┌───────────┴─────────────┐
│  «abstract»             │
│  AbstractS3OssService   │
├─────────────────────────┤
│ # getS3Client()         │
│ # getDefaultExpire()    │
├─────────────────────────┤
│ + uploadFile()          │◄─────────┐
│ + getFile()             │          │ 依赖
│ + getObjectURL()        │          │
└───────────▲─────────────┘          │
            │                        │
            │ 继承                    │
            │                        │
    ┌───────┴──────────┐             │
    │                  │             │
┌───┴──────────┐  ┌────┴─────────┐  │
│MinioOssService│  │RustfsOssService│ │
└───┬──────────┘  └────┬─────────┘  │
    │                  │             │
    │ 依赖              │ 依赖         │
    ▼                  ▼             │
┌────────────┐    ┌─────────────┐   │
│MinioService│    │RustfsService│   │
│ implements │    │  implements │   │
│ S3Client   │    │  S3Client   │   │
└────────────┘    └─────────────┘   │
                                    │
    ┌───────────────────────────────┘
    │
    ▼
┌──────────────────────┐
│   «interface»        │
│     S3Client         │
├──────────────────────┤
│ + createBucket()     │
│ + bucketExists()     │
│ + putObject()        │
│ + getObject()        │
│ + getPresignedUrl()  │
│ + removeObject()     │
└──────────────────────┘
```

## 模块依赖关系

```
ingot-commons
    └── OssService (接口)
    └── OSSResult (结果类)

ingot-oss-common
    ├── S3Client (接口)
    ├── AbstractS3OssService (抽象类)
    ├── S3Properties (配置基类)
    ├── OssObjectInfo (对象信息)
    ├── OssPathParser (路径解析)
    └── 注解
        ├── @OssUrl
        └── @OssSaveUrl

ingot-oss-minio
    ├── 依赖: ingot-oss-common
    ├── MinioService (实现 S3Client)
    ├── MinioOssService (继承 AbstractS3OssService)
    ├── MinioProperties (配置)
    └── MinioAutoConfiguration (自动配置)

ingot-oss-rustfs
    ├── 依赖: ingot-oss-common
    ├── RustfsService (实现 S3Client)
    ├── RustfsOssService (继承 AbstractS3OssService)
    ├── RustfsProperties (配置)
    └── RustfsAutoConfiguration (自动配置)

业务服务
    ├── 依赖: ingot-oss-minio 或 ingot-oss-rustfs (二选一)
    └── 使用: OssService (接口)
```

## 设计模式

### 1. 策略模式 (Strategy Pattern)

`OssService` 接口定义了对象存储的操作策略，不同的实现（MinIO、RustFS）提供不同的具体策略。

```java
// 策略接口
public interface OssService {
    OSSResult uploadFile(...);
}

// 具体策略 A
public class MinioOssService extends AbstractS3OssService { }

// 具体策略 B
public class RustfsOssService extends AbstractS3OssService { }

// 使用策略
@Service
public class FileService {
    private final OssService ossService;  // 具体使用哪个策略由配置决定
}
```

### 2. 模板方法模式 (Template Method Pattern)

`AbstractS3OssService` 定义了对象存储操作的模板流程，子类提供具体的实现细节。

```java
public abstract class AbstractS3OssService implements OssService {
    
    // 模板方法
    @Override
    public OSSResult uploadFile(String bucket, String fileName, InputStream inputStream) {
        try {
            OSSResult result = new OSSResult();
            getS3Client().putObject(bucket, fileName, inputStream);  // 调用钩子方法
            result.setBucketName(bucket);
            result.setFileName(fileName);
            result.setUrl(getObjectURL(bucket + "/" + fileName));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // 钩子方法（由子类实现）
    protected abstract S3Client getS3Client();
    protected abstract int getDefaultExpiredTime();
}
```

### 3. 适配器模式 (Adapter Pattern)

`MinioService` 和 `RustfsService` 将不同的底层 SDK（MinIO SDK、AWS SDK）适配到统一的 `S3Client` 接口。

```java
// 目标接口
public interface S3Client {
    void putObject(String bucket, String objectName, InputStream stream);
}

// 适配器 A - 适配 MinIO SDK
public class MinioService implements S3Client {
    private MinioClient client;  // 被适配的对象
    
    @Override
    public void putObject(...) {
        // 调用 MinIO SDK 的方法
        client.putObject(PutObjectArgs.builder()...);
    }
}

// 适配器 B - 适配 AWS SDK
public class RustfsService implements S3Client {
    private software.amazon.awssdk.services.s3.S3Client s3Client;  // 被适配的对象
    
    @Override
    public void putObject(...) {
        // 调用 AWS SDK 的方法
        s3Client.putObject(PutObjectRequest.builder()...);
    }
}
```

## 扩展新的存储实现

要支持新的 S3 兼容存储（例如阿里云OSS、腾讯云COS），只需：

### 步骤1：创建新模块

```
ingot-framework/
  └── ingot-oss-aliyun/
      ├── build.gradle
      └── src/main/java/com/ingot/framework/oss/aliyun/
          ├── properties/
          │   └── AliyunProperties.java
          ├── service/
          │   ├── AliyunService.java       (实现 S3Client)
          │   └── AliyunOssService.java    (继承 AbstractS3OssService)
          └── AliyunAutoConfiguration.java
```

### 步骤2：实现 S3Client

```java
public class AliyunService implements S3Client {
    private OSS ossClient;  // 阿里云SDK客户端
    
    @Override
    public void putObject(String bucket, String objectName, InputStream stream) {
        // 调用阿里云 OSS SDK
        ossClient.putObject(bucket, objectName, stream);
    }
    
    // ... 实现其他方法
}
```

### 步骤3：继承 AbstractS3OssService

```java
public class AliyunOssService extends AbstractS3OssService {
    private final AliyunService aliyunService;
    private final AliyunProperties properties;
    
    @Override
    protected S3Client getS3Client() {
        return aliyunService;
    }
    
    @Override
    protected int getDefaultExpiredTime() {
        return properties.getExpiredTime();
    }
}
```

### 步骤4：创建自动配置

```java
@AutoConfiguration
@EnableConfigurationProperties(AliyunProperties.class)
public class AliyunAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(AliyunService.class)
    public AliyunService aliyunService(AliyunProperties properties) {
        return new AliyunService(...);
    }
    
    @Bean
    @ConditionalOnMissingBean(OssService.class)
    public OssService aliyunOssService(AliyunService aliyunService, AliyunProperties properties) {
        return new AliyunOssService(aliyunService, properties);
    }
}
```

**完成！** 业务代码完全无需修改，只需替换依赖和配置即可使用新的存储。

## 配置优先级

当多个 OSS 实现同时存在时，通过 `@ConditionalOnMissingBean(OssService.class)` 确保只有一个生效：

1. 如果只有 MinIO 依赖 → 使用 MinioOssService
2. 如果只有 RustFS 依赖 → 使用 RustfsOssService
3. 如果两者都有 → 由 Spring Boot 的 AutoConfiguration 顺序决定（建议只引入一个）

推荐做法：**业务模块只引入一个 OSS 实现依赖**。

## 注解处理流程

### @OssUrl 序列化流程

```
查询数据库
    ↓
获取到路径: "avatars/user/123.jpg"
    ↓
Jackson 序列化
    ↓
检测到 @OssUrl 注解
    ↓
调用 OssUrlSerializer
    ↓
注入 OssService
    ↓
调用 ossService.getObjectURL("avatars/user/123.jpg")
    ↓
返回预签名URL: "http://localhost:9000/avatars/user/123.jpg?X-Amz-Algorithm=..."
    ↓
前端接收到完整URL
```

### @OssSaveUrl 反序列化流程

```
前端提交带签名的URL
    ↓
"http://localhost:9000/avatars/user/123.jpg?X-Amz-Algorithm=..."
    ↓
Jackson 反序列化
    ↓
检测到 @OssSaveUrl 注解
    ↓
调用 OssSaveUrlDeserializer
    ↓
解析URL，提取路径: "avatars/user/123.jpg"
    ↓
保存到数据库
```

## 性能考虑

### 1. 连接池

MinIO 和 AWS SDK 都内置了连接池管理，无需额外配置。

### 2. 预签名URL缓存

如果同一文件需要频繁生成URL，可以考虑缓存：

```java
@Cacheable(value = "ossUrls", key = "#path + '-' + #expireSeconds")
public String getObjectURL(String path, int expireSeconds) {
    return ossService.getObjectURL(path, expireSeconds);
}
```

### 3. 异步上传

大文件上传建议使用异步：

```java
@Async
public CompletableFuture<OSSResult> uploadFileAsync(String bucket, MultipartFile file) {
    return CompletableFuture.completedFuture(ossService.uploadFile(bucket, fileName, inputStream));
}
```

## 安全考虑

### 1. 访问凭证

- 使用环境变量或配置中心管理 AccessKey 和 SecretKey
- 不要硬编码在代码中
- 定期轮换密钥

### 2. URL签名

- 合理设置过期时间，避免URL永久有效
- 敏感文件使用短过期时间（如5分钟）
- 公开资源可以使用长过期时间（如1天）

### 3. 权限控制

- Bucket级别的访问控制
- 对象级别的权限管理
- 结合业务层权限验证

## 总结

这个架构设计实现了：

1. ✅ **高内聚低耦合** - 业务层与存储实现完全解耦
2. ✅ **开闭原则** - 对扩展开放，对修改关闭
3. ✅ **依赖倒置** - 依赖抽象（接口）而非具体实现
4. ✅ **单一职责** - 每个类只负责一个功能
5. ✅ **里氏替换** - 所有 S3Client 实现可以互相替换

通过这种设计，可以轻松支持任何 S3 兼容的对象存储服务，且业务代码完全无感知。
