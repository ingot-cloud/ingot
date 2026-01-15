# Ingot OSS Common

## 概述

`ingot-oss-common` 是对象存储服务的公共抽象层，提供了统一的接口和基础实现，支持 S3 兼容的对象存储服务（如 MinIO、RustFS 等）。

## 架构设计

### 核心接口和类

1. **S3Client** - S3 兼容客户端接口
   - 定义了 S3 协议的核心操作（创建/删除 Bucket、上传/下载对象、获取预签名 URL 等）
   - 所有 S3 兼容的存储服务都应该实现这个接口

2. **AbstractS3OssService** - S3 OSS 服务抽象类
   - 实现了 `OssService` 接口
   - 提供了基于 `S3Client` 的通用业务逻辑
   - 子类只需要提供 `S3Client` 实例和默认过期时间

3. **OssObjectInfo** - OSS 对象信息
   - 封装了 Bucket 名称和对象名称

4. **OssPathParser** - OSS 路径解析器
   - 支持解析完整 URL 和 bucket/objectName 格式的路径

5. **S3Properties** - S3 通用配置属性
   - 包含 URL、AccessKey、SecretKey、过期时间等配置

## 如何实现新的 S3 兼容存储

以 MinIO 为例：

### 1. 实现 S3Client 接口

```java
@RequiredArgsConstructor
public class MinioService implements S3Client, InitializingBean {
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private MinioClient client;

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
        // MinIO 特定的实现
    }
    
    // ... 实现其他方法
}
```

### 2. 继承 AbstractS3OssService

```java
@Slf4j
@RequiredArgsConstructor
public class MinioOssService extends AbstractS3OssService {
    private final MinioService minioService;
    private final MinioProperties minioProperties;

    @Override
    protected S3Client getS3Client() {
        return minioService;
    }

    @Override
    protected int getDefaultExpiredTime() {
        return minioProperties.getExpiredTime();
    }
}
```

### 3. 创建自动配置类

```java
@AutoConfiguration
@EnableConfigurationProperties({MinioProperties.class})
public class MinioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MinioService.class)
    @ConditionalOnProperty(name = "ingot.oss.minio.url")
    public MinioService minioService(MinioProperties properties) {
        return new MinioService(
                properties.getUrl(),
                properties.getAccessKey(),
                properties.getSecretKey()
        );
    }

    @Bean
    @ConditionalOnMissingBean(OssService.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public OssService minioOSSService(MinioService minioService,
                                      MinioProperties properties) {
        return new MinioOssService(minioService, properties);
    }
}
```

## 使用方式

### 1. 添加依赖

选择一个 S3 兼容的实现，例如 MinIO：

```gradle
dependencies {
    implementation project(":ingot-framework:ingot-oss-minio")
}
```

或者使用 RustFS：

```gradle
dependencies {
    implementation project(":ingot-framework:ingot-oss-rustfs")
}
```

### 2. 配置

**MinIO 配置示例：**

```yaml
ingot:
  oss:
    minio:
      url: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      expired-time: 300
```

**RustFS 配置示例：**

```yaml
ingot:
  oss:
    rustfs:
      url: http://localhost:8080
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1
      expired-time: 300
```

### 3. 业务代码使用

```java
@Service
@RequiredArgsConstructor
public class FileService {
    private final OssService ossService;

    public OSSResult upload(String bucket, String fileName, InputStream inputStream) {
        return ossService.uploadFile(bucket, fileName, inputStream);
    }

    public String getFileUrl(String path) {
        return ossService.getObjectURL(path);
    }
}
```

### 4. 无缝切换

要从 MinIO 切换到 RustFS，只需要：

1. 修改 Gradle 依赖，将 `ingot-oss-minio` 改为 `ingot-oss-rustfs`
2. 修改配置文件，将 `ingot.oss.minio.*` 改为 `ingot.oss.rustfs.*`
3. 业务代码无需修改

## 注解支持

### @OssUrl - 自动转换为预签名 URL

```java
public class FileDTO {
    @OssUrl
    private String fileUrl;  // 自动转换为带过期时间的访问URL
    
    @OssUrl(expireSeconds = 600)
    private String imageUrl; // 自定义过期时间
}
```

### @OssSaveUrl - 保存时去除签名信息

```java
public class FileEntity {
    @OssSaveUrl
    private String fileUrl;  // 保存时自动去除签名参数
}
```

## 优势

1. **统一接口** - 业务层只需要依赖 `OssService` 接口，不关心具体实现
2. **易于扩展** - 新增 S3 兼容存储只需实现 `S3Client` 接口
3. **无缝切换** - 通过配置即可切换不同的存储实现
4. **代码复用** - 公共逻辑在 `AbstractS3OssService` 中实现，避免重复代码
5. **类型安全** - 使用 Java 接口和抽象类，编译期检查

## 现有实现

- **ingot-oss-minio** - MinIO 实现（基于 MinIO Java SDK）
- **ingot-oss-rustfs** - RustFS 实现（基于 AWS S3 SDK）
